#!/usr/bin/env python3
"""
==============================================================================
Admin Data Initializer for Industry Alumni Portal
Initializes / seeds:
  1. Email Notification Templates (in 'notification' database)
  2. Active SMTP Server Configuration (in 'notification' database)
  3. AI Model Presets with 8B Model Ready (in 'ai_service_db' database)
==============================================================================
"""

import sys
import uuid
import datetime
import psycopg2
from psycopg2.extras import execute_values

# Ensure clean UTF-8 encoding across platforms (e.g. Windows CMD/PowerShell)
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8")

# Default Azure PostgreSQL Configuration
PGHOST = "nicdbpgs.postgres.database.azure.com"
PGPORT = 5432
PGUSER = "pguser"
PGPASSWORD = "NicDB@123"

# ==============================================================================
# 1. SMTP Server Configuration Data
# ==============================================================================
SMTP_CONFIG = {
    "host": "smtp.gmail.com",
    "port": 587,
    "username": "otakuslimesgeneration@gmail.com",
    "password": "gzes urwv wnpy tixo",
    "sender_email": "otakuslimesgeneration@gmail.com",
    "sender_name": "NSBM Industry & Alumni Portal",
    "auth_enabled": True,
    "starttls_enabled": True,
    "ssl_enabled": False,
    "is_active": True
}

# ==============================================================================
# 2. AI Model Presets Data (Supports 4B, 7B, and 8B LLM variants)
# ==============================================================================
AI_MODEL_PRESETS = [
    {
        "id": "preset-gemini-35-flash",
        "config_name": "Google Gemini 3.5 Flash (Cloud API - Active)",
        "provider": "GEMINI_API",
        "model_name": "gemini-3.5-flash",
        "gpu_layers": 0,
        "context_size": 8192,
        "threads": 4,
        "temperature": 0.2,
        "azure_endpoint": "https://generativelanguage.googleapis.com/v1beta",
        "azure_deployment_name": "gemini-3.5-flash",
        "is_active": True
    },
    {
        "id": "preset-llama3-8b-cpu",
        "config_name": "Meta Llama 3.1 8B Instruct (Quantized CPU)",
        "provider": "LOCAL_GGUF",
        "model_name": "Meta Llama 3.1 8B Instruct",
        "repo_id": "bartowski/Meta-Llama-3.1-8B-Instruct-GGUF",
        "filename": "Meta-Llama-3.1-8B-Instruct-Q3_K_M.gguf",
        "gpu_layers": 0,
        "context_size": 4096,
        "threads": 4,
        "temperature": 0.2,
        "is_active": False
    },
    {
        "id": "preset-qwen3-4b-cpu",
        "config_name": "Qwen 3 4B Instruct (Fast CPU)",
        "provider": "LOCAL_GGUF",
        "model_name": "Qwen 3 4B Instruct",
        "repo_id": "lmstudio-community/Qwen3-4B-Instruct-2507-GGUF",
        "filename": "Qwen3-4B-Instruct-2507-Q4_K_M.gguf",
        "gpu_layers": 0,
        "context_size": 4096,
        "threads": 4,
        "temperature": 0.2,
        "is_active": False
    },
    {
        "id": "preset-qwen25-7b-cpu",
        "config_name": "Qwen 2.5 7B Instruct (Balanced CPU)",
        "provider": "LOCAL_GGUF",
        "model_name": "Qwen 2.5 7B Instruct",
        "repo_id": "Qwen/Qwen2.5-7B-Instruct-GGUF",
        "filename": "qwen2.5-7b-instruct-q3_k_m.gguf",
        "gpu_layers": 0,
        "context_size": 4096,
        "threads": 4,
        "temperature": 0.2,
        "is_active": False
    },
    {
        "id": "preset-azure-openai-4o-mini",
        "config_name": "Azure OpenAI GPT-4o Mini (Enterprise Cloud)",
        "provider": "AZURE_OPENAI",
        "model_name": "gpt-4o-mini",
        "gpu_layers": 0,
        "context_size": 8192,
        "threads": 4,
        "temperature": 0.2,
        "azure_endpoint": "https://your-resource-name.openai.azure.com/",
        "azure_deployment_name": "gpt-4o-mini",
        "is_active": False
    },
    {
        "id": "preset-openai-compatible-vllm",
        "config_name": "Self-Hosted vLLM / Ollama Endpoint",
        "provider": "OPENAI_COMPATIBLE",
        "model_name": "llama-3.1-8b",
        "gpu_layers": 0,
        "context_size": 4096,
        "threads": 4,
        "temperature": 0.2,
        "azure_endpoint": "http://localhost:11434/v1",
        "is_active": False
    }
]

