# Partie A — Étape 03 : Audit des layers avec Dive

Cette étape couvre l'**audit du contenu et de l'efficacité des images** (livrable A.4) :
analyse des layers, identification des fichiers superflus, mesure de l'efficacité contre
les seuils du sujet, et optimisation.

---

## Méthodologie

Comme pour Trivy, l'audit est réalisé en **daemonless** : chaque image est exportée en
archive tar (`buildah push`), puis analysée par Dive en mode CI
(`dive --ci --source docker-archive`). Automatisé par `ci-scripts/audit-dive.sh`.

Les seuils exigés par le sujet sont définis dans `ci-scripts/.dive-ci` :

```yaml
rules:
  lowestEfficiency: 0.95          # efficacité minimale : 95%
  highestWastedBytes: 20MB        # gaspillage max : 20 Mo
  highestUserWastedPercent: 0.10  # gaspillage max : 10%
```

```bash
./ci-scripts/audit-dive.sh v1
```

---

## Résultats

Les **6 images respectent les seuils**, avec une marge très confortable.

| Image | Taille | Efficacité | Gaspillage | % gaspillé | Verdict |
|---|---|---|---|---|---|
| miage-bank-annuaire | 240 Mo | 99,83 % | 639 kB | 0,28 % | PASS |
| miage-bank-clients | 261 Mo | 99,84 % | 639 kB | 0,25 % | PASS |
| miage-bank-composite | 238 Mo | 99,83 % | 639 kB | 0,28 % | PASS |
| miage-bank-comptes | 241 Mo | 99,83 % | 639 kB | 0,28 % | PASS |
| miage-bank-configserver | 240 Mo | 99,83 % | 639 kB | 0,28 % | PASS |
| miage-bank-proxy | 237 Mo | 99,83 % | 639 kB | 0,28 % | PASS |

Efficacité **≈ 99,83 %** (seuil 95 %), gaspillage **≈ 639 kB** (seuil 20 Mo), pourcentage
gaspillé **≈ 0,28 %** (seuil 10 %). Toutes les marges sont largement respectées.

---

## Structure des layers

Chaque image est composée des couches suivantes :

1. Les couches de l'image de base **`eclipse-temurin:17-jre-alpine`** (système Alpine + JRE) ;
2. une couche issue du `RUN addgroup && adduser` (création de l'utilisateur non-root) ;
3. une couche contenant le **JAR applicatif** (`COPY --chown`).

La quasi-totalité du poids vient du JRE et du JAR (incompressibles), d'où l'excellente
efficacité : il n'y a quasiment aucun fichier dupliqué ou superflu ajouté par notre build.

---

## Fichiers inefficients identifiés

Dive ne relève que **639 kB** de gaspillage, concentrés sur quelques fichiers présents en
double (Count = 2) entre la couche de base et la couche de création d'utilisateur :

| Fichier | Espace gaspillé | Cause |
|---|---|---|
| `/etc/ssl/certs/ca-certificates.crt` | 436 kB | réécrit lors de la création d'utilisateur |
| `/lib/apk/db/installed` | 121 kB | base de données apk modifiée |
| `/etc/passwd`, `/etc/group`, `/etc/shadow` | ~3 kB | modifiés par `adduser`/`addgroup` |
| divers binaires busybox | 0 B | métadonnées dupliquées |

Ces doublons proviennent du fait que la couche `RUN addgroup && adduser` réécrit des
fichiers système (`/etc/passwd`, `/etc/group`, `/etc/shadow`) déjà présents dans l'image
de base : l'ancienne version est alors comptée comme « gaspillée ».

---

## Optimisation : avant / après

L'optimisation majeure a eu lieu dès le choix de l'image de base et de la stratégie de
copie. Comparatif avec la version précédente du projet (base JDK complète, copie non
optimisée) :

| Service | Avant | Après | Gain |
|---|---|---|---|
| clients | 549 Mo | 261 Mo | **−52 %** |
| comptes | 529 Mo | 241 Mo | **−54 %** |

Leviers appliqués :

- **base `jre-alpine`** au lieu d'un JDK complet (le JDK n'est pas nécessaire à
  l'exécution) → division par deux de la taille ;
- **copie du JAR en une seule couche** avec `COPY --chown`, sans cache ni fichier
  temporaire résiduel → efficacité quasi maximale.

Optimisation résiduelle possible (non nécessaire ici) : les ~639 kB de doublons liés à
`adduser` pourraient être évités en utilisant une image de base fournissant déjà un
utilisateur non-root, ou via un build multi-stage. Le gain (0,28 %) ne le justifie pas.

---

## Livrables de cette étape

- Rapports `dive-<service>.txt` (dossier courant)
- Tableau des efficacités et gaspillages par image
- Structure et taille des layers
- Identification des fichiers superflus
- Optimisation avant/après documentée
- Captures : `dive-analysis.png` (vue détaillée des layers), `client.png` et
  `compte.png` (audits par service)
