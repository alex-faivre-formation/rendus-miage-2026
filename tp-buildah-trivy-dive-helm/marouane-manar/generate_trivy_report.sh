#!/bin/bash
set -e

VERSION="1.0.0"

echo "========================================="
echo " Génération des rapports Trivy (Local)"
echo "========================================="

echo "1. Vérification de la présence de Trivy..."
if ! command -v trivy &> /dev/null
then
    echo "Trivy non trouvé, installation locale (dans le dossier) rapide..."
    curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh -s -- -b .
    TRIVY_CMD="./trivy"
else
    TRIVY_CMD="trivy"
fi

IMAGES=("miage-bank-front" "banque-annuaire" "banque-configserver" "banque-clientservice" "banque-compteservice" "banque-compositeservice" "banque-apigateway")

# Création d'un dossier propre pour stocker tous les rapports
mkdir -p rapports_trivy

for IMG in "${IMAGES[@]}"; do
    echo "----------------------------------------"
    echo "Scan de l'image : $IMG"
    echo "----------------------------------------"
    
    # 1. Export local sans démon Docker
    buildah push ${IMG}:${VERSION} docker-archive:${IMG}.tar
    
    # 2. Affichage sur le terminal
    $TRIVY_CMD image --input ${IMG}.tar --severity HIGH,CRITICAL
    
    # 3. Export JSON / SARIF
    $TRIVY_CMD image --input ${IMG}.tar --format json --output rapports_trivy/${IMG}.json --severity HIGH,CRITICAL
    $TRIVY_CMD image --input ${IMG}.tar --format sarif --output rapports_trivy/${IMG}.sarif --severity HIGH,CRITICAL
    
    # 4. Nettoyage de l'archive tar
    rm ${IMG}.tar
done

echo "========================================="
echo "Terminé ! Tous les rapports (JSON/SARIF) ont été générés dans le dossier 'rapports_trivy'."
echo "========================================="
