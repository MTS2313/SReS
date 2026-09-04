# TASK-002 — Scripts da VPS, healthcheck e rollback

## Estado

approved

## Objetivo

Implementar instalação e operação segura na VPS sem checkout permanente, com deploy por SHA, lock, health check HTTP, histórico simples e rollback sem manipular migrations ou volumes.

## Escopo

- `deploy/bootstrap.sh`;
- `deploy/scripts/install.sh`;
- `sres-deploy`, `sres-healthcheck`, `sres-rollback`;
- validação estrita de SHA e envs;
- `flock`, atomicidade dos registros e rollback da imagem;
- proteção de logs, permissões e ausência de comandos destrutivos.

## Fora do escopo

GitHub Actions, publicação GHCR, Nginx/TLS, backup completo, nova funcionalidade da API e alteração de migrations.

## TDD/validação

Criar testes falhos para SHA inválida, env ausente, lock concorrente, health 5xx, rollback e preservação de volumes. Usar fixtures descartáveis explicitamente identificadas como teste; nunca apontar para `/opt/sres` real sem confirmação.

## Critérios de conclusão

- apenas SHA de 40 hexadecimais é aceita;
- shell injection não é possível pelo argumento;
- deploy exige health HTTP `UP`;
- falha tenta rollback apenas para release anterior válida;
- lock impede deploy concorrente;
- bootstrap não sobrescreve env existente;
- scripts instalados com ownership/permissões documentados;
- rollback não executa `clean`, `down -v`, remove volume ou downgrade de schema.

## Resposta esperada

Relatar testes, simulação de falhas, inspeção dos comandos destrutivos, logs sanitizados e estado recomendado.
