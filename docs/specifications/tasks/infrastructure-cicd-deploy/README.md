# Infraestrutura, CI/CD e deploy do SReS

## Estado da iniciativa

- Estado: ready
- Branch-base: main
- API MVP: concluído
- TASK-001 até TASK-005 foram aprovadas após revisão; TASK-006 é a única task liberada para execução.

## Leitura obrigatória

1. [Specification source](../../spec-source/infrastructure-cicd-deploy/README.md)
2. [Plano de execução](../../spec-execution/infrastructure-cicd-deploy/README.md)
3. [Arquitetura do backend](../../../architecture/backend.md)
4. [Contrato da API](../../../architecture/api.md)
5. [Documentação operacional local](../../../operations/local-development.md)
6. `backend/Dockerfile`, `compose.yaml`, `scripts/` e configuração atual do backend

## Ordem das tasks

| Ordem | Task | Estado | Dependências |
|---|---|---|---|
| 1 | [TASK-001 — Estrutura de produção e Compose](task-001-estrutura-compose.md) | approved | API MVP, decisões mínimas da VPS |
| 2 | [TASK-002 — Scripts da VPS, healthcheck e rollback](task-002-scripts-vps-healthcheck-rollback.md) | approved | TASK-001 |
| 3 | [TASK-003 — CI backend e publicação GHCR](task-003-ci-ghcr.md) | approved | TASK-001, acesso GHCR |
| 4 | [TASK-004 — Deploy production via SSH](task-004-deploy-production-ssh.md) | approved | TASK-002, TASK-003, VPS |
| 5 | [TASK-005 — Nginx/TLS e preparação da VPS](task-005-nginx-tls-vps.md) | approved | TASK-001, TASK-002, TASK-004, DNS |
| 6 | [TASK-006 — Validação ponta a ponta de deploy](task-006-validacao-deploy.md) | ready | TASK-001 a TASK-005, backups |

## Regra de avanço

Somente a TASK-006 está liberada (`ready`). As tasks anteriores, incluindo a TASK-005, foram aprovadas após revisão.

## Fora do escopo

Kubernetes, k3s, frontend, autoscaling, multi-region, múltiplas VPS, blue/green sofisticado, Prometheus/Grafana, tracing externo, Terraform complexo, RAG, embeddings e escolha produtiva definitiva do Ollama.
