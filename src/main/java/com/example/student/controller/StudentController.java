package com.example.student.controller;

import com.example.student.dto.ApiResponse;
import com.example.student.dto.StudentRequestDTO;
import com.example.student.dto.StudentResponseDTO;
import com.example.student.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Tag(name = "Student Management", description = "Comprehensive APIs for managing student profiles")
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    @Operation(summary = "Get all students", description = "Returns paginated and sorted list of all students")
    public ResponseEntity<ApiResponse<Page<StudentResponseDTO>>> getAllStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "firstName") String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        Page<StudentResponseDTO> students = studentService.getAllStudents(pageable);
        return ResponseEntity.ok(ApiResponse.success("Students retrieved successfully", students));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get student by ID")
    public ResponseEntity<ApiResponse<StudentResponseDTO>> getStudentById(@PathVariable Long id) {
        StudentResponseDTO student = studentService.getStudentById(id);
        return ResponseEntity.ok(ApiResponse.success("Student found", student));
    }

    @GetMapping("/search")
    @Operation(summary = "Search student by email", description = "Find student using exact email match")
    public ResponseEntity<ApiResponse<StudentResponseDTO>> getStudentByEmail(
            @RequestParam String email) {
        StudentResponseDTO student = studentService.getStudentByEmail(email);
        return ResponseEntity.ok(ApiResponse.success("Student found", student));
    }

    @GetMapping("/search/name")
    @Operation(summary = "Search students by name", description = "Partial/case-insensitive search by first or last name")
    public ResponseEntity<ApiResponse<List<StudentResponseDTO>>> searchByName(
            @Parameter(description = "Name keyword to search") @RequestParam String q) {
        List<StudentResponseDTO> students = studentService.searchByName(q);
        return ResponseEntity.ok(ApiResponse.success("Search results", students));
    }

    @GetMapping("/course/{course}")
    @Operation(summary = "Get students by course", description = "Filter all students enrolled in a specific course")
    public ResponseEntity<ApiResponse<List<StudentResponseDTO>>> getStudentsByCourse(
            @PathVariable String course) {
        List<StudentResponseDTO> students = studentService.getStudentsByCourse(course);
        return ResponseEntity.ok(ApiResponse.success("Students in course: " + course, students));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get student statistics", description = "Returns total count, unique courses, and average age")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        Map<String, Object> stats = studentService.getStudentStats();
        return ResponseEntity.ok(ApiResponse.success("Statistics retrieved", stats));
    }

    @PostMapping
    @Operation(summary = "Create new student")
    public ResponseEntity<ApiResponse<StudentResponseDTO>> createStudent(
            @Valid @RequestBody StudentRequestDTO requestDTO) {
        StudentResponseDTO student = studentService.createStudent(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Student created successfully", student));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Full update of student record")
    public ResponseEntity<ApiResponse<StudentResponseDTO>> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentRequestDTO requestDTO) {
        StudentResponseDTO student = studentService.updateStudent(id, requestDTO);
        return ResponseEntity.ok(ApiResponse.success("Student updated successfully", student));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partial update of student record",
            description = "Update only specific fields. Send only the fields you want to change.")
    public ResponseEntity<ApiResponse<StudentResponseDTO>> partialUpdateStudent(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {
        StudentResponseDTO student = studentService.partialUpdateStudent(id, updates);
        return ResponseEntity.ok(ApiResponse.success("Student partially updated", student));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete student by ID")
    public ResponseEntity<ApiResponse<Void>> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.ok(ApiResponse.success("Student deleted successfully", null));
    }
}
