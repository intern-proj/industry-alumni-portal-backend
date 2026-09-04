package com.nsbm.authservice.service;

import com.nsbm.authservice.dto.ApplyPartnerRegistrationRequest;
import com.nsbm.authservice.dto.AuthResponse;
import com.nsbm.authservice.dto.CompletePartnerRegistrationRequest;
import com.nsbm.authservice.dto.CompleteStaffRegistrationRequest;
import com.nsbm.authservice.dto.CreateAdminRequest;
import com.nsbm.authservice.dto.ForgotPasswordRequest;
import com.nsbm.authservice.dto.LoginRequest;
import com.nsbm.authservice.dto.LoginResponse;
import com.nsbm.authservice.dto.OtpEmailPayload;
import com.nsbm.authservice.dto.OtpVerificationRequest;
import com.nsbm.authservice.dto.ResetPasswordRequest;
import com.nsbm.authservice.dto.StaffInvitationRequest;
import com.nsbm.authservice.dto.Step1LoginResponse;
import com.nsbm.authservice.dto.TokenValidationResponse;
import com.nsbm.notification_service.dto.UpdateEmailDTO;
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
    private final StudentRepository studentRepository;
    private final IndustryPartnerRepository partnerRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RabbitTemplate rabbitTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.rabbitmq.exchange:notification.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key:notification.routingkey}")
    private String routingKey;

    @Value("${app.otp.expiration-minutes:5}")
    private long otpExpirationMinutes;

    @Value("${app.reset-password.expiration-minutes:15}")
    private long resetPasswordExpirationMinutes;

    @Value("${app.reset-password.frontend-url:http://localhost:3000/reset-password}")
    private String resetPasswordFrontendUrl;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    public AuthService(ManagementStaffRepository staffRepository,
                       PendingStaffRepository pendingStaffRepository,
                       PendingPartnerRepository pendingPartnerRepository,
                       StudentRepository studentRepository,
                       IndustryPartnerRepository partnerRepository,
                       OtpCodeRepository otpCodeRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       RabbitTemplate rabbitTemplate,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.staffRepository = staffRepository;
        this.pendingStaffRepository = pendingStaffRepository;
        this.pendingPartnerRepository = pendingPartnerRepository;
        this.studentRepository = studentRepository;
        this.partnerRepository = partnerRepository;
        this.otpCodeRepository = otpCodeRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public void createAdmin(CreateAdminRequest request) {
        if (staffRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException("Username '" + request.username() + "' is already taken.");
        }
        if (staffRepository.existsByEmail(request.email())) {
            throw new StaffAlreadyExistsException("Administrator with email " + request.email() + " already exists.");
        }

        ManagementStaff admin = ManagementStaff.builder()
                .username(request.username())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.SYSTEM_ADMIN)
                .build();

        staffRepository.save(admin);
        log.info("Successfully created new Administrator account: {}", request.username());
    }

    @Transactional
    public void deleteUser(String identifier) {
        boolean deleted = false;
        
        // 1. Delete from staffRepository (active credentials)
        if (staffRepository.existsByUsername(identifier)) {
            staffRepository.deleteByUsername(identifier);
            deleted = true;
        } else if (staffRepository.existsByEmail(identifier)) {
            staffRepository.deleteByEmail(identifier);
            deleted = true;
        }

        // 2. Delete from pendingStaffRepository (pending invitations)
        if (pendingStaffRepository.existsByEmail(identifier)) {
            pendingStaffRepository.deleteByEmail(identifier);
            deleted = true;
        }

        if (deleted) {
            log.info("Deleted auth credentials / invitations for identifier: {}", identifier);
        } else {
            log.warn("No auth records found to delete for identifier: {}", identifier);
        }
    }

    @Transactional
    public void inviteStaff(StaffInvitationRequest request) {
        // 1. Verify user does not already exist as registered staff
        if (staffRepository.existsByEmail(request.email())) {
            throw new StaffAlreadyExistsException("A staff account with email " + request.email() + " already exists.");
        }
        // 2. If there's an existing pending invite, delete it first (allows re-invite after revoke)
        if (pendingStaffRepository.existsByEmail(request.email())) {
            pendingStaffRepository.deleteByEmail(request.email());
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
        String invitationUrl = frontendUrl + "/staff/complete-registration?token=" + token;
        UpdateEmailDTO message = new UpdateEmailDTO(
                request.email(),
                request.email(),
                "GENERAL_UPDATE",
                "You have been invited as a " + request.role() + ". Complete registration here: " + invitationUrl,
                invitationUrl
        );

        sendRabbitNotification(message);
    }

    @Transactional
    public void revokeStaffInvitation(String email) {
        // Always attempt delete — the @Modifying query is idempotent
        pendingStaffRepository.deleteByEmail(email);
        log.info("Revoked pending staff invitation for email: {}", email);
    }

    private void sendRabbitNotification(UpdateEmailDTO message) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            log.info("Published notification message to RabbitMQ: {}", message.updateType());
        } catch (Exception e) {
            log.error("Failed to send message to RabbitMQ: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public String completeStaffRegistration(CompleteStaffRegistrationRequest request) {
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

        return managementStaff.getEmail();
    }

    @Transactional
    public void createPendingPartner(ApplyPartnerRegistrationRequest request) {
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
        UpdateEmailDTO message = new UpdateEmailDTO(
                request.email(),
                request.companyName(),
                "GENERAL_UPDATE",
                "Your partner registration application has been received and is pending admin approval.",
                frontendUrl + "/login"
        );
        sendRabbitNotification(message);
    }

    @Transactional(readOnly = true)
    public java.util.List<PendingPartner> getAllPendingPartners() {
        return pendingPartnerRepository.findAll();
    }

    @Transactional
    public void approvePendingPartner(Long id) {
        PendingPartner pendingPartner = pendingPartnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pending partner not found"));
        
        String invitationUrl = frontendUrl + "/partner/complete-registration?token=" + pendingPartner.getRegistrationToken();
        UpdateEmailDTO message = new UpdateEmailDTO(
                pendingPartner.getEmail(),
                pendingPartner.getCompanyName(),
                "GENERAL_UPDATE",
                "Your registration request has been approved. Please complete your registration here: " + invitationUrl,
                invitationUrl
        );
        sendRabbitNotification(message);
    }

    @Transactional
    public void rejectPendingPartner(Long id) {
        PendingPartner pendingPartner = pendingPartnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pending partner not found"));
        
        UpdateEmailDTO message = new UpdateEmailDTO(
                pendingPartner.getEmail(),
                pendingPartner.getCompanyName(),
                "GENERAL_UPDATE",
                "We regret to inform you that your partner registration application has been rejected.",
                frontendUrl
        );
        sendRabbitNotification(message);
        pendingPartnerRepository.delete(pendingPartner);
    }

    @Transactional(readOnly = true)
    public java.util.List<IndustryPartner> getAllIndustryPartners() {
        return partnerRepository.findAll();
    }

    @Transactional
    public void toggleIndustryPartnerStatus(Long id) {
        IndustryPartner partner = partnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Industry partner not found"));
        // Assuming we will add accountStatus to IndustryPartner
        if ("INACTIVE".equals(partner.getAccountStatus())) {
            partner.setAccountStatus("ACTIVE");
        } else {
            partner.setAccountStatus("INACTIVE");
        }
        partnerRepository.save(partner);
    }

    @Transactional
    public void deleteIndustryPartner(Long id) {
        IndustryPartner partner = partnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Industry partner not found"));
        partnerRepository.delete(partner);
    }

    @Transactional
    public String completePartnerRegistration(CompletePartnerRegistrationRequest request) {
        PendingPartner pendingPartner = pendingPartnerRepository.findByRegistrationToken(request.registrationToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired registration token."));
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
        partnerRepository.save(partner);
        pendingPartnerRepository.delete(pendingPartner);
        return partner.getEmail();
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String identifier = request.username() != null ? request.username().trim() : "";

        // 1. Check Student login (Direct single-step authentication)
        Optional<Student> studentOpt = studentRepository.findByUsername(identifier);
        if (studentOpt.isEmpty()) {
            studentOpt = studentRepository.findByEmail(identifier);
        }
        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();
            if (passwordEncoder.matches(request.password(), student.getPasswordHash())) {
                String token = jwtTokenProvider.generateToken(student.getUsername(), student.getEmail(), Role.STUDENT.name(), "STUDENT");
                return LoginResponse.direct(new AuthResponse(token, student.getUsername(), student.getEmail(), Role.STUDENT.name(), "STUDENT"));
            }
            throw new InvalidCredentialsException("Invalid username or password.");
        }

        // 2. Check Management Staff login (Triggers 2FA OTP for academic & management staff)
        Optional<ManagementStaff> staffOpt = staffRepository.findByUsername(identifier);
        if (staffOpt.isEmpty()) {
            staffOpt = staffRepository.findByEmail(identifier);
        }
        if (staffOpt.isPresent()) {
            ManagementStaff staff = staffOpt.get();
            if (staff.getRole() == Role.SYSTEM_ADMIN) {
                throw new InvalidCredentialsException("Administrators must log in via the dedicated Administrator Portal.");
            }
            if (passwordEncoder.matches(request.password(), staff.getPasswordHash())) {
                Step1LoginResponse step1 = generateAndSendOtp(staff.getUsername(), staff.getEmail());
                return LoginResponse.otpRequired(step1.sessionToken(), staff.getUsername(), "A 6-digit verification code has been sent to your registered email.", otpExpirationMinutes * 60L);
            }
            throw new InvalidCredentialsException("Invalid username or password.");
        }

        // 3. Check Industry Partner login (Triggers 2FA OTP)
        Optional<IndustryPartner> partnerOpt = partnerRepository.findByUsername(identifier);
        if (partnerOpt.isEmpty()) {
            partnerOpt = partnerRepository.findByEmail(identifier);
        }
        if (partnerOpt.isPresent()) {
            IndustryPartner partner = partnerOpt.get();
            if (passwordEncoder.matches(request.password(), partner.getPasswordHash())) {
                Step1LoginResponse step1 = generateAndSendOtp(partner.getUsername(), partner.getEmail());
                return LoginResponse.otpRequired(step1.sessionToken(), partner.getUsername(), "A 6-digit verification code has been sent to your registered email.", otpExpirationMinutes * 60L);
            }
            throw new InvalidCredentialsException("Invalid username or password.");
        }

        throw new InvalidCredentialsException("Invalid username or password.");
    }



    @Transactional
    public Step1LoginResponse initiateAdminLogin(LoginRequest request) {
        String identifier = request.username() != null ? request.username().trim() : "";
        ManagementStaff staff = staffRepository.findByUsername(identifier)
                .or(() -> staffRepository.findByEmail(identifier))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password."));

        if (staff.getRole() != Role.SYSTEM_ADMIN) {
            throw new InvalidCredentialsException("Invalid username or password.");
        }

        if (!passwordEncoder.matches(request.password(), staff.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid username or password.");
        }

        return generateAndSendOtp(staff.getUsername(), staff.getEmail());
    }

    @Transactional
    public Step1LoginResponse initiateStaffLogin(LoginRequest request) {
        return initiateAdminLogin(request);
    }

    private Step1LoginResponse generateAndSendOtp(String username, String email) {
        String otpCode = String.format("%06d", new SecureRandom().nextInt(1000000));
        String sessionToken = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(otpExpirationMinutes);

        OtpCode otpEntity = OtpCode.builder()
                .username(username)
                .code(otpCode)
                .sessionToken(sessionToken)
                .expiresAt(expiresAt)
                .build();
        otpCodeRepository.save(otpEntity);

        OtpEmailPayload otpPayload = new OtpEmailPayload(
                email,
                otpCode
        );
        try {
            rabbitTemplate.convertAndSend(exchange, "notification.otp", otpPayload);
            log.info("Published OTP notification message to RabbitMQ for user {}", username);
        } catch (Exception e) {
            log.error("Failed to send OTP message to RabbitMQ: {}", e.getMessage(), e);
        }

        return new Step1LoginResponse(
                sessionToken,
                username,
                "OTP verification code sent to registered email.",
                otpExpirationMinutes * 60L
        );
    }

    @Transactional
    public AuthResponse verifyOtp(OtpVerificationRequest request) {
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

        String username = otpEntity.getUsername();
        otpCodeRepository.delete(otpEntity);

        // 1. Check if user is ManagementStaff (Admin / Academic Staff)
        Optional<ManagementStaff> staffOpt = staffRepository.findByUsername(username);
        if (staffOpt.isPresent()) {
            ManagementStaff staff = staffOpt.get();
            String token = jwtTokenProvider.generateToken(staff.getUsername(), staff.getEmail(), staff.getRole().name(), "MANAGEMENT_STAFF");
            return new AuthResponse(token, staff.getUsername(), staff.getEmail(), staff.getRole().name(), "MANAGEMENT_STAFF");
        }

        // 2. Check if user is IndustryPartner
        Optional<IndustryPartner> partnerOpt = partnerRepository.findByUsername(username);
        if (partnerOpt.isPresent()) {
            IndustryPartner partner = partnerOpt.get();
            String token = jwtTokenProvider.generateToken(partner.getUsername(), partner.getEmail(), Role.INDUSTRY_PARTNER.name(), "INDUSTRY_PARTNER");
            return new AuthResponse(token, partner.getUsername(), partner.getEmail(), Role.INDUSTRY_PARTNER.name(), "INDUSTRY_PARTNER");
        }

        throw new InvalidCredentialsException("Associated user account not found.");
    }

    @Transactional
    public AuthResponse verifyStaffOtp(OtpVerificationRequest request) {
        return verifyOtp(request);
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

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.email();

        // 1. Check if email belongs to a Student -> Reject
        Optional<Student> studentOpt = studentRepository.findByEmail(email);
        if (studentOpt.isPresent()) {
            throw new IllegalArgumentException("Forgot password feature is not available for students.");
        }

        // 2. Check if email belongs to ManagementStaff
        Optional<ManagementStaff> staffOpt = staffRepository.findByEmail(email);
        String userType = null;
        if (staffOpt.isPresent()) {
            ManagementStaff staff = staffOpt.get();
            if (staff.getRole() == Role.SYSTEM_ADMIN) {
                throw new IllegalArgumentException("Forgot password feature is not available for admins.");
            }
            userType = "MANAGEMENT_STAFF";
        } else {
            // 3. Check if email belongs to IndustryPartner
            Optional<IndustryPartner> partnerOpt = partnerRepository.findByEmail(email);
            if (partnerOpt.isPresent()) {
                userType = "INDUSTRY_PARTNER";
            }
        }

        if (userType == null) {
            throw new InvalidCredentialsException("No user account found with the provided email address.");
        }

        // 4. Invalidate any existing reset tokens for this email
        passwordResetTokenRepository.deleteByEmail(email);

        // 5. Generate reset token and set expiration
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(resetPasswordExpirationMinutes);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .email(email)
                .token(token)
                .userType(userType)
                .expiresAt(expiresAt)
                .build();
        passwordResetTokenRepository.save(resetToken);

        // 6. Build reset link and send via RabbitMQ
        String resetUrl = resetPasswordFrontendUrl + "?token=" + token;
        UpdateEmailDTO message = new UpdateEmailDTO(
                email,
                email,
                "GENERAL_UPDATE",
                "You requested a password reset. Please click the link to set a new password: " + resetUrl + "\nThis link will expire in " + resetPasswordExpirationMinutes + " minutes.",
                resetUrl
        );
        sendRabbitNotification(message);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired password reset token."));

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            passwordResetTokenRepository.delete(resetToken);
            throw new InvalidTokenException("Password reset token has expired.");
        }

        String email = resetToken.getEmail();
        String userType = resetToken.getUserType();

        if ("MANAGEMENT_STAFF".equals(userType)) {
            ManagementStaff staff = staffRepository.findByEmail(email)
                    .orElseThrow(() -> new InvalidCredentialsException("Staff account not found."));
            if (staff.getRole() == Role.SYSTEM_ADMIN) {
                throw new IllegalArgumentException("Forgot password feature is not available for admins.");
            }
            staff.setPasswordHash(passwordEncoder.encode(request.newPassword()));
            staffRepository.save(staff);
        } else if ("INDUSTRY_PARTNER".equals(userType)) {
            IndustryPartner partner = partnerRepository.findByEmail(email)
                    .orElseThrow(() -> new InvalidCredentialsException("Industry partner account not found."));
            partner.setPasswordHash(passwordEncoder.encode(request.newPassword()));
            partnerRepository.save(partner);
        } else {
            throw new InvalidTokenException("Invalid user type in reset token.");
        }

        passwordResetTokenRepository.delete(resetToken);
    }

}
