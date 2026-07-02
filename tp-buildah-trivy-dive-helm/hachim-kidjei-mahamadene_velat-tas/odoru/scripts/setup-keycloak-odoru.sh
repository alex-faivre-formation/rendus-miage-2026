#!/usr/bin/env bash
set -euo pipefail

# ==========================================================
# Initialisation Keycloak pour Odoru
# ==========================================================

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8090}"
ADMIN_USER="${KEYCLOAK_ADMIN_USER:-admin}"
ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"

REALM="odoru"
CLIENT_ID="odoru-front"
DEFAULT_PASSWORD="secret123"

echo "=================================================="
echo "Initialisation Keycloak pour Odoru"
echo "Keycloak URL : ${KEYCLOAK_URL}"
echo "Realm        : ${REALM}"
echo "Client       : ${CLIENT_ID}"
echo "=================================================="

echo ""
echo "1. Récupération du token admin..."

ADMIN_TOKEN=$(curl -s -X POST "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" \
  -d "username=${ADMIN_USER}" \
  -d "password=${ADMIN_PASSWORD}" \
  | python3 -c "import sys,json; print(json.load(sys.stdin).get('access_token',''))")

if [ -z "${ADMIN_TOKEN}" ]; then
  echo "ERREUR : impossible de récupérer le token admin Keycloak."
  exit 1
fi

echo "Token admin récupéré."

echo ""
echo "2. Création du realm ${REALM} si nécessaire..."

REALM_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  "${KEYCLOAK_URL}/admin/realms/${REALM}")

if [ "${REALM_STATUS}" = "200" ]; then
  echo "Realm ${REALM} existe déjà."
else
  curl -s -X POST "${KEYCLOAK_URL}/admin/realms" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{
      \"realm\": \"${REALM}\",
      \"enabled\": true
    }" > /dev/null

  echo "Realm ${REALM} créé."
fi

echo ""
echo "3. Création des rôles realm..."

for ROLE in MEMBER SECRETARY TEACHER PRESIDENT; do
  ROLE_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    "${KEYCLOAK_URL}/admin/realms/${REALM}/roles/${ROLE}")

  if [ "${ROLE_STATUS}" = "200" ]; then
    echo "Rôle ${ROLE} existe déjà."
  else
    curl -s -X POST "${KEYCLOAK_URL}/admin/realms/${REALM}/roles" \
      -H "Authorization: Bearer ${ADMIN_TOKEN}" \
      -H "Content-Type: application/json" \
      -d "{
        \"name\": \"${ROLE}\",
        \"description\": \"Odoru role ${ROLE}\"
      }" > /dev/null

    echo "Rôle ${ROLE} créé."
  fi
done

echo ""
echo "4. Création ou mise à jour du client ${CLIENT_ID}..."

CLIENT_UUID=$(curl -s \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  "${KEYCLOAK_URL}/admin/realms/${REALM}/clients?clientId=${CLIENT_ID}" \
  | python3 -c "import sys,json; data=json.load(sys.stdin); print(data[0]['id'] if data else '')")

CLIENT_CONFIG=$(cat <<JSON
{
  "clientId": "${CLIENT_ID}",
  "name": "Odoru Frontend",
  "enabled": true,
  "protocol": "openid-connect",
  "publicClient": true,
  "standardFlowEnabled": true,
  "directAccessGrantsEnabled": true,
  "implicitFlowEnabled": false,
  "serviceAccountsEnabled": false,
  "authorizationServicesEnabled": false,
  "rootUrl": "http://localhost:30081",
  "baseUrl": "http://localhost:30081",
  "adminUrl": "http://localhost:30081",
  "redirectUris": [
    "http://localhost:30081/*",
    "http://localhost:5173/*"
  ],
  "webOrigins": [
    "http://localhost:30081",
    "http://localhost:5173"
  ],
  "attributes": {
    "pkce.code.challenge.method": "S256",
    "post.logout.redirect.uris": "http://localhost:30081/*##http://localhost:5173/*"
  }
}
JSON
)

if [ -z "${CLIENT_UUID}" ]; then
  curl -s -X POST "${KEYCLOAK_URL}/admin/realms/${REALM}/clients" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "${CLIENT_CONFIG}" > /dev/null

  echo "Client ${CLIENT_ID} créé."
else
  curl -s -X PUT "${KEYCLOAK_URL}/admin/realms/${REALM}/clients/${CLIENT_UUID}" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "${CLIENT_CONFIG}" > /dev/null

  echo "Client ${CLIENT_ID} mis à jour."
fi

