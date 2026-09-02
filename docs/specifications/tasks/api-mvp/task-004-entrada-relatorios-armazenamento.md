# TASK-004 — Entrada de relatórios e armazenamento

## Estado

planned

## Dependências

- TASK-003 aceita.

## Objetivo

Permitir que uma conta crie e consulte relatórios pela API com PDF opcional, cota reservada, idempotência e arquivos privados no MinIO.

## Leitura obrigatória

- [Fonte](../../spec-source/api-mvp/README.md)
- [Execução](../../spec-execution/api-mvp/README.md)
- [Relatórios](../../../domains/reports.md)
- [MinIO](../../../integrations/minio.md)
- [Contrato da API](../../../architecture/api.md)

## Escopo

- Criar migrations de relatório, origem, tipo, estado, arquivo, tentativas e idempotência.
- Implementar tipos fixos EXECUTIVE_SUMMARY, DETAILED_ANALYSIS e STRUCTURED_EXTRACTION.
- Implementar adaptador MinIO privado e metadados no PostgreSQL.
- Validar MIME/assinatura aplicável, tamanho máximo 10 MB e até 50 páginas.
- Extrair texto com PDFBox sem OCR.
- Fazer upload temporário, transação de cota+relatório, associação final e compensação.
- Suportar Idempotency-Key por conta por 24 horas.
- Criar endpoint multipart que responde 202, estado PENDING e Location.
- Listar com page/size/sort, consultar por ID e impedir acesso cruzado.
- Transmitir entrada/resultado autorizado pela API sem URL pública; resultado ainda pode estar ausente.
- Registrar origem API.
- Preparar contratos reutilizáveis pelo Telegram.

## Fora do escopo

- Worker, chamada Ollama e conteúdo Markdown de saída.
- Bot Telegram.
- OCR, múltiplos arquivos ou exclusão.

## Passos verificáveis

1. Escrever testes falhos para validação PDF, idempotência, autorização, 202, concorrência e compensação.
2. Registrar Red.
3. Implementar schema e portas de armazenamento.
4. Usar MinIO controlado e PostgreSQL real nos testes de integração necessários.
5. Tornar Green e refatorar o fluxo sem transação distribuída fictícia.
6. Executar validações completas.

## Validação obrigatória

- scripts/test.sh — domínio, HTTP, segurança, PostgreSQL e MinIO.
- scripts/validate.sh — migrations, módulos e contrato.
- Cenários: PDF inválido/grande/51 páginas, sem PDF, quota esgotada, chave repetida e usuário alheio.
- Cenário de falha entre objeto temporário e transação sem órfão definitivo ou dupla cota.

## Critérios de conclusão

- [ ] API aceita texto e um PDF válido opcional.
- [ ] Rejeições acontecem antes da reserva quando possível.
- [ ] Resposta aceita é 202 com UUID, PENDING e Location.
- [ ] Idempotency-Key repetida não duplica relatório/cota.
- [ ] Propriedade e conta bloqueada são respeitadas.
- [ ] MinIO permanece privado e banco guarda apenas referência/metadados.
- [ ] Compensação cobre falhas demonstradas por teste.
- [ ] Paginação e download autenticado funcionam.
- [ ] Evidências TDD e scripts verdes foram fornecidas.

## Contrato da resposta do agente

Informar resumo, arquivos alterados, evidência TDD, migrations, cenários MinIO/PDF, scripts, desvios, riscos e estado recomendado.
