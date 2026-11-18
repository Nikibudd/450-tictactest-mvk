# Web-Konsole

TicTacTest bleibt eine Konsolen-Applikation. Das unveränderte Jar aus dem Release läuft in einer Web-Konsole:
Für jede Browser-Verbindung startet ein eigener Java-Prozess, Ein- und Ausgabe laufen über einen WebSocket
in ein Terminal im Browser.

```
Browser ── HTTP/WebSocket ──► nginx :80 ──► ttyd 127.0.0.1:7681 ── Pseudo-Terminal ──► java -jar tictactest.jar
   │                            └── /version.txt (laufende Version)
   └── Frontend (statische Seite, bettet das Terminal per <iframe> ein)
```

## Werkzeug: ttyd

| Werkzeug   | Beurteilung |
|------------|-------------|
| **ttyd**   | Gewählt. Ein statisches Binary, echtes Pseudo-Terminal, xterm.js im Browser. ANSI-Farben und Zeilenumbrüche funktionieren ohne eigenen Code. Begrenzung der Verbindungen (`--max-clients`) und Bindung an eine Adresse (`--interface`) eingebaut. |
| GoTTY      | Gleiches Prinzip, aber seit Jahren kaum gepflegt. |
| websocketd | Überträgt nur Textzeilen, kein Terminal. Braucht eine eigene Seite, die die ANSI-Farbcodes des Spielfelds übersetzt. |

## Lokal starten

Voraussetzung: `gh`, Java 21+, `ttyd`, `nginx`, `python3` (macOS: `brew install ttyd nginx`).

```
scripts/run-local.sh v1.0.0
```

Das Skript lädt die Assets des Releases herunter (es baut nichts) und startet:

- ttyd mit dem Jar auf `127.0.0.1:7681`
- nginx auf http://localhost:8080 mit derselben Konfiguration wie auf EC2 (`deploy/tictactest.nginx.conf`)
- das Frontend aus dem Frontend-Archiv auf http://localhost:8000 (`python3 -m http.server`)

## ttyd-Optionen

```
ttyd --interface 127.0.0.1 --port 7681 --writable --max-clients 8 \
     --client-option titleFixed=TicTacTest --client-option disableLeaveAlert=true \
     java -Xmx32m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -jar tictactest.jar
```

| Option | Zweck |
|--------|-------|
| `--writable` | Ohne diese Option ist das Terminal seit ttyd 1.7 schreibgeschützt: Man sieht das Spielfeld, kann aber nicht tippen. |
| `--interface 127.0.0.1` | ttyd ist nur lokal erreichbar, von aussen nur über nginx. |
| `--max-clients 8` | Höchstens 8 gleichzeitige Spiele, siehe Speicherbedarf unten. |
| `-Xmx32m -XX:+UseSerialGC -XX:TieredStopAtLevel=1` | Kleiner Heap, einfacher Garbage Collector und nur der schnelle JIT-Compiler: weniger Speicher pro Spiel. |
| `disableLeaveAlert` | Kein "Seite wirklich verlassen?"-Dialog beim Schliessen des Tabs. |

## Beobachtungen

- **Spielende**: Das Jar beendet sich, ttyd schliesst die Verbindung. Das Terminal zeigt "Press ⏎ to Reconnect".
  Enter baut eine neue Verbindung auf, und damit startet ein neuer Prozess: eine neue Partie.
- **Ungültige Eingabe** (z. B. `abc`): `HumanPlayer` wirft eine `NumberFormatException`, der Stacktrace erscheint
  im Terminal und die Partie ist zu Ende. Ein besetztes Feld führt ebenso zu einer `IllegalStateException`.
  Das ist das Verhalten des Jars, wir ändern es nicht.
- **Mehrere Tabs**: Jeder Tab ist ein eigener Java-Prozess. Mit drei Tabs laufen drei JVMs.
  Gemessen auf der t2.micro (1 GB RAM): rund 40 MB pro Spiel, mit 8 Spielen 322 MB, 527 MB bleiben frei.
  Ohne Begrenzung könnten genug offene Tabs den Server lahmlegen. `--max-clients 8` lehnt die 9. Verbindung ab.

## Sicherheit

Eine Web-Konsole führt einen Befehl für jeden aus, der die URL kennt, und die URL ist öffentlich.
Wenige Minuten nachdem die Instanz online war, stand bereits ein automatischer Scanner im nginx-Log.

