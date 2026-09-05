@echo off
REM ==============================================================================
REM Batch wrapper for deploy-ai-service.ps1
REM ==============================================================================
cd /d "%~dp0.."
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0deploy-ai-service.ps1" %*
