#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/deploy/compose.prod.yml"
ENV_FILE="${1:-}"

[[ -f "$COMPOSE_FILE" ]] || { echo "Compose de produção ausente: $COMPOSE_FILE" >&2; exit 2; }
[[ -f "$ROOT_DIR/deploy/postgres-init/01-keycloak-schema.sql" ]] || { echo "Init SQL do schema Keycloak ausente" >&2; exit 2; }
[[ -n "$ENV_FILE" && -f "$ENV_FILE" ]] || { echo "Env de validação ausente" >&2; exit 2; }

CONFIG_ARGS=(--env-file "$ENV_FILE" -f "$COMPOSE_FILE")
rendered="$(docker compose "${CONFIG_ARGS[@]}" config)"

for service in api postgres minio keycloak; do
  grep -Eq "^  ${service}:$" <<<"$rendered" || {
    echo "Serviço ausente: $service" >&2
    exit 3
  }
done

grep -Eq '^    image: .*sres-api:' <<<"$rendered"
grep -Eq '^    restart: (unless-stopped|always|on-failure)' <<<"$rendered"
grep -Eq '^    healthcheck:' <<<"$rendered"
grep -Eq '127\.0\.0\.1:' <<<"$rendered"
grep -Eq 'sres_postgres_data|sres_minio_data|sres_keycloak_data' <<<"$rendered"
grep -Eq 'pg_isready' <<<"$rendered"
grep -Eq '\bmc\b' <<<"$rendered"
grep -Eq '\bready\b' <<<"$rendered"
grep -Eq '/dev/tcp/127\.0\.0\.1/9000' <<<"$rendered"
grep -Eq '/actuator/health|/dev/tcp/127\.0\.0\.1/8080' <<<"$rendered"

for service in postgres; do
  if awk -v service="$service" '
    $0 == "  " service ":" { in_service=1; next }
    in_service && $0 ~ /^  [[:alnum:]_-]+:$/ { in_service=0 }
    in_service && $0 ~ /^    ports:$/ { found=1 }
    END { exit found ? 0 : 1 }
  ' <<<"$rendered"; then
    echo "Serviço de infraestrutura expõe porta diretamente: $service" >&2
    exit 4
  fi
done

awk -v service="minio" '
  $0 == "  " service ":" { in_service=1; next }
  in_service && $0 ~ /^  [[:alnum:]_-]+:$/ { in_service=0 }
  in_service && $0 ~ /^    ports:$/ { found=1 }
  END { exit found ? 0 : 1 }
' <<<"$rendered" || {
  echo "MinIO S3 precisa de publicação loopback para o Nginx" >&2
  exit 4
}
grep -Eq 'host_ip: 127\.0\.0\.1' <<<"$rendered" && grep -Eq 'target: 9000' <<<"$rendered" || {
  echo "MinIO não está restrito a loopback" >&2
  exit 4
}

! grep -Eq 'sres-dev-realm|start-dev|latest|privileged: true|/var/run/docker.sock' "$COMPOSE_FILE"
! grep -Eiq 'password: *[^$].*(change-me|secret|admin)' "$COMPOSE_FILE"
! grep -Eq 'ollama:|telegram:' "$COMPOSE_FILE"

echo "Compose de produção validado estruturalmente."
