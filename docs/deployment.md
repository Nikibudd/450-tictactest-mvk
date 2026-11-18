# Deployment

TicTacTest läuft als Web-App in AWS (Learner Lab, `us-east-1`). Die Infrastruktur beschreibt OpenTofu in `infra/`.
Welche Version läuft, bestimmt allein das Deployment.

```
Browser ──► Frontend: S3 Static Website (öffentlicher Bucket)
   │          index.html, app.js, … aus dem Release   +   config.js von OpenTofu
   │
   └──────► EC2 t2.micro, Elastic IP ── nginx :80 ── ttyd ── java -jar /opt/tictactest/current/tictactest.jar
                  ▲                       └─ /version.txt
                  │ SSM Run Command (kein SSH)
              Deploy-Skript ──► privater Artefakt-Bucket (Jars) ◄── Instanz liest über das Instance Profile
```

## Infrastruktur (`infra/`)

| Datei | Inhalt |
|-------|--------|
| `versions.tf` | Provider `aws` und `awscc`, Profil `learnerlab` |
| `variables.tf` | Region, Instanztyp, AMI-Name, Paketversionen, ttyd-Version und Prüfsumme, SSH-Zugang |
| `instance.tf` | Instanz, Elastic IP, Security Group, Key Pair, Instance Profile (data) |
| `user-data.sh.tftpl` | Einrichtung beim ersten Start: Java 25, nginx, ttyd, Dienst |
| `frontend.tf` | öffentlicher Bucket als Static Website, `config.js` |
| `artifacts.tf` | privater Bucket für die Jars |
| `outputs.tf` | URLs und alles, was ein Deployment braucht |

`deploy/tictactest.service` und `deploy/tictactest.nginx.conf` bettet `user_data` beim ersten Start ein.

### Instanz und Web-Konsole

- **Feste Adresse**: `aws_eip`. Die Instanz wird am Ende jeder Learner-Lab-Session gestoppt, die IP der
  Elastic IP bleibt. Das Frontend verwendet den Hostnamen der Elastic IP (`ec2-….compute-1.amazonaws.com`),
  nicht die IP: Der Web-Filter des Netzes, aus dem wir getestet haben, blockiert Seiten, die nur über eine IP-Adresse aufgerufen werden.
- **Laufzeitumgebung reproduzierbar**:
  - fester AMI-Name statt "neuestes Image" (ein neues AMI würde sonst bei `tofu plan` die Instanz ersetzen)
  - Paketversionen fest: `java-25-amazon-corretto-headless` und `nginx` mit `epoch:version-release`.
    Amazon Linux 2023 bindet das Paket-Repository zusätzlich an die Version des AMI.
  - ttyd ist nicht im Repository: feste Version von GitHub, `sha256sum -c` bricht bei falscher Prüfsumme ab
- **Dienst**: systemd-Unit `tictactest.service`, startet beim Booten (`enable`), als Benutzer `tictactest`,
  `Restart=always` nach einem Absturz.
- **Gleiche Version nach Neustart**: Das Deployment setzt den Symlink `/opt/tictactest/current` auf
  `/opt/tictactest/releases/<version>`. Er liegt auf der EBS-Disk, systemd startet nach dem Booten wieder dasselbe Jar.
- **Instance Profile**: Eigene IAM-Rollen sind im Learner Lab verboten. Die Instanz bekommt das vorhandene
  `LabInstanceProfile` (Rolle `LabRole`, enthält `AmazonSSMManagedInstanceCore` und S3-Zugriff).
  Auf der Instanz liegen keine Access Keys.

### Security Group

| Port | Quelle | Begründung |
|------|--------|------------|
| 80/tcp | `0.0.0.0/0` | nginx: Web-Konsole (HTTP + WebSocket) und `/version.txt`. Muss für alle Spielenden offen sein. |
| 22/tcp | eigene IP (`ssh_allowed_cidr`) | Fehlersuche per SSH. Für Deployments nicht nötig (SSM), könnte entfallen. |
| ausgehend | alle | Pakete, ttyd-Download, S3, SSM-Agent |

Port 7681 (ttyd) ist nicht offen, ttyd hört nur auf `127.0.0.1`.

### Frontend-Hosting

- Bucket als Static Website, Bucket-Policy erlaubt öffentliches `s3:GetObject`.
- **Konfiguration aus der Infrastruktur**: Nur OpenTofu kennt die Adresse der Instanz. Es schreibt deshalb
  `config.js` (`aws_s3_object` aus `frontend-config.js.tftpl`) mit `backendUrl` direkt in den Bucket.
  Das Frontend-Deployment schliesst `config.js` beim Hochladen und Löschen aus (`--exclude config.js`).