create_or_update_user() {
  local USERNAME="$1"
  local EMAIL="$2"
  local FIRST_NAME="$3"
  local LAST_NAME="$4"
  shift 4
  local ROLES=("$@")

  echo ""
  echo "Utilisateur : ${USERNAME}"

  USER_UUID=$(curl -s \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    "${KEYCLOAK_URL}/admin/realms/${REALM}/users?username=${USERNAME}&exact=true" \
    | python3 -c "import sys,json; data=json.load(sys.stdin); print(data[0]['id'] if data else '')")

  USER_CONFIG=$(cat <<JSON
{
  "username": "${USERNAME}",
  "enabled": true,
  "emailVerified": true,
  "email": "${EMAIL}",
  "firstName": "${FIRST_NAME}",
  "lastName": "${LAST_NAME}"
}
JSON
)

  if [ -z "${USER_UUID}" ]; then
    curl -s -X POST "${KEYCLOAK_URL}/admin/realms/${REALM}/users" \
      -H "Authorization: Bearer ${ADMIN_TOKEN}" \
      -H "Content-Type: application/json" \
      -d "${USER_CONFIG}" > /dev/null

    USER_UUID=$(curl -s \
      -H "Authorization: Bearer ${ADMIN_TOKEN}" \
      "${KEYCLOAK_URL}/admin/realms/${REALM}/users?username=${USERNAME}&exact=true" \
      | python3 -c "import sys,json; data=json.load(sys.stdin); print(data[0]['id'] if data else '')")

    echo "  Créé."
  else
    curl -s -X PUT "${KEYCLOAK_URL}/admin/realms/${REALM}/users/${USER_UUID}" \
      -H "Authorization: Bearer ${ADMIN_TOKEN}" \
      -H "Content-Type: application/json" \
      -d "${USER_CONFIG}" > /dev/null

    echo "  Mis à jour."
  fi

  curl -s -X PUT "${KEYCLOAK_URL}/admin/realms/${REALM}/users/${USER_UUID}/reset-password" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{
      \"type\": \"password\",
      \"value\": \"${DEFAULT_PASSWORD}\",
      \"temporary\": false
    }" > /dev/null

  ROLE_ARRAY="["
  FIRST=true

  for ROLE in "${ROLES[@]}"; do
    ROLE_JSON=$(curl -s \
      -H "Authorization: Bearer ${ADMIN_TOKEN}" \
      "${KEYCLOAK_URL}/admin/realms/${REALM}/roles/${ROLE}")

    if [ "${FIRST}" = true ]; then
      ROLE_ARRAY="${ROLE_ARRAY}${ROLE_JSON}"
      FIRST=false
    else
      ROLE_ARRAY="${ROLE_ARRAY},${ROLE_JSON}"
    fi
  done

  ROLE_ARRAY="${ROLE_ARRAY}]"

  curl -s -X POST "${KEYCLOAK_URL}/admin/realms/${REALM}/users/${USER_UUID}/role-mappings/realm" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "${ROLE_ARRAY}" > /dev/null

  echo "  Mot de passe : ${DEFAULT_PASSWORD}"
  echo "  Rôles        : ${ROLES[*]}"
}

echo ""
echo "5. Création des utilisateurs de démonstration..."

create_or_update_user "lea.martin" "lea.martin@example.com" "Lea" "Martin" MEMBER
create_or_update_user "sara.bernard" "sara.bernard@example.com" "Sara" "Bernard" MEMBER SECRETARY
create_or_update_user "marc.durand" "marc.durand@example.com" "Marc" "Durand" MEMBER TEACHER
create_or_update_user "paul.moreau" "paul.moreau@example.com" "Paul" "Moreau" MEMBER PRESIDENT

echo ""
echo "6. Test de récupération d'un token utilisateur..."

USER_TOKEN_SIZE=$(curl -s -X POST "${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=${CLIENT_ID}" \
  -d "username=lea.martin" \
  -d "password=${DEFAULT_PASSWORD}" \
  | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('access_token','')))")

if [ "${USER_TOKEN_SIZE}" -gt 0 ]; then
  echo "Token utilisateur OK. Taille du token : ${USER_TOKEN_SIZE}"
else
  echo "ERREUR : impossible de récupérer un token utilisateur."
  exit 1
fi

echo ""
echo "=================================================="
echo "Configuration Keycloak Odoru terminée avec succès."
echo ""
echo "Comptes créés :"
echo "  lea.martin / secret123       -> MEMBER"
echo "  sara.bernard / secret123     -> MEMBER + SECRETARY"
echo "  marc.durand / secret123      -> MEMBER + TEACHER"
echo "  paul.moreau / secret123      -> MEMBER + PRESIDENT"
echo "=================================================="
