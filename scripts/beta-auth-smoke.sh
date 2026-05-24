#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${CAMPUSHUB_BASE_URL:-https://ustc.suntomb.qzz.io}"
STUDENT_EMAIL="${CAMPUSHUB_SMOKE_STUDENT_EMAIL:-}"
STUDENT_PASSWORD="${CAMPUSHUB_SMOKE_STUDENT_PASSWORD:-}"
ADMIN_EMAIL="${CAMPUSHUB_SMOKE_ADMIN_EMAIL:-}"
ADMIN_PASSWORD="${CAMPUSHUB_SMOKE_ADMIN_PASSWORD:-}"

if [[ -z "$STUDENT_EMAIL" || -z "$STUDENT_PASSWORD" || -z "$ADMIN_EMAIL" || -z "$ADMIN_PASSWORD" ]]; then
  echo "Missing CAMPUSHUB_SMOKE_STUDENT_EMAIL/PASSWORD or CAMPUSHUB_SMOKE_ADMIN_EMAIL/PASSWORD" >&2
  exit 2
fi

pass_count=0
fail_count=0

check_status() {
  local label="$1"
  local method="$2"
  local path="$3"
  local expected="$4"
  local token="${5:-}"
  local body="${6:-}"
  local auth_args=()
  local body_args=()
  if [[ -n "$token" ]]; then
    auth_args=(-H "Authorization: Bearer $token")
  fi
  if [[ -n "$body" ]]; then
    body_args=(-H "Content-Type: application/json" --data "$body")
  fi
  local status
  status=$(curl -sS -o "/tmp/campushub-smoke-response.$$" -w "%{http_code}" -X "$method" "${BASE_URL}${path}" "${auth_args[@]}" "${body_args[@]}" || true)
  rm -f "/tmp/campushub-smoke-response.$$"
  if [[ "$status" == "$expected" ]]; then
    echo "PASS $label $method $path -> $status"
    pass_count=$((pass_count + 1))
  else
    echo "FAIL $label $method $path expected $expected got $status"
    fail_count=$((fail_count + 1))
  fi
}

login() {
  local email="$1"
  local password="$2"
  local response
  response=$(curl -sS -X POST "${BASE_URL}/api/auth/login" -H "Content-Type: application/json" --data "{\"emailOrUsername\":\"${email}\",\"password\":\"${password}\"}")
  python3 -c 'import json,sys; data=json.load(sys.stdin); print(data.get("data", {}).get("token", ""))' <<<"$response"
}

check_status "anonymous public goods" GET "/api/goods" 200
check_status "anonymous public tasks" GET "/api/tasks" 200
check_status "anonymous write blocked" POST "/api/goods" 401 "" '{}'
check_status "anonymous admin blocked" GET "/api/admin/wallet/recharges" 401

student_token=$(login "$STUDENT_EMAIL" "$STUDENT_PASSWORD")
if [[ -z "$student_token" ]]; then
  echo "FAIL student login did not return token"
  exit 1
fi
echo "PASS student login returned token"
pass_count=$((pass_count + 1))

admin_token=$(login "$ADMIN_EMAIL" "$ADMIN_PASSWORD")
if [[ -z "$admin_token" ]]; then
  echo "FAIL admin login did not return token"
  exit 1
fi
echo "PASS admin login returned token"
pass_count=$((pass_count + 1))

check_status "student me" GET "/api/auth/me" 200 "$student_token"
check_status "student admin forbidden" GET "/api/admin/wallet/recharges" 403 "$student_token"
check_status "student private mismatch forbidden" GET "/api/wallet/users/1" 403 "$student_token"
check_status "admin me" GET "/api/auth/me" 200 "$admin_token"
check_status "admin wallet" GET "/api/admin/wallet/recharges" 200 "$admin_token"
check_status "admin payment" GET "/api/admin/payment/orders" 200 "$admin_token"
check_status "admin ops" GET "/api/admin/ops/analytics/overview" 200 "$admin_token"
check_status "admin governance" GET "/api/admin/governance/dashboard" 200 "$admin_token"

if [[ "$fail_count" -gt 0 ]]; then
  echo "CampusHub beta auth smoke failed: ${fail_count} failed, ${pass_count} passed"
  exit 1
fi

echo "CampusHub beta auth smoke passed: ${pass_count} checks"
