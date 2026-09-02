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

Cada `update_id` processado será registrado de forma única. Se o Telegram reenviar a mesma atualização, a aplicação não repetirá a ação nem consumirá nova cota.

## Estado da conversa

O progresso da conversa será persistido no PostgreSQL e expirará depois de 30 minutos sem interação.

A persistência deve permitir continuar depois de reinício da aplicação, incluindo etapas como:

- escolha do tipo;
- espera pela descrição;
- espera pelo PDF opcional;
- confirmação ou criação da solicitação.

Depois da expiração, uma nova interação começa um novo fluxo. A expiração da conversa não cancela um relatório já criado.

O estado da conversa não substitui o relatório. Depois que a solicitação for criada, o relatório passa a ser a fonte de verdade do processamento.

## Capacidades iniciais

- vincular o usuário do Telegram a uma conta;
- listar os três tipos disponíveis;
- receber a descrição do relatório;
- receber um PDF opcional;
- informar que a solicitação entrou na fila;
- informar falhas compreensíveis;
- entregar resumo e arquivo Markdown;
- consultar a cota semanal.

Conta bloqueada não poderá gerar novo vínculo nem solicitar relatório, mas o bloqueio não apaga o vínculo existente.

## Cardinalidade do vínculo

- uma conta aceita somente um vínculo ativo com Telegram;
- um usuário do Telegram aceita somente um vínculo ativo com conta;
- a substituição exige desvincular o vínculo atual e gerar um novo código.

## Vinculação

1. A conta ativa e autenticada gera um código.
2. O código vale por 10 minutos e aceita um único uso.
3. A pessoa envia o código ao bot.
4. A aplicação valida código, prazo, uso e disponibilidade do vínculo.
5. O identificador confirmado do Telegram é associado à conta.
6. O código é invalidado.

## Fluxo de solicitação

1. O bot verifica e registra o `update_id`.
2. O bot identifica a conta pelo vínculo confirmado.
3. A aplicação confirma que a conta está ativa.
4. A pessoa escolhe o tipo de relatório.
5. O estado da conversa é persistido a cada etapa.
6. O bot coleta a descrição.
7. O bot aceita um PDF opcional de até 10 MB e 50 páginas.
8. O mesmo caso de uso da API valida entrada e cota.
9. O bot confirma o recebimento sem aguardar o Ollama.
10. Após o processamento persistir o novo estado, o módulo recebe o evento de conclusão ou falha.
11. O sistema envia o resultado ou uma mensagem adequada.

## Entrega e falhas

A entrega terá no máximo três tentativas:

1. primeira tentativa imediatamente;
2. segunda tentativa após 1 minuto;
3. terceira tentativa após 5 minutos.

Se todas falharem:

- o relatório permanece `COMPLETED`;
- a falha de entrega é registrada separadamente;
- o conteúdo continua disponível pela API;
- não há nova execução do Ollama;
- a cota não é devolvida, pois o relatório foi produzido.

## Restrições

- mensagens e arquivos devem respeitar limites definidos pela aplicação;
- arquivos do Telegram devem ser transferidos para o MinIO antes do processamento;
- tokens do bot são segredos de infraestrutura;
- comandos recebidos não substituem autorização administrativa;
- uma falha no envio ao Telegram não deve apagar um relatório já concluído.
