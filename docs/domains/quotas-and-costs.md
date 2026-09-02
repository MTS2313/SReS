# Cotas e custos

## Estado

**Planejado.**

## Objetivo

Limitar o volume semanal de relatórios por conta e registrar o consumo técnico de cada processamento.

## Origem da cota

A cota base vem do plano de relatórios associado à conta. O Plano Inicial permitirá **10 relatórios por semana**.

A cota efetiva poderá incluir ajustes administrativos auditáveis além do limite definido pelo plano.

O período regular começa toda segunda-feira às 00:00 e termina no início da segunda-feira seguinte, usando o fuso `America/Sao_Paulo`.

A aplicação deve representar período, origem e consumo de forma auditável. Instantes persistidos devem continuar inequívocos mesmo com a regra de negócio expressa no fuso escolhido.

## Estados da cota

A capacidade semanal será observada como:

- disponível;
- reservada;
- consumida.

## Reserva e confirmação

1. Ao aceitar uma solicitação de uma conta ativa, o sistema reserva uma unidade.
2. A reserva impede que solicitações concorrentes ultrapassem o limite.
3. Quando o relatório termina com sucesso, a reserva vira consumo definitivo.
4. Quando ocorre uma falha técnica definitiva, a reserva retorna ao saldo disponível.
5. Uma solicitação duplicada reconhecida não cria outra reserva.

A operação de reserva precisa ser atômica no PostgreSQL.

Uma reserva pertence à alocação em que foi criada, mesmo se o processamento atravessar uma renovação ou troca de plano.

## Reset por troca de plano

Quando um administrador troca o plano:

- a mudança é imediata;
- a alocação anterior é encerrada para novos consumos;
- uma nova alocação é criada com o limite completo do novo plano;
- o novo saldo vale até a próxima segunda-feira às 00:00;
- consumo e reservas anteriores permanecem no histórico;
- consumo anterior não reduz a nova cota;
- relatórios em processamento continuam vinculados à alocação anterior;
- falhas ou conclusões posteriores desses relatórios atualizam a alocação de origem, sem alterar o novo saldo.

A operação deve ser atômica no PostgreSQL e impedir que uma solicitação seja reservada durante a transição com regras parcialmente aplicadas.

## Ajustes administrativos

A role `ADMIN` poderá ajustar a cota de uma conta.

Todo ajuste ou reset por troca de plano exigirá um motivo e registrará:

- administrador responsável;
- conta afetada;
- plano anterior e novo plano, quando aplicável;
- valor anterior;
- valor novo;
- diferença aplicada;
- motivo;
- data e hora.

O histórico também deverá permitir distinguir:

- cota originada do plano;
- reset por troca de plano;
- acréscimo ou redução administrativa;
- reserva;
- consumo;
- devolução por falha.

Alterar a cota ou o plano não apaga lançamentos anteriores.

Pagamentos, assinaturas, faturas e compra automática de cota estão fora do MVP.

## Métricas por processamento

Quando fornecidas pela integração com o modelo, serão registradas:

- modelo utilizado;
- tokens de entrada;
- tokens de saída;
- total de tokens;
- duração do processamento;
- número de tentativas;
- custo monetário estimado.

A ausência de alguma métrica do provedor não deve impedir a conclusão do relatório; o dado deve permanecer identificado como indisponível.

## Estimativa monetária

O modelo configurado terá um valor técnico por milhão de tokens.

A estimativa inicial será:

`custo estimado = total de tokens × valor por milhão / 1.000.000`

Esse valor é uma estimativa interna de operação. Não representa preço cobrado do cliente e não substitui contabilidade de energia, hardware ou infraestrutura.

## Visibilidade

Usuários poderão consultar:

- plano atual;
- cota total da alocação atual;
- unidades disponíveis, reservadas e consumidas;
- data da próxima renovação;
- histórico próprio de consumo.

Somente administradores poderão consultar:

- tokens e duração por relatório;
- custo monetário estimado por relatório e por período;
- histórico auditável de ajustes e resets;
- métricas operacionais agregadas.

O custo técnico não será apresentado ao usuário como preço.