- **Nie in diesen Bucket**: Credentials, Secrets, `.tfstate`, `.tfvars`, private Schlüssel, Logs, Jars eines
  privaten Repos. Alles darin ist öffentlich lesbar.

### Learner-Lab-Besonderheit: Buckets über `awscc`

`aws_s3_bucket` schlägt im Learner Lab fehl: Der `aws`-Provider liest nach dem Erstellen jedes Buckets die
Object-Lock-Konfiguration, und eine Service Control Policy verbietet `s3:GetBucketObjectLockConfiguration`
(auch mit Provider 6.x). Die Buckets entstehen deshalb über `awscc_s3_bucket` (Cloud Control API).
Policy, Website-Konfiguration und Objekte verwaltet weiterhin der `aws`-Provider. Weil `awscc` kein
`force_destroy` kennt, leert ein `terraform_data` mit `local-exec`-Provisioner die Buckets bei `tofu destroy`.

### Artefakt-Bezug: privater S3-Bucket

Das Deploy-Skript kopiert das Jar aus dem GitHub-Release in den privaten Artefakt-Bucket
(`releases/<version>/tictactest.jar`), die Instanz holt es dort über das Instance Profile.

- **Privates Repository**: funktioniert unverändert. Die Instanz braucht keinen GitHub-Token, nur das Deploy-Skript
  (lokal `gh`, in GitHub Actions `GITHUB_TOKEN`). Ein direkter Download vom Release bräuchte bei einem privaten
  Repo einen Token auf der Instanz.
- **Nach Neustart**: Das Jar liegt bereits auf der Disk, `current` zeigt darauf. Der Bucket wird nur beim
  Deployment gebraucht.
- **Version wechseln**: Deploy-Skript mit anderem Tag, kein `tofu apply`.
- Gegenüber Kopieren per SSH: kein offener Port 22 und kein privater Schlüssel in der Pipeline nötig.

### Instanz einrichten vs. Applikation deployen

`user_data` installiert nur die Laufzeitumgebung und den Dienst, nicht das Jar. Würde `user_data` das Jar
herunterladen, wäre die Version Teil der Infrastruktur: Ein neuer Release ginge nur über `tofu apply`.
`user_data` läuft ausserdem nur beim ersten Start, eine Änderung würde eine laufende Instanz nicht neu einrichten.
Deshalb gilt `user_data_replace_on_change = true`: Eine geänderte Einrichtung ersetzt die Instanz, statt
stillschweigend nichts zu bewirken. Danach muss die aktuelle Version neu deployt werden.

## Outputs

```
tofu -chdir=infra output
```

| Output | Zweck | Vertraulich? |
|--------|-------|--------------|
| `frontend_url` | Hier wird gespielt | nein, öffentlich |
| `backend_url`, `version_url` | Web-Konsole, laufende Version | nein, öffentlich |
| `public_ip` | Adresse der Elastic IP | nein |
| `region`, `instance_id`, `frontend_bucket`, `artifact_bucket` | Ziele für das Deployment | nicht geheim, ohne AWS-Credentials nutzlos; nicht unnötig veröffentlichen |
| `ssh_command` | Zugang für Admins | nein, der private Schlüssel bleibt lokal |

Vertraulich sind die AWS-Credentials und der private SSH-Schlüssel. Sie sind keine Outputs.
Der State (`terraform.tfstate`) bleibt lokal und wird nicht committed.

## Deploy von Hand

Einmalig: Infrastruktur erstellen (Learner Lab gestartet, Credentials im Profil `learnerlab`).

```
cd infra
cp terraform.tfvars.example terraform.tfvars    # eigene IP für SSH eintragen
tofu init
tofu plan
tofu apply
cd ..
```

Für jedes Deployment:

```
source scripts/env-from-tofu.sh           # REPO, INSTANCE_ID, Buckets, URLs aus den OpenTofu-Outputs
scripts/deploy-backend.sh v1.0.0
scripts/deploy-frontend.sh v1.0.0
tofu -chdir=infra plan                    # "No changes": Ein Deployment ändert die Infrastruktur nicht
```

`scripts/deploy-backend.sh <tag>`:

1. Jar aus dem GitHub-Release laden (`gh release download`) und in den Artefakt-Bucket kopieren
2. `aws ssm send-command` (`AWS-RunShellScript`) führt `scripts/remote/install-backend.sh` als root auf der Instanz aus:
   Jar aus dem Bucket holen, `version.txt` schreiben, **Rauchtest** (startet das Jar, fragt es nach dem ersten Zug?),
   Symlink `current` umsetzen, `systemctl restart tictactest`, alte Releases bis auf drei löschen
3. `/version.txt` abfragen, bis die neue Version erscheint (höchstens 2 Minuten), sonst Fehler

`scripts/deploy-frontend.sh <tag>`:

