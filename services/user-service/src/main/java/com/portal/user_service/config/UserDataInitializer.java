package com.portal.user_service.config;

import com.portal.user_service.model.*;
import com.portal.user_service.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class UserDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UserDataInitializer.class);

    private final UserProfileRepository userProfileRepository;
    private final AcademicRecordRepository academicRecordRepository;
    private final SkillRepository skillRepository;
    private final JobPreferenceRepository jobPreferenceRepository;
    private final ResumeRepository resumeRepository;

    @Value("${app.admin.default-username:admin}")
    private String adminUsername;

    @Value("${app.admin.default-email:prasadkvithana@gmail.com}")
    private String adminEmail;

    public UserDataInitializer(UserProfileRepository userProfileRepository,
                               AcademicRecordRepository academicRecordRepository,
                               SkillRepository skillRepository,
                               JobPreferenceRepository jobPreferenceRepository,
                               ResumeRepository resumeRepository) {
        this.userProfileRepository = userProfileRepository;
        this.academicRecordRepository = academicRecordRepository;
        this.skillRepository = skillRepository;
        this.jobPreferenceRepository = jobPreferenceRepository;
        this.resumeRepository = resumeRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Starting UserDataInitializer...");

        // 1. Initialize Admin Profile if missing
        if (!userProfileRepository.existsById(adminUsername)) {
            UserProfile adminProfile = UserProfile.builder()
                    .userId(adminUsername)
                    .firstName("System")
                    .lastName("Admin")
                    .email(adminEmail)
                    .userRole(UserRole.SYSTEM_ADMIN)
                    .accountStatus(AccountStatus.ACTIVE)
                    .profilePicUrl("https://ui-avatars.com/api/?name=System+Admin&background=0D8ABC&color=fff")
                    .bio("Default System Administrator account.")
                    .build();
            userProfileRepository.save(adminProfile);
            log.info("Admin UserProfile created successfully.");
        }

        // 2. Check if student data already exists in the database
        if (userProfileRepository.existsById("student1") || userProfileRepository.existsByUserRole(UserRole.STUDENT)) {
            log.info("Student data already present in database. Skipping student initialization to preserve existing records.");
            return;
        }

        // 3. Seed student profiles only if completely missing (fresh database setup)
        log.info("No existing student profiles detected. Seeding default student profiles...");
        seedComprehensiveStudents();
        log.info("Student profiles, academic records, skills, and preferences initialized successfully!");
    }

    private void seedComprehensiveStudents() {
        // --- Student 1: John Smith ---
        seedStudent(
                "student1", "John", "Smith", "student1@students.nsbm.ac.lk", "+94 77 123 4501",
                "Full-Stack Software Engineer | React, Spring Boot & Cloud Architectures",
                "Passionate software engineering undergraduate specializing in reactive web applications, distributed microservices, and containerized cloud deployments. Active open-source contributor and hackathon enthusiast.",
                "https://linkedin.com/in/johnsmith-se", "https://github.com/johnsmith-dev",
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=400",
                "Faculty of Computing", "Department of Software Engineering", "BSc (Hons) in Software Engineering",
                3.82, "Full-Stack Software Engineer",
                List.of("Java", "Spring Boot", "React", "TypeScript", "PostgreSQL", "Docker", "REST APIs"),
                "[{\"title\":\"Enterprise Microservices E-Commerce Platform\",\"description\":\"Architected a resilient event-driven e-commerce platform using Spring Cloud, RabbitMQ, PostgreSQL, and React.\",\"tech_stack\":[\"Java\",\"Spring Boot\",\"React\",\"RabbitMQ\",\"PostgreSQL\",\"Docker\"]},{\"title\":\"Real-Time Collaborative Code Workspace\",\"description\":\"Constructed a browser-based collaborative programming IDE with WebSockets, OT synchronization, and Redis.\",\"tech_stack\":[\"React\",\"TypeScript\",\"Node.js\",\"WebSocket\",\"Redis\",\"Tailwind CSS\"]}]"
        );

        // --- Student 2: Emma Johnson ---
        seedStudent(
                "student2", "Emma", "Johnson", "student2@students.nsbm.ac.lk", "+94 77 123 4502",
                "Cloud & DevOps Specialist | Kubernetes, Docker, CI/CD & AWS",
                "DevOps and cloud engineer focused on building automated CI/CD deployment pipelines, declarative Kubernetes orchestrations, and infrastructure-as-code using Terraform.",
                "https://linkedin.com/in/emma-johnson-cloud", "https://github.com/emma-devops",
                "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&q=80&w=400",
                "Faculty of Computing", "Department of Network Engineering", "BSc (Hons) in Computer Networks",
                3.75, "Cloud / DevOps Engineer",
                List.of("Docker", "Kubernetes", "AWS", "Terraform", "Linux", "GitHub Actions", "Python"),
                "[{\"title\":\"Automated Multi-Cluster Kubernetes Platform\",\"description\":\"Engineered GitOps delivery workflows using ArgoCD, Helm charts, and automated container security scans.\",\"tech_stack\":[\"Kubernetes\",\"Terraform\",\"Docker\",\"AWS\",\"GitHub Actions\"]},{\"title\":\"Zero-Trust Service Mesh & Observability Suite\",\"description\":\"Implemented Istio service mesh mTLS encryption with Prometheus metrics gathering and Grafana alerting.\",\"tech_stack\":[\"Istio\",\"Linux\",\"Prometheus\",\"Grafana\",\"Bash\"]}]"
        );

        // --- Student 3: Michael Brown ---
        seedStudent(
                "student3", "Michael", "Brown", "student3@students.nsbm.ac.lk", "+94 77 123 4503",
                "Backend Systems & Distributed Architecture Engineer | Java & Go",
                "High-performance backend developer with deep experience in asynchronous event streaming, SQL performance tuning, and resilient distributed microservices architectures.",
                "https://linkedin.com/in/michael-brown-backend", "https://github.com/mbrown-systems",
                "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=400",
                "Faculty of Computing", "Department of Software Engineering", "BSc (Hons) in Software Engineering",
                3.91, "Backend Software Engineer",
                List.of("Java", "Spring Boot", "Go", "Apache Kafka", "PostgreSQL", "Redis", "gRPC"),
                "[{\"title\":\"High-Throughput Financial Transaction Ledger\",\"description\":\"Designed an event-sourced transaction processor handling 10k ops/sec with Apache Kafka and strict ACID guarantees.\",\"tech_stack\":[\"Java\",\"Spring Boot\",\"Kafka\",\"PostgreSQL\",\"Docker\"]},{\"title\":\"Distributed Rate Limiter & API Gateway Plugin\",\"description\":\"Built sliding-window rate limiters in Go backed by Redis clusters for ultra-low latency API traffic shaping.\",\"tech_stack\":[\"Go\",\"Redis\",\"gRPC\",\"Docker\"]}]"
        );

        // --- Student 4: Sophia Taylor ---
        seedStudent(
                "student4", "Sophia", "Taylor", "student4@students.nsbm.ac.lk", "+94 77 123 4504",
                "Frontend Engineer & UI/UX Craftsman | React, Next.js & Modern Web",
                "Creative frontend engineer committed to crafting pixel-perfect, accessible, and high-performance digital experiences. Enthusiastic about micro-interactions and atomic design systems.",
                "https://linkedin.com/in/sophia-taylor-ui", "https://github.com/sophia-frontend",
                "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=400",
                "Faculty of Computing", "Department of Software Engineering", "BSc (Hons) in Multimedia & Web",
                3.84, "Frontend Engineer",
                List.of("React", "TypeScript", "Next.js", "Tailwind CSS", "Redux", "Figma", "REST APIs"),
                "[{\"title\":\"Accessible Healthcare Telemedicine Portal\",\"description\":\"Constructed a WCAG 2.1 AA compliant telehealth platform with appointment scheduling and WebRTC video integration.\",\"tech_stack\":[\"React\",\"Next.js\",\"TypeScript\",\"Tailwind CSS\",\"WebRTC\"]},{\"title\":\"Interactive Financial Analytics Dashboard\",\"description\":\"Responsive financial analytics dashboard featuring dynamic charting, theme switching, and real-time data sync.\",\"tech_stack\":[\"React\",\"TypeScript\",\"Tailwind CSS\",\"REST APIs\"]}]"
        );

        // --- Student 5: William Anderson ---
        seedStudent(
                "student5", "William", "Anderson", "student5@students.nsbm.ac.lk", "+94 77 123 4505",
                "AI/ML Engineer & Data Scientist | Python, PyTorch & LLM Fine-Tuning",
                "Applied machine learning researcher and engineer applying modern natural language processing and computer vision models to enterprise automation challenges.",
                "https://linkedin.com/in/william-anderson-ai", "https://github.com/wanderson-ml",
                "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&q=80&w=400",
                "Faculty of Computing", "Department of Data Science", "BSc (Hons) in Data Science",
                3.88, "AI/ML Engineer",
                List.of("Python", "PyTorch", "FastAPI", "Pandas", "Scikit-Learn", "Docker", "PostgreSQL"),
                "[{\"title\":\"Automated Resume Intelligence Extraction Engine\",\"description\":\"Built hybrid OCR and LLM fine-tuning pipeline extracting skills and experience from unstructured PDF resumes with 96% accuracy.\",\"tech_stack\":[\"Python\",\"FastAPI\",\"PyTorch\",\"Docker\",\"PostgreSQL\"]},{\"title\":\"Industrial Predictive Equipment Maintenance Model\",\"description\":\"Trained recurrent neural network predicting industrial hardware failures from IoT telemetry time-series streams.\",\"tech_stack\":[\"Python\",\"Pandas\",\"Scikit-Learn\",\"FastAPI\"]}]"
        );

        // --- Student 6: Olivia Thomas ---
        seedStudent(
                "student6", "Olivia", "Thomas", "student6@students.nsbm.ac.lk", "+94 77 123 4506",
                "Mobile App Developer | React Native, Kotlin & Flutter Specialist",
                "Mobile engineer dedicated to building silky-smooth 60fps applications, offline-first data synchronization, and intuitive cross-platform user experiences.",
                "https://linkedin.com/in/olivia-thomas-mobile", "https://github.com/olivia-mobile",
                "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&q=80&w=400",
                "Faculty of Computing", "Department of Software Engineering", "BSc (Hons) in Software Engineering",
                3.68, "Mobile Application Developer",
                List.of("React Native", "Kotlin", "Flutter", "TypeScript", "Firebase", "Redux", "REST APIs"),
                "[{\"title\":\"Smart Campus Guide & Interactive Event App\",\"description\":\"Built a cross-platform mobile navigation app with beacon indoor positioning and instant event QR check-ins.\",\"tech_stack\":[\"React Native\",\"TypeScript\",\"Firebase\",\"Redux\",\"REST APIs\"]},{\"title\":\"Offline-First Personal Budgeting Application\",\"description\":\"Native Android personal budgeting client with biometric authentication, SQLite local cache, and cloud sync.\",\"tech_stack\":[\"Kotlin\",\"Android\",\"Firebase\",\"REST APIs\"]}]"
        );

        // --- Student 7: James Jackson ---
        seedStudent(
                "student7", "James", "Jackson", "student7@students.nsbm.ac.lk", "+94 77 123 4507",
                "Cybersecurity & Network Systems Engineer | Ethical Hacking & SecOps",
                "Cybersecurity enthusiast specializing in defensive architecture, penetration testing, automated static analysis (SAST), and secure development lifecycles.",
                "https://linkedin.com/in/james-jackson-sec", "https://github.com/jjackson-sec",
                "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?auto=format&fit=crop&q=80&w=400",
                "Faculty of Computing", "Department of Network Engineering", "BSc (Hons) in Computer Security",
                3.70, "Cybersecurity Analyst / Engineer",
                List.of("Linux", "Python", "Docker", "Wireshark", "Bash", "Penetration Testing", "Security"),
                "[{\"title\":\"Automated Pull-Request Vulnerability Scanner\",\"description\":\"Constructed an automated static application security testing analyzer checking for OWASP Top 10 vulnerabilities.\",\"tech_stack\":[\"Python\",\"Docker\",\"Linux\",\"Bash\"]},{\"title\":\"Distributed Honeynet Threat Intelligence System\",\"description\":\"Deployed simulated vulnerable micro-services recording attacker attack vectors and cataloging IOC patterns.\",\"tech_stack\":[\"Linux\",\"Docker\",\"Python\",\"Wireshark\"]}]"
        );

        // --- Student 8: Ava White ---
        seedStudent(
                "student8", "Ava", "White", "student8@students.nsbm.ac.lk", "+94 77 123 4508",
                "Full-Stack Enterprise Solutions Developer | .NET Core, C# & React",
                "Enterprise software developer focused on Domain-Driven Design (DDD), Clean Architecture, high-reliability relational databases, and enterprise integration patterns.",
                "https://linkedin.com/in/ava-white-dev", "https://github.com/ava-white",
                "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=80&w=400",
                "Faculty of Engineering", "Department of Computer Systems", "BSc (Hons) in Computer Systems Engineering",
                3.78, "Enterprise Software Engineer",
                List.of("C#", ".NET Core", "React", "PostgreSQL", "SQL Server", "Docker", "Azure"),
                "[{\"title\":\"Enterprise Inventory & Warehouse Management\",\"description\":\"Developed clean-architecture warehouse logistics platform utilizing CQRS pattern, MediatR, and React interface.\",\"tech_stack\":[\"C#\",\".NET Core\",\"PostgreSQL\",\"Docker\",\"React\"]},{\"title\":\"Industrial Asset Lifecycle Management System\",\"description\":\"Engineered equipment depreciation tracking and predictive maintenance schedule portal.\",\"tech_stack\":[\".NET Core\",\"React\",\"Docker\",\"Azure\"]}]"
        );

        // --- Student 9: Alexander Harris ---
        seedStudent(
                "student9", "Alexander", "Harris", "student9@students.nsbm.ac.lk", "+94 77 123 4509",
                "Data Platform & Big Data Engineer | Kafka, Spark, PostgreSQL & ETL",
                "Data infrastructure developer passionate about stream processing at scale, building fault-tolerant data pipelines, and analytical data warehouse platforms.",
                "https://linkedin.com/in/alexander-harris-data", "https://github.com/aharris-data",
                "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?auto=format&fit=crop&q=80&w=400",
                "Faculty of Computing", "Department of Data Science", "BSc (Hons) in Data Science",
                3.65, "Data Engineer",
                List.of("Python", "Apache Spark", "Apache Kafka", "PostgreSQL", "SQL", "Docker", "Airflow"),
                "[{\"title\":\"Real-Time Telemetry ETL Streaming Pipeline\",\"description\":\"Built real-time event streaming pipeline processing 50k logs/second with Apache Kafka, Spark Streaming, and PostgreSQL.\",\"tech_stack\":[\"Apache Kafka\",\"Python\",\"PostgreSQL\",\"Docker\"]},{\"title\":\"Automated Lakehouse Ingestion & DQ Verification\",\"description\":\"Constructed Apache Airflow DAGs orchestrating batch data ingestion, deduplication, and schema validation.\",\"tech_stack\":[\"Python\",\"SQL\",\"PostgreSQL\",\"Airflow\"]}]"
        );

        // --- Student 10: Mia Martin ---
        seedStudent(
                "student10", "Mia", "Martin", "student10@students.nsbm.ac.lk", "+94 77 123 4510",
                "Site Reliability Engineer (SRE) & Infrastructure Developer | Go, Linux & Cloud",
                "Infrastructure and resilience engineer passionate about chaos engineering, automated incident mitigation, container runtimes, and building robust self-healing cloud platforms.",
                "https://linkedin.com/in/mia-martin-sre", "https://github.com/mia-martin-infra",
                "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&q=80&w=400",
                "Faculty of Engineering", "Department of Computer Systems", "BSc (Hons) in Computer Systems Engineering",
                3.95, "Site Reliability Engineer (SRE)",
                List.of("Go", "Linux", "Kubernetes", "Docker", "Terraform", "Prometheus", "Git"),
                "[{\"title\":\"Automated Chaos Engineering Verification Platform\",\"description\":\"Engineered resilience verification tool simulating network partitions and resource starvation across Kubernetes clusters.\",\"tech_stack\":[\"Go\",\"Kubernetes\",\"Docker\",\"Linux\"]},{\"title\":\"Distributed Server Telemetry Exporter Daemon\",\"description\":\"Developed high-frequency lightweight system metric collector emitting Prometheus metrics with near-zero CPU overhead.\",\"tech_stack\":[\"Go\",\"Linux\",\"Docker\",\"Prometheus\"]}]"
        );
    }

    private void seedStudent(String userId, String firstName, String lastName, String email, String phone,
                             String headline, String bio, String linkedinUrl, String githubUrl, String profilePicUrl,
                             String faculty, String department, String degreeProgram, double gpa, String desiredRole,
                             List<String> skills, String projectsJson) {
        // 1. Save UserProfile
        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phone(phone)
                .headline(headline)
                .bio(bio)
                .linkedinUrl(linkedinUrl)
                .githubUrl(githubUrl)
                .profilePicUrl(profilePicUrl)
                .userRole(UserRole.STUDENT)
                .accountStatus(AccountStatus.ACTIVE)
                .faculty(faculty)
                .department(department)
                .isActivelyLooking(true)
                .projects(projectsJson)
                .build();
        userProfileRepository.save(profile);

        // 2. Save Academic Record
        AcademicRecord academicRecord = AcademicRecord.builder()
                .recordId(UUID.randomUUID().toString())
                .userId(userId)
                .faculty(faculty)
                .department(department)
                .degreeProgram(degreeProgram)
                .semester(2)
                .year(3)
                .gpa(gpa)
                .batch("22.1")
                .build();
        academicRecordRepository.save(academicRecord);

        // 3. Save Skills
        List<Skill> skillEntities = new ArrayList<>();
        for (String s : skills) {
            skillEntities.add(Skill.builder()
                    .skillId(UUID.randomUUID().toString())
                    .userId(userId)
                    .skillName(s)
                    .skillLevel("ADVANCED")
                    .category("TECHNICAL")
                    .build());
        }
        skillRepository.saveAll(skillEntities);

        // 4. Save Job Preference
        JobPreference jobPreference = JobPreference.builder()
                .preferenceId(UUID.randomUUID().toString())
                .userId(userId)
                .jobRole(desiredRole)
                .location("Colombo, Sri Lanka (Hybrid / Remote)")
                .jobType("INTERNSHIP")
                .build();
        jobPreferenceRepository.save(jobPreference);
    }
}
