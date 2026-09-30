# Test Coverage Gate

Beschreibt, wie JaCoCo-Testabdeckung geprüft, im Pull Request kommentiert und über die Zeit
nachverfolgt wird. Umgesetzt über drei Workflow-Dateien:

- `.github/workflows/build-test-coverage.yaml` — **reusable workflow** (`workflow_call`), enthält
  die Jobs `build`, `test`, `coverage` (generiert den JaCoCo-Report und extrahiert die Kennzahlen
  in ein `coverage-summary`-Artifact). Wird von den beiden folgenden Workflows aufgerufen, damit
  diese Logik nicht doppelt gepflegt werden muss.
- `.github/workflows/CI.yaml` — Validierung bei Pull Requests (und Push auf `develop`): ruft
  `build-test-coverage.yaml` auf, danach prüft `coverage-check` Minimum & Regression und kommentiert
  den PR.
- `.github/workflows/coverage-history.yaml` — läuft **nur bei Push auf `main`**, und nur wenn sich
  Code/Tests tatsächlich geändert haben (Pfadfilter auf `src/**`, `build.gradle`, `gradlew` etc.).
  Ruft ebenfalls `build-test-coverage.yaml` auf, danach hängt `update-coverage-history` den Stand an
  `history.csv` im Branch `coverage-history` an.

## Ablauf

```
Pull Request (main/develop)              Push auf main (Code/Tests geändert)
        │                                          │
        ▼                                          ▼
┌──────────────────────┐                 ┌──────────────────────┐
│ build-test-coverage    │                 │ build-test-coverage    │
│ (build → test → coverage)│               │ (build → test → coverage)│
└──────────┬────────────┘                 └──────────┬────────────┘
           ▼                                          ▼
   ┌────────────────┐                       ┌──────────────────────┐
   │ coverage-check   │                       │ update-coverage-history │
   │ vergleicht mit   │                       │ hängt neuen Eintrag an  │
   │ Minimum & letztem│                       │ history.csv im Branch   │
   │ main-Stand,      │                       │ coverage-history an      │
   │ kommentiert PR,  │                       └──────────────────────┘
   │ failt bei Verstoß│
   └────────────────┘
```

`build`/`test` laufen dadurch **nicht mehr** bei jedem Push auf `main` — nur noch, wenn Code oder
Tests sich geändert haben, und nicht doppelt zur bereits im PR gelaufenen Validierung. Ein reiner
Doku- oder Workflow-Änderungs-Merge auf `main` löst keinen weiteren Build/Test/Coverage-Lauf aus.

## Regeln

- **Minimum**: Line Coverage muss ≥ 10 % sein (`THRESHOLD` in `CI.yaml`, aktuell bewusst niedrig
  angesetzt, da die Testsuite noch am Anfang steht — kann später erhöht werden).
- **Keine Regression**: Line Coverage eines PRs darf nicht tiefer sein als der zuletzt auf `main`
  gemessene Stand.
- Beide Prüfungen laufen im Job `coverage-check`, der als **required status check** für `main`
  konfiguriert ist — ein Verstoß blockiert den Merge-Button.
- Der Kommentar im PR wird bei jedem Lauf aktualisiert (kein neuer Kommentar pro Push), über
  [`marocchino/sticky-pull-request-comment`](https://github.com/marocchino/sticky-pull-request-comment).

## coverage-history Branch

`history.csv` liegt in einem eigenen, **von `main` unabhängigen** Branch `coverage-history` (siehe
dessen `README.md`). Er wird ausschliesslich vom `update-coverage-history`-Job gepflegt, bei jedem
Push auf `main` mit Code-/Test-Änderungen. Format und Zweck sind dort dokumentiert — geplant ist
eine spätere Visualisierung dieser Zeitreihe über GitHub Pages (noch nicht umgesetzt).
