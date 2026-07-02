#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-1.0.0}"

mkdir -p build-reports/dive
mkdir -p build-reports/oci

IMAGES=(
  "config-server"
  "discovery-service"
  "member-service"
  "course-service"
  "competition-service"
  "badge-service"
  "statistics-service"
  "api-gateway"
  "front"
)

SUMMARY_FILE="build-reports/dive/dive-summary-${VERSION}.md"

echo "# Analyse Dive - Images Odoru ${VERSION}" > "${SUMMARY_FILE}"
echo "" >> "${SUMMARY_FILE}"
echo "Ce rapport synthétise l'analyse des images Odoru avec Dive." >> "${SUMMARY_FILE}"
echo "" >> "${SUMMARY_FILE}"
echo "| Image | Archive analysée | Rapport JSON | Statut |" >> "${SUMMARY_FILE}"
echo "|---|---|---|---|" >> "${SUMMARY_FILE}"

echo "=================================================="
echo " Analyse Dive des images Odoru"
echo " Version : ${VERSION}"
echo "=================================================="

for IMAGE in "${IMAGES[@]}"; do
  ARCHIVE_PATH="build-reports/oci/${IMAGE}-${VERSION}.tar"
  JSON_REPORT="build-reports/dive/${IMAGE}-dive.json"

  echo ""
  echo "--------------------------------------------------"
  echo "Image   : ${IMAGE}"
  echo "Archive : ${ARCHIVE_PATH}"
  echo "Rapport : ${JSON_REPORT}"
  echo "--------------------------------------------------"

  if [ ! -f "${ARCHIVE_PATH}" ]; then
    echo "ERREUR : archive absente : ${ARCHIVE_PATH}"
    echo "Lance d'abord : ./scripts/scan-trivy.sh ${VERSION}"
    echo "| ${IMAGE} | ${ARCHIVE_PATH} | - | Archive absente |" >> "${SUMMARY_FILE}"
    exit 1
  fi

  echo ">>> Analyse Dive en mode rapport JSON"

  if dive \
      --source docker-archive \
      --json "${JSON_REPORT}" \
      "${ARCHIVE_PATH}"; then
    echo "OK : analyse Dive réussie pour ${IMAGE}"
    echo "| ${IMAGE} | ${ARCHIVE_PATH} | ${JSON_REPORT} | OK |" >> "${SUMMARY_FILE}"
  else
    echo "ATTENTION : Dive a terminé avec un code non nul pour ${IMAGE}"
    echo "Le rapport peut être partiellement généré selon le cas."
    echo "| ${IMAGE} | ${ARCHIVE_PATH} | ${JSON_REPORT} | À vérifier |" >> "${SUMMARY_FILE}"
  fi
done

echo ""
echo "=================================================="
echo " Rapports Dive générés"
echo "=================================================="
ls -lh build-reports/dive

echo ""
echo "Résumé : ${SUMMARY_FILE}"
echo ""
echo "=================================================="
echo " Analyse Dive terminée"
echo "=================================================="
