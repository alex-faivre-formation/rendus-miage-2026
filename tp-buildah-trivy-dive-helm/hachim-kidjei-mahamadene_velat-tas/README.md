Hachim Kidjei MAHAMADENE — Velat TAS

# TP — Buildah, Trivy, Dive & Helm/Kubernetes — Projet Odoru

## 1. Contexte du rendu

Ce rendu correspond au TP **Buildah, Trivy, Dive & Helm/Kubernetes**.

Le sujet de référence mentionne l’application **MIAGE Bank**. Dans ce rendu, les mêmes objectifs techniques ont été appliqués au projet microservices **Odoru**, une application de gestion d’un club de danse rythmique.

Cette adaptation conserve l’esprit du sujet :

- construction d’images OCI ;
- analyse de sécurité des images ;
- analyse des couches d’images ;
- packaging Helm ;
- déploiement Kubernetes ;
- sécurisation du déploiement ;
- gestion des secrets ;
- approche GitOps avec ArgoCD.

Le projet Odoru présente une architecture microservices comparable à celle attendue dans le TP :

- frontend web ;
- API Gateway ;
- microservices métier ;
- bases PostgreSQL ;
- Config Server ;
- Discovery Service ;
- authentification centralisée avec Keycloak ;
- déploiement Kubernetes avec Helm ;
- analyse d’images avec Trivy et Dive ;
- synchronisation GitOps avec ArgoCD.

L’objectif du rendu est de démontrer la capacité à :

- construire des images OCI avec Buildah ;
- analyser la sécurité des images avec Trivy ;
- analyser les couches d’images avec Dive ;
- automatiser la chaîne de build ;
- packager une application microservices avec Helm ;
- déployer l’application sur Kubernetes ;
- sécuriser le déploiement avec RBAC, NetworkPolicy, probes, resources et HPA ;
- gérer les secrets avec des `Secret` Kubernetes ;
- automatiser la configuration Keycloak ;
- initialiser les données métier nécessaires aux tests ;
- démontrer une approche GitOps avec ArgoCD ;
- vérifier l’accès applicatif complet depuis le navigateur.

---

## 2. Présentation synthétique de l’application Odoru

Odoru est une application microservices destinée à gérer un club de danse rythmique.

Elle permet notamment de gérer :

- les membres du club ;
- les cours ;
- les compétitions ;
- les badges ;
- les statistiques ;
- l’authentification des utilisateurs via Keycloak.

L’application est composée des éléments suivants :

| Élément | Rôle |
| --- | --- |
| `odoru-front` | Interface web de démonstration |
| `api-gateway` | Point d’entrée unique vers les microservices |
| `member-service` | Gestion des membres |
| `course-service` | Gestion des cours |
| `competition-service` | Gestion des compétitions |
| `badge-service` | Gestion des badges |
| `statistics-service` | Agrégation de statistiques |
| `config-server` | Configuration centralisée |
| `discovery-service` | Découverte de services avec Eureka |
| `keycloak` | Authentification et gestion des rôles |
| PostgreSQL | Bases de données des microservices |

---

## 3. Architecture générale

L’architecture suit une logique microservices.

```text
Navigateur
   |
   | http://localhost:30081
   v
Frontend Odoru
   |
   | Redirection OAuth2/OpenID Connect
   v
Keycloak
   |
   | Token JWT
   v
Frontend Odoru
   |
   | Appels API sécurisés
   v
API Gateway
   |
   +--> member-service        --> member-postgres
   +--> course-service        --> course-postgres
   +--> competition-service   --> competition-postgres
   +--> badge-service         --> badge-postgres
   +--> statistics-service
   |
   +--> Discovery Service
   |
   +--> Config Server
```

En Kubernetes, l’application est déployée dans le namespace :

```text
odoru
```

---

## 4. Structure du dépôt

Structure principale du projet :

```text
.
├── build-reports
│   ├── dive
│   ├── oci
│   └── trivy
├── infrastructure
│   ├── argocd
│   │   └── odoru-application.yaml
│   ├── config-repo
│   └── helm
│       └── odoru
├── odoru-front
├── scripts
│   ├── analyze-dive.sh
│   ├── build-all.sh
│   ├── scan-trivy.sh
│   ├── seed-kubernetes-data.sh
│   └── setup-keycloak-odoru.sh
├── services
│   ├── api-gateway
│   ├── badge-service
│   ├── competition-service
│   ├── config-server
│   ├── course-service
│   ├── discovery-service
│   ├── member-service
│   └── statistics-service
└── tp-buildah-trivy-dive-helm
    └── hachim-mahamadene
        └── README.md
```

---

## 5. Environnement logiciel utilisé

Le projet a été testé dans un environnement local basé sur Windows, WSL2 Ubuntu et Docker Desktop avec Kubernetes activé.

| Outil | Version utilisée / attendue | Rôle |
| --- | --- | --- |
| Windows | Windows 10 / 11 | Système hôte |
| WSL2 Ubuntu | Ubuntu 24.04.1 LTS | Environnement Linux |
| Docker Desktop | Docker Engine actif | Runtime conteneur |
| Kubernetes Docker Desktop | `docker-desktop`, testé en v1.32.2 | Cluster Kubernetes local |
| Java | OpenJDK 17 | Compilation des microservices Spring Boot |
| Node.js | v22.22.3 | Build du frontend React/Vite |
| npm | 11.9.0 | Gestion des dépendances frontend |
| Helm | v3.21.0 | Packaging et rendu Kubernetes |
| Buildah | 1.33.7 | Construction des images OCI |
| Trivy | 0.71.0 | Analyse de vulnérabilités |
| Dive | 0.13.1 | Analyse des couches d’images |
| kubectl | Compatible Docker Desktop | Administration Kubernetes |
| ArgoCD | Manifests officiels `stable` | GitOps et synchronisation du cluster |

Commandes de vérification :

```bash
git status
kubectl config current-context
kubectl get nodes
helm version
buildah --version
trivy --version
dive --version
java -version
node -v
npm -v
docker --version
kubectl version --client
```

Résultat attendu :

```text
docker-desktop
docker-desktop Ready
Helm OK
Buildah OK
Trivy OK
Dive OK
Java 17 OK
Node.js OK
npm OK
Docker OK
kubectl OK
```

---

## 6. Logiciels nécessaires pour reproduire le projet

Pour reproduire le projet sur un autre poste, les logiciels suivants doivent être installés :

| Logiciel | Obligatoire | Utilisation |
| --- | --- | --- |
| Git | Oui | Cloner le dépôt |
| Docker Desktop | Oui | Exécuter Kubernetes localement et charger les images |
| Kubernetes Docker Desktop | Oui | Déployer l’application |
| WSL2 Ubuntu | Recommandé | Exécuter les scripts Linux |
| Java 17 | Oui | Compiler les microservices |
| Node.js + npm | Oui | Compiler le frontend |
| Buildah | Oui pour la Partie A | Construire les images OCI |
| Trivy | Oui pour la Partie A | Scanner les images |
| Dive | Oui pour la Partie A | Analyser les couches d’images |
| Helm | Oui pour la Partie B | Valider et rendre le chart Kubernetes |
| kubectl | Oui | Vérifier et administrer le cluster |
| ArgoCD | Oui pour la démo GitOps | Tester la synchronisation GitOps |

Installation indicative sous WSL2 Ubuntu :

```bash
sudo apt update
sudo apt install -y curl wget unzip git openjdk-17-jdk software-properties-common
sudo add-apt-repository universe -y
sudo apt update
sudo apt install -y buildah tree
```

