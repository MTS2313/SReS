#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
WORKFLOW="$ROOT_DIR/.github/workflows/deploy-production.yml"
[[ -f "$WORKFLOW" ]] || { echo "deploy-production.yml ausente" >&2; exit 2; }

has() { grep -Fq -- "$2" "$1" || { echo "Contrato ausente: $2" >&2; exit 3; }; }
not_has() { ! grep -Fq -- "$2" "$1" || { echo "Conteúdo proibido: $2" >&2; exit 4; }; }

has "$WORKFLOW" 'push:'
has "$WORKFLOW" 'branches:'
has "$WORKFLOW" '- main'
has "$WORKFLOW" 'workflow_dispatch:'
has "$WORKFLOW" 'environment: production'
has "$WORKFLOW" 'group: sres-production'
has "$WORKFLOW" 'cancel-in-progress: false'
has "$WORKFLOW" "vars.PRODUCTION_DEPLOY_ENABLED == 'true'"
has "$WORKFLOW" 'uses: ./.github/workflows/publish-image.yml'
has "$WORKFLOW" 'needs: publish-image'
has "$WORKFLOW" 'VPS_HOST: ${{ secrets.VPS_HOST }}'
has "$WORKFLOW" 'VPS_PORT: ${{ secrets.VPS_PORT }}'
has "$WORKFLOW" 'VPS_USER: ${{ secrets.VPS_USER }}'
has "$WORKFLOW" 'VPS_SSH_PRIVATE_KEY: ${{ secrets.VPS_SSH_PRIVATE_KEY }}'
has "$WORKFLOW" 'VPS_SSH_KNOWN_HOSTS: ${{ secrets.VPS_SSH_KNOWN_HOSTS }}'
has "$WORKFLOW" 'BatchMode=yes'
has "$WORKFLOW" 'StrictHostKeyChecking=yes'
has "$WORKFLOW" 'UserKnownHostsFile='
has "$WORKFLOW" 'sudo /usr/local/bin/sres-deploy'
has "$WORKFLOW" 'GITHUB_SHA: ${{ github.sha }}'
not_has "$WORKFLOW" 'pull_request_target'
not_has "$WORKFLOW" 'root@'
not_has "$WORKFLOW" 'StrictHostKeyChecking=no'
not_has "$WORKFLOW" 'UserKnownHostsFile=/dev/null'
not_has "$WORKFLOW" 'docker compose'
not_has "$WORKFLOW" 'git pull'
not_has "$WORKFLOW" 'POSTGRES_PASSWORD'
not_has "$WORKFLOW" 'MINIO_ROOT_PASSWORD'
not_has "$WORKFLOW" 'KEYCLOAK_ADMIN_PASSWORD'
not_has "$WORKFLOW" 'TELEGRAM_BOT_TOKEN'
not_has "$WORKFLOW" 'SRES_OLLAMA'

echo "Workflow de deploy SSH validado estruturalmente."
