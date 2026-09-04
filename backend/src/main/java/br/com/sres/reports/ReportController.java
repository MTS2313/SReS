package br.com.sres.reports;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {
    private final ReportService reports;

    public ReportController(ReportService reports) { this.reports = reports; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Criar relatório", description = "Aceita texto e PDF opcional. O processamento é assíncrono e a solicitação aceita retorna PENDING. A mesma Idempotency-Key na conta referencia o relatório já criado.", requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE, schema = @Schema(implementation = ReportMultipartRequest.class))))
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Relatório aceito para processamento"),
            @ApiResponse(responseCode = "400", description = "Requisição ou PDF inválido", content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = "#/components/schemas/ProblemDetails"))),
            @ApiResponse(responseCode = "401", description = "Autenticação necessária", content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = "#/components/schemas/ProblemDetails"))),
            @ApiResponse(responseCode = "409", description = "Conta bloqueada ou quota indisponível", content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = "#/components/schemas/ProblemDetails")))
    })
    public ResponseEntity<ReportResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                   @Parameter(hidden = true) @RequestParam String type,
                                                   @Parameter(hidden = true) @RequestParam String description,
                                                   @Parameter(description = "PDF opcional, máximo de 10 MB e 50 páginas") @RequestPart(required = false) MultipartFile file,
                                                   @Parameter(description = "Chave opcional de idempotência por conta", example = "report-key-2026-001") @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        ReportResponse response = reports.create(jwt, type, description, file, key);
        return ResponseEntity.accepted().location(URI.create("/api/v1/reports/" + response.id())).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar meus relatórios", description = "Lista relatórios da conta autenticada com paginação por page, size e sort.")
    @ApiResponse(responseCode = "200", description = "Relatórios da conta autenticada")
    public ReportPageResponse list(@AuthenticationPrincipal Jwt jwt,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size,
                                   @RequestParam(defaultValue = "desc") String sort) {
        return reports.list(jwt, page, size, sort);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar relatório", description = "Consulta um relatório próprio pelo UUID.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Relatório encontrado"), @ApiResponse(responseCode = "404", description = "Relatório não encontrado", content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = "#/components/schemas/ProblemDetails")))})
    public ReportResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) { return reports.get(jwt, id); }

    @GetMapping("/{id}/input")
    @Operation(summary = "Baixar PDF de entrada", description = "Baixa o PDF de entrada do relatório próprio quando disponível.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "PDF de entrada", content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE, schema = @Schema(type = "string", format = "binary"))), @ApiResponse(responseCode = "404", description = "Arquivo não encontrado", content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = "#/components/schemas/ProblemDetails")))})
    public ResponseEntity<byte[]> input(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        var file = reports.input(jwt, id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.contentType())).body(file.content());
    }

    @GetMapping("/{id}/output")
    @Operation(summary = "Baixar resultado Markdown", description = "Baixa o resultado Markdown do relatório próprio quando o processamento estiver concluído.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Resultado Markdown", content = @Content(mediaType = "text/markdown", schema = @Schema(type = "string", format = "binary"))), @ApiResponse(responseCode = "404", description = "Resultado não encontrado", content = @Content(mediaType = "application/problem+json", schema = @Schema(ref = "#/components/schemas/ProblemDetails")))})
    public ResponseEntity<byte[]> output(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        var file = reports.output(jwt, id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.contentType())).body(file.content());
    }
}
