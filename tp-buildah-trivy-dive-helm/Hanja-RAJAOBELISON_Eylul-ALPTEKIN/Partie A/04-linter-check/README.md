# Partie A — Étape 04 : Lint du Containerfile avec Hadolint

Cette étape couvre l'**analyse statique du Containerfile** via Hadolint (livrable A.5 /
bonus CI), outil qui vérifie la conformité aux bonnes pratiques OCI et aux règles de
sécurité des images.

---

## Résultat

```bash
hadolint "Partie A/01-image-build/Containerfile"
# (aucune sortie)
```

**Sortie vide = 0 avertissement, 0 erreur.** Le Containerfile est entièrement conforme
aux règles Hadolint.

---

## Règles vérifiées et conformité

Hadolint contrôle une centaine de règles (préfixe `DL` pour Dockerfile rules, `SC` pour
ShellCheck). Les principales qui auraient pu s'appliquer à notre Containerfile et les
raisons pour lesquelles elles passent :

| Règle | Description | Statut |
|---|---|---|
| DL3002 | Ne pas utiliser `root` comme utilisateur final |  `USER app` présent |
| DL3006 | Toujours préciser un tag sur l'image de base | `eclipse-temurin:17-jre-alpine` (tag fixé) |
| DL3007 | Ne pas utiliser le tag `latest` | tag de version explicite |
| DL3008 | Épingler les versions des paquets `apt-get` | pas de `apt-get` dans notre image Alpine |
| DL3018 | Épingler les versions `apk add` |  pas de `apk add` |
| DL3025 | Utiliser un tableau JSON pour `ENTRYPOINT`/`CMD` |  `ENTRYPOINT ["java", "-jar", ...]` |
| DL4006 | Utiliser `SHELL` ou tableau pour les commandes | notre `RUN` est simple, sans opérateur shell risqué |

Les choix de conception qui expliquent ce résultat propre :

- **Image de base avec tag versionné et non `latest`** : reproductibilité garantie.
- **Utilisateur non-root (`app`)** : bonne pratique de sécurité exigée par Hadolint,
  Kubernetes et les benchmarks CIS.
- **`COPY --chown`** : copie et attribution des droits en une seule instruction
  (pas de `RUN chown` séparé qui créerait une couche superflue et serait signalé).
- **`ENTRYPOINT` en forme JSON** (`["java", "-jar", ...]`) : forme recommandée, évite
  l'interprétation par un shell intermédiaire.
- **Absence de `RUN apt-get`/`apk add`** : pas de cache de gestionnaire de paquets
  à nettoyer, pas de risque de version non épinglée.

---

## Intégration dans la CI (bonus)

Hadolint est intégré dans le pipeline **GitHub Actions** (`ci-pipeline.yml`) comme
première étape, avant le build Buildah. Un Containerfile invalide interrompt
immédiatement la chaîne, garantissant qu'aucune image non conforme n'est produite.

```yaml
- name: Lint du Containerfile
  uses: hadolint/hadolint-action@v3.1.0
  with:
    dockerfile: Partie A/01-image-build/Containerfile
```
