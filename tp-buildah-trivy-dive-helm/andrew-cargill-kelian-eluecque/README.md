# Andrew CARGILL Kelian ELUECQUE
## Projet MIAGE Bank - Partie A et B

# MIAGE Bank - Chaîne d'Intégration Continue (CI/CD)

Ce document décrit la démarche d'ingénierie DevOps mise en œuvre pour l'architecture micro-services de l'application **MIAGE Bank**. Il détaille la structure du pipeline d'intégration continue basé sur **GitHub Actions**, ainsi que l'utilisation d'outils modernes, sécurisés et performants pour la conteneurisation et l'audit des images OCI : **Hadolint**, **Buildah**, **Trivy** et **Dive**.

---

## 1. Vision Globale et Objectifs de la Chaîne

Dans une architecture distribuée moderne (micro-services), la gestion des images de conteneurs requiert une rigueur accrue en matière de sécurité, d'optimisation de l'espace et d'automatisation. Les objectifs clés de cette chaîne CI/CD sont :
1. **L'automatisation et le parallélisme :** Utilisation d'une stratégie matricielle pour compiler et auditer les 6 micro-services de manière indépendante et simultanée.
2. **La conteneurisation "Daemonless" :** Remplacement de Docker par **Buildah** pour s'affranchir du démon root de Docker, éliminant ainsi un vecteur d'attaque majeur en production.
3. **La conformité et la qualité du code de configuration :** Analyse statique des fichiers `Containerfile` avec **Hadolint**.
4. **La sécurité par le design (DevSecOps) :** Intégration d'une *Quality Gate* stricte bloquant le pipeline en cas de vulnérabilité critique détectée par **Trivy**.
5. **L'efficience des couches (Layers) :** Analyse fine de l'empreinte de l'image et détection du gaspillage d'espace via **Dive**.

---

## 2. Architecture et Structure du Pipeline

Le pipeline est défini dans le fichier `.github/workflows/build-pipeline.yml` et s'exécute sur des agents `ubuntu-latest`.

### Stratégie Matricielle
Le pipeline utilise la fonctionnalité `matrix` de GitHub Actions pour exécuter le même cycle de jobs en parallèle sur l'ensemble des modules :
* `Banque-Annuaire`
* `Banque-ConfigServer`
* `Banque-APIGateway`
* `Banque-ClientService`
* `Banque-CompteService`
* `Banque-CompositeService`

La configuration `fail-fast: false` est activée pour s'assurer que l'échec de la construction d'un micro-service ne bloque pas l'analyse et la validation des autres.

---

## 3. Description Détaillée des Étapes de la Chaîne

Chaque exécution parallèle effectue la séquence d'étapes suivantes :

### A. Préparation de l'environnement et Compilation
1. **Checkout du code :** Récupération du code source depuis le dépôt Git via `actions/checkout@v4`.
2. **Setup Java & Cache Maven :** Configuration du JDK 17 (Distribution Temurin) et activation du système de cache natif de GitHub Actions pour Maven (`cache: 'maven'`). Cela réduit drastiquement le temps de téléchargement des dépendances à chaque build.
3. **Compilation applicative :** Génération du package `.jar` executable via la commande :
   ```bash
   mvn -f "${{ matrix.service }}/pom.xml" clean package -DskipTests
   ```

### B. Analyse Statique (Linting) du Containerfile
* **Outil utilisé :** `Hadolint`
* **Rôle :** Vérifier que les instructions de construction respectent les bonnes pratiques (non-utilisation de `latest` pour les images de base, regroupement des commandes `RUN`, nettoyage des caches de package, etc.).
* **Seuil :** Configuré sur `warning` pour garantir une propreté stricte avant la phase de build.

### C. Construction de l'image OCI sans Démon (Daemonless)
* **Outil utilisé :** `Buildah`
* **Rôle :** Compilation de l'image à partir du `Containerfile`.
* **Mécanisme technique :** 1. Passage en minuscules du nom du service pour se conformer aux standards de nommage des registres d'images.
  2. Exécution de `buildah bud` (Build Using Dockerfile) de manière isolée sans privilèges Root globaux ni dépendance à un socket Docker externe (`/var/run/docker.sock`).
  3. Exportation immédiate de l'image sous forme d'archive tar standard OCI via :
     ```bash
     buildah push --remove-signatures "$IMAGE_NAME:latest" docker-archive:"build-reports/$IMAGE_NAME.tar"
     ```
     Cette archive sert d'artefact unique pour les outils d'audit suivants.

