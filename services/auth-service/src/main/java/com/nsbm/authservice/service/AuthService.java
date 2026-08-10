package com.nsbm.authservice.service;

import com.nsbm.authservice.dto.EmailNotificationMessage;
import com.nsbm.authservice.dto.StaffInvitationRequest;
import com.nsbm.authservice.exception.StaffAlreadyExistsException;
import com.nsbm.authservice.entity.PendingStaff;
import com.nsbm.authservice.repository.ManagementStaffRepository;
import com.nsbm.authservice.repository.PendingStaffRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final ManagementStaffRepository staffRepository;
    private final PendingStaffRepository pendingStaffRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key}")
    private String routingKey;

    public AuthService(ManagementStaffRepository staffRepository,
                       PendingStaffRepository pendingStaffRepository,
                       RabbitTemplate rabbitTemplate) {
        this.staffRepository = staffRepository;
        this.pendingStaffRepository = pendingStaffRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public void inviteStaff(StaffInvitationRequest request) {
        // 1. Verify user does not already exist
        //if (staffRepository.existsByEmail(request.email()) || pendingStaffRepository.existsByEmail(request.email())) {
        //    throw new StaffAlreadyExistsException("Staff member with email " + request.email() + " is already registered or invited.");
        //}

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

        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}