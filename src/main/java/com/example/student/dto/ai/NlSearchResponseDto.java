package com.example.student.dto.ai;

import com.example.student.dto.StudentResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing natural language search results")
public class NlSearchResponseDto {

    @Schema(description = "Original natural language query", example = "computer science students younger than 21")
    private String query;

    @Schema(description = "Total number of matched students", example = "12")
    private int totalMatched;

    @Schema(description = "List of matching student details")
    private List<StudentResponseDTO> students;
}
