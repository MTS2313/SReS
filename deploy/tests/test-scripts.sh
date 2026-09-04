#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SCRIPTS_DIR="$ROOT_DIR/deploy/scripts"
TEST_DIR="$(mktemp -d)"
trap 'rm -rf -- "$TEST_DIR"' EXIT

assert_failure() {
  local label="$1"
  shift
  if "$@" >/dev/null 2>&1; then
    echo "Falha esperada não ocorreu: $label" >&2
    exit 3
  fi
}
assert_value() {
  [[ -f "$1" && "$(<"$1")" == "$2" ]] || { echo "Valor inesperado em $1" >&2; exit 4; }
}
for script in install.sh sres-common sres-deploy sres-healthcheck sres-rollback; do
  [[ -x "$SCRIPTS_DIR/$script" ]] || { echo "Script ausente: $script" >&2; exit 2; }
done
assert_failure "SHA curta" "$SCRIPTS_DIR/sres-deploy" 0123456789abcdef
assert_failure "argumento extra" "$SCRIPTS_DIR/sres-rollback" unexpected
assert_failure "imagem com tag arbitrária" env SRES_API_IMAGE=ghcr.io/acme/sres-api:latest "$SCRIPTS_DIR/sres-deploy" 0123456789abcdef0123456789abcdef01234567

if grep -R -n -E 'docker compose down -v|docker volume rm|docker system prune|docker image prune|flyway[[:space:]]+clean|rm[[:space:]]+-rf' "$ROOT_DIR/deploy/scripts" "$ROOT_DIR/deploy/bootstrap.sh"; then
  echo "Comando destrutivo encontrado" >&2
  exit 5
fi
if grep -R -n -E '(^|[[:space:]])eval([[:space:]]|$)' "$ROOT_DIR/deploy/scripts" "$ROOT_DIR/deploy/bootstrap.sh"; then
  echo "eval não é permitido" >&2
  exit 6
fi

FAKE_BIN="$TEST_DIR/bin"
mkdir -p "$FAKE_BIN"
for fake in fake-curl fake-health fake-docker; do
  cp "$ROOT_DIR/deploy/tests/fixtures/$fake" "$FAKE_BIN/$fake"
