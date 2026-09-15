@echo off
REM ==============================================================================
REM Batch wrapper for deploy-application-service.ps1
REM ==============================================================================
cd /d "%~dp0.."
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0deploy-application-service.ps1" %*
