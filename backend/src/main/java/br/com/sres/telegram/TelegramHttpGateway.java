package br.com.sres.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "sres.integrations.telegram.enabled", havingValue = "true")
public class TelegramHttpGateway implements TelegramGateway {
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper;
    private final String api;
    private final String token;
    private long offset;

    public TelegramHttpGateway(ObjectMapper mapper, @Value("${SRES_TELEGRAM_TOKEN:}") String token,
                               @Value("${SRES_TELEGRAM_API:https://api.telegram.org}") String api) {
        this.mapper = mapper; this.token = token; this.api = api;
    }

    @Override public void sendMessage(long chatId, String message) { postJson("sendMessage", "{\"chat_id\":" + chatId + ",\"text\":" + quote(message) + "}"); }

    @Override public void sendResult(long chatId, String summary, byte[] markdown) {
        sendMessage(chatId, summary);
        String boundary = "----sres-telegram-boundary";
        String prefix = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"chat_id\"\r\n\r\n" + chatId + "\r\n"
                + "--" + boundary + "\r\nContent-Disposition: form-data; name=\"document\"; filename=\"report.md\"\r\nContent-Type: text/markdown\r\n\r\n";
        byte[] start = prefix.getBytes(StandardCharsets.UTF_8);
        byte[] end = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        byte[] body = new byte[start.length + markdown.length + end.length];
        System.arraycopy(start, 0, body, 0, start.length);
        System.arraycopy(markdown, 0, body, start.length, markdown.length);
        System.arraycopy(end, 0, body, start.length + markdown.length, end.length);
        request("sendDocument", HttpRequest.BodyPublishers.ofByteArray(body), "multipart/form-data; boundary=" + boundary);
    }

    @Override public List<TelegramUpdate> poll() {
        if (token.isBlank()) return List.of();
        try {
            JsonNode result = mapper.readTree(get("getUpdates?timeout=25&offset=" + offset)).path("result");
            List<TelegramUpdate> updates = new ArrayList<>();
            for (JsonNode node : result) {
                offset = Math.max(offset, node.path("update_id").asLong() + 1);
                JsonNode message = node.path("message");
                if (message.isMissingNode()) continue;
                JsonNode from = message.path("from");
                JsonNode chat = message.path("chat");
                JsonNode document = message.path("document");
                TelegramUpdate.Document file = document.isMissingNode() ? null : new TelegramUpdate.Document(document.path("file_id").asText(), document.path("file_name").asText("input.pdf"), document.path("mime_type").asText("application/pdf"), document.path("file_size").asLong(), null);
                updates.add(new TelegramUpdate(node.path("update_id").asLong(), from.path("id").asLong(), chat.path("id").asLong(), message.path("text").asText(null), file));
            }
            return updates;
        } catch (Exception exception) { return List.of(); }
    }

    @Override public byte[] download(TelegramUpdate.Document document) {
        try {
            JsonNode file = mapper.readTree(get("getFile?file_id=" + document.fileId())).path("result");
            HttpRequest request = HttpRequest.newBuilder(URI.create(api + "/file/bot" + token + "/" + file.path("file_path").asText())).GET().build();
            return client.send(request, HttpResponse.BodyHandlers.ofByteArray()).body();
        } catch (Exception exception) { throw new IllegalStateException("Falha ao baixar arquivo do Telegram", exception); }
    }

    private String postJson(String method, String body) { return request(method, HttpRequest.BodyPublishers.ofString(body), "application/json"); }
    private String get(String method) {
        try {
            var response = client.send(HttpRequest.newBuilder(URI.create(api + "/bot" + token + "/" + method)).GET().build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) throw new IllegalStateException("Telegram retornou HTTP " + response.statusCode());
            return response.body();
        } catch (Exception exception) { throw new IllegalStateException("Falha na comunicação com Telegram", exception); }
    }
    private String request(String method, HttpRequest.BodyPublisher body, String contentType) {
        try {
            var builder = HttpRequest.newBuilder(URI.create(api + "/bot" + token + "/" + method)).POST(body);
            if (contentType != null) builder.header("Content-Type", contentType);
            var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) throw new IllegalStateException("Telegram retornou HTTP " + response.statusCode());
            return response.body();
        } catch (Exception exception) { throw new IllegalStateException("Falha na comunicação com Telegram", exception); }
    }
    private static String quote(String value) { try { return new ObjectMapper().writeValueAsString(value); } catch (Exception exception) { throw new IllegalStateException(exception); } }
}
