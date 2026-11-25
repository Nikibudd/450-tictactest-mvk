# TicTacTest

Tic-Tac-Toe als Konsolen-Applikation (Modul 450), im Modul 324 als Web-App in die Cloud gebracht.

**Spielen: <http://esklgor9lojpjlcambdvtvsgc-kcwobptdsysq.s3-website-us-east-1.amazonaws.com>**

Das Jar aus dem GitHub-Release läuft unverändert in einer Web-Konsole (ttyd) auf einer EC2-Instanz,
das Frontend ist eine statische Seite in einem S3-Bucket.

## Dokumentation

- [IAC.md](docs/IAC.md): Einstieg OpenTofu, State und Git, Kosten
- [webkonsole.md](docs/webkonsole.md): Web-Konsole, Sicherheit, Versionsabfrage, Frontend
- [deployment.md](docs/deployment.md): Infrastruktur, Deployment, Secrets, Reflexion
- [RELEASE.md](docs/RELEASE.md): Release-Prozess
- [TEST_CONCEPT.md](docs/TEST_CONCEPT.md), [TESTS.md](docs/TESTS.md), [COVERAGE.md](docs/COVERAGE.md): Tests (Modul 450)

## Struktur

| Ordner | Inhalt |
|--------|--------|
| `src/` | Spiel (Java) |
| `frontend/` | statische Webseite |
| `infra/` | OpenTofu: EC2, Elastic IP, S3-Buckets |
| `deploy/` | systemd-Unit und nginx-Konfiguration für die Instanz |
| `scripts/` | lokaler Start, Deployment, Änderungserkennung |
