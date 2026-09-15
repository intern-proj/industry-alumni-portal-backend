@echo off
REM ==============================================================================
REM Batch wrapper for deploy-api-gateway.ps1
REM ==============================================================================
cd /d "%~dp0.."
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0deploy-api-gateway.ps1" %*
