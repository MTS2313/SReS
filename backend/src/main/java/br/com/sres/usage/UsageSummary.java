package br.com.sres.usage;

import java.time.LocalDate;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UsageSummary", description = "Estado da quota da conta.", example = "{\"accountId\":\"00000000-0000-0000-0000-000000000001\",\"allocationId\":\"00000000-0000-0000-0000-000000000011\",\"planId\":\"00000000-0000-0000-0000-000000000010\",\"total\":10,\"available\":8,\"reserved\":1,\"consumed\":1,\"nextRenewal\":\"2026-09-07\"}")
public record UsageSummary(UUID accountId, UUID allocationId, UUID planId, int total,
                           int available, int reserved, int consumed, LocalDate nextRenewal) { }
