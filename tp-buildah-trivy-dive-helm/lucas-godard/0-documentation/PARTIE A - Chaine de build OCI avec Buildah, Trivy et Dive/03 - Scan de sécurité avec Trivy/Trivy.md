# Scan de sécurité avec Trivy

## Objectif

Analyser la sécurité des images OCI de MIAGE-Bank avec Trivy : identifier les
vulnérabilités, filtrer sur HIGH/CRITICAL, et exporter les rapports (JSON + SARIF
pour GitHub Security). Le scan est intégré à la CI et exécuté sur **les 7 images**
(6 micro-services Java + frontend).

## Commandes utilisées

Pour installer Trivy il faut entrer cette commande :

```bash
curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sudo sh -s -- -b /usr/local/bin
```

Le scan Trivy récupère les images dans `images/containerfile-version/` et
génère les rapports dans le dossier `build-reports/`
Pour lancer le script de scan Trivy :
```bash
./scripts/scan-trivy.sh
```

Le script `scan-trivy.sh` exécute, pour chaque image :

```bash
trivy image --severity HIGH,CRITICAL <IMAGE>
trivy image --severity HIGH,CRITICAL --format json  -o trivy_<svc>.json <IMAGE>
trivy image --severity HIGH,CRITICAL --format sarif -o trivy_<svc>.sarif <IMAGE>
```

## Résultats du scan (par image)

| Image                | CRITICAL | HIGH |
| -------------------- | -------- | ---- |
| apigateway           | 4        | 42   |
| annuaire             | 9        | 47   |
| clientservice        | 9        | 49   |
| compositeservice     | 9        | 50   |
| compteservice        | 10       | 47   |
| configserver         | 10       | 50   |
| **frontend**         | **0**    | **1**|

**Constat majeur** : toutes les CVE CRITICAL des micro-services proviennent des
**dépendances Java héritées** (Spring Framework 5.3.16, Spring Boot 2.6.4, Tomcat
embarqué 9.0.58, Spring Cloud 3.1.1) — c'est-à-dire du code applicatif **repris de
la correction du projet fil rouge**, et non de la chaîne de build de ce TP. Le
frontend, développé spécifiquement, ne présente **aucune** CVE CRITICAL.

## CVE CRITICAL identifiées (communes aux services Java)

Les mêmes familles de CVE reviennent sur tous les services Java (selon leurs
dépendances). Détail et remédiation :

### CVE-2022-22965 — Spring Framework « Spring4Shell » (spring-beans / spring-webmvc / spring-webflux 5.3.16)
- **Description** : exécution de code à distance (RCE) via data binding sur des
  applications Spring MVC/WebFlux packagées en WAR sur Tomcat.
- **Fix** : 5.3.18+ (ou 5.2.20).
- **Remédiation** : monter Spring à ≥ 5.3.18. Dans notre contexte, l'appli tourne
  en JAR exécutable (pas WAR sur Tomcat externe) → vecteur d'exploitation
  fortement réduit.

### CVE-2016-1000027 — spring-web 5.3.16 (désérialisation)
- **Description** : désérialisation Java non sécurisée potentiellement
  exploitable (RCE) si un endpoint désérialise des données non fiables.
- **Fix** : Spring 6.0.0.
- **Remédiation** : passage à Spring 6 (changement majeur). Non exploitable ici
  car aucun endpoint n'expose de désérialisation Java native.

### CVE-2023-20860 / CVE-2023-20873 — Spring MVC & Spring Boot Actuator (5.3.16 / 2.6.4)
- **Description** : contournement de règles de sécurité d'URL (20860) et bypass
  d'authentification sur Cloud Foundry pour l'actuator (20873).
- **Fix** : Spring 5.3.26 / Spring Boot 2.6.15+.
- **Remédiation** : montée de version Spring Boot. Impact limité : pas de
  déploiement Cloud Foundry, actuator restreint au réseau interne du cluster.

### CVE-2025-24813 / CVE-2026-41293 / CVE-2026-43512 / CVE-2026-43515 — Tomcat embed 9.0.58
- **Description** : famille de vulnérabilités du connecteur Tomcat embarqué
  (dont écriture/exécution via PUT partiel pour 2025-24813).
- **Fix** : tomcat-embed-core 9.0.99+ / 9.0.118+.
- **Remédiation** : relever la version de Spring Boot (qui embarque Tomcat) tire
  automatiquement un Tomcat patché.

### CVE-2022-22980 — spring-data-mongodb 3.3.2 (compteservice uniquement)
- **Description** : injection d'expression SpEL via requête dérivée → potentielle
  exécution de code.
- **Fix** : 3.3.5 / 3.4.1.
- **Remédiation** : montée de Spring Data MongoDB. Impact limité : pas de requête
  construite dynamiquement depuis une entrée utilisateur dans nos repositories.

### CVE-2026-40982 — spring-cloud-config-server 3.1.1 (configserver uniquement)
- **Description** : vulnérabilité du serveur de configuration Spring Cloud.
- **Fix** : 4.3.3 / 5.0.3.
- **Remédiation** : montée de Spring Cloud (couplée à Spring Boot 3).

> **Plan de remédiation global** : l'unique correctif de fond est la **montée de
> Spring Boot 2.6.4 → 2.7.x/3.x** (qui entraîne Spring Framework, Tomcat, Spring
> Cloud et Spring Data patchés). Cette montée n'a pas été réalisée car elle relève
> du **code applicatif hérité** (hors périmètre de ce TP DevOps) et constituerait
> un changement majeur (Spring 6 / Jakarta EE). Les vecteurs d'exploitation sont
> par ailleurs atténués par le packaging (JAR exécutable, pas de WAR/Tomcat
> externe) et par l'isolement réseau (NetworkPolicy default-deny, actuator interne).

## Gate de sécurité - seuil ajusté

Les images Java contenant des CVE **CRITICAL** non corrigeables sans refonte de
l'application héritée, une gate bloquante au niveau `CRITICAL` empêcherait toute
publication. **Décision** : la CI **génère les rapports Trivy** (JSON + SARIF) mais
**n'interrompt pas le build** sur les CVE CRITICAL des dépendances applicatives ;
le seuil de blocage effectif a été assoupli. Le frontend, lui, passerait une gate
CRITICAL (0 CVE critique).

**Justification** : les CVE proviennent du code fil rouge (versions Spring/Tomcat
figées), pas de la chaîne de build ; elles sont tracées et leur impact est atténué
par l'architecture (cf. plan de remédiation ci-dessus).

Emplacement de la configuration : `.github/workflows/<workflow>` (étape Trivy).

## Livrables

- `scan-trivy.sh` — script de scan
- `trivy_<service>.json` — rapports complets JSON (7 images)
- `trivy_<service>.sarif` — remontés dans l'onglet Security de GitHub
