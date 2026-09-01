# Integração com Telegram

## Estado

**Planejado.**

## Papel no MVP

O Telegram será a primeira interface funcional do SReS. Ele permitirá solicitar relatórios e receber resultados.

As regras de negócio continuarão na aplicação. O bot será um adaptador da API, preservando a possibilidade de adicionar um aplicativo mobile ou frontend web posteriormente.

## Capacidades iniciais

- vincular o usuário do Telegram a uma conta;
- listar os três tipos disponíveis;
- receber a descrição do relatório;
- receber um PDF opcional;
- informar que a solicitação entrou na fila;
- informar falhas compreensíveis;
- entregar resumo e arquivo Markdown;
- consultar a cota semanal.

## Vinculação

A conta autenticada gera um código temporário e o envia ao bot. O bot apresenta o código e a identidade confirmada do Telegram à API.

O código deve:

- ser imprevisível;
- expirar;
- ser validado pelo backend;
- não poder ser reutilizado depois de consumido.

O prazo de expiração ainda não foi decidido.

## Fluxo de solicitação

1. O bot identifica a conta pelo vínculo confirmado.
2. A pessoa escolhe o tipo de relatório.
3. O bot coleta a descrição.
4. O bot aceita um PDF opcional.
5. A API valida entrada e cota.
6. O bot confirma o recebimento sem aguardar o Ollama.
7. Após o processamento, o sistema envia o resultado.

## Restrições

- mensagens e arquivos devem respeitar limites definidos pela API;
- arquivos do Telegram devem ser transferidos para o MinIO antes do processamento;
- tokens do bot são segredos de infraestrutura;
- comandos recebidos não substituem autorização administrativa;
- uma falha no envio ao Telegram não deve apagar um relatório já concluído.

A política de novas tentativas para entrega no Telegram ainda será definida.
