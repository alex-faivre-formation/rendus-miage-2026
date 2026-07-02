#!/bin/bash
# On force l'arrêt du script si une commande échoue
set -e

# Définition du réseau et des dossiers de configuration
NETWORK_NAME="banque-net"
CONFIG_DIR="$(pwd)/Banque-configs"
PROMETHEUS_DIR="$(pwd)/config-prometheus"

status_services() {
    echo "========================================================="
    echo "📊 ÉTAT DES CONTENEURS BANQUE"
    echo "========================================================="
    podman ps --filter network=$NETWORK_NAME --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
}

stop_services() {
    echo "========================================================="
    echo "🛑 ARRÊT ET NETTOYAGE DES CONTENEURS"
    echo "========================================================="

    CONTAINERS=("banque-compositeservice" "banque-compteservice" "banque-clientservice" \
                 "banque-apigateway" "banque-configserver" "banque-annuaire" \
                 "banque-zipkin" "prometheus" "mongo-db" "mysql-db")

    for CTR in "${CONTAINERS[@]}"; do
        if podman ps -a --format "{{.Names}}" | grep -q "^$CTR$"; then
            echo "Arrêt et suppression de : $CTR"
            podman rm -f "$CTR" 2>/dev/null || true
        fi
    done
    echo "✅ Tout est propre !"
}

start_services() {
    echo "========================================================="
    echo "🌐 1. CRÉATION DU RÉSEAU"
    echo "========================================================="
    podman network create $NETWORK_NAME 2>/dev/null || true
    echo "✅ Réseau '$NETWORK_NAME' prêt."

    echo "========================================================="
    echo "🗄️  2. LANCEMENT DES BASES DE DONNÉES & OUTILS"
    echo "========================================================="
    # Base MySQL pour le ClientService
    echo "💾 Lancement de MySQL (mysql-db)..."
    podman run -d --name mysql-db \
        --network $NETWORK_NAME \
        -e MYSQL_ROOT_PASSWORD=root \
        -e MYSQL_DATABASE=banquebd \
        -p 3306:3306 \
        docker.io/library/mysql:oracle

    # Base MongoDB pour le CompteService
    echo "🍃 Lancement de MongoDB (mongo-db)..."
    podman run -d --name mongo-db \
        --network $NETWORK_NAME \
        -e MONGO_INITDB_ROOT_USERNAME=root \
        -e MONGO_INITDB_ROOT_PASSWORD=root \
        -e MONGO_INITDB_DATABASE=banquebd \
        -p 27017:27017 \
        docker.io/library/mongo:latest

    # Serveur Zipkin pour le traçage distribué
        echo "🔎 Lancement de Zipkin (banque-zipkin)..."
        podman run -d --name banque-zipkin \
            --network $NETWORK_NAME \
            --network-alias bnkzipkin \
            -p 9411:9411 \
            docker.io/openzipkin/zipkin:latest

    echo "========================================================="
    echo "🏗️  3. LANCEMENT DE L'INFRASTRUCTURE SPRING CLOUD"
    echo "========================================================="
    # L'Annuaire (Eureka) doit démarrer en premier
    echo "📁 Lancement de l'Annuaire Eureka (banque-annuaire)..."
    podman run -d --name banque-annuaire \
        --network $NETWORK_NAME \
        -p 10001:10001 \
        banque-annuaire:latest

    # Le Serveur de Config lit les fichiers .yml locaux de Banque-configs
    echo "⚙️  Lancement du Config Server (banque-configserver)..."
    podman run -d --name banque-configserver \
        --network $NETWORK_NAME \
        -p 10003:10003 \
        -v "$CONFIG_DIR:/configs:Z" \
        -e SPRING_PROFILES_ACTIVE=native \
        -e SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCH_LOCATIONS=file:///configs \
        -e WAIT_HOSTS=banque-annuaire:10001 \
        banque-configserver:latest

    echo "⏳ Attente de 15 secondes pour stabiliser l'infrastructure (Eureka & Config)..."
    sleep 15

    echo "========================================================="
    echo "🚀 4. LANCEMENT DES MICROSERVICES MÉTIERS & GATEWAY"
    echo "========================================================="
    # Variables d'environnement partagées pour l'aiguillage interne au réseau
    ENV_INFRA="-e SPRING_CLOUD_CONFIG_URI=http://banque-configserver:10003 -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://banque-annuaire:10001/eureka/"

    # Service Client (Lien MySQL)
    echo "👤 Lancement du ClientService..."
    podman run -d --name banque-clientservice \
        --network $NETWORK_NAME \
        $ENV_INFRA \
        -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql-db:3306/banquebd \
        -e SPRING_DATASOURCE_USERNAME=root \
        -e SPRING_DATASOURCE_PASSWORD=root \
        -e WAIT_HOSTS=banque-configserver:10003,mysql-db:3306 \
        banque-clientservice:latest

    # Service Compte (Lien MongoDB)
    echo "💳 Lancement du CompteService..."
    podman run -d --name banque-compteservice \
        --network $NETWORK_NAME \
        $ENV_INFRA \
        -e SPRING_DATA_MONGODB_URI=mongodb://mongo-db:27017/banquebd \
        -e WAIT_HOSTS=banque-configserver:10003,mongo-db:27017 \
        banque-compteservice:latest

    # Service Composite (Orchestrateur)
    echo "🔄 Lancement du CompositeService..."
    podman run -d --name banque-compositeservice \
        --network $NETWORK_NAME \
        $ENV_INFRA \
        -e WAIT_HOSTS=banque-configserver:10003,banque-clientservice:8080,banque-compteservice:8080 \
        banque-compositeservice:latest

    # API Gateway (Point d'entrée de l'architecture)
    echo "🚪 Lancement de l'API Gateway (banque-apigateway)..."
    podman run -d --name banque-apigateway \
        --network $NETWORK_NAME \
        -p 10000:10000 \
        $ENV_INFRA \
        -e WAIT_HOSTS=banque-configserver:10003,banque-compositeservice:8080 \
        banque-apigateway:latest

    echo "========================================================="
    echo "📊 5. LANCEMENT DE LA SUPERVISION (PROMETHEUS)"
    echo "========================================================="
    # Conteneur Prometheus officiel alimenté par ton dossier config-prometheus
    if [ -d "$PROMETHEUS_DIR" ]; then
        echo "📈 Lancement de Prometheus..."
        podman run -d --name prometheus \
            --network $NETWORK_NAME \
            -p 9090:9090 \
            -v "$PROMETHEUS_DIR/prometheus.yml:/etc/prometheus/prometheus.yml:Z" \
            docker.io/prom/prometheus:latest
    else
        echo "⏩ Pas de dossier config-prometheus trouvé à la racine, étape sautée."
    fi

    echo ""
    echo "🎉 Architecture lancée avec succès !"
    status_services
}

# Gestion des arguments du script
case "$1" in
    start)
        start_services
        ;;
    stop)
        stop_services
        ;;
    status)
        status_services
        ;;
    *)
        echo "Usage: $0 {start|stop|status}"
        exit 1
        ;;
esac