Installation de Helm :

```bash
curl -fsSL https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
```

Node.js peut être installé via NodeSource :

```bash
curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
sudo apt install -y nodejs
```

---

# Partie A — Buildah, Trivy et Dive

## 7. Objectifs de la Partie A

La Partie A du TP porte sur :

- l’analyse comparative Docker / Buildah ;
- la construction d’images OCI avec Buildah ;
- l’analyse de sécurité avec Trivy ;
- l’analyse des couches avec Dive ;
- l’automatisation de la chaîne de build.

---

## 8. Analyse comparative Docker / Buildah

Docker est une solution complète de gestion de conteneurs. Il repose historiquement sur un démon centralisé.

Buildah est un outil spécialisé dans la construction d’images OCI. Il permet de construire des images sans dépendre d’un démon Docker permanent.

| Critère | Docker | Buildah |
| --- | --- | --- |
| Architecture | Client + démon Docker | Sans démon centralisé |
| Usage principal | Build, run, gestion complète | Build d’images OCI |
| Format | Images Docker/OCI | Images OCI |
| Sécurité | Dépend du démon Docker | Réduction de la surface d’attaque |
| Intérêt DevSecOps | Très répandu | Adapté aux pipelines rootless et OCI |

Dans ce rendu, Buildah est utilisé pour construire toutes les images des composants Odoru.

---

## 9. Images construites avec Buildah

Les images construites sont :

```text
localhost/odoru/config-server:1.0.0
localhost/odoru/discovery-service:1.0.0
localhost/odoru/api-gateway:1.0.0
localhost/odoru/member-service:1.0.0
localhost/odoru/course-service:1.0.0
localhost/odoru/competition-service:1.0.0
localhost/odoru/badge-service:1.0.0
localhost/odoru/statistics-service:1.0.0
localhost/odoru/front:1.0.0
```

Chaque service dispose d’un `Containerfile`.

Exemples :

```text
services/member-service/Containerfile
services/api-gateway/Containerfile
services/config-server/Containerfile
odoru-front/Containerfile
```

---

## 10. Script de build intégré

Le script principal de build est :

```text
scripts/build-all.sh
```

Il permet de construire toutes les images Odoru avec Buildah.

Commande :

```bash
chmod +x scripts/*.sh
./scripts/build-all.sh 1.0.0
```

Vérification :

```bash
buildah images | grep odoru
```

Ce script centralise la construction des images et rend le processus reproductible.

---

## 11. Export des images OCI

Les images construites sont exportées sous forme d’archives dans :

```text
build-reports/oci
```

Exemples :

```text
build-reports/oci/member-service-1.0.0.tar
build-reports/oci/api-gateway-1.0.0.tar
build-reports/oci/front-1.0.0.tar
```

Ces archives permettent de charger les images dans Docker Desktop pour le déploiement Kubernetes local.

Commandes :

```bash
docker load -i build-reports/oci/config-server-1.0.0.tar
docker load -i build-reports/oci/discovery-service-1.0.0.tar
docker load -i build-reports/oci/api-gateway-1.0.0.tar
docker load -i build-reports/oci/member-service-1.0.0.tar
docker load -i build-reports/oci/course-service-1.0.0.tar
docker load -i build-reports/oci/competition-service-1.0.0.tar
docker load -i build-reports/oci/badge-service-1.0.0.tar
docker load -i build-reports/oci/statistics-service-1.0.0.tar
docker load -i build-reports/oci/front-1.0.0.tar
```

Vérification :

```bash
docker images | grep odoru
```

---

## 12. Analyse Trivy

Trivy est utilisé pour analyser les vulnérabilités des images.

Script utilisé :

```text
scripts/scan-trivy.sh
```

Commande :

```bash
./scripts/scan-trivy.sh 1.0.0
```

Les rapports sont générés dans :

```text
build-reports/trivy
```

Exemples :

```text
api-gateway-trivy.json
api-gateway-trivy.sarif
member-service-trivy.json
member-service-trivy.sarif
front-trivy.json
front-trivy.sarif
```

Les rapports SARIF permettent une intégration dans des outils de sécurité et de revue de code.

Plan de remédiation général :

| Problème possible | Remédiation |
| --- | --- |
| Image de base vulnérable | Mettre à jour l’image de base |
| Dépendance vulnérable | Mettre à jour la dépendance applicative |
| Paquet système vulnérable | Rebuilder l’image avec une base corrigée |
| Vulnérabilité non exploitable localement | Documenter et surveiller |

---

## 13. Analyse Dive

Dive est utilisé pour analyser les couches des images.

Script utilisé :

```text
scripts/analyze-dive.sh
```

Commande :

```bash
./scripts/analyze-dive.sh 1.0.0
```

Les rapports sont générés dans :

```text
build-reports/dive
```

Exemples :

```text
api-gateway-dive.json
member-service-dive.json
front-dive.json
dive-summary-1.0.0.md
```

L’analyse Dive permet de contrôler :

- le nombre de couches ;
- le contenu des couches ;
- l’efficacité de l’image ;
- la présence éventuelle de fichiers inutiles ;
- les pistes d’optimisation.

Optimisations retenues :

- utilisation d’images d’exécution légères ;
- séparation build / runtime ;
- exclusion des dossiers inutiles ;
- absence de `target`, `node_modules` et fichiers temporaires dans les images finales ;
- centralisation des exports dans `build-reports`.

---

# Partie B — Helm, Kubernetes, sécurité et GitOps

## 14. Objectifs de la Partie B

La Partie B porte sur :

- la création d’un chart Helm complet ;
- le déploiement Kubernetes de l’application ;
- la sécurisation du déploiement ;
- la gestion des secrets ;
- la configuration d’un mode production ;
- la mise en place d’une approche GitOps avec ArgoCD ;
- la démonstration d’une dérive et de sa correction automatique.

---

## 15. Chart Helm Odoru

Le chart Helm est disponible dans :

```text
infrastructure/helm/odoru
```

Structure principale :

```text
infrastructure/helm/odoru
├── Chart.yaml
├── values.yaml
├── values-prod.yaml
└── templates
    ├── namespace.yaml
    ├── secrets.yaml
    ├── serviceaccount.yaml
    ├── networkpolicy.yaml
    ├── hpa.yaml
    ├── ingress.yaml
    ├── postgres.yaml
    ├── keycloak.yaml
    ├── config-server.yaml
    ├── discovery-service.yaml
    ├── api-gateway.yaml
    ├── microservices.yaml
    └── front.yaml
```

Le chart permet de déployer :

- le namespace `odoru` ;
- les secrets Kubernetes ;
- les bases PostgreSQL ;
- Keycloak ;
- Config Server ;
- Discovery Service ;
- API Gateway ;
- les microservices métier ;
- le frontend ;
- RBAC ;
- NetworkPolicy ;
- HPA ;
- Ingress.

---

## 16. Infrastructure Kubernetes déployée

Le chart Helm Odoru déploie l’ensemble de l’application dans un namespace dédié :

```text
odoru
```

L’infrastructure Kubernetes est composée des ressources suivantes :

