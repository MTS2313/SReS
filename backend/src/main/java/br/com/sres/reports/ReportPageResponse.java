package br.com.sres.reports;

import java.util.List;

public record ReportPageResponse(List<ReportResponse> content, int page, int size, long totalElements) { }
