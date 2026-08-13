CREATE TABLE user_profiles (
  user_id VARCHAR(50) PRIMARY KEY,
  first_name VARCHAR(100) NOT NULL,
  last_name VARCHAR(100) NOT NULL,
  email VARCHAR(100) UNIQUE NOT NULL,
  phone VARCHAR(20),
  bio TEXT,
  profile_pic_url VARCHAR(255),
  user_type VARCHAR(20) NOT NULL,
  is_actively_looking BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE academic_records (
  record_id VARCHAR(50) PRIMARY KEY,
  user_id VARCHAR(50) NOT NULL,
  faculty VARCHAR(100),
  department VARCHAR(100),
  degree_program VARCHAR(100),
  semester INT,
  year INT,
  gpa DECIMAL(3,2),
  batch VARCHAR(10),
  CONSTRAINT fk_academic_user FOREIGN KEY (user_id) REFERENCES user_profiles(user_id) ON DELETE CASCADE
);

CREATE TABLE skills (
  skill_id VARCHAR(50) PRIMARY KEY,
  user_id VARCHAR(50) NOT NULL,
  skill_name VARCHAR(100) NOT NULL,
  skill_level VARCHAR(50),
  CONSTRAINT fk_skills_user FOREIGN KEY (user_id) REFERENCES user_profiles(user_id) ON DELETE CASCADE
);

CREATE TABLE resumes (
  resume_id VARCHAR(50) PRIMARY KEY,
  user_id VARCHAR(50) NOT NULL,
  file_url VARCHAR(255) NOT NULL,
  file_name VARCHAR(100) NOT NULL,
  is_primary BOOLEAN DEFAULT FALSE,
  uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_resumes_user FOREIGN KEY (user_id) REFERENCES user_profiles(user_id) ON DELETE CASCADE
);

CREATE TABLE job_preferences (
  preference_id VARCHAR(50) PRIMARY KEY,
  user_id VARCHAR(50) NOT NULL,
  job_role VARCHAR(100),
  location VARCHAR(100),
  job_type VARCHAR(50),
  CONSTRAINT fk_preferences_user FOREIGN KEY (user_id) REFERENCES user_profiles(user_id) ON DELETE CASCADE
);