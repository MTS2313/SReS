# API MVP do SReS

## Estado da iniciativa

- Estado: completed
- Branch-base: main
- Última revisão: 2026-09-04

## Objetivo

Implementar de ponta a ponta a API MVP documentada do SReS, incluindo ambiente local, segurança, domínios, processamento Ollama, Telegram, testes e entrega operacional, sem frontend.

## Leitura obrigatória

1. [Fonte da specification](../../spec-source/api-mvp/README.md)
2. [Plano de execução](../../spec-execution/api-mvp/README.md)
3. [Documentação viva](../../../README.md)
4. [Arquitetura do backend](../../../architecture/backend.md)
5. [Contrato da API](../../../architecture/api.md)

Para cada task, ler também os documentos de domínio e integração indicados nela.

## Ordem das tasks

| Ordem | Task | Estado | Dependências |
|---|---|---|---|
| 1 | [TASK-001 — Bootstrap e infraestrutura](task-001-bootstrap-infraestrutura.md) | approved | Nenhuma |
| 2 | [TASK-002 — Identidade, contas e planos](task-002-identidade-contas-planos.md) | approved | TASK-001 |
| 3 | [TASK-003 — Cotas, custos e administração](task-003-cotas-custos-administracao.md) | approved | TASK-002 |
| 4 | [TASK-004 — Entrada de relatórios e armazenamento](task-004-entrada-relatorios-armazenamento.md) | approved | TASK-003 |
| 5 | [TASK-005 — Processamento e Ollama](task-005-processamento-ollama.md) | approved | TASK-004 |
| 6 | [TASK-006 — Telegram](task-006-telegram.md) | approved | TASK-005 |
| 7 | [TASK-007 — Observabilidade e robustez](task-007-observabilidade-robustez.md) | approved | TASK-006 |
| 8 | [TASK-008 — Validação ponta a ponta](task-008-validacao-ponta-a-ponta.md) | approved | TASK-007 |

## Regra de avanço

Executar somente uma task ready. Não iniciar dependências enquanto a revisão da task anterior não resultar em APPROVED_NEXT_TASK.

Cada task exige TDD:

1. teste falho;
2. evidência Red;
3. implementação mínima;
4. suíte Green;
5. refatoração;
6. scripts/test.sh e scripts/validate.sh.

A TASK-001, a TASK-002, a TASK-003, a TASK-004, a TASK-005, a TASK-006, a TASK-007 e a TASK-008 foram aceitas após revisão técnica. A iniciativa API MVP está concluída com resultado OBJECTIVE_COMPLETE; não há tasks restantes nesta iniciativa.

## Resultado global esperado

- API e ambiente executáveis por scripts.
- Fluxo conta → plano/cota → relatório → processamento → resultado comprovado.
- Telegram e Ollama ativáveis por configuração.
- Segurança, concorrência, idempotência, armazenamento e auditoria testados.
- OpenAPI, Dockerfile e documentação operacional atualizados.
- Todas as oito tasks aceitas com evidências; resultado final OBJECTIVE_COMPLETE.
