# TASK-001 — Estrutura de produção e Compose

## Estado

approved

## Objetivo

Criar a estrutura `deploy/` e o Compose de produção para API, PostgreSQL, MinIO e Keycloak em uma única VPS, com rede interna, volumes persistentes, imagens fixadas, healthchecks e envs de exemplo sem secrets.

## Escopo

- `deploy/compose.prod.yml` com serviços `api`, `postgres`, `minio` e `keycloak`;
- `deploy/app.env.example` e `deploy/deploy.env.example`;
- `deploy/README.md` e `deploy/INFRASTRUCTURE.md`;
- rede interna, restart policy, healthchecks e portas somente internas/loopback;
- compatibilidade com API, Flyway, `ddl-auto=validate`, MinIO e issuer Keycloak;
- `.dockerignore` revisado para não enviar secrets, `.git`, target, logs ou temporários.

## Fora do escopo

Workflows, scripts de VPS, SSH, Nginx/TLS automatizado, backups executáveis, Ollama no Compose, Telegram real, frontend e deploy em produção.

## TDD/validação

Antes do Compose final, criar checks falhos para serviços, portas, healthchecks, volumes e ausência de secrets. Validar `docker compose config`, build real da imagem e startup local isolado sem destruir volumes.

## Critérios de conclusão

- Compose aceita configuração somente por env externo;
- nenhuma credencial real versionada;
- API não publica banco, MinIO ou console Keycloak para Internet;
- Ollama e Telegram desligados não impedem a composição;
- volumes nomeados e restart/healthchecks definidos;
- Flyway e JPA validam o banco no startup;
- documentação explica limites, portas e contexto do monorepo.

## Resposta esperada

Relatar arquivos, Red/Green/Refactor, `docker compose config`, build, smoke local, volumes preservados, riscos e `APPROVED_NEXT_TASK`, `NEEDS_CORRECTION` ou `BLOCKED`.
