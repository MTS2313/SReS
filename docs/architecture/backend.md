# Arquitetura do backend

## Estado

**Planejado.** O repositório ainda não contém código ou infraestrutura.

## Direção

O backend será construído com foco em API e processamento assíncrono de relatórios. Interfaces como Telegram, aplicativo mobile ou frontend web serão consumidores do mesmo núcleo de aplicação.

A aplicação será um monólito modular com Spring Modulith. A decisão e suas consequências estão registradas no [ADR-001](../decisions/ADR-001-spring-modulith.md).

## Tecnologias decididas

| Responsabilidade | Tecnologia |
| --- | --- |
| Linguagem e runtime | Java 21 |
| Build | Maven |
| Backend e API | Spring Boot |
| Organização modular | Spring Modulith |
| Integração com modelos | Spring AI |
| Extração textual de PDF | Apache PDFBox |
| Mapeamento entre camadas | MapStruct |
| Persistência relacional | PostgreSQL com JPA |
| Evolução do schema | Flyway |
| Arquivos | MinIO compatível com S3 |
| Identidade e autenticação | Keycloak |
| Inferência local | Ollama |
| Contrato HTTP | OpenAPI |
| Saúde e métricas | Spring Boot Actuator |

O projeto deverá incluir Maven Wrapper para reduzir dependência da versão de Maven instalada na máquina de execução.

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

Spring Modulith será usado para verificar limites e dependências. Isso não implica transformar cada módulo em microsserviço.

## Interações entre módulos

Operações que precisam responder imediatamente ou manter uma única transação usarão chamadas por contratos explícitos. Isso inclui:

- localizar ou provisionar a conta;
- validar e reservar cota;
- criar a solicitação de relatório;
- armazenar referências de arquivos.

Eventos do Spring Modulith serão usados apenas depois de mudanças relevantes no processamento, inicialmente:

- relatório concluído;
- relatório falho.

O módulo Telegram poderá reagir a esses eventos para entregar resultado ou informar falha. O domínio de relatórios não conhecerá detalhes do canal.

Não haverá comunicação exclusivamente por eventos nem eventos para cada operação interna.

## Componentes planejados

- API para contas, vinculação com Telegram, relatórios, cotas, custos e administração;
- bot Telegram embutido na aplicação e executado por long polling;
- worker interno para buscar e processar trabalhos pendentes;
- integração Spring AI com o Ollama;
- Apache PDFBox para extrair texto de PDFs sem OCR;
- PostgreSQL como fonte dos dados estruturados e do estado de processamento;
- MinIO para PDFs de entrada e arquivos Markdown de saída;
- Keycloak como autoridade de identidade e emissão de tokens.

## Persistência e migrations

O Flyway será a única fonte para mudanças estruturais no banco.

O Hibernate deverá usar `ddl-auto=validate` nos ambientes controlados, validando o mapeamento JPA sem criar ou alterar tabelas automaticamente.

## Processamento assíncrono

O envio de uma solicitação não aguardará a resposta do Ollama.

O estado do trabalho será persistido no PostgreSQL. Um worker agendado buscará trabalhos `PENDING` e usará bloqueio seguro no banco para impedir que duas execuções processem o mesmo relatório. Reiniciar a aplicação não deve perder trabalhos já aceitos.

Não haverá RabbitMQ ou outra fila externa no MVP.

Um relatório em `PROCESSING` há mais de 30 minutos será considerado interrompido e voltará para `PENDING`. Esse tempo será configurável. A implementação deve reduzir o risco de recuperar um trabalho que ainda esteja sendo executado legitimamente.

## Provisionamento de conta

Após receber o primeiro token válido de um usuário do Keycloak, a aplicação criará sob demanda o registro local correspondente, caso ele ainda não exista.

A operação precisa ser idempotente e usar o identificador estável do usuário no Keycloak. A aplicação não armazenará senhas.

## Contrato da API

A API seguirá as decisões registradas em [Contrato da API](api.md), incluindo:

- prefixo `/api/v1`;
- UUID como identificador externo;
- paginação em coleções;
- criação de relatório com `multipart/form-data`;
- download autenticado pela API;
- erros Problem Details conforme RFC 9457;
- contrato OpenAPI.

Telegram e HTTP usam os mesmos serviços de aplicação. O bot não faz chamadas HTTP contra a própria aplicação.

## Observabilidade

A primeira versão terá:

- logs estruturados;
- correlation ID por requisição;
- propagação do correlation ID nos fluxos assíncronos quando aplicável;
- endpoints de saúde e métricas com Spring Boot Actuator.

Os endpoints operacionais não devem ficar expostos publicamente sem controle. Prometheus, Grafana e tracing distribuído não fazem parte da infraestrutura obrigatória do MVP.

## Separação de responsabilidades

- Keycloak mantém credenciais, login e identidade.
- A aplicação mantém os dados de negócio associados ao identificador do usuário no Keycloak.
- PostgreSQL mantém metadados e estados; não armazena o conteúdo binário dos PDFs.
- MinIO mantém arquivos em bucket privado e a aplicação conserva as referências necessárias.
- Ollama executa o modelo compartilhado, mas não é fonte de verdade para relatórios, consumo ou custos.
- Telegram traduz interações do usuário para casos de uso da aplicação; regras de negócio não ficam presas ao bot.

## Segurança mínima

- endpoints de usuário exigem token emitido pelo Keycloak;
- operações administrativas exigem a role `ADMIN`;
- o bot não confia em identificadores digitados como prova de posse;
- a vinculação com Telegram usa código temporário de uso único;
- arquivos são acessados por caminhos controlados pela aplicação, sem exposição pública irrestrita do bucket.

## Questões ainda abertas

- dependências exatas permitidas entre os módulos;
- modelo Ollama inicial;
- política de retenção dos arquivos;
- tecnologia da futura interface.
