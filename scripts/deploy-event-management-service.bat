@echo off
REM ==============================================================================
REM Batch wrapper for deploy-event-management-service.ps1
REM ==============================================================================
cd /d "%~dp0.."
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0deploy-event-management-service.ps1" %*
