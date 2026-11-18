# Setzt die Variablen für die Deploy-Skripte aus den OpenTofu-Outputs. Aufruf: source scripts/env-from-tofu.sh
_outputs=$(tofu -chdir="$(git rev-parse --show-toplevel)/infra" output -json)
_out() { jq -r ".$1.value" <<< "$_outputs"; }
export AWS_PROFILE=learnerlab
export AWS_REGION=$(_out region)
export REPO=Nikibudd/450-tictactest-mvk
export INSTANCE_ID=$(_out instance_id)
export ARTIFACT_BUCKET=$(_out artifact_bucket)
export FRONTEND_BUCKET=$(_out frontend_bucket)
export BACKEND_URL=$(_out backend_url)
export FRONTEND_URL=$(_out frontend_url)
unset _outputs
unset -f _out
