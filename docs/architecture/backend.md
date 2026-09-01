# Arquitetura do backend

## Estado

**Planejado.** O repositório ainda não contém código ou infraestrutura.

## Direção

O backend será construído com foco em API e processamento assíncrono de relatórios. Interfaces como Telegram, aplicativo mobile ou frontend web serão consumidores do mesmo núcleo de aplicação.

A aplicação será um monólito modular com Spring Modulith. A decisão e suas consequências estão registradas no [ADR-001](../decisions/ADR-001-spring-modulith.md).

## Tecnologias decididas

| Responsabilidade | Tecnologia |
| --- | --- |
| Backend e API | Spring Boot |
| Organização modular | Spring Modulith |
| Integração com modelos | Spring AI |
| Mapeamento entre camadas | MapStruct |
| Persistência relacional | PostgreSQL com JPA |
| Evolução do schema | Flyway |
| Arquivos | MinIO compatível com S3 |
| Identidade e autenticação | Keycloak |
| Inferência local | Ollama |
| Contrato HTTP | OpenAPI |

## Organização modular

Os módulos serão orientados às responsabilidades do produto, inicialmente:

- contas;
- relatórios;
- cotas;
- custos;
- Telegram;
- Ollama;
- armazenamento;
- administração.

Cada módulo encapsulará entidades, repositórios e detalhes internos. Comunicação entre módulos ocorrerá por contratos explícitos. Um módulo não deverá manipular diretamente entidades ou repositórios internos de outro.

Spring Modulith será usado para verificar limites e dependências. Isso não implica transformar cada módulo em microsserviço nem exige eventos internos para interações simples.

## Componentes planejados

- API para contas, vinculação com Telegram, relatórios, cotas, custos e administração;
- bot do Telegram como adaptador de entrada e entrega;
- worker interno para buscar e processar trabalhos pendentes;
- integração Spring AI com o Ollama;
- PostgreSQL como fonte dos dados estruturados e do estado de processamento;
- MinIO para PDFs de entrada e arquivos Markdown de saída;
- Keycloak como autoridade de identidade e emissão de tokens.

## Persistência e migrations

O Flyway será a única fonte para mudanças estruturais no banco.

O Hibernate deverá usar `ddl-auto=validate` nos ambientes controlados, validando o mapeamento JPA sem criar ou alterar tabelas automaticamente.

## Processamento assíncrono

O envio de uma solicitação não aguardará a resposta do Ollama.

O estado do trabalho será persistido no PostgreSQL. Um worker agendado buscará trabalhos pendentes e usará bloqueio seguro no banco para evitar que duas execuções processem o mesmo relatório. Reiniciar a aplicação não deve perder trabalhos já aceitos.

Não haverá RabbitMQ ou outra fila externa no MVP.

A consulta e o bloqueio exatos serão definidos na implementação, respeitando o comportamento do PostgreSQL e a possibilidade de mais de uma instância da aplicação.

## Provisionamento de conta

Após receber o primeiro token válido de um usuário do Keycloak, a aplicação criará sob demanda o registro local correspondente, caso ele ainda não exista.

A operação precisa ser idempotente e usar o identificador estável do usuário no Keycloak. A aplicação não armazenará senhas.

## Contrato da API

A API será documentada com OpenAPI desde a primeira versão. Esse contrato servirá ao Telegram, às operações técnicas e ao futuro cliente web ou mobile.

A documentação da API não deve antecipar decisões visuais ou tecnologia do frontend.

## Separação de responsabilidades

- Keycloak mantém credenciais, login e identidade.
- A aplicação mantém os dados de negócio associados ao identificador do usuário no Keycloak.
- PostgreSQL mantém metadados e estados; não armazena o conteúdo binário dos PDFs.
- MinIO mantém arquivos e a aplicação conserva as referências necessárias.
- Ollama executa os modelos, mas não é fonte de verdade para relatórios, consumo ou custos.
- Telegram traduz interações do usuário para operações da aplicação; regras de negócio não ficam presas ao bot.

## Segurança mínima

- endpoints de usuário exigem token emitido pelo Keycloak;
- operações administrativas exigem a role `ADMIN`;
- o bot não confia em identificadores digitados como prova de posse;
- a vinculação com Telegram usa código temporário de uso único;
- arquivos são acessados por caminhos controlados pela aplicação, sem exposição pública irrestrita do bucket.

## Questões ainda abertas

- dependências exatas permitidas entre os módulos;
- política de recuperação de trabalhos interrompidos durante processamento;
- modelo Ollama associado a cada tipo de relatório;
- política de novas tentativas de entrega no Telegram;
- tecnologia da futura interface.
