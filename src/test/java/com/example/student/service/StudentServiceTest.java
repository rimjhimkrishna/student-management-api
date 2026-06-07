package com.example.student.service;

import com.example.student.dto.StudentRequestDTO;
import com.example.student.dto.StudentResponseDTO;
import com.example.student.exception.StudentNotFoundException;
import com.example.student.model.Student;
import com.example.student.repository.StudentRepository;
import com.example.student.service.impl.StudentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudentServiceImpl studentService;

    private Student student;
    private StudentRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        student = Student.builder()
                .id(1L)
                .firstName("Rahul")
                .lastName("Kumar")
                .email("rahul@example.com")
                .phone("9876543210")
                .course("Computer Science")
                .age(20)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        requestDTO = StudentRequestDTO.builder()
                .firstName("Rahul")
                .lastName("Kumar")
                .email("rahul@example.com")
                .phone("9876543210")
                .course("Computer Science")
                .age(20)
                .build();
    }

    @Test
    void testGetAllStudents() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Student> page = new PageImpl<>(Collections.singletonList(student));
        
        when(studentRepository.findAll(pageable)).thenReturn(page);

        Page<StudentResponseDTO> result = studentService.getAllStudents(pageable);
        
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("rahul@example.com");
        verify(studentRepository, times(1)).findAll(pageable);
    }

    @Test
    void testGetStudentById_Success() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        StudentResponseDTO result = studentService.getStudentById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("Rahul");
        verify(studentRepository, times(1)).findById(1L);
    }

    @Test
    void testGetStudentById_NotFound() {
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.getStudentById(1L))
                .isInstanceOf(StudentNotFoundException.class)
                .hasMessageContaining("Student not found with id: 1");
        
        verify(studentRepository, times(1)).findById(1L);
    }

    @Test
    void testGetStudentByEmail_Success() {
        when(studentRepository.findByEmail("rahul@example.com")).thenReturn(Optional.of(student));

        StudentResponseDTO result = studentService.getStudentByEmail("rahul@example.com");

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("rahul@example.com");
        verify(studentRepository, times(1)).findByEmail("rahul@example.com");
    }

    @Test
    void testGetStudentByEmail_NotFound() {
        when(studentRepository.findByEmail("rahul@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.getStudentByEmail("rahul@example.com"))
                .isInstanceOf(StudentNotFoundException.class)
                .hasMessageContaining("Student not found with email: rahul@example.com");
        
        verify(studentRepository, times(1)).findByEmail("rahul@example.com");
    }

    @Test
    void testCreateStudent_Success() {
        when(studentRepository.existsByEmail("rahul@example.com")).thenReturn(false);
        when(studentRepository.save(any(Student.class))).thenReturn(student);

        StudentResponseDTO result = studentService.createStudent(requestDTO);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("rahul@example.com");
        verify(studentRepository, times(1)).existsByEmail("rahul@example.com");
        verify(studentRepository, times(1)).save(any(Student.class));
    }

    @Test
    void testCreateStudent_DuplicateEmail() {
        when(studentRepository.existsByEmail("rahul@example.com")).thenReturn(true);

        assertThatThrownBy(() -> studentService.createStudent(requestDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Student already exists with email: rahul@example.com");

        verify(studentRepository, times(1)).existsByEmail("rahul@example.com");
        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    void testUpdateStudent_Success() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(studentRepository.existsByEmailAndIdNot("rahul@example.com", 1L)).thenReturn(false);
        when(studentRepository.save(any(Student.class))).thenReturn(student);

        StudentResponseDTO result = studentService.updateStudent(1L, requestDTO);

        assertThat(result).isNotNull();
        assertThat(result.getLastName()).isEqualTo("Kumar");
        verify(studentRepository, times(1)).findById(1L);
        verify(studentRepository, times(1)).existsByEmailAndIdNot("rahul@example.com", 1L);
        verify(studentRepository, times(1)).save(student);
    }

    @Test
    void testUpdateStudent_DuplicateEmail() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
        when(studentRepository.existsByEmailAndIdNot("rahul@example.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> studentService.updateStudent(1L, requestDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already in use: rahul@example.com");

        verify(studentRepository, times(1)).findById(1L);
        verify(studentRepository, times(1)).existsByEmailAndIdNot("rahul@example.com", 1L);
        verify(studentRepository, never()).save(any(Student.class));
    }

    @Test
    void testDeleteStudent_Success() {
        when(studentRepository.existsById(1L)).thenReturn(true);
        doNothing().when(studentRepository).deleteById(1L);

        studentService.deleteStudent(1L);

        verify(studentRepository, times(1)).existsById(1L);
        verify(studentRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteStudent_NotFound() {
        when(studentRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> studentService.deleteStudent(1L))
                .isInstanceOf(StudentNotFoundException.class)
                .hasMessageContaining("Student not found with id: 1");

        verify(studentRepository, times(1)).existsById(1L);
        verify(studentRepository, never()).deleteById(1L);
    }
}
