package br.com.sres.usage;

import java.time.Instant;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LedgerResponse", description = "Movimentação auditável da quota.")
public record LedgerResponse(UUID id, String entryType, int unitsDelta, int valueBefore,
                             int valueAfter, String actorSubject, String reason, Instant createdAt) { }
