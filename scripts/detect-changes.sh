#!/usr/bin/env bash
# Ermittelt, ob sich Backend (Jar) und Frontend seit dem vorherigen Release-Tag geändert haben.
# usage: scripts/detect-changes.sh <tag>   Ausgabe im Format von $GITHUB_OUTPUT: previous=, backend=, frontend=
set -euo pipefail

TAG=${1:?"usage: scripts/detect-changes.sh <tag>"}
# nächster Tag v* in der History vor diesem Tag, nicht der letzte Commit: ein Release fasst alle Commits seither zusammen
PREVIOUS=$(git describe --tags --abbrev=0 --match 'v*' "$TAG^" 2>/dev/null || true)

if [ -z "$PREVIOUS" ]; then
  echo "Kein vorheriger Release-Tag: erster Release, Backend und Frontend werden deployt" >&2
  echo "previous="
  echo "backend=true"
  echo "frontend=true"
  exit 0
fi

CHANGED=$(git diff --name-only "$PREVIOUS" "$TAG")
echo "Geändert seit $PREVIOUS:" >&2
sed 's/^/  /' <<< "$CHANGED" >&2

matches() {
  if grep -qE "$1" <<< "$CHANGED"; then echo true; else echo false; fi
}

echo "previous=$PREVIOUS"
# Backend: alles, was im Jar landet oder seinen Build bestimmt
echo "backend=$(matches '^(src/main/|build\.gradle$|settings\.gradle$|gradle\.properties$|gradle/|gradlew)')"
echo "frontend=$(matches '^frontend/')"
