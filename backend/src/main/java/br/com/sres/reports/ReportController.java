package br.com.sres.reports;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
    private final ReportService reports;

    public ReportController(ReportService reports) { this.reports = reports; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReportResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                   @RequestParam String type,
                                                   @RequestParam String description,
                                                   @RequestPart(required = false) MultipartFile file,
                                                   @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        ReportResponse response = reports.create(jwt, type, description, file, key);
        return ResponseEntity.accepted().location(URI.create("/api/v1/reports/" + response.id())).body(response);
    }

    @GetMapping
    public ReportPageResponse list(@AuthenticationPrincipal Jwt jwt,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size,
                                   @RequestParam(defaultValue = "desc") String sort) {
        return reports.list(jwt, page, size, sort);
    }

    @GetMapping("/{id}")
    public ReportResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) { return reports.get(jwt, id); }

    @GetMapping("/{id}/input")
    public ResponseEntity<byte[]> input(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        var file = reports.input(jwt, id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.contentType())).body(file.content());
    }

    @GetMapping("/{id}/output")
    public ResponseEntity<byte[]> output(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        var file = reports.output(jwt, id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.contentType())).body(file.content());
    }
}
