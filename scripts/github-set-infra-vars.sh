#!/usr/bin/env bash
# Richtet das GitHub-Environment "production" ein und überträgt die OpenTofu-Outputs als Variablen.
# Nach jedem tofu apply ausführen, das Adressen oder Namen ändert (z. B. nach destroy + apply).
set -euo pipefail

source "$(dirname "$0")/env-from-tofu.sh"

# nur main und Release-Tags dürfen in production deployen und die Secrets benutzen
gh api -X PUT "repos/$REPO/environments/production" \
  -F 'deployment_branch_policy[protected_branches]=false' \
  -F 'deployment_branch_policy[custom_branch_policies]=true' > /dev/null
EXISTING=$(gh api "repos/$REPO/environments/production/deployment-branch-policies" --jq '.branch_policies[] | "\(.type):\(.name)"')
for policy in branch:main tag:v*; do
  grep -qxF "$policy" <<< "$EXISTING" || gh api -X POST "repos/$REPO/environments/production/deployment-branch-policies" \
    -f "type=${policy%%:*}" -f "name=${policy#*:}" > /dev/null
done

for name in AWS_REGION INSTANCE_ID ARTIFACT_BUCKET FRONTEND_BUCKET BACKEND_URL FRONTEND_URL; do
  gh variable set "$name" -R "$REPO" --env production --body "${!name}"
done
echo "Environment production: Variablen aus den OpenTofu-Outputs gesetzt"
