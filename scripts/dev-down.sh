#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
# No --volumes: stopping preserves development data.
docker compose --project-name sres-dev down --remove-orphans
