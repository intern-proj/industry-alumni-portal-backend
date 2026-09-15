package com.nsbm.notification_service.repository;

import com.nsbm.notification_service.model.SmtpConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SmtpConfigurationRepository extends JpaRepository<SmtpConfiguration, Long> {

    Optional<SmtpConfiguration> findFirstByIsActiveTrueOrderByIdDesc();
}
