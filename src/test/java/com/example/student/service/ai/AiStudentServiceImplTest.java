package com.example.student.service.ai;

import com.example.student.dto.StudentResponseDTO;
import com.example.student.dto.ai.*;
import com.example.student.exception.StudentNotFoundException;
import com.example.student.model.Student;
import com.example.student.repository.StudentRepository;
import com.example.student.service.ai.impl.AiStudentServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AiStudentServiceImplTest {

    private AiStudentServiceImpl aiStudentService;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ClaudeApiClient claudeApiClient;

    @Mock
    private AiCacheService aiCacheService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Student mockStudent;

    @BeforeEach
    public void setUp() {
        aiStudentService = new AiStudentServiceImpl(studentRepository, claudeApiClient, aiCacheService, objectMapper);

        mockStudent = Student.builder()
                .id(1L)
                .firstName("Rahul")
                .lastName("Kumar")
                .email("rahul@example.com")
                .course("Computer Science")
                .age(20)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    public void testGetStudyPlan_ValidStudent_ReturnsStudyPlan() {
        // Arrange
        when(aiCacheService.getFromCache(eq("studyPlans"), eq(1L), eq(StudyPlanResponseDto.class))).thenReturn(null);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(mockStudent));
        when(claudeApiClient.sendPrompt(any(), any())).thenReturn("Mocked study plan");

        // Act
        StudyPlanResponseDto result = aiStudentService.getStudyPlan(1L);

        // Assert
        assertNotNull(result);
        assertEquals("Mocked study plan", result.getStudyPlan());
        assertFalse(result.isCachedResponse());
        verify(aiCacheService, times(1)).putInCache(eq("studyPlans"), eq(1L), any());
    }

    @Test
    public void testGetStudyPlan_StudentNotFound_Throws404() {
        // Arrange
        when(aiCacheService.getFromCache(eq("studyPlans"), eq(2L), eq(StudyPlanResponseDto.class))).thenReturn(null);
        when(studentRepository.findById(2L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(StudentNotFoundException.class, () -> {
            aiStudentService.getStudyPlan(2L);
        });
    }

    @Test
    public void testGetStudyPlan_CacheHit_DoesNotCallClaude() {
        // Arrange
        StudyPlanResponseDto cachedDto = StudyPlanResponseDto.builder()
                .studentId(1L)
                .studentName("Rahul Kumar")
                .course("Computer Science")
                .studyPlan("Cached plan")
                .generatedAt(LocalDateTime.now())
                .cachedResponse(false)
                .build();

        when(aiCacheService.getFromCache(eq("studyPlans"), eq(1L), eq(StudyPlanResponseDto.class))).thenReturn(cachedDto);

        // Act
        StudyPlanResponseDto result = aiStudentService.getStudyPlan(1L);

        // Assert
        assertNotNull(result);
        assertEquals("Cached plan", result.getStudyPlan());
        assertTrue(result.isCachedResponse());
        verify(studentRepository, never()).findById(anyLong());
        verify(claudeApiClient, never()).sendPrompt(any(), any());
    }

    @Test
    public void testGetStudyPlan_ClaudeUnavailable_ReturnsFallback() {
        // Arrange
        when(aiCacheService.getFromCache(eq("studyPlans"), eq(1L), eq(StudyPlanResponseDto.class))).thenReturn(null);
        when(studentRepository.findById(1L)).thenReturn(Optional.of(mockStudent));
        when(claudeApiClient.sendPrompt(any(), any())).thenReturn("AI service is temporarily unavailable. Please try again in a moment.");

        // Act
        StudyPlanResponseDto result = aiStudentService.getStudyPlan(1L);

        // Assert
        assertNotNull(result);
        assertEquals("AI service is temporarily unavailable. Please try again in a moment.", result.getStudyPlan());
    }

    @Test
    public void testNaturalLanguageSearch_ValidQuery_ReturnsMatchedStudents() throws JsonProcessingException {
        // Arrange
        List<Student> students = Arrays.asList(mockStudent);
        when(studentRepository.findAll(any(PageRequest.class))).thenReturn(PageImpl.class.cast(new PageImpl<>(students)));
        
        String claudeJsonResponse = "[{\"id\":1,\"firstName\":\"Rahul\",\"lastName\":\"Kumar\",\"email\":\"rahul@example.com\",\"course\":\"Computer Science\",\"age\":20}]";
        when(claudeApiClient.sendPrompt(any(), any())).thenReturn(claudeJsonResponse);

        NlSearchRequestDto request = NlSearchRequestDto.builder().query("cs students under 21").build();

        // Act
        NlSearchResponseDto response = aiStudentService.searchStudentsNaturalLanguage(request);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getTotalMatched());
        assertEquals("Rahul Kumar", response.getStudents().get(0).getFirstName() + " " + response.getStudents().get(0).getLastName());
    }

    @Test
    public void testNaturalLanguageSearch_InvalidJsonFromClaude_ReturnsEmptyList() {
        // Arrange
        List<Student> students = Arrays.asList(mockStudent);
        when(studentRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(students));
        when(claudeApiClient.sendPrompt(any(), any())).thenReturn("This is not JSON");

        NlSearchRequestDto request = NlSearchRequestDto.builder().query("cs students under 21").build();

        // Act
        NlSearchResponseDto response = aiStudentService.searchStudentsNaturalLanguage(request);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getTotalMatched());
        assertTrue(response.getStudents().isEmpty());
    }

    @Test
    public void testAtRiskDetection_ProcessesInBatches() throws JsonProcessingException {
        // Arrange
        List<Student> students = new ArrayList<>();
        for (long i = 1; i <= 25; i++) {
            students.add(Student.builder()
                    .id(i)
                    .firstName("Name" + i)
                    .lastName("Last" + i)
                    .course("Mathematics")
                    .age(19)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        when(studentRepository.findAll()).thenReturn(students);
        
        // Mock responses for batch 1 (size 20) and batch 2 (size 5)
        String batch1Result = "[{\"studentId\":1,\"riskLevel\":\"HIGH\",\"reason\":\"Low participation\",\"recommendedAction\":\"Counseling\"}]";
        String batch2Result = "[{\"studentId\":21,\"riskLevel\":\"LOW\",\"reason\":\"Age mismatch\",\"recommendedAction\":\"None\"}]";
        when(claudeApiClient.sendPrompt(any(), any()))
                .thenReturn(batch1Result)
                .thenReturn(batch2Result);

        // Act
        AtRiskResponseDto response = aiStudentService.getAtRiskStudents();

        // Assert
        assertNotNull(response);
        assertEquals(25, response.getTotalAnalyzed());
        assertEquals(2, response.getAtRiskCount());
        verify(claudeApiClient, times(2)).sendPrompt(any(), any());
    }

    @Test
    public void testEmailGenerator_ExceedsMaxStudents_ThrowsValidationException() {
        // Arrange
        List<Long> ids = new ArrayList<>();
        for (long i = 1; i <= 51; i++) {
            ids.add(i);
        }
        EmailRequestDto request = EmailRequestDto.builder()
                .studentIds(ids)
                .purpose("Reminder")
                .tone(EmailRequestDto.EmailTone.FRIENDLY)
                .build();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            aiStudentService.generateEmailContent(request);
        });
    }

    @Test
    public void testEmailGenerator_RunsInParallel_AllEmailsGenerated() {
        // Arrange
        List<Long> ids = Arrays.asList(1L, 2L);
        Student student2 = Student.builder()
                .id(2L)
                .firstName("Aarav")
                .lastName("Sharma")
                .course("Computer Science")
                .email("aarav@example.com")
                .age(22)
                .build();

        when(studentRepository.findAllById(ids)).thenReturn(Arrays.asList(mockStudent, student2));
        
        // Mock two calls to claude (one for each student)
        when(claudeApiClient.sendPrompt(any(), any()))
                .thenReturn("Subject: CS Exam\n\nBody:\nDear student...")
                .thenReturn("Subject: CS Exam\n\nBody:\nDear student...");

        EmailRequestDto request = EmailRequestDto.builder()
                .studentIds(ids)
                .purpose("Exam reminder")
                .tone(EmailRequestDto.EmailTone.FORMAL)
                .build();

        // Act
        EmailResponseDto response = aiStudentService.generateEmailContent(request);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getTotalGenerated());
        assertEquals(2, response.getEmails().size());
        verify(claudeApiClient, times(2)).sendPrompt(any(), any());
    }
}
