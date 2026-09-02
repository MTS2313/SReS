# Integração com Telegram

## Estado

**Planejado.**

## Papel no MVP

O Telegram será a primeira interface funcional do SReS. Ele permitirá solicitar relatórios e receber resultados.

O bot será um módulo da mesma aplicação Spring Boot. Ele chamará os casos de uso diretamente e não fará requisições HTTP contra a própria API.

## Recebimento de atualizações

O MVP usará long polling.

Essa escolha evita configurar webhook público, mas impõe uma restrição operacional: inicialmente apenas uma instância ativa deverá executar o polling. Caso a API seja escalada horizontalmente, será necessário garantir um único poller ativo ou separar essa responsabilidade.

Webhook e suporte simultâneo aos dois modos estão fora do MVP.

## Capacidades iniciais

- vincular o usuário do Telegram a uma conta;
- listar os três tipos disponíveis;
- receber a descrição do relatório;
- receber um PDF opcional;
- informar que a solicitação entrou na fila;
- informar falhas compreensíveis;
- entregar resumo e arquivo Markdown;
- consultar a cota semanal.

## Cardinalidade do vínculo

- uma conta aceita somente um vínculo ativo com Telegram;
- um usuário do Telegram aceita somente um vínculo ativo com conta;
- a substituição exige desvincular o vínculo atual e gerar um novo código.

## Vinculação

A conta autenticada gera um código temporário e o envia ao bot. O bot apresenta o código e a identidade confirmada do Telegram ao caso de uso da aplicação.

O código deve:

- ser imprevisível;
- valer por 10 minutos;
- aceitar um único uso;
- ser validado pelo backend;
- ser invalidado após uso ou expiração.

## Fluxo de solicitação

1. O bot identifica a conta pelo vínculo confirmado.
2. A pessoa escolhe o tipo de relatório.
3. O bot coleta a descrição.
4. O bot aceita um PDF opcional de até 10 MB e 50 páginas.
5. O mesmo caso de uso da API valida entrada e cota.
6. O bot confirma o recebimento sem aguardar o Ollama.
7. Após o processamento persistir o novo estado, o módulo recebe o evento de conclusão ou falha.
8. O sistema envia o resultado ou uma mensagem adequada.

## Entrega e falhas

A entrega de um relatório concluído terá no máximo três tentativas.

Se todas falharem:

- o relatório permanece `COMPLETED`;
- a falha de entrega é registrada separadamente;
- o conteúdo continua disponível pela API;
- não há nova execução do Ollama;
- a cota não é devolvida, pois o relatório foi produzido.

As regras de intervalo entre as três tentativas ainda serão definidas na implementação.

## Restrições

- mensagens e arquivos devem respeitar limites definidos pela aplicação;
- arquivos do Telegram devem ser transferidos para o MinIO antes do processamento;
- tokens do bot são segredos de infraestrutura;
- comandos recebidos não substituem autorização administrativa;
- uma falha no envio ao Telegram não deve apagar um relatório já concluído.
