package com.example.student.service.ai;

public class ClaudeApiContext {
    private static final ThreadLocal<String> FEATURE = ThreadLocal.withInitial(() -> "general");

    public static void setFeature(String feature) {
        FEATURE.set(feature);
    }

    public static String getFeature() {
        return FEATURE.get();
    }

    public static void clear() {
        FEATURE.remove();
    }
}
