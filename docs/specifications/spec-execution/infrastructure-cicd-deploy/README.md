# Plano de execução — Infraestrutura, CI/CD e deploy do SReS

## Estado

planned — nenhuma task desta iniciativa foi implementada.

## Regra de execução

Executar uma única task `ready` por vez. A task seguinte só pode ser liberada após revisão da evidência da anterior. Esta iniciativa não altera o estado concluído do API MVP.

## Dependências externas

Antes das tasks de deploy, o responsável deve confirmar:

- acesso administrativo ao repositório GitHub e ao GHCR;
- VPS Linux com Docker Compose plugin, Nginx, Certbot ou mecanismo TLS equivalente;
- CPU, RAM, disco, I/O, firewall e portas disponíveis;
- domínios definidos: `api.sres.morfeu.cloud`, `auth.sres.morfeu.cloud` e `s3.sres.morfeu.cloud`;
- política de backup e destino externo;
- decisão sobre GHCR privado;
- valores de produção fornecidos somente na VPS.

Os domínios da infraestrutura estão definidos como `api.sres.morfeu.cloud`, `auth.sres.morfeu.cloud` e `s3.sres.morfeu.cloud`. A TASK-005 produz a automação local/versionada mesmo sem acesso SSH do agente; acesso à VPS, secrets e DNS reais são necessários somente para a execução posterior da TASK-006.

## Fases

| Fase | Task | Dependências | Resultado |
|---|---|---|---|
| 1 | TASK-001 — Estrutura de produção e Compose | API MVP, Dockerfile, decisões de VPS, domínio/ports conceituais | `deploy/`, Compose validado, envs exemplo, volumes, rede e healthchecks |
| 2 | TASK-002 — Scripts da VPS, healthcheck e rollback | TASK-001 | install/bootstrap, deploy, healthcheck, rollback, lock e histórico |
| 3 | TASK-003 — CI backend e publicação GHCR | TASK-001, GHCR e GitHub permissions | `backend.yml`, build/teste, imagem SHA imutável |
| 4 | TASK-004 — Deploy production via SSH | TASK-002, TASK-003, GitHub Environment e VPS preparada | workflow de produção, flag habilitável e SSH restrito |
| 5 | TASK-005 — Bootstrap, Nginx/TLS e preparação reproduzível da VPS | TASK-001/002/004, Docker/Nginx na VPS, DNS e TLS | bootstrap, usuário, permissões, templates, reverse proxy, headers e procedimento HTTPS |
| 6 | TASK-006 — Validação ponta a ponta de deploy | TASK-001 a TASK-005, acesso à VPS e secrets controlados | execução real do bootstrap/deploy, HTTPS, rollback, persistência e evidências finais |

## Critério de interrupção

Interromper e reportar se houver secret no repositório/imagem/log, porta pública indevida, SSH root, sudo irrestrito, SHA não validada, Compose incompatível, health falso, migration destrutiva sem plano, ausência de backup mínimo ou divergência entre variáveis da API e os templates.

## Estratégia de validação comum

- `docker compose config` com arquivos de exemplo;
- `shellcheck` quando disponível;
- validação YAML;
- `docker build -f backend/Dockerfile .`;
- inspeção de history/config da imagem;
- testes de entradas inválidas dos scripts;
- health check com serviço saudável, lento, ausente e HTTP 5xx;
- lock concorrente;
- rollback local com duas SHAs controladas;
- confirmação de que volumes permanecem após deploy/down;
- inspeção de logs sem secrets;
- `git diff --check`, `git status --short` e `git diff --stat`.

## Evidência e resposta

Cada task deve registrar Red, Green, Refactor quando houver código/script testável, comandos executados, ambiente, limitações, arquivos alterados e estado recomendado: `APPROVED_NEXT_TASK`, `NEEDS_CORRECTION` ou `BLOCKED`.