| Ressource Kubernetes | Rôle dans Odoru |
| --- | --- |
| `Namespace` | Isole les ressources de l’application |
| `Deployment` | Déploie les composants applicatifs et techniques |
| `Service` | Permet la communication réseau entre les pods |
| `Secret` | Stocke les identifiants PostgreSQL et Keycloak |
| `ServiceAccount` | Fournit une identité Kubernetes dédiée aux pods |
| `Role` / `RoleBinding` | Définit les droits associés au ServiceAccount |
| `NetworkPolicy` | Contrôle les flux réseau entrants dans le namespace |
| `HorizontalPodAutoscaler` | Prépare l’autoscaling horizontal |
| `Ingress` | Prépare l’exposition HTTP via Traefik |
| `Application ArgoCD` | Synchronise l’état Kubernetes avec le dépôt Git |

---

## 17. Composants applicatifs déployés

Le chart Helm déploie les composants suivants sous forme de `Deployment` :

| Deployment | Description | Port |
| --- | --- | --- |
| `front` | Frontend web Odoru | 80 |
| `api-gateway` | Point d’entrée API | 8080 |
| `member-service` | Gestion des membres | 8081 |
| `course-service` | Gestion des cours | 8082 |
| `competition-service` | Gestion des compétitions | 8083 |
| `badge-service` | Gestion des badges | 8084 |
| `statistics-service` | Statistiques | 8085 |
| `config-server` | Configuration centralisée | 8888 |
| `discovery-service` | Service Eureka | 8761 |
| `keycloak` | Fournisseur d’identité | 8080 |

---

## 18. Bases PostgreSQL déployées

Chaque microservice métier nécessitant une base dispose de sa propre base PostgreSQL.

| Deployment PostgreSQL | Base créée | Service associé |
| --- | --- | --- |
| `member-postgres` | `odoru_member_db` | `member-service` |
| `course-postgres` | `odoru_course_db` | `course-service` |
| `competition-postgres` | `odoru_competition_db` | `competition-service` |
| `badge-postgres` | `odoru_badge_db` | `badge-service` |

Cette séparation respecte la logique microservices : chaque service métier possède sa propre persistance.

---

## 19. Services Kubernetes et exposition locale

Le déploiement Kubernetes distingue les services exposés vers l’extérieur et les services réservés aux communications internes du cluster.

| Composant | Type Kubernetes | Accès local | Rôle |
| --- | --- | --- | --- |
| `front` | `NodePort` | `http://localhost:30081` | Interface web utilisée par le navigateur |
| `api-gateway` | `NodePort` | `http://localhost:30080` | Point d’entrée HTTP vers les microservices |
| `keycloak` | `ClusterIP` | Accès interne Kubernetes | Fournisseur d’identité OAuth2/OpenID Connect |
| microservices métier | `ClusterIP` | Accès interne Kubernetes | Services applicatifs |
| bases PostgreSQL | `ClusterIP` | Accès interne Kubernetes | Persistance des microservices |

Le frontend et l’API Gateway sont exposés en `NodePort` afin de permettre un test local simple :

```text
Frontend    : http://localhost:30081
API Gateway : http://localhost:30080
```

Keycloak est volontairement conservé en `ClusterIP`. Cela signifie qu’il est accessible à l’intérieur du cluster Kubernetes via :

```text
http://keycloak:8080
```

Cette adresse est utilisée par les pods Kubernetes, notamment par l’API Gateway pour valider les tokens JWT.

En revanche, le navigateur Windows utilisé pour tester l’application se trouve à l’extérieur du cluster Kubernetes. Il ne peut donc pas résoudre directement l’adresse interne :

```text
http://keycloak:8080
```

Or, dans un flux OAuth2/OpenID Connect, le navigateur doit accéder directement à Keycloak pour afficher la page de connexion. Pour cette raison, un port-forward local est utilisé pendant la démonstration :

```powershell
kubectl port-forward -n odoru svc/keycloak 8090:8080
```

Ce port-forward rend Keycloak temporairement accessible depuis le navigateur à l’adresse :

```text
http://localhost:8090
```

Le port-forward ne déploie pas Keycloak. Keycloak est déjà déployé dans Kubernetes par Helm. Le port-forward crée uniquement un tunnel local entre le poste de test et le service Keycloak interne au cluster.

En production ou dans un environnement d’intégration plus complet, ce port-forward serait remplacé par une exposition stable via Ingress ou par un domaine dédié, par exemple :

```text
http://keycloak.odoru.local
```

Ce choix permet de conserver une exposition minimale dans le cluster local : seuls le frontend et l’API Gateway sont exposés directement, tandis que Keycloak reste interne et n’est ouvert localement que pendant les tests d’authentification.

---

## 20. Communication entre les composants

Le flux principal est le suivant :

```text
Utilisateur
   |
   v
front
   |
   v
keycloak
   |
   v
front
   |
   v
api-gateway
   |
   +--> member-service
   +--> course-service
   +--> competition-service
   +--> badge-service
   +--> statistics-service
```

Les microservices récupèrent leur configuration via :

```text
config-server
```

Ils s’enregistrent auprès de :

```text
discovery-service
```

L’authentification repose sur :

```text
keycloak
```

Les services métier accèdent à leurs bases PostgreSQL respectives via des services internes de type `ClusterIP`.

---

## 21. Configuration standard et production

Deux fichiers de configuration sont disponibles.

Configuration standard :

```text
infrastructure/helm/odoru/values.yaml
```

Configuration production :

```text
infrastructure/helm/odoru/values-prod.yaml
```

La configuration production active notamment :

```text
NetworkPolicy
HPA
Ingress
RBAC
ServiceAccount
resources
probes
```

---

## 22. Sécurité Kubernetes

Les éléments de sécurité suivants sont présents dans le chart Helm.

| Élément | Statut | Description |
| --- | --- | --- |
| ServiceAccount | Implémenté | ServiceAccount dédié `odoru-app` |
| RBAC | Implémenté | Role et RoleBinding |
| NetworkPolicy | Implémenté | Politique default-deny et autorisations internes |
| Secrets Kubernetes | Implémenté | Secrets PostgreSQL et Keycloak |
| Resources | Implémenté | `requests` et `limits` CPU/mémoire |
| Probes | Implémenté | `readinessProbe`, `livenessProbe`, `startupProbe` pour Keycloak |
| HPA | Implémenté | Autoscaling horizontal |
| Ingress | Implémenté | Ingress Traefik avec `odoru.local` |

---

## 23. NetworkPolicy

La configuration production génère plusieurs `NetworkPolicy`.

| NetworkPolicy | Rôle |
| --- | --- |
| `odoru-default-deny-ingress` | Bloque les flux entrants par défaut |
| `odoru-allow-same-namespace` | Autorise les communications internes au namespace |
| `odoru-allow-ingress-controller` | Autorise l’accès via l’Ingress Controller |

Cette configuration permet de documenter une logique réseau plus contrôlée que le comportement Kubernetes par défaut.

---

## 24. Gestion des secrets

Les secrets Kubernetes sont générés par :

```text
infrastructure/helm/odoru/templates/secrets.yaml
```

Secrets créés :

```text
odoru-postgres-secret
odoru-keycloak-secret
```

Ils contiennent :

```text
POSTGRES_USER
POSTGRES_PASSWORD
KEYCLOAK_ADMIN
KEYCLOAK_ADMIN_PASSWORD
```

Ce rendu utilise des `Secret` Kubernetes, conformément au critère :

```text
Vault/ESO ou Secret Kubernetes
```

Dans un contexte de production réel, une solution comme Vault ou External Secrets Operator permettrait une gestion plus centralisée des secrets.

---

## 25. Probes et ressources

Tous les composants principaux disposent de :

- `readinessProbe` ;
- `livenessProbe` ;
- `resources.requests` ;
- `resources.limits`.

Les services Spring Boot utilisent :

```text
/actuator/health
```

