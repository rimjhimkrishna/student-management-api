package com.example.student.controller;

import com.example.student.dto.ai.ChatRequestDto;
import com.example.student.dto.ai.EmailRequestDto;
import com.example.student.dto.ai.NlSearchRequestDto;
import com.example.student.model.Student;
import com.example.student.repository.StudentRepository;
import com.example.student.service.ai.ClaudeApiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AiStudentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClaudeApiClient claudeApiClient;

    private Student savedStudent;

    @BeforeEach
    public void setup() {
        studentRepository.deleteAll();

        Student student = Student.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .course("Computer Science")
                .age(20)
                .phone("1234567890")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        savedStudent = studentRepository.save(student);
    }

    @Test
    @WithMockUser(username = "admin")
    public void testStudyPlanEndpoint_AuthenticatedUser_Returns200() throws Exception {
        when(claudeApiClient.sendPrompt(any(), any())).thenReturn("Personalized Study Plan Details");

        mockMvc.perform(get("/api/v1/ai/students/" + savedStudent.getId() + "/study-plan")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.studentName", is("John Doe")))
                .andExpect(jsonPath("$.data.studyPlan", is("Personalized Study Plan Details")));
    }

    @Test
    public void testStudyPlanEndpoint_UnauthenticatedUser_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/ai/students/" + savedStudent.getId() + "/study-plan")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin")
    public void testStudyPlanEndpoint_StudentNotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/ai/students/99999/study-plan")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.message", containsString("Student not found")));
    }

    @Test
    @WithMockUser(username = "admin")
    public void testNlSearch_ValidQuery_Returns200WithStudents() throws Exception {
        String mockResponse = "[{\"id\":" + savedStudent.getId() + ",\"firstName\":\"John\",\"lastName\":\"Doe\",\"email\":\"john.doe@example.com\",\"course\":\"Computer Science\",\"age\":20}]";
        when(claudeApiClient.sendPrompt(any(), any())).thenReturn(mockResponse);

        NlSearchRequestDto request = NlSearchRequestDto.builder()
                .query("computer science students younger than 21")
                .build();

        mockMvc.perform(post("/api/v1/ai/students/nl-search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.totalMatched", is(1)))
                .andExpect(jsonPath("$.data.students[0].firstName", is("John")));
    }

    @Test
    @WithMockUser(username = "admin")
    public void testChatbot_ValidMessage_ReturnsAiResponse() throws Exception {
        when(claudeApiClient.sendPrompt(any(), any())).thenReturn("There are 1 students in Computer Science.");

        ChatRequestDto request = ChatRequestDto.builder()
                .message("How many CS students are there?")
                .build();

        mockMvc.perform(post("/api/v1/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.aiResponse", containsString("1 students")));
    }

    @Test
    @WithMockUser(username = "admin")
    public void testEmailGenerator_TooManyStudents_Returns400() throws Exception {
        List<Long> ids = new ArrayList<>();
        for (long i = 1; i <= 51; i++) {
            ids.add(i);
        }
        EmailRequestDto request = EmailRequestDto.builder()
                .studentIds(ids)
                .purpose("Reminder")
                .tone(EmailRequestDto.EmailTone.FRIENDLY)
                .build();

        mockMvc.perform(post("/api/v1/ai/students/email-content")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")));
    }

    @Test
    @WithMockUser(username = "admin")
    public void testPdfReport_ValidMonth_ReturnsPdfFile() throws Exception {
        when(claudeApiClient.sendPrompt(any(), any())).thenReturn("Institutional Monthly Report details.");

        mockMvc.perform(get("/api/v1/ai/reports/monthly")
                        .param("month", "6")
                        .param("year", "2026")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string("Content-Disposition", containsString("student-report-6-2026.pdf")));
    }

    @Test
    @WithMockUser(username = "admin")
    public void testAnalyticsInsights_Returns200WithStatsAndAiText() throws Exception {
        when(claudeApiClient.sendPrompt(any(), any())).thenReturn("Strategic recommendations narrative.");

        mockMvc.perform(get("/api/v1/ai/analytics/insights")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.rawStats.totalStudents", is(1)))
                .andExpect(jsonPath("$.data.aiInsights", is("Strategic recommendations narrative.")));
    }

    @Test
    @WithMockUser(username = "admin")
    public void testAtRiskDetection_Returns200WithCategorizedStudents() throws Exception {
        String mockResponse = "[{\"studentId\":" + savedStudent.getId() + ",\"riskLevel\":\"HIGH\",\"reason\":\"Age concerns\",\"recommendedAction\":\"Meeting\"}]";
        when(claudeApiClient.sendPrompt(any(), any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/ai/students/at-risk")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.atRiskCount", is(1)))
                .andExpect(jsonPath("$.data.students[0].studentName", is("John Doe")));
    }

    @Test
    @WithMockUser(username = "admin")
    public void testExistingStudentCrud_UnaffectedByAiChanges() throws Exception {
        mockMvc.perform(get("/api/v1/students/" + savedStudent.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.data.firstName", is("John")));
    }

    @Test
    @WithMockUser(username = "admin")
    public void testSwaggerDocs_AllNewEndpointsDocumented() throws Exception {
        mockMvc.perform(get("/v3/api-docs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/ai/students/{id}/study-plan']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/ai/students/nl-search']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/ai/analytics/insights']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/ai/students/at-risk']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/ai/chat']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/ai/students/email-content']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/ai/reports/monthly']").exists());
    }

    @Test
    @WithMockUser(username = "rate-limit-user")
    public void testRateLimit_ExceedLimit_Returns429() throws Exception {
        when(claudeApiClient.sendPrompt(any(), any())).thenReturn("Response content");

        // Execute 60 successful calls
        for (int i = 0; i < 60; i++) {
            mockMvc.perform(get("/api/v1/ai/students/" + savedStudent.getId() + "/study-plan")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        // The 61st call must return 429
        mockMvc.perform(get("/api/v1/ai/students/" + savedStudent.getId() + "/study-plan")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.message", containsString("AI rate limit exceeded")));
    }
}
