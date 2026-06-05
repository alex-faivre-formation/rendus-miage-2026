#!/usr/bin/env bash
set -euo pipefail

# ==========================================================
# Seed des données métier Odoru dans Kubernetes
# ==========================================================

NAMESPACE="${NAMESPACE:-odoru}"
LOCAL_PORT="${LOCAL_PORT:-8081}"
SERVICE_NAME="member-service"

echo "=================================================="
echo "Seed des données métier Odoru"
echo "Namespace : ${NAMESPACE}"
echo "Service   : ${SERVICE_NAME}"
echo "=================================================="

kubectl wait --for=condition=available deployment/member-service -n "${NAMESPACE}" --timeout=180s

echo ""
echo "Ouverture temporaire d'un port-forward vers member-service..."

kubectl port-forward -n "${NAMESPACE}" "svc/${SERVICE_NAME}" "${LOCAL_PORT}:8081" >/tmp/odoru-member-port-forward.log 2>&1 &
PF_PID=$!

cleanup() {
  echo ""
  echo "Arrêt du port-forward..."
  kill "${PF_PID}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

sleep 5

create_member_if_absent() {
  local USERNAME="$1"
  local FIRST_NAME="$2"
  local LAST_NAME="$3"
  local EMAIL="$4"
  local CITY="$5"
  local COUNTRY="$6"

  echo ""
  echo "Vérification du membre ${USERNAME}..."

  HTTP_CODE=$(curl -s -o "/tmp/${USERNAME}.json" -w "%{http_code}" \
    "http://localhost:${LOCAL_PORT}/api/members/username/${USERNAME}" || true)

  if [ "${HTTP_CODE}" = "200" ]; then
    echo "  Existe déjà."
    cat "/tmp/${USERNAME}.json"
    echo ""
    return
  fi

  echo "  Création..."

  curl -s -i -X POST "http://localhost:${LOCAL_PORT}/api/members" \
    -H "Content-Type: application/json" \
    -d "{
      \"username\": \"${USERNAME}\",
      \"password\": \"secret123\",
      \"firstName\": \"${FIRST_NAME}\",
      \"lastName\": \"${LAST_NAME}\",
      \"email\": \"${EMAIL}\",
      \"city\": \"${CITY}\",
      \"country\": \"${COUNTRY}\",
      \"expertiseLevel\": \"BEGINNER\",
      \"medicalCertificateProvided\": true,
      \"membershipFeePaid\": true
    }"

  echo ""
}

create_member_if_absent "lea.martin" "Lea" "Martin" "lea.martin@example.com" "Toulouse" "France"
create_member_if_absent "sara.bernard" "Sara" "Bernard" "sara.bernard@example.com" "Toulouse" "France"
create_member_if_absent "marc.durand" "Marc" "Durand" "marc.durand@example.com" "Toulouse" "France"
create_member_if_absent "paul.moreau" "Paul" "Moreau" "paul.moreau@example.com" "Toulouse" "France"

echo ""
echo "Vérification finale des membres :"

curl -s "http://localhost:${LOCAL_PORT}/api/members"
echo ""

echo ""
echo "=================================================="
echo "Seed métier terminé."
echo "Comptes métier disponibles :"
echo "  lea.martin"
echo "  sara.bernard"
echo "  marc.durand"
echo "  paul.moreau"
echo "=================================================="
