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
@Schema(description = "Response containing the personalized study plan for a student")
public class StudyPlanResponseDto {
    
    @Schema(description = "ID of the student", example = "1")
    private Long studentId;

    @Schema(description = "Full name of the student", example = "Rahul Kumar")
    private String studentName;

    @Schema(description = "Course name", example = "Computer Science")
    private String course;

    @Schema(description = "AI-generated 3-month study plan content", example = "Month 1: Focus on Java core syntax...")
    private String studyPlan;

    @Schema(description = "Timestamp when report was generated", example = "2026-06-07T10:30:00")
    private LocalDateTime generatedAt;

    @Schema(description = "Flag indicating if the response was served from cache", example = "false")
    private boolean cachedResponse;
}
