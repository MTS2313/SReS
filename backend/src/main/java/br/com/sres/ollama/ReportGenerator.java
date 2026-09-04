package br.com.sres.ollama;

public interface ReportGenerator {
    GenerationResult generate(String reportType, String prompt);
}
