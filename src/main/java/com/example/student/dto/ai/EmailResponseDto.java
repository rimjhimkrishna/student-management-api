package com.example.student.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing batch generated personalized email drafts")
public class EmailResponseDto {

    @Schema(description = "Purpose of the email", example = "Exam reminder for final semester exams next week")
    private String purpose;

    @Schema(description = "Tone of the email", example = "FRIENDLY")
    private String tone;

    @Schema(description = "Total number of emails drafted", example = "3")
    private int totalGenerated;

    @Schema(description = "List of individual email drafts")
    private List<SingleEmailDto> emails;

    @Schema(description = "Generation timestamp", example = "2026-06-07T10:00:00")
    private LocalDateTime generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Single personalized email draft")
    public static class SingleEmailDto {
        @Schema(description = "ID of the student", example = "1")
        private Long studentId;

        @Schema(description = "Name of the student", example = "Rahul Kumar")
        private String studentName;

        @Schema(description = "Email address of the student", example = "rahul@example.com")
        private String email;

        @Schema(description = "Suggested subject line", example = "Your Computer Science Exam is Coming Up!")
        private String subject;

        @Schema(description = "Suggested body content", example = "Dear Rahul, your final exams are next week...")
        private String body;
    }
}
