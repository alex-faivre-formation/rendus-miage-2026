# Audit des images avec Dive

## Objectif

Auditer les layers des images OCI : mesurer l'efficacité, identifier les fichiers
superflus, et valider des seuils en mode CI. L'audit est intégré à la CI et
exécuté sur les 7 images.

## Seuils CI exigés par le sujet

| Critère                     | Seuil  |
| --------------------------- | ------ |
| Efficacité minimale         | 95 %   |
| Espace gaspillé maximum     | 20 Mo  |
| % d'espace gaspillé maximum | 10 %   |

## Commande utilisée

Pour installer Dive :
```bash
wget -qO dive.deb https://github.com/wagoodman/dive/releases/download/v0.12.0/dive_0.12.0_linux_amd64.deb
sudo dpkg -i dive.deb
```

L'audit Dive prend les images se trouvant dans le dossier `images/containerfile-version`
et place ses rapports dans le dossier `build-reports/dive`. Pour lancer le script :
```bash
./scripts/audit-dive.sh
```

Le script `audit-dive.sh` exécute, pour chaque image :

```bash
CI=true dive <IMAGE> --ci \
  --lowestEfficiency 0.95 \
  --highestWastedBytes 20MB \
  --highestUserWastedPercent 0.10
```

## Résultats

| Image            | Efficacité | Gaspillé   | % gaspillé | Résultat |
| ---------------- | ---------- | ---------- | ---------- | -------- |
| apigateway       | 99,45 %    | 2,5 Mo     | 1,11 %     | PASS     |
| annuaire         | 99,46 %    | 2,5 Mo     | 1,10 %     | PASS     |
| clientservice    | 99,49 %    | 2,5 Mo     | 1,02 %     | PASS     |
| compositeservice | 99,46 %    | 2,5 Mo     | 1,10 %     | PASS     |
| compteservice    | 99,46 %    | 2,5 Mo     | 1,08 %     | PASS     |
| configserver     | 99,46 %    | 2,5 Mo     | 1,10 %     | PASS     |
| frontend         | 99,38 %    | 0,63 Mo    | 1,16 %     | PASS     |

Toutes les images **dépassent largement** les seuils (efficacité > 99 % contre
95 % requis, gaspillage ~2,5 Mo contre 20 Mo autorisés). Chaque audit retourne
`Result: PASS [Total:3] [Passed:3] [Failed:0]`.

## Fichiers superflus identifiés

**Images Java** — le gaspillage (~2,5 Mo) provient de **fichiers système Debian**
dupliqués entre layers (écrits dans un layer, modifiés dans un autre), aucun lié à
l'application :

| Fichier                              | Espace gaspillé |
| ------------------------------------ | --------------- |
| `/var/cache/debconf/templates.dat`   | 1,4 Mo          |
| `/var/log/dpkg.log`                  | 396 kB          |
| `/var/log/lastlog`                   | 322 kB          |
| `/var/lib/dpkg/status`               | 242 kB          |
| `/var/log/apt/history.log`           | 42 kB           |
| (+ divers `/var/log`, `/etc` < 35 kB)| —               |

**Frontend** — gaspillage encore plus faible (0,63 Mo), essentiellement
`/etc/ssl/certs/ca-certificates.crt` (436 kB) et la base de paquets Alpine
`/lib/apk/db/installed` (165 kB).

## Optimisations - analyse avant / après

L'image est construite en **multi-stage** dès le départ (build Maven séparé du
runtime), ce qui explique le très bon score :

- **Stage build** : Maven + JDK (lourd) - **non embarqué** dans l'image finale.
- **Stage runtime** : `eclipse-temurin:11-jre` (JRE seul) + layers Spring Boot
  extraits (dependencies / application séparés pour le cache).

**Avant (hypothèse mono-stage avec JDK + cache Maven)** : image de plusieurs
centaines de Mo, cache `.m2` embarqué, efficacité dégradée.
**Après (multi-stage actuel)** : efficacité **99,4 %**, gaspillage **2,5 Mo**, PASS
sur les 3 seuils.

**Optimisations supplémentaires possibles** (le gaspillage résiduel étant des
fichiers système) : nettoyer `/var/cache/*` et `/var/log/*` dans le layer final
(`rm -rf` en fin de RUN).

## Gate

Aucune dérogation nécessaire : **les 7 images passent les trois seuils Dive**.

## Livrables

- `audit-dive.sh` — script d'audit
- `dive_<service>.txt` — sortie Dive complète par image (7 fichiers)
