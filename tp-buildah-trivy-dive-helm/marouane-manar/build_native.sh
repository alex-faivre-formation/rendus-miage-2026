#!/bin/bash
set -e

VERSION="1.0.0"
echo "Construction layer par layer en mode natif (version ${VERSION}) avec Buildah..."

SERVICES=("Banque-Annuaire" "Banque-ConfigServer" "Banque-ClientService" "Banque-CompteService" "Banque-CompositeService" "Banque-APIGateway")

for SERVICE in "${SERVICES[@]}"; do
    echo "--------------------------"
    echo "Build natif de : $SERVICE"
    echo "--------------------------"

    JAR_PATH="./miage-bank-back/${SERVICE}/target/"
    JAR_FILE=$(ls ${JAR_PATH}/*.jar 2>/dev/null | head -n 1)

    if [ -z "$JAR_FILE" ]; then
        echo "Erreur : Aucun JAR trouvé pour $SERVICE. Veuillez lancer mvn clean package d'abord."
        continue
    fi

    container_id=$(buildah from docker.io/library/eclipse-temurin:17-jre-alpine)
    
    buildah config --workingdir /app $container_id
    buildah copy $container_id "$JAR_FILE" /app/app.jar
    
    buildah config --port 8080 $container_id
    buildah config --cmd "java -jar /app/app.jar" $container_id
    
    buildah commit $container_id "${SERVICE,,}-native:${VERSION}"
    buildah rm $container_id
    
    echo "Image ${SERVICE,,}-native:${VERSION} générée avec succès."
done

echo "Terminé ! Toutes les images ont été construites sans ContainerFile."
