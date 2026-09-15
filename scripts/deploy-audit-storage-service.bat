@echo off
REM ==============================================================================
REM Batch wrapper for deploy-audit-storage-service.ps1
REM ==============================================================================
cd /d "%~dp0.."
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0deploy-audit-storage-service.ps1" %*
