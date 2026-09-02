#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
target="${1:-}"
if [[ "$target" != "sres-dev" ]]; then echo "Uso: $0 sres-dev" >&2; exit 2; fi
read -r -p "Digite RESET sres-dev para remover os volumes de desenvolvimento: " confirmation
if [[ "$confirmation" != "RESET sres-dev" ]]; then echo "Confirmação não aceita; nada foi removido." >&2; exit 3; fi
docker compose --project-name "$target" down --volumes --remove-orphans
