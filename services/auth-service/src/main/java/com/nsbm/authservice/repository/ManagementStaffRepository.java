package com.nsbm.authservice.repository;

import com.nsbm.authservice.entity.ManagementStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ManagementStaffRepository extends JpaRepository<ManagementStaff, Long> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<ManagementStaff> findByUsername(String username);
    Optional<ManagementStaff> findByEmail(String email);
}
