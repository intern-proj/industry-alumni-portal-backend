import uuid
from datetime import datetime
from sqlalchemy import Boolean, Column, DateTime, Float, Integer, JSON, String, Text
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


class ResumeEmbeddingCache(Base):
    __tablename__ = "resume_embedding_cache"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    resume_url = Column(String(500), unique=True, index=True, nullable=False)
    user_id = Column(String(100), nullable=True, index=True)
    extracted_skills = Column(JSON, default=list)
    semantic_profile_text = Column(Text, nullable=True)
    embedding_vector = Column(JSON, nullable=True)  # List of 384 floats
    target_role = Column(String(255), nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)


class VacancyEmbeddingCache(Base):
    __tablename__ = "vacancy_embedding_cache"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    vacancy_id = Column(String(100), unique=True, index=True, nullable=False)
    structured_text = Column(Text, nullable=True)
    embedding_vector = Column(JSON, nullable=True)  # List of 384 floats
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)


class CandidateProfileCache(Base):
    __tablename__ = "candidate_profile_cache"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String(100), unique=True, index=True, nullable=False)
    
    # LLM-extracted structured fields (merged from ALL resumes)
    extracted_skills = Column(JSON, default=list)        
    seniority_level = Column(String(50), nullable=True)  
    experience_years = Column(Float, default=0.0)
    target_roles = Column(JSON, default=list)             
    education = Column(Text, nullable=True)               
    faculty = Column(String(100), nullable=True)          
    certifications = Column(JSON, default=list)
    
    # Semantic profile
    semantic_profile_text = Column(Text, nullable=True)   
    embedding_vector = Column(JSON, nullable=True)        
    
    # Source tracking
    resume_urls = Column(JSON, default=list)              
    profile_skills = Column(JSON, default=list)           
    
    # Status
    status = Column(String(20), default="PENDING")        
    
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)


class AiModelConfig(Base):
    __tablename__ = "ai_model_configs"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    config_name = Column(String(100), nullable=False)
    provider = Column(String(50), default="LOCAL_GGUF")  # LOCAL_GGUF, AZURE_OPENAI, OPENAI_COMPATIBLE
    model_name = Column(String(200), nullable=False)
    repo_id = Column(String(255), nullable=True)
    filename = Column(String(255), nullable=True)
    gpu_layers = Column(Integer, default=0)
    context_size = Column(Integer, default=4096)
    threads = Column(Integer, default=4)
    temperature = Column(Float, default=0.2)
    azure_endpoint = Column(String(500), nullable=True)
    azure_api_key = Column(String(255), nullable=True)
    azure_deployment_name = Column(String(100), nullable=True)
    is_active = Column(Boolean, default=False)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

