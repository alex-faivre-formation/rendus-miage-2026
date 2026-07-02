#!/usr/bin/env bash
# =============================================================================
#  build-buildah-native.sh — Build layer par layer sans Containerfile
# -----------------------------------------------------------------------------
#  Mode natif Buildah (from / run / copy / config / commit).
#  On reproduit le résultat du Containerfile
#
#  Exemple d'usage :
#     scripts/build-buildah-native.sh                        ---> boucle sur tous les backend
#     scripts/build-buildah-native.sh Banque-APIGateway      ---> construction d'un service précis
#
#  /!\ non utilisable pour la création de l'image Frontend
# =============================================================================

# le script s'arrête si une commande échoue
set -euo pipefail

# définition des images de base Maven/JRE
MAVEN_IMG="docker.io/library/maven:3.9-eclipse-temurin-11"
RUNTIME_IMG="docker.io/library/eclipse-temurin:11-jre-jammy"

# déplacement à la racine du projet
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

# création du dossier d'exportation pour les images natives
mkdir -p images/native

# dossier temporaire (supprimé proprement même si le script plante)
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# associe un port à chaque microservice (on garde le Frontend pour l'ignorer proprement)
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
TARGETS=("$@")
if [[ ${#TARGETS[@]} -eq 0 ]]; then
  TARGETS=("${!SERVICES[@]}")
fi

for RAW_MODULE in "${TARGETS[@]}"; do
  # Nettoyage si l'utilisateur tape "src/Banque-..." au lieu de "Banque-..."
  MODULE="${RAW_MODULE#src/}"

  # on ignore le Frontend car il utilise Node/Nginx, pas Maven/Java
  if [[ "$MODULE" == "Banque-Frontend" ]]; then
    echo "==> Ignoré : $MODULE (Le frontend n'utilise pas le build natif Java)"
    continue
  fi

  PORT="${SERVICES[$MODULE]:?Service inconnu : $MODULE}"
  NAME="$(echo "${MODULE#Banque-}" | tr '[:upper:]' '[:lower:]')"
  TAG="localhost/miage-bank/${NAME}:7.0-native"

  echo "============================================================================="
  echo "==> Démarrage du build natif : ${MODULE} -> ${TAG} (Port: ${PORT})"
  echo "============================================================================="

  # Nettoyage du dossier temporaire pour éviter les conflits entre deux boucles
  rm -rf "${WORK:?}"/*

  # compilation Maven
  # -----------------------------------------------------------------------------
  echo "==> [1/5] Compilation Maven de ${MODULE} dans un conteneur jetable"
  # -----------------------------------------------------------------------------
  bld="$(buildah from "$MAVEN_IMG")"
  buildah run "$bld" -- mkdir -p /workspace/extracted

  buildah copy "$bld" "src/${MODULE}/pom.xml" /workspace/pom.xml
  buildah copy "$bld" "src/${MODULE}/src"     /workspace/src

  buildah run --workingdir /workspace "$bld" -- mvn -B -q -DskipTests clean package
  buildah run --workingdir /workspace "$bld" -- \
      bash -c 'cp target/*.jar /workspace/application.jar'
  buildah run --workingdir /workspace/extracted "$bld" -- \
      java -Djarmode=layertools -jar /workspace/application.jar extract

  # récupération des artefacts
  # -----------------------------------------------------------------------------
  echo "==> [2/5] Extraction des couches du builder vers l'hôte"
  # -----------------------------------------------------------------------------
  buildah run "$bld" -- tar -C /workspace/extracted -cf - . | tar -C "$WORK" -xf -
  buildah rm "$bld" >/dev/null

  # runtime / conteneur final
  # -----------------------------------------------------------------------------
  echo "==> [3/5] Assemblage natif de l'image runtime"
  # -----------------------------------------------------------------------------
  ctr="$(buildah from "$RUNTIME_IMG")"

  # création utilisateur non-root
  buildah run "$ctr" -- groupadd --system --gid 1001 spring
  buildah run "$ctr" -- useradd  --system --uid 1001 --gid spring --no-create-home spring

  # copie des 4 couches
  buildah copy --chown spring:spring "$ctr" "$WORK/dependencies"          /app
  buildah copy --chown spring:spring "$ctr" "$WORK/spring-boot-loader"    /app
  buildah copy --chown spring:spring "$ctr" "$WORK/snapshot-dependencies" /app
  buildah copy --chown spring:spring "$ctr" "$WORK/application"           /app

  # configuration des métadonnées
  buildah config \
    --workingdir /app \
    --user spring:spring \
    --port "${PORT}" \
    --env "APP_PORT=${PORT}" \
    --env "JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0" \
    --label "org.opencontainers.image.title=miage-bank-${NAME}" \
    --label "org.opencontainers.image.version=7.0-native" \
    --entrypoint '["java","org.springframework.boot.loader.JarLauncher"]' \
    "$ctr"

  # sauvegarde de l'image
  # -----------------------------------------------------------------------------
  echo "==> [4/5] Commit de l'image locale"
  # -----------------------------------------------------------------------------
  buildah commit --rm "$ctr" "$TAG"

  # -----------------------------------------------------------------------------
  echo "==> [5/5] Exportation .tar vers images/native/"
  # -----------------------------------------------------------------------------
  rm -f "images/native-version/${NAME}.tar"
  buildah push "${TAG}" "docker-archive:images/native-version/${NAME}.tar"
  echo "OK : ${MODULE} exporté avec succès."
  echo ""

done

echo "============================================================================="
echo "==> Bilan des images natives générées :"
ls -lh images/native/*.tar