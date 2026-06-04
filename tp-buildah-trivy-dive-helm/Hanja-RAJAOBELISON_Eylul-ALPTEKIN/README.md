# RAJAOBELISON Hanja & ALPTEKIN Eylul : MIAGE-Bank (Master MIAGE M2 ITN)

**Cours :** Cloud & Kubernetes — Session 2026  
**TP :** Buildah, Trivy, Dive & Helm / Kubernetes

[![CI Status](https://github.com/hanja19/miage-bank-projet/actions/workflows/ci-pipeline.yml/badge.svg)](https://github.com/hanja19/miage-bank-projet/actions/workflows/ci-pipeline.yml)

---

## Objectif du projet

Ce dépôt regroupe l'ensemble des travaux de conteneurisation, de sécurisation et
de déploiement de l'application **MIAGE-Bank**, une architecture micro-services
Spring Boot (Spring Cloud) pour une banque simplifiée.

- **Partie A**: Chaîne de build OCI : construction des images avec **Buildah**,
  scan de sécurité avec **Trivy**, audit des layers avec **Dive**, lint du
  Containerfile avec **Hadolint**, et automatisation via **GitHub Actions**.
- **Partie B** :Packaging **Helm**, déploiement **Kubernetes** (Minikube), gestion
  des secrets (Vault + External Secrets Operator), exposition via **Traefik** et
  GitOps avec **ArgoCD**.

Application source : <https://github.com/hialmar/AMSC>

---

## Architecture de l'application

| Micro-service | Rôle | Port | Stockage |
|---|---|---|---|
| `amc_annuaire` | Découverte de services (Eureka) | 10001 | — |
| `amc_configserver` | Serveur de configuration | 10003 | — |
| `amc_clients` | Gestion des profils clients | 10011 | MySQL |
| `amc_comptes` | Gestion des comptes bancaires | 10021 | MongoDB |
| `amc_composite` | Service composite | 10031 | — |
| `amc_proxy` | API Gateway | 10000 | — |

---

## Structure du dépôt

```
MIAGE-BANK-PROJET/
├── README.md                  ← ce fichier
├── start.sh                   ← script tout-en-un (Partie A + B)
├── ci-scripts/                ← scripts de la chaîne de build
│   ├── build-images.sh        ← build Buildah
│   ├── build-native.sh        ← build Buildah natif
│   ├── scan-trivy.sh          ← scan Trivy
│   └── audit-dive.sh          ← audit Dive
├── Partie A/
│   ├── 01-image-build/        ← Containerfile + analyse Docker/Buildah
│   ├── 02-security-scan/      ← rapports Trivy (JSON/SARIF)
│   ├── 03-image-audit/        ← audit Dive + optimisations
│   └── 04-linter-check/       ← lint Hadolint
├── Partie B/
│   └── miage-bank/            ← chart Helm complet
│       ├── 01-infrastructure/ ← ArgoCD, Vault, ESO
│       └── 02-helm-deployment/← documentation déploiement
├── .github/workflows/         ← pipeline CI/CD GitHub Actions
└── src/AMSC/                  ← code source des micro-services
```

---

## Démarrage rapide pour tout lancer en une commande

```bash
chmod +x start.sh && ./start.sh
```

Ce script fait **tout automatiquement** dans l'ordre :

| Étape | Action | Résultat attendu |
|---|---|---|
| A.1 | Build des 6 images avec Buildah | 6 images ~250MB |
| A.2 | Scan de sécurité Trivy (gate CRITICAL) | Rapports JSON/SARIF générés |
| A.3 | Audit des layers avec Dive | 6/6 PASS, efficacité ~99.83% |
| A.4 | Lint du Containerfile avec Hadolint | 0 erreur |
| B.1 | Démarrage Minikube | Cluster Ready |
| B.2 | Installation Traefik | 1/1 Running |
| B.3 | Installation ArgoCD | Running |
| B.4 | Installation Vault | 1/1 Running |
| B.5 | Installation ESO | Running |
| B.6 | Configuration secrets Vault | SecretSynced |
| B.7 | Déploiement chart Helm MIAGE-Bank | Pods déployés |
| B.8 | Déploiement ArgoCD GitOps | Synced |

> Le script vérifie les prérequis avant chaque partie et s'arrête avec un message
> clair si un outil manque, avec le lien d'installation.

---

## Partie A: Chaîne de build OCI

### Prérequis Partie A

Vérifier que ces outils sont installés :

```bash
buildah --version   # doit afficher buildah version 1.x
trivy --version     # doit afficher Version: 0.x
dive --version      # doit afficher dive 0.x
hadolint --version  # doit afficher Haskell Dockerfile Linter 2.x
java --version      # doit afficher openjdk 17
mvn --version       # doit afficher Apache Maven 3.x
```

Si un outil manque :
- Buildah : https://buildah.io/
- Trivy : https://aquasecurity.github.io/trivy/
- Dive : https://github.com/wagoodman/dive
- Hadolint : https://github.com/hadolint/hadolint
- JDK 17 : https://adoptium.net/
- Maven : https://maven.apache.org/

### Étape A.1 : Construire les images avec Buildah

```bash
chmod +x ci-scripts/*.sh
./ci-scripts/build-images.sh v1

# Vérifier les 6 images créées
buildah images | grep miage-bank
```

### Étape A.2: Scanner la sécurité avec Trivy

```bash
./ci-scripts/scan-trivy.sh v1

# Rapports générés dans :
ls "Partie A/02-security-scan/reports/"
# 12 fichiers : 6 JSON + 6 SARIF
```

> /!\ Des CVE CRITICAL sont détectées sur les JARs Spring Boot (dépendances Tomcat/Spring).
> Le script retourne exit 1 sur CRITICAL : comportement attendu et documenté.
> Plan de remédiation : montée de version Spring Boot 3.2.3 → 3.3.x

### Étape A.3 :Auditer les layers avec Dive

```bash
./ci-scripts/audit-dive.sh v1

# Rapports générés dans :
ls "Partie A/03-image-audit/"
```

### Étape A.4 :Lint du Containerfile avec Hadolint

```bash
hadolint "Partie A/01-image-build/Containerfile"
# doit afficher : 0 erreur, 0 warning
```

### Résultats attendus Partie A

| Outil | Résultat attendu |
|---|---|
| Buildah | 6 images ~250MB (-52% vs Docker) |
| Trivy | 0 CVE Alpine, CVE JAR documentées |
| Dive | 6/6 PASS, efficacité ~99.83% |
| Hadolint | 0 erreur |
| CI GitHub Actions |  Verte |

---

## Partie B: Déploiement Kubernetes

### Prérequis Partie B

Vérifier que ces outils sont installés :

```bash
minikube version    # doit afficher v1.x
kubectl version     # doit afficher Client Version
helm version        # doit afficher v3.x
argocd version      # doit afficher argocd v2.x
```

Si un outil manque :
- Minikube : https://minikube.sigs.k8s.io/docs/start/
- kubectl : https://kubernetes.io/docs/tasks/tools/
- Helm : https://helm.sh/docs/intro/install/
- ArgoCD CLI : https://argo-cd.readthedocs.io/en/stable/cli_installation/

### Étape B.1 :Démarrer Minikube

```bash
minikube start
minikube addons enable metrics-server
kubectl get nodes    # doit afficher Ready
```

### Étape B.2 :Installer Traefik

```bash
helm repo add traefik https://helm.traefik.io/traefik
helm repo update
helm upgrade --install traefik traefik/traefik \
  -n traefik --create-namespace
kubectl get pods -n traefik    # doit afficher 1/1 Running
```

### Étape B.3:Installer ArgoCD

```bash
helm repo add argo https://argoproj.github.io/argo-helm
helm repo update
helm upgrade --install argocd argo/argo-cd \
  -n argocd --create-namespace
kubectl wait --for=condition=Ready pod \
  -l app.kubernetes.io/name=argocd-server \
  -n argocd --timeout=120s
kubectl get pods -n argocd    # doit afficher Running
```

### Étape B.4:Installer Vault

```bash
helm repo add hashicorp https://helm.releases.hashicorp.com
helm repo update
helm upgrade --install vault hashicorp/vault \
  -n vault --create-namespace \
  --set server.dev.enabled=true \
  --set server.dev.devRootToken=root
kubectl wait --for=condition=Ready pod \
  -l app.kubernetes.io/name=vault \
  -n vault --timeout=120s
kubectl get pods -n vault    # doit afficher 1/1 Running
```

### Étape B.5:Installer External Secrets Operator

```bash
helm repo add external-secrets https://charts.external-secrets.io
helm repo update
helm upgrade --install external-secrets \
  external-secrets/external-secrets \
  -n external-secrets --create-namespace
kubectl wait --for=condition=Ready pod \
  -l app.kubernetes.io/name=external-secrets \
  -n external-secrets --timeout=120s
kubectl get pods -n external-secrets    # doit afficher Running
```

### Étape B.6:Configurer les secrets dans Vault

```bash
kubectl create namespace miage-bank --dry-run=client -o yaml | kubectl apply -f -

kubectl exec -n vault vault-0 -- \
  env VAULT_TOKEN=root \
  vault kv put secret/miage-bank/db \
  mysql-password=root mongo-password=root

kubectl create secret generic vault-token \
  --from-literal=token=root \
  -n miage-bank --dry-run=client -o yaml | kubectl apply -f -
```

### Étape B.7:Déployer MIAGE-Bank via Helm

```bash
cd "Partie B"
helm lint miage-bank/    # doit afficher 0 chart(s) failed

helm upgrade --install miage-bank miage-bank/ \
  -n miage-bank --create-namespace \
  --set secrets.mysqlRootPassword=root \
  --set secrets.mongoRootPassword=root
cd ..
```

### Étape B.8:Déployer via ArgoCD

```bash
kubectl apply -f "Partie B/miage-bank/01-infrastructure/namespace.yaml"
kubectl apply -f "Partie B/miage-bank/01-infrastructure/cluster-secret-store.yaml"
kubectl apply -f "Partie B/miage-bank/01-infrastructure/external-secret.yaml"
kubectl apply -f "Partie B/miage-bank/01-infrastructure/argocd-app.yaml"
argocd app get miage-bank    # doit afficher Synced
```

### Vérification finale

```bash
kubectl get pods -n miage-bank             # pods déployés
kubectl get svc -n miage-bank              # services exposés
kubectl get ingress -n miage-bank          # ingress Traefik
kubectl get hpa -n miage-bank              # autoscaling actif
kubectl get networkpolicies -n miage-bank  # sécurité réseau
kubectl get externalsecret -n miage-bank   # secrets ESO
argocd app get miage-bank                  # GitOps Synced
```

---

## Stack technique

- **Build** : Buildah, Maven, JDK 17
- **Sécurité** : Trivy, Dive, Hadolint
- **CI/CD** : GitHub Actions
- **Orchestration** : Kubernetes (Minikube), Helm, ArgoCD, Traefik
- **Secrets** : HashiCorp Vault + External Secrets Operator
- **IA** : Claude (Anthropic) — utilisé pour la génération de scripts, templates
  Helm, configurations K8s et documentation. Toutes les réponses ont été
  comprises, vérifiées et adaptées au contexte MIAGE-Bank.

Environnement : **WSL (Ubuntu)** avec Buildah et Maven.
