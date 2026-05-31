package com.example.student.service;

import com.example.student.dto.StudentRequestDTO;
import com.example.student.dto.StudentResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface StudentService {

    Page<StudentResponseDTO> getAllStudents(Pageable pageable);

    StudentResponseDTO getStudentById(Long id);

    StudentResponseDTO getStudentByEmail(String email);

    List<StudentResponseDTO> searchByName(String nameQuery);

    List<StudentResponseDTO> getStudentsByCourse(String course);

    Map<String, Object> getStudentStats();

    StudentResponseDTO createStudent(StudentRequestDTO requestDTO);

    StudentResponseDTO updateStudent(Long id, StudentRequestDTO requestDTO);

    StudentResponseDTO partialUpdateStudent(Long id, Map<String, Object> updates);

    void deleteStudent(Long id);
}
