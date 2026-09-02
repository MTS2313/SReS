# TASK-007 — Observabilidade, segurança e robustez operacional

## Estado

planned

## Dependências

- TASK-006 aceita.

## Objetivo

Fechar contratos transversais, segurança negativa, limpeza e diagnóstico para que o MVP seja operável sem expor dados internos.

## Leitura obrigatória

- [Fonte](../../spec-source/api-mvp/README.md)
- [Execução](../../spec-execution/api-mvp/README.md)
- [Arquitetura](../../../architecture/backend.md)
- [Contrato da API](../../../architecture/api.md)
- Todos os documentos de domínio e integração afetados por testes transversais.

## Escopo

- Padronizar Problem Details RFC 9457 para validação, autenticação, autorização, bloqueio, cota, conflito e recurso inexistente.
- Implementar correlation ID em HTTP e propagação aplicável ao processamento/eventos.
- Estruturar logs sem segredos, tokens, conteúdo integral ou stack trace para cliente.
- Configurar Actuator com exposição mínima e proteção apropriada.
- Consolidar OpenAPI, autenticação e schemas/estados.
- Completar endpoints administrativos de relatórios, custos, métricas e auditoria.
- Implementar/validar limpezas: temporários MinIO após 1 hora, idempotência após 24 horas e conversas Telegram após 30 minutos.
- Reforçar jobs contra execução concorrente indevida.
- Adicionar testes negativos de propriedade, role e conta bloqueada em todas as superfícies.
- Adicionar regressões de concorrência e falhas entre módulos.
- Atualizar documentação viva quando o comportamento estiver comprovado como current, sem apagar decisões planejadas ainda não implementadas.

## Fora do escopo

- Prometheus, Grafana, tracing distribuído ou SIEM.
- Nova funcionalidade de produto.
- Política definitiva de retenção de arquivos.
- CI/CD e infraestrutura de produção.

## Passos verificáveis

1. Escrever testes falhos para cada contrato transversal ausente.
2. Demonstrar Red.
3. Implementar tratamento mínimo e configuração segura.
4. Executar matriz USER/ADMIN/BLOCKED/propriedade.
5. Validar jobs de limpeza com relógio controlado e concorrência.
6. Tornar Green, refatorar e revisar logs/OpenAPI.
7. Executar todos os scripts.

## Validação obrigatória

- scripts/test.sh — matriz de segurança, erros, jobs e regressões.
- scripts/validate.sh — suíte global, módulos, migrations e OpenAPI aplicável.
- Inspeção de logs — sem credenciais, tokens ou conteúdo sensível.
- Inspeção Actuator — somente endpoints necessários e protegidos.
- Verificação dos links e estados atualizados em docs.

## Critérios de conclusão

- [ ] Erros possuem formato consistente e correlation ID.
- [ ] Autorização negativa cobre recursos alheios, roles e BLOCKED.
- [ ] Custos monetários permanecem exclusivos de ADMIN.
- [ ] Logs não vazam segredos ou conteúdo.
- [ ] Actuator está mínimo e protegido.
- [ ] OpenAPI reflete os endpoints e estados reais.
- [ ] Limpezas respeitam 1h, 24h e 30min sem apagar registros ativos.
- [ ] Jobs concorrentes não duplicam efeitos.
- [ ] Evidências TDD e scripts verdes foram fornecidas.

## Contrato da resposta do agente

Informar resumo, arquivos alterados, evidência TDD, matriz de segurança, inspeções, scripts, documentos atualizados, desvios, riscos e estado recomendado.
