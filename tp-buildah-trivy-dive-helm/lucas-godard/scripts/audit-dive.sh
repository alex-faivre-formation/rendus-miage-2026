#!/usr/bin/env bash
# =============================================================================
#  audit-dive.sh — Audit de l'image MIAGE-Bank avec Dive en mode CI
# -----------------------------------------------------------------------------
#  Prend les images depuis le dossier images/ et exporte dans build-reports/dive/
#  Vérifie que l'image respecte les seuils d'efficacité :
#    - Efficacité minimale : 95%
#    - Espace gaspillé maximum : 20 Mo
#    - Pourcentage d'espace gaspillé : 10%
#
#  Exemple d'usage :
#     scripts/audit-dive.sh                        ---> audite TOUTES les archives de images/
#     scripts/audit-dive.sh images/apigateway.tar  ---> audite uniquement cette archive
# =============================================================================

# le script s'arrête si une commande échoue
set -euo pipefail

# déplacement à la racine du projet
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

# création du dossier build-reports/dive/
OUT="build-reports/dive"
mkdir -p "$OUT"

# logique de boucle
TARGETS=("$@")
if [[ ${#TARGETS[@]} -eq 0 ]]; then
  shopt -s nullglob
  TARGETS=(images/containerfile-version/*.tar)
  shopt -u nullglob

  if [[ ${#TARGETS[@]} -eq 0 ]]; then
    echo "Aucune archive .tar trouvée dans le dossier 'images/'."
    exit 1
  fi
fi

for ARCHIVE in "${TARGETS[@]}"; do
  NAME="$(basename "$ARCHIVE" .tar)"
  REPORT_FILE="$OUT/dive_${NAME}.txt"

  echo "============================================================================="
  echo "==> Démarrage de l'audit Dive pour : $ARCHIVE"
  echo "==> Seuils : Eff >= 95% | Wasted <= 20MB | Wasted % <= 10%"
  echo "============================================================================="

  # exécution de l'audit Dive
  dive "docker-archive://${ARCHIVE}" \
    --ci \
    --lowestEfficiency=0.95 \
    --highestWastedBytes=20MB \
    --highestUserWastedPercent=0.10 | tee "$REPORT_FILE"

  echo "OK : Rapport Dive sauvegardé dans $REPORT_FILE"
  echo ""
done