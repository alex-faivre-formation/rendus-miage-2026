# 01 - Préparation de l'infrastructure Kubernetes

Ce module décrit la configuration de l'environnement d'accueil sur le cluster Kubernetes (Minikube). L'accent est mis sur l'isolation et la sécurité réseau.

---

## 1. Isolation logique (Namespace)
Nous utilisons un Namespace dédié pour l'application MIAGE-Bank. Cela permet de séparer nos ressources (Pods, Services, Ingress) des autres applications et des services système de Kubernetes.

* **Fichier :** `namespace.yaml`
* **Avantage :** Évite les conflits de noms et permet une gestion propre des ressources par projet.

---

## 2. Sécurité réseau (NetworkPolicies)
Par défaut, tous les Pods d'un cluster peuvent communiquer entre eux. Pour respecter les bonnes pratiques de sécurité (principe du moindre privilège), nous avons implémenté des NetworkPolicies.

* **Fichier :** `networkpolicy.yaml`
* **Principe :** Nous appliquons une politique de "Default Deny" (tout bloquer par défaut).
* **Flux autorisés :** Seuls les flux indispensables sont ouverts (ex: l'Ingress Controller vers les services clients/comptes, et les services vers la base de données).

---

## 3. Identités des services (ServiceAccount)
Chaque micro-service s'exécute avec un ServiceAccount spécifique au lieu d'utiliser le compte par défaut.

* **Fichier :** `serviceaccount.yaml`
* **Objectif :** Limiter les droits de l'application sur l'API Kubernetes au strict minimum nécessaire.

---

## 4. Limitation des ressources
Pour garantir la stabilité du cluster Minikube, des limites de ressources (CPU et Mémoire) sont définies. Cela empêche un micro-service défaillant de consommer toute la mémoire du nœud et de faire planter les autres services.

---

> **Verdict de déploiement :** L'infrastructure est prête et sécurisée. Le cluster est désormais capable d'héberger les micro-services en respectant une architecture Zero-Trust.