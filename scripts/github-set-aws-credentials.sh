#!/usr/bin/env bash
# Überträgt die Learner-Lab-Credentials aus dem AWS-Profil als Secrets ins GitHub-Environment "production".
# Nach jedem "Start Lab" ausführen, die Credentials laufen mit der Session ab.
# Die Werte gehen über stdin an gh, sie erscheinen weder in der Kommandozeile noch in der Shell-History.
set -euo pipefail

REPO=${REPO:-Nikibudd/450-tictactest-mvk}
# bewusst nicht $AWS_PROFILE: das zeigt in der Shell evtl. auf ein ganz anderes Konto
PROFILE=${LAB_PROFILE:-learnerlab}

# abgelaufene Credentials gar nicht erst hochladen
aws sts get-caller-identity --profile "$PROFILE" > /dev/null

for key in aws_access_key_id aws_secret_access_key aws_session_token; do
  aws configure get "$key" --profile "$PROFILE" \
    | gh secret set "$(tr '[:lower:]' '[:upper:]' <<< "$key")" -R "$REPO" --env production
done
echo "AWS-Credentials aus Profil $PROFILE im Environment production gesetzt"
