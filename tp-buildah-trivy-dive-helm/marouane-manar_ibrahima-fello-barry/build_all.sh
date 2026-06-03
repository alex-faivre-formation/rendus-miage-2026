#!/bin/bash
set -e

VERSION="1.0.0"

# ==========================================
# 1. Compilation des sources Java avec Maven
# ==========================================
echo "Étape 1: Compilation du backend (miage-bank-back) via Maven..."
cd miage-bank-back
mvn clean package -DskipTests # compile le projet et crée un fichier .jar pour chaque micro-service
cd ..

# ==========================================
# 2. Construction du Frontend
# ==========================================
echo "Etape 2: Construction du Front-end (miage-bank-front)..."
buildah bud -t miage-bank-front:${VERSION} ./miage-bank-front # buildah bud est l'équivalent de docker build

# ==========================================
# 3. Construction des micro-services (Containerfile)
# ==========================================
echo "Etape 3: Construction des micro-services Backend..."

SERVICES=("Banque-Annuaire" "Banque-ConfigServer" "Banque-ClientService" "Banque-CompteService" "Banque-CompositeService" "Banque-APIGateway") # le nom de tous les micro-services

# parcours de chaque micro-service
for SERVICE in "${SERVICES[@]}"; do
    echo "--------------------------"
    echo "Build de : $SERVICE"
    echo "--------------------------"
    
    # On se place dans le dossier du micro-service pour que la commande COPY "target/*.jar" fonctionne en local
    cd "miage-bank-back/$SERVICE"
    
    # Buildah utilise le ContainerFile et on cible le dossier du micro-service avec le bon tag
    buildah bud -f ../../ContainerFile -t "${SERVICE,,}:${VERSION}" .
    
    cd ../..
done

echo "Succès ! Tout le TP MIAGE-Bank (Backend + Frontend) est compilé."
