# TASK-006 — Telegram de ponta a ponta

## Estado

approved

## Dependências

- TASK-005 aceita.

## Objetivo

Entregar vínculo, conversa, criação e entrega de relatórios pelo Telegram usando os mesmos casos de uso da API.

## Leitura obrigatória

- [Fonte](../../spec-source/api-mvp/README.md)
- [Execução](../../spec-execution/api-mvp/README.md)
- [Telegram](../../../integrations/telegram.md)
- [Contas](../../../domains/accounts.md)
- [Relatórios](../../../domains/reports.md)

## Escopo

- Criar migrations para vínculo, código temporário, conversa, update processado e entrega.
- Implementar geração autenticada de código de vínculo em /api/v1/me/telegram-link.
- Código imprevisível, uso único e validade de 10 minutos.
- Garantir relação um Telegram por conta e uma conta por Telegram.
- Implementar adaptador de bot e long polling condicional por configuração.
- Garantir apenas um poller por instância/configuração e documentar limite de escala.
- Deduplicar update_id antes de efeitos.
- Persistir máquina de conversa e expirar após 30 minutos.
- Permitir tipo, descrição, PDF opcional e criação pelo mesmo serviço da API, sem HTTP interno.
- Reagir a eventos de conclusão/falha.
- Entregar resumo e Markdown com tentativas imediata, +1 minuto e +5 minutos.
- Manter COMPLETED e cota consumida se todas as entregas falharem.
- Usar fake de Telegram nos testes; token real é opcional.

## Fora do escopo

- Webhook Telegram, múltiplos bots ou vários pollers coordenados.
- Frontend para vínculo.
- Alterar regras do relatório ou da cota.

## Passos verificáveis

1. Escrever testes falhos para código, cardinalidade, deduplicação, conversa, bloqueio e retries.
2. Demonstrar Red.
3. Implementar persistência e porta do Telegram.
4. Implementar fake determinístico antes do adaptador real.
5. Tornar Green e refatorar a máquina de estados.
6. Executar suíte completa sem token real.
7. Executar smoke opcional se token de desenvolvimento estiver disponível.

## Validação obrigatória

- scripts/test.sh — vínculo, conversa, update, relatório e entrega.
- scripts/validate.sh — migrations, módulos e suíte.
- Cenários: código expirado/reusado, vínculo duplicado, conta bloqueada, update repetido e reinício da conversa.
- Cenários de três falhas de entrega sem alterar relatório/cota.
- Smoke real opcional; nunca registrar token em saída ou arquivo.

## Critérios de conclusão

- [ ] Aplicação inicia sem token Telegram.
- [ ] Código cumpre prazo, uso e cardinalidade.
- [ ] update_id repetido não repete efeito.
- [ ] Conversa sobrevive reinício e expira em 30 minutos.
- [ ] Bot cria relatório sem chamar HTTP interno.
- [ ] PDF segue os mesmos limites da API.
- [ ] Eventos geram entrega e retries corretos.
- [ ] Falha de entrega não muda COMPLETED nem devolve cota.
- [ ] Evidências TDD e scripts verdes foram fornecidas.

## Contrato da resposta do agente

Informar resumo, arquivos alterados, evidência TDD, migrations, testes do fake, smoke opcional ou omissão, scripts, desvios, riscos e estado recomendado.
