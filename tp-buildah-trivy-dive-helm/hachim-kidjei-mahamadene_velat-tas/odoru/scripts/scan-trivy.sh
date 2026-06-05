#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-1.0.0}"

mkdir -p build-reports/oci
mkdir -p build-reports/trivy

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

echo "=================================================="
echo " Scan Trivy des images Odoru"
echo " Version : ${VERSION}"
echo "=================================================="

for IMAGE in "${IMAGES[@]}"; do
  IMAGE_REF="localhost/odoru/${IMAGE}:${VERSION}"
  ARCHIVE_PATH="build-reports/oci/${IMAGE}-${VERSION}.tar"
  JSON_REPORT="build-reports/trivy/${IMAGE}-trivy.json"
  SARIF_REPORT="build-reports/trivy/${IMAGE}-trivy.sarif"

  echo ""
  echo "--------------------------------------------------"
  echo "Image   : ${IMAGE_REF}"
  echo "Archive : ${ARCHIVE_PATH}"
  echo "--------------------------------------------------"

  echo ">>> Vérification de l'image locale Buildah"
if ! buildah inspect "${IMAGE_REF}" >/dev/null 2>&1; then
  echo "ERREUR : l'image ${IMAGE_REF} n'existe pas dans Buildah."
  echo "Lance d'abord : ./scripts/build-all.sh ${VERSION}"
  exit 1
fi

  echo ">>> Suppression de l'ancienne archive si elle existe"
  rm -f "${ARCHIVE_PATH}"

  echo ">>> Export de l'image Buildah au format docker-archive"
  buildah push \
    "${IMAGE_REF}" \
    "docker-archive:${ARCHIVE_PATH}:${IMAGE_REF}"

  echo ">>> Scan Trivy HIGH/CRITICAL"
  trivy image \
    --input "${ARCHIVE_PATH}" \
    --severity HIGH,CRITICAL

  echo ">>> Export du rapport JSON"
  trivy image \
    --input "${ARCHIVE_PATH}" \
    --severity HIGH,CRITICAL \
    --format json \
    --output "${JSON_REPORT}"

  echo ">>> Export du rapport SARIF"
  trivy image \
    --input "${ARCHIVE_PATH}" \
    --severity HIGH,CRITICAL \
    --format sarif \
    --output "${SARIF_REPORT}"

  echo ">>> Gate de sécurité CRITICAL"
  if trivy image \
      --input "${ARCHIVE_PATH}" \
      --severity CRITICAL \
      --exit-code 1; then
    echo "OK : aucune vulnérabilité CRITICAL détectée pour ${IMAGE_REF}"
  else
    echo "ATTENTION : des vulnérabilités CRITICAL ont été détectées pour ${IMAGE_REF}"
    echo "Le résultat est conservé dans les rapports Trivy et devra être documenté dans le README."
  fi
done

echo ""
echo "=================================================="
echo " Rapports Trivy générés"
echo "=================================================="
ls -lh build-reports/trivy

echo ""
echo "=================================================="
echo " Scan Trivy terminé"
echo "=================================================="
