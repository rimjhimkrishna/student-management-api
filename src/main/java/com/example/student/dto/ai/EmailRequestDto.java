package com.example.student.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body containing parameters for generating personalized bulk emails")
public class EmailRequestDto {

    @NotEmpty(message = "studentIds must not be empty")
    @Size(max = 50, message = "studentIds list cannot exceed 50 students")
    @Schema(description = "List of student IDs to draft emails for", example = "[1, 2, 3]")
    private List<Long> studentIds;

    @NotEmpty(message = "purpose must not be empty")
    @Schema(description = "General purpose of the email", example = "Exam reminder for final semester exams next week")
    private String purpose;

    @NotNull(message = "tone must not be null")
    @Schema(description = "Tone of the email (FRIENDLY, FORMAL, URGENT)", example = "FRIENDLY")
    private EmailTone tone;

    public enum EmailTone {
        FRIENDLY,
        FORMAL,
        URGENT
    }
}
