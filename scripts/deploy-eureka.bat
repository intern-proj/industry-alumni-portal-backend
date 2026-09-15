@echo off
REM ==============================================================================
REM Batch wrapper for deploy-eureka.ps1
REM Bypasses PowerShell ExecutionPolicy restrictions automatically
REM ==============================================================================

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0deploy-eureka.ps1" %*
