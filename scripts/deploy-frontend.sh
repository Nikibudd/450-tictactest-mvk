#!/usr/bin/env bash
# Ersetzt das Frontend im S3-Bucket durch das Frontend-Archiv eines GitHub-Releases.
# config.js stammt aus der Infrastruktur (OpenTofu) und wird weder hochgeladen noch gelöscht.
# usage: scripts/deploy-frontend.sh <tag, z. B. v1.2.0>
# Braucht: REPO, FRONTEND_BUCKET, FRONTEND_URL und AWS-Credentials (lokal: source scripts/env-from-tofu.sh)
set -euo pipefail

TAG=${1:?"usage: scripts/deploy-frontend.sh <tag>"}
VERSION=${TAG#v}
: "${REPO:?}" "${FRONTEND_BUCKET:?}" "${FRONTEND_URL:?}"

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

echo "1/2 Frontend-Archiv aus Release $TAG in den Bucket synchronisieren"
gh release download "$TAG" -R "$REPO" -p 'tictactest-frontend-*.zip' -D "$WORK"
# -DD: Zeitstempel auf jetzt setzen. aws s3 sync lädt nur hoch, was grösser oder neuer ist; beim Rollback sind die
# Dateien eines älteren Releases sonst älter als die im Bucket, und gleich grosse wie version.json blieben stehen.
unzip -q -DD "$WORK"/tictactest-frontend-*.zip -d "$WORK/site"
# --delete entfernt Dateien, die es im neuen Frontend nicht mehr gibt; --exclude schützt config.js davor
aws s3 sync "$WORK/site" "s3://$FRONTEND_BUCKET" --delete --exclude config.js --cache-control no-cache --only-show-errors

echo "2/2 Prüfen, dass $FRONTEND_URL die Version $VERSION ausliefert"
for _ in $(seq 1 12); do
  SERVED=$(curl -fsS --max-time 5 "$FRONTEND_URL/version.json" 2>/dev/null | jq -r .version 2>/dev/null || true)
  if [ "$SERVED" = "$VERSION" ]; then
    curl -fsS --max-time 5 "$FRONTEND_URL/config.js" | grep -q backendUrl \
      || { echo "::error::config.js fehlt im Frontend-Bucket, tofu apply ausführen" >&2; exit 1; }
    echo "Frontend $VERSION wird ausgeliefert"
    exit 0
  fi
  sleep 5
done
echo "::error::Frontend meldet '${SERVED:-nichts}' statt $VERSION" >&2
exit 1