### D. Audit de Sécurité et Quality Gate
* **Outil utilisé :** `Trivy`
* **Rôle :** Scan de vulnérabilités (CVE) au sein de la base de l'image (OS) et des bibliothèques applicatives (dépendances Java).
* **Fonctionnement :**
  1. **La Gate (Sécurité Stricte) :** Analyse l'archive `.tar` et renvoie un code de sortie `1` (échec du job) si une seule faille de sévérité `CRITICAL` est détectée.
  2. **Génération des Rapports :** Si la Gate est franchie, Trivy génère des rapports complets aux formats standardisés **JSON** et **SARIF** (intégration native dans l'onglet Sécurité de GitHub).

### E. Analyse de l'Efficience des Couches
* **Outil utilisé :** `Dive`
* **Rôle :** Estimer le score d'efficacité de l'image (pourcentage d'espace utile par rapport à l'espace dupliqué ou perdu).
* **Mécanisme :** Exécution en mode non-interactif via la variable `CI=true` sur l'archive tar. L'analyse échouera si l'image présente des anomalies majeures de construction (fichiers temporaires volumineux non supprimés dans la même couche).

### F. Collecte et Persistance des Artefacts
* **Outil utilisé :** `actions/upload-artifact@v4`
* **Rôle :** Sauvegarde de l'ensemble des rapports d'audit (Trivy JSON, SARIF, logs de Dive) dans l'interface de GitHub pour une durée de 7 jours. La condition `if: always()` garantit la récupération des rapports même si la chaîne échoue à cause d'une faille de sécurité ou d'une inefficience.

---

## 4. Guide d'Exécution et d'Interprétation

### Déclenchement Automatique
La chaîne est configurée de manière transparente pour l'équipe de développement. Elle s'exécute automatiquement lors de :
* Tout **Push** sur la branche principale `main`.
* Toute **Pull Request** ciblant la branche `main`.

### Comment Consulter les Résultats ?
1. Rendez-vous sur votre dépôt GitHub, onglet **Actions**.
2. Sélectionnez le workflow **"CI/CD Pipeline - MIAGE Bank"**.
3. Vous visualiserez les 6 exécutions parallèles. Un indicateur vert ou rouge vous signalera l'état de conformité de chaque micro-service.
4. En bas de la page du build, la section **Artifacts** vous permettra de télécharger les fichiers `rapports-audit-Banque-XXXX.zip` contenant les analyses détaillées de Trivy.

---

## 5. Tests et Exécution Locale (Démarche de Débug)

Avant de pousser vos modifications sur la branche `main`, il est fortement recommandé de tester localement les étapes clés sur votre poste de travail Linux/macOS.

### Compiler l'image localement avec Buildah
```bash
# Se placer à la racine du micro-service concerné
cd Banque-ClientService

# Construire l'image sans démon Docker
buildah bud -f Containerfile -t miage-bank-client:latest .

# Exporter l'image au format OCI tar pour les outils d'analyse
buildah push miage-bank-client:latest docker-archive:client-service.tar
```

### Scanner l'image localement avec Trivy
```bash
# Analyser le fichier tar généré pour intercepter les failles critiques
trivy input client-service.tar --severity CRITICAL
```

### Analyser l'efficacité des couches avec Dive
```bash
# Lancer l'interface interactive de Dive pour explorer le contenu de chaque layer
dive docker-archive://client-service.tar
```
# MIAGE Bank - Guide de Déploiement Continu GitOps (Bout en Bout)

## 1. Vision Globale du Socle de Déploiement

Le déploiement de l'architecture micro-services de la **MIAGE Bank** s'appuie sur le paradigme du **GitOps déclaratif**. L'intégralité de l'état cible du cluster Kubernetes est définie sous forme de code dans un dépôt Git de référence. L'orchestration automatise le déploiement des six micro-services applicatifs :
* `Banque-Annuaire` (Registre Eureka)
* `Banque-ConfigServer` (Serveur de configuration Spring Cloud)
* `Banque-APIGateway` (Passerelle de routage et point d'entrée unique)
* `Banque-ClientService` (Gestion des données clients)
* `Banque-CompteService` (Gestion des comptes bancaires)
* `Banque-CompositeService` (Orchestrateur métier)

---

## 2. Architecture et Structuration du Chart Helm

Pour industrialiser le packaging des manifestes Kubernetes, le choix s'est porté sur la conception d'un **Umbrella Chart** (Chart parapluie) nommé `helm-miage-bank/`. Cette structure centralise la configuration tout en permettant d'itérer dynamiquement sur les spécificités de chaque micro-service.

### A. Arborescence du Répertoire GitOps
```text
helm-miage-bank/
├── Chart.yaml                  # Métadonnées globales et inter-dépendances
├── values.yaml                 # Profil de configuration par défaut (Environnement de Dev)
├── values-prod.yaml            # Surcharges durcies spécifiques à la Production
└── templates/
    ├── _helpers.tpl            # Fonctions nommées et gestion homogène des labels OCI
    ├── deployment.yaml         # Déploiement paramétré bouclant sur la liste des services
    ├── service.yaml            # Abstraction réseau interne (ClusterIP) pour chaque pod
    ├── ingress.yaml            # Point de terminaison et routage HTTP externe
    ├── networkpolicy.yaml      # Règles de cloisonnement et d'isolation réseau de couche 4
    └── secrets.yaml            # Déclaration des secrets d'infrastructure (ou manifestes ESO)
```

### B. Gestion des Fichiers de Configuration (`values.yaml` vs `values-prod.yaml`)

* **`values.yaml` (Profil Développement / Local) :** Optimisé pour une exécution à faible empreinte sur un cluster local (type Minikube ou Kind). Le nombre de réplicas par service est fixé à `1`, le type de service réseau est configuré en `ClusterIP`, les profils Spring activés pointent vers des environnements de test (`dev`), et les allocations de ressources matérielles (CPU/RAM) sont réduites au strict minimum.
* **`values-prod.yaml` (Profil Production / Haute Disponibilité) :** Surcharge l'état initial pour répondre aux exigences de production :
  * **Multi-réplication** : Minimum `3` instances par micro-service pour assurer la résilience et la tolérance aux pannes.
  * **Sondes de Santé (Probes)** : Configuration systématique d'une `livenessProbe` (détection des blocages de la JVM) et d'une `readinessProbe` (validation que le serveur Spring Boot est totalement initialisé et apte à consommer du trafic HTTP).
  * **Contrôle des Ressources** : Définition stricte des requêtes et limites de ressources (`requests` et `limits` CPU/Mémoire) afin de garantir l'étanchéité du cluster et d'éviter les arrêts brutaux de type *OOMKilled* (Out Of Memory).

---

## 3. Gestion Sécurisée des Configurations et des Secrets (SecDevOps)

Le respect de la conformité bancaire interdit le stockage d'identifiants ou de secrets en clair au sein des dépôts Git. La gestion des secrets est donc externalisée et découplée du Chart Helm via deux approches professionnelles acceptées :

### Approche A : HashiCorp Vault + External Secrets Operator (ESO)
1. Les données sensibles (mots de passe de base de données, secrets applicatifs) sont stockées de façon chiffrée dans une instance sécurisée **HashiCorp Vault**.
2. Un contrôleur **External Secrets Operator (ESO)** est déployé dans le cluster Kubernetes.
3. Le projet déclare un manifeste `ExternalSecret` qui référence le chemin secret dans Vault. ESO intercepte cet objet, s'authentifie auprès de Vault, extrait la valeur, et génère dynamiquement un `Secret` Kubernetes natif en mémoire dans le Namespace applicatif cible.

### Approche B : Secrets Kubernetes Natifs (StringData)
1. Un Secret Kubernetes natif avec `stringData` est créé séparément du chart pour isoler le cycle de vie des informations sensibles.
2. Ce secret est ensuite référencé de manière sécurisée par son nom dans la configuration d'environnement des conteneurs.

---

## 4. Isolation et Sécurité Réseau (Network Policies)

Par défaut, Kubernetes applique un réseau ouvert où tous les pods peuvent communiquer sans restriction. Pour sécuriser les transactions de la **MIAGE Bank**, le réseau est cloisonné au niveau de la couche 4 (Transport) via des objets **NetworkPolicy** selon le principe du moindre privilège.

* **Politique d'interdiction par défaut (*Default-Deny*) :** Tout flux réseau entrant (*Ingress*) sur le namespace est restreint et autorisé uniquement depuis le contrôleur de domaine ou d'Ingress (Traefik / Minikube).
* **Flux APIGateway exclusif :** Le pod `Banque-APIGateway` est le seul composant du namespace autorisé à recevoir du trafic réseau en provenance du contrôleur Ingress externe.
* **Cloisonnement inter-services :** Les services métiers finaux (`Banque-ClientService`, `Banque-CompteService`) rejettent catégoriquement toute tentative de connexion directe depuis l'extérieur du cluster ou depuis d'autres micro-services non autorisés. Ils n'acceptent des flux entrants **que** si la source provient explicitement des pods `Banque-APIGateway` ou `Banque-CompositeService`.
* **Isolation des Persistances :** Les bases de données (Mongo/Postgres) ou instances de cache n'acceptent de trafic réseau qu'en provenance stricte des micro-services métiers authentifiés qui en ont la charge directe.

---

## 5. Exposition des Services (Ingress)

L'accès à la plateforme bancaire depuis l'extérieur du cluster est centralisé et sécurisé par une ressource **Ingress**, s'appuyant sur un contrôleur industriel de classe Traefik.

Le fichier `ingress.yaml` mappe le nom de domaine qualifié de la banque (paramétrable via `values.yaml`) et gère la réécriture des chemins HTTP :
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: miage-bank-ingress
  annotations:
    traefik.ingress.kubernetes.io/rewrite-target: /
spec:
  ingressClassName: traefik
  rules:
  - host: banque.miage.local
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: banque-apigateway
            port:
              number: 8080
```
Ce mécanisme de reverse-proxy masque l'architecture interne du cluster. L'API Gateway intercepte l'ensemble des flux entrants sur le port `8080` avant de les distribuer aux services internes en s'appuyant sur le CoreDNS de Kubernetes.

---

## 6. Cinématique GitOps avec ArgoCD et Gestion de la Dérive

Le déploiement continu élimine l'usage impératif de commandes directes (type `kubectl apply`). Il est entièrement opéré par **ArgoCD**, qui assure la réconciliation automatique de l'infrastructure ciblant la branche `main`.

### A. Manifeste de l'Application ArgoCD
Le fichier `application.yaml` déclare l'application auprès d'ArgoCD, liant le dépôt GitOps au cluster Kubernetes avec synchronisation automatique (`prune: true`, `selfHeal: true`) :
```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: miage-bank-gitops
  namespace: argocd
spec:
  project: default
  source:
    repoURL: '[https://github.com/votre-compte/rendus-miage-2026.git](https://github.com/votre-compte/rendus-miage-2026.git)'
    targetRevision: HEAD
    path: tp-buildah-trivy-dive-helm/helm-miage-bank
    helm:
      valueFiles:
        - values-prod.yaml
  destination:
    server: '[https://kubernetes.default.svc](https://kubernetes.default.svc)'
    namespace: miage-bank
  syncPolicy:
    automated:
      prune: true       # Supprime les ressources K8s obsolètes non déclarées sur Git
      selfHeal: true    # Corrige automatiquement les modifications manuelles (dérive)
```

### B. Démonstration Pratique de la Gestion de la Dérive (*Drift Management*)

La gestion de la dérive logicielle garantit que personne ne peut modifier l'infrastructure en production de manière artisanale ou non planifiée.

#### Scénario de Simulation de Dérive et Auto-Réconciliation :
1. **État Initial Stable** : L'application est déployée de manière conforme. Le fichier `values-prod.yaml` sur Git exige la présence de `3` réplicas pour le service `Banque-ClientService`. L'interface graphique d'ArgoCD affiche un statut au vert : **`Synced`** (Synchronisé) et **`Healthy`** (En bonne santé).
2. **Introduction Manuelle de la Dérive** : Un opérateur se connecte directement au cluster et exécute une commande impérative de contournement via la CLI Kubernetes en modifiant manuellement le nombre de réplicas :
   ```bash
   kubectl scale deployment banque-clientservice --replicas=5 -n miage-bank
   ```
3. **Détection Immédiate** : Lors de son cycle régulier de surveillance (polling/webhook), ArgoCD compare l'état réel du cluster (5 pods actifs) avec l'état cible stocké sur Git (3 pods). Constatant l'écart, ArgoCD détecte le statut `OutOfSync` et fait passer visuellement l'application au statut d'alerte **`OutOfSync`** (Désynchronisé).
4. **Auto-Réconciliation (*Self-Healing*)** : 
   Le paramètre `selfHeal` étant activé à `true` dans la politique de synchronisation, ArgoCD déclenche la réconciliation et prend immédiatement des mesures correctives automatiques sans intervention humaine. Il écrase la modification impérative non autorisée et réapplique les manifestes déclaratifs issus de Git. Les deux pods excédentaires sont immédiatement terminés et détruits, ramenant l'infrastructure à son état nominal de `3` réplicas. L'application repasse instantanément au statut **`Synced`**.

---

## 7. Guide d'Exécution et Commandes de Diagnostic Local

Avant de soumettre formellement vos modifications via une *Pull Request*, il est fortement recommandé d'exécuter les procédures de validation attendues du chart avant déploiement :

### Validation syntaxique et rendu local des templates Helm (Dry-Run)
Ces commandes permettent de valider la syntaxe de vos fichiers Helm et de visualiser les manifestes Kubernetes générés sans altérer le cluster :
```bash
helm lint ./helm-miage-bank/
helm template miage-bank ./helm-miage-bank/ --values ./helm-miage-bank/values-prod.yaml
helm install --dry-run miage-bank ./helm-miage-bank/
```

### Initialisation locale d'ArgoCD et Synchronisation Manuelle
Si vous souhaitez tester la cinématique complète sur un cluster de développement local (type Minikube) :
```bash
# 1. Création du namespace et installation d'ArgoCD
kubectl create namespace argocd
kubectl apply -n argocd -f [https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml](https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml)

# 2. Enregistrement de l'application MIAGE Bank
kubectl apply -f argocd/application.yaml

# 3. Forcer manuellement la première synchronisation via la CLI ArgoCD
argocd app sync miage-bank-gitops
```