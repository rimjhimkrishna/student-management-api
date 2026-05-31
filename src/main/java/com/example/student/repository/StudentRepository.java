package com.example.student.repository;

import com.example.student.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    List<Student> findByCourseIgnoreCase(String course);

    @Query("SELECT s FROM Student s WHERE " +
            "LOWER(s.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(s.lastName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Student> searchByName(@Param("query") String query);

    @Query("SELECT DISTINCT s.course FROM Student s ORDER BY s.course")
    List<String> findDistinctCourses();

    @Query("SELECT AVG(s.age) FROM Student s")
    Double findAverageAge();
}
