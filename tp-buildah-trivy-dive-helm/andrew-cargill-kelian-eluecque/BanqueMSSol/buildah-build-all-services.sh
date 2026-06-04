#!/bin/bash
set -e

# Définition de la fonction générique de construction native
build_native_image() {
    local SERVICE_DIR=$1
    local IMAGE_NAME=$(echo "$SERVICE_DIR" | tr '[:upper:]' '[:lower:]')

    # 1. Le dictionnaire est maintenant encapsulé dans la fonction
    local -A SERVICE_PORTS=(
        ["Banque-Annuaire"]="10001"
        ["Banque-ConfigServer"]="10003"
        ["Banque-APIGateway"]="10000"
        ["Banque-ClientService"]="8080"
        ["Banque-CompteService"]="10021"
        ["Banque-CompositeService"]="10031"
    )

    # On récupère le port correspondant au service en cours
    local PORT=${SERVICE_PORTS[$SERVICE_DIR]}

    echo "========================================================"
    echo " Construction native Buildah pour : $SERVICE_DIR"
    echo " Image cible : $IMAGE_NAME:buildah-natif"
    echo "========================================================"

    # 2. Instanciation du conteneur de travail
    local CONTAINER=$(buildah from docker.io/library/eclipse-temurin:17-jre)
    buildah config --workingdir /app $CONTAINER
    buildah config --user root $CONTAINER

    # 3. Sécurité OS et utilisateur
    buildah run $CONTAINER sh -c "apt-get update && apt-get upgrade -y && apt-get clean && rm -rf /var/lib/apt/lists/*"
    buildah run $CONTAINER sh -c "groupadd -r miage && useradd -r -g miage miage"

    # 4. Identification et copie du JAR
    local JAR_FILE=$(ls $SERVICE_DIR/target/*.jar | grep -v "\.original$" | head -n 1)
    if [ -z "$JAR_FILE" ]; then
        echo "ERREUR : Aucun fichier JAR trouvé dans $SERVICE_DIR/target/. Avez-vous compilé avec Maven ?"
        exit 1
    fi
    buildah copy $CONTAINER "$JAR_FILE" /app/app.jar

    # 5. Droits et bascule utilisateur
    buildah run $CONTAINER chown miage:miage /app/app.jar
    buildah config --user miage $CONTAINER

    # 6. Configuration dynamique du port
    if [ -n "$PORT" ]; then
        echo " -> Configuration du port exposé : $PORT"
        buildah config --port $PORT $CONTAINER
    else
        echo " -> AVERTISSEMENT : Aucun port défini pour $SERVICE_DIR dans le dictionnaire !"
    fi

    # 7. Point d'entrée et Création de l'image
    buildah config --entrypoint '["java", "-jar", "app.jar"]' $CONTAINER

    echo " -> Génération de l'image OCI finale..."
    buildah commit $CONTAINER "$IMAGE_NAME:buildah-natif"

    # 8. Nettoyage obligatoire
    buildah rm $CONTAINER
    echo " -> Terminé pour $SERVICE_DIR !"
    echo ""
}

# --- Programme Principal ---

# Liste exacte de vos micro-services
SERVICES=(
    "Banque-Annuaire"
    "Banque-ConfigServer"
    "Banque-APIGateway"
    "Banque-ClientService"
    "Banque-CompteService"
    "Banque-CompositeService"
)

echo "Démarrage du pipeline de build natif..."

for SERVICE in "${SERVICES[@]}"; do
    if [ -d "$SERVICE" ]; then
        build_native_image "$SERVICE"
    else
        echo "AVERTISSEMENT : Le dossier $SERVICE n'existe pas, il est ignoré."
    fi
done

echo "Toutes les images ont été construites avec succès en mode natif !"
