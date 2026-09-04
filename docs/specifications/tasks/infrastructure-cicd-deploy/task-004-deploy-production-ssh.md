# TASK-004 — Deploy production via SSH

## Estado

approved

## Objetivo

Conectar publicação GHCR a uma VPS por GitHub Environment, com SSH conhecido, usuário dedicado, flag de habilitação e execução de script fixo.

## Escopo

- `.github/workflows/deploy-production.yml`;
- `workflow_dispatch`, push em `main` e path filters relevantes;
- `environment: production`;
- `concurrency` sem cancelamento;
- `VPS_HOST`, `VPS_PORT`, `VPS_USER`, `VPS_SSH_PRIVATE_KEY`, `VPS_SSH_KNOWN_HOSTS`;
- `PRODUCTION_DEPLOY_ENABLED` como variable;
- chamada única a `/usr/local/bin/sres-deploy <sha>`.

## Fora do escopo

Provisionamento da VPS, emissão TLS, secrets da aplicação, Docker socket no runner e comandos arbitrários remotos.

## TDD/validação

Testar workflow parseável, flag false sem SSH, known hosts obrigatório, permissions mínimas, concurrency e path filters. Usar ambiente de teste explicitamente identificado para simular SSH; não conectar a produção sem autorização.

## Critérios de conclusão

- flag false testa/publica sem conectar;
- flag true usa Environment e chave exclusiva;
- host key checking permanece ativo;
- não há secrets da aplicação em Actions;
- dois deploys não executam simultaneamente;
- SHA publicada é exatamente a enviada ao script.

## Resposta esperada

Relatar validação YAML, teste da flag, inspeção de permissions/secrets, evidência de execução autorizada e estado recomendado.