Les bases PostgreSQL et Keycloak utilisent des probes TCP.

Keycloak dispose également d’une `startupProbe`. Ce choix permet d’éviter que Kubernetes redémarre Keycloak trop tôt pendant sa phase de démarrage.

Les probes permettent à Kubernetes de vérifier :

- si un pod est prêt à recevoir du trafic ;
- si un pod doit être redémarré en cas de blocage ;
- si un service est disponible après démarrage.

Les ressources CPU/mémoire permettent de déclarer :

- une consommation minimale (`requests`) ;
- une limite maximale (`limits`).

---

## 26. HPA

La configuration production génère des `HorizontalPodAutoscaler` pour :

```text
api-gateway
member-service
course-service
competition-service
badge-service
statistics-service
```

Paramètres principaux :

```yaml
minReplicas: 1
maxReplicas: 3
targetCPUUtilizationPercentage: 70
```

En environnement local Docker Desktop, les HPA nécessitent `metrics-server` pour exploiter les métriques CPU. Sans `metrics-server`, les objets HPA existent mais les métriques CPU peuvent apparaître comme non disponibles.

---

## 27. Ingress

La configuration production génère un Ingress :

```text
odoru-ingress
```

Host configuré :

```text
odoru.local
```

Routes :

```text
/      -> front
/api   -> api-gateway
```

Le chart conserve également les NodePort pour faciliter les tests locaux :

```text
http://localhost:30081
http://localhost:30080
```

---

## 28. Validation Helm

Validation du chart standard :

```bash
helm lint infrastructure/helm/odoru
helm template odoru infrastructure/helm/odoru > /tmp/odoru-rendered.yaml
helm install odoru infrastructure/helm/odoru --dry-run --debug
```

Résultats attendus :

```text
1 chart(s) linted, 0 chart(s) failed
STATUS: pending-install
```

Validation du chart avec `values-prod.yaml` :

```bash
helm lint infrastructure/helm/odoru -f infrastructure/helm/odoru/values-prod.yaml

helm template odoru infrastructure/helm/odoru \
  -f infrastructure/helm/odoru/values-prod.yaml \
  > /tmp/odoru-prod-rendered.yaml

helm install odoru infrastructure/helm/odoru \
  -f infrastructure/helm/odoru/values-prod.yaml \
  --dry-run --debug
```

Résultats attendus :

```text
1 chart(s) linted, 0 chart(s) failed
STATUS: pending-install
```

La version production génère bien :

```text
NetworkPolicy
ServiceAccount
Role
RoleBinding
HorizontalPodAutoscaler
Ingress
```

Commande de vérification :

```bash
grep -n "kind: NetworkPolicy\|kind: HorizontalPodAutoscaler\|kind: Ingress\|kind: ServiceAccount\|kind: Role\|kind: RoleBinding" /tmp/odoru-prod-rendered.yaml
```

---

## 29. Déploiement Kubernetes sans ArgoCD

Pour un déploiement direct avec Helm, utiliser :

```bash
helm upgrade --install odoru infrastructure/helm/odoru \
  -f infrastructure/helm/odoru/values-prod.yaml
```

Vérification :

```bash
kubectl get pods -n odoru
kubectl get deployments -n odoru
kubectl get svc -n odoru
```

Résultat attendu :

```text
Tous les pods en 1/1 Running
Tous les deployments en 1/1
front exposé en NodePort 30081
api-gateway exposé en NodePort 30080
keycloak en ClusterIP 8080
```

---

## 30. Attention sur Helm et ArgoCD

Une fois ArgoCD installé et l’application Odoru synchronisée par ArgoCD, il ne faut plus appliquer les changements directement avec :

```bash
helm upgrade --install odoru ...
```

Dans ce mode GitOps, ArgoCD devient la source de vérité. La bonne procédure est :

```text
Modifier les fichiers du chart Helm
        ↓
git add / commit / push
        ↓
ArgoCD détecte le changement
        ↓
ArgoCD synchronise le cluster
```

Ce point évite des conflits de propriété entre des ressources créées par ArgoCD et des ressources qu’Helm essaierait ensuite de reprendre directement.

---

# Automatisation Keycloak et données métier

## 31. Rôle de l’automatisation

Le déploiement Kubernetes lance les pods, les services, les bases PostgreSQL et Keycloak. Cependant, Keycloak doit aussi contenir :

- un realm ;
- un client OAuth2/OpenID Connect ;
- des rôles ;
- des utilisateurs ;
- des redirections frontend.

De plus, l’application Odoru possède aussi une base métier. Un utilisateur peut donc exister dans Keycloak sans être reconnu par l’application si son profil métier n’existe pas dans `member-service`.

Deux scripts ont été ajoutés pour automatiser cette initialisation :

```text
scripts/setup-keycloak-odoru.sh
scripts/seed-kubernetes-data.sh
```

Ces scripts rendent le projet reproductible après un déploiement Kubernetes.

---

## 32. Accès local à Keycloak pour le navigateur

Pour tester l’application depuis un navigateur Windows, le port-forward Keycloak doit être lancé depuis **PowerShell Windows** :

```powershell
kubectl config use-context docker-desktop
kubectl port-forward -n odoru svc/keycloak 8090:8080
```

La fenêtre PowerShell doit rester ouverte pendant toute la durée du test applicatif.

Résultat attendu :

```text
Forwarding from 127.0.0.1:8090 -> 8080
Forwarding from [::1]:8090 -> 8080
```

Vérification dans le navigateur :

```text
http://localhost:8090/realms/odoru/.well-known/openid-configuration
```

Résultat attendu : une réponse JSON Keycloak contenant notamment :

```text
issuer: http://localhost:8090/realms/odoru
```

Important : si cette fenêtre PowerShell est fermée, le frontend peut encore être accessible sur `http://localhost:30081`, mais la redirection d’authentification vers Keycloak échouera avec :

```text
ERR_CONNECTION_REFUSED
```

---

## 33. Script `setup-keycloak-odoru.sh`

Le script `setup-keycloak-odoru.sh` initialise automatiquement la configuration Keycloak nécessaire à Odoru.

Commande :

```bash
./scripts/setup-keycloak-odoru.sh
```

Il réalise les opérations suivantes :

| Étape | Description |
| --- | --- |
| Récupération du token admin | Connexion à Keycloak avec le compte administrateur |
| Création du realm | Création du realm `odoru` s’il n’existe pas |
| Création des rôles | Création des rôles `MEMBER`, `SECRETARY`, `TEACHER`, `PRESIDENT` |
| Configuration du client | Création ou mise à jour du client `odoru-front` |
| Configuration OAuth2/OIDC | Définition des redirect URIs et web origins |
| Création des utilisateurs | Création des utilisateurs de démonstration |
| Attribution des rôles | Association des rôles Keycloak aux utilisateurs |
| Test de token | Vérification de la récupération d’un token utilisateur |

Configuration créée :

```text
Realm        : odoru
Client       : odoru-front
Redirect URI : http://localhost:30081/*
Web Origin   : http://localhost:30081
```

Utilisateurs créés :

| Utilisateur | Mot de passe | Rôles Keycloak |
| --- | --- | --- |
| `lea.martin` | `secret123` | MEMBER |
| `sara.bernard` | `secret123` | MEMBER + SECRETARY |
| `marc.durand` | `secret123` | MEMBER + TEACHER |
| `paul.moreau` | `secret123` | MEMBER + PRESIDENT |

Ce script évite de configurer manuellement Keycloak via l’interface d’administration.

---

