# Arquitetura do backend

## Estado

**Planejado.** O repositório ainda não contém código ou infraestrutura.

## Direção

O backend será construído com foco em API e processamento assíncrono de relatórios. Interfaces como Telegram, aplicativo mobile ou frontend web serão consumidores do mesmo núcleo de aplicação.

## Tecnologias decididas

| Responsabilidade | Tecnologia |
| --- | --- |
| Backend e API | Spring Boot |
| Integração com modelos | Spring AI |
| Mapeamento entre camadas | MapStruct |
| Persistência relacional | PostgreSQL com JPA |
| Arquivos | MinIO compatível com S3 |
| Identidade e autenticação | Keycloak |
| Inferência local | Ollama |

## Componentes planejados

- API para contas, vinculação com Telegram, relatórios, cotas, custos e administração;
- bot do Telegram como adaptador de entrada e entrega;
- worker interno para buscar e processar trabalhos pendentes;
- integração Spring AI com o Ollama;
- PostgreSQL como fonte dos dados estruturados e do estado de processamento;
- MinIO para PDFs de entrada e arquivos Markdown de saída;
- Keycloak como autoridade de identidade e emissão de tokens.

## Processamento assíncrono

O envio de uma solicitação não aguardará a resposta do Ollama.

O estado do trabalho será persistido no PostgreSQL. Um worker interno executará os itens pendentes e registrará sucesso ou falha. Não haverá RabbitMQ ou outra fila externa no MVP.

Essa decisão reduz infraestrutura, mas cria um limite: PostgreSQL e o mecanismo de concorrência do worker precisarão impedir que duas instâncias processem o mesmo trabalho. A estratégia exata de bloqueio ainda será definida durante a implementação.

## Separação de responsabilidades

- Keycloak mantém credenciais, login e identidade.
- A aplicação mantém os dados de negócio associados ao identificador do usuário no Keycloak.
- PostgreSQL mantém metadados e estados; não deve armazenar o conteúdo binário dos PDFs.
- MinIO mantém arquivos e a aplicação conserva apenas as referências necessárias.
- Ollama executa os modelos, mas não é fonte de verdade para relatórios, consumo ou custos.
- Telegram traduz interações do usuário para operações da API; regras de negócio não devem ficar presas ao bot.

## Segurança mínima

- endpoints de usuário exigem token emitido pelo Keycloak;
- operações administrativas exigem a role `ADMIN`;
- o bot não deve confiar apenas em identificadores informados manualmente;
- a vinculação com Telegram usa código temporário gerado pela aplicação;
- arquivos devem ser acessados por caminhos controlados pela aplicação, sem exposição pública irrestrita do bucket.

## Questões ainda abertas

- organização interna dos módulos e pacotes;
- provisionamento local da conta após a criação no Keycloak;
- estratégia de concorrência e recuperação do worker;
- limites de tamanho e de páginas do PDF;
- expiração e quantidade de usos do código de vinculação;
- tecnologia da futura interface.
