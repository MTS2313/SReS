package br.com.sres.ollama;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "sres.integrations.ollama.enabled", havingValue = "true")
@ConditionalOnBean(ChatModel.class)
public class OllamaReportGenerator implements ReportGenerator {
    private final ChatModel model;

    public OllamaReportGenerator(ChatModel model) { this.model = model; }

    @Override
    public GenerationResult generate(String reportType, String prompt) {
        ChatResponse response = model.call(new Prompt(prompt));
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new IllegalStateException("Resposta inválida do Ollama");
        }
        String markdown = response.getResult().getOutput().getText();
        var usage = response.getMetadata() == null ? null : response.getMetadata().getUsage();
        Integer input = usage == null ? null : usage.getPromptTokens();
        Integer output = usage == null ? null : usage.getCompletionTokens();
        Integer total = usage == null ? null : usage.getTotalTokens();
        String selectedModel = response.getMetadata() == null ? null : response.getMetadata().getModel();
        return new GenerationResult(markdown, selectedModel, input, output, total);
    }
}
