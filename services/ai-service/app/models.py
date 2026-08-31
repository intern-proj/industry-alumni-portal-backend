import uuid
from datetime import datetime
from sqlalchemy import Column, DateTime, Float, Integer, JSON, String, Text
from app.database import Base


class VacancyRecord(Base):
    __tablename__ = "job_vacancies"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    job_title = Column(String(255), nullable=False, index=True)
    company_name = Column(String(255), nullable=True, index=True)
    workplace_type = Column(String(50), nullable=True)
    min_experience_years = Column(Float, default=0.0)
    salary_raw = Column(String(255), nullable=True)
    
    # JSON-structured fields for PostgreSQL / SQLite
    locations = Column(JSON, default=list)
    required_skills = Column(JSON, default=list)
    preferred_skills = Column(JSON, default=list)
    responsibilities = Column(JSON, default=list)
    education_requirements = Column(JSON, default=list)
    contact_emails = Column(JSON, default=list)
    raw_extracted_payload = Column(JSON, nullable=False)
    
    source_image_url = Column(Text, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow)
