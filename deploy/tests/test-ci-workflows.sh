#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
WORKFLOW_DIR="$ROOT_DIR/.github/workflows"

backend="$WORKFLOW_DIR/backend.yml"
publish="$WORKFLOW_DIR/publish-image.yml"
[[ -f "$backend" ]] || { echo "Workflow backend.yml ausente" >&2; exit 2; }
[[ -f "$publish" ]] || { echo "Workflow publish-image.yml ausente" >&2; exit 2; }

has() { grep -Fq -- "$2" "$1" || { echo "Contrato ausente em $1: $2" >&2; exit 3; }; }
has "$backend" 'pull_request:'
has "$backend" 'push:'
has "$backend" "java-version: '21'"
has "$backend" 'distribution: temurin'
has "$backend" 'cache: maven'
has "$backend" 'scripts/validate.sh'
has "$backend" 'permissions:'
has "$backend" 'contents: read'
! grep -Fq 'packages: write' "$backend"
! grep -Fq 'pull_request_target' "$backend"
! grep -Eiq 'ssh|VPS|sres-deploy' "$backend"

has "$publish" 'workflow_call:'
has "$publish" 'workflow_dispatch:'
has "$publish" 'contents: read'
has "$publish" 'needs: validate'
has "$publish" 'docker/setup-buildx-action@v3'
has "$publish" 'docker/login-action@v3'
has "$publish" 'password: ${{ secrets.GITHUB_TOKEN }}'
has "$publish" 'docker/build-push-action@v6'
has "$publish" 'context: .'
has "$publish" 'file: ./backend/Dockerfile'
has "$publish" 'packages: write'
has "$publish" 'push: true'
has "$publish" 'github.sha'
has "$publish" 'GITHUB_REPOSITORY_OWNER,,}/sres-api'
! grep -Fq 'pull_request:' "$publish"
! grep -Eiq '(^|[^-])latest([[:space:]]|$|:)' "$publish"
! grep -Eiq 'ssh|VPS|sres-deploy|VPS_HOST|VPS_USER' "$publish"

echo "Workflows CI/GHCR validados estruturalmente."