done
ln -s "$FAKE_BIN/fake-curl" "$FAKE_BIN/curl"
ln -s "$FAKE_BIN/fake-docker" "$FAKE_BIN/docker"
chmod +x "$FAKE_BIN"/*

APP_ENV="$TEST_DIR/app.env"
DEPLOY_ENV="$TEST_DIR/deploy.env"
: > "$APP_ENV"
printf '%s\n' "SRES_API_IMAGE=ghcr.io/acme/sres-api" "SRES_APP_ENV_FILE=$APP_ENV" "SRES_COMPOSE_PROJECT=sres-test" > "$DEPLOY_ENV"
HEALTH_ENV=(PATH="$FAKE_BIN:$PATH" FAKE_CURL_MAX_FILE="$TEST_DIR/curl-max" SRES_DEPLOY_ENV_FILE="$DEPLOY_ENV" SRES_HEALTH_URL=http://127.0.0.1/actuator/health SRES_HEALTH_RETRIES=1 SRES_HEALTH_INTERVAL_SECONDS=0 SRES_HEALTH_TIMEOUT_SECONDS=1 SRES_HEALTH_GLOBAL_TIMEOUT_SECONDS=1)
assert_failure "health status DOWN" env "${HEALTH_ENV[@]}" FAKE_CURL_MODE=down "$SCRIPTS_DIR/sres-healthcheck"
assert_failure "health timeout" env "${HEALTH_ENV[@]}" FAKE_CURL_MODE=timeout "$SCRIPTS_DIR/sres-healthcheck"
assert_value "$TEST_DIR/curl-max" 1

STATE_DIR="$TEST_DIR/releases"
COMMON_ENV=(PATH="$FAKE_BIN:$PATH" SRES_DOCKER_BIN=docker SRES_HEALTHCHECK_BIN=fake-health SRES_COMPOSE_FILE="$ROOT_DIR/deploy/compose.prod.yml" SRES_DEPLOY_ENV_FILE="$DEPLOY_ENV" SRES_STATE_DIR="$STATE_DIR" SRES_LOCK_FILE="$TEST_DIR/deploy.lock" FAKE_DOCKER_LOG="$TEST_DIR/docker.log" FAKE_HEALTH_COUNT_FILE="$TEST_DIR/health-count" SRES_HEALTH_RETRIES=1 SRES_HEALTH_GLOBAL_TIMEOUT_SECONDS=1)
sha_one=0123456789abcdef0123456789abcdef01234567
sha_two=abcdef0123456789abcdef0123456789abcdef01
env "${COMMON_ENV[@]}" FAKE_HEALTH_SEQUENCE=up "$SCRIPTS_DIR/sres-deploy" "$sha_one" >/dev/null
assert_value "$STATE_DIR/current" "$sha_one"
[[ ! -e "$STATE_DIR/previous" ]] || { echo "previous indevido" >&2; exit 7; }

printf '0\n' > "$TEST_DIR/health-count"
set +e
env "${COMMON_ENV[@]}" FAKE_HEALTH_SEQUENCE=down,up "$SCRIPTS_DIR/sres-deploy" "$sha_two" >/dev/null 2>&1
rc=$?
set -e
[[ "$rc" -ne 0 ]] || { echo "deploy deveria falhar" >&2; exit 8; }
assert_value "$STATE_DIR/current" "$sha_one"

printf '%s\n' "$sha_two" > "$STATE_DIR/previous"
printf '0\n' > "$TEST_DIR/health-count"
env "${COMMON_ENV[@]}" FAKE_HEALTH_SEQUENCE=up "$SCRIPTS_DIR/sres-rollback" >/dev/null
assert_value "$STATE_DIR/current" "$sha_two"
assert_value "$STATE_DIR/previous" "$sha_one"

rm -f -- "$STATE_DIR/previous"
assert_failure "rollback sem release anterior" env "${COMMON_ENV[@]}" "$SCRIPTS_DIR/sres-rollback"

assert_failure "pull falho sem release anterior" env "${COMMON_ENV[@]}" FAKE_DOCKER_PULL_FAIL=1 "$SCRIPTS_DIR/sres-deploy" "$sha_one"

printf '0\n' > "$TEST_DIR/health-count"
env "${COMMON_ENV[@]}" FAKE_DOCKER_SLEEP=2 FAKE_HEALTH_SEQUENCE=up "$SCRIPTS_DIR/sres-deploy" "$sha_two" >/dev/null 2>&1 &
first_pid=$!
sleep 0.2
assert_failure "deploy concorrente" env "${COMMON_ENV[@]}" FAKE_HEALTH_SEQUENCE=up "$SCRIPTS_DIR/sres-deploy" "$sha_two"
wait "$first_pid"

INSTALL_ROOT="$TEST_DIR/install-root"
INSTALL_CONFIG="$TEST_DIR/install-config"
INSTALL_BIN="$TEST_DIR/install-bin"
mkdir -p "$INSTALL_ROOT" "$INSTALL_CONFIG" "$INSTALL_BIN"
printf 'preserve-app\n' > "$INSTALL_ROOT/app.env"
printf 'preserve-deploy\n' > "$INSTALL_CONFIG/deploy.env"
env SRES_SOURCE_ROOT="$ROOT_DIR" SRES_INSTALL_ROOT="$INSTALL_ROOT" SRES_INSTALL_CONFIG_DIR="$INSTALL_CONFIG" SRES_INSTALL_BIN_DIR="$INSTALL_BIN" "$SCRIPTS_DIR/install.sh" >/dev/null
assert_value "$INSTALL_ROOT/app.env" preserve-app
assert_value "$INSTALL_CONFIG/deploy.env" preserve-deploy
[[ "$(stat -c '%a' "$INSTALL_ROOT/app.env")" == 600 && "$(stat -c '%a' "$INSTALL_CONFIG/deploy.env")" == 600 ]] || { echo "permissões inseguras" >&2; exit 9; }
echo "Scripts operacionais validados."
