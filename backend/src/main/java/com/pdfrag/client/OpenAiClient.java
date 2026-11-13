package com.pdfrag.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pdfrag.config.AppProperties;
import com.pdfrag.exception.ApiException;

/**
 * Talks to the OpenAI REST API using plain HTTP calls (no extra SDK needed).
 * Two calls are used:
 *   POST /embeddings        -> turns text into a vector of numbers
 *   POST /chat/completions  -> asks the LLM to write an answer
 */
@Component
public class OpenAiClient {

    private static final int EMBEDDING_BATCH_SIZE = 64;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AppProperties props;
    private final RestClient http;

    public OpenAiClient(AppProperties props) {
        this.props = props;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(120_000);
        this.http = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(props.openai().baseUrl())
                .build();
    }

    public boolean isConfigured() {
        String key = props.openai().apiKey();
        return key != null && !key.isBlank() && !key.startsWith("sk-your");
    }

    /** Converts each text into an embedding vector (same order as the input). */
    public List<double[]> embed(List<String> texts) {
        List<double[]> result = new ArrayList<>();
        for (int start = 0; start < texts.size(); start += EMBEDDING_BATCH_SIZE) {
            List<String> batch = texts.subList(start, Math.min(start + EMBEDDING_BATCH_SIZE, texts.size()));
            Map<String, Object> body = Map.of(
                    "model", props.openai().embeddingModel(),
                    "input", batch);

            JsonNode response = post("/embeddings", body);

            double[][] ordered = new double[batch.size()][];
            for (JsonNode item : response.path("data")) {
                int index = item.path("index").asInt();
                JsonNode values = item.path("embedding");
                double[] vector = new double[values.size()];
                for (int i = 0; i < vector.length; i++) {
                    vector[i] = values.get(i).asDouble();
                }
                if (index >= 0 && index < ordered.length) {
                    ordered[index] = vector;
                }
            }
            for (double[] vector : ordered) {
                if (vector == null) {
                    throw new ApiException(HttpStatus.BAD_GATEWAY, "OpenAI returned an incomplete embedding response.");
                }
            }
            result.addAll(Arrays.asList(ordered));
        }
        return result;
    }

    /** Sends chat messages (role + content) to the LLM and returns the answer text. */
    public String chat(List<Map<String, String>> messages) {
        Map<String, Object> body = Map.of(
                "model", props.openai().chatModel(),
                "messages", messages,
                "temperature", 0.2);

        JsonNode response = post("/chat/completions", body);
        String answer = response.path("choices").path(0).path("message").path("content").asText("");
        if (answer.isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "OpenAI returned an empty answer.");
        }
        return answer.trim();
    }

    private JsonNode post(String path, Object body) {
        if (!isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "OpenAI API key is not set. Add OPENAI_API_KEY to the .env file and restart the app.");
        }
        try {
            JsonNode node = http.post()
                    .uri(path)
                    .header("Authorization", "Bearer " + props.openai().apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (node == null) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "OpenAI returned an empty response.");
            }
            return node;
        } catch (RestClientResponseException e) {
            int code = e.getStatusCode().value();
            String detail = extractMessage(e.getResponseBodyAsString());
            if (code == 401) {
                detail = "The OpenAI API key is invalid. " + detail;
            } else if (code == 429) {
                detail = "OpenAI rate limit or quota reached. " + detail;
            }
            throw new ApiException(HttpStatus.BAD_GATEWAY, "OpenAI error (" + code + "): " + detail);
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Could not reach OpenAI: " + e.getMessage());
        }
    }

    private static String extractMessage(String responseBody) {
        try {
            String message = MAPPER.readTree(responseBody).path("error").path("message").asText("");
            if (!message.isBlank()) {
                return message;
            }
        } catch (Exception ignored) {
            // fall through to the raw body
        }
        if (responseBody == null) {
            return "";
        }
        return responseBody.length() > 300 ? responseBody.substring(0, 300) : responseBody;
    }
}
