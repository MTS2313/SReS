# Relatórios

## Estado

**Planejado.**

## Objetivo

Controlar a solicitação, o processamento, o resultado e o histórico dos relatórios gerados pelo SReS.

## Tipos iniciais

- **Resumo executivo:** produz uma síntese curta, priorizando pontos centrais.
- **Análise detalhada:** produz uma leitura mais profunda e organizada do material.
- **Extração estruturada:** identifica e organiza informações presentes no material.

As estruturas exatas de saída e os prompts ainda serão definidos. Os três tipos serão controlados internamente e não serão editáveis pelo usuário.

## Entrada

Cada solicitação poderá conter:

- uma descrição textual obrigatória;
- um único PDF opcional.

Imagens, áudio, múltiplos PDFs e OCR não fazem parte do MVP.

## Saída

O resultado completo será armazenado como arquivo Markdown no MinIO.

Pelo Telegram, o usuário receberá:

- uma mensagem curta com o resumo do resultado;
- o arquivo Markdown completo.

A geração de arquivos PDF de saída não faz parte desta versão.

## Fluxo principal

1. O sistema identifica a conta vinculada ao Telegram.
2. O usuário escolhe um tipo fixo de relatório.
3. O sistema recebe a descrição e, opcionalmente, o PDF.
4. O arquivo de entrada é armazenado no MinIO.
5. Uma unidade de cota é reservada de forma atômica.
6. A solicitação é registrada para processamento assíncrono.
7. O worker prepara o contexto e executa o agente correspondente por meio do Spring AI.
8. Métricas de processamento e o resultado são persistidos.
9. No sucesso, a reserva é confirmada como consumo.
10. O resumo e o arquivo completo são entregues pelo Telegram.

## Falhas e repetição

Em falha de processamento, o sistema fará uma tentativa automática adicional.

- se a segunda tentativa concluir, a cota é consumida normalmente;
- se a segunda tentativa também falhar por motivo técnico, a reserva é liberada;
- a falha e suas tentativas permanecem registradas para diagnóstico;
- não haverá repetição indefinida.

Erros causados por entrada inválida devem ser rejeitados antes da reserva sempre que possível. A classificação completa entre falha técnica e falha atribuível à entrada ainda será definida.

## Concorrência

Uma conta não pode ultrapassar sua cota por enviar solicitações simultâneas. Reserva e validação precisam ocorrer como uma única operação consistente.

O worker também deve impedir o processamento duplicado de uma mesma solicitação.

## Relações conceituais

- uma conta possui muitos relatórios;
- um relatório possui um tipo;
- um relatório pode possuir um arquivo de entrada;
- um relatório concluído possui um arquivo de saída;
- um relatório produz um registro de consumo e métricas de custo.
