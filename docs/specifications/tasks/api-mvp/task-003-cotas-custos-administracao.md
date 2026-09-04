# TASK-003 — Cotas, custos e administração

## Estado

approved

## Dependências

- TASK-002 aceita.

## Objetivo

Implementar alocações semanais, reserva concorrente, ajustes, troca de plano com reset e visibilidade segura de uso e custos.

## Leitura obrigatória

- [Fonte](../../spec-source/api-mvp/README.md)
- [Execução](../../spec-execution/api-mvp/README.md)
- [Cotas e custos](../../../domains/quotas-and-costs.md)
- [Planos](../../../domains/plans.md)
- [Contrato da API](../../../architecture/api.md)

## Escopo

- Criar migrations para alocações, lançamentos, ajustes e auditoria.
- Criar alocação semanal em America/Sao_Paulo a partir do plano.
- Expor consulta do plano, saldo, reservado, consumido, próxima renovação e histórico próprio em /api/v1/usage.
- Implementar reserva, confirmação e devolução atômicas como contratos do módulo.
- Impedir dupla reserva com concorrência real.
- Implementar renovação regular sem duplicar período.
- Implementar ajuste administrativo com motivo e histórico.
- Implementar atribuição/troca imediata de plano com encerramento da alocação anterior e saldo integral novo até segunda-feira.
- Preservar relatórios/reservas da alocação de origem.
- Modelar registro de tokens, duração, tentativas e custo estimado para consumo posterior.
- Expor custos monetários e auditoria somente para ADMIN.
- Garantir que conta BLOCKED não reserve.

## Fora do escopo

- Criação e processamento de relatório.
- Integração real com Ollama.
- Preço comercial, cobrança ou compra de cota.

## Passos verificáveis

1. Escrever testes falhos de calendário, reserva, concorrência, bloqueio, ajuste, reset e autorização.
2. Demonstrar Red antes da implementação.
3. Implementar migrations e regras mínimas.
4. Usar Testcontainers PostgreSQL nos cenários que dependem de lock e transação.
5. Tornar a suíte Green e refatorar sem expor internals.
6. Executar validação completa.

## Validação obrigatória

- scripts/test.sh — regras, API e concorrência.
- scripts/validate.sh — migrations, JPA validate e módulos.
- Teste simultâneo: uma unidade disponível, duas reservas, apenas uma aceita.
- Teste de troca de plano com consumo e reserva anteriores preservados.

## Critérios de conclusão

- [ ] Período semanal respeita America/Sao_Paulo.
- [ ] Reserva/confirmar/devolver são atômicos e idempotentes quando aplicável.
- [ ] Concorrência não produz saldo negativo.
- [ ] Renovação não duplica alocação.
- [ ] Troca de plano cria saldo integral e preserva histórico anterior.
- [ ] Ajustes e resets registram administrador, motivo e valores.
- [ ] USER não vê custo monetário; ADMIN vê.
- [ ] BLOCKED não inicia consumo.
- [ ] Evidências TDD e scripts verdes foram fornecidas.

## Contrato da resposta do agente

Informar resumo, arquivos alterados, evidência TDD, migrations, testes concorrentes, scripts, desvios, riscos e estado recomendado.
