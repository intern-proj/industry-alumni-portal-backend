@echo off
REM ==============================================================================
REM Batch wrapper for deploy-event-participation-service.ps1
REM ==============================================================================
cd /d "%~dp0.."
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0deploy-event-participation-service.ps1" %*
