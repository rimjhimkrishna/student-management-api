package com.example.student.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body containing chat message and session identifiers")
public class ChatRequestDto {

    @NotBlank(message = "Message must not be blank")
    @Schema(description = "User's query/question", example = "How many students are in Computer Science?")
    private String message;

    @Schema(description = "Optional session identifier to track convo state", example = "optional-session-id-for-context")
    private String sessionId;
}
