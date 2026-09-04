package br.com.sres.usage;

import java.time.Instant;
import java.util.UUID;

public record LedgerResponse(UUID id, String entryType, int unitsDelta, int valueBefore,
                             int valueAfter, String actorSubject, String reason, Instant createdAt) { }
