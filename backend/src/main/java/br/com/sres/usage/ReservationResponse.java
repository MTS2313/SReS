package br.com.sres.usage;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ReservationResponse", description = "Reserva de unidade de quota.")
public record ReservationResponse(UUID id, int units, String status) { }
