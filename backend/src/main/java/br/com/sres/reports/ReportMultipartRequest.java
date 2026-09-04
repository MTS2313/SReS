package br.com.sres.reports;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

@Schema(name = "ReportMultipartRequest", description = "Dados multipart para entrada de relatório.")
public record ReportMultipartRequest(
        @Schema(description = "Tipo fixo do relatório", example = "EXECUTIVE_SUMMARY", requiredMode = Schema.RequiredMode.REQUIRED)
        String type,
        @Schema(description = "Descrição da solicitação", example = "Resumo executivo fictício", requiredMode = Schema.RequiredMode.REQUIRED)
        String description,
        @Schema(description = "PDF opcional, máximo de 10 MB e 50 páginas", format = "binary")
        MultipartFile file) {
}
