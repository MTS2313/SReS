# Integração com MinIO

## Estado

**Atual — armazenamento de entrada e saída implementado; limpeza de temporários validada na TASK-007.**

## Objetivo

Armazenar os arquivos de entrada e saída dos relatórios sem transferir conteúdo binário para o PostgreSQL.

## Estratégia inicial

O MVP usará um único bucket privado.

Os objetos definitivos serão organizados conceitualmente por conta e relatório:

`accounts/{accountId}/reports/{reportId}/`

Dentro desse prefixo poderão existir:

- PDF original de entrada;
- arquivo Markdown de saída.

Os nomes físicos exatos poderão incluir identificadores internos, mas não deverão depender do nome enviado pelo usuário.

## Upload temporário

Como PostgreSQL e MinIO não compartilham uma transação, o PDF será inicialmente enviado para uma área temporária controlada pela aplicação.

Fluxo planejado:

1. receber e validar formato, tamanho e páginas;
2. armazenar o PDF em prefixo temporário;
3. reservar cota e criar o relatório em transação no PostgreSQL;
4. associar e organizar o objeto no prefixo definitivo do relatório;
5. remover o temporário caso a operação de negócio falhe.

Uma rotina de limpeza removerá objetos que permanecerem temporários por mais de 1 hora. Objetos já associados a um relatório não participam dessa limpeza.

## Responsabilidades

- MinIO mantém o conteúdo binário.
- PostgreSQL mantém metadados, referências e estado dos arquivos.
- A aplicação autoriza leitura e escrita.
- Telegram não recebe acesso direto permanente ao bucket.
- Compensações da aplicação tratam inconsistências entre banco e armazenamento.

## Segurança

- o bucket não será público;
- credenciais do MinIO serão segredos de infraestrutura;
- a API deve validar propriedade e autorização antes de entregar um arquivo;
- nome e caminho fornecidos pelo usuário não serão usados diretamente como chave;
- tipo, tamanho e quantidade de páginas serão validados antes do processamento.

## Retenção

Não haverá exclusão pelo usuário nem remoção automática de arquivos definitivos no MVP. Arquivos serão mantidos enquanto a conta estiver ativa.

Essa política deverá ser revisada antes do lançamento público.

## Limites do MVP

- um único PDF de entrada por relatório;
- PDF com até 10 MB e 50 páginas;
- um arquivo Markdown como resultado completo;
- sem versionamento funcional de arquivos;
- sem bucket individual por conta;
- sem URLs públicas permanentes.
