package com.nsbm.authservice.service;

import com.nsbm.authservice.dto.*;
import com.nsbm.authservice.entity.*;
import com.nsbm.authservice.exception.InvalidCredentialsException;
import com.nsbm.authservice.exception.InvalidTokenException;
import com.nsbm.authservice.exception.StaffAlreadyExistsException;
import com.nsbm.authservice.exception.UsernameAlreadyExistsException;
import com.nsbm.authservice.repository.IndustryPartnerRepository;
import com.nsbm.authservice.repository.ManagementStaffRepository;
import com.nsbm.authservice.repository.PendingPartnerRepository;
import com.nsbm.authservice.repository.PendingStaffRepository;
import com.nsbm.authservice.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final ManagementStaffRepository staffRepository;
    private final PendingStaffRepository pendingStaffRepository;
    private final PendingPartnerRepository pendingPartnerRepository;
    private final IndustryPartnerRepository partnerRepository;
    private final RabbitTemplate rabbitTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.rabbitmq.exchange:notification.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key:notification.routingkey}")
    private String routingKey;

    public AuthService(ManagementStaffRepository staffRepository,
                       PendingStaffRepository pendingStaffRepository,
                       PendingPartnerRepository pendingPartnerRepository,
                       IndustryPartnerRepository partnerRepository,
                       RabbitTemplate rabbitTemplate,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.staffRepository = staffRepository;
        this.pendingStaffRepository = pendingStaffRepository;
        this.pendingPartnerRepository = pendingPartnerRepository;
        this.partnerRepository = partnerRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
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

    @Transactional
    public void createPendingPartner(ApplyPartnerRegistrationRequest request) {
        // Use injected instance 'partnerRepository' instead of class 'IndustryPartnerRepository'
        if (IndustryPartnerRepository.existsByEmail(request.email()) || pendingPartnerRepository.existsByEmail(request.email())) {
            throw new StaffAlreadyExistsException("Partner with email " + request.email() + " is already registered or invited.");
        }
        String token = UUID.randomUUID().toString();
        PendingPartner pendingPartner = PendingPartner.builder()
                .representativeFullName(request.representativeFullName())
                .email(request.email())
                .phone(request.phone())
                .representativeJobRole(request.representativeJobRole())
                .companyName(request.companyName())
                .companyIndustry(request.companyIndustry())
                .companyAddress(request.companyAddress())
                .companyDescription(request.companyDescription())
                .registrationToken(token)
                .build();
        pendingPartnerRepository.save(pendingPartner);
        String invitationUrl = "https://portal.domain.com/complete-partner-registration?token=" + token;
        EmailNotificationMessage message = new EmailNotificationMessage(
                request.email(),
                "Industry Partner Registration Link",
                "Your registration request has been approved. Please complete your registration here: " + invitationUrl,
                "PARTNER_REGISTRATION"
        );
        sendRabbitNotification(message);
    }

    @Transactional
    public void completePartnerRegistration(CompletePartnerRegistrationRequest request) {
        PendingPartner pendingPartner = pendingPartnerRepository.findByRegistrationToken(request.registrationToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired registration token."));
        // Use injected instance 'partnerRepository' instead of class 'IndustryPartnerRepository'
        if (IndustryPartnerRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException("Username '" + request.username() + "' is already taken.");
        }
        IndustryPartner partner = IndustryPartner.builder()
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .representativeFullName(pendingPartner.getRepresentativeFullName())
                .email(pendingPartner.getEmail())
                .phone(pendingPartner.getPhone())
                .representativeJobRole(pendingPartner.getRepresentativeJobRole())
                .companyName(pendingPartner.getCompanyName())
                .companyIndustry(pendingPartner.getCompanyIndustry())
                .companyAddress(pendingPartner.getCompanyAddress())
                .companyDescription(pendingPartner.getCompanyDescription())
                .build();
        // Use injected instance 'partnerRepository' instead of class 'IndustryPartnerRepository'
        partnerRepository.save(partner);
        pendingPartnerRepository.delete(pendingPartner);
    }

    @Transactional(readOnly = true)
    public AuthResponse loginStudentOrPartner(LoginRequest request) {
        // Check Student login
        Optional<Student> studentOpt = studentRepository.findByUsername(request.username());
        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();
            if (passwordEncoder.matches(request.password(), student.getPasswordHash())) {
                String token = jwtTokenProvider.generateToken(student.getUsername(), student.getEmail(), Role.STUDENT.name(), "STUDENT");
                return new AuthResponse(token, student.getUsername(), student.getEmail(), Role.STUDENT.name(), "STUDENT");
            }
        }

        // Check Industry Partner login
        Optional<IndustryPartner> partnerOpt = partnerRepository.findByUsername(request.username());
        if (partnerOpt.isPresent()) {
            IndustryPartner partner = partnerOpt.get();
            if (passwordEncoder.matches(request.password(), partner.getPasswordHash())) {
                String token = jwtTokenProvider.generateToken(partner.getUsername(), partner.getEmail(), Role.INDUSTRY_PARTNER.name(), "INDUSTRY_PARTNER");
                return new AuthResponse(token, partner.getUsername(), partner.getEmail(), Role.INDUSTRY_PARTNER.name(), "INDUSTRY_PARTNER");
            }
        }

        throw new InvalidCredentialsException("Invalid username or password.");
    }

}