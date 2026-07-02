# Partie A — Étape 02 : Scan de sécurité avec Trivy

Cette étape couvre le **scan de sécurité** des images MIAGE-Bank (livrable A.3) :
rapport complet, liste des CVE HIGH/CRITICAL, explication des vulnérabilités et plan
de remédiation.

---

## Méthodologie

Comme l'environnement est en **WSL sans démon Docker**, Trivy ne peut pas lire les
images depuis un socket Docker. La solution retenue (daemonless) :

1. chaque image est exportée en archive `docker-archive` via `buildah push` ;
2. Trivy scanne l'archive avec l'option `--input`.

Le scan est filtré sur les sévérités **HIGH** et **CRITICAL**, et chaque image produit
deux rapports : un **JSON** (analyse détaillée) et un **SARIF** (intégrable dans GitHub
Security). Le tout est automatisé par `ci-scripts/scan-trivy.sh`.

```bash
./ci-scripts/scan-trivy.sh v1
```

Une « gate » interrompt le build si une CVE **CRITICAL** est détectée (voir décision
plus bas).

---

## Synthèse des résultats

La couche système (**Alpine 3.23**) est **saine sur toutes les images : 0 vulnérabilité**.
La totalité des CVE provient des **dépendances Java embarquées dans le JAR**
(`app/app.jar`), conséquence d'une version de base de Spring Boot vieillissante.

| Image | HIGH | CRITICAL | Total |
|---|---|---|---|
| miage-bank-configserver | 32 | 5 | 37 |
| miage-bank-clients | 33 | 4 | 37 |
| miage-bank-composite | 32 | 4 | 36 |
| miage-bank-annuaire | 30 | 4 | 34 |
| miage-bank-comptes | 30 | 4 | 34 |
| miage-bank-proxy | 26 | 0 | 26 |

**5 services sur 6 portent des CVE CRITICAL.** Seul le `proxy` (API Gateway) en est
exempt — il n'embarque pas Tomcat (basé sur Netty/WebFlux), d'où l'absence de la CVE
critique Tomcat. Le `configserver` est le plus exposé (5 CRITICAL) car il cumule les CVE
Tomcat **et** la CVE propre à Spring Cloud Config Server.

> Méthode pour obtenir ces décomptes à partir des rapports JSON :
> ```bash
> for f in "Partie A/02-security-scan/reports"/trivy-miage-bank-*.json; do
>   echo -n "$(basename "$f" .json) : "
>   jq -r '[.Results[]?.Vulnerabilities[]?.Severity] | group_by(.) | map("\(.[0]) \(length)") | join(", ")' "$f"
> done
> ```

---

## CVE CRITICAL — explication et remédiation

### CVE-2025-24813 — Apache Tomcat embed-core (10.1.19)

Vulnérabilité d'exécution de code à distance et/ou de divulgation d'informations via une
requête **PUT partielle**, lorsque l'écriture par défaut du servlet est activée. C'est la
CVE la plus grave car elle peut mener à une compromission du conteneur.
**Remédiation :** mettre à jour Tomcat vers **≥ 10.1.35** (obtenu automatiquement en
montant la version de Spring Boot, voir plus bas).

### CVE-2026-40982 — Spring Cloud Config Server (4.1.0) — *configserver uniquement*

**Path traversal** permettant potentiellement de lire des fichiers de configuration hors
du périmètre autorisé. Critique pour un serveur qui distribue la configuration de tout le
système.
**Remédiation :** Spring Cloud Config Server **≥ 4.3.3** (alignement de la version de
Spring Cloud).

### Autres CRITICAL Tomcat (configserver)

CVE-2026-41293 (validation d'entrée incorrecte), CVE-2026-43512 (contournement
d'authentification *digest*), CVE-2026-43515 (autorisation incorrecte). Toutes corrigées
par la mise à jour de Tomcat **≥ 10.1.55**.

---

## CVE HIGH — principales et remédiation

Les CVE HIGH se regroupent par composant ; la remédiation est commune (mise à jour de
version) :

| Composant | Exemples de CVE | Nature | Version corrigée |
|---|---|---|---|
| `tomcat-embed-core` | CVE-2024-50379, CVE-2025-55752 | RCE (TOCTOU), directory traversal | ≥ 10.1.45 |
| `spring-web` / `spring-webmvc` | CVE-2024-22259, CVE-2024-38816 | URL parsing, path traversal | ≥ 6.1.14 |
| `spring-boot` | CVE-2025-22235 | matcher actuator incorrect | ≥ 3.3.11 |
| `spring-security-crypto` | CVE-2025-22228 | longueur de mot de passe BCrypt | ≥ 6.2.10 |
| `netty-*` (proxy) | CVE-2025-24970, CVE-2025-55163 | crash natif TLS, DoS HTTP/2 | ≥ 4.1.124 |
| `spring-cloud-gateway-server` (proxy) | CVE-2025-41235, CVE-2025-41253 | headers non fiables, injection EL | ≥ 4.2.6 |
| `jettison` | CVE-2022-40150, CVE-2023-1436 | DoS (récursion, mémoire) | ≥ 1.5.4 |
| `bouncycastle` (bcprov-jdk18on) | CVE-2026-5598 | fuite de clé privée (timing) | ≥ 1.84 |
| `xstream` | CVE-2022-40151, CVE-2024-47072 | DoS sérialisation | ≥ 1.4.21 |

---

## Plan de remédiation global

**Cause racine :** l'application est buildée sur **Spring Boot 3.2.3**, dont les
dépendances gérées (Tomcat 10.1.19, Spring Framework 6.1.4, Spring Security 6.2.2…) sont
aujourd'hui obsolètes.

**Correctif principal (couvre la majorité des CVE) :** monter la version du parent dans
le `pom.xml` vers la dernière maintenance 3.x :

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.11</version> <!-- au lieu de 3.2.3 -->
</parent>
```

Cette montée de version met à jour **transitivement** Tomcat, Spring Framework et Spring
Security, ce qui résout l'essentiel des HIGH/CRITICAL. Il faut **aligner aussi la version
de Spring Cloud** (BOM) pour corriger `spring-cloud-config-server` et
`spring-cloud-gateway-server`.

**Dépendances non gérées par le BOM** (jettison, bouncycastle, xstream, netty) : les
surcharger explicitement dans `<dependencyManagement>` avec les versions corrigées du
tableau ci-dessus.

**Procédure :** `mvn clean package` → reconstruire les images
(`./ci-scripts/build-images.sh v2`) → rescanner (`./ci-scripts/scan-trivy.sh v2`) pour
obtenir un comparatif avant/après.

---

## Décision sur la gate de sécurité

La gate CRITICAL a **correctement interrompu le build** (preuve que le mécanisme
fonctionne). Appliquer la remédiation implique une montée de version majeure de Spring
Boot/Spring Cloud, qui peut introduire des ruptures de compatibilité avec la configuration
de sécurité **Okta/OAuth2** de l'application, et nécessite une phase de re-test qui dépasse
le périmètre et le délai du TP.

Conformément à l'autorisation explicite du sujet, nous **documentons les vulnérabilités et
leur plan de remédiation** (ci-dessus) et **assumons le risque résiduel** pour permettre à
la chaîne d'aller à son terme. En production, la montée de version Spring Boot serait
appliquée et validée avant tout déploiement.

---

## Livrables de cette étape

- Rapports `trivy-<service>.json` et `trivy-<service>.sarif` (dossier `reports/`)
- Liste des CVE HIGH/CRITICAL ci-dessus
- Plan de remédiation par composant + correctif racine
- Décision documentée sur la gate
- Capture : `trivy-table.png`
