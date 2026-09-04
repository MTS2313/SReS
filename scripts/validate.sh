#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
test -f backend/pom.xml
test -x backend/mvnw
test -d backend/.mvn
test -d backend/src
test ! -e pom.xml
test ! -e mvnw
test ! -e .mvn
test ! -e src
java -version 2>&1 | rg 'version "21' >/dev/null
test -f .env.example && test -f infra/keycloak/sres-dev-realm.json
test -f backend/Dockerfile
test -f .dockerignore
! rg -n '(PASSWORD|TOKEN|SECRET)\s*=' backend/Dockerfile .dockerignore
test -f backend/src/main/resources/db/migration/V1__bootstrap.sql
! rg -n 'ddl-auto:\s*(create|create-drop|update)|spring\.jpa\.hibernate\.ddl-auto=(create|create-drop|update)' backend/src
! rg -n 'OLLAMA_ENABLED:true|TELEGRAM_ENABLED:true' .env.example compose.yaml backend/src
(cd backend && ./mvnw -B clean verify)
