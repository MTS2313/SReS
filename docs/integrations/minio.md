# Integração com MinIO

## Estado

**Planejado.**

## Objetivo

Armazenar os arquivos de entrada e saída dos relatórios sem transferir conteúdo binário para o PostgreSQL.

## Estratégia inicial

O MVP usará um único bucket privado.

Os objetos serão organizados conceitualmente por conta e relatório:

`accounts/{accountId}/reports/{reportId}/`

Dentro desse prefixo poderão existir:

- PDF original de entrada;
- arquivo Markdown de saída.

Os nomes físicos exatos poderão incluir identificadores internos, mas não deverão depender do nome enviado pelo usuário.

## Responsabilidades

- MinIO mantém o conteúdo binário.
- PostgreSQL mantém metadados, referências e estado dos arquivos.
- A aplicação autoriza leitura e escrita.
- Telegram não recebe acesso direto permanente ao bucket.

## Segurança

- o bucket não será público;
- credenciais do MinIO serão segredos de infraestrutura;
- a API deve validar propriedade e autorização antes de entregar um arquivo;
- nome e caminho fornecidos pelo usuário não serão usados diretamente como chave;
- tipo, tamanho e quantidade de páginas serão validados antes do processamento.

## Limites do MVP

- um único PDF de entrada por relatório;
- PDF com até 10 MB e 50 páginas;
- um arquivo Markdown como resultado completo;
- sem versionamento funcional de arquivos;
- sem bucket individual por conta;
- sem URLs públicas permanentes.

A política de retenção e exclusão dos arquivos ainda não foi definida.
