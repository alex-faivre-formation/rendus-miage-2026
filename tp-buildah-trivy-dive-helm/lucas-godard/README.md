# NOM : GODARD, Prénom : Lucas

# Projet DevOps — MIAGE Bank : Buildah/Trivy/Dive & Helm/Kubernetes

Projet réalisé dans le cadre du cours **M2 MIAGE - Mise en œuvre DevOps**.
Il couvre la conteneurisation (Partie A) et le déploiement Kubernetes
GitOps (Partie B) de l'application micro-services **MIAGE-Bank**.

## Origine du projet

L'application MIAGE-Bank (micro-services Spring Cloud) provient de la correction d'une autre matière du cursus (cours
Architecture Micro-services Cloud). Le TP porte sur la
**chaîne DevOps** construite autour de cette application : build OCI, analyse de
sécurité, packaging Helm, déploiement Kubernetes et GitOps. Le code applicatif
Java n'a pas été développé ici ; un **frontend Angular** a en revanche été ajouté
pour illustrer l'application. L'application MIAGE-Bank initiale n'a pas été modifiée : des
fichiers inutiles au projet DevOps tel que le docker-compose.yml sont présents.

Le repository initial (publique) : [https://github.com/llluuu02/miage-bank-devops.git](https://github.com/llluuu02/miage-bank-devops.git)


## Où trouver la documentation et les livrables

Toute la documentation détaillée se trouve dans le dossier
**[`0-documentation/`](0-documentation)**, organisé selon le plan du sujet :

```
0-documentation/
├── PARTIE A - Chaine de build OCI avec Buildah, Trivy et Dive/
│   ├── 01 - Analyse comparative Docker vs Buildah/   (DockerVSBuildah.md)
│   ├── 02 - Build de MIAGE-Bank avec Buildah/        (build-buildah*.sh, ContainerfileVSNative.md)
│   ├── 03 - Scan de sécurité avec Trivy/             (scan-trivy.sh, Trivy.md, reports)
│   ├── 04 - Audit de l'image avec Dive/              (audit-dive.sh, Dive.md, reports)
│   └── 05 - Script de build intégré/                 (ci-miage-bank.yml, CI.md)
└── PARTIE B - Packaging Helm & Déploiement Kubernetes de MIAGE-Bank/
    ├── 01 - Chart Helm pour MIAGE-Bank/              (Helm.md + copie du chart)
    ├── 02 - Déploiement dans Kubernetes/             (Deploiement.md)
    └── 03 - GitOps avec ArgoCD/                      (GitOps.md, copie de la config argocd)
```

Les **artefacts déployés** sont à la racine du dépôt :

| Élément                     | Emplacement                         |
| --------------------------- |-------------------------------------|
| Chart Helm                  | `miage-bank/`                       |
| Application ArgoCD          | `argocd/application.yaml`           |
| Containerfile services Java | `src/Containerfile`                 |
| Containerfile frontend      | `src/Banque-Frontend/Containerfile` |
| Pipeline CI                 | `.github/workflows/`                |
| Micro-services Spring       | `src/Banque-*/`                     |
| Frontend Angular            | `src/Banque-Frontend/`              |


## Structure du projet (vue d'ensemble)

- **[`src/Banque-Annuaire`](src/Banque-Annuaire)** — Eureka (service registry)
- **[`src/Banque-ConfigServer`](src/Banque-ConfigServer)** — Spring Cloud Config
- **[`src/Banque-ClientService`](src/Banque-ClientService)** — service clients (avec MySQL)
- **[`src/Banque-CompteService`](src/Banque-CompteService)** — service comptes (avec MongoDB)
- **[`src/Banque-CompositeService`](src/Banque-CompositeService)** — agrégation client + comptes
- **[`src/Banque-APIGateway`](src/Banque-APIGateway)** — point d'entrée backend unique
- **[`src/Banque-Frontend`](src/Banque-Frontend)** — frontend Angular
- **[`miage-bank/`](miage-bank)** — chart Helm (déploiement Kubernetes)
- **[`argocd/`](argocd)** — configuration GitOps
- **[`scripts/`](scripts)** — scripts de build Buildah / scan Trivy / audit Dive
- **[`build-reports/`](build-reports)** — reports Trivy et Dive
- **[`images/`](images)** — images .tar native et containerfile version

## Démarrage rapide

Il n'y a pas de démarrage rapide pour la partie A, tout se fait automatiquement lors d'un git push.
La CI publie les images qui serviront pour la partie B.
Le guide de la partie B (prérequis, Vault/ESO, Traefik, ArgoCD) est documenté dans le fichier
[`Installation.md`](0-documentation/PARTIE%20B%20-%20Packaging%20Helm%20%26%20D%C3%A9ploiement%20Kubernetes%20de%20MIAGE-Bank/Installation.md).



## Environnement

Développé et testé sous **WSL2 Debian**(12.8) + **minikube**(1.38.1), OS Windows 11(10.0.26200.8457)
et navigateur Chrome(148.0.7778.217).

Images publiées sur **GitHub Container Registry (GHCR)**.


## Utilisation de l'IA (Claude et Gemini)
Le frontend de l'application MIAGE-Bank a entièrement été développé par l'IA.
Elle a également été utilisé dans un but de compréhension et d'aide à la rédaction de documents.
Les concepts clés du projet ont été assimilé.
