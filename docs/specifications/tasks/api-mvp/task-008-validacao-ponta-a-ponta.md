# TASK-008 — Validação e entrega ponta a ponta

## Estado

planned

## Dependências

- TASK-007 aceita.

## Objetivo

Comprovar o MVP completo em ambiente reproduzível, empacotar a API e entregar documentação suficiente para execução local e futura integração de frontend.

## Leitura obrigatória

- [Fonte](../../spec-source/api-mvp/README.md)
- [Execução](../../spec-execution/api-mvp/README.md)
- [Entrada das tasks](README.md)
- Toda a documentação viva em docs relacionada ao MVP.

## Escopo

- Escrever primeiro testes ponta a ponta falhos para lacunas do fluxo global; não reabrir silenciosamente tasks anteriores.
- Validar conta USER e ADMIN no realm local.
- Validar provisionamento, plano, cota, relatório sem PDF e com PDF, worker simulado, resultado e download.
- Validar bloqueio, idempotência, concorrência, falha definitiva e troca de plano.
- Validar fluxo Telegram completo com fake, incluindo vínculo, conversa e entrega.
- Criar `backend/Dockerfile` multi-stage ou equivalente enxuto e seguro para a API, com contexto de build documentado para o monorepo.
- Garantir configuração externa e imagem sem segredos.
- Consolidar scripts/validate.sh como entrada global.
- Criar documentação operacional: pré-requisitos, env, dev-up/down, teste, validação, reset, execução da API, Ollama e Telegram opcionais.
- Documentar benchmark necessário antes de fixar modelo produtivo.
- Atualizar docs vivos de Planned para Current somente onde a implementação e evidências sustentarem.
- Verificar todos os links das specifications e documentação.

## Fora do escopo

- Corrigir silenciosamente critério não satisfeito de task anterior; nesse caso, reportar bloqueio/correção.
- Frontend, CI/CD, deploy ou observabilidade externa.
- Ativar credenciais produtivas ou escolher modelo sem benchmark real.

## Passos verificáveis

1. Escrever cenário ponta a ponta que falha antes dos ajustes finais autorizados.
2. Registrar Red e identificar se a lacuna pertence a esta task ou exige correção anterior.
3. Implementar apenas empacotamento, documentação e integração final autorizados.
4. Tornar o cenário Green e refatorar harness.
5. Construir e inspecionar a imagem.
6. Executar scripts/dev-up.sh, test.sh e validate.sh.
7. Parar o ambiente sem apagar volumes.
8. Produzir matriz final dos critérios AC-001 a AC-013.

## Validação obrigatória

- scripts/dev-up.sh — ambiente local saudável.
- scripts/test.sh — toda a suíte verde.
- scripts/validate.sh — validação global e ponta a ponta.
- Build de `backend/Dockerfile` pelo procedimento e contexto documentados.
- Execução da imagem com PostgreSQL, MinIO e Keycloak locais e externos simulados.
- scripts/dev-down.sh — parada segura.
- reset-dev.sh somente em alvo descartável e com confirmação explícita, se executado.

## Critérios de conclusão

- [ ] Fluxo API ponta a ponta passa com e sem PDF.
- [ ] Fluxo Telegram simulado passa.
- [ ] Casos negativos e concorrentes globais passam.
- [ ] Imagem construída a partir de `backend/Dockerfile` inicia sem segredos embutidos.
- [ ] OpenAPI permite integrar futuro cliente sem ler código.
- [ ] Scripts são a única interface operacional mutável documentada.
- [ ] Documentação distingue Current, Planned e limitações pré-lançamento.
- [ ] Todos AC-001 a AC-013 possuem evidência ou a iniciativa permanece bloqueada.
- [ ] Evidências finais Red–Green–Refactor e scripts verdes foram fornecidas.

## Contrato da resposta do agente

Informar resumo, arquivos alterados, evidência TDD, matriz AC-001–AC-013, scripts/testes, imagem gerada, validações omitidas, desvios, riscos e recomendação OBJECTIVE_COMPLETE ou bloqueio.
