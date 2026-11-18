# Infrastructure as Code mit OpenTofu

Auftrag "OpenTofu / Terraform – Erste Cloud-Infrastruktur". Die Konfiguration liegt in `infra/` und wurde später
für die Web-App erweitert (siehe [deployment.md](deployment.md)).

## 1. Zugang zum AWS Learner Lab

- Anmeldung über die Einladung zu AWS Academy, Kurs **324 HS26 IAC**: funktioniert.
- Nach "Start Lab" liefert "AWS Details → AWS CLI" temporäre Credentials (Access Key, Secret Key, Session Token).
  Sie stehen lokal im Profil `[learnerlab]` in `~/.aws/credentials`, nicht im Repository, und laufen mit der Session ab.
- `aws sts get-caller-identity --profile learnerlab` meldet die Rolle `voclabs` im Lab-Konto.
- **Regionen**: nur `us-east-1` und `us-west-2`. `eu-central-1` liefert `UnauthorizedOperation`.
- **Verfügbare Dienste** (lesend geprüft): EC2, VPC, S3, Lambda, RDS, DynamoDB, ECS, ECR, EKS, IAM (nur lesen),
  CloudFormation, CloudWatch, SNS, SQS, Elastic Load Balancing, Route 53, Secrets Manager, SSM, Elastic Beanstalk.
  Gesperrt: App Runner.
- **Einschränkungen**, auf die wir gestossen sind: keine eigenen IAM-Rollen (stattdessen `LabInstanceProfile`),
  `s3:GetBucketObjectLockConfiguration` verboten (darum Buckets über `awscc`, siehe deployment.md).

## 2. Grundbegriffe

> Entwurf. Gemäss Auftrag in eigenen Worten formulieren.

| Begriff | Bedeutung |
|---------|-----------|
| **Infrastructure as Code** | Server, Netzwerke, Firewalls usw. werden in Dateien beschrieben statt von Hand in einer Konsole angeklickt. Die Dateien sind versioniert, überprüfbar und lassen dieselbe Umgebung beliebig oft gleich entstehen. |
| **Terraform** | Werkzeug von HashiCorp, das Infrastruktur aus deklarativen `.tf`-Dateien erstellt, ändert und löscht. Seit 2023 unter der Business Source License, also nicht mehr Open Source. |
| **OpenTofu** | Open-Source-Fork von Terraform (Linux Foundation), entstanden nach dem Lizenzwechsel. Gleiche Sprache und praktisch gleiche Befehle (`tofu` statt `terraform`), bestehende Provider funktionieren. |
| **Provider** | Plugin, das OpenTofu mit einer Plattform verbindet und deren API anspricht, z. B. `hashicorp/aws`. Er weiss, wie man eine EC2-Instanz anlegt oder liest. |
| **Resource** | Ein Objekt, das OpenTofu verwalten soll, z. B. `aws_instance` oder `aws_security_group`. Man beschreibt den gewünschten Zustand, OpenTofu erstellt oder ändert es. |
| **Variable** | Eingabewert, der die Konfiguration anpassbar macht, ohne den Code zu ändern, z. B. Region oder Instanztyp. Werte kommen aus Defaults, `.tfvars`-Dateien oder der Kommandozeile. |
| **Output** | Wert, den OpenTofu nach `apply` ausgibt, z. B. die öffentliche Adresse. Für Menschen und als Schnittstelle zu anderen Werkzeugen (unsere Deploy-Skripte lesen die Outputs). |
| **State** | Datei (`terraform.tfstate`), in der OpenTofu festhält, welche realen Objekte zu welcher Resource gehören und welche Attribute sie haben. Ohne State wüsste OpenTofu nicht, was es schon erstellt hat. |

## 3. Erste Konfiguration

Installiert: OpenTofu 1.13 (`brew install opentofu`), AWS CLI 2.

Die erste Version in `infra/` enthielt:

| Datei | Inhalt |
|-------|--------|
| `versions.tf` | `terraform`-Block mit Versionsanforderungen und Provider `aws` mit Region und Profil |
| `variables.tf` | Region, Instanztyp (`t2.micro`), Pfad zum öffentlichen Schlüssel, erlaubte IP für SSH |
| `main.tf` | Amazon-Linux-Image (data), Key Pair, Security Group mit Port 22 nur für die eigene IP, EC2-Instanz |
| `outputs.tf` | öffentliche IP, DNS-Name, fertiger `ssh`-Befehl |
| `terraform.tfvars.example` | Vorlage für die eigene IP; `terraform.tfvars` selbst wird nicht committed |

- **SSH-Schlüssel**: lokal erzeugt (`ssh-keygen -t ed25519 -f ~/.ssh/tictactest_ec2`), ausserhalb des Repos.
  Nur der öffentliche Teil geht über `aws_key_pair` nach AWS.
- **Zugriff getestet**: `ssh -i ~/.ssh/tictactest_ec2 ec2-user@<public_dns>` → Amazon Linux 2023.

**Rolle von Provider und Resource**: Der Provider `aws` ist die Verbindung zu AWS. Er authentifiziert sich mit dem
Profil `learnerlab` und übersetzt die Resources in API-Aufrufe. Die Resources beschreiben, was existieren soll:
Instanz, Security Group, Key Pair. OpenTofu vergleicht das mit dem State und der Wirklichkeit und führt nur die
Differenz aus.

