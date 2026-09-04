package br.com.sres.reports;

import java.time.Instant;
import java.util.UUID;

public record ReportResponse(UUID id, String type, String origin, String description, String status,
                             boolean inputAvailable, Instant createdAt) { }
