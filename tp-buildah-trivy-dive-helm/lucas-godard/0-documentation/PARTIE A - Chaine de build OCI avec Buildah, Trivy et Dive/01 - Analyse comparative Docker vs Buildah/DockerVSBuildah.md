# Analyse comparative Docker vs Buildah

Analyse des différences entre Docker et Buildah, et justification du
choix de **Buildah** pour la chaîne de build OCI de MIAGE-Bank (exécutée en CI
GitHub Actions).

## 1. Architecture : démon vs daemonless

**Docker** repose sur une architecture **client-serveur** : la commande `docker`
(client) dialogue avec un **démon** central (`dockerd`) qui s'exécute en
permanence, généralement en **root**, et orchestre builds, conteneurs, images et
réseaux. Tout passe par ce processus privilégié unique.

**Buildah** est **daemonless** : il n'y a aucun service de fond. Chaque commande
`buildah` est un **processus éphémère** qui fait son travail (créer un conteneur de
build, exécuter une instruction, committer une image) puis se termine. Il s'appuie
directement sur les bibliothèques bas niveau de l'écosystème conteneur
(`containers/storage`, `containers/image`, runtime OCI type `runc`/`crun`), sans
intermédiaire long-running.

**Conséquence concrète dans notre CI** : sur un runner GitHub Actions, Buildah
n'exige pas de démarrer ni de privilégier un démon Docker. Le job
`build-buildah.sh` invoque `buildah bud` directement, sans `dockerd`.

## 2. Sécurité : surface d'attaque et privilèges

- **Socket Docker** : le démon expose `/var/run/docker.sock`. Quiconque y a accès
  contrôle de fait la machine (on peut démarrer un conteneur privilégié montant le
  système hôte). Dans une CI partagée, exposer ce socket est un risque majeur
  d'**escalade de privilèges**.
- **Exécution root** : `dockerd` tourne traditionnellement en root, donc un build
  malveillant ou une faille du démon a un impact root.
- **Buildah rootless** : Buildah s'exécute en **espace utilisateur** (rootless) via
  les **user namespaces** Linux — l'utilisateur du pipeline est « root » *dans* le
  namespace du build, mais reste **non privilégié** sur l'hôte. Pas de socket à
  exposer, pas de démon privilégié : la **surface d'attaque est nettement réduite**.

Notre Containerfile renforce encore la posture : l'image runtime crée et utilise un
**utilisateur non-root `spring` (uid 1001)** (`USER spring:spring`), de sorte que le
conteneur final ne tourne pas non plus en root.

## 3. Conformité OCI et interopérabilité

Buildah produit des images conformes à l'**OCI Image Format Specification**,
strictement compatibles avec Docker, Podman et tout runtime OCI. Concrètement :

- l'image construite par Buildah est poussée sur **GHCR** et **tirée par
  Kubernetes** (Partie B) sans aucune adaptation ;
- Buildah lit les **Containerfile/Dockerfile** sans modification (`buildah bud -f
  Containerfile`), donc la migration depuis Docker est transparente ;
- on peut aussi **se passer de Containerfile** et scripter l'image en mode natif
  (cf. `Comparaison.md`), ce que Docker ne permet pas nativement.

## 4. Cas d'usage CI/CD et choix retenu

Buildah est particulièrement pertinent en CI/CD **rootless** (runners GitLab,
pipelines Kubernetes, GitHub Actions) :

- **pas de démon à provisionner** → moins de configuration, pas de Docker-in-Docker
  privilégié ;
- **rootless** → conforme aux politiques de sécurité des plateformes CI
  mutualisées ;
- **scriptable** → intégration naturelle dans un script shell
  (`build-buildah.sh`) appelé par GitHub Actions.

**Choix pour MIAGE-Bank** : Buildah a été retenu pour construire les 7 images (6
micro-services Java + frontend) directement dans GitHub Actions, en rootless, avec
un Containerfile **multi-stage** (build Maven séparé du runtime JRE) garantissant
des images légères (efficacité Dive > 99 %, cf. section 04). Docker aurait imposé
un démon privilégié sans bénéfice ici.