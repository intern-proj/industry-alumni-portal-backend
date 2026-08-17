package com.nsbm.authservice.service;

import com.nsbm.authservice.dto.*;
import com.nsbm.authservice.entity.*;
import com.nsbm.authservice.exception.*;
import com.nsbm.authservice.repository.*;
import com.nsbm.authservice.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final ManagementStaffRepository staffRepository;
    private final PendingStaffRepository pendingStaffRepository;
    private final PendingPartnerRepository pendingPartnerRepository;
    private final IndustryPartnerRepository partnerRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final RabbitTemplate rabbitTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.rabbitmq.exchange:notification.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key:notification.routingkey}")
    private String routingKey;

    @Value("${app.otp.expiration-minutes:5}")
    private long otpExpirationMinutes;

    public AuthService(ManagementStaffRepository staffRepository,
                       PendingStaffRepository pendingStaffRepository,
                       PendingPartnerRepository pendingPartnerRepository,
                       IndustryPartnerRepository partnerRepository,
                       OtpCodeRepository otpCodeRepository,
                       RabbitTemplate rabbitTemplate,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.staffRepository = staffRepository;
        this.pendingStaffRepository = pendingStaffRepository;
        this.pendingPartnerRepository = pendingPartnerRepository;
        this.partnerRepository = partnerRepository;
        this.otpCodeRepository = otpCodeRepository;
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

    @Transactional
    public Step1LoginResponse initiateStaffLogin(LoginRequest request) {
        ManagementStaff staff = staffRepository.findByUsername(request.username())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password."));

        if (!passwordEncoder.matches(request.password(), staff.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid username or password.");
        }

        String otpCode = String.format("%06d", new SecureRandom().nextInt(1000000));
        String sessionToken = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(otpExpirationMinutes);

        OtpCode otpEntity = OtpCode.builder()
                .username(staff.getUsername())
                .code(otpCode)
                .sessionToken(sessionToken)
                .expiresAt(expiresAt)
                .build();
        otpCodeRepository.save(otpEntity);

        EmailNotificationMessage message = new EmailNotificationMessage(
                staff.getEmail(),
                "Your Management Portal Verification Code",
                "Your OTP code is " + otpCode + ". It will expire in " + otpExpirationMinutes + " minutes.",
                "STAFF_OTP"
        );
        sendRabbitNotification(message);

        return new Step1LoginResponse(
                sessionToken,
                staff.getUsername(),
                "OTP verification code sent to registered email.",
                otpExpirationMinutes * 60
        );
    }

    @Transactional
    public AuthResponse verifyStaffOtp(OtpVerificationRequest request) {
        Optional<OtpCode> otpOpt = otpCodeRepository.findTopBySessionTokenAndCodeOrderByCreatedAtDesc(
                request.sessionToken(), request.otpCode());

        if (otpOpt.isEmpty()) {
            otpOpt = otpCodeRepository.findTopByUsernameAndCodeOrderByCreatedAtDesc(
                    request.sessionToken(), request.otpCode());
        }

        OtpCode otpEntity = otpOpt.orElseThrow(() -> new OtpInvalidException("Invalid or expired OTP code."));

        if (otpEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            otpCodeRepository.delete(otpEntity);
            throw new OtpInvalidException("OTP code has expired. Please log in again.");
        }

        ManagementStaff staff = staffRepository.findByUsername(otpEntity.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Associated staff member not found."));

        otpCodeRepository.delete(otpEntity);

        String token = jwtTokenProvider.generateToken(staff.getUsername(), staff.getEmail(), staff.getRole().name(), "MANAGEMENT_STAFF");
        return new AuthResponse(token, staff.getUsername(), staff.getEmail(), staff.getRole().name(), "MANAGEMENT_STAFF");
    }

    public TokenValidationResponse validateToken(String token) {
        if (jwtTokenProvider.validateToken(token)) {
            String username = jwtTokenProvider.getUsernameFromToken(token);
            String email = jwtTokenProvider.getEmailFromToken(token);
            String role = jwtTokenProvider.getRoleFromToken(token);
            String userType = jwtTokenProvider.getUserTypeFromToken(token);
            return new TokenValidationResponse(true, username, email, role, userType);
        }
        return new TokenValidationResponse(false, null, null, null, null);
    }

}