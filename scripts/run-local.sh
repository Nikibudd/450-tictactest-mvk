#!/usr/bin/env bash
# Startet TicTacTest lokal nur aus den Assets eines GitHub-Releases:
#   Web-Konsole (ttyd hinter nginx) auf http://localhost:8080, Frontend auf http://localhost:8000
# Benötigt: gh, java, ttyd, nginx, python3. Beenden mit Ctrl+C.
set -euo pipefail

TAG=${1:?"usage: scripts/run-local.sh <release-tag, z. B. v1.0.0>"}
REPO=${REPO:-Nikibudd/450-tictactest-mvk}
ROOT=$(cd "$(dirname "$0")/.." && pwd)
DIR="$ROOT/.local-run/$TAG"

rm -rf "$DIR"
mkdir -p "$DIR/assets" "$DIR/backend" "$DIR/frontend" "$DIR/nginx"
gh release download "$TAG" -R "$REPO" -D "$DIR/assets"

cp "$DIR"/assets/*.jar "$DIR/backend/tictactest.jar"
echo "${TAG#v}" > "$DIR/backend/version.txt"

if compgen -G "$DIR/assets/tictactest-frontend-*.zip" > /dev/null; then
  unzip -q "$DIR"/assets/tictactest-frontend-*.zip -d "$DIR/frontend"
else
  echo "Release $TAG hat kein Frontend-Archiv, verwende ./frontend aus dem Repo"
  cp -R "$ROOT/frontend/." "$DIR/frontend"
fi

# dieselbe nginx-Konfiguration wie auf EC2, nur Port und Pfad angepasst
sed -e "s|listen 80 default_server;|listen 127.0.0.1:8080;|" -e "s|/opt/tictactest/current|$DIR/backend|" \
  "$ROOT/deploy/tictactest.nginx.conf" > "$DIR/nginx/tictactest.conf"
cat > "$DIR/nginx/nginx.conf" <<NGINX
worker_processes 1;
daemon off;
pid $DIR/nginx/nginx.pid;
error_log $DIR/nginx/error.log;
events { worker_connections 64; }
http {
    access_log off;
    client_body_temp_path $DIR/nginx/tmp;
    proxy_temp_path $DIR/nginx/tmp;
    fastcgi_temp_path $DIR/nginx/tmp;
    uwsgi_temp_path $DIR/nginx/tmp;
    scgi_temp_path $DIR/nginx/tmp;
    include $DIR/nginx/tictactest.conf;
}
NGINX

trap 'kill 0' EXIT
ttyd --interface 127.0.0.1 --port 7681 --writable --max-clients 8 \
  --client-option titleFixed=TicTacTest --client-option disableLeaveAlert=true \
  java -Xmx32m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -jar "$DIR/backend/tictactest.jar" &
nginx -c "$DIR/nginx/nginx.conf" &
(cd "$DIR/frontend" && python3 -m http.server 8000 --bind 127.0.0.1) &

echo "Web-Konsole: http://localhost:8080   Version: http://localhost:8080/version.txt   Frontend: http://localhost:8000"
wait
