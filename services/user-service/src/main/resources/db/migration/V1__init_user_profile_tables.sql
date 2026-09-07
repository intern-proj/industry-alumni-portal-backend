CREATE TABLE IF NOT EXISTS user_profiles (
  user_id VARCHAR(50) PRIMARY KEY,
  first_name VARCHAR(100) NOT NULL,
  last_name VARCHAR(100) NOT NULL,
  email VARCHAR(100) UNIQUE NOT NULL,
  phone VARCHAR(20),
  bio TEXT,
  profile_pic_url VARCHAR(255),
  user_role VARCHAR(50) NOT NULL DEFAULT 'STUDENT',
  account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  faculty VARCHAR(100),
  department VARCHAR(100),
  is_actively_looking BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS academic_records (
  record_id VARCHAR(50) PRIMARY KEY,
  user_id VARCHAR(50) NOT NULL,
  faculty VARCHAR(100),
  department VARCHAR(100),
  degree_program VARCHAR(100),
  semester INT,
  year INT,
  gpa DECIMAL(3,2),
  batch VARCHAR(10),
  transcript_url VARCHAR(255),
  CONSTRAINT fk_academic_user FOREIGN KEY (user_id) REFERENCES user_profiles(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS skills (
  skill_id VARCHAR(50) PRIMARY KEY,
  user_id VARCHAR(50) NOT NULL,
  skill_name VARCHAR(100) NOT NULL,
  skill_level VARCHAR(50),
  category VARCHAR(50),
  CONSTRAINT fk_skills_user FOREIGN KEY (user_id) REFERENCES user_profiles(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS resumes (
  resume_id VARCHAR(50) PRIMARY KEY,
  user_id VARCHAR(50) NOT NULL,
  file_url VARCHAR(255) NOT NULL,
  file_name VARCHAR(100) NOT NULL,
  is_primary BOOLEAN DEFAULT FALSE,
  uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_resumes_user FOREIGN KEY (user_id) REFERENCES user_profiles(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS job_preferences (
  preference_id VARCHAR(50) PRIMARY KEY,
  user_id VARCHAR(50) NOT NULL,
  job_role VARCHAR(100),
  location VARCHAR(100),
  job_type VARCHAR(50),
  CONSTRAINT fk_preferences_user FOREIGN KEY (user_id) REFERENCES user_profiles(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS speaker_profiles (
  speaker_id VARCHAR(50) PRIMARY KEY,
  user_id VARCHAR(50),
  name VARCHAR(100) NOT NULL,
  organization VARCHAR(150),
  designation VARCHAR(100),
  bio TEXT,
  contact_email VARCHAR(100),
  contact_phone VARCHAR(20),
  expertise_tags VARCHAR(255),
  profile_pic_url VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_speaker_user FOREIGN KEY (user_id) REFERENCES user_profiles(user_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS faculties (
  faculty_id VARCHAR(50) PRIMARY KEY,
  name VARCHAR(100) NOT NULL UNIQUE,
  code VARCHAR(20) UNIQUE,
  description TEXT
);

CREATE TABLE IF NOT EXISTS departments (
  department_id VARCHAR(50) PRIMARY KEY,
  faculty_id VARCHAR(50) NOT NULL,
  name VARCHAR(100) NOT NULL,
  code VARCHAR(20),
  CONSTRAINT fk_dept_faculty FOREIGN KEY (faculty_id) REFERENCES faculties(faculty_id) ON DELETE CASCADE
);