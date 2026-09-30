# coverage-history

Dieser Branch enthält keinen Code, sondern nur `history.csv` — eine Zeitreihe der JaCoCo-Testabdeckung
von `main`. Er wird automatisch vom `update-coverage-history`-Job in `.github/workflows/CI.yaml`
gepflegt, jedes Mal wenn ein Push auf `main` die Testsuite durchläuft.

## Format (`history.csv`)

CSV, ein Eintrag pro Zeile:

| Spalte | Bedeutung |
|---|---|
| `date` | UTC-Zeitstempel des CI-Laufs (ISO 8601) |
| `commit` | Kurzer Commit-Hash (12 Zeichen) auf `main`, der getestet wurde |
| `line_covered` / `line_missed` | JaCoCo `LINE`-Counter |
| `line_coverage_pct` | `line_covered / (line_covered + line_missed) * 100`, 2 Nachkommastellen |
| `branch_covered` / `branch_missed` | JaCoCo `BRANCH`-Counter |
| `branch_coverage_pct` | analog zu `line_coverage_pct` |

## Zweck

- Der `coverage-check`-Job (läuft auf Pull Requests) liest den letzten Eintrag aus dieser Datei als
  Vergleichswert, um Coverage-Regressionen zu erkennen.
- Geplant (noch nicht umgesetzt): Visualisierung dieser Zeitreihe über GitHub Pages.