**Fehlkonfiguration**: Wäre der Befehl `bash` oder `login` statt `java -jar`, hätte jede Besucherin eine Shell
auf dem Server. Sie könnte Dateien lesen, Software installieren (z. B. Krypto-Miner), andere Systeme angreifen
und über den Metadata-Service die Credentials des Instance Profiles (LabRole) abholen, also auf unser AWS-Konto
zugreifen.

**Massnahmen**:

- Der Befehl ist fest: `java -jar …/tictactest.jar`. Die ttyd-Option `--url-arg` bleibt aus, sonst würden
  URL-Parameter zu Argumenten des Befehls.
- Der Dienst läuft als Systembenutzer `tictactest` ohne Login-Shell und ohne Schreibrechte auf die Releases.
- systemd-Härtung: `NoNewPrivileges`, `ProtectSystem=strict`, `ProtectHome`, `PrivateTmp`.
- ttyd hört nur auf `127.0.0.1`, von aussen ist nur nginx auf Port 80 erreichbar.
- Metadata-Service nur mit IMDSv2 (`http_tokens = "required"`).
- `--max-clients` gegen Überlastung.

## Versionsabfrage

```
curl http://<web-konsole>/version.txt
1.2.0
```

- Das Deployment schreibt `version.txt` neben das Jar (`/opt/tictactest/releases/<version>/`).
  nginx liefert die Datei des Releases aus, auf den der Symlink `current` zeigt.
- Die Version stammt aus dem Release-Tag (`v1.2.0` → `1.2.0`). Dieselbe Zahl steckt im Dateinamen des Jars,
  weil der Build sie aus dem Tag übernimmt. Das Manifest im Jar enthält keine Version, und wir verändern das Jar nicht.
- Das Frontend liegt auf einer anderen Origin. nginx sendet deshalb `Access-Control-Allow-Origin: *`,
  sonst blockiert der Browser die Abfrage ("blocked by CORS policy" in der Entwickler-Konsole).
  `Cache-Control: no-store` verhindert, dass nach einem Deployment eine alte Version angezeigt wird.

## Frontend

Plain HTML/CSS/JavaScript in `frontend/`, ohne Framework und ohne Build-Step:

- `index.html`: Titel, Spielregeln, Terminal als `<iframe>`, Anzeige beider Versionen
- `config.js`: Adresse der Web-Konsole, wird vor `app.js` geladen
- `app.js`: setzt die Terminal-URL aus der Konfiguration und fragt die Versionen ab

Die Adresse der Web-Konsole steht nicht im Code. `config.js` im Repo zeigt auf `http://localhost:8080`.
In der Cloud schreibt OpenTofu eine eigene `config.js` mit der Adresse der Instanz in den Bucket
(siehe [deployment.md](deployment.md)).

Die Frontend-Version kommt aus `version.json`, die der Release-Workflow ins Frontend-Archiv legt.
Lokal ohne Release gibt es sie nicht, die Seite zeigt dann "unbekannt".

Lokal mit zwei Servern getestet: Das Einbetten funktioniert (ttyd setzt kein `X-Frame-Options`),
die Backend-Version erscheint dank CORS-Header.

## Release

Ein Tag `v*` erzeugt einen GitHub-Release mit (siehe [RELEASE.md](RELEASE.md)):

- `450-tictactest-mvk-<version>.jar`
- `tictactest-frontend-<version>.zip` (Inhalt von `frontend/` plus `version.json`)

**Start-Skript, systemd-Unit und nginx-Konfiguration** (`deploy/`) gehören zur Infrastruktur, nicht zum Release.
Sie legen fest, *wie* die Applikation läuft, ändern sich selten und werden beim ersten Start der Instanz
eingerichtet (`user_data`). Eine Änderung daran läuft deshalb über `tofu plan`/`tofu apply`.
Der Release enthält nur, *was* läuft.

**Warum auch das Frontend als Release-Asset?** Ein Deployment, ein Rollback oder ein Wiederaufbau nach
`tofu destroy` braucht genau die Dateien einer Version, ohne Checkout und ohne Bauen.
Jar und Frontend eines Releases gehören zusammen, und man sieht im Release, was ausgeliefert wurde.
In mehreren Umgebungen läuft so überall dasselbe Artefakt.