## 34. Script `seed-kubernetes-data.sh`

Le script `seed-kubernetes-data.sh` initialise les données métier nécessaires dans l’application Odoru.

Keycloak gère l’identité et les rôles applicatifs, mais les utilisateurs doivent également exister dans la base métier du `member-service`.

Ce script assure donc la cohérence suivante :

```text
Utilisateurs Keycloak
        |
        v
Profils métier member-service
        |
        v
Base PostgreSQL member-postgres
```

Commande :

```bash
./scripts/seed-kubernetes-data.sh
```

Il réalise les opérations suivantes :

| Étape | Description |
| --- | --- |
| Attente du `member-service` | Vérifie que le deployment `member-service` est disponible |
| Port-forward temporaire | Ouvre un accès local au service métier |
| Vérification des membres | Vérifie si chaque membre existe déjà |
| Création si nécessaire | Crée les membres absents |
| Affichage final | Liste les membres disponibles |
| Nettoyage | Arrête le port-forward temporaire |

Membres métier vérifiés ou créés :

```text
lea.martin
sara.bernard
marc.durand
paul.moreau
```

Ce script évite une incohérence entre un utilisateur authentifié dans Keycloak et un profil métier absent dans l’application.

---

## 35. Ordre recommandé après le déploiement Kubernetes

Terminal PowerShell Windows :

```powershell
kubectl config use-context docker-desktop
kubectl port-forward -n odoru svc/keycloak 8090:8080
```

Terminal WSL ou PowerShell séparé :

```bash
./scripts/setup-keycloak-odoru.sh
./scripts/seed-kubernetes-data.sh
```

Vérification Keycloak :

```text
http://localhost:8090/realms/odoru/.well-known/openid-configuration
```

L’application peut ensuite être testée avec :

```text
http://localhost:30081
```

Compte principal de démonstration :

```text
lea.martin / secret123
```

---

## 36. Validation backend complète

Récupération d’un token :

```bash
TOKEN=$(curl -s -X POST "http://localhost:8090/realms/odoru/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=odoru-front" \
  -d "username=lea.martin" \
  -d "password=secret123" | python3 -c "import sys,json; print(json.load(sys.stdin).get('access_token',''))")

echo "TOKEN_SIZE=${#TOKEN}"
echo "$TOKEN" | awk -F. '{print "JWT_PARTS=" NF}'
```

Résultat attendu :

```text
TOKEN_SIZE=1395
JWT_PARTS=3
```

Test de l’API Gateway :

```bash
curl -i -H "Authorization: Bearer $TOKEN" \
  http://localhost:30080/api/members/username/lea.martin
```

Résultat attendu :

```text
HTTP/1.1 200 OK
```

Exemple de réponse :

```json
{
  "id": 1,
  "lastName": "Martin",
  "firstName": "Lea",
  "email": "lea.martin@example.com",
  "username": "lea.martin",
  "city": "Toulouse",
  "country": "France",
  "expertiseLevel": 1,
  "registrationStatus": "PENDING_REVIEW",
  "membershipFeePaid": false,
  "medicalCertificateProvided": false,
  "registrationCheckedBySecretary": false,
  "roles": ["MEMBER"]
}
```

Ce test valide la chaîne suivante :

```text
Keycloak
   -> token JWT
      -> API Gateway
         -> member-service
            -> member-postgres
```

---

# Accès applicatif

## 37. Accéder à l’application

Une fois les pods démarrés, Keycloak initialisé et les données métier créées, l’application est accessible via :

```text
http://localhost:30081
```

Compte de test principal :

```text
lea.martin / secret123
```

Le scénario attendu est le suivant :

```text
1. Le navigateur ouvre le frontend sur http://localhost:30081
2. Le frontend redirige vers Keycloak sur http://localhost:8090
3. L’utilisateur se connecte avec lea.martin / secret123
4. Keycloak redirige vers le frontend
5. Le frontend appelle l’API Gateway sur http://localhost:30080
6. L’API Gateway valide le token JWT
7. L’API Gateway transmet la requête au member-service
8. Le member-service retourne le profil métier depuis PostgreSQL
```

Preuve fonctionnelle attendue dans l’interface :

```text
Utilisateur : Lea Martin
Username    : @lea.martin
ID métier   : 1
Rôle        : MEMBER
```

---

## 38. Ports utilisés pendant le test local

| URL | Rôle | Origine |
| --- | --- | --- |
| `http://localhost:30081` | Frontend Odoru | Service Kubernetes `front` exposé en NodePort |
| `http://localhost:30080` | API Gateway | Service Kubernetes `api-gateway` exposé en NodePort |
| `http://localhost:8090` | Keycloak | Port-forward local vers le service interne `keycloak` |

Le lien `http://localhost:8090` n’est disponible que si le port-forward Keycloak est actif. Il faut donc conserver ouverte la fenêtre PowerShell contenant :

```powershell
kubectl port-forward -n odoru svc/keycloak 8090:8080
```

---

## 39. Vérifications utiles en cas de problème

Vérifier les pods :

```bash
kubectl get pods -n odoru
kubectl get deployments -n odoru
```

Vérifier les services :

```bash
kubectl get svc -n odoru
```

Vérifier Keycloak depuis le navigateur :

```text
http://localhost:8090/realms/odoru/.well-known/openid-configuration
```

Vérifier l’API Gateway avec un token :

```bash
curl -i -H "Authorization: Bearer $TOKEN" \
  http://localhost:30080/api/members/username/lea.martin
```

Vérifier les logs :

```bash
kubectl logs -n odoru deployment/api-gateway --tail=100
kubectl logs -n odoru deployment/member-service --tail=100
kubectl logs -n odoru deployment/keycloak --tail=100
```

---

# GitOps avec ArgoCD

## 40. Manifeste ArgoCD

Le manifeste ArgoCD est disponible dans :

```text
infrastructure/argocd/odoru-application.yaml
```

Contenu principal :

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: odoru
  namespace: argocd
spec:
  project: default

  source:
    repoURL: https://github.com/hachimkidjei/odoru.git
    targetRevision: main
    path: infrastructure/helm/odoru
    helm:
      valueFiles:
        - values.yaml
        - values-prod.yaml

  destination:
    server: https://kubernetes.default.svc
    namespace: odoru

  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
      - CreateNamespace=true
```

---

## 41. Installation ArgoCD

ArgoCD a été installé dans le namespace `argocd` :

```bash
kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -

kubectl apply -n argocd \
  -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```

Vérification :

```bash
kubectl get pods -n argocd
```

Résultat attendu :

```text
argocd-application-controller     1/1 Running
argocd-applicationset-controller  1/1 Running
argocd-dex-server                 1/1 Running
argocd-notifications-controller   1/1 Running
argocd-redis                      1/1 Running
argocd-repo-server                1/1 Running
argocd-server                     1/1 Running
```

Vérification de la CRD `Application` :

```bash
kubectl api-resources | grep applications
```

Résultat attendu :

```text
applications app,apps argoproj.io/v1alpha1 true Application
```

---

## 42. Application ArgoCD Odoru

Application du manifeste :

```bash
kubectl apply -f infrastructure/argocd/odoru-application.yaml
```

Vérification :

```bash
kubectl get application odoru -n argocd
```

Résultat attendu :

```text
NAME    SYNC STATUS   HEALTH STATUS
odoru   Synced        Progressing
```

Le statut `Synced` confirme que l’état déclaré dans GitHub est synchronisé avec le cluster Kubernetes.

Le statut `Progressing` concerne l’état de santé applicatif observé par ArgoCD au moment du test. Il peut apparaître pendant :

- le redéploiement des pods ;
- l’évaluation des probes ;
- l’utilisation de HPA sans `metrics-server` dans un environnement local.

---

## 43. Démonstration de dérive ArgoCD

Une dérive volontaire peut être créée en modifiant manuellement le nombre de replicas du microservice `member-service` :

```bash
kubectl scale deployment member-service -n odoru --replicas=2
```

Grâce à la configuration :

```yaml
syncPolicy:
  automated:
    prune: true
    selfHeal: true
