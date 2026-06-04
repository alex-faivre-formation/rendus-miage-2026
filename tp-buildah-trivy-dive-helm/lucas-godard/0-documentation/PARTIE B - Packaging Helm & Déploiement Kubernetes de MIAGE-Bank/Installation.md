# Guide d'installation — MIAGE-Bank (de zéro)

Ce guide part d'une machine **vierge** (rien d'installé) et va jusqu'à
l'application accessible dans le navigateur. Il a été écrit sous
**Windows 11 + WSL2 (Debian)**, mais les commandes Linux valent pour toute
distribution.

> Toutes les commandes `kubectl`/`helm`/`minikube` se lancent **dans WSL** (terminal
> Linux). Seule l'édition du fichier hosts Windows se fait côté Windows.

---

## 0. Prérequis système

- **Windows 11** avec **WSL2** activé et une distribution **Debian/Ubuntu**.
  (Dans PowerShell admin : `wsl --install -d Debian`, puis redémarrer.)
- **Docker Desktop** avec l'intégration WSL activée, OU Docker installé dans WSL
  (minikube utilise le driver `docker`).
- Au moins **4 Go de RAM** et **2 CPU** disponibles pour minikube.

---

## 1. Installer les outils dans WSL

```bash
# Mise à jour de base + utilitaires
sudo apt-get update && sudo apt-get install -y curl wget git apt-transport-https ca-certificates gnupg

# --- kubectl ---
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
kubectl version --client

# --- minikube ---
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube
minikube version

# --- Helm ---
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
helm version
```

> Buildah, Trivy, Dive et Hadolint **ne sont pas nécessaires en local** : la
> Partie A tourne entièrement dans la CI GitHub Actions. Tu n'en as besoin que si
> tu veux rejouer les scans manuellement.

---

## 2. Démarrer le cluster

```bash
minikube start --driver=docker --cpus=2 --memory=4096
kubectl get nodes        # le nœud doit être Ready
```

---

## 3. Installer les briques d'infrastructure (prérequis du chart)

Ces composants doivent exister **avant** de déployer MIAGE-Bank (le chart ne les
crée pas lui-même — voir la note "œuf ou la poule" dans la doc Partie B).

### 3.1 External Secrets Operator (CRD ExternalSecret / SecretStore)

```bash
helm repo add external-secrets https://charts.external-secrets.io
helm repo update
helm install external-secrets external-secrets/external-secrets \
  -n external-secrets --create-namespace --set installCRDs=true
kubectl -n external-secrets rollout status deploy/external-secrets
```

### 3.2 Vault (mode dev) + secrets

```bash
helm repo add hashicorp https://helm.releases.hashicorp.com
helm repo update
helm install vault hashicorp/vault -n default \
  --set "server.dev.enabled=true" --set "server.dev.devRootToken=root"
kubectl -n default rollout status statefulset/vault

# Peupler Vault avec les identifiants des bases (KV v2)
kubectl -n default exec vault-0 -- sh -c '
  vault kv put secret/miage-bank/clientservice username=client_user password=root!
  vault kv put secret/miage-bank/compteservice username=compte_user password=root
'
```

> ⚠️ Vault dev garde ses secrets **en mémoire** : si le pod `vault-0` redémarre, il
> faut relancer les deux `vault kv put` ci-dessus.

### 3.3 Traefik (Ingress controller)

```bash
helm repo add traefik https://traefik.github.io/charts
helm repo update
helm install traefik traefik/traefik -n traefik --create-namespace
kubectl -n traefik rollout status deploy/traefik
```

### 3.4 ArgoCD

```bash
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
kubectl -n argocd rollout status deploy/argocd-server
```

---

## 4. Récupérer le projet

```bash
git clone https://github.com/llluuu02/miage-bank-devops.git
cd miage-bank-devops
```

> Les images des micro-services sont déjà publiées sur GHCR par la CI ; il n'y a
> rien à builder en local pour déployer.

---

## 5. Déployer MIAGE-Bank via ArgoCD (GitOps)

```bash
kubectl apply -f argocd/application.yaml
kubectl -n argocd get application miage-bank      # attendre Synced / Healthy
kubectl -n miage-bank get pods                    # tous les pods passent Running
```

> Premier démarrage : le config server clone un dépôt Git au boot, puis les autres
> services s'enregistrent dans Eureka — comptez 1 à 2 minutes avant que tout soit
> `1/1`.

Alternative sans GitOps (déploiement direct) :
```bash
helm install miage-bank ./miage-bank -n miage-bank --create-namespace
```

---

## 6. Exposer et accéder à l'application

### 6.1 Tunnel (donne une IP au LoadBalancer Traefik)

Dans un terminal **dédié, laissé ouvert** (demande le mot de passe sudo) :
```bash
minikube tunnel
```

Vérifier dans un autre terminal :
```bash
kubectl -n traefik get svc traefik    # EXTERNAL-IP doit être renseignée (souvent 127.0.0.1)
kubectl -n miage-bank get ingress     # ADDRESS renseignée, CLASS = traefik
```

### 6.2 Résolution du nom de domaine

Le navigateur tournant sous **Windows**, on édite le hosts **Windows** (le hosts WSL
ne suffit pas).

Dans **PowerShell en administrateur** :
```powershell
Add-Content C:\Windows\System32\drivers\etc\hosts "`n127.0.0.1 miage-bank.local"
```

Et côté WSL (pour tester avec curl) :
```bash
grep miage-bank /etc/hosts || echo "127.0.0.1 miage-bank.local" | sudo tee -a /etc/hosts
```

### 6.3 Accès

- Frontend : ouvrir **http://miage-bank.local/** dans le navigateur.
- API (test) : `curl http://miage-bank.local/api/clients`

---

## 7. Accéder à l'interface ArgoCD (optionnel)

```bash
# mot de passe admin initial
kubectl -n argocd get secret argocd-initial-admin-secret \
  -o jsonpath="{.data.password}" | base64 -d; echo

# exposer l'UI
kubectl -n argocd port-forward svc/argocd-server 8080:443
# -> https://localhost:8080  (utilisateur : admin)
```

---

## 8. Dépannage rapide

| Symptôme                                   | Cause probable / solution                                                    |
| ------------------------------------------ | ---------------------------------------------------------------------------- |
| `miage-bank.local` ne résout pas (navigateur) | Ligne manquante dans le hosts **Windows** ; `minikube tunnel` non lancé.   |
| EXTERNAL-IP du svc traefik reste `<pending>` | `minikube tunnel` non actif, ou un ancien tunnel bloqué (`pkill -f "minikube tunnel"` puis relancer). |
| ExternalSecret en `SecretSyncedError`      | Vault redémarré -> re-seeder les secrets (étape 3.2).                         |
| Pods qui redémarrent en boucle au 1er boot | Démarrage lent (config server / Eureka) ; patienter 1-2 min.                 |
| `helm upgrade` échoue (ownership)          | Le chart est géré par ArgoCD : déployer via Git + ArgoCD, pas en `helm upgrade` manuel. |

---

## 9. Nettoyage

```bash
kubectl delete -f argocd/application.yaml      # retire l'app (et ses ressources via prune)
minikube stop                                  # arrête le cluster
minikube delete                                # supprime tout (repartir de zéro)
```
