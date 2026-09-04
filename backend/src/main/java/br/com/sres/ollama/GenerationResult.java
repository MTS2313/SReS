package br.com.sres.ollama;

public record GenerationResult(String markdown, String model, Integer inputTokens, Integer outputTokens, Integer totalTokens) {
    public GenerationResult {
        if (markdown == null || markdown.isBlank()) throw new IllegalArgumentException("Resposta vazia do modelo");
    }
}
