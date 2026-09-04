package br.com.sres.reports;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ReportPageResponse", description = "Página de relatórios.")
public record ReportPageResponse(List<ReportResponse> content, int page, int size, long totalElements) { }
