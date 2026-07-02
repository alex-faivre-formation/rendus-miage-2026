#!/usr/bin/env bash
#
# build-native.sh — Approche 2 (Buildah natif, sans Containerfile)
# Construit UNE image "à la main", couche par couche, pour démontrer le mode
# natif de Buildah et comparer avec l'approche Containerfile.
#
# À placer dans : ci-scripts/build-native.sh
# À lancer depuis la RACINE du projet :  ./ci-scripts/build-native.sh
#
set -euo pipefail

# On prend un service en exemple : amc_clients
JAR="$(find src/AMSC/amc_clients -path '*/target/*.jar' \
        ! -name '*.original' ! -name '*-sources.jar' ! -name '*-javadoc.jar' | head -1)"

if [ -z "$JAR" ]; then
  echo "!! JAR de amc_clients introuvable. Lance 'mvn package' d'abord." >&2
  exit 1
fi

echo ">> Construction native depuis : $JAR"

# 1. Conteneur de travail à partir de l'image de base
ctr=$(buildah from eclipse-temurin:17-jre-alpine)

# 2. Création de l'utilisateur non-root (équivalent du RUN du Containerfile)
buildah run "$ctr" -- addgroup -S app
buildah run "$ctr" -- adduser -S -G app app

# 3. Copie du JAR
buildah copy --chown app:app "$ctr" "$JAR" /app/app.jar

# 4. Configuration de l'image (équivalent des WORKDIR/USER/EXPOSE/ENTRYPOINT)
buildah config \
  --workingdir /app \
  --user app \
  --port 8080 \
  --label org.opencontainers.image.title="miage-bank-clients" \
  --entrypoint '["java","-jar","/app/app.jar"]' \
  "$ctr"

# 5. Commit en image OCI
buildah commit "$ctr" localhost/miage-bank-clients-native:v1

# 6. Nettoyage du conteneur de travail
buildah rm "$ctr"

echo ">> Image native créée : localhost/miage-bank-clients-native:v1"
buildah images --filter "reference=localhost/miage-bank-clients-native*"