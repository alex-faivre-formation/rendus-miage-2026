#!/usr/bin/env bash
#
# audit-dive.sh — Audit des layers des images MIAGE-Bank avec Dive (mode CI)
#
# Daemonless : chaque image est exportée en archive tar puis analysée par Dive
# via --source docker-archive. Aucun démon Docker requis.
#
# Seuils définis dans ci-scripts/.dive-ci (efficacité >= 95%, gaspillage <= 20Mo / <= 10%).
# Lancer depuis la RACINE :  ./ci-scripts/audit-dive.sh [TAG]
#
set -euo pipefail

TAG="${1:-v1}"
REGISTRY_PREFIX="localhost/miage-bank"
REPORTS_DIR="${REPORTS_DIR:-Partie A/03-image-audit}"
DIVE_CONFIG="${DIVE_CONFIG:-ci-scripts/.dive-ci}"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT
mkdir -p "$REPORTS_DIR"

mapfile -t IMAGES < <(buildah images --format '{{.Name}}:{{.Tag}}' \
  | grep "^${REGISTRY_PREFIX}-" | grep ":${TAG}\$" | grep -v 'amc_' | sort -u)

if [ "${#IMAGES[@]}" -eq 0 ]; then
  echo "!! Aucune image ${REGISTRY_PREFIX}-*:${TAG}. Lance build-images.sh d'abord." >&2
  exit 1
fi

echo "=== Dive — audit de ${#IMAGES[@]} image(s) en mode CI ==="
fail=0

for image in "${IMAGES[@]}"; do
  name="$(basename "${image%%:*}")"
  tar="${TMP_DIR}/${name}.tar"

  echo ""
  echo ">> ${image}"
  buildah push "$image" "docker-archive:${tar}"

  if dive --ci --ci-config "$DIVE_CONFIG" --source docker-archive "$tar" \
       | tee "${REPORTS_DIR}/dive-${name}.txt"; then
    echo "   - seuils Dive : OK"
  else
    echo "   !! seuils non respectés pour ${name}"
    fail=1
  fi
done

echo ""
echo "=== Rapports Dive dans : ${REPORTS_DIR} ==="
ls -1 "${REPORTS_DIR}"/dive-*.txt 2>/dev/null || true

if [ "$fail" -ne 0 ]; then
  echo ""
  echo "!! Au moins une image ne respecte pas les seuils d'efficacité." >&2
  exit 1
fi
echo ">> Toutes les images respectent les seuils (efficacité >= 95%)."