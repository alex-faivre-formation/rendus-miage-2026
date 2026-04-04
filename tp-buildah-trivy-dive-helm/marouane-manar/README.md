# Marouane MANAR
## Projet MIAGE Bank - Partie A et B

L'ensemble des livrables de ce TP se trouve dans ce répertoire.

---

# Partie A — Chaîne de build OCI avec Buildah, Trivy et Dive

## 1. Analyse comparative Docker vs Buildah
L'analyse comparative détaillée entre l'architecture classique de Docker et l'architecture "Daemonless/Rootless" de Buildah est disponible dans le fichier dédié à la racine : [`ANALYSE_COMPARATIVE_DOCKER_BUILDAH.md`](./ANALYSE_COMPARATIVE_DOCKER_BUILDAH.md).

## 2. Build de MIAGE-Bank avec Buildah

L'objectif de cette partie est de faire un build de l'ensemble des services (6 services back et 1 front) de l'application MIAGE-Bank avec Buildah (build OCI)

L'application source `MIAGE-Bank` étant architecturée en micro-services (6 services dans le dossier `miage-bank-back`), j'ai fait le choix de conteneuriser l'ensemble de l'application. Au lieu d'avoir 6 fois le même fichier, j'ai préféré utiliser un seul `ContainerFile` partagé. 

### Approche 1 : Via un Containerfile
Le script `build_all.sh` gère cette première approche :
1. Il compile tout le projet Java globalement avec Maven.
2. Il construit une image Nginx pour un petit frontend web (implémenté en plus pour pouvoir illustrer le TP visuellement).
3. Il boucle sur chaque sous-module micro-service pour exécuter Buildah en utilisant le `ContainerFile` commun, en adaptant le contexte de build pour cibler le `.jar` correct.

_Exécution locale :_ `./build_all.sh`

### Approche 2 : Construction layer par layer en mode natif Buildah
Le script `build_native.sh` gère cette méthode. Il réalise la même boucle sur tous les micro-services, mais utilise les commandes natives (`buildah from`, `buildah config`, `buildah copy`, `buildah commit`) plutôt qu'un `Containerfile`. 

### Comparaison des résultats

Le résultat final est le même, mais la méthode de construction est différente. En effet, la méthode avec ContainerFile est plus lisible et portable, tandis que la méthode layer-by-layer est plus flexible et s'intègre mieux dans des pipelines CI/CD. Elle s'intègre mieux car elle permet d'écrire des scripts plus complexes et de gérer les erreurs de manière plus fine. On peut passer des variables dynamiques ou des secrets directement aux commandes du script sans avoir besoin de les écrire (ou de les exposer) dans un fichier de configuration statique comme le ContainerFile. 

Pour résumer, l'approche ContainerFile est plus lisible et compréhensible mais moins flexible, tandis que la méthode layer-by-layer est plus flexible et s'intègre mieux dans des pipelines CI/CD. Par contre, l'approche layer-by-layer n'est pas standardisée et peut être plus difficile à maintenir. Il faut choisir la méthode de build en fonction du contexte.

## 3. Scan de sécurité avec Trivy

Pour automatiser nos audits, le script `generate_trivy_report.sh` va exporter et filtrer nos 7 images générées, afin de produire pour chacune le rapport JSON et SARIF. Les fichiers seront stockés dans le dossier `rapports_trivy/`. Lors d'un push, ces opérations sont exécutées automatiquement par la pipeline Github Actions.

### Analyse des résultats
L'image de base (`eclipse-temurin:17-jre-alpine`) s'avère robuste (quelques failles OS mineures). La majorité écrasante des failles proviennent des dépendances de l'applicatif Java (`app.jar`), qui totalise environ 40 vulnérabilités de haut niveau (36 HIGH, 4 CRITICAL).

### Détail des failles CRITICAL
* **CVE-2022-22965 (Spring4Shell)** : Faille de type Remote Code Execution (RCE) via le Data Binding de Spring MVC ou WebFlux fonctionnant sur JDK 9+. C'est la vulnérabilité la plus critique identifiée.
* **CVE-2016-1000027** : Faille liée à la désérialisation non sécurisée d'objets Java via `HttpInvokerServiceExporter` dans `spring-web`. Cela peut permettre l'exécution de code arbitraire s'il est exploité.
* **CVE-2023-20873** : Vulnérabilité de contournement de sécurité (Security Bypass) sur Spring Boot Actuator.

*(Il existe également 36 failles HIGH portant principalement sur les bibliothèques embarquées `snakeyaml` et `jackson`)*.

### Plan de remédiation global
Ces vulnérabilités découlent toutes d'une seule et même racine : **L'utilisation d'une version très obsolète de Spring Boot (la 2.6.4)** dans le fichier `pom.xml` parent du projet fourni pour le TP.
1. **Action requise** : Il conviendrait de migrer globalement le projet vers une version moderne et sécurisée comme **Spring Boot 3.3.x ou 3.4.x**. Cela mettra instantanément à jour toutes les dépendances transitives (`spring-core`, `snakeyaml`, `jackson`) vers des versions patchées.
2. **Pour le système OS (Alpine)** : Mettre à jour l'image de base (`temurin:17-jre-alpine`) régulièrement pour embarquer les derniers correctifs de paquets via l'utilisation rigoureuse des derniers *digests* OCI.

