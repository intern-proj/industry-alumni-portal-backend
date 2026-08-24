package com.portal.userprofileservice.repository;

import com.portal.userprofileservice.model.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FacultyRepository extends JpaRepository<Faculty, String> {
    Optional<Faculty> findByName(String name);
    Optional<Faculty> findByCode(String code);
    boolean existsByName(String name);
}
