# ADR-001: Adotar monólito modular com Spring Modulith

## Status

Aceita — implementada na fundação da TASK-001.

## Contexto

O SReS precisa ser pequeno o bastante para uma primeira implementação rápida, mas terá responsabilidades distintas: contas, relatórios, cotas, custos, Telegram, Ollama e armazenamento.

Uma estrutura monolítica puramente dividida em camadas globais tende a misturar regras entre essas áreas à medida que o produto cresce. Microsserviços adicionariam implantação, comunicação distribuída e observabilidade desnecessárias para o MVP.

## Decisão

O backend será uma única aplicação Spring Boot organizada como monólito modular com Spring Modulith.

Os módulos serão orientados aos domínios e integrações do produto. Cada módulo deve:

- possuir claramente suas responsabilidades;
- encapsular detalhes internos;
- expor apenas contratos necessários a outros módulos;
- evitar acesso direto aos repositórios e entidades internas de outro módulo;
- permitir validação dos limites modulares com os recursos do Spring Modulith.

Módulos iniciais esperados:

- contas;
- relatórios;
- cotas;
- custos;
- integrações com Telegram;
- integração com Ollama;
- armazenamento de arquivos;
- administração.

Cotas e custos podem permanecer próximos na implementação inicial, desde que suas responsabilidades continuem explícitas. A divisão física final deve ser validada quando o esqueleto do projeto existir.

## Alternativas consideradas

### Camadas globais

Foi rejeitada como direção principal porque favorece acoplamento transversal entre controllers, services e repositories de domínios diferentes.

### Microsserviços

Foi rejeitada porque o tamanho atual não justifica múltiplos deploys, comunicação remota, consistência distribuída e infraestrutura adicional.

## Consequências

### Positivas

- um único artefato e processo de implantação;
- limites de domínio verificáveis;
- estrutura preparada para evolução sem antecipar microsserviços;
- testes modulares e documentação de dependências com Spring Modulith.

### Custos e restrições

- os limites só terão valor se forem respeitados no código;
- eventos internos não devem ser usados sem necessidade;
- Spring Modulith não transforma automaticamente módulos em serviços independentes;
- integrações técnicas continuam sujeitas às regras do domínio que consomem.

## Regra prática

A simplicidade do MVP tem prioridade. Um módulo deve existir por responsabilidade real do produto, não apenas para aumentar a quantidade de módulos.
