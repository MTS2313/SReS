# Cotas e custos

## Estado

**Planejado.**

## Objetivo

Limitar o volume semanal de relatórios por conta e registrar o consumo técnico de cada processamento.

## Cota inicial

Cada conta terá, inicialmente, uma cota padrão de **10 relatórios por semana**.

A renovação ocorrerá toda segunda-feira em horário fixo. O fuso horário e o horário exato ainda precisam ser definidos.

A aplicação deve representar o período e o consumo de forma auditável, sem depender apenas de um contador difícil de reconstruir.

## Estados da cota

A capacidade semanal será observada como:

- disponível;
- reservada;
- consumida.

## Reserva e confirmação

1. Ao aceitar uma solicitação, o sistema reserva uma unidade.
2. A reserva impede que solicitações concorrentes ultrapassem o limite.
3. Quando o relatório termina com sucesso, a reserva vira consumo definitivo.
4. Quando ocorre uma falha técnica definitiva, a reserva retorna ao saldo disponível.

A operação de reserva precisa ser atômica no PostgreSQL.

## Ajustes administrativos

A role `ADMIN` poderá ajustar a cota de uma conta. O ajuste deverá ser registrado de forma que seja possível distinguir:

- cota padrão;
- acréscimo ou redução administrativa;
- reserva;
- consumo;
- devolução por falha.

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

Cada modelo terá um valor técnico configurável por milhão de tokens.

A estimativa inicial será:

`custo estimado = total de tokens × valor por milhão / 1.000.000`

Esse valor é uma estimativa interna de operação. Não representa preço cobrado do cliente e não substitui contabilidade de energia, hardware ou infraestrutura.

## Consultas previstas

A API permitirá consultar:

- cota total do período;
- unidades disponíveis, reservadas e consumidas;
- data da próxima renovação;
- histórico de consumo;
- tokens e duração por relatório;
- custo estimado por relatório e por período.
