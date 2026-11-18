# Release

Ein Tag `v*` auf `main` (z. B. `v1.2.0`) erzeugt einen GitHub-Release mit dem Jar und dem Frontend-Archiv.
Umgesetzt in `.github/workflows/release.yaml`. Ein Push auf `main` ohne Tag führt nur CI aus (`main.yaml`).

## Ablauf

```
git tag v1.2.0 && git push origin v1.2.0      (Tag auf einem Commit von main)
        │
        ▼
┌───────────────────────────────┐
│ changes                       │  Tag liegt auf main? Was hat sich seit dem vorherigen Tag v* geändert?
└──────────────┬────────────────┘
               ▼
┌───────────────────────────────┐
│ ci (build-test-coverage.yaml) │  build (Jar-Version = Tag) → test/mutation → coverage
└──────────────┬────────────────┘
               ▼
┌───────────────────────────────┐
│ release                       │  Frontend-Archiv packen, GitHub-Release mit Jar + Archiv
└───────────────────────────────┘
```

- **Freigabe**: Merge auf `main`, danach setzt man den Tag. Ein Release entsteht nur, wenn `ci` grün ist
  (Tests, 90 % Branch Coverage, 90 % Mutation Score). Tags auf anderen Branches lässt der Workflow fehlschlagen.
- **Version**: Semantic Versioning aus dem Tag, `v1.2.0` → `1.2.0`.
- **Assets**:
  - `450-tictactest-mvk-<version>.jar`, gebaut im Job `build` und von dort übernommen, also genau das getestete Artefakt
  - `tictactest-frontend-<version>.zip`: Inhalt von `frontend/` plus `version.json` mit der Version
- **Release Notes**: von GitHub generiert, ergänzt um die erkannten Änderungen (Backend/Frontend).
- **Re-Run**: existiert der Release schon, wird der Job ohne Fehler übersprungen.

## Neuen Release erstellen

```
git switch main && git pull
git tag v1.2.0
git push origin v1.2.0
```

## Ausführen

Benötigt Java 21 oder neuer (kompiliert mit `options.release = 21`, gebaut mit JDK 25):

```
java -jar 450-tictactest-mvk-<version>.jar
```

Im Browser, nur aus den Release-Assets: `scripts/run-local.sh v1.2.0` (siehe [webkonsole.md](webkonsole.md)).

Lokal bauen: `./gradlew jar` → `build/libs/450-tictactest-mvk.jar` (ohne Version).