1. Frontend-Archiv aus dem Release laden und entpacken
2. `aws s3 sync … --delete --exclude config.js`: ersetzt das Frontend, löscht entfernte Dateien, lässt `config.js` stehen
3. `version.json` abfragen, bis die neue Version ausgeliefert wird, und prüfen, dass `config.js` noch da ist

### SSM oder SSH?

| | SSM Run Command | SSH |
|---|---|---|
| Zugang | AWS-Credentials, die die Pipeline ohnehin braucht | zusätzlich privater Schlüssel als Secret |
| Netzwerk | kein offener Port, der Agent verbindet sich nach aussen | Port 22 muss für die IPs von GitHub Actions offen sein (wechselnde Adressen) |
| Nachvollziehbarkeit | Befehl, Status und Ausgabe in der AWS-Konsole | nur im Workflow-Log |
| Nachteil | Agent und Instance Profile nötig, Ausgabe nur gekürzt, etwas langsamer | einfacher zu verstehen, direkte Ausgabe |

Entscheidung: SSM.

### Getestet

- Rauchtest: Ein Jar, das kein Jar ist, bricht das Deployment ab ("Invalid or corrupt jarfile"),
  SSM meldet `Failed`, die bisherige Version läuft weiter.
- Last: 8 gleichzeitige Spiele brauchen 322 MB, die 9. Verbindung lehnt ttyd ab.
- Release A (`v1.0.0`) und Release B (`v1.1.0`, Knopf "Neue Partie") von Hand deployt.
  Nach Release B meldet `tofu plan`: "No changes. Your infrastructure matches the configuration."

## Automatisches Deployment

```
Tag v1.2.0 ─► release.yaml
               ├─ changes    Tag auf main? Vergleich mit dem vorherigen Tag v* → backend=true|false, frontend=true|false
               ├─ ci         Build (Jar-Version aus dem Tag), Tests, Mutation, Coverage
               ├─ release    GitHub-Release mit Jar und Frontend-Archiv
               └─ deploy ──► deploy.yaml (nur wenn backend oder frontend geändert)
                              ├─ Release vorhanden? AWS-Credentials gültig?
                              ├─ scripts/deploy-backend.sh   (wenn backend)   → prüft /version.txt
                              └─ scripts/deploy-frontend.sh  (wenn frontend)  → prüft version.json

Actions → deploy → Run workflow (Tag, Backend ja/nein, Frontend ja/nein) ─► deploy.yaml
```

- **Nur bei einem Release**: `release.yaml` startet nur bei Tags `v*`. Ein Push auf `main` startet `main.yaml`
  (CI und Coverage-Historie), das kein Deployment kennt.
- **Nur Release-Artefakte**: `deploy.yaml` lädt Jar und Frontend-Archiv aus dem GitHub-Release, es baut nichts.
- **Keine Infrastruktur**: Die Pipeline kennt kein `tofu`. Sie bekommt Instanz-ID, Buckets und URLs als
  GitHub-Variablen. `tofu apply` läuft nur lokal.
- **Keine parallelen Deployments**: `concurrency: deploy-production` mit `cancel-in-progress: false`.
  Ein zweites Deployment wartet, bis das erste fertig ist. GitHub hält höchstens ein wartendes Deployment;
  kommt ein drittes dazu, wird das wartende verworfen.
- **Dieselben Skripte wie von Hand**: Die Pipeline ruft `scripts/deploy-backend.sh` und `scripts/deploy-frontend.sh`
  auf. Was lokal funktioniert, funktioniert auch in der Pipeline.

## Secrets-Konzept

### Welche Zugangsdaten braucht die Pipeline?

| Wert | Art | Herkunft | Gültigkeit | Ablage |
|------|-----|----------|------------|--------|
| `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN` | **Secret** | Learner Lab, "AWS Details" | bis zum Ende der Lab-Session (max. 4 h) | Environment-Secret `production` |
| `GITHUB_TOKEN` | Secret | automatisch von GitHub pro Lauf | nur während des Laufs | nichts zu tun, nur `contents: read` |
| `AWS_REGION`, `INSTANCE_ID`, `ARTIFACT_BUCKET`, `FRONTEND_BUCKET`, `BACKEND_URL`, `FRONTEND_URL` | **Variable** | OpenTofu-Outputs | bis zum nächsten `tofu destroy` | Environment-Variable `production` |

Kein SSH-Schlüssel: Das Deployment läuft über SSM, die Instanz holt das Jar selbst über ihr Instance Profile.

**Secrets vs. Variables**: Secrets sind verschlüsselt, lassen sich nach dem Speichern nicht mehr lesen und
werden in Logs als `***` maskiert. Variables sind Klartext und in Logs sichtbar. Alles, womit man auf das
AWS-Konto zugreifen kann, ist ein Secret. Adressen und Namen, die nur zusammen mit den Credentials nützen und
ohnehin in den Outputs stehen, sind Variables. So bleiben sie lesbar und man sieht im Log, wohin deployt wurde.

