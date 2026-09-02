# Integração com Ollama

## Estado

**Planejado.**

## Objetivo

Usar modelos executados no Ollama para produzir os relatórios, integrados ao backend por meio do Spring AI.

## Modelo de agente do MVP

Cada tipo de relatório será tratado como um agente especializado composto por:

- um propósito fixo;
- um prompt interno próprio;
- regras de entrada e saída conhecidas.

Os três agentes usarão inicialmente o mesmo modelo Ollama configurável. A especialização será feita pelos prompts, não por modelos distintos.

Cada processamento fará uma chamada principal ao modelo. Não haverá cadeia entre analista e revisor, decisões autônomas ou orquestração dinâmica entre vários agentes.

Essa limitação reduz custo, latência e complexidade. A qualidade deverá ser validada antes de justificar modelos diferentes ou uma segunda etapa de revisão.

## Tipos previstos

- resumo executivo;
- análise detalhada;
- extração estruturada.

Todos produzirão Markdown. A extração estruturada usará seções, listas e tabelas, sem resposta JSON obrigatória no MVP.

Os prompts e formatos exatos ainda precisam ser definidos. Eles não serão configuráveis pelo usuário nesta versão.

## Prompts e configuração

Os prompts serão arquivos versionados no repositório junto da aplicação. Isso permitirá revisar mudanças e relacionar o comportamento do relatório à versão implantada.

Serão configurações externas:

- endereço do Ollama;
- modelo compartilhado;
- parâmetros técnicos permitidos;
- valor estimado por milhão de tokens.

Não haverá edição de prompt pelo banco de dados ou por endpoint administrativo no MVP.

O modelo inicial específico ainda será escolhido considerando o hardware disponível e a qualidade necessária.

## Contexto enviado

A chamada poderá combinar:

- a descrição informada pelo usuário;
- o texto extraído de um PDF opcional de até 10 MB e 50 páginas;
- as instruções internas do tipo escolhido.

OCR, interpretação de imagens e transcrição de áudio estão fora do MVP.

## Métricas

A integração deverá capturar, quando disponíveis:

- modelo;
- tokens de entrada e saída;
- duração;
- resultado da tentativa;
- mensagem técnica de erro apropriada para diagnóstico.

As métricas alimentam o domínio de custos, mas não substituem o estado do relatório no PostgreSQL.

## Falhas

Uma falha permite uma tentativa automática adicional. Depois da segunda falha técnica:

- o relatório é marcado como falho;
- a reserva de cota é devolvida;
- o erro é registrado;
- um evento de falha é publicado para integrações interessadas;
- o usuário recebe uma resposta adequada pelo canal solicitante.

O conteúdo técnico sensível da falha não deve ser exposto diretamente ao usuário.
