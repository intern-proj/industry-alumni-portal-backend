@echo off
REM ==============================================================================
REM Batch wrapper to execute init-admin-data.py
REM Seeds email templates, active SMTP server, and AI model presets in Azure
REM ==============================================================================

python "%~dp0init-admin-data.py" %*
