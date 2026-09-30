# DevContainer Continuous Deployment

Beschreibt, wie das DevContainer-Image gebaut, versioniert, freigegeben und von CI/CD sowie lokalen
Entwicklungsumgebungen genutzt wird. Umgesetzt in `.github/workflows/devcontainer-auto-deploy.yaml`.

## Versionierungskonzept

Jedes Image wird mit dem **Commit-Hash** (kurz, 12 Zeichen) des Commits getaggt, der `.devcontainer/**`
geändert hat, z.B. `ghcr.io/nikibudd/450-tictactest-mvk-devcontainer:a1b2c3d4e5f6`.

- Automatisch, eindeutig, kein manuelles Versionieren nötig.
- Der Tag ist ein Audit-Trail: man sieht sofort, welcher Commit ein Image erzeugt hat.
- `latest` ist kein Alias für "neuester Build", sondern für den **zuletzt offiziell freigegebenen** Build.

## Ablauf

```
Push auf main (.devcontainer/**)
        │
        ▼
┌───────────────────┐
│ build-and-push     │  baut Image, pusht nach ghcr.io mit Tag <commit-sha>
└─────────┬──────────┘
          │
          ▼
┌───────────────────┐
│ release             │  wartet auf Freigabe (GitHub Environment "devcontainer-release",
│ (environment gate)  │  required reviewers) → retaggt <commit-sha> als "latest"
└─────────┬──────────┘
          │
          ▼
┌───────────────────┐
│ open-release-pr    │  öffnet automatisch einen PR, der .devcontainer/RELEASED_VERSION
└────────────────────┘  aktualisiert (Nachvollziehbarkeit, kein weiteres Gate)
```

Bei Pull Requests, die `.devcontainer/**` ändern, läuft nur `build-and-push` **ohne Push** (reine
Build-Validierung) — Images werden ausschliesslich von `main` aus veröffentlicht.

## Freigabeprozess

Der `release`-Job läuft im GitHub **Environment** `devcontainer-release`. Damit ein Image tatsächlich
zu `latest` wird, muss dieses Environment im Repository unter
**Settings → Environments → devcontainer-release → Required reviewers** mit mindestens einer
freigabeberechtigten Person konfiguriert sein. Ohne diese manuelle Konfiguration pausiert der Job nicht
und jede Änderung würde ungeprüft durchlaufen — die Konfiguration ist also Voraussetzung, nicht optional.

Solange kein Reviewer freigegeben hat, bleibt `latest` unverändert und zeigt weiterhin auf den zuletzt
freigegebenen Stand. Ungeprüfte Images existieren zwar in der Registry (unter ihrem Commit-Hash-Tag),
werden aber von keinem CI-Job oder lokaler Umgebung automatisch verwendet, da diese ausschliesslich
`:latest` referenzieren.

## Nutzung durch CI und lokale Umgebung

- **CI (`CI.yaml`)**: alle Jobs referenzieren `ghcr.io/nikibudd/450-tictactest-mvk-devcontainer:latest`
  und erhalten damit automatisch den zuletzt freigegebenen Stand.
- **Lokal (`devcontainer.json`)**: `"image": "ghcr.io/nikibudd/450-tictactest-mvk-devcontainer:latest"`
  — beim Neu-Öffnen des DevContainers wird automatisch dasselbe freigegebene Image gezogen, kein lokaler
  Build aus dem `Dockerfile` mehr.

## Voraussetzungen (einmalige manuelle Einrichtung)

1. Environment `devcontainer-release` mit Required Reviewers anlegen (siehe oben).
2. Repository-Settings → Actions → General → **"Allow GitHub Actions to create and approve pull
   requests"** aktivieren, sonst schlägt der `open-release-pr`-Job fehl.
