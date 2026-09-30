# Test Coverage Gate

Beschreibt, wie JaCoCo-Testabdeckung in Pull Requests geprüft, kommentiert und über die Zeit
nachverfolgt wird. Umgesetzt in `.github/workflows/CI.yaml` (Jobs `coverage`, `coverage-check`,
`update-coverage-history`).

## Ablauf

```
Push / Pull Request
        │
        ▼
┌────────────────┐
│ build, test      │
└────────┬─────────┘
         ▼
┌────────────────┐
│ coverage         │  generiert jacocoTestReport (XML + HTML), extrahiert
│                  │  Line-/Branch-Coverage in coverage-summary Artifact
└────────┬─────────┘
         │
   ┌─────┴─────────────────────┐
   ▼ (nur bei Pull Request)    ▼ (nur bei Push auf main)
┌────────────────┐   ┌──────────────────────┐
│ coverage-check   │   │ update-coverage-history │
│ vergleicht mit   │   │ hängt neuen Eintrag an  │
│ Minimum & letztem│   │ history.csv im Branch   │
│ main-Stand,      │   │ coverage-history an      │
│ kommentiert PR,  │   └──────────────────────┘
│ failt bei Verstoß│
└────────────────┘
```

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
Push auf `main`. Format und Zweck sind dort dokumentiert — geplant ist eine spätere Visualisierung
dieser Zeitreihe über GitHub Pages (noch nicht umgesetzt).
