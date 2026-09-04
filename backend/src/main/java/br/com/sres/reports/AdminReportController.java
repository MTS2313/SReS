package br.com.sres.reports;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reports")
public class AdminReportController {
    private final ReportService reports;

    public AdminReportController(ReportService reports) { this.reports = reports; }

    @GetMapping
    public ReportPageResponse list(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size,
                                   @RequestParam(defaultValue = "desc") String sort) {
        return reports.adminList(page, size, sort);
    }
}