**Environment statt Repository**: Secrets und Variables liegen im GitHub-Environment `production`.
Es lässt nur `main` und Tags `v*` zu. Ein Workflow auf einem Feature-Branch kann die Credentials nicht verwenden.

### Einrichten und erneuern

```
scripts/github-set-infra-vars.sh        # nach tofu apply: Environment anlegen, Variablen aus den Outputs setzen
scripts/github-set-aws-credentials.sh   # nach jedem "Start Lab": Credentials aus dem Profil learnerlab übertragen
```

Die Credentials gehen über stdin an `gh secret set`, sie stehen weder in der Kommandozeile noch in der History.
Das Skript verwendet fest das Profil `learnerlab` und ignoriert `AWS_PROFILE`: Das zeigt in der eigenen Shell
unter Umständen auf ein ganz anderes Konto. Genau das ist uns beim ersten Einrichten passiert: Für etwa eine Minute
lagen die Werte eines fremden Profils im Environment, bevor wir sie überschrieben haben. Kein Workflow hatte
sie verwendet, weil der Deploy-Workflow noch nicht existierte.

### Abgelaufene Credentials

Die Learner-Lab-Credentials laufen mit jeder Session ab. Es gibt keinen Weg, sie automatisch zu erneuern:
Nur wer im Learner Lab eingeloggt ist, bekommt neue.

- **Erkennen**: Der erste Schritt im Deploy-Job ist `aws sts get-caller-identity`. Mit abgelaufenen Credentials
  schlägt er mit einer klaren Meldung fehl, bevor etwas deployt wird. Der Release selbst entsteht trotzdem.
- **Nachdeployen**: Lab starten, `scripts/github-set-aws-credentials.sh`, dann im fehlgeschlagenen Lauf
  "Re-run failed jobs". Ein Re-Run liest die Secrets neu. Alternativ den Deploy-Workflow von Hand mit dem Tag starten.
- Die Instanz selbst ist nicht betroffen: Sie läuft mit dem Instance Profile, dessen Credentials AWS laufend erneuert.

### Wer kann die Secrets sehen oder benutzen?

- **Sehen**: niemand, auch nicht die Repo-Besitzerin. Man kann sie nur überschreiben oder löschen.
- **Benutzen**: Workflows, die im Environment `production` laufen, also nur auf `main` oder einem Tag `v*`.
  Wer auf `main` pushen oder Tags setzen darf, kann einen Workflow schreiben, der sie benutzt.
- **Logs**: GitHub maskiert Secret-Werte. Abgeleitete Werte (z. B. Base64 eines Secrets) erkennt es nicht.
  Deshalb geben die Skripte nie Credentials aus, und `set -x` kommt in den Deploy-Skripten nicht vor.
  Die Logs haben wir nach den Läufen kontrolliert.
- **Forks und Pull Requests**: Workflows aus Forks bekommen keine Secrets. Pull Requests aus dem eigenen Repo laufen
  auf ihrem Branch, und der darf das Environment nicht benutzen.

### Schaden und Reaktion

Mit den Learner-Lab-Credentials kann man im Lab-Konto alles tun, was die Rolle `voclabs` darf: Instanzen starten
(Kosten, Budget des Labs aufgebraucht), Buckets löschen, unsere Instanz übernehmen. Der Schaden ist auf das
Sandbox-Konto und höchstens die Dauer der Session begrenzt.

Reaktion: Lab-Session beenden (macht die Credentials ungültig), in AWS nach fremden Ressourcen suchen,
im Learner Lab "Reset" falls nötig, Infrastruktur mit `tofu apply` neu aufbauen, neue Credentials setzen.
In einem echten Konto: Schlüssel sofort deaktivieren, CloudTrail auswerten, Schlüssel ersetzen.

### Im echten Unternehmen: OIDC

Ohne Learner-Lab-Einschränkungen speichert man gar keine AWS-Credentials in GitHub. GitHub Actions bekommt pro
Lauf ein signiertes OIDC-Token, das Repository, Branch/Tag und Environment enthält. In AWS vertraut eine
IAM-Rolle diesem Identity Provider nur für `repo:Nikibudd/450-tictactest-mvk:environment:production`.
`aws-actions/configure-aws-credentials` tauscht das Token gegen kurzlebige Credentials (z. B. 1 Stunde).
Es gibt nichts, was ablaufen, rotiert oder gestohlen werden müsste. Im Learner Lab geht das nicht, weil wir
weder Identity Provider noch IAM-Rollen anlegen dürfen.
