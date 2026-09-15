@echo off
REM ==============================================================================
REM Batch wrapper for deploy-discovery-server.ps1
REM ==============================================================================
cd /d "%~dp0.."
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0deploy-discovery-server.ps1" %*
