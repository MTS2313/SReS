# TASK-002 — Identidade, contas e planos

## Estado

approved

## Dependências

- TASK-001 aceita.

## Objetivo

Entregar autenticação Keycloak, provisionamento local, estados de conta, Plano Inicial e administração básica de contas e planos.

## Leitura obrigatória

- [Fonte](../../spec-source/api-mvp/README.md)
- [Execução](../../spec-execution/api-mvp/README.md)
- [Contas](../../../domains/accounts.md)
- [Planos](../../../domains/plans.md)
- [Contrato da API](../../../architecture/api.md)

## Escopo

- Criar migrations Flyway para contas, planos e auditoria indispensável desta fatia.
- Seed idempotente do Plano Inicial ativo, padrão e com limite semanal 10.
- Validar JWT e mapear roles USER e ADMIN do realm.
- Provisionar conta local idempotente pelo subject no primeiro acesso válido.
- Associar nova conta ao plano padrão.
- Implementar /api/v1/me.
- Implementar consulta administrativa, bloqueio e desbloqueio.
- Implementar criação, consulta, alteração, ativação/inativação e definição de plano padrão.
- Impedir nova atribuição de plano inativo; não migrar contas existentes automaticamente.
- Garantir que BLOCKED continue autenticável e consultável, deixando proibições de consumo prontas para as próximas tasks.
- Usar MapStruct nos limites em que houver mapeamento entre domínio e API.

## Fora do escopo

- Alocação de cota, troca de plano com reset e custo.
- Relatórios, arquivos, Ollama e Telegram.
- Billing ou autoatendimento para contratar plano.

## Passos verificáveis

1. Escrever testes falhos para JWT/roles, provisionamento concorrente, /me, bloqueio e regras do plano.
2. Executar scripts/test.sh e guardar evidência Red.
3. Criar migrations e implementação mínima para Green.
4. Testar PostgreSQL real com Testcontainers para unicidade do subject e plano padrão.
5. Refatorar limites dos módulos e mapeamentos.
6. Executar a suíte completa e verificação modular.

## Validação obrigatória

- scripts/test.sh — unitários, segurança HTTP e integração PostgreSQL.
- scripts/validate.sh — Flyway em banco limpo, JPA validate e Spring Modulith.
- Cenários com tokens USER, ADMIN, inválido e sem token.

## Critérios de conclusão

- [ ] Realm local autentica USER e ADMIN conforme configuração da API.
- [ ] Primeiro acesso cria exatamente uma conta mesmo com concorrência.
- [ ] /api/v1/me deriva a identidade do token.
- [ ] Nova conta recebe Plano Inicial.
- [ ] Plano padrão é único e plano inativo recusa nova atribuição.
- [ ] ADMIN bloqueia/desbloqueia; USER não acessa administração.
- [ ] BLOCKED preserva consulta e dados.
- [ ] Migrations são repetíveis em banco vazio.
- [ ] Evidências Red–Green–Refactor e scripts verdes foram fornecidas.

## Contrato da resposta do agente

Informar resumo, arquivos alterados, evidência TDD, migrations, scripts/testes, validações omitidas, desvios, riscos e estado recomendado.
