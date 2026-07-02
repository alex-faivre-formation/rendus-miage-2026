#!/usr/bin/env bash
#
# scan-trivy.sh — Scan de sécurité des images MIAGE-Bank avec Trivy
#
# Daemonless : chaque image est exportée en archive tar (buildah push), puis
# lue par Trivy via --input. Aucun démon Docker requis.
#
# À placer dans : ci-scripts/scan-trivy.sh
# Lancer depuis la RACINE :  ./ci-scripts/scan-trivy.sh [TAG]
#
# Variables surchargeables :
#   GATE_SEVERITY=HIGH ./ci-scripts/scan-trivy.sh   # abaisser la gate si besoin
#   REPORTS_DIR=build-reports ./ci-scripts/scan-trivy.sh
#
set -euo pipefail

TAG="${1:-v1}"
REGISTRY_PREFIX="localhost/miage-bank"
REPORTS_DIR="${REPORTS_DIR:-Partie A/02-security-scan/reports}"
GATE_SEVERITY="${GATE_SEVERITY:-CRITICAL}"   # niveau qui interrompt le build
SCAN_SEVERITY="HIGH,CRITICAL"                # sévérités reportées

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT
mkdir -p "$REPORTS_DIR"

# Liste des images miage-bank au bon tag (on exclut les vieilles amc_*)
mapfile -t IMAGES < <(buildah images --format '{{.Name}}:{{.Tag}}' \
  | grep "^${REGISTRY_PREFIX}-" | grep ":${TAG}\$" | grep -v 'amc_' | sort -u)

if [ "${#IMAGES[@]}" -eq 0 ]; then
  echo "!! Aucune image ${REGISTRY_PREFIX}-*:${TAG}. Lance build-images.sh d'abord." >&2
  exit 1
fi

echo "=== Trivy — scan de ${#IMAGES[@]} image(s) | report: ${SCAN_SEVERITY} | gate: ${GATE_SEVERITY} ==="
gate_failed=0

for image in "${IMAGES[@]}"; do
  name="$(basename "${image%%:*}")"           # ex: miage-bank-clients
  tar="${TMP_DIR}/${name}.tar"

  echo ""
  echo ">> ${image}"
  echo "   - export tar (daemonless)"
  buildah push "$image" "docker-archive:${tar}"

  # Rapport JSON (HIGH + CRITICAL)
  trivy image --quiet --input "$tar" \
    --severity "$SCAN_SEVERITY" \
    --format json --output "${REPORTS_DIR}/trivy-${name}.json"

  # Rapport SARIF (compatible GitHub Security)
  trivy image --quiet --input "$tar" \
    --severity "$SCAN_SEVERITY" \
    --format sarif --output "${REPORTS_DIR}/trivy-${name}.sarif"

  # Affichage table à l'écran
  trivy image --quiet --input "$tar" --severity "$SCAN_SEVERITY" --format table || true

  # Gate : échec si au moins une CVE de niveau GATE_SEVERITY
  if ! trivy image --quiet --input "$tar" \
        --severity "$GATE_SEVERITY" --exit-code 1 --format table >/dev/null 2>&1; then
    echo "   !! GATE échouée : CVE ${GATE_SEVERITY} détectée sur ${name}"
    gate_failed=1
  else
    echo "   - gate ${GATE_SEVERITY} : OK"
  fi
done

echo ""
echo "=== Rapports générés dans : ${REPORTS_DIR} ==="
ls -1 "${REPORTS_DIR}"/trivy-*.json 2>/dev/null || true

if [ "$gate_failed" -ne 0 ]; then
  echo ""
  echo "!! BUILD INTERROMPU : des CVE ${GATE_SEVERITY} ont été trouvées." >&2
  echo "   (Tu peux abaisser la gate : GATE_SEVERITY=HIGH ... et le documenter dans ton rendu.)" >&2
  exit 1
fi

echo ">> Aucune CVE ${GATE_SEVERITY} : gate franchie pour toutes les images."
