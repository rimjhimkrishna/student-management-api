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
@Schema(description = "Response containing list of students identified as at-risk")
public class AtRiskResponseDto {

    @Schema(description = "Total number of student profiles analyzed", example = "100")
    private int totalAnalyzed;

    @Schema(description = "Number of students flagged as at-risk", example = "5")
    private int atRiskCount;

    @Schema(description = "List of flagged at-risk students")
    private List<AtRiskStudentDto> students;

    @Schema(description = "Timestamp when report was generated", example = "2026-06-07T10:00:00")
    private LocalDateTime generatedAt;
}
