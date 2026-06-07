package com.example.student.service.ai;

import com.example.student.exception.ClaudeApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClaudeApiClient {

    @Value("${claude.api.key}")
    private String apiKey;

    @Value("${claude.api.url}")
    private String apiUrl;

    @Value("${claude.api.model}")
    private String modelName;

    @Value("${claude.api.max-tokens:1500}")
    private int defaultMaxTokens;

    @Value("${claude.api.timeout-seconds:30}")
    private int timeoutSeconds;

    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private OkHttpClient httpClient;

    // Ordered list of free models to try — if one is rate-limited, next is used
    private static final List<String> FREE_MODEL_FALLBACK_CHAIN = Arrays.asList(
            "google/gemma-3-27b-it:free",
            "meta-llama/llama-3.2-3b-instruct:free",
            "meta-llama/llama-3.3-70b-instruct:free",
            "nousresearch/hermes-3-llama-3.1-405b:free",
            "nvidia/nemotron-nano-9b-v2:free"
    );

    @PostConstruct
    public void init() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .build();
        log.info("AI_INIT | Primary model: {} | Fallback chain size: {}", modelName, FREE_MODEL_FALLBACK_CHAIN.size());
    }

    @CircuitBreaker(name = "claudeApi", fallbackMethod = "fallbackResponse")
    @Retry(name = "claudeApi")
    public String sendPrompt(String systemPrompt, String userPrompt) {
        return sendPromptWithContext(systemPrompt, userPrompt, defaultMaxTokens);
    }

    @CircuitBreaker(name = "claudeApi", fallbackMethod = "fallbackResponse")
    @Retry(name = "claudeApi")
    public String sendPromptWithContext(String systemPrompt, String userPrompt, int maxTokens) {
        String feature = ClaudeApiContext.getFeature();
        long startTime = System.currentTimeMillis();

        Counter.builder("claude.api.requests.total")
                .tag("feature", feature)
                .register(meterRegistry)
                .increment();

        log.info("AI_REQUEST | feature={} | promptLength={} | maxTokens={}", feature, userPrompt.length(), maxTokens);

        // Build messages list once — reuse across model attempts
        List<Map<String, String>> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);
        }
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        messages.add(userMessage);

        // Try primary model first, then fallback chain
        List<String> modelsToTry = new ArrayList<>();
        modelsToTry.add(modelName);
        for (String m : FREE_MODEL_FALLBACK_CHAIN) {
            if (!m.equals(modelName)) modelsToTry.add(m);
        }

        Exception lastException = null;
        for (String currentModel : modelsToTry) {
            try {
                String result = callModel(currentModel, messages, maxTokens, feature, startTime);
                if (!currentModel.equals(modelName)) {
                    log.info("AI_FALLBACK_MODEL_SUCCESS | Used fallback model: {}", currentModel);
                }
                return result;
            } catch (ClaudeApiException e) {
                lastException = e;
                // Only retry next model on rate-limit (429) or not-found (404)
                if (e.getHttpStatus() == HttpStatus.TOO_MANY_REQUESTS || e.getHttpStatus() == HttpStatus.NOT_FOUND) {
                    log.warn("AI_MODEL_SKIP | model={} | status={} | Trying next model...", currentModel, e.getHttpStatus());
                } else {
                    // For auth (401) or other errors, don't try more models
                    throw e;
                }
            } catch (IOException e) {
                lastException = e;
                log.warn("AI_MODEL_SKIP | model={} | reason=IOException | Trying next model...", currentModel);
            }
        }

        // All models exhausted
        log.error("AI_ALL_MODELS_FAILED | All {} models in fallback chain failed", modelsToTry.size());
        Counter.builder("claude.api.errors.total")
                .tag("error_type", "ALL_MODELS_FAILED")
                .register(meterRegistry)
                .increment();
        throw new ClaudeApiException(
                "All AI models are rate-limited or unavailable. Please try again in a few seconds.",
                HttpStatus.SERVICE_UNAVAILABLE,
                "ALL_MODELS_FAILED"
        );
    }

    private String callModel(String model, List<Map<String, String>> messages, int maxTokens,
                             String feature, long startTime) throws IOException {
        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("model", model);
        requestBodyMap.put("max_tokens", maxTokens);
        requestBodyMap.put("messages", messages);

        String requestBodyJson = objectMapper.writeValueAsString(requestBodyMap);

        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("HTTP-Referer", "http://localhost:8080")
                .addHeader("X-Title", "Student Management API")
                .addHeader("content-type", "application/json")
                .post(RequestBody.create(requestBodyJson, MediaType.get("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            long duration = System.currentTimeMillis() - startTime;

            if (!response.isSuccessful()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                log.error("AI_ERROR | model={} | feature={} | status={} | response={}", model, feature, response.code(), responseBody);

                HttpStatus status = HttpStatus.resolve(response.code());
                if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;

                String errorCode = response.code() == 401 ? "UNAUTHORIZED"
                        : response.code() == 429 ? "RATE_LIMIT_EXCEEDED"
                        : response.code() == 404 ? "MODEL_NOT_FOUND"
                        : "API_ERROR";

                Counter.builder("claude.api.errors.total")
                        .tag("error_type", errorCode)
                        .register(meterRegistry)
                        .increment();

                throw new ClaudeApiException(
                        "OpenRouter API returned error status: " + response.code() + " " + responseBody,
                        status,
                        errorCode
                );
            }

            String responseBodyString = response.body().string();
            Map<?, ?> responseMap = objectMapper.readValue(responseBodyString, Map.class);

            List<?> choices = (List<?>) responseMap.get("choices");
            if (choices == null || choices.isEmpty()) {
                log.warn("OpenRouter API returned empty choices for model={}", model);
                return "";
            }

            Map<?, ?> choiceMap = (Map<?, ?>) choices.get(0);
            Map<?, ?> messageMap = (Map<?, ?>) choiceMap.get("message");
            String text = messageMap != null ? (String) messageMap.get("content") : "";

            Timer.builder("claude.api.response.duration")
                    .tag("feature", feature)
                    .register(meterRegistry)
                    .record(duration, TimeUnit.MILLISECONDS);

            log.info("AI_RESPONSE | model={} | feature={} | responseLength={} | durationMs={}",
                    model, feature, text != null ? text.length() : 0, duration);

            return text;
        }
    }

    // Fallback response for sendPrompt
    public String fallbackResponse(String systemPrompt, String userPrompt, Throwable t) {
        log.error("AI_FALLBACK | Method: sendPrompt | Cause: {} | Msg: {}", t.getClass().getSimpleName(), t.getMessage());
        return "AI service is temporarily unavailable. Please try again in a moment.";
    }

    // Fallback response for sendPromptWithContext
    public String fallbackResponse(String systemPrompt, String userPrompt, int maxTokens, Throwable t) {
        log.error("AI_FALLBACK | Method: sendPromptWithContext | Cause: {} | Msg: {}", t.getClass().getSimpleName(), t.getMessage());
        return "AI service is temporarily unavailable. Please try again in a moment.";
    }
}
