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

## Canais de criação

Relatórios poderão ser solicitados:

- pelo bot do Telegram;
- por endpoint autenticado da API.

Os dois canais executam o mesmo caso de uso e obedecem às mesmas regras. O módulo Telegram chama o serviço da aplicação diretamente, sem HTTP interno.

## Estados

| Estado | Significado |
| --- | --- |
| `PENDING` | Aceito e aguardando o worker |
| `PROCESSING` | Selecionado e em processamento |
| `COMPLETED` | Conteúdo gerado e persistido com sucesso |
| `FAILED` | Processamento encerrado sem sucesso após as tentativas permitidas |

A entrega pelo Telegram não altera o estado do processamento. Um relatório continua `COMPLETED` mesmo quando sua notificação ou seu arquivo não puderem ser entregues pelo canal.

Cancelamento não faz parte do MVP.

## Entrada

Cada solicitação poderá conter:

- uma descrição textual obrigatória;
- um único PDF opcional;
- PDF com no máximo 10 MB e 50 páginas.

Pela API, a criação usará `multipart/form-data`. O texto do PDF será extraído com Apache PDFBox. Imagens, áudio, múltiplos PDFs e OCR não fazem parte do MVP.

Tamanho, quantidade de páginas e formato precisam ser validados antes de reservar cota e agendar processamento.

## Saída

O resultado completo será armazenado como arquivo Markdown no MinIO.

Todos os tipos, inclusive extração estruturada, produzirão Markdown. A extração usará seções, listas e tabelas, sem JSON obrigatório nesta versão.

Pelo Telegram, o usuário receberá:

- uma mensagem curta com o resumo do resultado;
- o arquivo Markdown completo.

Pela API, o usuário consultará o relatório e fará download por endpoint autenticado.

A geração de arquivos PDF de saída não faz parte desta versão.

## Fluxo principal

1. O canal identifica a conta.
2. O usuário escolhe um tipo fixo de relatório.
3. O sistema recebe e valida a descrição e o PDF opcional.
4. O arquivo de entrada é armazenado no MinIO.
5. Uma unidade de cota é reservada de forma atômica.
6. O relatório é criado em `PENDING`.
7. O worker agendado seleciona e bloqueia o trabalho no PostgreSQL.
8. O relatório muda para `PROCESSING`.
9. O worker prepara o contexto e executa o agente por meio do Spring AI.
10. Métricas e resultado são persistidos.
11. No sucesso, o relatório muda para `COMPLETED` e a reserva vira consumo.
12. Um evento de conclusão é publicado para integrações interessadas.
13. Se a origem ou o vínculo exigir entrega, o módulo Telegram processa o evento.

Na falha definitiva, o estado `FAILED`, a devolução da reserva e o evento correspondente são registrados antes da notificação ao canal.

## Persistência e recuperação

A solicitação aceita deve sobreviver ao reinício da aplicação. O PostgreSQL manterá o estado do trabalho e o worker não dependerá apenas de uma tarefa `@Async` criada em memória.

O bloqueio no banco deve impedir processamento duplicado quando houver mais de uma execução ou instância.

Um trabalho em `PROCESSING` por mais de 30 minutos voltará para `PENDING`. O limite será configurável. A recuperação contará como nova tentativa e precisa ser registrada para diagnóstico.

## Falhas e repetição

Em falha de processamento, o sistema fará uma tentativa automática adicional.

- se a segunda tentativa concluir, a cota é consumida normalmente;
- se a segunda tentativa também falhar por motivo técnico, o relatório muda para `FAILED` e a reserva é liberada;
- a falha e suas tentativas permanecem registradas;
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
