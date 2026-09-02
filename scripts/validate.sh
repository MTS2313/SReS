#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
java -version 2>&1 | rg 'version "21' >/dev/null
test -f .env.example && test -f infra/keycloak/sres-dev-realm.json
test -f src/main/resources/db/migration/V1__bootstrap.sql
! rg -n 'ddl-auto:\s*(create|create-drop|update)|spring\.jpa\.hibernate\.ddl-auto=(create|create-drop|update)' src
! rg -n 'OLLAMA_ENABLED:true|TELEGRAM_ENABLED:true' .env.example compose.yaml src
./mvnw -B clean verify
