#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
docker compose --project-name sres-dev up -d --wait
docker compose --project-name sres-dev ps
