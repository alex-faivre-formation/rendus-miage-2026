#!/usr/bin/env bash
#
# build-images.sh — Approche 1 (Containerfile)
# Construit une image OCI par micro-service MIAGE-Bank avec Buildah.
#
# À placer dans : ci-scripts/build-images.sh
# À lancer depuis la RACINE du projet :  ./ci-scripts/build-images.sh [TAG]
#
set -euo pipefail

TAG="${1:-v1}"
REGISTRY_PREFIX="localhost/miage-bank"
CONTAINERFILE="Partie A/01-image-build/Containerfile"
SERVICES_DIR="src/AMSC"

echo "=== MIAGE-Bank — build des images (tag: ${TAG}) ==="

# 1. Si aucun JAR n'existe, on compile d'abord avec Maven
if [ -z "$(find "$SERVICES_DIR" -path '*/target/*.jar' ! -name '*.original' 2>/dev/null | head -1)" ]; then
  echo ">> Aucun JAR trouvé — compilation Maven (mvn -DskipTests package)..."
  ( cd "$SERVICES_DIR" && mvn -q -DskipTests clean package )
fi

# 2. Découverte automatique des micro-services bootables
#    (on exclut les .original, sources et javadoc, qui ne sont pas exécutables)
mapfile -t JARS < <(find "$SERVICES_DIR" -maxdepth 3 -path '*/target/*.jar' \
  ! -name '*.original' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | sort)

if [ "${#JARS[@]}" -eq 0 ]; then
  echo "!! Aucun JAR exécutable trouvé. Lance 'mvn package' dans src/AMSC d'abord." >&2
  exit 1
fi

echo ">> ${#JARS[@]} micro-service(s) détecté(s)."

# 3. Build d'une image par micro-service via le Containerfile
for jar in "${JARS[@]}"; do
  # nom du module = dossier deux niveaux au-dessus du JAR (ex: amc_clients)
  module=$(basename "$(dirname "$(dirname "$jar")")")
  # on retire le préfixe amc_ pour un nom d'image lisible (ex: clients)
  image="${REGISTRY_PREFIX}-${module#amc_}:${TAG}"

  echo ""
  echo ">> Build de ${image}"
  echo "   JAR : ${jar}"
  buildah bud \
    -f "$CONTAINERFILE" \
    --build-arg JAR_FILE="$jar" \
    -t "$image" \
    .
done

echo ""
echo "=== Images construites ==="
buildah images --filter "reference=${REGISTRY_PREFIX}*"