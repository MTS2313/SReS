# Integração com Ollama

## Estado

**Planejado.**

## Objetivo

Usar modelos executados no Ollama para produzir os relatórios, integrados ao backend por meio do Spring AI.

## Modelo de agente do MVP

Cada tipo de relatório será tratado como um agente especializado composto por:

- um propósito fixo;
- um prompt interno;
- um modelo configurado pela aplicação;
- regras de entrada e saída conhecidas.

Cada processamento fará uma chamada principal ao modelo. Não haverá cadeia entre analista e revisor, decisões autônomas ou orquestração dinâmica entre vários agentes.

Essa limitação reduz custo, latência e complexidade. A qualidade deverá ser validada antes de justificar uma segunda etapa de revisão.

## Tipos previstos

- resumo executivo;
- análise detalhada;
- extração estruturada.

Os prompts e formatos exatos ainda precisam ser definidos e versionados. Eles não serão configuráveis pelo usuário nesta versão.

## Contexto enviado

A chamada poderá combinar:

- a descrição informada pelo usuário;
- o texto extraído de um PDF opcional;
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
- o usuário recebe uma resposta adequada pelo canal solicitante.

O conteúdo técnico sensível da falha não deve ser exposto diretamente ao usuário.

## Configuração

Endereço do Ollama, modelo e parâmetros técnicos serão configurações de ambiente ou da aplicação. A estratégia para associar modelos diferentes aos tipos de relatório ainda não foi definida.
