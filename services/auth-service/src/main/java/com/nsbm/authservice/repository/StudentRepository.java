package com.nsbm.authservice.repository;

import com.nsbm.authservice.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
