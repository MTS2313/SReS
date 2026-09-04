package br.com.sres.usage;

import java.util.UUID;

public record ReservationResponse(UUID id, int units, String status) { }
