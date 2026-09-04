#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DEPLOY_DIR="$ROOT_DIR/deploy"
COMPOSE="$DEPLOY_DIR/compose.prod.yml"

fail() { echo "$*" >&2; exit 1; }
for required in \
  "$DEPLOY_DIR/bootstrap.sh" \
  "$DEPLOY_DIR/nginx/sres-api.conf.template" \
  "$DEPLOY_DIR/nginx/sres-auth.conf.template" \
  "$DEPLOY_DIR/nginx/sres-s3.conf.template" \
  "$DEPLOY_DIR/scripts/sres-nginx-check" \
  "$DEPLOY_DIR/scripts/sres-nginx-apply" \
  "$DEPLOY_DIR/sudoers/sres-deploy"; do
  [[ -f "$required" ]] || fail "arquivo esperado ausente: $required"
done

grep -Fq '127.0.0.1:${SRES_API_HOST_PORT:-18081}:8080' "$COMPOSE" || fail "bind API inválido"
grep -Fq '127.0.0.1:${SRES_KEYCLOAK_HOST_PORT:-18083}:8080' "$COMPOSE" || fail "bind Keycloak inválido"
grep -Fq '127.0.0.1:${SRES_MINIO_HOST_PORT:-18084}:9000' "$COMPOSE" || fail "bind MinIO inválido"
! grep -Eq '0\.0\.0\.0|ollama:|telegram:' "$COMPOSE" || fail "exposição/serviço proibido"

grep -Fq 'SSH, TCP 80 e TCP 443' "$DEPLOY_DIR/README.md" || fail "contrato público não explicita SSH, TCP 80 e TCP 443"
grep -Fq 'somente SSH, TCP 80 e TCP 443' "$DEPLOY_DIR/INFRASTRUCTURE.md" || fail "firewall não explicita SSH, TCP 80 e TCP 443"
grep -Fq '18081' "$DEPLOY_DIR/INFRASTRUCTURE.md" || fail "porta loopback da API não documentada"
grep -Fq '18083' "$DEPLOY_DIR/INFRASTRUCTURE.md" || fail "porta loopback do Keycloak não documentada"
grep -Fq '18084' "$DEPLOY_DIR/INFRASTRUCTURE.md" || fail "porta loopback do MinIO não documentada"

for domain in api.sres.morfeu.cloud auth.sres.morfeu.cloud s3.sres.morfeu.cloud; do
  grep -Fq "$domain" "$DEPLOY_DIR/deploy.env.example" || fail "domínio ausente: $domain"
done
grep -R -Fq '__SRES_API_DOMAIN__' "$DEPLOY_DIR/nginx" || fail "placeholder de domínio API ausente"
grep -R -Fq '__SRES_AUTH_DOMAIN__' "$DEPLOY_DIR/nginx" || fail "placeholder de domínio Keycloak ausente"
grep -R -Fq '__SRES_S3_DOMAIN__' "$DEPLOY_DIR/nginx" || fail "placeholder de domínio MinIO ausente"
grep -R -Fq 'client_max_body_size 16m' "$DEPLOY_DIR/nginx" || fail "limite de upload ausente"
for header in Host X-Real-IP X-Forwarded-For X-Forwarded-Proto; do
  grep -R -Fq "$header" "$DEPLOY_DIR/nginx" || fail "header ausente: $header"
done
grep -Fq 'nginx -t' "$DEPLOY_DIR/scripts/sres-nginx-apply" || fail "apply não valida nginx"
grep -Fq 'systemctl reload nginx' "$DEPLOY_DIR/scripts/sres-nginx-apply" || fail "apply não possui reload seguro"
! grep -Fq 'systemctl reload nginx' "$DEPLOY_DIR/scripts/sres-nginx-check" || fail "check não pode fazer reload"
grep -Fq 'visudo -cf' "$ROOT_DIR/deploy/bootstrap.sh" || fail "bootstrap não valida sudoers"
grep -Fq 'sres-deploy' "$DEPLOY_DIR/sudoers/sres-deploy" || fail "sudoers ausente"
grep -Fq 'require_command docker' "$ROOT_DIR/deploy/bootstrap.sh" || fail "bootstrap não valida Docker"
grep -Fq 'docker compose version' "$ROOT_DIR/deploy/bootstrap.sh" || fail "bootstrap não valida Compose"
grep -Fq 'require_command nginx' "$ROOT_DIR/deploy/bootstrap.sh" || fail "bootstrap não valida Nginx"
grep -Fq 'check_port_free 18081' "$ROOT_DIR/deploy/bootstrap.sh" || fail "bootstrap não valida porta API"
grep -Fq 'check_port_free 18083' "$ROOT_DIR/deploy/bootstrap.sh" || fail "bootstrap não valida porta Keycloak"
grep -Fq 'check_port_free 18084' "$ROOT_DIR/deploy/bootstrap.sh" || fail "bootstrap não valida porta MinIO"
! grep -R -n -E 'docker system prune|docker volume prune|docker compose down -v|rm[[:space:]]+-rf[[:space:]]+/(opt|etc|var)' "$DEPLOY_DIR/bootstrap.sh" "$DEPLOY_DIR/scripts" "$DEPLOY_DIR/nginx" || fail "operação destrutiva encontrada"

if "$DEPLOY_DIR/bootstrap.sh" >/dev/null 2>&1; then
  fail "bootstrap deveria exigir root"
fi

echo "Bootstrap e Nginx validados estruturalmente."
