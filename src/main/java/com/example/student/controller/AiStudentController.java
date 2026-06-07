package com.example.student.controller;

import com.example.student.dto.ApiResponse;
import com.example.student.dto.ai.*;
import com.example.student.service.ai.AiStudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI Features", description = "Claude AI-powered intelligent endpoints")
public class AiStudentController {

    private final AiStudentService aiStudentService;

    @GetMapping("/students/{id}/study-plan")
    @Operation(summary = "Personalized Study Plan Generator", description = "Generates a 3-month study plan for a specific student based on their profile data.")
    public ResponseEntity<ApiResponse<StudyPlanResponseDto>> getStudyPlan(@PathVariable Long id) {
        StudyPlanResponseDto response = aiStudentService.getStudyPlan(id);
        return ResponseEntity.ok(ApiResponse.success("Study plan generated successfully", response));
    }

    @PostMapping("/students/nl-search")
    @Operation(summary = "Natural Language Student Search", description = "Filter students using natural language queries (e.g. computer science students younger than 21).")
    public ResponseEntity<ApiResponse<NlSearchResponseDto>> nlSearch(@Valid @RequestBody NlSearchRequestDto request) {
        NlSearchResponseDto response = aiStudentService.searchStudentsNaturalLanguage(request);
        String message = response.getTotalMatched() > 0 ? "Search completed successfully" : "Could not process search query. Please rephrase.";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @GetMapping("/analytics/insights")
    @Operation(summary = "Smart Analytics & Insights", description = "Fetch aggregated statistics and structured narrative analysis from Claude AI.")
    public ResponseEntity<ApiResponse<AnalyticsResponseDto>> getAnalyticsInsights() {
        AnalyticsResponseDto response = aiStudentService.getAnalyticsInsights();
        return ResponseEntity.ok(ApiResponse.success("Analytics insights generated successfully", response));
    }

    @GetMapping("/students/at-risk")
    @Operation(summary = "At-Risk Student Detection", description = "Process students in batches to evaluate risk levels, risk causes and recommended actions.")
    public ResponseEntity<ApiResponse<AtRiskResponseDto>> getAtRiskStudents() {
        AtRiskResponseDto response = aiStudentService.getAtRiskStudents();
        return ResponseEntity.ok(ApiResponse.success("At-risk assessment completed successfully", response));
    }

    @PostMapping("/chat")
    @Operation(summary = "Conversational Chatbot", description = "Ask institutional queries with real-time stats context injected into the Claude assistant.")
    public ResponseEntity<ApiResponse<ChatResponseDto>> chatbotQuery(@Valid @RequestBody ChatRequestDto request) {
        ChatResponseDto response = aiStudentService.chatbotQuery(request);
        return ResponseEntity.ok(ApiResponse.success("Chatbot response received", response));
    }

    @PostMapping("/students/email-content")
    @Operation(summary = "Bulk Personalized Email Content Generator", description = "Generate custom email content for up to 50 students in parallel.")
    public ResponseEntity<ApiResponse<EmailResponseDto>> generateEmailContent(@Valid @RequestBody EmailRequestDto request) {
        EmailResponseDto response = aiStudentService.generateEmailContent(request);
        return ResponseEntity.ok(ApiResponse.success("Personalized emails drafted successfully", response));
    }

    @GetMapping("/reports/monthly")
    @Operation(summary = "AI PDF Report Generator", description = "Generates a monthly analytical report PDF for the specified month and year.")
    public ResponseEntity<byte[]> generateMonthlyReportPdf(
            @RequestParam int month,
            @RequestParam int year) {
        byte[] pdfBytes = aiStudentService.generateMonthlyReportPdf(month, year);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"student-report-" + month + "-" + year + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
