package com.example.student.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Details about a student identified as at-risk")
public class AtRiskStudentDto {

    @Schema(description = "ID of the student", example = "45")
    private Long studentId;

    @Schema(description = "Full name of the student", example = "John Doe")
    private String studentName;

    @Schema(description = "Enrolled course", example = "Advanced Mathematics")
    private String course;

    @Schema(description = "Age of the student", example = "19")
    private int age;

    @Schema(description = "Evaluated risk level (LOW/MEDIUM/HIGH)", example = "HIGH")
    private String riskLevel;

    @Schema(description = "Detailed reason for risk flags", example = "Enrolled 90 days ago, age seems low for advanced course")
    private String reason;

    @Schema(description = "AI recommended action path", example = "Schedule counselor meeting")
    private String recommendedAction;
}
