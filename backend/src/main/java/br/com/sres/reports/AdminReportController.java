package br.com.sres.reports;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/admin/reports")
@Tag(name = "Administration")
@SecurityRequirement(name = "bearerAuth")
public class AdminReportController {
    private final ReportService reports;

    public AdminReportController(ReportService reports) { this.reports = reports; }

    @GetMapping
    @Operation(summary = "Listar relatórios para administração", description = "Lista relatórios de todas as contas. Requer ROLE_ADMIN.")
    public ReportPageResponse list(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size,
                                   @RequestParam(defaultValue = "desc") String sort) {
        return reports.adminList(page, size, sort);
    }
}
