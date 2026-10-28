# Release

Jeder Push auf `main`, der Code oder Build ändert, erzeugt automatisch ein GitHub Release mit dem
lauffähigen Jar. Umgesetzt in `.github/workflows/main.yaml` (Job `release`).

## Ablauf

```
Merge develop → main (Code/Build geändert)
        │
        ▼
┌──────────────────────────────┐
│ ci (build-test-coverage.yaml)  │  build → test/mutation → coverage
│ build lädt tictactoe-jar hoch  │
└──────────────┬───────────────┘
        ┌──────┴──────────────────┐
        ▼                         ▼
┌──────────────────────┐   ┌──────────────────────┐
│ update-coverage-history │   │ release                │
└──────────────────────┘   │ GitHub Release + Jar   │
                           └──────────────────────┘
```

- **Freigabe**: wie beim DevContainer ist der Merge auf `main` die Freigabe. Ein Release entsteht nur,
  wenn `ci` grün ist (Tests, 90 % Branch Coverage, 90 % Mutation Score).
- **Version**: die ersten 12 Zeichen des Commit-Hashes, z. B. `a1b2c3d4e5f6`. Tag `release-<hash>`
  (das Präfix verhindert, dass Git den Tag mit einem abgekürzten Commit-Hash verwechselt).
- **Jar**: `450-tictactest-mvk-<hash>.jar`, gebaut im Job `build` und von dort übernommen, also genau das
  getestete Artefakt. Der Jar wird zusätzlich bei jedem CI-Lauf als Artifact `tictactoe-jar` hochgeladen.
- **Release Notes**: von GitHub generiert (gemergte PRs seit dem letzten Release).
- **Re-Run**: existiert das Release für den Commit schon, wird der Job ohne Fehler übersprungen.
- Reine Doku- oder Workflow-Änderungen auf `main` erzeugen kein Release (Pfadfilter in `main.yaml`).

## Ausführen

Benötigt Java 21 oder neuer (kompiliert mit `options.release = 21`, gebaut mit JDK 25):

```
java -jar 450-tictactest-mvk-<hash>.jar
```

Lokal bauen: `./gradlew jar` → `build/libs/450-tictactest-mvk.jar` (ohne Version).
