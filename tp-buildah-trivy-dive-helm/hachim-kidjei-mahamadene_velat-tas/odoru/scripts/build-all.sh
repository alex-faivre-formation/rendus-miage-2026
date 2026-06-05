#!/usr/bin/env bash
set -euo pipefail

VERSION="${1:-1.0.0}"

echo "=================================================="
echo " Build des images Odoru avec Buildah"
echo " Version : ${VERSION}"
echo "=================================================="

build_image() {
  local image_name="$1"
  local containerfile_path="$2"

  echo ""
  echo "--------------------------------------------------"
  echo "Build : ${image_name}:${VERSION}"
  echo "Containerfile : ${containerfile_path}"
  echo "--------------------------------------------------"

  buildah bud \
    -f "${containerfile_path}" \
    -t "${image_name}:${VERSION}" \
    .
}

build_image "odoru/config-server" "services/config-server/Containerfile"
build_image "odoru/discovery-service" "services/discovery-service/Containerfile"
build_image "odoru/member-service" "services/member-service/Containerfile"
build_image "odoru/course-service" "services/course-service/Containerfile"
build_image "odoru/competition-service" "services/competition-service/Containerfile"
build_image "odoru/badge-service" "services/badge-service/Containerfile"
build_image "odoru/statistics-service" "services/statistics-service/Containerfile"
build_image "odoru/api-gateway" "services/api-gateway/Containerfile"
build_image "odoru/front" "odoru-front/Containerfile"

echo ""
echo "=================================================="
echo " Images Odoru construites"
echo "=================================================="
buildah images | grep "odoru/" || true
