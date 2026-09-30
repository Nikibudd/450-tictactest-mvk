# DevContainer Continuous Deployment

Beschreibt, wie das DevContainer-Image gebaut, versioniert, freigegeben und von CI/CD sowie lokalen
Entwicklungsumgebungen genutzt wird. Umgesetzt in `.github/workflows/devcontainer-auto-deploy.yaml`.

## Versionierungskonzept

Jedes Image wird mit dem **Commit-Hash** (kurz, 12 Zeichen) des Commits getaggt, der
`.devcontainer/Dockerfile` geändert hat, z.B.
`ghcr.io/nikibudd/450-tictactest-mvk-devcontainer:a1b2c3d4e5f6`.

- Automatisch, eindeutig, kein manuelles Versionieren nötig.
- Der Tag ist ein Audit-Trail: man sieht sofort, welcher Commit ein Image erzeugt hat.
- Es gibt **kein** bewegliches `latest`. `CI.yaml` und `devcontainer.json` referenzieren immer einen
  konkreten, gepinnten Hash — reproduzierbar, kein "was ist gerade latest"-Rätsel.

## Ablauf

```
Push auf main (.devcontainer/Dockerfile geändert)
        │
        ▼
┌────────────────────┐
│ build-and-push      │  baut Image, pusht nach ghcr.io mit Tag <commit-sha>
└─────────┬───────────┘
          │
          ▼
┌────────────────────┐
│ open-release-pr     │  ersetzt den Image-Tag in CI.yaml + devcontainer.json
└─────────┬───────────┘  durch <commit-sha>, öffnet PR
          │
          ▼
   Review & Merge      ←── DAS ist die Freigabe
          │
          ▼
   CI & lokale DevContainer nutzen automatisch <commit-sha>
```

Bei Pull Requests, die `.devcontainer/Dockerfile` ändern, läuft nur `build-and-push` **ohne Push**
(reine Build-Validierung) — Images werden ausschliesslich von `main` aus veröffentlicht.

Der Trigger reagiert bewusst nur auf `.devcontainer/Dockerfile`, nicht auf das ganze
`.devcontainer/`-Verzeichnis: Nur Änderungen am Dockerfile verändern das Image tatsächlich. Der
Release-PR selbst ändert nur `CI.yaml` und `devcontainer.json` (Referenzen), nie das Dockerfile —
sein Merge löst also keinen erneuten Build aus. Frühere Version hatte hier einen Bug: ein separates
`RELEASED_VERSION`-Audit-File lag unter `.devcontainer/**` und löste beim Mergen des Release-PRs
einen Loop aus (Build → Freigabe → neuer Release-PR → Merge → Build → ...). Diese Datei wurde
entfernt.

## Freigabeprozess

Es gibt kein GitHub Environment und kein manuelles Approval-Gate mehr. **Der Review und Merge des
automatisch erstellten Release-PRs ist die Freigabe.** Solange der PR offen ist, laufen CI-Jobs und
lokale DevContainer weiterhin mit dem zuletzt gemergten (freigegebenen) Hash — das neue, ungeprüfte
Image existiert zwar bereits in der Registry, wird aber von nichts automatisch verwendet, bis jemand
den PR reviewt und mergt.

## Nutzung durch CI und lokale Umgebung

- **CI (`CI.yaml`)**: alle drei Jobs (`build`, `test`, `coverage`) referenzieren
  `ghcr.io/nikibudd/450-tictactest-mvk-devcontainer:<commit-sha>` — der jeweils zuletzt freigegebene
  Stand.
- **Lokal (`devcontainer.json`)**: `"image": "ghcr.io/nikibudd/450-tictactest-mvk-devcontainer:<commit-sha>"`
  — beim Neu-Öffnen des DevContainers wird automatisch derselbe freigegebene Stand gezogen, kein
  lokaler Build aus dem `Dockerfile` mehr.
- Beide Stellen werden vom Release-PR gemeinsam aktualisiert, sodass CI und lokale Umgebung nie
  auseinanderlaufen.

## Voraussetzungen (einmalige manuelle Einrichtung)

Repository-Settings → Actions → General → **"Allow GitHub Actions to create and approve pull
requests"** aktivieren, sonst schlägt der `open-release-pr`-Job fehl.