# ==============================================================================
# 3. Email Notification Templates Data
# ==============================================================================
EMAIL_TEMPLATES = [
    (
        "AUTH_OTP_CODE",
        "Staff & Admin 2FA Security Passcode",
        "🔒 NSBM Security Verification Passcode: {{otpCode}}",
        """<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <style>
    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f1f5f9; margin: 0; padding: 20px; color: #1e293b; }
    .container { max-width: 580px; margin: 0 auto; background: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
    .header { background: linear-gradient(135deg, #1e3a8a, #0f172a); color: #ffffff; padding: 28px; text-align: center; }
    .header h1 { margin: 0; font-size: 20px; font-weight: 700; }
    .content { padding: 32px 28px; }
    .otp-box { background: #eff6ff; border: 2px dashed #3b82f6; border-radius: 12px; padding: 20px; text-align: center; margin: 24px 0; }
    .otp-val { font-size: 36px; font-weight: 800; letter-spacing: 8px; color: #1d4ed8; font-family: monospace; }
    .expiry { font-size: 12px; color: #64748b; margin-top: 8px; }
    .footer { background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 20px; text-align: center; font-size: 11px; color: #94a3b8; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header"><h1>NSBM Security Verification</h1></div>
    <div class="content">
      <p>Hello User,</p>
      <p>We received a sign-in attempt to your portal account. Use the following One-Time Security Passcode to complete authentication:</p>
      <div class="otp-box">
        <div class="otp-val">{{otpCode}}</div>
        <div class="expiry">Expires in 10 minutes • Do not share this code with anyone</div>
      </div>
      <p style="font-size: 13px; color: #64748b;">If you did not initiate this request, please notify University Information Security immediately.</p>
    </div>
    <div class="footer"><p>NSBM Industry & Alumni Portal • Automated Security Delivery</p></div>
  </div>
</body>
</html>""",
        "Dispatched during staff or administrative login for mandatory multi-factor authentication."
    ),
    (
        "STAFF_INVITATION",
        "Staff & Faculty Onboarding Invitation",
        "Official Invitation to Join NSBM Industry & Alumni Portal",
        """<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <style>
    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #f1f5f9; margin: 0; padding: 20px; color: #1e293b; }
    .container { max-width: 580px; margin: 0 auto; background: #fff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
    .header { background: linear-gradient(135deg, #065f46, #047857); color: #fff; padding: 28px; text-align: center; }
    .header h1 { margin: 0; font-size: 20px; font-weight: 700; }
    .content { padding: 32px 28px; line-height: 1.6; }
    .role-pill { display: inline-block; background: #ecfdf5; color: #047857; border: 1px solid #a7f3d0; padding: 4px 12px; border-radius: 9999px; font-weight: 700; font-size: 12px; }
    .btn { display: block; width: fit-content; margin: 24px auto; background: #059669; color: #fff; text-decoration: none; padding: 12px 28px; border-radius: 10px; font-weight: 700; font-size: 14px; text-align: center; }
    .footer { background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 20px; text-align: center; font-size: 11px; color: #94a3b8; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header"><h1>Welcome to NSBM Faculty & Staff Network</h1></div>
    <div class="content">
      <p>Dear Colleague,</p>
      <p>You have been formally invited to join the <strong>NSBM Industry & Alumni Collaboration Portal</strong> in the following official capacity:</p>
      <div style="text-align: center; margin: 16px 0;"><span class="role-pill">{{assignedRole}}</span></div>
      <p>Click below to initialize your university account and configure your credentials:</p>
      <a href="{{activationUrl}}" class="btn">Activate Staff Portal Account</a>
      <p style="font-size: 12px; color: #64748b;">This invitation expires in 48 hours.</p>
    </div>
    <div class="footer"><p>NSBM Green University • Industry Collaboration & Career Guidance Unit</p></div>
  </div>
</body>
</html>""",
        "Sent when a system administrator invites new academic or executive staff."
    ),
    (
        "PARTNER_REGISTRATION_APPROVED",
        "Corporate Partner Registration Approval",
        "Welcome to NSBM Corporate Network - Partnership Approved",
        """<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <style>
    body { font-family: 'Segoe UI', sans-serif; background: #f8fafc; margin: 0; padding: 20px; color: #0f172a; }
    .container { max-width: 580px; margin: 0 auto; background: #fff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
    .header { background: linear-gradient(135deg, #0284c7, #0369a1); color: #fff; padding: 28px; text-align: center; }
    .content { padding: 32px 28px; line-height: 1.6; }
    .btn { display: block; width: fit-content; margin: 24px auto; background: #0284c7; color: #fff; text-decoration: none; padding: 12px 28px; border-radius: 10px; font-weight: 700; text-align: center; }
    .footer { background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 20px; text-align: center; font-size: 11px; color: #94a3b8; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header"><h2>Corporate Partnership Approved</h2></div>
    <div class="content">
      <p>Dear {{companyName}} Recruitment Team,</p>
      <p>Congratulations! Your corporate registration with NSBM Industry & Alumni Portal has been reviewed and approved by the University Collaboration Unit.</p>
      <p>You can now log in, post internships and graduate vacancies, and discover verified NSBM student talent:</p>
      <a href="{{portalUrl}}" class="btn">Access Corporate Portal</a>
    </div>
    <div class="footer"><p>NSBM Industry Collaboration Unit • Connecting Academia with Industry</p></div>
  </div>
</body>
</html>""",
        "Sent to corporate partners upon administrative approval."
    ),
    (
        "VACANCY_APPROVED",
        "Job Vacancy Approved & Published",
        "🎉 Vacancy Approved & Published: {{jobTitle}}",
        """<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <style>
    body { font-family: 'Segoe UI', sans-serif; background: #f8fafc; margin: 0; padding: 20px; color: #0f172a; }
    .container { max-width: 580px; margin: 0 auto; background: #fff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
    .header { background: linear-gradient(135deg, #059669, #047857); color: #fff; padding: 28px; text-align: center; }
    .content { padding: 32px 28px; line-height: 1.6; }
    .footer { background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 20px; text-align: center; font-size: 11px; color: #94a3b8; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header"><h2>Vacancy Approved & Live</h2></div>
    <div class="content">
      <p>Dear {{partnerName}},</p>
      <p>Your vacancy posting for <strong>{{jobTitle}}</strong> has been approved and is now visible to all eligible NSBM students and alumni.</p>
      <p>Our smart AI matching engine will begin indexing candidate resumes and notifying matching students.</p>
    </div>
    <div class="footer"><p>NSBM Career Guidance Unit • Automated Opportunity Broadcast</p></div>
  </div>
</body>
</html>""",
        "Sent when a posted job vacancy is approved by platform administrators."
    ),
    (
        "VACANCY_CHANGES_REQUESTED",
        "Job Vacancy Modification Request",
        "⚠️ Action Required: Changes Requested for {{jobTitle}}",
        """<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <style>
    body { font-family: 'Segoe UI', sans-serif; background: #f8fafc; margin: 0; padding: 20px; color: #0f172a; }
    .container { max-width: 580px; margin: 0 auto; background: #fff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
    .header { background: linear-gradient(135deg, #d97706, #b45309); color: #fff; padding: 28px; text-align: center; }
    .content { padding: 32px 28px; line-height: 1.6; }
    .comment-box { background: #fef3c7; border-left: 4px solid #f59e0b; padding: 14px; margin: 16px 0; border-radius: 4px; }
    .footer { background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 20px; text-align: center; font-size: 11px; color: #94a3b8; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header"><h2>Changes Requested</h2></div>
    <div class="content">
      <p>Dear {{partnerName}},</p>
      <p>The University Review Committee has reviewed your posting for <strong>{{jobTitle}}</strong> and requested the following modifications:</p>
      <div class="comment-box"><p style="margin: 0;">{{adminFeedback}}</p></div>
      <p>Please log in to update and resubmit the vacancy for immediate approval.</p>
    </div>
    <div class="footer"><p>NSBM Industry Collaboration Unit</p></div>
  </div>
</body>
</html>""",
        "Sent when changes are requested on a job vacancy."
    ),
    (
        "VACANCY_REJECTED",
        "Job Vacancy Review Notice (Rejected)",
        "❌ Vacancy Review Notice: {{jobTitle}}",
        """<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <style>
    body { font-family: 'Segoe UI', sans-serif; background: #f8fafc; margin: 0; padding: 20px; color: #0f172a; }
    .container { max-width: 580px; margin: 0 auto; background: #fff; border-radius: 16px; overflow: hidden; border: 1px solid #e2e8f0; }
    .header { background: linear-gradient(135deg, #dc2626, #b91c1c); color: #fff; padding: 28px; text-align: center; }
    .content { padding: 32px 28px; line-height: 1.6; }
    .footer { background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 20px; text-align: center; font-size: 11px; color: #94a3b8; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header"><h2>Vacancy Review Notice</h2></div>
    <div class="content">
      <p>Dear {{partnerName}},</p>
      <p>We regret to inform you that your posting <strong>{{jobTitle}}</strong> could not be approved at this time.</p>
      <p><strong>Reason:</strong> {{rejectionReason}}</p>
    </div>
    <div class="footer"><p>NSBM Industry Collaboration Unit</p></div>
  </div>
</body>
</html>""",
        "Sent when a posted vacancy is declined."
    ),
    (
        "EVENT_INVITATION",
        "University Event & Summit Invitation",
        "📅 You're Invited: {{eventName}} — NSBM Green University",
        """<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <style>
    body { font-family: 'Segoe UI', sans-serif; background: #f8fafc; margin: 0; padding: 20px; color: #0f172a; }
    .container { max-width: 580px; margin: 0 auto; background: #fff; border-radius: 16px; overflow: hidden; border: 1px solid #e2e8f0; }
    .header { background: linear-gradient(135deg, #4f46e5, #4338ca); color: #fff; padding: 28px; text-align: center; }
    .content { padding: 32px 28px; line-height: 1.6; }
    .btn { display: block; width: fit-content; margin: 24px auto; background: #4f46e5; color: #fff; text-decoration: none; padding: 12px 28px; border-radius: 10px; font-weight: 700; text-align: center; }
    .footer { background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 20px; text-align: center; font-size: 11px; color: #94a3b8; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header"><h2>NSBM Official Event Invitation</h2></div>
    <div class="content">
      <p>Hello {{userName}},</p>
      <p>You are cordially invited to attend <strong>{{eventName}}</strong> organized by NSBM Green University.</p>
      <p><strong>Date & Time:</strong> {{eventDate}}<br><strong>Venue:</strong> {{eventVenue}}</p>
      <a href="{{eventUrl}}" class="btn">Register & Reserve Seat</a>
    </div>
    <div class="footer"><p>NSBM Event & Summit Management</p></div>
  </div>
</body>
</html>""",
        "Sent to students and partners inviting them to university events."
    ),
    (
        "EVENT_REMINDER",
        "Event Countdown & Upcoming Reminder",
        "⏰ Reminder: {{eventName}} starts tomorrow!",
        """<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <style>
    body { font-family: 'Segoe UI', sans-serif; background: #f8fafc; margin: 0; padding: 20px; color: #0f172a; }
    .container { max-width: 580px; margin: 0 auto; background: #fff; border-radius: 16px; overflow: hidden; border: 1px solid #e2e8f0; }
    .header { background: linear-gradient(135deg, #7c3aed, #6d28d9); color: #fff; padding: 28px; text-align: center; }
    .content { padding: 32px 28px; line-height: 1.6; }
    .footer { background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 20px; text-align: center; font-size: 11px; color: #94a3b8; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header"><h2>Event Starting Tomorrow</h2></div>
    <div class="content">
      <p>Hello {{userName}},</p>
      <p>This is a reminder that <strong>{{eventName}}</strong> is taking place tomorrow at <strong>{{eventVenue}}</strong>.</p>
      <p>Please present your registration QR code at the entrance for automated attendance verification.</p>
    </div>
    <div class="footer"><p>NSBM Event Management Unit</p></div>
  </div>
</body>
</html>""",
        "Automated countdown reminder for registered attendees."
    ),
    (
        "CAMPUS_ANNOUNCEMENT",
        "Campus Announcement Broadcast",
        "📢 NSBM University Announcement: {{title}}",
        """<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <style>
    body { font-family: 'Segoe UI', sans-serif; background: #f8fafc; margin: 0; padding: 20px; color: #0f172a; }
    .container { max-width: 580px; margin: 0 auto; background: #fff; border-radius: 16px; overflow: hidden; border: 1px solid #e2e8f0; }
    .header { background: linear-gradient(135deg, #0f172a, #1e293b); color: #fff; padding: 28px; text-align: center; }
    .content { padding: 32px 28px; line-height: 1.6; }
    .footer { background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 20px; text-align: center; font-size: 11px; color: #94a3b8; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header"><h2>Official University Broadcast</h2></div>
    <div class="content">
      <p>{{announcementBody}}</p>
    </div>
    <div class="footer"><p>NSBM Green University • Industry & Alumni Portal Broadcast</p></div>
  </div>
</body>
</html>""",
        "Broadcasting platform announcements to campus community."
    ),
    (
        "PROFILE_APPROVED",
        "Candidate Profile Verification & Activation",
        "✅ Profile Verified & Approved — Industry Alumni Portal",
        """<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <style>
    body { font-family: 'Segoe UI', sans-serif; background: #f8fafc; margin: 0; padding: 20px; color: #0f172a; }
    .container { max-width: 580px; margin: 0 auto; background: #fff; border-radius: 16px; overflow: hidden; border: 1px solid #e2e8f0; }
    .header { background: linear-gradient(135deg, #059669, #10b981); color: #fff; padding: 28px; text-align: center; }
    .content { padding: 32px 28px; line-height: 1.6; }
    .btn { display: block; width: fit-content; margin: 24px auto; background: #059669; color: #fff; text-decoration: none; padding: 12px 28px; border-radius: 10px; font-weight: 700; text-align: center; }
    .footer { background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 20px; text-align: center; font-size: 11px; color: #94a3b8; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header"><h2>Profile Verified & Active</h2></div>
    <div class="content">
      <p>Hello {{userName}},</p>
      <p>Your academic profile and credentials have been verified by NSBM Career Guidance Unit.</p>
      <p>Your resume is now indexed for AI matching with top tier industry vacancies and internship opportunities.</p>
      <a href="{{portalUrl}}" class="btn">View Candidate Dashboard</a>
    </div>
    <div class="footer"><p>NSBM Career Services Unit</p></div>
  </div>
</body>
</html>""",
        "Notifies students when their academic profile verification is approved."
    ),
    (
        "CERTIFICATE_ISSUED",
        "Digital Achievement Certificate Issued",
        "🎓 Congratulations: Your Certificate for {{eventName}} is Ready!",
        """<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <style>
    body { font-family: 'Segoe UI', sans-serif; background: #f8fafc; margin: 0; padding: 20px; color: #0f172a; }
    .container { max-width: 580px; margin: 0 auto; background: #fff; border-radius: 16px; overflow: hidden; border: 1px solid #e2e8f0; }
    .header { background: linear-gradient(135deg, #b45309, #d97706); color: #fff; padding: 28px; text-align: center; }
    .content { padding: 32px 28px; line-height: 1.6; }
    .cert-box { background: #fef3c7; border: 2px dashed #f59e0b; border-radius: 12px; padding: 18px; text-align: center; margin: 20px 0; }
    .btn { display: block; width: fit-content; margin: 24px auto; background: #b45309; color: #fff; text-decoration: none; padding: 12px 28px; border-radius: 10px; font-weight: 700; text-align: center; }
    .footer { background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 20px; text-align: center; font-size: 11px; color: #94a3b8; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header"><h2>Certificate of Achievement</h2></div>
    <div class="content">
      <p>Congratulations {{recipientName}},</p>
      <p>Your digital certificate of participation for <strong>{{eventName}}</strong> has been signed and issued.</p>
      <div class="cert-box">
        <p style="margin: 0; font-weight: 700; color: #92400e;">Certificate ID: {{certificateId}}</p>
        <p style="margin: 4px 0 0; font-size: 12px; color: #b45309;">Digitally Verified via QR Blockchain Ledger</p>
      </div>
      <a href="{{downloadUrl}}" class="btn">Download Official Certificate</a>
    </div>
    <div class="footer"><p>NSBM Green University • Digital Credentials Unit</p></div>
  </div>
</body>
</html>""",
        "Sent when an event participation certificate is generated and issued."
    )
]


