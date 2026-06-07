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
@Schema(description = "Request body containing the natural language search query")
public class NlSearchRequestDto {

    @NotBlank(message = "Search query must not be blank")
    @Schema(description = "Natural language search query", example = "show me computer science students younger than 21")
    private String query;
}
