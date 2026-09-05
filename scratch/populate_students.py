import psycopg2
import uuid
import json

conn = psycopg2.connect(
    host='nicdbpgs.postgres.database.azure.com',
    port=5432,
    dbname='user_db',
    user='pguser',
    password='NicDB@123',
    sslmode='require'
)
conn.autocommit = False
cur = conn.cursor()

try:
    print("[1/4] Ensuring headline, linkedin_url, and github_url columns exist...")
    cur.execute("ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS headline VARCHAR(255);")
    cur.execute("ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS linkedin_url VARCHAR(255);")
    cur.execute("ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS github_url VARCHAR(255);")
    conn.commit()
    print("      Columns verified/added successfully.")

    print("\n[2/4] Purging existing student records...")
    student_ids = [f"student{i}" for i in range(1, 11)] + ["student"]
    
    # Also find any other user_profiles with user_role = 'STUDENT'
    cur.execute("SELECT user_id FROM user_profiles WHERE user_role = 'STUDENT' AND user_id != 'admin';")
    extra_students = [r[0] for r in cur.fetchall()]
    all_purge_ids = list(set(student_ids + extra_students))
    print(f"      Purging data for {len(all_purge_ids)} student IDs: {all_purge_ids}")

    for uid in all_purge_ids:
        cur.execute("DELETE FROM skills WHERE user_id = %s;", (uid,))
        cur.execute("DELETE FROM academic_records WHERE user_id = %s;", (uid,))
        # Check if job_preferences table exists
        cur.execute("DELETE FROM job_preferences WHERE user_id = %s;", (uid,))
        # Check if resumes table exists
        cur.execute("DELETE FROM resumes WHERE user_id = %s;", (uid,))
        cur.execute("DELETE FROM user_profiles WHERE user_id = %s;", (uid,))
    
    conn.commit()
    print("      Existing student records dropped cleanly.")

    print("\n[3/4] Repopulating fresh student dummy profiles...")
    students_data = [
        {
            "id": "student1",
            "firstName": "John",
            "lastName": "Smith",
            "email": "student1@students.nsbm.ac.lk",
            "phone": "+94 77 123 4501",
            "headline": "Full-Stack Software Engineer | React, Spring Boot & Cloud Architectures",
            "bio": "Passionate software engineering undergraduate specializing in reactive web applications, distributed microservices, and containerized cloud deployments. Active open-source contributor and hackathon enthusiast.",
            "linkedin": "https://linkedin.com/in/johnsmith-se",
            "github": "https://github.com/johnsmith-dev",
            "pic": "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=400",
            "faculty": "Faculty of Computing",
            "department": "Department of Software Engineering",
            "degree": "BSc (Hons) in Software Engineering",
            "gpa": 3.82,
            "role": "Full-Stack Software Engineer",
            "skills": ["Java", "Spring Boot", "React", "TypeScript", "PostgreSQL", "Docker", "REST APIs"],
            "projects": [
                {
                    "title": "Enterprise Microservices E-Commerce Platform",
                    "description": "Architected a resilient event-driven e-commerce platform using Spring Cloud, RabbitMQ, PostgreSQL, and React.",
                    "tech_stack": ["Java", "Spring Boot", "React", "RabbitMQ", "PostgreSQL", "Docker"]
                },
                {
                    "title": "Real-Time Collaborative Code Workspace",
                    "description": "Constructed a browser-based collaborative programming IDE with WebSockets, OT synchronization, and Redis.",
                    "tech_stack": ["React", "TypeScript", "Node.js", "WebSocket", "Redis", "Tailwind CSS"]
                }
            ]
        },
        {
            "id": "student2",
            "firstName": "Emma",
            "lastName": "Johnson",
            "email": "student2@students.nsbm.ac.lk",
            "phone": "+94 77 123 4502",
            "headline": "Cloud & DevOps Specialist | Kubernetes, Docker, CI/CD & AWS",
            "bio": "DevOps and cloud engineer focused on building automated CI/CD deployment pipelines, declarative Kubernetes orchestrations, and infrastructure-as-code using Terraform.",
            "linkedin": "https://linkedin.com/in/emma-johnson-cloud",
            "github": "https://github.com/emma-devops",
            "pic": "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&q=80&w=400",
            "faculty": "Faculty of Computing",
            "department": "Department of Network Engineering",
            "degree": "BSc (Hons) in Computer Networks",
            "gpa": 3.75,
            "role": "Cloud / DevOps Engineer",
            "skills": ["Docker", "Kubernetes", "AWS", "Terraform", "Linux", "GitHub Actions", "Python"],
            "projects": [
                {
                    "title": "Automated Multi-Cluster Kubernetes Platform",
                    "description": "Engineered GitOps delivery workflows using ArgoCD, Helm charts, and automated container security scans.",
                    "tech_stack": ["Kubernetes", "Terraform", "Docker", "AWS", "GitHub Actions"]
                },
                {
                    "title": "Zero-Trust Service Mesh & Observability Suite",
                    "description": "Implemented Istio service mesh mTLS encryption with Prometheus metrics gathering and Grafana alerting.",
                    "tech_stack": ["Istio", "Linux", "Prometheus", "Grafana", "Bash"]
                }
            ]
        },
        {
            "id": "student3",
            "firstName": "Michael",
            "lastName": "Brown",
            "email": "student3@students.nsbm.ac.lk",
            "phone": "+94 77 123 4503",
            "headline": "Backend Systems & Distributed Architecture Engineer | Java & Go",
            "bio": "High-performance backend developer with deep experience in asynchronous event streaming, SQL performance tuning, and resilient distributed microservices architectures.",
            "linkedin": "https://linkedin.com/in/michael-brown-backend",
            "github": "https://github.com/mbrown-systems",
            "pic": "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=400",
            "faculty": "Faculty of Computing",
            "department": "Department of Software Engineering",
            "degree": "BSc (Hons) in Software Engineering",
            "gpa": 3.91,
            "role": "Backend Software Engineer",
            "skills": ["Java", "Spring Boot", "Go", "Apache Kafka", "PostgreSQL", "Redis", "gRPC"],
            "projects": [
                {
                    "title": "High-Throughput Financial Transaction Ledger",
                    "description": "Designed an event-sourced transaction processor handling 10k ops/sec with Apache Kafka and strict ACID guarantees.",
                    "tech_stack": ["Java", "Spring Boot", "Kafka", "PostgreSQL", "Docker"]
                },
                {
                    "title": "Distributed Rate Limiter & API Gateway Plugin",
                    "description": "Built sliding-window rate limiters in Go backed by Redis clusters for ultra-low latency API traffic shaping.",
                    "tech_stack": ["Go", "Redis", "gRPC", "Docker"]
                }
            ]
        },
        {
            "id": "student4",
            "firstName": "Sophia",
            "lastName": "Taylor",
            "email": "student4@students.nsbm.ac.lk",
            "phone": "+94 77 123 4504",
            "headline": "Frontend Engineer & UI/UX Craftsman | React, Next.js & Modern Web",
            "bio": "Creative frontend engineer committed to crafting pixel-perfect, accessible, and high-performance digital experiences. Enthusiastic about micro-interactions and atomic design systems.",
            "linkedin": "https://linkedin.com/in/sophia-taylor-ui",
            "github": "https://github.com/sophia-frontend",
            "pic": "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=400",
            "faculty": "Faculty of Computing",
            "department": "Department of Software Engineering",
            "degree": "BSc (Hons) in Multimedia & Web",
            "gpa": 3.84,
            "role": "Frontend Engineer",
            "skills": ["React", "TypeScript", "Next.js", "Tailwind CSS", "Redux", "Figma", "REST APIs"],
            "projects": [
                {
                    "title": "Accessible Healthcare Telemedicine Portal",
                    "description": "Constructed a WCAG 2.1 AA compliant telehealth platform with appointment scheduling and WebRTC video integration.",
                    "tech_stack": ["React", "Next.js", "TypeScript", "Tailwind CSS", "WebRTC"]
                },
                {
                    "title": "Interactive Financial Analytics Dashboard",
                    "description": "Responsive financial analytics dashboard featuring dynamic charting, theme switching, and real-time data sync.",
                    "tech_stack": ["React", "TypeScript", "Tailwind CSS", "REST APIs"]
                }
            ]
        },
        {
            "id": "student5",
            "firstName": "William",
            "lastName": "Anderson",
            "email": "student5@students.nsbm.ac.lk",
            "phone": "+94 77 123 4505",
            "headline": "AI/ML Engineer & Data Scientist | Python, PyTorch & LLM Fine-Tuning",
            "bio": "Applied machine learning researcher and engineer applying modern natural language processing and computer vision models to enterprise automation challenges.",
            "linkedin": "https://linkedin.com/in/william-anderson-ai",
            "github": "https://github.com/wanderson-ml",
            "pic": "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&q=80&w=400",
            "faculty": "Faculty of Computing",
            "department": "Department of Data Science",
            "degree": "BSc (Hons) in Data Science",
            "gpa": 3.88,
            "role": "AI/ML Engineer",
            "skills": ["Python", "PyTorch", "FastAPI", "Pandas", "Scikit-Learn", "Docker", "PostgreSQL"],
            "projects": [
                {
                    "title": "Automated Resume Intelligence Extraction Engine",
                    "description": "Built hybrid OCR and LLM fine-tuning pipeline extracting skills and experience from unstructured PDF resumes with 96% accuracy.",
                    "tech_stack": ["Python", "FastAPI", "PyTorch", "Docker", "PostgreSQL"]
                },
                {
                    "title": "Industrial Predictive Equipment Maintenance Model",
                    "description": "Trained recurrent neural network predicting industrial hardware failures from IoT telemetry time-series streams.",
                    "tech_stack": ["Python", "Pandas", "Scikit-Learn", "FastAPI"]
                }
            ]
        },
        {
            "id": "student6",
            "firstName": "Olivia",
            "lastName": "Thomas",
            "email": "student6@students.nsbm.ac.lk",
            "phone": "+94 77 123 4506",
            "headline": "Mobile App Developer | React Native, Kotlin & Flutter Specialist",
            "bio": "Mobile engineer dedicated to building silky-smooth 60fps applications, offline-first data synchronization, and intuitive cross-platform user experiences.",
            "linkedin": "https://linkedin.com/in/olivia-thomas-mobile",
            "github": "https://github.com/olivia-mobile",
            "pic": "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&q=80&w=400",
            "faculty": "Faculty of Computing",
            "department": "Department of Software Engineering",
            "degree": "BSc (Hons) in Software Engineering",
            "gpa": 3.68,
            "role": "Mobile Application Developer",
            "skills": ["React Native", "Kotlin", "Flutter", "TypeScript", "Firebase", "Redux", "REST APIs"],
            "projects": [
                {
                    "title": "Smart Campus Guide & Interactive Event App",
                    "description": "Built a cross-platform mobile navigation app with beacon indoor positioning and instant event QR check-ins.",
                    "tech_stack": ["React Native", "TypeScript", "Firebase", "Redux", "REST APIs"]
                },
                {
                    "title": "Offline-First Personal Budgeting Application",
                    "description": "Native Android personal budgeting client with biometric authentication, SQLite local cache, and cloud sync.",
                    "tech_stack": ["Kotlin", "Android", "Firebase", "REST APIs"]
                }
            ]
        },
        {
            "id": "student7",
            "firstName": "James",
            "lastName": "Jackson",
            "email": "student7@students.nsbm.ac.lk",
            "phone": "+94 77 123 4507",
            "headline": "Cybersecurity & Network Systems Engineer | Ethical Hacking & SecOps",
            "bio": "Cybersecurity enthusiast specializing in defensive architecture, penetration testing, automated static analysis (SAST), and secure development lifecycles.",
            "linkedin": "https://linkedin.com/in/james-jackson-sec",
            "github": "https://github.com/jjackson-sec",
            "pic": "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?auto=format&fit=crop&q=80&w=400",
            "faculty": "Faculty of Computing",
            "department": "Department of Network Engineering",
            "degree": "BSc (Hons) in Computer Security",
            "gpa": 3.70,
            "role": "Cybersecurity Analyst / Engineer",
            "skills": ["Linux", "Python", "Docker", "Wireshark", "Bash", "Penetration Testing", "Security"],
            "projects": [
                {
                    "title": "Automated Pull-Request Vulnerability Scanner",
                    "description": "Constructed an automated static application security testing analyzer checking for OWASP Top 10 vulnerabilities.",
                    "tech_stack": ["Python", "Docker", "Linux", "Bash"]
                },
                {
                    "title": "Distributed Honeynet Threat Intelligence System",
                    "description": "Deployed simulated vulnerable micro-services recording attacker attack vectors and cataloging IOC patterns.",
                    "tech_stack": ["Linux", "Docker", "Python", "Wireshark"]
                }
            ]
        },
        {
            "id": "student8",
            "firstName": "Ava",
            "lastName": "White",
            "email": "student8@students.nsbm.ac.lk",
            "phone": "+94 77 123 4508",
            "headline": "Full-Stack Enterprise Solutions Developer | .NET Core, C# & React",
            "bio": "Enterprise software developer focused on Domain-Driven Design (DDD), Clean Architecture, high-reliability relational databases, and enterprise integration patterns.",
            "linkedin": "https://linkedin.com/in/ava-white-dev",
            "github": "https://github.com/ava-white",
            "pic": "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=80&w=400",
            "faculty": "Faculty of Engineering",
            "department": "Department of Computer Systems",
            "degree": "BSc (Hons) in Computer Systems Engineering",
            "gpa": 3.78,
            "role": "Enterprise Software Engineer",
            "skills": ["C#", ".NET Core", "React", "PostgreSQL", "SQL Server", "Docker", "Azure"],
            "projects": [
                {
                    "title": "Enterprise Inventory & Warehouse Management",
                    "description": "Developed clean-architecture warehouse logistics platform utilizing CQRS pattern, MediatR, and React interface.",
                    "tech_stack": ["C#", ".NET Core", "PostgreSQL", "Docker", "React"]
                },
                {
                    "title": "Industrial Asset Lifecycle Management System",
                    "description": "Engineered equipment depreciation tracking and predictive maintenance schedule portal.",
                    "tech_stack": [".NET Core", "React", "Docker", "Azure"]
                }
            ]
        },
        {
            "id": "student9",
            "firstName": "Alexander",
            "lastName": "Harris",
            "email": "student9@students.nsbm.ac.lk",
            "phone": "+94 77 123 4509",
            "headline": "Data Platform & Big Data Engineer | Kafka, Spark, PostgreSQL & ETL",
            "bio": "Data infrastructure developer passionate about stream processing at scale, building fault-tolerant data pipelines, and analytical data warehouse platforms.",
            "linkedin": "https://linkedin.com/in/alexander-harris-data",
            "github": "https://github.com/aharris-data",
            "pic": "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?auto=format&fit=crop&q=80&w=400",
            "faculty": "Faculty of Computing",
            "department": "Department of Data Science",
            "degree": "BSc (Hons) in Data Science",
            "gpa": 3.65,
            "role": "Data Engineer",
            "skills": ["Python", "Apache Spark", "Apache Kafka", "PostgreSQL", "SQL", "Docker", "Airflow"],
            "projects": [
                {
                    "title": "Real-Time Telemetry ETL Streaming Pipeline",
                    "description": "Built real-time event streaming pipeline processing 50k logs/second with Apache Kafka, Spark Streaming, and PostgreSQL.",
                    "tech_stack": ["Apache Kafka", "Python", "PostgreSQL", "Docker"]
                },
                {
                    "title": "Automated Lakehouse Ingestion & DQ Verification",
                    "description": "Constructed Apache Airflow DAGs orchestrating batch data ingestion, deduplication, and schema validation.",
                    "tech_stack": ["Python", "SQL", "PostgreSQL", "Airflow"]
                }
            ]
        },
        {
            "id": "student10",
            "firstName": "Mia",
            "lastName": "Martin",
            "email": "student10@students.nsbm.ac.lk",
            "phone": "+94 77 123 4510",
            "headline": "Site Reliability Engineer (SRE) & Infrastructure Developer | Go, Linux & Cloud",
            "bio": "Infrastructure and resilience engineer passionate about chaos engineering, automated incident mitigation, container runtimes, and building robust self-healing cloud platforms.",
            "linkedin": "https://linkedin.com/in/mia-martin-sre",
            "github": "https://github.com/mia-martin-infra",
            "pic": "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&q=80&w=400",
            "faculty": "Faculty of Engineering",
            "department": "Department of Computer Systems",
            "degree": "BSc (Hons) in Computer Systems Engineering",
            "gpa": 3.95,
            "role": "Site Reliability Engineer (SRE)",
            "skills": ["Go", "Linux", "Kubernetes", "Docker", "Terraform", "Prometheus", "Git"],
            "projects": [
                {
                    "title": "Automated Chaos Engineering Verification Platform",
                    "description": "Engineered resilience verification tool simulating network partitions and resource starvation across Kubernetes clusters.",
                    "tech_stack": ["Go", "Kubernetes", "Docker", "Linux"]
                },
                {
                    "title": "Distributed Server Telemetry Exporter Daemon",
                    "description": "Developed high-frequency lightweight system metric collector emitting Prometheus metrics with near-zero CPU overhead.",
                    "tech_stack": ["Go", "Linux", "Docker", "Prometheus"]
                }
            ]
        }
    ]

    for s in students_data:
        # 1. UserProfile
        cur.execute("""
            INSERT INTO user_profiles (
                user_id, first_name, last_name, email, phone, headline, bio, 
                linkedin_url, github_url, profile_pic_url, user_role, account_status, 
                faculty, department, is_actively_looking, projects, created_at, updated_at
            ) VALUES (
                %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW()
            );
        """, (
            s["id"], s["firstName"], s["lastName"], s["email"], s["phone"], s["headline"], s["bio"],
            s["linkedin"], s["github"], s["pic"], "STUDENT", "ACTIVE",
            s["faculty"], s["department"], True, json.dumps(s["projects"])
        ))

        # 2. Academic Record
        rec_id = str(uuid.uuid4())
        cur.execute("""
            INSERT INTO academic_records (
                record_id, user_id, faculty, department, degree_program, semester, year, gpa, batch
            ) VALUES (
                %s, %s, %s, %s, %s, %s, %s, %s, %s
            );
        """, (
            rec_id, s["id"], s["faculty"], s["department"], s["degree"], 2, 3, s["gpa"], "22.1"
        ))

        # 3. Skills
        for sk in s["skills"]:
            sk_id = str(uuid.uuid4())
            cur.execute("""
                INSERT INTO skills (
                    skill_id, user_id, skill_name, skill_level, category
                ) VALUES (
                    %s, %s, %s, %s, %s
                );
            """, (
                sk_id, s["id"], sk, "ADVANCED", "TECHNICAL"
            ))

        # 4. Job Preference
        jp_id = str(uuid.uuid4())
        cur.execute("""
            INSERT INTO job_preferences (
                preference_id, user_id, job_role, location, job_type
            ) VALUES (
                %s, %s, %s, %s, %s
            );
        """, (
            jp_id, s["id"], s["role"], "Colombo, Sri Lanka (Hybrid / Remote)", "INTERNSHIP"
        ))

    conn.commit()
    print("      Successfully repopulated all 10 student profiles!")

    print("\n[4/4] Verification check on student1:")
    cur.execute("SELECT user_id, first_name, last_name, headline, phone, linkedin_url, github_url FROM user_profiles WHERE user_id = 'student1';")
    print("      Profile:", cur.fetchone())
    cur.execute("SELECT degree_program, gpa, batch FROM academic_records WHERE user_id = 'student1';")
    print("      Academic Record:", cur.fetchone())
    cur.execute("SELECT skill_name FROM skills WHERE user_id = 'student1';")
    print("      Skills:", [r[0] for r in cur.fetchall()])

except Exception as e:
    conn.rollback()
    print(f"\nERROR: {e}")
    raise
finally:
    cur.close()
    conn.close()
