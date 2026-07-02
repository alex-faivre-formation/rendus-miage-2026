#!/bin/bash
# ============================================================
# start.sh — MIAGE-Bank : déploiement complet de A à Z
# RAJAOBELISON Hanja & ALPTEKIN Eylul — Master MIAGE M2 ITN
# ============================================================

set -e
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

info()    { echo -e "${GREEN}[INFO]${NC} $1"; }
warning() { echo -e "${YELLOW}[WARN]${NC} $1"; }
error()   { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

echo "============================================================"
echo " MIAGE-Bank — Déploiement complet"
echo " RAJAOBELISON Hanja & ALPTEKIN Eylul — Master MIAGE M2 ITN"
echo "============================================================"

# ── PARTIE A ─────────────────────────────────────────────────
echo ""
echo "━━━ PARTIE A — Chaîne de build OCI ━━━━━━━━━━━━━━━━━━━━━"

info "Vérification des prérequis Partie A..."
command -v buildah  >/dev/null 2>&1 || error "buildah non installé → https://buildah.io/"
command -v trivy    >/dev/null 2>&1 || error "trivy non installé → https://aquasecurity.github.io/trivy/"
command -v dive     >/dev/null 2>&1 || error "dive non installé → https://github.com/wagoodman/dive"
command -v hadolint >/dev/null 2>&1 || error "hadolint non installé → https://github.com/hadolint/hadolint"
info "Prérequis Partie A OK"

chmod +x ci-scripts/*.sh

# A.1 — Build images
info "A.1 — Build des images avec Buildah..."
./ci-scripts/build-images.sh v1
info "Images buildées"

# A.2 — Scan Trivy
info "A.2 — Scan de sécurité Trivy..."
./ci-scripts/scan-trivy.sh v1 || warning "CVE CRITICAL détectées — gate documentée, comportement attendu"
info "Rapports Trivy → Partie A/02-security-scan/reports/"

# A.3 — Audit Dive
info "A.3 — Audit layers Dive..."
./ci-scripts/audit-dive.sh v1
info "Rapports Dive → Partie A/03-image-audit/"

# A.4 — Hadolint
info "A.4 — Lint Containerfile avec Hadolint..."
hadolint "Partie A/01-image-build/Containerfile"
info "Hadolint : 0 erreur"

# ── PARTIE B ─────────────────────────────────────────────────
echo ""
echo "━━━ PARTIE B — Déploiement Kubernetes ━━━━━━━━━━━━━━━━━━"

info "Vérification des prérequis Partie B..."
command -v minikube >/dev/null 2>&1 || error "minikube non installé → https://minikube.sigs.k8s.io/docs/start/"
command -v kubectl  >/dev/null 2>&1 || error "kubectl non installé → https://kubernetes.io/docs/tasks/tools/"
command -v helm     >/dev/null 2>&1 || error "helm non installé → https://helm.sh/docs/intro/install/"
info "Prérequis Partie B OK"

# B.1 — Minikube
info "B.1 — Démarrage Minikube..."
minikube start
minikube addons enable metrics-server
info "Minikube démarré"

# B.2 — Traefik
info "B.2 — Installation Traefik..."
helm repo add traefik https://helm.traefik.io/traefik 2>/dev/null || true
helm repo update
helm upgrade --install traefik traefik/traefik -n traefik --create-namespace
info "Traefik installé "

# B.3 — ArgoCD
info "B.3 — Installation ArgoCD..."
helm repo add argo https://argoproj.github.io/argo-helm 2>/dev/null || true
helm repo update
helm upgrade --install argocd argo/argo-cd -n argocd --create-namespace
info "ArgoCD installé "

# B.4 — Vault
info "B.4 — Installation Vault..."
helm repo add hashicorp https://helm.releases.hashicorp.com 2>/dev/null || true
helm repo update
helm upgrade --install vault hashicorp/vault \
  -n vault --create-namespace \
  --set server.dev.enabled=true \
  --set server.dev.devRootToken=root
kubectl wait --for=condition=Ready pod \
  -l app.kubernetes.io/name=vault \
  -n vault --timeout=120s
info "Vault installé"

# B.5 — ESO
info "B.5 — Installation External Secrets Operator..."
helm repo add external-secrets https://charts.external-secrets.io 2>/dev/null || true
helm repo update
helm upgrade --install external-secrets \
  external-secrets/external-secrets \
  -n external-secrets --create-namespace
info "ESO installé "

# B.6 — Secrets Vault
info "B.6 — Configuration secrets Vault..."
kubectl create namespace miage-bank --dry-run=client -o yaml | kubectl apply -f -
kubectl exec -n vault vault-0 -- \
  env VAULT_TOKEN=root \
  vault kv put secret/miage-bank/db \
  mysql-password=root mongo-password=root
kubectl create secret generic vault-token \
  --from-literal=token=root \
  -n miage-bank --dry-run=client -o yaml | kubectl apply -f -
info "Secrets Vault configurés "

# B.7 — Helm deploy
info "B.7 — Déploiement chart Helm MIAGE-Bank..."
cd "Partie B"
helm lint miage-bank/
helm upgrade --install miage-bank miage-bank/ \
  -n miage-bank --create-namespace \
  --set secrets.mysqlRootPassword=root \
  --set secrets.mongoRootPassword=root
cd ..
info "Chart Helm déployé "

# B.8 — ArgoCD app
info "B.8 — Déploiement application ArgoCD..."
kubectl apply -f "Partie B/miage-bank/01-infrastructure/namespace.yaml"
kubectl apply -f "Partie B/miage-bank/01-infrastructure/cluster-secret-store.yaml"
kubectl apply -f "Partie B/miage-bank/01-infrastructure/external-secret.yaml"
kubectl apply -f "Partie B/miage-bank/01-infrastructure/argocd-app.yaml"
info "ArgoCD app déployée "

# ── RÉSUMÉ FINAL ──────────────────────────────────────────────
echo ""
echo "━━━ VÉRIFICATION FINALE ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "--- Pods ---"
kubectl get pods -n miage-bank
echo ""
echo "--- NetworkPolicies ---"
kubectl get networkpolicies -n miage-bank
echo ""
echo "--- HPA ---"
kubectl get hpa -n miage-bank
echo ""
echo "--- External Secrets ---"
kubectl get externalsecret -n miage-bank 2>/dev/null || true
echo ""
echo "============================================================"
echo " MIAGE-Bank déployé avec succès !"
echo ""
echo " Rapports Trivy  : Partie A/02-security-scan/reports/"
echo " Rapports Dive   : Partie A/03-image-audit/"
echo " CI GitHub       : https://github.com/hanja19/miage-bank-projet/actions"
echo "============================================================"
