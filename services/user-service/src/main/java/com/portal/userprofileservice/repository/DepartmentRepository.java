package com.portal.userprofileservice.repository;

import com.portal.userprofileservice.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, String> {
    List<Department> findByFacultyFacultyId(String facultyId);
}
