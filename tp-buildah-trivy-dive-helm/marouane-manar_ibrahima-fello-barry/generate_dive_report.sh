#!/bin/bash
set -e

VERSION="1.0.0"

echo "========================================="
echo " Génération des analyses Dive (Local)"
echo "========================================="

echo "Vérification de la présence de Dive..."
if ! command -v dive &> /dev/null
then
    echo "Dive non trouvé, téléchargement temporaire dans le répertoire courant..."
    wget -q https://github.com/wagoodman/dive/releases/download/v0.12.0/dive_0.12.0_linux_amd64.tar.gz
    tar -xzf dive_0.12.0_linux_amd64.tar.gz -C .
    DIVE_CMD="./dive"
else
    DIVE_CMD="dive"
fi

IMAGES=("miage-bank-front" "banque-annuaire" "banque-configserver" "banque-clientservice" "banque-compteservice" "banque-compositeservice" "banque-apigateway")


mkdir -p build-reports/dive 

for IMG in "${IMAGES[@]}"; do
    echo "----------------------------------------"
    echo "Audit de l'image : $IMG"
    echo "----------------------------------------"
    
    # Export de l'image pour que dive la lise facilement
    buildah push ${IMG}:${VERSION} docker-archive:${IMG}.tar
    
    # On exécute dive en mode CI (texte brut) et on capture le output
    $DIVE_CMD docker-archive://${IMG}.tar --ci --ci-config .dive-ci > build-reports/dive/${IMG}.txt || true
    
    # On affiche également dans le terminal pour que vous puissiez voir
    cat build-reports/dive/${IMG}.txt | grep -E "Image size|Wasted Space|Efficiency" || true
    
    # Nettoyage
    rm ${IMG}.tar
done

rm -f dive_0.12.0_linux_amd64.tar.gz

echo "========================================="
echo "Terminé ! Les métriques complètes se trouvent dans 'build-reports/dive/'"
echo "========================================="
