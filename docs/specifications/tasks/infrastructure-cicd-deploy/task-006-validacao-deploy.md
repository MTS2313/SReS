# TASK-006 — Validação ponta a ponta de deploy

## Estado

planned

## Objetivo

Comprovar em ambiente descartável ou VPS explicitamente autorizada que a imagem é publicada, instalada, iniciada, validada, acessível por HTTPS e revertida com segurança.

## Escopo

- ambiente novo com Compose de produção;
- pull GHCR por credencial de leitura;
- Flyway em banco controlado e JPA `ddl-auto=validate`;
- health PostgreSQL/MinIO/Keycloak/API;
- smoke `/actuator/health`, `/v3/api-docs` quando habilitado e endpoint protegido sem token → 401;
- deploy de duas SHAs controladas;
- falha determinística e rollback;
- confirmação de volumes e dados preservados;
- backup/restauração de PostgreSQL, MinIO e Keycloak;
- documentação final e troubleshooting.

## Fora do escopo

Deploy produtivo irreversível, alteração de negócio, rollback de schema, HA, múltiplas VPS, benchmark produtivo Ollama e Telegram real obrigatório.

## TDD/validação

Criar primeiro cenário falho de integração global que detecte lacunas reais. Executar em ambiente identificado; não mascarar falhas de tasks anteriores. Validar concorrência de deploy, health lento, falha de pull, falha de startup e rollback.

## Critérios de conclusão

- build e pull por SHA funcionam;
- API não depende de Maven/source tree em runtime;
- serviços persistentes sobrevivem a recriação dos containers;
- Ollama/Telegram desabilitados permitem startup;
- smoke HTTPS e autenticação passam;
- rollback de imagem passa sem desfazer migration nem devolver dados;
- backups são restauráveis;
- nenhum secret aparece em imagem, GitHub, logs ou documentação;
- todas as tasks anteriores permanecem inalteradas.

## Resposta esperada

Relatar Red/Green/Refactor, comandos, releases/SHA, health, smoke, rollback, backups, volumes, inspeção de secrets, limitações e estado final da iniciativa.

