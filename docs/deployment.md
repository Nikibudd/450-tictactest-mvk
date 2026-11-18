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
