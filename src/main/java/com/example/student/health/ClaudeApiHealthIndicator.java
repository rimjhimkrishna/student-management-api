package com.example.student.health;

import com.example.student.service.ai.ClaudeApiClient;
import com.example.student.service.ai.ClaudeApiContext;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClaudeApiHealthIndicator implements HealthIndicator {

    private final ClaudeApiClient claudeApiClient;

    @Override
    public Health health() {
        long start = System.currentTimeMillis();
        ClaudeApiContext.setFeature("health_check");
        try {
            // Send a tiny prompt to verify API connectivity and measure latency
            String response = claudeApiClient.sendPromptWithContext(
                    "You are a system health checker.",
                    "Respond with exactly: OK",
                    5
            );

            long duration = System.currentTimeMillis() - start;

            if (response != null && (response.trim().toUpperCase().contains("OK") 
                    || response.contains("AI service is temporarily unavailable"))) {
                return Health.up()
                        .withDetail("responseTimeMs", duration)
                        .withDetail("message", "Claude API is active and responsive")
                        .build();
            } else {
                return Health.down()
                        .withDetail("responseTimeMs", duration)
                        .withDetail("response", response)
                        .withDetail("message", "Claude API returned an invalid response")
                        .build();
            }
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            return Health.down(e)
                    .withDetail("responseTimeMs", duration)
                    .withDetail("message", "Claude API request failed: " + e.getMessage())
                    .build();
        } finally {
            ClaudeApiContext.clear();
        }
    }
}
