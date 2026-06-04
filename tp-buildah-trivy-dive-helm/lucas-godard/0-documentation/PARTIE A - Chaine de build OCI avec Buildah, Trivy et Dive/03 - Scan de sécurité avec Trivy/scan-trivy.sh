#!/usr/bin/env bash
# =============================================================================
#  scan-trivy.sh — Analyse de sécurité d'une image MIAGE-Bank avec Trivy
# -----------------------------------------------------------------------------
#  Prend les images depuis le dossier images/ et exporte dans build-reports/
#    - trivy_<nom>.txt    : table lisible (HIGH + CRITICAL)
#    - trivy_<nom>.json   : rapport COMPLET (toutes sévérités) -> machine
#    - trivy_<nom>.sarif  : SARIF (onglet "Security" de GitHub)
#
#  Exemple d'usage :
#     scripts/scan-trivy.sh                        ---> scanne TOUTES les archives de images/
#     scripts/scan-trivy.sh images/apigateway.tar  ---> scanne uniquement cette archive
# =============================================================================

# le script s'arrête si une commande échoue
set -euo pipefail

# déplacement à la racine du projet
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

# création du dossier build-reports
OUT="build-reports/trivy"
mkdir -p "$OUT"

# logique de boucle si pas d'argument
TARGETS=("$@")
if [[ ${#TARGETS[@]} -eq 0 ]]; then
  shopt -s nullglob
  TARGETS=(images/containerfile-version/*.tar)
  shopt -u nullglob

  if [[ ${#TARGETS[@]} -eq 0 ]]; then
    echo "Aucune archive .tar trouvée dans le dossier 'images/containerfile-version/'."
    exit 1
  fi
fi

for IMAGE in "${TARGETS[@]}"; do
  echo "============================================================================="
  echo "==> Démarrage du scan Trivy pour : $IMAGE"
  echo "============================================================================="

  # extraction du nom du service pour nommer les rapports
  if [[ "$IMAGE" == *.tar ]]; then
    SRC=(--input "$IMAGE")
    NAME="$(basename "$IMAGE" .tar)"
  else
    SRC=("$IMAGE")
    NAME="$(echo "$IMAGE" | sed 's#.*/##; s#:.*##')"
  fi

  # export du scan en format TXT et affichage dans le terminal (HIGH et CRITICAL)
  echo "==> [Trivy] Génération Table HIGH/CRITICAL..."
  trivy image --scanners vuln --severity HIGH,CRITICAL --no-progress \
    --format table "${SRC[@]}" | tee "$OUT/trivy_${NAME}.txt"

  # export du scan en format JSON
  echo "==> [Trivy] Génération Rapport JSON complet..."
  trivy image --scanners vuln --no-progress \
    --format json -o "$OUT/trivy_${NAME}.json" "${SRC[@]}"

  # export du scan en format SARIF
  echo "==> [Trivy] Génération Rapport SARIF (GitHub Security)..."
  trivy image --scanners vuln --severity HIGH,CRITICAL --no-progress \
    --format sarif -o "$OUT/trivy_${NAME}.sarif" "${SRC[@]}"

  echo "OK : Rapports Trivy pour ${NAME} sauvegardés dans ${OUT}/"
  echo ""
done