package br.com.sres.usage;

import java.time.LocalDate;
import java.util.UUID;

public record UsageSummary(UUID accountId, UUID allocationId, UUID planId, int total,
                           int available, int reserved, int consumed, LocalDate nextRenewal) { }
