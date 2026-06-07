package com.example.student.controller;

import com.example.student.dto.StudentRequestDTO;
import com.example.student.dto.StudentResponseDTO;
import com.example.student.exception.StudentNotFoundException;
import com.example.student.service.StudentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.context.annotation.Import;
import com.example.student.security.SecurityConfig;
import com.example.student.security.JwtAuthenticationFilter;
import com.example.student.security.AiRateLimitFilter;

@WebMvcTest(StudentController.class)
@WithMockUser
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AiRateLimitFilter.class})
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StudentService studentService;

    @MockBean
    private com.example.student.security.JwtService jwtService;

    @MockBean
    private com.example.student.security.CustomUserDetailsService userDetailsService;

    private StudentResponseDTO responseDTO;
    private StudentRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        responseDTO = StudentResponseDTO.builder()
                .id(1L)
                .firstName("Rahul")
                .lastName("Kumar")
                .email("rahul@gmail.com")
                .phone("9876543210")
                .course("Computer Science")
                .age(20)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        requestDTO = StudentRequestDTO.builder()
                .firstName("Rahul")
                .lastName("Kumar")
                .email("rahul@gmail.com")
                .phone("9876543210")
                .course("Computer Science")
                .age(20)
                .build();
    }

    @Test
    void testGetAllStudents() throws Exception {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("firstName"));
        when(studentService.getAllStudents(pageable))
                .thenReturn(new PageImpl<>(Collections.singletonList(responseDTO)));

        mockMvc.perform(get("/api/v1/students")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "firstName")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.message", is("Students retrieved successfully")))
                .andExpect(jsonPath("$.data.content[0].email", is("rahul@gmail.com")));

        verify(studentService, times(1)).getAllStudents(pageable);
    }

    @Test
    void testGetStudentById_Success() throws Exception {
        when(studentService.getStudentById(1L)).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/students/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.message", is("Student found")))
                .andExpect(jsonPath("$.data.firstName", is("Rahul")));

        verify(studentService, times(1)).getStudentById(1L);
    }

    @Test
    void testGetStudentById_NotFound() throws Exception {
        when(studentService.getStudentById(99L))
                .thenThrow(new StudentNotFoundException("Student not found with id: 99"));

        mockMvc.perform(get("/api/v1/students/99")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.message", is("Student not found with id: 99")));

        verify(studentService, times(1)).getStudentById(99L);
    }

    @Test
    void testGetStudentByEmail_Success() throws Exception {
        when(studentService.getStudentByEmail("rahul@gmail.com")).thenReturn(responseDTO);

        mockMvc.perform(get("/api/v1/students/search")
                        .param("email", "rahul@gmail.com")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.message", is("Student found")))
                .andExpect(jsonPath("$.data.email", is("rahul@gmail.com")));

        verify(studentService, times(1)).getStudentByEmail("rahul@gmail.com");
    }

    @Test
    void testCreateStudent_Success() throws Exception {
        when(studentService.createStudent(any(StudentRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.message", is("Student created successfully")))
                .andExpect(jsonPath("$.data.id", is(1)));

        verify(studentService, times(1)).createStudent(any(StudentRequestDTO.class));
    }

    @Test
    void testCreateStudent_ValidationError() throws Exception {
        StudentRequestDTO invalidRequest = StudentRequestDTO.builder()
                .firstName("") // Blank
                .lastName("Kumar")
                .email("invalid-email") // Invalid email pattern
                .age(150) // Outside @Max(100) limit
                .build();

        mockMvc.perform(post("/api/v1/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.data.firstName").exists())
                .andExpect(jsonPath("$.data.email").exists())
                .andExpect(jsonPath("$.data.age").exists());

        verify(studentService, never()).createStudent(any(StudentRequestDTO.class));
    }

    @Test
    void testUpdateStudent_Success() throws Exception {
        when(studentService.updateStudent(eq(1L), any(StudentRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(put("/api/v1/students/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.message", is("Student updated successfully")))
                .andExpect(jsonPath("$.data.firstName", is("Rahul")));

        verify(studentService, times(1)).updateStudent(eq(1L), any(StudentRequestDTO.class));
    }

    @Test
    void testDeleteStudent_Success() throws Exception {
        doNothing().when(studentService).deleteStudent(1L);

        mockMvc.perform(delete("/api/v1/students/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.message", is("Student deleted successfully")));

        verify(studentService, times(1)).deleteStudent(1L);
    }
}
