package com.nsbm.authservice.service;

import com.nsbm.authservice.dto.*;
import com.nsbm.authservice.entity.ManagementStaff;
import com.nsbm.authservice.exception.InvalidTokenException;
import com.nsbm.authservice.entity.PendingStaff;
import com.nsbm.authservice.exception.StaffAlreadyExistsException;
import com.nsbm.authservice.exception.UsernameAlreadyExistsException;
import com.nsbm.authservice.repository.ManagementStaffRepository;
import com.nsbm.authservice.repository.PendingStaffRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final ManagementStaffRepository staffRepository;
    private final PendingStaffRepository pendingStaffRepository;
    //private final PendingPartnerRepository pendingPartnerRepository;
    //private final IndustryPartnerRepository partnerRepository;
    private final RabbitTemplate rabbitTemplate;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.rabbitmq.exchange:notification.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key:notification.routingkey}")
    private String routingKey;

    public AuthService(ManagementStaffRepository staffRepository,
                       PendingStaffRepository pendingStaffRepository,
                       //PendingPartnerRepository pendingPartnerRepository,
                       //IndustryPartnerRepository partnerRepository,
                       RabbitTemplate rabbitTemplate,
                       PasswordEncoder passwordEncoder) {
        this.staffRepository = staffRepository;
        this.pendingStaffRepository = pendingStaffRepository;
        //this.pendingPartnerRepository = pendingPartnerRepository;
        //this.partnerRepository = partnerRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void inviteStaff(StaffInvitationRequest request) {
        // 1. Verify user does not already exist
        if (staffRepository.existsByEmail(request.email()) || pendingStaffRepository.existsByEmail(request.email())) {
            throw new StaffAlreadyExistsException("Staff member with email " + request.email() + " is already registered or invited.");
        }

        // 2. Generate unique registration token
        String token = UUID.randomUUID().toString();

        // 3. Save to pending_staff table
        PendingStaff pendingStaff = PendingStaff.builder()
                .email(request.email())
                .role(request.role())
                .invitationToken(token)
                .build();
        pendingStaffRepository.save(pendingStaff);

        // 4. Publish message to RabbitMQ for Notification Service
        String invitationUrl = "https://portal.domain.com/complete-registration?token=" + token;
        EmailNotificationMessage message = new EmailNotificationMessage(
                request.email(),
                "Portal Staff Registration Invitation",
                "You have been invited as a " + request.role() + ". Complete registration here: " + invitationUrl,
                "STAFF_INVITATION"
        );

        sendRabbitNotification(message);
    }

    private void sendRabbitNotification(EmailNotificationMessage message) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            log.info("Published notification message to RabbitMQ: {}", message.eventType());
        } catch (Exception e) {
            log.error("Failed to send message to RabbitMQ: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void completeStaffRegistration(CompleteStaffRegistrationRequest request) {
        // 1. Fetch pending registration record using invitation token
        PendingStaff pendingStaff = pendingStaffRepository.findByInvitationToken(request.invitationToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired invitation token."));

        // 2. Ensure chosen username is globally unique
        if (staffRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException("Username '" + request.username() + "' is already taken.");
        }

        // 3. Create permanent ManagementStaff entity
        ManagementStaff managementStaff = ManagementStaff.builder()
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .email(pendingStaff.getEmail())
                .role(pendingStaff.getRole())
                .build();

        // 4. Save to management_staff table
        staffRepository.save(managementStaff);

        // 5. Remove record from pending_staff staging table
        pendingStaffRepository.delete(pendingStaff);
    }




}