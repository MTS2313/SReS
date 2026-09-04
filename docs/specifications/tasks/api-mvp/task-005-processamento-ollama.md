# TASK-005 — Processamento assíncrono e Ollama

## Estado

approved

## Dependências

- TASK-004 aceita.

## Objetivo

Processar relatórios persistidos de forma segura, gerar Markdown pelos agentes Ollama e atualizar cota, métricas e eventos.

## Leitura obrigatória

- [Fonte](../../spec-source/api-mvp/README.md)
- [Execução](../../spec-execution/api-mvp/README.md)
- [Relatórios](../../../domains/reports.md)
- [Ollama](../../../integrations/ollama.md)
- [Cotas e custos](../../../domains/quotas-and-costs.md)

## Escopo

- Versionar três prompts, um para cada tipo fixo.
- Definir porta de geração e adaptador Spring AI/Ollama habilitado por configuração.
- Permitir fake determinístico nos testes e inicialização sem Ollama.
- Implementar worker agendado sobre PostgreSQL com seleção/lock que impeça processamento duplo.
- Executar PENDING → PROCESSING → COMPLETED ou FAILED.
- Gerar Markdown e armazenar o resultado no MinIO.
- Persistir modelo, tokens disponíveis, duração, tentativas e custo estimado.
- Fazer uma tentativa adicional após falha técnica.
- Devolver reserva na falha definitiva e confirmar consumo no sucesso, sempre na alocação de origem.
- Recuperar PROCESSING com mais de 30 minutos para nova tentativa segura.
- Publicar eventos internos de conclusão e falha após persistência.
- Não exigir benchmark ou modelo real para a suíte determinística.

## Fora do escopo

- Escolher definitivamente o modelo de produção.
- Segundo agente revisor, ferramentas autônomas ou JSON de saída.
- Telegram e entrega por canal.

## Passos verificáveis

1. Escrever testes falhos para prompts/tipos, lock, transições, retry, recuperação, métricas e cota.
2. Demonstrar Red.
3. Implementar porta, fake e worker mínimo.
4. Testar lock e recuperação com PostgreSQL real.
5. Testar armazenamento de saída e falhas do MinIO/Ollama.
6. Tornar Green, refatorar e validar módulos.
7. Documentar como executar smoke opcional contra Ollama real sem torná-lo obrigatório.

## Validação obrigatória

- scripts/test.sh — worker, concorrência, integração e eventos.
- scripts/validate.sh — suíte completa, módulos e migrations.
- Teste com dois workers concorrentes para um relatório.
- Testes de sucesso, falha transitória, falha definitiva e processamento antigo.
- Smoke real somente se ambiente/modelo estiverem disponíveis; ausência não bloqueia.

## Critérios de conclusão

- [ ] Cada tipo usa seu prompt versionado e o mesmo modelo configurado.
- [ ] API inicia com Ollama desabilitado.
- [ ] Dois workers não processam o mesmo relatório.
- [ ] Retry total é limitado a duas tentativas.
- [ ] Sucesso confirma cota e persiste Markdown/métricas.
- [ ] Falha definitiva devolve cota e marca FAILED.
- [ ] Recuperação após 30 minutos não perde rastreabilidade.
- [ ] Eventos são publicados sem acoplar Telegram.
- [ ] Evidências TDD e scripts verdes foram fornecidas.

## Contrato da resposta do agente

Informar resumo, arquivos alterados, evidência TDD, testes de concorrência/falha, scripts, smoke real ou motivo da omissão, desvios, riscos e estado recomendado.
