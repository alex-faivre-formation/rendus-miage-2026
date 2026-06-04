#!/bin/bash
# On force l'arrêt du script si une commande échoue (hors tests Trivy gérés manuellement)
set -e

export JAVA_HOME=$(readlink -f $(which java) | sed "s:/bin/java::")
export PATH=$JAVA_HOME/bin/$PATH
SERVICES=("Banque-Annuaire" "Banque-ConfigServer" "Banque-APIGateway" "Banque-ClientService" "Banque-CompteService" "Banque-CompositeService")
REPORT_DIR="build-reports"
mkdir -p $REPORT_DIR

echo "Nettoyage préventif de l'environnement de build..."
rm -f "$REPORT_DIR"/*.tar
rm -f "$REPORT_DIR"/*.json
rm -f "$REPORT_DIR"/*.sarif

cleanup() {
    local exit_code=$?
    if [ $exit_code -ne 0 ]; then
        echo ""
        echo "Une erreur est survenue (Code: $exit_code). Nettoyage rigoureux en cours..."

        # 1. Supprimer les archives tar
        rm -f "$REPORT_DIR"/*.tar

        # 2. Supprimer TOUTES les images liées au projet
        # On utilise un pattern pour être sûr de ne rien oublier
        for SERVICE in "${SERVICES[@]}"; do
            # On génère le nom d'image exactement comme dans la boucle de build
            IMG_TO_DELETE=$(echo "$SERVICE" | tr '[:upper:]' '[:lower:]')

            echo "Suppression de l'image : $IMG_TO_DELETE:latest"
            # --force permet de supprimer l'image même si elle a des conteneurs (stoppés) associés
            buildah rmi --force "$IMG_TO_DELETE:latest" 2>/dev/null || true
        done
        
        echo "Ménage terminé. Système propre."
    fi
}

# On dit au script d'appeler 'cleanup' en cas d'erreur (ERR) ou d'interruption (SIGINT/SIGTERM)
trap cleanup ERR SIGINT SIGTERM


for SERVICE in "${SERVICES[@]}"; do
    echo "--- Traitement de $SERVICE ---"
    
    # 1. Compilation
    mvn -f "$SERVICE/pom.xml" clean package -DskipTests

    # 2. Construction (Approche 1)
    # On indique à buildah d'utiliser le Containerfile situé DANS le dossier du service
    IMAGE_NAME=$(echo "$SERVICE" | tr '[:upper:]' '[:lower:]')
    
    echo "Construction de l'image $IMAGE_NAME utilisant $SERVICE/Containerfile..."
    # L'argument $SERVICE à la fin définit le "contexte" de build
    buildah bud --no-cache -f "$SERVICE/Containerfile" -t "$IMAGE_NAME:latest" "$SERVICE"

    echo "Service $SERVICE validé."
done
