# Build de MIAGE-Bank avec Buildah : Containerfile vs natif

Le sujet demande de construire l'image OCI selon **deux approches** et de les
comparer. Les deux produisent une image **équivalente** ; elles diffèrent par la
manière de la décrire.

## Approche 1 : via Containerfile (`build-buildah.sh`)

Build **déclaratif** : un `Containerfile` multi-stage décrit l'image, et
`buildah bud` le construit.

```bash
buildah bud \
  --build-arg MODULE="${MODULE}" \
  --build-arg APP_PORT="${PORT}" \
  --layers \
  -f "${DOCKERFILE}" \
  -t "localhost/miage-bank/${NAME}:7.0" .
```

Caractéristiques :
- **Multi-stage** : stage `builder` (Maven + JDK 11) compile le JAR et extrait les
  couches Spring Boot (`layertools extract`) ; stage runtime (`eclipse-temurin:11-jre`)
  ne reçoit que les couches nécessaires → image finale légère.
- **Paramétré** : un **seul** Containerfile sert les 6 services Java via
  `ARG MODULE` / `ARG APP_PORT`. Le script choisit le bon Containerfile (celui du
  frontend pour `Banque-Frontend`).
- `--layers` active le cache de couches (rebuilds plus rapides).
- Ordre des `COPY` des couches du moins au plus volatile (dependencies →
  spring-boot-loader → snapshot-dependencies → application) pour **optimiser le
  cache**.

## Approche 2 : build natif, sans Containerfile (`build-buildah-native.sh`)

Build **impératif** : on reproduit exactement le même résultat avec les primitives
Buildah (`from` / `run` / `copy` / `config` / `commit`), sans aucun Containerfile.

Étapes du script :
1. **`buildah from maven:3.9...`** crée un conteneur builder jetable ; on y copie
   `pom.xml` + `src`, puis `buildah run ... mvn package` compile dans le conteneur
   (aucun Maven/JDK requis sur l'hôte).
2. Extraction des couches Spring Boot du builder vers l'hôte via `tar` (sans mount).
3. **`buildah from eclipse-temurin:11-jre`** crée le conteneur runtime ; on y crée
   l'utilisateur `spring` (`buildah run ... useradd`), on `buildah copy` les 4
   couches du JAR, puis `buildah config` fixe `workingdir`, `user`, `port`, `env`,
   `labels` et `entrypoint` (équivalents des directives du Containerfile).
4. **`buildah commit`** fige l'image.

Caractéristiques :
- Pas de Containerfile : l'image est entièrement **scriptée** (utile pour des
  builds dynamiques, conditionnels, ou pilotés par des variables).
- Le build Maven s'exécute dans un **conteneur jetable** → l'hôte n'a besoin que de
  Buildah.
- Le résultat (couches, métadonnées, utilisateur non-root, entrypoint) est
  **identique** à l'approche Containerfile.

## Comparaison

| Aspect                 | Containerfile (`bud`)            | Natif (`from/run/copy/commit`)        |
| ---------------------- | -------------------------------- | ------------------------------------- |
| Style                  | Déclaratif                       | Impératif (script shell)              |
| Lisibilité             | Élevée (format standard)         | Plus verbeux                          |
| Portabilité            | Lisible aussi par Docker/Podman  | Spécifique à Buildah                  |
| Cache de couches       | Natif (`--layers`)               | À gérer manuellement                  |
| Flexibilité dynamique  | Limitée (ARG / stages)           | Totale (logique shell arbitraire)     |
| Image produite         | identique                        | identique                             |

## Conclusion

Les deux approches aboutissent à la **même image OCI** (mêmes couches, même
utilisateur non-root, même entrypoint `JarLauncher`). Le **Containerfile** est
retenu pour la CI (lisible, standard, cache de couches), tandis que le **build
natif** démontre la maîtrise du mode impératif de Buildah et reste pertinent pour
des scénarios de build dynamiques. C'est l'intérêt de Buildah par rapport à Docker :
pouvoir choisir entre les deux paradigmes.

## Livrables

- `build-buildah.sh` — build via Containerfile (les 7 images, en matrice CI)
- `build-buildah-native.sh` — build natif équivalent (sans Containerfile)
