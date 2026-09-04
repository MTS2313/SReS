package br.com.sres.reports;

import java.time.Instant;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ReportResponse", description = "Relatório e seu estado de processamento.", example = "{\"id\":\"00000000-0000-0000-0000-000000000020\",\"type\":\"EXECUTIVE_SUMMARY\",\"origin\":\"API\",\"description\":\"Resumo fictício\",\"status\":\"PENDING\",\"inputAvailable\":false,\"createdAt\":\"2026-09-04T12:00:00Z\"}")
public record ReportResponse(UUID id, String type, String origin, String description, String status,
                             boolean inputAvailable, Instant createdAt) { }
