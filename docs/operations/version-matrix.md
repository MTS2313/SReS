# Matriz de versões da fundação

TASK-001 fixa Java 21, Maven 3.9.11 pelo Maven Wrapper, Spring Boot 3.5.6, Spring Modulith 1.4.13 e Spring AI 1.0.2.

Spring Modulith 1.4 é compilado para a linha Spring Boot 3.5. Spring AI 1.0.2 fornece o starter oficial `spring-ai-starter-model-ollama` usado aqui. Boot 3.5 mantém a base Jakarta/Spring Framework 6.2 compatível com Java 21. As demais bibliotecas diretas também estão pinadas: MapStruct 1.6.3, PDFBox 3.0.6 e springdoc 2.8.13; versões transitivas ficam sob os BOMs do Boot, Modulith e AI.

O starter Ollama está no classpath para a futura integração, mas `SRES_OLLAMA_ENABLED` é `false` por padrão e o Compose não inicia Ollama. Telegram não possui integração nesta task e `SRES_TELEGRAM_ENABLED` também é `false` por padrão.
