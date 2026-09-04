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
java -version 2>&1 | grep -E 'version "21' >/dev/null
test -f .env.example && test -f infra/keycloak/sres-dev-realm.json
test -f backend/Dockerfile
test -f .dockerignore
! grep -En '(PASSWORD|TOKEN|SECRET)[[:space:]]*=' backend/Dockerfile .dockerignore
test -f backend/src/main/resources/db/migration/V1__bootstrap.sql
! grep -REn 'ddl-auto:[[:space:]]*(create|create-drop|update)|spring\.jpa\.hibernate\.ddl-auto=(create|create-drop|update)' backend/src
! grep -REn 'OLLAMA_ENABLED:true|TELEGRAM_ENABLED:true' .env.example compose.yaml backend/src
(cd backend && ./mvnw -B clean verify)
