#!/usr/bin/env bash
# =============================================================================
#  build-buildah.sh — Build avec Containerfile
# -----------------------------------------------------------------------------
#  Mode Buildah avec Containerfile
#
#  Exemple d'usage :
#     scripts/build-buildah.sh                              ---> construction de toutes les images
#     scripts/build-buildah.sh src/Banque-CompteService     ---> construction de l'image en paramètre
# =============================================================================

# le script s'arrête si une commande échoue
set -euo pipefail

# déplacement à la racine du projet
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

# création dossier images s'il n'exiqte pas
mkdir -p images/containerfile-version

# associe un port à chaque microservice
declare -A SERVICES=(
  [Banque-Annuaire]=10001
  [Banque-ConfigServer]=10003
  [Banque-ClientService]=10011
  [Banque-CompteService]=10021
  [Banque-CompositeService]=10031
  [Banque-APIGateway]=10000
  [Banque-Frontend]=80
)

# si on lance le script sans argument -> construction de toutes les images
# sinon on construit l'image du service passé en paramètre
TARGETS=("$@")
if [[ ${#TARGETS[@]} -eq 0 ]]; then
  TARGETS=("${!SERVICES[@]}")
fi

for MODULE in "${TARGETS[@]}"; do
  PORT="${SERVICES[$MODULE]:?Service inconnu : $MODULE}"

  # formatage du nom
  NAME="$(echo "${MODULE#Banque-}" | tr '[:upper:]' '[:lower:]')"
  TAG="localhost/miage-bank/${NAME}:7.0"

  # choix du Containerfile (microservice avec springboot ou frontend avec angular)
  DOCKERFILE="src/Containerfile"
  if [[ "$MODULE" == "Banque-Frontend" ]]; then
    DOCKERFILE="src/Banque-Frontend/Containerfile"
  fi

  # construction de l'image
  echo "==> Build de ${MODULE} -> ${TAG} (port ${PORT} avec ${DOCKERFILE})"
  buildah bud \
    --build-arg MODULE="${MODULE}" \
    --build-arg APP_PORT="${PORT}" \
    --layers \
    -f "${DOCKERFILE}" \
    -t "${TAG}" \
    .

  # exportation de l'image construite au format .tar
  echo "==> Exportation de l'image dans images/containerfile-version/${NAME}.tar"
  rm -f "images/containerfile-version/${NAME}.tar"
  buildah push "${TAG}" "docker-archive:images/containerfile-version/${NAME}.tar"
done

echo
echo "==> Images construites :"
buildah images --filter 'reference=localhost/miage-bank/*'