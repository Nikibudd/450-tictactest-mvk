#!/bin/bash
# Läuft per SSM als root auf der Instanz: holt das Jar aus dem Artefakt-Bucket, prüft es und schaltet um.
# usage: install-backend.sh <version> <artifact-bucket>
set -euo pipefail

VERSION=$1
BUCKET=$2
BASE=/opt/tictactest
DIR="$BASE/releases/$VERSION"

mkdir -p "$DIR"
aws s3 cp "s3://$BUCKET/releases/$VERSION/tictactest.jar" "$DIR/tictactest.jar" --only-show-errors
echo "$VERSION" > "$DIR/version.txt"
chmod -R a+rX "$DIR"

# Rauchtest vor dem Umschalten: Startet das Jar und fragt nach dem ersten Zug? Ein kaputtes Jar ersetzt
# die laufende Version nicht.
OUTPUT=$(timeout 30 runuser -u tictactest -- java -jar "$DIR/tictactest.jar" < /dev/null 2>&1 || true)
if ! grep -q "(0-8)" <<< "$OUTPUT"; then
  echo "$OUTPUT" >&2
  echo "Jar $VERSION startet nicht. Es läuft weiterhin: $(cat "$BASE/current/version.txt" 2>/dev/null || echo keine Version)" >&2
  exit 1
fi

# Symlink atomar umsetzen; er liegt auf der EBS-Disk und überlebt so jeden Neustart der Instanz
ln -sfn "$DIR" "$BASE/current.new"
mv -T "$BASE/current.new" "$BASE/current"
systemctl restart tictactest

# die letzten drei Releases behalten
CURRENT=$(readlink -f "$BASE/current")
for old in $(ls -1dt "$BASE"/releases/* | tail -n +4); do
  [ "$old" = "$CURRENT" ] || rm -rf "$old"
done

echo "Backend $VERSION läuft"
