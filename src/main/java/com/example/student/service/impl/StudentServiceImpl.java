package com.example.student.service.impl;

import com.example.student.dto.StudentRequestDTO;
import com.example.student.dto.StudentResponseDTO;
import com.example.student.exception.StudentNotFoundException;
import com.example.student.model.Student;
import com.example.student.repository.StudentRepository;
import com.example.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;

    @Override
    @Cacheable("students")
    public Page<StudentResponseDTO> getAllStudents(Pageable pageable) {
        log.info("Fetching all students with pagination: {}", pageable);
        return studentRepository.findAll(pageable).map(this::toDTO);
    }

    @Override
    public StudentResponseDTO getStudentById(Long id) {
        log.info("Fetching student with id: {}", id);
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with id: " + id));
        return toDTO(student);
    }

    @Override
    public StudentResponseDTO getStudentByEmail(String email) {
        log.info("Fetching student with email: {}", email);
        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with email: " + email));
        return toDTO(student);
    }

    @Override
    public List<StudentResponseDTO> searchByName(String nameQuery) {
        log.info("Searching students by name: {}", nameQuery);
        return studentRepository.searchByName(nameQuery)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<StudentResponseDTO> getStudentsByCourse(String course) {
        log.info("Fetching students by course: {}", course);
        return studentRepository.findByCourseIgnoreCase(course)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getStudentStats() {
        log.info("Generating student statistics");
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStudents", studentRepository.count());
        stats.put("courses", studentRepository.findDistinctCourses());
        Double avgAge = studentRepository.findAverageAge();
        stats.put("averageAge", avgAge != null ? Math.round(avgAge * 10.0) / 10.0 : 0);
        return stats;
    }

    @Override
    @Transactional
    @CacheEvict(value = "students", allEntries = true)
    public StudentResponseDTO createStudent(StudentRequestDTO requestDTO) {
        log.info("Creating new student with email: {}", requestDTO.getEmail());
        if (studentRepository.existsByEmail(requestDTO.getEmail())) {
            throw new IllegalArgumentException("Student already exists with email: " + requestDTO.getEmail());
        }
        Student student = toEntity(requestDTO);
        Student saved = studentRepository.save(student);
        return toDTO(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "students", allEntries = true)
    public StudentResponseDTO updateStudent(Long id, StudentRequestDTO requestDTO) {
        log.info("Updating student with id: {}", id);
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with id: " + id));
        if (studentRepository.existsByEmailAndIdNot(requestDTO.getEmail(), id)) {
            throw new IllegalArgumentException("Email already in use: " + requestDTO.getEmail());
        }
        student.setFirstName(requestDTO.getFirstName());
        student.setLastName(requestDTO.getLastName());
        student.setEmail(requestDTO.getEmail());
        student.setCourse(requestDTO.getCourse());
        student.setAge(requestDTO.getAge());
        student.setPhone(requestDTO.getPhone());
        return toDTO(studentRepository.save(student));
    }

    @Override
    @Transactional
    @CacheEvict(value = "students", allEntries = true)
    public StudentResponseDTO partialUpdateStudent(Long id, Map<String, Object> updates) {
        log.info("Partially updating student with id: {}", id);
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with id: " + id));
        updates.forEach((key, value) -> {
            switch (key) {
                case "firstName" -> student.setFirstName((String) value);
                case "lastName" -> student.setLastName((String) value);
                case "email" -> {
                    String newEmail = (String) value;
                    if (studentRepository.existsByEmailAndIdNot(newEmail, id)) {
                        throw new IllegalArgumentException("Email already in use: " + newEmail);
                    }
                    student.setEmail(newEmail);
                }
                case "course" -> student.setCourse((String) value);
                case "age" -> student.setAge(value instanceof Integer ? (Integer) value : Integer.parseInt(value.toString()));
                case "phone" -> student.setPhone((String) value);
                default -> log.warn("Unknown field in partial update: {}", key);
            }
        });
        return toDTO(studentRepository.save(student));
    }

    @Override
    @Transactional
    @CacheEvict(value = "students", allEntries = true)
    public void deleteStudent(Long id) {
        log.info("Deleting student with id: {}", id);
        if (!studentRepository.existsById(id)) {
            throw new StudentNotFoundException("Student not found with id: " + id);
        }
        studentRepository.deleteById(id);
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

    private Student toEntity(StudentRequestDTO dto) {
        return Student.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .course(dto.getCourse())
                .age(dto.getAge())
                .phone(dto.getPhone())
                .build();
    }
}
