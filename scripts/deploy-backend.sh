#!/usr/bin/env bash
# Deployt das Jar eines GitHub-Releases auf die EC2-Instanz. Die Infrastruktur bleibt unverändert.
# usage: scripts/deploy-backend.sh <tag, z. B. v1.2.0>
# Braucht: REPO, ARTIFACT_BUCKET, INSTANCE_ID, BACKEND_URL und AWS-Credentials (lokal: source scripts/env-from-tofu.sh)
set -euo pipefail

TAG=${1:?"usage: scripts/deploy-backend.sh <tag>"}
VERSION=${TAG#v}
: "${REPO:?}" "${ARTIFACT_BUCKET:?}" "${INSTANCE_ID:?}" "${BACKEND_URL:?}"

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

echo "1/3 Jar aus Release $TAG in den Artefakt-Bucket kopieren"
gh release download "$TAG" -R "$REPO" -p '*.jar' -D "$WORK"
aws s3 cp "$WORK"/*.jar "s3://$ARTIFACT_BUCKET/releases/$VERSION/tictactest.jar" --only-show-errors

echo "2/3 Instanz holt das Jar und startet den Dienst neu (SSM, kein SSH)"
SCRIPT=$(base64 < "$(dirname "$0")/remote/install-backend.sh" | tr -d '\n')
COMMAND_ID=$(aws ssm send-command \
  --instance-ids "$INSTANCE_ID" \
  --document-name AWS-RunShellScript \
  --comment "deploy backend $VERSION" \
  --parameters "$(jq -n --arg c "echo $SCRIPT | base64 -d | bash -s -- '$VERSION' '$ARTIFACT_BUCKET'" '{commands: [$c]}')" \
  --query Command.CommandId --output text)
aws ssm wait command-executed --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID" || true
INVOCATION=$(aws ssm get-command-invocation --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID" --output json)
jq -r .StandardOutputContent <<< "$INVOCATION"
jq -r .StandardErrorContent <<< "$INVOCATION" >&2
STATUS=$(jq -r .Status <<< "$INVOCATION")
if [ "$STATUS" != Success ]; then
  echo "::error::Deployment auf der Instanz fehlgeschlagen ($STATUS)" >&2
  exit 1
fi

echo "3/3 Prüfen, dass $BACKEND_URL/version.txt die Version $VERSION meldet"
for _ in $(seq 1 24); do
  RUNNING=$(curl -fsS --max-time 5 "$BACKEND_URL/version.txt" 2>/dev/null || true)
  if [ "$RUNNING" = "$VERSION" ]; then
    echo "Backend $VERSION läuft"
    exit 0
  fi
  sleep 5
done
echo "::error::Nach 2 Minuten läuft immer noch '${RUNNING:-nichts}' statt $VERSION" >&2
exit 1