def seed_notification_database():
    print("\n=======================================================")
    print(" [1/2] Initializing Notification Database ('notification')")
    print("=======================================================")

    conn = psycopg2.connect(
        host=PGHOST,
        port=PGPORT,
        user=PGUSER,
        password=PGPASSWORD,
        dbname="notification",
        sslmode="require"
    )
    cur = conn.cursor()

    # 1. Seed / Upsert Email Notification Templates
    print("\n>>> Seeding / Updating Email Notification Templates...")
    template_upsert_query = """
        INSERT INTO notification_templates (template_code, name, subject, body, description, created_at, updated_at)
        VALUES (%s, %s, %s, %s, %s, NOW(), NOW())
        ON CONFLICT (template_code) DO UPDATE
        SET name = EXCLUDED.name,
            subject = EXCLUDED.subject,
            body = EXCLUDED.body,
            description = EXCLUDED.description,
            updated_at = NOW();
    """

    for code, name, subject, body, desc in EMAIL_TEMPLATES:
        cur.execute(template_upsert_query, (code, name, subject, body, desc))
        print(f"  [+] Upserted Template: {code} ({name})")

    conn.commit()

    # 2. Seed Active SMTP Server Configuration
    print("\n>>> Checking SMTP Server Configuration...")
    cur.execute("SELECT count(*) FROM smtp_configurations")
    count = cur.fetchone()[0]

    if count == 0:
        print("  [*] No SMTP configuration found. Inserting active Gmail SMTP settings...")
        cur.execute("""
            INSERT INTO smtp_configurations (
                host, port, username, password, sender_email, sender_name,
                auth_enabled, starttls_enabled, ssl_enabled, is_active, created_at, updated_at
            ) VALUES (
                %(host)s, %(port)s, %(username)s, %(password)s, %(sender_email)s, %(sender_name)s,
                %(auth_enabled)s, %(starttls_enabled)s, %(ssl_enabled)s, %(is_active)s, NOW(), NOW()
            )
        """, SMTP_CONFIG)
        conn.commit()
        print(f"  [+] Seeded active SMTP server: {SMTP_CONFIG['host']}:{SMTP_CONFIG['port']} (Sender: {SMTP_CONFIG['sender_email']})")
    else:
        print(f"  [i] Found {count} existing SMTP configuration(s). Updating active flag...")
        cur.execute("UPDATE smtp_configurations SET is_active = TRUE WHERE is_active = TRUE")
        conn.commit()

    cur.close()
    conn.close()
    print("  [OK] Notification database initialization complete.")