**Vorbereitung für TicTacTest online**: Die Instanz ist der Ort, an dem die Applikation später läuft. Sie ist
reproduzierbar beschrieben, also lässt sie sich für dev, staging und prod gleich aufbauen. Im nächsten Schritt kommen
dazu: Laufzeitumgebung beim ersten Start (`user_data`), eine feste Adresse (Elastic IP), ein offener Port für die
Web-Konsole und ein Bucket für das Frontend. Genau das ist inzwischen umgesetzt (siehe [deployment.md](deployment.md)).

### Ablauf

```
tofu init       Provider herunterladen, .terraform/ und .terraform.lock.hcl anlegen
tofu validate   Syntax und Verweise prüfen
tofu plan       Konfiguration ↔ State ↔ Wirklichkeit vergleichen und anzeigen, was sich ändern würde
tofu apply      genau das ausführen (am besten den gespeicherten Plan: tofu plan -out=x.tfplan, tofu apply x.tfplan)
tofu output     Outputs anzeigen
tofu destroy    alles wieder löschen, was im State steht
```

## 4. State und Git

Der **State** ist das Gedächtnis von OpenTofu: welche Resource welcher realen Instanz-ID, welchem Bucket usw.
entspricht. Ohne ihn würde OpenTofu bei jedem `apply` alles neu erstellen und Bestehendes nicht mehr löschen können.
Ist er verloren oder veraltet, entstehen doppelte oder verwaiste Ressourcen. Er enthält ausserdem Attribute im
Klartext, je nach Ressource auch Passwörter oder Schlüssel. In Teams liegt er deshalb in einem Remote-Backend
(z. B. S3 mit Locking), damit nicht zwei Personen gleichzeitig `apply` ausführen.

| Gehört in Git | Gehört nie in Git |
|---------------|-------------------|
| `*.tf`, `*.tftpl` | `terraform.tfstate`, `*.tfstate.*` (Backups) |
| `.terraform.lock.hcl` (fixiert Provider-Versionen und Prüfsummen) | `.terraform/` (heruntergeladene Provider, gross und plattformabhängig) |
| `terraform.tfvars.example` | `*.tfvars` mit echten Werten (z. B. eigene IP) |
| Doku | Access Keys, Session Tokens, Secrets |
| | private SSH-Schlüssel, `*.tfplan` (enthält Werte), `crash.log` |

Umgesetzt in `.gitignore`. Kontrolle: `git status` zeigt nur die erlaubten Dateien. Die AWS-Credentials liegen in
`~/.aws/credentials`, der private Schlüssel in `~/.ssh/`, beide ausserhalb des Repos.

## 5. Kostenkontrolle

Preise `us-east-1`, On-Demand (das Learner Lab rechnet gegen ein festes Budget pro Lernenden):

| Ressource | Preis | pro Monat |
|-----------|-------|-----------|
| EC2 `t2.micro` | ca. 0.0116 USD/h | ca. 8.50 USD |
| öffentliche IPv4 / Elastic IP | 0.005 USD/h, auch wenn die Instanz gestoppt ist | ca. 3.60 USD |
| EBS gp3, 8 GB | 0.08 USD/GB-Monat | ca. 0.65 USD |
| S3, ein paar KB | vernachlässigbar | < 0.01 USD |

**Warum zerstören?** Eine vergessene Instanz kostet weiter, Tag und Nacht. Eine gestoppte Instanz kostet
weniger, Disk und IP aber weiter. Im Learner Lab ist das Budget begrenzt, ist es aufgebraucht, ist das Lab gesperrt.

**Massnahmen**:

- `tofu destroy` nach dem Test. Weil alles im State steht, wird nichts vergessen.
- kleinste sinnvolle Instanz (`t2.micro`), Instanztyp als Variable
- das Learner Lab stoppt Instanzen am Ende jeder Session automatisch
- in einem echten Konto: AWS Budgets mit Alarm, Tags (`Name`, `Project`) für die Kostenauswertung,
  Testumgebungen zeitgesteuert abbauen

**IaC und Kosten**: Mit IaC ist jede Ressource im Code sichtbar, und `destroy` räumt vollständig auf. Eine Umgebung
lässt sich nur für die Dauer eines Tests aufbauen und danach in Minuten wieder genauso herstellen. Von Hand
angeklickte Ressourcen vergisst man dagegen leicht.

## 6. Ausblick: CI/CD und Umgebungen

Inzwischen umgesetzt: Ein Release-Tag deployt automatisch die Applikation, die Infrastruktur bleibt dabei unverändert.
Infrastrukturänderungen liessen sich ebenso in die Pipeline holen: Bei einem Pull Request, der `infra/` ändert, läuft
`tofu plan` und der Plan erscheint als Kommentar zur Prüfung. Nach dem Merge führt die Pipeline genau diesen Plan mit
`tofu apply` aus. Dafür braucht es einen Remote-State, damit Pipeline und Menschen denselben State sehen.

Für **dev, staging und prod** wird dieselbe Konfiguration mit verschiedenen Variablen (Grösse, Namen) und je eigenem
State angewendet. Änderungen gehen zuerst nach dev, dann staging, dann prod. So zeigt sich ein Fehler, bevor er
Benutzerinnen trifft, und jede Umgebung ist nachvollziehbar und gleich aufgebaut. Details: Reflexion in
[deployment.md](deployment.md).