```

ArgoCD détecte la dérive et réconcilie l’état réel du cluster avec l’état déclaré dans Git.

Preuve attendue dans les événements ArgoCD :

```text
Updated sync status: Synced -> OutOfSync
Partial sync operation ... succeeded
Updated sync status: OutOfSync -> Synced
```

Vérification finale :

```bash
kubectl get application odoru -n argocd -o jsonpath='{.status.sync.status}{"\n"}'
kubectl get deployment member-service -n odoru -o jsonpath='{.spec.replicas}{"\n"}'
```

Résultat attendu :

```text
Synced
1
```

Cela valide le mécanisme GitOps attendu : une modification manuelle du cluster est détectée comme dérive, puis corrigée automatiquement par ArgoCD grâce à `selfHeal`.

---

# Procédure complète de reproduction

## 44. Cloner le dépôt

```bash
git clone https://github.com/hachimkidjei/odoru.git
cd odoru
```

---

## 45. Vérifier l’environnement

```bash
git status
kubectl config use-context docker-desktop
kubectl get nodes
docker ps
helm version
buildah --version
trivy --version
dive --version
java -version
node -v
npm -v
```

---

## 46. Construire les services Java

```bash
cd services/config-server && chmod +x mvnw && ./mvnw clean package -DskipTests && cd ../..
cd services/discovery-service && chmod +x mvnw && ./mvnw clean package -DskipTests && cd ../..
cd services/api-gateway && chmod +x mvnw && ./mvnw clean package -DskipTests && cd ../..
cd services/member-service && chmod +x mvnw && ./mvnw clean package -DskipTests && cd ../..
cd services/course-service && chmod +x mvnw && ./mvnw clean package -DskipTests && cd ../..
cd services/competition-service && chmod +x mvnw && ./mvnw clean package -DskipTests && cd ../..
cd services/badge-service && chmod +x mvnw && ./mvnw clean package -DskipTests && cd ../..
cd services/statistics-service && chmod +x mvnw && ./mvnw clean package -DskipTests && cd ../..
```

---

## 47. Construire le frontend

```bash
cd odoru-front
npm install
npm run build
cd ..
```

---

## 48. Construire les images OCI avec Buildah

```bash
chmod +x scripts/*.sh
./scripts/build-all.sh 1.0.0
```

Vérification :

```bash
buildah images | grep odoru
```

---

## 49. Analyser les images avec Trivy

```bash
./scripts/scan-trivy.sh 1.0.0
```

Rapports :

```text
build-reports/trivy
```

---

## 50. Analyser les images avec Dive

```bash
./scripts/analyze-dive.sh 1.0.0
```

Rapports :

```text
build-reports/dive
```

---

## 51. Charger les images dans Docker Desktop

```bash
docker load -i build-reports/oci/config-server-1.0.0.tar
docker load -i build-reports/oci/discovery-service-1.0.0.tar
docker load -i build-reports/oci/api-gateway-1.0.0.tar
docker load -i build-reports/oci/member-service-1.0.0.tar
docker load -i build-reports/oci/course-service-1.0.0.tar
docker load -i build-reports/oci/competition-service-1.0.0.tar
docker load -i build-reports/oci/badge-service-1.0.0.tar
docker load -i build-reports/oci/statistics-service-1.0.0.tar
docker load -i build-reports/oci/front-1.0.0.tar
```

Vérification :

```bash
docker images | grep odoru
```

---

## 52. Déployer avec Helm

Déploiement direct :

```bash
helm upgrade --install odoru infrastructure/helm/odoru \
  -f infrastructure/helm/odoru/values-prod.yaml
```

Vérification :

```bash
kubectl get pods -n odoru
kubectl get deployments -n odoru
kubectl get svc -n odoru
```

---

## 53. Initialiser Keycloak et les données métier

Dans PowerShell Windows :

```powershell
kubectl config use-context docker-desktop
kubectl port-forward -n odoru svc/keycloak 8090:8080
```

Dans un autre terminal :

```bash
./scripts/setup-keycloak-odoru.sh
./scripts/seed-kubernetes-data.sh
```

---

## 54. Vérifier l’accès applicatif

Vérifier Keycloak dans le navigateur :

```text
http://localhost:8090/realms/odoru/.well-known/openid-configuration
```

Vérifier le frontend :

```text
http://localhost:30081
```

Compte :

```text
lea.martin / secret123
```

Vérifier l’API Gateway :

```bash
TOKEN=$(curl -s -X POST "http://localhost:8090/realms/odoru/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=odoru-front" \
  -d "username=lea.martin" \
  -d "password=secret123" | python3 -c "import sys,json; print(json.load(sys.stdin).get('access_token',''))")

echo "TOKEN_SIZE=${#TOKEN}"
echo "$TOKEN" | awk -F. '{print "JWT_PARTS=" NF}'

curl -i -H "Authorization: Bearer $TOKEN" \
  http://localhost:30080/api/members/username/lea.martin
```

Résultat attendu :

```text
TOKEN_SIZE=1395
JWT_PARTS=3
HTTP/1.1 200 OK
```

---

# Couverture des critères d’évaluation

## 55. Critères d’évaluation — Partie A

| Critère | Pondération |
| --- | --- |
| Analyse comparative Docker / Buildah | 15% |
| Chaîne de build Buildah fonctionnelle | 25% |
| Rapport Trivy + plan de remédiation | 20% |
| Analyse Dive + optimisations | 15% |
| Script de build intégré et documenté | 25% |

## 56. Couverture Partie A dans Odoru

| Critère | Couverture |
| --- | --- |
| Analyse comparative Docker / Buildah | Présente dans le README |
| Chaîne de build Buildah fonctionnelle | `scripts/build-all.sh` |
| Rapport Trivy + plan de remédiation | `build-reports/trivy` |
| Analyse Dive + optimisations | `build-reports/dive` |
| Script de build intégré et documenté | `build-all.sh`, `scan-trivy.sh`, `analyze-dive.sh` |

---

## 57. Critères d’évaluation — Partie B

| Critère | Pondération |
| --- | --- |
| Chart Helm complet et conforme | 30% |
| Déploiement Kubernetes avec sécurité : RBAC, NetworkPolicy, HPA | 25% |
| Gestion des secrets : Vault/ESO ou Secret Kubernetes | 20% |
| GitOps ArgoCD fonctionnel avec démonstration de dérive | 25% |

## 58. Couverture Partie B dans Odoru

| Critère | Couverture |
| --- | --- |
| Chart Helm complet et conforme | Chart `infrastructure/helm/odoru`, validé par `helm lint`, `helm template`, `dry-run` |
| Sécurité Kubernetes | ServiceAccount, RBAC, NetworkPolicy, HPA, resources, probes |
| Gestion des secrets | Secrets Kubernetes PostgreSQL et Keycloak |
| GitOps ArgoCD | Manifeste ArgoCD, sync automatique, `prune`, `selfHeal`, démo dérive `OutOfSync -> Synced` |

---

# Points d’attention liés à l’environnement local

## 59. Points d’attention

Le déploiement a été réalisé dans un environnement Kubernetes local basé sur Docker Desktop. Certains comportements peuvent donc dépendre des composants disponibles dans ce cluster local.

Points d’attention identifiés :

- les `HorizontalPodAutoscaler` nécessitent un `metrics-server` pour exploiter les métriques CPU ;
- le statut `Health` ArgoCD peut apparaître temporairement `Progressing` pendant le démarrage ou le redéploiement des pods ;
- l’Ingress `odoru.local` nécessite un contrôleur Ingress compatible, par exemple Traefik, ainsi qu’une résolution locale du nom de domaine ;
- les secrets sont gérés avec des `Secret` Kubernetes pour répondre au périmètre du TP ;
- dans un contexte de production réel, une solution comme Vault ou External Secrets Operator serait plus adaptée pour la gestion centralisée des secrets ;
- Keycloak est déployé en `ClusterIP` et nécessite un port-forward local pour être utilisé depuis le navigateur pendant les tests.

Ces points sont documentés pour faciliter la reproduction et l’interprétation des résultats dans un environnement Kubernetes local.

---

## 60. Point spécifique sur Keycloak

Dans ce rendu, Keycloak est déployé en `ClusterIP`. Ce choix limite l’exposition réseau directe du fournisseur d’identité dans le cluster local.

Conséquence : pour tester le flux d’authentification depuis le navigateur Windows, il faut ouvrir un port-forward temporaire :

```powershell
kubectl port-forward -n odoru svc/keycloak 8090:8080
```

Ce port-forward est uniquement un mécanisme de test local. Il ne déploie pas l’application et ne remplace pas Helm. Il permet simplement au navigateur d’accéder au service Keycloak déjà présent dans Kubernetes.

En environnement de production, cette exposition serait remplacée par une solution plus stable :

- Ingress dédié pour Keycloak ;
- nom de domaine local ou réel ;
- configuration TLS ;
- persistance dédiée de Keycloak ;
- gestion centralisée des secrets.

---

# Conclusion

Ce rendu démontre l’adaptation complète du TP Buildah, Trivy, Dive et Helm/Kubernetes au projet microservices Odoru.

La Partie A couvre :

- l’analyse comparative Docker / Buildah ;
- la construction des images OCI avec Buildah ;
- l’export des images ;
- les scans Trivy ;
- les analyses Dive ;
- l’automatisation via scripts.

La Partie B couvre :

- un chart Helm complet ;
- un déploiement Kubernetes structuré ;
- des services NodePort et ClusterIP ;
- des secrets Kubernetes ;
- RBAC ;
- NetworkPolicy ;
- probes ;
- resources ;
- HPA ;
- Ingress ;
- une configuration `values-prod.yaml` ;
- une synchronisation GitOps avec ArgoCD ;
- une démonstration de dérive corrigée automatiquement ;
- une initialisation automatisée de Keycloak et des données métier.

Le test final valide la chaîne complète :

```text
Navigateur
   -> Frontend Odoru
      -> Keycloak
         -> Token JWT
            -> API Gateway
               -> member-service
                  -> PostgreSQL
```

La réponse `HTTP/1.1 200 OK` obtenue sur :

```text
http://localhost:30080/api/members/username/lea.martin
```

ainsi que l’accès visuel au tableau de bord Odoru sur :

```text
http://localhost:30081
```

confirment le fonctionnement applicatif de bout en bout.

---

## Déploiement local complet avec Kubernetes, Helm et Docker Desktop

Cette section décrit la procédure permettant de reconstruire et déployer l’application Odoru sur un poste local équipé de Docker Desktop avec Kubernetes activé.

L’application est déployée dans Kubernetes avec Helm. Deux services sont exposés en `NodePort` :

| Composant | Adresse locale |
|---|---|
| Frontend React | http://localhost:30081 |
| API Gateway | http://localhost:30080 |

Keycloak est déployé dans le cluster en `ClusterIP`. Il n’est donc pas exposé directement à l’extérieur du cluster. Pour permettre au navigateur et aux scripts d’initialisation d’y accéder, il faut ouvrir un port-forward local :

```bash
kubectl port-forward -n odoru svc/keycloak 8090:8080
L’adresse Keycloak utilisée par le frontend est donc :

http://localhost:8090
1. Récupérer le projet
git clone https://github.com/hachimkidjei/odoru.git
cd odoru

Si le projet est déjà cloné :

git pull
git status

Résultat attendu :

On branch main
Your branch is up to date with 'origin/main'.

nothing to commit, working tree clean
2. Vérifier les prérequis
kubectl config use-context docker-desktop
kubectl get nodes

docker --version
helm version
buildah --version
trivy --version
dive --version
java -version
node -v
npm -v

Résultat attendu pour Kubernetes :

docker-desktop   Ready
3. Vérifier la configuration frontend

Le frontend utilise Vite. Les variables d’environnement nécessaires au build sont définies dans :

odoru-front/.env.production

Contenu attendu :

VITE_KEYCLOAK_URL=http://localhost:8090
VITE_KEYCLOAK_REALM=odoru
VITE_KEYCLOAK_CLIENT_ID=odoru-front
VITE_API_BASE_URL=http://localhost:30080

Ces valeurs ne sont pas des secrets. Elles servent uniquement à indiquer au frontend où trouver Keycloak et l’API Gateway en local.

Vérification :

cat odoru-front/.env.production
4. Construire le frontend
cd odoru-front
npm install
npm run build
cd ..

Vérification du build :

grep -R "localhost:8090\|odoru-front\|localhost:30080" -n odoru-front/dist/assets | head

Le résultat doit contenir des références à :

localhost:8090
odoru-front
localhost:30080
5. Construire les services Java
cd services/config-server && chmod +x mvnw && ./mvnw clean package -DskipTests && cd ../..
cd services/discovery-service && chmod +x mvnw && ./mvnw clean package -DskipTests && cd ../..
cd services/api-gateway && chmod +x mvnw && ./mvnw clean package -DskipTests && cd ../..
cd services/member-service && chmod +x mvnw && ./mvnw clean package -DskipTests && cd ../..
cd services/course-service && chmod +x mvnw && ./mvnw clean package -DskipTests && cd ../..
cd services/competition-service && chmod +x mvnw && ./mvnw clean package -DskipTests && cd ../..
cd services/badge-service && chmod +x mvnw && ./mvnw clean package -DskipTests && cd ../..
cd services/statistics-service && chmod +x mvnw && ./mvnw clean package -DskipTests && cd ../..
6. Construire les images avec Buildah
chmod +x scripts/*.sh
./scripts/build-all.sh 1.0.0

Vérification :

buildah images | grep odoru

Résultat attendu :

localhost/odoru/config-server
localhost/odoru/discovery-service
localhost/odoru/api-gateway
localhost/odoru/member-service
localhost/odoru/course-service
localhost/odoru/competition-service
localhost/odoru/badge-service
localhost/odoru/statistics-service
localhost/odoru/front
7. Transférer les images Buildah vers Docker Desktop

Docker Desktop Kubernetes utilise les images disponibles dans le moteur Docker local. Les images construites avec Buildah doivent donc être poussées vers Docker :

buildah push localhost/odoru/config-server:1.0.0 docker-daemon:localhost/odoru/config-server:1.0.0
buildah push localhost/odoru/discovery-service:1.0.0 docker-daemon:localhost/odoru/discovery-service:1.0.0
buildah push localhost/odoru/api-gateway:1.0.0 docker-daemon:localhost/odoru/api-gateway:1.0.0
buildah push localhost/odoru/member-service:1.0.0 docker-daemon:localhost/odoru/member-service:1.0.0
buildah push localhost/odoru/course-service:1.0.0 docker-daemon:localhost/odoru/course-service:1.0.0
buildah push localhost/odoru/competition-service:1.0.0 docker-daemon:localhost/odoru/competition-service:1.0.0
buildah push localhost/odoru/badge-service:1.0.0 docker-daemon:localhost/odoru/badge-service:1.0.0
buildah push localhost/odoru/statistics-service:1.0.0 docker-daemon:localhost/odoru/statistics-service:1.0.0
buildah push localhost/odoru/front:1.0.0 docker-daemon:localhost/odoru/front:1.0.0

Vérification :

docker images | grep odoru
8. Vérifier le chart Helm
helm lint infrastructure/helm/odoru
helm lint infrastructure/helm/odoru -f infrastructure/helm/odoru/values-prod.yaml

Résultat attendu :

1 chart(s) linted, 0 chart(s) failed
9. Déployer l’application avec Helm

Pour un premier déploiement ou une mise à jour :

helm upgrade --install odoru infrastructure/helm/odoru \
  -f infrastructure/helm/odoru/values-prod.yaml

Pour repartir d’un environnement propre :

helm uninstall odoru --ignore-not-found
kubectl delete namespace odoru --ignore-not-found

Attendre que le namespace soit supprimé :

kubectl get namespace odoru

Si la commande retourne NotFound, c’est normal : le namespace a bien été supprimé.

Puis redéployer :

helm upgrade --install odoru infrastructure/helm/odoru \
  -f infrastructure/helm/odoru/values-prod.yaml
10. Attendre le démarrage des services
kubectl rollout status deployment/config-server -n odoru --timeout=300s
kubectl rollout status deployment/discovery-service -n odoru --timeout=300s
kubectl rollout status deployment/keycloak -n odoru --timeout=600s
kubectl rollout status deployment/member-service -n odoru --timeout=300s
kubectl rollout status deployment/course-service -n odoru --timeout=300s
kubectl rollout status deployment/competition-service -n odoru --timeout=300s
kubectl rollout status deployment/badge-service -n odoru --timeout=300s
kubectl rollout status deployment/statistics-service -n odoru --timeout=300s
kubectl rollout status deployment/api-gateway -n odoru --timeout=300s
kubectl rollout status deployment/front -n odoru --timeout=300s

Vérification globale :

kubectl get pods -n odoru
kubectl get deployments -n odoru
kubectl get svc -n odoru

Résultat attendu :

Tous les pods        1/1 Running
Tous les deployments 1/1
11. Ouvrir l’accès local à Keycloak

Keycloak est exposé en interne dans Kubernetes avec un service ClusterIP. Pour l’utiliser depuis le navigateur et depuis les scripts locaux, ouvrir un port-forward :

kubectl port-forward -n odoru svc/keycloak 8090:8080

Garder ce terminal ouvert.

Vérification dans un deuxième terminal :

curl -i http://localhost:8090
curl -i http://localhost:8090/realms/master

Résultats attendus :

HTTP/1.1 302 Found

et :

HTTP/1.1 200 OK
12. Initialiser Keycloak

Dans un deuxième terminal, depuis la racine du projet :

./scripts/setup-keycloak-odoru.sh

Ce script initialise :

le realm odoru ;
les rôles MEMBER, SECRETARY, TEACHER, PRESIDENT ;
le client public odoru-front ;
les utilisateurs de démonstration.

Résultat attendu :

Token admin récupéré.
Realm odoru créé.
Rôle MEMBER créé.
Rôle SECRETARY créé.
Rôle TEACHER créé.
Rôle PRESIDENT créé.
Client odoru-front créé.
Token utilisateur OK.

Si les éléments existent déjà, le script les met à jour.

Vérifier le realm :

curl -i http://localhost:8090/realms/odoru/.well-known/openid-configuration

Résultat attendu :

HTTP/1.1 200 OK
13. Initialiser les données métier
./scripts/seed-kubernetes-data.sh

Ce script initialise les membres métier dans member-service.

Résultat attendu :

Seed métier terminé.
Comptes métier disponibles :
  lea.martin
  sara.bernard
  marc.durand
  paul.moreau
14. Tester l’authentification Keycloak
TOKEN=$(curl -s -X POST "http://localhost:8090/realms/odoru/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=odoru-front" \
  -d "username=lea.martin" \
  -d "password=secret123" | python3 -c "import sys,json; print(json.load(sys.stdin).get('access_token',''))")

echo "TOKEN_SIZE=${#TOKEN}"
echo "$TOKEN" | awk -F. '{print "JWT_PARTS=" NF}'

Résultat attendu :

TOKEN_SIZE=...
JWT_PARTS=3
15. Tester l’API Gateway
curl -i -H "Authorization: Bearer $TOKEN" \
  http://localhost:30080/api/members/username/lea.martin

Résultat attendu :

HTTP/1.1 200 OK

Exemple de réponse :

{
  "id": 1,
  "lastName": "Martin",
  "firstName": "Lea",
  "email": "lea.martin@example.com",
  "username": "lea.martin",
  "city": "Toulouse",
  "country": "France",
  "expertiseLevel": 1,
  "roles": ["MEMBER"]
}
16. Accéder à l’application Web

Garder le port-forward Keycloak actif :

kubectl port-forward -n odoru svc/keycloak 8090:8080

Puis ouvrir dans le navigateur :

http://localhost:30081

Si le navigateur garde une ancienne version du frontend en cache, faire :

CTRL + F5

ou ouvrir une fenêtre de navigation privée.

17. Comptes de démonstration
Utilisateur	Mot de passe	Rôles
lea.martin	secret123	MEMBER
sara.bernard	secret123	MEMBER + SECRETARY
marc.durand	secret123	MEMBER + TEACHER
paul.moreau	secret123	MEMBER + PRESIDENT

Exemple attendu après connexion avec sara.bernard :

Utilisateur : Sara Bernard
Username : @sara.bernard
ID membre : 2
Rôles : SECRETARY + MEMBER
Points importants
Le frontend et l’API Gateway sont exposés par des services NodePort.
Keycloak reste interne au cluster et doit être rendu accessible localement par kubectl port-forward.
Le port-forward Keycloak doit rester ouvert pendant l’utilisation de l’application.
Les scripts setup-keycloak-odoru.sh et seed-kubernetes-data.sh doivent être exécutés après un déploiement propre, car le namespace et les bases sont recréés.
Les variables du fichier odoru-front/.env.production sont nécessaires au build du frontend. Sans elles, le frontend construit des URLs Keycloak invalides.

Tu peux ajouter juste avant cette section une phrase simple :

```markdown
La procédure ci-dessous a été testée sur deux postes différents avec Docker Desktop Kubernetes. Elle permet de reconstruire les images, de déployer les ressources Kubernetes avec Helm, d’initialiser Keycloak, d’injecter les données métier et d’accéder à l’application via le navigateur.

Et pour être cohérent avec ce que tu as constaté : ne parle pas de PowerShell comme obligation. Tu peux éventuellement ajouter une note courte :

Sous Windows avec WSL, le port-forward peut être lancé depuis WSL. Si le navigateur ne voit pas `localhost:8090`, il est aussi possible de lancer le même port-forward depuis PowerShell.

Mais dans la procédure principale, garde seulement WSL.
