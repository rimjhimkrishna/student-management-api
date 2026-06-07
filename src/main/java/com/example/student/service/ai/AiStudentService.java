package com.example.student.service.ai;

import com.example.student.dto.ai.*;

public interface AiStudentService {
    
    StudyPlanResponseDto getStudyPlan(Long id);

    NlSearchResponseDto searchStudentsNaturalLanguage(NlSearchRequestDto request);

    AnalyticsResponseDto getAnalyticsInsights();

    AtRiskResponseDto getAtRiskStudents();

    ChatResponseDto chatbotQuery(ChatRequestDto request);

    EmailResponseDto generateEmailContent(EmailRequestDto request);

    byte[] generateMonthlyReportPdf(int month, int year);
}