def seed_ai_service_database():
    print("\n=======================================================")
    print(" [2/2] Initializing AI Service Database ('ai_service_db')")
    print("=======================================================")

    conn = psycopg2.connect(
        host=PGHOST,
        port=PGPORT,
        user=PGUSER,
        password=PGPASSWORD,
        dbname="ai_service_db",
        sslmode="require"
    )
    cur = conn.cursor()

    # 1. Ensure table exists
    cur.execute("""
        CREATE TABLE IF NOT EXISTS ai_model_configs (
            id VARCHAR(36) PRIMARY KEY,
            config_name VARCHAR(100) NOT NULL,
            provider VARCHAR(50) DEFAULT 'LOCAL_GGUF',
            model_name VARCHAR(200) NOT NULL,
            repo_id VARCHAR(255),
            filename VARCHAR(255),
            gpu_layers INTEGER DEFAULT 0,
            context_size INTEGER DEFAULT 4096,
            threads INTEGER DEFAULT 4,
            temperature FLOAT DEFAULT 0.2,
            azure_endpoint VARCHAR(500),
            azure_api_key VARCHAR(255),
            azure_deployment_name VARCHAR(100),
            is_active BOOLEAN DEFAULT FALSE,
            created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
        );
    """)
    conn.commit()

    # 2. Seed AI Model Presets
    print("\n>>> Seeding AI Model Presets (8B Model Ready)...")
    cur.execute("SELECT count(*) FROM ai_model_configs")
    count = cur.fetchone()[0]

    upsert_model_query = """
        INSERT INTO ai_model_configs (
            id, config_name, provider, model_name, repo_id, filename,
            gpu_layers, context_size, threads, temperature, azure_endpoint, azure_deployment_name,
            is_active, created_at, updated_at
        ) VALUES (
            %(id)s, %(config_name)s, %(provider)s, %(model_name)s, %(repo_id)s, %(filename)s,
            %(gpu_layers)s, %(context_size)s, %(threads)s, %(temperature)s, %(azure_endpoint)s, %(azure_deployment_name)s,
            %(is_active)s, NOW(), NOW()
        )
        ON CONFLICT (id) DO UPDATE
        SET config_name = EXCLUDED.config_name,
            provider = EXCLUDED.provider,
            model_name = EXCLUDED.model_name,
            repo_id = EXCLUDED.repo_id,
            filename = EXCLUDED.filename,
            context_size = EXCLUDED.context_size,
            threads = EXCLUDED.threads,
            temperature = EXCLUDED.temperature,
            updated_at = NOW();
    """

    for preset in AI_MODEL_PRESETS:
        cur.execute(upsert_model_query, {
            "id": preset.get("id"),
            "config_name": preset.get("config_name"),
            "provider": preset.get("provider"),
            "model_name": preset.get("model_name"),
            "repo_id": preset.get("repo_id"),
            "filename": preset.get("filename"),
            "gpu_layers": preset.get("gpu_layers", 0),
            "context_size": preset.get("context_size", 4096),
            "threads": preset.get("threads", 4),
            "temperature": preset.get("temperature", 0.2),
            "azure_endpoint": preset.get("azure_endpoint"),
            "azure_deployment_name": preset.get("azure_deployment_name"),
            "is_active": preset.get("is_active", False)
        })
        active_label = " [ACTIVE]" if preset.get("is_active") else ""
        print(f"  [+] Upserted AI Preset: {preset['config_name']} ({preset['model_name']}){active_label}")

    conn.commit()
    cur.close()
    conn.close()
    print("  [OK] AI Service database initialization complete.")


if __name__ == "__main__":
    print("=======================================================")
    print(" NSBM Industry & Alumni Portal - Admin Data Initializer")
    print(f" Target Azure PostgreSQL: {PGHOST}:{PGPORT}")
    print("=======================================================")

    try:
        seed_notification_database()
        seed_ai_service_database()

        print("\n=======================================================")
        print(" SUCCESS: ALL ADMIN DATA INITIALIZED SUCCESSFULLY!")
        print(" - 11 Email Templates Upserted")
        print(f" - Active SMTP Server: {SMTP_CONFIG['host']}:{SMTP_CONFIG['port']}")
        print(f" - 5 AI Model Presets Configured (Active: Meta Llama 3.1 8B)")
        print("=======================================================\n")
    except Exception as e:
        print(f"\n[!] ERROR during admin data initialization: {e}", file=sys.stderr)
        sys.exit(1)
