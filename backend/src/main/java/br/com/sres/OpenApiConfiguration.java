package br.com.sres;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Bean
    OpenAPI sresOpenApi() {
        var problemDetails = new ObjectSchema()
                .name("ProblemDetails")
                .description("Erro HTTP no formato RFC 9457. O correlationId identifica a requisição.")
                .addProperties("type", new StringSchema().format("uri"))
                .addProperties("title", new StringSchema())
                .addProperties("status", new IntegerSchema().format("int32"))
                .addProperties("detail", new StringSchema())
                .addProperties("instance", new StringSchema().format("uri"))
                .addProperties("correlationId", new StringSchema());
        return new OpenAPI().info(new Info()
                        .title("SReS API")
                .description("API do SReS para contas, quotas, relatórios, processamento e integrações. A autenticação usa tokens Bearer JWT emitidos pelo Keycloak; operações sob /api/v1/admin exigem a role ADMIN.")
                        .version("v1"))
                .tags(java.util.List.of(
                        new Tag().name("Account").description("Conta autenticada e vínculo Telegram."),
                        new Tag().name("Plans").description("Planos de relatórios."),
                        new Tag().name("Usage").description("Quota, reservas e histórico da conta."),
                        new Tag().name("Reports").description("Entrada, consulta e arquivos de relatórios."),
                        new Tag().name("Telegram").description("Vínculo da conta com Telegram."),
                        new Tag().name("Administration").description("Operações exclusivas para administradores.")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                                .description("Token Bearer JWT emitido pelo Keycloak configurado em SRES_KEYCLOAK_ISSUER."))
                        .addSchemas("ProblemDetails", problemDetails));
    }
}
