package com.example.student.service.ai;

import com.example.student.exception.ClaudeApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClaudeApiClientTest {

    private ClaudeApiClient claudeApiClient;

    @Mock
    private OkHttpClient httpClient;

    @Mock
    private Call call;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @BeforeEach
    public void setUp() {
        claudeApiClient = new ClaudeApiClient(objectMapper, meterRegistry);
        ReflectionTestUtils.setField(claudeApiClient, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(claudeApiClient, "apiUrl", "http://localhost");
        ReflectionTestUtils.setField(claudeApiClient, "modelName", "claude-sonnet-4-20250514");
        ReflectionTestUtils.setField(claudeApiClient, "defaultMaxTokens", 1500);
        ReflectionTestUtils.setField(claudeApiClient, "httpClient", httpClient);
    }

    @Test
    public void testSendPrompt_Success_ReturnsResponse() throws IOException {
        // Arrange
        String mockResponseBody = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Mocked AI response\"}}]}";
        Response response = createMockResponse(200, mockResponseBody);
        when(httpClient.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);

        // Act
        String result = claudeApiClient.sendPrompt("System prompt", "User prompt");

        // Assert
        assertEquals("Mocked AI response", result);
        verify(httpClient, times(1)).newCall(any());
    }

    @Test
    public void testSendPrompt_InvalidApiKey_ThrowsClaudeApiException() throws IOException {
        // Arrange
        String mockResponseBody = "{\"error\":{\"type\":\"authentication_error\",\"message\":\"invalid api key\"}}";
        Response response = createMockResponse(401, mockResponseBody);
        when(httpClient.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);

        // Act & Assert
        ClaudeApiException exception = assertThrows(ClaudeApiException.class, () -> {
            claudeApiClient.sendPrompt("System prompt", "User prompt");
        });

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getHttpStatus());
        assertEquals("UNAUTHORIZED", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("401"));
    }

    @Test
    public void testSendPrompt_RateLimitExceeded_ThrowsException() throws IOException {
        // Arrange
        String mockResponseBody = "{\"error\":{\"type\":\"rate_limit_error\",\"message\":\"rate limit exceeded\"}}";
        Response response = createMockResponse(429, mockResponseBody);
        when(httpClient.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);

        // Act & Assert
        ClaudeApiException exception = assertThrows(ClaudeApiException.class, () -> {
            claudeApiClient.sendPrompt("System prompt", "User prompt");
        });

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getHttpStatus());
        assertEquals("RATE_LIMIT_EXCEEDED", exception.getErrorCode());
    }

    @Test
    public void testSendPrompt_NetworkTimeout_ThrowsException() throws IOException {
        // Arrange
        when(httpClient.newCall(any())).thenReturn(call);
        when(call.execute()).thenThrow(new IOException("Timeout connecting to server"));

        // Act & Assert
        ClaudeApiException exception = assertThrows(ClaudeApiException.class, () -> {
            claudeApiClient.sendPrompt("System prompt", "User prompt");
        });

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getHttpStatus());
        assertEquals("NETWORK_ERROR", exception.getErrorCode());
    }

    @Test
    public void testSendPrompt_EmptyResponse_HandledGracefully() throws IOException {
        // Arrange
        String mockResponseBody = "{\"choices\":[]}";
        Response response = createMockResponse(200, mockResponseBody);
        when(httpClient.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);

        // Act
        String result = claudeApiClient.sendPrompt("System prompt", "User prompt");

        // Assert
        assertEquals("", result);
    }

    @Test
    public void testFallbackResponse_ReturnsUserFriendlyMessage() {
        // Arrange & Act
        String fallback = claudeApiClient.fallbackResponse("System prompt", "User prompt", new RuntimeException("API error"));

        // Assert
        assertEquals("AI service is temporarily unavailable. Please try again in a moment.", fallback);
    }

    private Response createMockResponse(int code, String bodyContent) {
        Request request = new Request.Builder().url("http://localhost").build();
        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("Mock response status")
                .body(ResponseBody.create(bodyContent, MediaType.get("application/json")))
                .build();
    }
}
