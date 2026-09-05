import asyncio
import json
import logging
import re
import threading
import time

try:
    from rich.console import Console
    console = Console()
except ImportError:
    console = None

try:
    import pika
    from pika.exceptions import AMQPConnectionError
    PIKA_AVAILABLE = True
except ImportError:
    pika = None
    AMQPConnectionError = Exception
    PIKA_AVAILABLE = False

import httpx

from app.config import settings
from app.database import SessionLocal
from app.services.pipeline import VacancyPipelineService

logger = logging.getLogger("ai_service.rabbitmq")

# Vacancy-service base URL
VACANCY_SERVICE_BASE_URL = settings.VACANCY_SERVICE_BASE_URL


class RabbitMQConsumer:
    def __init__(self):
        self.thread: threading.Thread | None = None
        self.connection = None
        self.channel = None
        self.is_running = False
        self.pipeline_service = VacancyPipelineService()

    def start(self):
        """Starts the RabbitMQ consumer in a background daemon thread."""
        if not PIKA_AVAILABLE:
            logger.warning("pika package is not installed. RabbitMQ consumer will not start.")
            return

        if not settings.RABBITMQ_ENABLED:
            logger.info("RabbitMQ is disabled in settings.")
            return

        self.is_running = True
        self.thread = threading.Thread(target=self._run_consumer, name="RabbitMQConsumerThread", daemon=True)
        self.thread.start()
        logger.info("RabbitMQ consumer thread started.")

    def stop(self):
        """Gracefully stops the consumer and closes connections."""
        self.is_running = False
        if self.connection and self.connection.is_open:
            try:
                self.connection.close()
            except Exception as e:
                logger.warning(f"Error closing RabbitMQ connection: {e}")
        logger.info("RabbitMQ consumer stopped.")

    def _run_consumer(self):
        """Main loop with auto-reconnect."""
        credentials = pika.PlainCredentials(settings.RABBITMQ_USERNAME, settings.RABBITMQ_PASSWORD)
        parameters = pika.ConnectionParameters(
            host=settings.RABBITMQ_HOST,
            port=settings.RABBITMQ_PORT,
            virtual_host=settings.RABBITMQ_VIRTUAL_HOST,
            credentials=credentials,
            heartbeat=600,
            blocked_connection_timeout=300
        )

        while self.is_running:
            try:
                logger.info(f"Connecting to RabbitMQ at {settings.RABBITMQ_HOST}:{settings.RABBITMQ_PORT}...")
                self.connection = pika.BlockingConnection(parameters)
                self.channel = self.connection.channel()

                # Declare topic exchange and durable queue
                self.channel.exchange_declare(
                    exchange=settings.RABBITMQ_EXCHANGE,
                    exchange_type="topic",
                    durable=True
                )
                # 1. Vacancy Flyer Queue
                self.channel.queue_declare(
                    queue=settings.RABBITMQ_QUEUE,
                    durable=True
                )
                self.channel.queue_bind(
                    exchange=settings.RABBITMQ_EXCHANGE,
                    queue=settings.RABBITMQ_QUEUE,
                    routing_key=settings.RABBITMQ_ROUTING_KEY
                )

                # 2. Application AI Match Queue
                APP_MATCH_QUEUE = "application.ai.match.queue"
                APP_MATCH_ROUTING_KEY = "application.submitted.match"
                self.channel.queue_declare(
                    queue=APP_MATCH_QUEUE,
                    durable=True
                )
                self.channel.queue_bind(
                    exchange=settings.RABBITMQ_EXCHANGE,
                    queue=APP_MATCH_QUEUE,
                    routing_key=APP_MATCH_ROUTING_KEY
                )

                # Set prefetch count to 1 so each consumer processes one flyer/application at a time
                self.channel.basic_qos(prefetch_count=1)

                logger.info(
                    f"Connected to RabbitMQ! Listening on '{settings.RABBITMQ_QUEUE}' and '{APP_MATCH_QUEUE}' "
                    f"bound to exchange '{settings.RABBITMQ_EXCHANGE}'..."
                )

                self.channel.basic_consume(
                    queue=settings.RABBITMQ_QUEUE,
                    on_message_callback=self._on_message,
                    auto_ack=False
                )
                self.channel.basic_consume(
                    queue=APP_MATCH_QUEUE,
                    on_message_callback=self._on_application_match_message,
                    auto_ack=False
                )

                self.channel.start_consuming()

            except AMQPConnectionError as e:
                logger.warning(f"RabbitMQ connection failed: {e}. Retrying in 5 seconds...")
                time.sleep(5)
            except Exception as e:
                if self.is_running:
                    logger.error(f"Unexpected RabbitMQ error: {e}. Retrying in 5 seconds...", exc_info=True)
                    time.sleep(5)
                else:
                    break

    def _on_message(self, ch, method, properties, body):
        """Callback invoked when a flyer upload event is received."""
        logger.info(f"Received message on {method.routing_key}: {body.decode('utf-8', errors='ignore')}")
        try:
            payload = json.loads(body.decode("utf-8"))
            vacancy_id = payload.get("vacancyId")
            partner_id = payload.get("partnerId")
            file_url = payload.get("fileUrl")
            storage_file_id = payload.get("storageFileId")

            if not file_url and storage_file_id:
                file_url = f"{settings.BACKEND_API_BASE_URL}/storage/download/{storage_file_id}"
            elif file_url and "localhost:8080" in file_url and "localhost:8080" not in settings.BACKEND_API_BASE_URL:
                file_url = file_url.replace("http://localhost:8080/api/v1", settings.BACKEND_API_BASE_URL)
                if not file_url.startswith("http"):
                    file_url = f"{settings.BACKEND_API_BASE_URL}/storage/download/{storage_file_id}"

            if not file_url:
                logger.error(f"Message missing fileUrl/storageFileId: {payload}")
                ch.basic_ack(delivery_tag=method.delivery_tag)
                return

            print("\n" + "=" * 80)
            print(f"📥 [AI SERVICE] NEW VACANCY FLYER RECEIVED ASYNCHRONOUSLY")
            print(f"   Vacancy ID:       {vacancy_id}")
            print(f"   Partner ID:       {partner_id}")
            print(f"   Storage File ID:  {storage_file_id}")
            print(f"   Download URL:     {file_url}")
            print("=" * 80 + "\n")

            # Process through OCR + LLM pipeline
            db = SessionLocal()
            try:
                # Run async pipeline synchronously in this worker thread
                if console:
                    with console.status(f"[bold green]Processing Vacancy Flyer ID: {vacancy_id}...", spinner="dots") as status:
                        # Run pipeline (it takes a few seconds)
                        response = asyncio.run(
                            self.pipeline_service.process_and_save(
                                image_url=file_url,
                                db=db,
                                partner_id=str(partner_id) if partner_id else None,
                                progress_callback=status.update
                            )
                        )
                else:
                    response = asyncio.run(
                        self.pipeline_service.process_and_save(
                            image_url=file_url,
                            db=db,
                            partner_id=str(partner_id) if partner_id else None
                        )
                    )

                extracted = response.extracted_data
                institutional = response.institutional_analysis

                # ── Pretty-print extraction results ──
                print("\n" + "=" * 80)
                print("🚀 [AI SERVICE] ASYNC VACANCY FLYER EXTRACTION COMPLETED SUCCESSFULLY")
                print("-" * 80)
                print(f"📋 Vacancy ID:             {vacancy_id} (AI Record: {response.vacancy_id})")
                print(f"🏢 Partner ID:             {partner_id}")
                print("-" * 80)
                print(f"💼 Job Title:              {extracted.job_title}")
                print(f"📊 Seniority Level:        {extracted.seniority_level or 'Not specified'}")
                print(f"📝 Employment Type:        {extracted.employment_type or 'Not specified'}")
                print(f"🏛️ Company Name:           {extracted.company_name or 'Not specified'}")
                print(f"💻 Workplace Type:         {extracted.workplace_type or 'Not specified'}")
                print(f"📍 Locations:              {', '.join(extracted.locations) if extracted.locations else 'Not specified'}")
                print(f"📅 Min Experience (yrs):   {extracted.min_experience_years}")
                print(f"💰 Salary Raw:             {extracted.salary_raw or 'Not specified'}")
                print(f"📅 Application Deadline:   {extracted.application_deadline or 'Not specified'}")
                print("-" * 80)
                print(f"🛠️ Required Skills:        {', '.join(extracted.required_skills) if extracted.required_skills else 'None'}")
                print(f"🌟 Preferred Skills:       {', '.join(extracted.preferred_skills) if extracted.preferred_skills else 'None'}")
                print(f"🎓 Education Reqs:         {', '.join(extracted.education_requirements) if extracted.education_requirements else 'None'}")
                print(f"📋 Responsibilities:       {', '.join(extracted.responsibilities) if extracted.responsibilities else 'None'}")
                print(f"✅ Eligibility Criteria:   {', '.join(extracted.eligibility_criteria) if extracted.eligibility_criteria else 'None'}")
                print("-" * 80)
                print(f"📧 Contact Emails:         {', '.join(extracted.contact_emails) if extracted.contact_emails else 'None'}")
                print(f"📞 Contact Phones:         {', '.join(extracted.contact_phones) if extracted.contact_phones else 'None'}")
                print(f"🔗 Application URLs:       {', '.join(extracted.application_urls) if extracted.application_urls else 'None'}")
                print("-" * 80)
                print(f"🎓 Target Faculty:         {institutional.target_faculty}")
                print(f"🎯 Institutional Match:    {institutional.institutional_match_score}%")
                print(f"👤 Suitable for Grads:     {'Yes' if institutional.is_suitable_for_interns_or_graduates else 'No'}")
                print(f"📚 Recommended Programs:   {', '.join(institutional.recommended_degree_programs) if institutional.recommended_degree_programs else 'None'}")
                print(f"✅ Approval:               {institutional.approval_recommendation}")
                print(f"📝 Fit Notes:              {institutional.institutional_fit_notes}")

                if institutional.missing_explicit_fields:
                    print("-" * 80)
                    print("⚠️  MISSING / AMBIGUOUS FIELDS:")
                    for flag in institutional.missing_explicit_fields:
                        print(f"   [{flag.severity}] {flag.field_name}: {flag.message}")
                        print(f"            → Suggestion: {flag.suggestion}")

                if institutional.compliance_flags:
                    print(f"🚩 Compliance Flags:       {', '.join(institutional.compliance_flags)}")

                print(f"⏱️ Processing Time:        {response.processing_time_seconds}s")
                print("=" * 80 + "\n")

                # ── Save extracted data back to vacancy-service DB ──
                self._update_vacancy_service(vacancy_id, extracted, institutional)

            finally:
                db.close()

            # Acknowledge the message
            ch.basic_ack(delivery_tag=method.delivery_tag)
            logger.info(f"Successfully processed and acknowledged message for vacancy {vacancy_id}")

        except Exception as e:
            logger.error(f"Error processing RabbitMQ message: {e}", exc_info=True)
            # Acknowledge to avoid infinite redelivery loop on corrupted files
            ch.basic_ack(delivery_tag=method.delivery_tag)

    def _update_vacancy_service(self, vacancy_id, extracted, institutional):
        """
        Sends the AI-extracted data back to the vacancy-service via HTTP PUT
        to update the vacancy record with parsed information.
        """
        if vacancy_id is None:
            logger.warning("Cannot update vacancy-service: vacancy_id is None")
            return

        # Build the update payload matching UpdateVacancyRequest DTO
        update_payload = {}

        # Title from AI extraction
        if extracted.job_title:
            update_payload["title"] = extracted.job_title

        # Build a rich description from responsibilities + eligibility
        description_parts = []
        if extracted.company_name:
            description_parts.append(f"Company: {extracted.company_name}")
        if extracted.responsibilities:
            description_parts.append("Responsibilities:\n• " + "\n• ".join(extracted.responsibilities))
        if extracted.eligibility_criteria:
            description_parts.append("Eligibility:\n• " + "\n• ".join(extracted.eligibility_criteria))
        if description_parts:
            update_payload["description"] = "\n\n".join(description_parts)

        # Build requirements string from required_skills + education
        requirements_parts = []
        if extracted.required_skills:
            requirements_parts.append("Required Skills: " + ", ".join(extracted.required_skills))
        if extracted.preferred_skills:
            requirements_parts.append("Preferred Skills: " + ", ".join(extracted.preferred_skills))
        if extracted.education_requirements:
            requirements_parts.append("Education: " + ", ".join(extracted.education_requirements))
        if extracted.min_experience_years and extracted.min_experience_years > 0:
            requirements_parts.append(f"Min Experience: {extracted.min_experience_years} years")
        if requirements_parts:
            update_payload["requirements"] = "\n".join(requirements_parts)

        # Location
        if extracted.locations:
            update_payload["location"] = ", ".join(extracted.locations)

        # Workplace type mapping
        if extracted.workplace_type:
            wt = extracted.workplace_type.upper().replace(" ", "_").replace("-", "_")
            if wt in ("ON_SITE", "ONSITE", "ON-SITE"):
                update_payload["workplaceType"] = "ON_SITE"
            elif wt in ("REMOTE",):
                update_payload["workplaceType"] = "REMOTE"
            elif wt in ("HYBRID",):
                update_payload["workplaceType"] = "HYBRID"

        # Employment type / Job type mapping
        if extracted.employment_type:
            et = extracted.employment_type.upper().replace(" ", "_").replace("-", "_")
            if "FULL" in et:
                update_payload["jobType"] = "FULL_TIME"
            elif "PART" in et:
                update_payload["jobType"] = "PART_TIME"
            elif "CONTRACT" in et:
                update_payload["jobType"] = "CONTRACT"
            elif "INTERN" in et:
                update_payload["jobType"] = "INTERNSHIP"

        # Salary
        if extracted.salary_raw:
            update_payload["salaryRange"] = extracted.salary_raw

        # Application deadline mapping (standard YYYY-MM-DD for Spring Boot LocalDate)
        if extracted.application_deadline:
            deadline_match = re.search(r'\b(\d{4}-\d{2}-\d{2})\b', str(extracted.application_deadline))
            if deadline_match:
                update_payload["applicationDeadline"] = deadline_match.group(1)

        # Tags from unique required + preferred skills without crude string chopping
        all_skills = list(dict.fromkeys((extracted.required_skills or []) + (extracted.preferred_skills or [])))
        if all_skills:
            selected_tags = []
            curr_len = 0
            for skill in all_skills[:15]:
                skill_clean = skill.strip()
                if not skill_clean:
                    continue
                if curr_len + len(skill_clean) + 2 > 290:
                    break
                selected_tags.append(skill_clean)
                curr_len += len(skill_clean) + 2
            if selected_tags:
                update_payload["tags"] = ", ".join(selected_tags)

        # Target faculty from institutional analysis
        if institutional.target_faculty:
            update_payload["targetFaculties"] = institutional.target_faculty

        # AI missing fields as JSON for admin review
        ai_analysis = {
            "companyName": extracted.company_name,
            "missingFields": [],
            "institutionalMatchScore": institutional.institutional_match_score,
            "approvalRecommendation": institutional.approval_recommendation,
            "isSuitableForGraduates": institutional.is_suitable_for_interns_or_graduates,
            "complianceFlags": institutional.compliance_flags,
            "fitNotes": institutional.institutional_fit_notes,
            "recommendedPrograms": institutional.recommended_degree_programs,
            "contactEmails": extracted.contact_emails if extracted.contact_emails else [],
            "contactPhones": extracted.contact_phones if extracted.contact_phones else []
        }

        if institutional.missing_explicit_fields:
            missing_fields_data = []
            for flag in institutional.missing_explicit_fields:
                missing_fields_data.append({
                    "field": flag.field_name,
                    "severity": flag.severity,
                    "message": flag.message,
                    "suggestion": flag.suggestion
                })
            ai_analysis["missingFields"] = missing_fields_data

        update_payload["aiMissingFields"] = json.dumps(ai_analysis)

        if not update_payload:
            logger.info("No fields to update on vacancy-service.")
            return

        # Candidate URLs to update vacancy-service (direct service or via API gateway)
        target_urls = []
        if settings.VACANCY_SERVICE_BASE_URL and ("localhost" not in settings.VACANCY_SERVICE_BASE_URL or "azurecontainerapps.io" not in settings.EUREKA_SERVER_URL):
            target_urls.append(f"{settings.VACANCY_SERVICE_BASE_URL.rstrip('/')}/{vacancy_id}")
        if settings.BACKEND_API_BASE_URL and ("localhost" not in settings.BACKEND_API_BASE_URL or "azurecontainerapps.io" not in settings.EUREKA_SERVER_URL):
            target_urls.append(f"{settings.BACKEND_API_BASE_URL.rstrip('/')}/vacancies/partner/{vacancy_id}")

        # Cloud auto-discovery fallbacks
        if "azurecontainerapps.io" in settings.EUREKA_SERVER_URL:
            env_domain = settings.EUREKA_SERVER_URL.split("discovery-server.")[-1].split("/")[0]
            target_urls.append(f"https://api-gateway.{env_domain}/api/v1/vacancies/partner/{vacancy_id}")
            target_urls.append(f"https://vacancy-service.internal.{env_domain}/api/v1/vacancies/partner/{vacancy_id}")
            target_urls.append(f"http://vacancy-service/api/v1/vacancies/partner/{vacancy_id}")
        
        # Local development fallback
        if not target_urls:
            target_urls = [
                f"http://localhost:8087/api/v1/vacancies/partner/{vacancy_id}",
                f"http://localhost:8080/api/v1/vacancies/partner/{vacancy_id}"
            ]
        
        seen_urls = set()
        updated_successfully = False

        for url in target_urls:
            if not url or url in seen_urls:
                continue
            seen_urls.add(url)
            try:
                with httpx.Client(timeout=30.0) as client:
                    resp = client.put(url, json=update_payload)

                if resp.status_code == 200:
                    print(f"\n[AI SERVICE] Vacancy #{vacancy_id} updated in vacancy-service successfully via {url}!")
                    print(f"   Updated fields: {', '.join(update_payload.keys())}")
                    logger.info(f"Vacancy {vacancy_id} updated in vacancy-service. Status: {resp.status_code}")
                    updated_successfully = True
                    break
                else:
                    logger.warning(
                        f"Attempted to update vacancy {vacancy_id} at {url}. "
                        f"Status: {resp.status_code}, Response: {resp.text[:200]}"
                    )
            except httpx.ConnectError:
                logger.warning(f"Cannot connect to vacancy endpoint at {url}.")
            except Exception as e:
                logger.warning(f"Error updating vacancy {vacancy_id} at {url}: {e}")

        if not updated_successfully:
            logger.error(f"Failed to update vacancy #{vacancy_id} across all configured endpoints.")
            print(f"\n[AI SERVICE] Failed to update vacancy #{vacancy_id} across all endpoints.")

    def _on_application_match_message(self, ch, method, properties, body):
        """Callback invoked when a candidate job application is queued for async AI matching."""
        logger.info(f"Received application AI match task: {body.decode('utf-8', errors='ignore')}")
        try:
            payload = json.loads(body.decode("utf-8"))
            app_id = payload.get("applicationId")
            vacancy_id = payload.get("vacancyId")
            resume_url = payload.get("resumeUrl")
            vac_title = payload.get("vacancyTitle") or f"Job Vacancy #{vacancy_id}"
            vac_reqs = payload.get("vacancyRequirements") or ""
            vac_desc = payload.get("vacancyDescription") or ""
            vac_tags = payload.get("vacancyTags") or ""
            cand_name = payload.get("candidateName") or "Applicant"
            cand_email = payload.get("candidateEmail")
            cand_faculty = payload.get("candidateFaculty")
            cand_skills = payload.get("candidateSkills") or []
            cover_letter = payload.get("coverLetter") or ""

            print("\n" + "=" * 80)
            print(f"[*] [AI SERVICE] ASYNC JOB APPLICATION AI MATCHING INTAKE")
            print(f"   Application ID:  {app_id}")
            print(f"   Candidate:       {cand_name} ({cand_email or 'N/A'})")
            print(f"   Vacancy:         {vac_title} (ID: {vacancy_id})")
            print(f"   Resume URL:      {resume_url}")
            print("=" * 80 + "\n")

            if not app_id:
                logger.error("Application match message missing applicationId.")
                ch.basic_ack(delivery_tag=method.delivery_tag)
                return

            from app.schemas import SingleApplicantMatchRequest
            from app.services.resume_matcher_service import ResumeVacancyMatcherService

            db = SessionLocal()
            try:
                matcher = ResumeVacancyMatcherService()
                match_req = SingleApplicantMatchRequest(
                    resume_url=resume_url,
                    cover_letter=cover_letter,
                    vacancy_id=vacancy_id,
                    vacancy_title=vac_title,
                    vacancy_requirements=vac_reqs,
                    vacancy_description=vac_desc,
                    vacancy_tags=vac_tags,
                    candidate_name=cand_name,
                    candidate_email=cand_email,
                    candidate_faculty=cand_faculty,
                    candidate_skills=cand_skills
                )

                match_res = asyncio.run(matcher.match_single_applicant(match_req, db))

                print("\n" + "=" * 80)
                print(f"[+] [AI SERVICE] APPLICATION MATCH COMPUTED SUCCESSFULLY")
                print(f"   Match Score:     {match_res.match_percentage}% ({match_res.match_tier})")
                print(f"   Applicant Summary: {match_res.fit_summary}")
                print(f"   Strong Fortes:   {', '.join(match_res.strong_fortes) if match_res.strong_fortes else 'None'}")
                print(f"   Matched Skills:  {', '.join(match_res.matched_skills) if match_res.matched_skills else 'None'}")
                print("=" * 80 + "\n")

                breakdown_str = json.dumps({
                    "summary": match_res.fit_summary,
                    "strongFortes": match_res.strong_fortes,
                    "skills_coverage": match_res.score_breakdown.skills_coverage if match_res.score_breakdown else 0,
                    "semantic_alignment": match_res.score_breakdown.semantic_alignment if match_res.score_breakdown else 0,
                    "cross_encoder_score": match_res.score_breakdown.cross_encoder_score if match_res.score_breakdown else 0,
                    "institutional_fit": match_res.score_breakdown.institutional_fit if match_res.score_breakdown else 0
                })

                insights_payload = {
                    "matchPercentage": match_res.match_percentage,
                    "matchedSkills": ", ".join(match_res.matched_skills) if match_res.matched_skills else "",
                    "missingSkills": ", ".join(match_res.missing_skills) if match_res.missing_skills else "",
                    "fitSummary": match_res.fit_summary,
                    "strongFortes": json.dumps(match_res.strong_fortes) if match_res.strong_fortes else "",
                    "scoreBreakdown": breakdown_str
                }

                candidate_urls = []
                if settings.APPLICATION_SERVICE_BASE_URL and ("localhost" not in settings.APPLICATION_SERVICE_BASE_URL or "azurecontainerapps.io" not in settings.EUREKA_SERVER_URL):
                    candidate_urls.append(f"{settings.APPLICATION_SERVICE_BASE_URL.rstrip('/')}/{app_id}/ai-insights")
                if settings.BACKEND_API_BASE_URL and ("localhost" not in settings.BACKEND_API_BASE_URL or "azurecontainerapps.io" not in settings.EUREKA_SERVER_URL):
                    candidate_urls.append(f"{settings.BACKEND_API_BASE_URL.rstrip('/')}/applications/{app_id}/ai-insights")

                # Cloud auto-discovery fallbacks
                if "azurecontainerapps.io" in settings.EUREKA_SERVER_URL:
                    env_domain = settings.EUREKA_SERVER_URL.split("discovery-server.")[-1].split("/")[0]
                    candidate_urls.append(f"https://api-gateway.{env_domain}/api/v1/applications/{app_id}/ai-insights")
                    candidate_urls.append(f"https://application-service.internal.{env_domain}/api/v1/applications/{app_id}/ai-insights")
                    candidate_urls.append(f"http://application-service/api/v1/applications/{app_id}/ai-insights")

                if not candidate_urls:
                    candidate_urls = [
                        f"http://localhost:8084/api/v1/applications/{app_id}/ai-insights",
                        f"http://localhost:8080/api/v1/applications/{app_id}/ai-insights"
                    ]
                seen_target_urls = set()
                for target_url in candidate_urls:
                    if not target_url or target_url in seen_target_urls:
                        continue
                    seen_target_urls.add(target_url)
                    try:
                        with httpx.Client(timeout=10.0) as client:
                            resp = client.put(target_url, json=insights_payload)
                            if resp.status_code in [200, 204]:
                                logger.info(f"Successfully saved AI insights to application-service via {target_url}")
                                saved_to_backend = True
                                break
                    except Exception as http_err:
                        logger.warning(f"Could not reach {target_url}: {http_err}")

                if not saved_to_backend:
                    logger.error(f"Failed to persist insights to application-service for application {app_id}")

            except Exception as match_err:
                logger.error(f"Error during applicant match computation: {match_err}", exc_info=True)
            finally:
                db.close()

            ch.basic_ack(delivery_tag=method.delivery_tag)

        except Exception as e:
            logger.error(f"Error handling applicant match RabbitMQ message: {e}", exc_info=True)
            ch.basic_ack(delivery_tag=method.delivery_tag)


consumer_instance = RabbitMQConsumer()
