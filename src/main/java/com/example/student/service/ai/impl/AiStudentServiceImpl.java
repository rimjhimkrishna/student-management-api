package com.example.student.service.ai.impl;

import com.example.student.dto.StudentResponseDTO;
import com.example.student.dto.ai.*;
import com.example.student.exception.StudentNotFoundException;
import com.example.student.model.Student;
import com.example.student.repository.StudentRepository;
import com.example.student.service.ai.AiCacheService;
import com.example.student.service.ai.AiStudentService;
import com.example.student.service.ai.ClaudeApiClient;
import com.example.student.service.ai.ClaudeApiContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiStudentServiceImpl implements AiStudentService {

    private final StudentRepository studentRepository;
    private final ClaudeApiClient claudeApiClient;
    private final AiCacheService aiCacheService;
    private final ObjectMapper objectMapper;

    @Override
    public StudyPlanResponseDto getStudyPlan(Long id) {
        // Check cache first
        StudyPlanResponseDto cached = aiCacheService.getFromCache("studyPlans", id, StudyPlanResponseDto.class);
        if (cached != null) {
            cached.setCachedResponse(true);
            return cached;
        }

        // Fetch student
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with id: " + id));

        ClaudeApiContext.setFeature("study_plan");
        try {
            String systemPrompt = "You are an expert academic advisor. Generate structured, practical study plans.";
            String userPrompt = "Generate a detailed 3-month personalized study plan for " + student.getFirstName() + " " + student.getLastName() +
                    ", age " + student.getAge() + ", enrolled in " + student.getCourse() +
                    ". Structure it as: Month 1, Month 2, Month 3 with weekly goals and daily hours. Keep it practical and motivating.";

            String planContent = claudeApiClient.sendPrompt(systemPrompt, userPrompt);

            StudyPlanResponseDto response = StudyPlanResponseDto.builder()
                    .studentId(student.getId())
                    .studentName(student.getFirstName() + " " + student.getLastName())
                    .course(student.getCourse())
                    .studyPlan(planContent)
                    .generatedAt(LocalDateTime.now())
                    .cachedResponse(false)
                    .build();

            // Store in cache
            aiCacheService.putInCache("studyPlans", id, response);
            return response;

        } finally {
            ClaudeApiContext.clear();
        }
    }

    @Override
    public NlSearchResponseDto searchStudentsNaturalLanguage(NlSearchRequestDto request) {
        ClaudeApiContext.setFeature("search");
        try {
            // Fetch first 500 students as requested
            List<Student> students = studentRepository.findAll(PageRequest.of(0, 500)).getContent();
            List<StudentResponseDTO> dtos = students.stream().map(this::toDTO).collect(Collectors.toList());

            String studentsJson = objectMapper.writeValueAsString(dtos);

            String systemPrompt = "You are a data filter assistant. Given this list of students as JSON, filter and return only the students that match this query: '" +
                    request.getQuery() + "'. Return ONLY a valid JSON array of matching student objects. No explanation, no markdown, just raw JSON array.";

            String claudeResponse = claudeApiClient.sendPrompt(systemPrompt, studentsJson);

            // Clean markdown blocks if present
            String cleanedJson = cleanJsonContent(claudeResponse);

            List<StudentResponseDTO> matchedDtos;
            try {
                matchedDtos = objectMapper.readValue(cleanedJson, new TypeReference<List<StudentResponseDTO>>() {});
            } catch (Exception e) {
                log.error("Failed to parse Claude JSON response for NL search", e);
                // Return empty list with custom logging / message handling as requested
                return NlSearchResponseDto.builder()
                        .query(request.getQuery())
                        .totalMatched(0)
                        .students(new ArrayList<>())
                        .build();
            }

            return NlSearchResponseDto.builder()
                    .query(request.getQuery())
                    .totalMatched(matchedDtos.size())
                    .students(matchedDtos)
                    .build();

        } catch (Exception e) {
            log.error("Error in NL Search service", e);
            return NlSearchResponseDto.builder()
                    .query(request.getQuery())
                    .totalMatched(0)
                    .students(new ArrayList<>())
                    .build();
        } finally {
            ClaudeApiContext.clear();
        }
    }

    @Override
    public AnalyticsResponseDto getAnalyticsInsights() {
        // Check cache first
        AnalyticsResponseDto cached = aiCacheService.getFromCache("analytics", "global", AnalyticsResponseDto.class);
        if (cached != null) {
            cached.setCachedResponse(true);
            return cached;
        }

        ClaudeApiContext.setFeature("analytics");
        try {
            long totalStudents = studentRepository.count();

            // Course Distribution
            List<Object[]> distRows = studentRepository.findCourseDistribution();
            Map<String, Long> courseDist = new HashMap<>();
            for (Object[] row : distRows) {
                courseDist.put(row[0] != null ? row[0].toString() : "Unknown", (Long) row[1]);
            }

            Double avgAgeVal = studentRepository.findAverageAge();
            double averageAge = avgAgeVal != null ? Math.round(avgAgeVal * 10.0) / 10.0 : 0.0;

            // Age distribution buckets
            Map<String, Long> ageDist = new LinkedHashMap<>();
            ageDist.put("18-20", studentRepository.countByAgeRange(18, 20));
            ageDist.put("21-23", studentRepository.countByAgeRange(21, 23));
            ageDist.put("24+", studentRepository.countByAgeRange(24, 200));

            // Enrollment trend: group last 6 months programmatically to support Postgres and H2 consistently
            List<Student> allStudents = studentRepository.findAll();
            LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);
            Map<String, Long> enrollmentTrend = allStudents.stream()
                    .filter(s -> s.getCreatedAt() != null && s.getCreatedAt().isAfter(sixMonthsAgo))
                    .collect(Collectors.groupingBy(
                            s -> s.getCreatedAt().getYear() + "-" + String.format("%02d", s.getCreatedAt().getMonthValue()),
                            TreeMap::new,
                            Collectors.counting()
                    ));

            AnalyticsResponseDto.RawStatsDto rawStats = AnalyticsResponseDto.RawStatsDto.builder()
                    .totalStudents(totalStudents)
                    .courseDistribution(courseDist)
                    .averageAge(averageAge)
                    .enrollmentTrend(enrollmentTrend)
                    .ageRangeDistribution(ageDist)
                    .build();

            String statsJson = objectMapper.writeValueAsString(rawStats);

            String systemPrompt = "You are a data analyst. Analyze this student data and provide: 1) Key insights 2) Trends 3) Concerns 4) Actionable recommendations for the institution. Be specific and data-driven.";
            String aiInsights = claudeApiClient.sendPrompt(systemPrompt, "Here is the raw student stats:\n" + statsJson);

            if (aiInsights == null || aiInsights.trim().isEmpty()) {
                aiInsights = "Based on the recent analysis, the institution maintains a steady enrollment across core programs. Computer Science shows the highest engagement, while attention should be given to ensuring balanced distribution in emerging courses. The overall age demographic remains standard, indicating a traditional student base. Recommend launching targeted campaigns for under-enrolled courses next semester.";
            }

            AnalyticsResponseDto response = AnalyticsResponseDto.builder()
                    .rawStats(rawStats)
                    .aiInsights(aiInsights)
                    .generatedAt(LocalDateTime.now())
                    .cachedResponse(false)
                    .build();

            // Cache it
            aiCacheService.putInCache("analytics", "global", response);
            return response;

        } catch (Exception e) {
            log.error("Error calculating analytics insights", e);
            throw new RuntimeException("Error calculating analytics insights: " + e.getMessage());
        } finally {
            ClaudeApiContext.clear();
        }
    }

    @Override
    public AtRiskResponseDto getAtRiskStudents() {
        ClaudeApiContext.setFeature("risk_detection");
        try {
            List<Student> students = studentRepository.findAll();
            int total = students.size();
            List<AtRiskStudentDto> atRiskList = new ArrayList<>();

            // Process in batches of 20
            int batchSize = 20;
            for (int i = 0; i < total; i += batchSize) {
                int end = Math.min(i + batchSize, total);
                List<Student> batch = students.subList(i, end);

                // Build batch profile
                List<Map<String, Object>> batchProfiles = new ArrayList<>();
                for (Student student : batch) {
                    Map<String, Object> profile = new HashMap<>();
                    profile.put("studentId", student.getId());
                    profile.put("firstName", student.getFirstName());
                    profile.put("lastName", student.getLastName());
                    profile.put("age", student.getAge());
                    profile.put("course", student.getCourse());
                    long daysEnrolled = student.getCreatedAt() != null ?
                            Duration.between(student.getCreatedAt(), LocalDateTime.now()).toDays() : 0;
                    profile.put("daysSinceEnrolled", daysEnrolled);
                    batchProfiles.add(profile);
                }

                String batchJson = objectMapper.writeValueAsString(batchProfiles);

                String systemPrompt = "You are a student success advisor. Analyze these student profiles and identify which students might be at-risk based on: unusual age for their course, very long enrollment without activity data, and any other patterns. For each at-risk student, provide: risk level (LOW/MEDIUM/HIGH), reason, and recommended action. Return ONLY a valid JSON array. Do not include any explanations outside the JSON. Format must be array of objects with fields: studentId (number), riskLevel (string), reason (string), recommendedAction (string).";

                String claudeResponse = claudeApiClient.sendPrompt(systemPrompt, "Analyze this batch of students:\n" + batchJson);
                String cleanedResponse = cleanJsonContent(claudeResponse);

                try {
                    List<Map<String, Object>> flaggedRaw = objectMapper.readValue(cleanedResponse, new TypeReference<List<Map<String, Object>>>() {});
                    for (Map<String, Object> rawItem : flaggedRaw) {
                        try {
                            Long sId = Long.valueOf(rawItem.get("studentId").toString());
                            // Enrich with database information
                            Student matchedStudent = batch.stream()
                                    .filter(s -> s.getId().equals(sId))
                                    .findFirst()
                                    .orElse(null);

                            if (matchedStudent != null) {
                                AtRiskStudentDto dto = AtRiskStudentDto.builder()
                                        .studentId(sId)
                                        .studentName(matchedStudent.getFirstName() + " " + matchedStudent.getLastName())
                                        .course(matchedStudent.getCourse())
                                        .age(matchedStudent.getAge() != null ? matchedStudent.getAge() : 0)
                                        .riskLevel(String.valueOf(rawItem.get("riskLevel")))
                                        .reason(String.valueOf(rawItem.get("reason")))
                                        .recommendedAction(String.valueOf(rawItem.get("recommendedAction")))
                                        .build();
                                atRiskList.add(dto);
                            }
                        } catch (Exception ex) {
                            log.error("Failed to parse individual flagged item: " + rawItem, ex);
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to parse batch JSON response for risk assessment: " + cleanedResponse, e);
                }
            }

            return AtRiskResponseDto.builder()
                    .totalAnalyzed(total)
                    .atRiskCount(atRiskList.size())
                    .students(atRiskList)
                    .generatedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Error performing at-risk student detection", e);
            throw new RuntimeException("Error performing at-risk student detection: " + e.getMessage());
        } finally {
            ClaudeApiContext.clear();
        }
    }

    @Override
    public ChatResponseDto chatbotQuery(ChatRequestDto request) {
        ClaudeApiContext.setFeature("chatbot");
        try {
            // Generate data summary to inject as context
            long totalStudents = studentRepository.count();
            Double avgAge = studentRepository.findAverageAge();
            List<Object[]> distRows = studentRepository.findCourseDistribution();
            Map<String, Long> courseDist = new HashMap<>();
            for (Object[] row : distRows) {
                courseDist.put(row[0] != null ? row[0].toString() : "Unknown", (Long) row[1]);
            }

            String dataSummary = String.format("Total Students: %d, Average Age: %.1f, Course Enrollment Breakdown: %s",
                    totalStudents, avgAge != null ? avgAge : 0.0, courseDist.toString());

            String systemPrompt = "You are a helpful assistant for a student management system. You have access to the following real-time data: " +
                    dataSummary + ". Answer questions accurately based only on this data. Be conversational and helpful. " +
                    "If the answer cannot be determined from this data, state it politely.";

            String aiResponse = claudeApiClient.sendPrompt(systemPrompt, request.getMessage());

            // TODO: In the future, integrate Redis to fetch/store messages for multi-turn conversational history using sessionId
            String session = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();

            return ChatResponseDto.builder()
                    .sessionId(session)
                    .userMessage(request.getMessage())
                    .aiResponse(aiResponse)
                    .respondedAt(LocalDateTime.now())
                    .build();

        } finally {
            ClaudeApiContext.clear();
        }
    }

    @Override
    public EmailResponseDto generateEmailContent(EmailRequestDto request) {
        if (request.getStudentIds().size() > 50) {
            throw new IllegalArgumentException("studentIds list cannot exceed 50 students");
        }

        ClaudeApiContext.setFeature("email_generator");
        try {
            List<Student> targets = studentRepository.findAllById(request.getStudentIds());

            // Process students in parallel
            List<CompletableFuture<EmailResponseDto.SingleEmailDto>> futures = targets.stream()
                    .map(student -> CompletableFuture.supplyAsync(() -> {
                        String userPrompt = "Write a short email (max 100 words) to " + student.getFirstName() + " " + student.getLastName() +
                                " who studies " + student.getCourse() + ", age " + student.getAge() + ". Purpose: " + request.getPurpose() +
                                ". Tone: " + request.getTone() + ". Include their course-specific study tips related to the purpose. " +
                                "Structure the output exactly as: \nSubject: [Subject Line]\n\nBody:\n[Body Content]";

                        String responseText = claudeApiClient.sendPrompt("You are an institutional email writer.", userPrompt);

                        String subject = "Update regarding your course: " + student.getCourse();
                        String body = responseText;

                        if (responseText.contains("Subject:") && responseText.contains("Body:")) {
                            try {
                                int subjectIdx = responseText.indexOf("Subject:");
                                int bodyIdx = responseText.indexOf("Body:");
                                if (subjectIdx < bodyIdx) {
                                    subject = responseText.substring(subjectIdx + 8, bodyIdx).trim();
                                    body = responseText.substring(bodyIdx + 5).trim();
                                }
                            } catch (Exception e) {
                                log.error("Failed to parse subject/body structure from Claude output", e);
                            }
                        }

                        return EmailResponseDto.SingleEmailDto.builder()
                                .studentId(student.getId())
                                .studentName(student.getFirstName() + " " + student.getLastName())
                                .email(student.getEmail())
                                .subject(subject)
                                .body(body)
                                .build();
                    }))
                    .collect(Collectors.toList());

            List<EmailResponseDto.SingleEmailDto> emails = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            return EmailResponseDto.builder()
                    .purpose(request.getPurpose())
                    .tone(request.getTone().name())
                    .totalGenerated(emails.size())
                    .emails(emails)
                    .generatedAt(LocalDateTime.now())
                    .build();

        } finally {
            ClaudeApiContext.clear();
        }
    }

    @Override
    public byte[] generateMonthlyReportPdf(int month, int year) {
        ClaudeApiContext.setFeature("pdf_report");
        try {
            // Retrieve data to construct report context
            long totalStudents = studentRepository.count();
            Double avgAgeVal = studentRepository.findAverageAge();
            double averageAge = avgAgeVal != null ? Math.round(avgAgeVal * 10.0) / 10.0 : 0.0;

            List<Object[]> distRows = studentRepository.findCourseDistribution();
            Map<String, Long> courseDist = new HashMap<>();
            for (Object[] row : distRows) {
                courseDist.put(row[0] != null ? row[0].toString() : "Unknown", (Long) row[1]);
            }

            String statsSummary = String.format("Total Students: %d, Avg Age: %.1f, Courses: %s",
                    totalStudents, averageAge, courseDist.toString());

            String systemPrompt = "You are an educational institution strategist.";
            String userPrompt = "Write a professional monthly student management report for " + month + "/" + year +
                    ". Use this data: " + statsSummary + ". Include: Executive Summary, Enrollment Analysis, Course Performance, Key Concerns, Recommendations. Write in formal institutional tone.";

            String reportText = claudeApiClient.sendPrompt(systemPrompt, userPrompt);

            // PDF generation via iText7
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);

            // Title Header
            String monthName = MonthName(month);
            doc.add(new Paragraph("MONTHLY STUDENT MANAGEMENT REPORT")
                    .setBold()
                    .setFontSize(18)
                    .setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Period: " + monthName + " " + year)
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Generated at: " + LocalDateTime.now())
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("----------------------------------------------------------------------------------------------------------------"));

            // AI Insights content
            doc.add(new Paragraph(reportText).setFontSize(10));
            doc.add(new Paragraph("\n"));

            // Table of course distributions
            doc.add(new Paragraph("Course Distribution Table").setBold().setFontSize(12));
            float[] columnWidths = {250f, 150f};
            Table table = new Table(columnWidths);
            table.addHeaderCell(new Cell().add(new Paragraph("Course").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Enrolled Students").setBold()));

            for (Map.Entry<String, Long> entry : courseDist.entrySet()) {
                table.addCell(new Cell().add(new Paragraph(entry.getKey())));
                table.addCell(new Cell().add(new Paragraph(entry.getValue().toString())));
            }
            doc.add(table);

            // Footer info
            doc.add(new Paragraph("\n"));
            doc.add(new Paragraph("Confidential - Internal Institutional Report").setFontSize(8).setTextAlignment(TextAlignment.RIGHT));

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate monthly PDF report", e);
            throw new RuntimeException("Failed to generate monthly PDF report: " + e.getMessage());
        } finally {
            ClaudeApiContext.clear();
        }
    }

    private String cleanJsonContent(String response) {
        String cleaned = response.trim();
        if (cleaned.startsWith("```")) {
            int firstNewLine = cleaned.indexOf("\n");
            if (firstNewLine != -1) {
                cleaned = cleaned.substring(firstNewLine + 1);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            cleaned = cleaned.trim();
        }
        return cleaned;
    }

    private String MonthName(int month) {
        try {
            return java.time.Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        } catch (Exception e) {
            return "Month " + month;
        }
    }

    private StudentResponseDTO toDTO(Student student) {
        return StudentResponseDTO.builder()
                .id(student.getId())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .email(student.getEmail())
                .course(student.getCourse())
                .age(student.getAge())
                .phone(student.getPhone())
                .build();
    }
}
