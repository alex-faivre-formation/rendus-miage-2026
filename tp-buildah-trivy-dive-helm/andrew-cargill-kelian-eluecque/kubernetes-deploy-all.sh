#!/bin/bash

# ==============================================================================
# SCRIPT DE DÉPLOIEMENT AUTOMATISÉ "ONE-CLICK" - MIAGE BANK
# ==============================================================================

# --- CHARGEMENT DES SECRETS DEPUIS LE FICHIER EXTERNE ---
SECRETS_FILE="vault.env"

if [ -f "$SECRETS_FILE" ]; then
    echo -e "Chargement des secrets depuis $SECRETS_FILE..."
    # La commande 'source' importe les variables du fichier texte dans le script
    source "$SECRETS_FILE"
else
    echo -e "\033[0;31m ERREUR FATALE : Le fichier $SECRETS_FILE est introuvable !\033[0m"
    echo -e "Veuillez créer un fichier $SECRETS_FILE contenant VAULT_TOKEN et les UNSEAL_KEY_X."
    exit 1
fi

# Vérification rapide pour s'assurer que le fichier n'était pas vide
if [ -z "$VAULT_TOKEN" ]; then
    echo -e "\033[0;31m ERREUR : VAULT_TOKEN n'a pas été trouvé dans $SECRETS_FILE.\033[0m"
    exit 1
fi

NAMESPACE="miage-bank"
HELM_RELEASE="miage-bank-release"
HELM_CHART_DIR="./miage-bank"
DATABASE_YAML="database.yaml"

# Couleurs pour l'affichage
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # Pas de couleur

echo -e "${BLUE}=== OPERATION KAWABONGA : DÉMARRAGE DU BOOTSTRAP TOTAL ===${NC}\n"

# ------------------------------------------------------------------------------
# ÉTAPE 1 : Nettoyage complet de l'ancien environnement
# ------------------------------------------------------------------------------
echo -e "${YELLOW} Étape 1 : Destruction du namespace existant [${NAMESPACE}]...${NC}"
kubectl delete namespace ${NAMESPACE} --ignore-not-found=true

echo -e "Attente de la purge complète par Kubernetes..."
while kubectl get namespace ${NAMESPACE} &>/dev/null; do
    sleep 2
done
echo -e "${GREEN} Ancien namespace nettoyé.${NC}\n"

# ------------------------------------------------------------------------------
# ÉTAPE 2 : Recréation des fondations
# ------------------------------------------------------------------------------
echo -e "${YELLOW} Étape 2 : Création du namespace tout neuf...${NC}"
kubectl create namespace ${NAMESPACE}

echo -e "${YELLOW} Étape 3 : Déploiement des bases de données (MySQL et MongoDB)...${NC}"
if [ -f "${DATABASE_YAML}" ]; then
    kubectl apply -f ${DATABASE_YAML} -n ${NAMESPACE}
else
    echo -e "${RED} Erreur : Fichier ${DATABASE_YAML} introuvable !${NC}"
    exit 1
fi
echo -e "${GREEN} Ressources de base de données appliquées.${NC}\n"

# ------------------------------------------------------------------------------
# ÉTAPE 3 : Réanimation et Configuration Secrète de Vault
# ------------------------------------------------------------------------------
echo -e "${YELLOW} Étape 4 : Réanimation de Vault dans le namespace 'default'...${NC}"
echo -e " Attente du démarrage complet du pod Vault..."
kubectl wait --for=condition=Ready pod/vault-0 -n default --timeout=90s

echo -e " Déverrouillage des portes blindées (Unseal)..."
kubectl exec -n default vault-0 -- vault operator unseal ${UNSEAL_KEY_1}
kubectl exec -n default vault-0 -- vault operator unseal ${UNSEAL_KEY_2}
kubectl exec -n default vault-0 -- vault operator unseal ${UNSEAL_KEY_3}

echo -e "Connexion en tant qu'administrateur Root..."
kubectl exec -n default vault-0 -- vault login ${VAULT_TOKEN}

echo -e "Mise à jour du registre Kubernetes dans Vault (Résolution du bug 403)..."
kubectl exec -n default vault-0 -- sh -c 'vault write auth/kubernetes/config kubernetes_host="https://$KUBERNETES_SERVICE_HOST:443"'
echo -e "${GREEN}✅ Vault est déverrouillé, configuré et prêt à valider les badges.${NC}\n"

# ------------------------------------------------------------------------------
# ÉTAPE 4 : Grand Final - Déploiement applicatif avec Helm
# ------------------------------------------------------------------------------
echo -e "${YELLOW} Étape 5 : Lancement du déploiement applicatif global via Helm...${NC}"
if [ -d "${HELM_CHART_DIR}" ]; then
    helm upgrade --install ${HELM_RELEASE} ${HELM_CHART_DIR} -n ${NAMESPACE}
else
    echo -e "${RED} Erreur : Répertoire Helm Chart ${HELM_CHART_DIR} introuvable !${NC}"
    exit 1
fi
echo -e "${GREEN} Helm Chart déployé avec succès.${NC}\n"

# ------------------------------------------------------------------------------
# ÉTAPE 5 : Surveillance de l'effet domino
# ------------------------------------------------------------------------------
echo -e "${BLUE}=== SUIVI DE L'ALLUMAGE DE L'INFRASTRUCTURE ===${NC}"
echo -e "${YELLOW}Le pont SecretStore va se valider, libérer les secrets et réveiller les pods.${NC}"
echo -e "Appuyez sur Ctrl+C pour quitter la surveillance des pods.\n"

sleep 3
kubectl get pods -n ${NAMESPACE} -w