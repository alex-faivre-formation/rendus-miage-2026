# RAJAOBELISON Hanja & ALPTEKIN Eylul — MIAGE-Bank (Master MIAGE M2 ITN)

**Cours :** Cloud & Kubernetes — Session 2026
**TP :** Buildah, Trivy, Dive & Helm / Kubernetes

---

## Objectif du projet

Ce dépôt regroupe l'ensemble des travaux de conteneurisation, de sécurisation et
de déploiement de l'application **MIAGE-Bank**, une architecture micro-services
Spring Boot (Spring Cloud) pour une banque simplifiée.

Le projet est découpé en deux parties évaluées séparément :

- **Partie A** — Chaîne de build OCI : construction des images avec **Buildah**,
  scan de sécurité avec **Trivy**, audit des layers avec **Dive**, lint du
  Containerfile avec **Hadolint**, et automatisation via **GitHub Actions**.
- **Partie B** — Packaging **Helm**, déploiement **Kubernetes** (Minikube), gestion
  des secrets (Vault + External Secrets Operator), exposition via **Traefik** et
  GitOps avec **ArgoCD**.

Application source : <https://github.com/hialmar/AMSC> (version **avec sécurité Okta/Auth0**).

---

## Architecture de l'application

MIAGE-Bank est un projet Maven multi-module. Chaque micro-service produit un JAR
exécutable et est conteneurisé en une image OCI dédiée.

| Micro-service | Rôle | Port | Stockage |
|---|---|---|---|
| `amc_annuaire` | Découverte de services (Eureka) | 10001 | — |
| `amc_configserver` | Serveur de configuration (Spring Cloud Config) | 10003 | — |
| `amc_clients` | Gestion des profils clients | 10011 | MySQL (`banquebd`) |
| `amc_comptes` | Gestion des comptes bancaires | 10021 | MongoDB |
| `amc_composite` | Service composite (clients + comptes) | 10031 | — |
| `amc_proxy` | API Gateway (point d'entrée + sécurité JWT) | 10000 | — |
| `amc_front` | Frontend Angular (hors périmètre images Java) | 4200 | — |

Services d'infrastructure : MySQL, MongoDB, Zipkin (traçage), Prometheus (monitoring).

> La version utilisée intègre la sécurité **Okta/Auth0** : tous les appels transitent
> par la Gateway avec un jeton JWT (OAuth2 Resource Server). Les identifiants de bases
> et les clés Auth0 sont gérés comme des secrets (voir Partie B).

---

## Structure du dépôt

```
MIAGE-BANK-PROJET/
├── README.md                  ← ce fichier
├── ci-scripts/                ← scripts de la chaîne de build
│   ├── build-images.sh        ← build Buildah (approche Containerfile)
│   ├── build-native.sh        ← build Buildah natif (approche layer par layer)
│   ├── scan-trivy.sh          ← scan de sécurité Trivy
│   └── audit-dive.sh          ← audit des layers Dive
├── Partie A/
│   ├── 01-image-build/        ← Containerfile + analyse Docker/Buildah
│   ├── 02-security-scan/      ← rapports Trivy (JSON/SARIF) + remédiation
│   ├── 03-image-audit/        ← audit Dive + optimisations
│   └── 04-linter-check/       ← lint Hadolint
├── Partie B/                  ← chart Helm, manifests K8s, ArgoCD
├── .github/workflows/         ← pipeline CI/CD (bonus)
└── src/AMSC/                  ← code source des micro-services
```

---

## Livrables — Partie A

| # | Section | Concepts clés | Lien |
| :--- | :--- | :--- | :--- |
| A.1 / A.2 | Analyse & Build | Daemonless, rootless, Buildah, deux approches | [01-image-build](./Partie%20A/01-image-build/README-01-image-build.md) |
| A.3 | Scan de sécurité | Trivy, CVE HIGH/CRITICAL, remédiation, SARIF | [02-security-scan](./Partie%20A/02-security-scan/README.md) |
| A.4 | Audit d'image | Dive, efficacité des layers, optimisation | [03-image-audit](./Partie%20A/03-image-audit/README-03-image-audit.md) |
| A.5 / Bonus | Lint & CI | Hadolint, GitHub Actions | [04-linter-check](./Partie%20A/04-linter-check/README.md) |

---

## Exécuter la chaîne de build (Partie A)

Prérequis : Buildah, Trivy, Dive, Hadolint, Maven, JDK 17. Lancer depuis la racine
du projet.

```bash
# 1. Construire toutes les images des micro-services (approche Containerfile)
./ci-scripts/build-images.sh v1

# 2. (Optionnel) construire une image en mode Buildah natif
./ci-scripts/build-native.sh

# 3. Scanner les images avec Trivy (rapports JSON + SARIF, gate CRITICAL)
./ci-scripts/scan-trivy.sh v1

# 4. Auditer les layers avec Dive (mode CI, seuils d'efficacité)
./ci-scripts/audit-dive.sh v1
```

---

## Stack technique & environnement

- **Build d'images** : Buildah (daemonless, rootless, compatible OCI/Podman)
- **Scan de sécurité** : Trivy
- **Audit de layers** : Dive
- **Lint** : Hadolint
- **CI/CD** : GitHub Actions
- **Orchestration** (Partie B) : Kubernetes via Minikube, Helm, ArgoCD, Traefik
- **Intelligence Artificielle :** Claude (Anthropic)  utilisé pour la génération
de scripts, templates Helm, configurations K8s et documentation. Conformément
aux modalités du TP, l'ensemble des réponses générées a été compris, vérifié et
adapté au contexte MIAGE-Bank avant intégration.

Environnement de réalisation : **WSL (Ubuntu)** avec Buildah et Maven.

## Comment verifier et corriger le projet : 

### Badge CI
[![CI Status](https://github.com/hanja19/miage-bank-projet/actions/workflows/ci-pipeline.yml/badge.svg)](https://github.com/hanja19/miage-bank-projet/actions/workflows/ci-pipeline.yml)

### Vérification rapide (Partie A)

```bash
# pour validr la chaîne de build complète
chmod +x ci-scripts/*.sh
./ci-scripts/build-images.sh v1       #build les 6 images
./ci-scripts/scan-trivy.sh v1         #scan sécurité → rapports dans Partie A/02-security-scan/reports/
./ci-scripts/audit-dive.sh v1         #l'audit layers → rapports dans Partie A/03-image-audit/
hadolint "Partie A/01-image-build/Containerfile"  # lint → 0 erreur
```

### Vérification rapide (Partie B)

```bash
cd "Partie B"
helm lint miage-bank/                          #0 erreurs
helm template miage-bank miage-bank/           #l'aperçu des manifests
helm install miage-bank miage-bank/ -n miage-bank --dry-run --create-namespace
```

### Rapports disponibles
- Trivy JSON/SARIF : `Partie A/02-security-scan/reports/`
- Dive CI : `Partie A/03-image-audit/`
- CI artifacts : onglet Actions → dernier run → `build-reports`
