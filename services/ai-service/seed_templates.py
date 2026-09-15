import psycopg2

conn = psycopg2.connect('postgresql://user:root@localhost:5432/notification')
cur = conn.cursor()

templates = [
    (
        "AUTH_OTP_CODE",
        "Staff & Admin 2FA Security Passcode",
        "🔒 NSBM Security Verification Passcode: {{otpCode}}",
        """<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <style>
    body { font-family: 'Segoe UI', sans-serif; background: #f1f5f9; margin: 0; padding: 20px; color: #1e293b; }
    .container { max-width: 580px; margin: 0 auto; background: #fff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
    .header { background: linear-gradient(135deg, #1e3a8a, #0f172a); color: #fff; padding: 28px; text-align: center; }
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
      <p>We received a sign-in attempt to your official portal account. Use the following One-Time Security Passcode to complete authentication:</p>
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
    body { font-family: 'Segoe UI', sans-serif; background: #f1f5f9; margin: 0; padding: 20px; color: #1e293b; }
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
    <div class="header"><h1>Staff Invitation</h1></div>
    <div class="content">
      <p>Dear Faculty / Staff Colleague,</p>
      <p>You have been formally invited to join the NSBM Industry & Alumni Portal with the designated administrative operational role of:</p>
      <p style="text-align: center;"><span class="role-pill">{{role}}</span></p>
      <p>Please complete your credential setup and password creation by clicking the button below:</p>
      <a href="{{invitationLink}}" class="btn">Complete Staff Registration</a>
      <p style="font-size: 12px; color: #64748b; word-break: break-all;">Direct Link: {{invitationLink}}</p>
    </div>
    <div class="footer"><p>NSBM Green University • Academic Operations Directorate</p></div>
  </div>
</body>
</html>""",
        "Dispatched when an administrator invites a faculty coordinator or staff member."
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
    body { font-family: 'Segoe UI', sans-serif; background: #f1f5f9; margin: 0; padding: 20px; color: #1e293b; }
    .container { max-width: 580px; margin: 0 auto; background: #fff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
    .header { background: linear-gradient(135deg, #0f766e, #0d9488); color: #fff; padding: 28px; text-align: center; }
    .header h1 { margin: 0; font-size: 20px; font-weight: 700; }
    .content { padding: 32px 28px; line-height: 1.6; }
    .btn { display: block; width: fit-content; margin: 24px auto; background: #0d9488; color: #fff; text-decoration: none; padding: 12px 28px; border-radius: 10px; font-weight: 700; font-size: 14px; text-align: center; }
    .footer { background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 20px; text-align: center; font-size: 11px; color: #94a3b8; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header"><h1>Partnership Verified</h1></div>
    <div class="content">
      <p>Dear {{companyName}} Team,</p>
      <p>We are delighted to confirm that your institutional partnership with NSBM Green University has been officially approved.</p>
      <p>You may now access your Employer Portal to publish internship vacancies and connect with undergraduate talent.</p>
      <a href="{{loginUrl}}" class="btn">Access Partner Portal</a>
    </div>
    <div class="footer"><p>NSBM Industry Interaction Cell • Career Guidance Unit</p></div>
  </div>
</body>
</html>""",
        "Dispatched when faculty management approves an employer registration."
    ),
    (
        "VACANCY_APPROVED",
        "Job Vacancy Approved & Published",
        "✅ Vacancy Approved & Published: {{jobTitle}}",
        """<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <style>
    body { font-family: 'Segoe UI', sans-serif; background: #f1f5f9; margin: 0; padding: 20px; color: #1e293b; }
    .container { max-width: 580px; margin: 0 auto; background: #fff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
    .header { background: linear-gradient(135deg, #059669, #10b981); color: #fff; padding: 28px; text-align: center; }
    .header h1 { margin: 0; font-size: 20px; font-weight: 700; }
    .content { padding: 32px 28px; line-height: 1.6; }
    .btn { display: block; width: fit-content; margin: 24px auto; background: #059669; color: #fff; text-decoration: none; padding: 12px 28px; border-radius: 10px; font-weight: 700; font-size: 14px; text-align: center; }
    .footer { background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 20px; text-align: center; font-size: 11px; color: #94a3b8; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header"><h1>Vacancy Live to Students</h1></div>
    <div class="content">
      <p>Hello {{companyName}},</p>
      <p>Great news! Your submitted vacancy for <strong>{{jobTitle}}</strong> has been reviewed and approved by the Faculty Coordinator.</p>
      <a href="{{actionLink}}" class="btn">View Live Vacancy</a>
    </div>
    <div class="footer"><p>NSBM Industry & Alumni Portal • Placement Division</p></div>
  </div>
</body>
</html>""",
        "Dispatched when a coordinator reviews and approves an employer job posting."
    ),
    (
        "VACANCY_CHANGES_REQUESTED",
        "Job Vacancy Modification Request",
        "📝 Action Required: Changes Requested for {{jobTitle}}",
        """<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <style>
    body { font-family: 'Segoe UI', sans-serif; background: #f1f5f9; margin: 0; padding: 20px; color: #1e293b; }
    .container { max-width: 580px; margin: 0 auto; background: #fff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
    .header { background: linear-gradient(135deg, #4338ca, #6366f1); color: #fff; padding: 28px; text-align: center; }
    .header h1 { margin: 0; font-size: 20px; font-weight: 700; }
    .content { padding: 32px 28px; line-height: 1.6; }
    .feedback-box { background: #eef2ff; border-left: 4px solid #6366f1; padding: 16px; border-radius: 6px; margin: 16px 0; font-size: 13px; }
    .btn { display: block; width: fit-content; margin: 24px auto; background: #4f46e5; color: #fff; text-decoration: none; padding: 12px 28px; border-radius: 10px; font-weight: 700; font-size: 14px; text-align: center; }
    .footer { background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 20px; text-align: center; font-size: 11px; color: #94a3b8; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header"><h1>Modifications Requested</h1></div>
    <div class="content">
      <p>Hello {{companyName}},</p>
      <p>The Faculty Coordinator reviewed your vacancy for <strong>{{jobTitle}}</strong> and requested adjustments:</p>
      <div class="feedback-box"><strong>Coordinator Feedback:</strong><br/>{{modificationNotes}}</div>
      <a href="{{actionLink}}" class="btn">Edit & Resubmit Post</a>
    </div>
    <div class="footer"><p>NSBM Industry & Alumni Portal • Placement Division</p></div>
  </div>
</body>
</html>""",
        "Dispatched when a coordinator requests edits or clarification before approving a job post."
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
    body { font-family: 'Segoe UI', sans-serif; background: #f1f5f9; margin: 0; padding: 20px; color: #1e293b; }
    .container { max-width: 580px; margin: 0 auto; background: #fff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
    .header { background: linear-gradient(135deg, #be123c, #e11d48); color: #fff; padding: 28px; text-align: center; }
    .header h1 { margin: 0; font-size: 20px; font-weight: 700; }
    .content { padding: 32px 28px; line-height: 1.6; }
    .note-box { background: #fff1f2; border-left: 4px solid #e11d48; padding: 16px; border-radius: 6px; margin: 16px 0; font-size: 13px; color: #881337; }
    .footer { background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 20px; text-align: center; font-size: 11px; color: #94a3b8; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header"><h1>Vacancy Review Notice</h1></div>
    <div class="content">
      <p>Hello {{companyName}},</p>
      <p>Your vacancy submission for <strong>{{jobTitle}}</strong> could not be approved at this time.</p>
      <div class="note-box"><strong>Review Notes:</strong><br/>{{rejectionReason}}</div>
    </div>
    <div class="footer"><p>NSBM Industry & Alumni Portal • Placement Division</p></div>
  </div>
</body>
</html>""",
        "Dispatched when a coordinator determines a vacancy does not meet posting guidelines."
    ),
    (
        "EVENT_INVITATION",
        "University Event & Summit Invitation",
        "🎟️ You're Invited: {{eventName}} — NSBM Green University",
        """<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <style>
    body { font-family: 'Segoe UI', sans-serif; background: #f1f5f9; margin: 0; padding: 20px; color: #1e293b; }
    .container { max-width: 580px; margin: 0 auto; background: #fff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
    .header { background: linear-gradient(135deg, #4f46e5, #7c3aed); color: #fff; padding: 28px; text-align: center; }
    .header h1 { margin: 0; font-size: 20px; font-weight: 700; }
    .content { padding: 32px 28px; line-height: 1.6; }
    .meta-box { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 16px; margin: 20px 0; }
    .btn { display: block; width: fit-content; margin: 24px auto; background: #6366f1; color: #fff; text-decoration: none; padding: 12px 28px; border-radius: 10px; font-weight: 700; font-size: 14px; text-align: center; }
    .footer { background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 20px; text-align: center; font-size: 11px; color: #94a3b8; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header"><h1>Official Event Invitation</h1></div>
    <div class="content">
      <p>Dear Member,</p>
      <p>You have been cordially invited to participate in:</p>
      <div class="meta-box">
        <div><strong>Event:</strong> {{eventName}}</div>
        <div><strong>Date:</strong> {{eventDate}}</div>
        <div><strong>Venue:</strong> {{location}}</div>
      </div>
      <a href="{{rsvpLink}}" class="btn">Confirm Your RSVP</a>
    </div>
    <div class="footer"><p>NSBM Event Management & Alumni Engagement Cell</p></div>
  </div>
</body>
</html>""",
        "Dispatched when students or alumni are invited to participate in a campus or virtual event."
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
    body { font-family: 'Segoe UI', sans-serif; background: #f1f5f9; margin: 0; padding: 20px; color: #1e293b; }
    .container { max-width: 580px; margin: 0 auto; background: #fff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
    .header { background: linear-gradient(135deg, #0284c7, #0ea5e9); color: #fff; padding: 28px; text-align: center; }
    .header h1 { margin: 0; font-size: 20px; font-weight: 700; }
    .content { padding: 32px 28px; line-height: 1.6; }
    .btn { display: block; width: fit-content; margin: 24px auto; background: #0284c7; color: #fff; text-decoration: none; padding: 12px 28px; border-radius: 10px; font-weight: 700; font-size: 14px; text-align: center; }
    .footer { background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 20px; text-align: center; font-size: 11px; color: #94a3b8; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header"><h1>Upcoming Event Reminder</h1></div>
    <div class="content">
      <p>Hello,</p>
      <p>This is a quick reminder that <strong>{{eventName}}</strong> is scheduled for <strong>{{eventDate}}</strong> at <strong>{{location}}</strong>.</p>
      <a href="{{eventLink}}" class="btn">View Event Pass</a>
    </div>
    <div class="footer"><p>NSBM Event Management & Student Affairs</p></div>
  </div>
</body>
</html>""",
        "Dispatched 24 hours prior to an event for confirmed participants."
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
    body { font-family: 'Segoe UI', sans-serif; background: #f1f5f9; margin: 0; padding: 20px; color: #1e293b; }
    .container { max-width: 580px; margin: 0 auto; background: #fff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
    .header { background: linear-gradient(135deg, #15803d, #16a34a); color: #fff; padding: 28px; text-align: center; }
    .header h1 { margin: 0; font-size: 20px; font-weight: 700; }
    .content { padding: 32px 28px; line-height: 1.6; }
    .footer { background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 20px; text-align: center; font-size: 11px; color: #94a3b8; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header"><h1>Campus Announcement</h1></div>
    <div class="content">
      <h2>{{title}}</h2>
      <p>{{announcementBody}}</p>
      <p style="margin-top: 24px; font-weight: 600; color: #047857;">— {{senderName}}</p>
    </div>
    <div class="footer"><p>NSBM University Official Communications Network</p></div>
  </div>
</body>
</html>""",
        "Dispatched when administrators broadcast a university-wide or faculty alert."
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
    body { font-family: 'Segoe UI', sans-serif; background: #f1f5f9; margin: 0; padding: 20px; color: #1e293b; }
    .container { max-width: 580px; margin: 0 auto; background: #fff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
    .header { background: linear-gradient(135deg, #059669, #10b981); color: #fff; padding: 28px; text-align: center; }
    .header h1 { margin: 0; font-size: 20px; font-weight: 700; }
    .content { padding: 32px 28px; line-height: 1.6; }
    .btn { display: block; width: fit-content; margin: 24px auto; background: #059669; color: #fff; text-decoration: none; padding: 12px 28px; border-radius: 10px; font-weight: 700; font-size: 14px; text-align: center; }
    .footer { background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 20px; text-align: center; font-size: 11px; color: #94a3b8; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header"><h1>Profile Verified</h1></div>
    <div class="content">
      <p>Hello {{userName}},</p>
      <p>Congratulations! Your academic and extracurricular profile on the Industry Alumni Portal has been verified and approved.</p>
      <a href="{{portalUrl}}" class="btn">View Public Profile</a>
    </div>
    <div class="footer"><p>NSBM Industry & Alumni Portal • Student Career Registry</p></div>
  </div>
</body>
</html>""",
        "Dispatched when a student or alumni profile is verified by the coordinator."
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
    body { font-family: 'Segoe UI', sans-serif; background: #f1f5f9; margin: 0; padding: 20px; color: #1e293b; }
    .container { max-width: 580px; margin: 0 auto; background: #fff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.06); border: 1px solid #e2e8f0; }
    .header { background: linear-gradient(135deg, #7c3aed, #a855f7); color: #fff; padding: 28px; text-align: center; }
    .header h1 { margin: 0; font-size: 20px; font-weight: 700; }
    .content { padding: 32px 28px; line-height: 1.6; }
    .cert-box { background: #faf5ff; border: 2px dashed #a855f7; border-radius: 12px; padding: 20px; text-align: center; margin: 20px 0; }
    .btn { display: block; width: fit-content; margin: 24px auto; background: #7c3aed; color: #fff; text-decoration: none; padding: 12px 28px; border-radius: 10px; font-weight: 700; font-size: 14px; text-align: center; }
    .footer { background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 20px; text-align: center; font-size: 11px; color: #94a3b8; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header"><h1>Certificate Issued</h1></div>
    <div class="content">
      <p>Dear {{studentName}},</p>
      <p>Congratulations on completing <strong>{{eventName}}</strong>!</p>
      <div class="cert-box">
        <p style="margin: 0; font-size: 13px; color: #6b21a8;">Verification Code:</p>
        <p style="margin: 4px 0 0 0; font-family: monospace; font-size: 18px; font-weight: 800; color: #581c87;">{{verificationCode}}</p>
      </div>
      <a href="{{downloadUrl}}" class="btn">Download Certificate</a>
    </div>
    <div class="footer"><p>NSBM University Examination & Certification Board</p></div>
  </div>
</body>
</html>""",
        "Dispatched when a student is awarded an institutional digital certificate."
    )
]

for code, name, subject, body, desc in templates:
    cur.execute('SELECT id FROM notification_templates WHERE template_code = %s;', (code,))
    row = cur.fetchone()
    if not row:
        cur.execute('''
            INSERT INTO notification_templates (template_code, name, subject, body, description, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, NOW(), NOW());
        ''', (code, name, subject, body, desc))
        print(f'Inserted template: {code}')
    else:
        print(f'Template already exists: {code}')

conn.commit()
cur.execute('SELECT count(*) FROM notification_templates;')
print('Total templates in DB now:', cur.fetchone()[0])
conn.close()