> **Remarque sur la gate de sécurité** :
> En temps normal, une vraie pipeline exige de configurer Trivy pour faire échouer le build en cas de faille détectée (via l'option `--exit-code 1`). Le problème ici, c'est que le code du TP utilise des versions de Spring tellement anciennes qu'il y a d'office 4 failles CRITICAL bloquantes. J'ai donc dû "baisser le niveau de sécurité attendu". Les rapports JSON et SARIF sont générés pour audit, mais la pipeline CI n'est pas bloquée.

## 4. Audit de l'image avec Dive

L'outil **Dive** a été configuré via le fichier `.dive-ci` qui servira à la pipeline Github Actions. Les seuils sont :
- Efficacité minimale : **95%**
- Espace gaspillé maximum : **20 MB**
- Pourcentage d'espace gaspillé maximum : **10%**

### Taille de chaque layer et taille totale des images

L'architecture étant standardisée, tous les micro-services back-end (Java) partagent exactement la même structure de layers. Voici la décomposition détaillée d'une image Java type :
- **Taille totale de l'image** : ~180 MB
- **Layer 1** (Image de base Alpine OS) : ~5 MB 
- **Layer 2** (JRE 17) : ~150 MB 
- **Layer 3** (Création de l'utilisateur non-root) : ~4 KB
- **Layer 4** (Copie du fichier `app.jar`) : Variable selon le service (de 20 à 30 MB)

Pour le micro-service Front-end, l'image est basée sur `nginx:alpine` avec la simple copie des fichiers statiques HTML/CSS/JS.

**Tableau récapitulatif des audits pour l'ensemble des conteneurs :**

| Nom de l'image | Espace gaspillé (Wasted) | Score d'Efficience | Passage de la Gate |
|---|---|---|---|
| `banque-annuaire` | 645 kB | 99.82 % | ✅ PASS |
| `banque-configserver` | 645 kB | 99.82 % | ✅ PASS |
| `banque-clientservice` | 645 kB | 99.83 % | ✅ PASS |
| `banque-compteservice` | 645 kB | 99.82 % | ✅ PASS |
| `banque-compositeservice` | 645 kB | 99.82 % | ✅ PASS |
| `banque-apigateway` | 645 kB | 99.82 % | ✅ PASS |
| `miage-bank-front` (Nginx) | 638 kB | 99.36 % | ✅ PASS |

*(L'espace gaspillé extrêmement faible provient uniquement de la mise à jour des certificats racines de l'OS de base).*

*(L'ensemble des exports d'analyse Dive sont disponibles dans le sous-dossier `rapports_dive/`).*

### Optimisation et approche "Avant / Après"
Pour répondre à l'exigence d'optimisation, j'ai fait le choix de ne pas faire d'avant/après classique. En effet, le projet a été conçu pour qu'il soit optimisé dès le départ (à l'étape 2) en séparant la compilation du packaging.

Si j'avais compilé le projet directement dans le `ContainerFile` avec une image de base très lourde (`openjdk:17`), Dive aurait détecté beaucoup de fichiers superflus,ce qui aurait fait chuter le score d'efficacité et créé une image beaucoup plus lourde. 

A la place, j'ai externalisé la compilation (via le script `build_all.sh`) sur la machine hôte. Le `ContainerFile` ne s'occupe que de copier le fichier `app.jar` généré dans une image minimale `eclipse-temurin:17-jre-alpine`. Cela simule le comportement d'un *multi-stage build*, ce qui me garantit de ne copier aucun élément superflu et d'avoir un très bon score sur l'audit Dive.

## 5. Script de build intégré (CI Github Actions)

Pour automatiser l'intégralité de la chaîne d'intégration continue, une pipeline Github Actions a été développée dans le fichier `.github/workflows/ci.yml`.

Cette pipeline réagit à chaque `push` et exécute séquentiellement les étapes exigées par le cahier des charges :
1. **Compilation Java** : Génération des binaires locaux via `mvn clean package`.
2. **Linting du ContainerFile** : Utilisation de `hadolint/hadolint-action` pour valider les bonnes pratiques OCI.
3. **Conteneurisation (Buildah)** : Exploitation du script `build_all.sh` pour générer les 7 images (6 Backends + 1 Frontend) et export sous forme d'archives locales (`.tar`).
4. **Scan de Sécurité (Trivy)** : Exécution de notre scanneur pour générer les rapports `JSON` et `SARIF`, avec upload automatique sur l'onglet *GitHub Security* de notre dépôt.
5. **Audit de gaspillage (Dive)** : Exécution de l'audit Dive respectant nos conditions de la *Quality Gate* (fichier `.dive-ci`) sur l'ensemble des archives.


---


# Partie B — Déploiement Kubernetes avec Helm et ArgoCD
*(À venir)*
