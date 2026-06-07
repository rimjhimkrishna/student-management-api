package com.example.student.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing conversational chatbot answer")
public class ChatResponseDto {

    @Schema(description = "Session identifier (created or matched)", example = "abc123")
    private String sessionId;

    @Schema(description = "Original message submitted by user", example = "How many students are in Computer Science?")
    private String userMessage;

    @Schema(description = "Claude's response text", example = "There are 34 students enrolled in Computer Science. Would you like to know more details?")
    private String aiResponse;

    @Schema(description = "Timestamp when reply was generated", example = "2026-06-07T10:30:00")
    private LocalDateTime respondedAt;
}
