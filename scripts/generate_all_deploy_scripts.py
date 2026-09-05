import os
from pathlib import Path

SERVICES = [
    {
        "name": "discovery-server",
        "file": "discovery-server/Dockerfile",
        "path": "discovery-server",
        "port": 8761,
        "ingress": "external",
        "cpu": "0.5",
        "mem": "1.0Gi",
        "is_java": True,
        "has_eureka": False,
        "is_ai": False
    },
    {
        "name": "api-gateway",
        "file": "api-gateway/Dockerfile",
        "path": "api-gateway",
        "port": 8080,
        "ingress": "external",
        "cpu": "0.5",
        "mem": "1.0Gi",
        "is_java": True,
        "has_eureka": True,
        "is_ai": False
    },
    {
        "name": "auth-service",
        "file": "services/auth-service/Dockerfile",
        "path": "services/auth-service",
        "port": 8081,
        "ingress": "internal",
        "cpu": "0.25",
        "mem": "0.5Gi",
        "is_java": True,
        "has_eureka": True,
        "is_ai": False
    },
    {
        "name": "event-management-service",
        "file": "services/event-management-service/Dockerfile",
        "path": "services/event-management-service",
        "port": 8082,
        "ingress": "internal",
        "cpu": "0.25",
        "mem": "0.5Gi",
        "is_java": True,
        "has_eureka": True,
        "is_ai": False
    },
    {
        "name": "event-participation-service",
        "file": "services/event-participation-service/Dockerfile",
        "path": "services/event-participation-service",
        "port": 8083,
        "ingress": "internal",
        "cpu": "0.25",
        "mem": "0.5Gi",
        "is_java": True,
        "has_eureka": True,
        "is_ai": False
    },
    {
        "name": "application-service",
        "file": "services/application-service/Dockerfile",
        "path": "services/application-service",
        "port": 8084,
        "ingress": "internal",
        "cpu": "0.25",
        "mem": "0.5Gi",
        "is_java": True,
        "has_eureka": True,
        "is_ai": False
    },
    {
        "name": "certificate-service",
        "file": "services/certificate-service/Dockerfile",
        "path": "services/certificate-service",
        "port": 8085,
        "ingress": "internal",
        "cpu": "0.25",
        "mem": "0.5Gi",
        "is_java": True,
        "has_eureka": True,
        "is_ai": False
    },
    {
        "name": "platform-management-service",
        "file": "services/platform-management-service/Dockerfile",
        "path": "services/platform-management-service",
        "port": 8086,
        "ingress": "internal",
        "cpu": "0.25",
        "mem": "0.5Gi",
        "is_java": True,
        "has_eureka": True,
        "is_ai": False
    },
    {
        "name": "vacancy-service",
        "file": "services/vacancy-service/Dockerfile",
        "path": "services/vacancy-service",
        "port": 8087,
        "ingress": "internal",
        "cpu": "0.25",
        "mem": "0.5Gi",
        "is_java": True,
        "has_eureka": True,
        "is_ai": False
    },
    {
        "name": "notification-service",
        "file": "services/notification-service/Dockerfile",
        "path": "services/notification-service",
        "port": 8088,
        "ingress": "internal",
        "cpu": "0.25",
        "mem": "0.5Gi",
        "is_java": True,
        "has_eureka": True,
        "is_ai": False
    },
    {
        "name": "audit-storage-service",
        "file": "services/audit-storage-service/Dockerfile",
        "path": "services/audit-storage-service",
        "port": 8089,
        "ingress": "internal",
        "cpu": "0.25",
        "mem": "0.5Gi",
        "is_java": True,
        "has_eureka": True,
        "is_ai": False
    },
    {
        "name": "user-service",
        "file": "services/user-service/Dockerfile",
        "path": "services/user-service",
        "port": 8090,
        "ingress": "internal",
        "cpu": "0.25",
        "mem": "0.5Gi",
        "is_java": True,
        "has_eureka": True,
        "is_ai": False
    },
    {
        "name": "ai-service",
        "file": "services/ai-service/Dockerfile",
        "path": "services/ai-service",
        "port": 8000,
        "ingress": "external",
        "cpu": "0.5",
        "mem": "1.0Gi",
        "is_java": False,
        "has_eureka": True,
        "is_ai": True
    }
]

SCRIPTS_DIR = Path(__file__).resolve().parent

def generate_ps1(svc):
    name = svc["name"]
    file = svc["file"]
    path = svc["path"]
    port = svc["port"]
    ingress = svc["ingress"]
    cpu = svc["cpu"]
    mem = svc["mem"]
    is_java = svc["is_java"]
    has_eureka = svc["has_eureka"]
    is_ai = svc["is_ai"]

    eureka_param = '    [string]$EurekaUrl = "",\n' if has_eureka else ''
    build_param = '    [switch]$BuildJar,\n' if is_java else ''
    ai_params = """    [string]$GeminiApiKey = "AIzaSyCwVuiV4796KTvQ8CFj2BBBQ-4z6WwJQAg",
    [string]$GeminiModel = "gemini-3.5-flash",\n""" if is_ai else ""

    eureka_discovery = f"""
# 2. Resolve Eureka Server URL
Write-Host "`n[2/4] Resolving Eureka Discovery Server URL..." -ForegroundColor Cyan
if (-not $EurekaUrl) {{
    $EurekaFqdn = az containerapp show --name "discovery-server" --resource-group $ResourceGroup --query "properties.configuration.ingress.fqdn" -o tsv 2>$null
    if ($EurekaFqdn) {{
        $EurekaUrl = "https://$EurekaFqdn/eureka/"
        Write-Host "Auto-discovered Eureka Server at: $EurekaUrl" -ForegroundColor Green
    }} else {{
        Write-Host "ERROR: Eureka Server ('discovery-server') is not deployed or accessible in $ResourceGroup." -ForegroundColor Red
        Write-Host "Please deploy discovery-server first: .\\scripts\\deploy-discovery-server.bat" -ForegroundColor Yellow
        exit 1
    }}
}} else {{
    Write-Host "Using specified Eureka URL: $EurekaUrl" -ForegroundColor Green
}}
""" if has_eureka else ""

    java_build_step = f"""
# 4. Check / Compile JAR with Maven
Write-Host "`n[4/5] Checking compiled JAR artifact for {name}..." -ForegroundColor Cyan
$JarExists = Get-ChildItem -Path "$ProjectRoot\\{path}\\target\\*.jar" -ErrorAction SilentlyContinue | Where-Object {{ $_.Name -notmatch "sources|original" }}
if ($BuildJar -or (-not $JarExists)) {{
    Write-Host "Building {name} with Maven..." -ForegroundColor Yellow
    if (Test-Path "$ProjectRoot\\mvnw.cmd") {{
        & "$ProjectRoot\\mvnw.cmd" package -DskipTests -pl "{path}" -am
    }} elseif (Get-Command mvn -ErrorAction SilentlyContinue) {{
        mvn package -DskipTests -pl "{path}" -am
    }} else {{
        Write-Host "ERROR: Maven ('mvn') or Maven wrapper ('mvnw.cmd') not found." -ForegroundColor Red
        exit 1
    }}
    if ($LASTEXITCODE -ne 0) {{
        Write-Host "Maven build failed for {name}." -ForegroundColor Red
        exit 1
    }}
}} else {{
    Write-Host "Found precompiled JAR: $($JarExists.Name)" -ForegroundColor Green
}}
""" if is_java else ""

    if is_ai:
        env_setup = """
# Prepare environment variables
$EnvVars = @(
    "EUREKA_SERVER_URL=$EurekaUrl",
    "USE_GEMINI_API=true",
    "GEMINI_API_KEY=$GeminiApiKey",
    "GEMINI_MODEL=$GeminiModel"
)
"""
        create_env_str = "--env-vars $EnvVars"
        update_env_str = "--set-env-vars $EnvVars"
    elif has_eureka:
        env_setup = """
$EnvVars = @(
    "EUREKA_SERVER_URL=$EurekaUrl",
    "FRONTEND_URL=https://wonderful-wave-0320abf00.3.azurestaticapps.net",
    "API_GATEWAY_URL=https://api-gateway.happybush-76206934.centralindia.azurecontainerapps.io",
    "CERTIFICATE_BASE_URL=https://api-gateway.happybush-76206934.centralindia.azurecontainerapps.io"
)
"""
        create_env_str = "--env-vars $EnvVars"
        update_env_str = "--set-env-vars $EnvVars"
    else:
        env_setup = ""
        create_env_str = ""
        update_env_str = ""

    return f"""# ==============================================================================
# Deploy {name} to Azure Container Apps
# Port: {port} | Ingress: {ingress} | CPU: {cpu} | Memory: {mem}
# ==============================================================================

[CmdletBinding()]
param (
{eureka_param}{build_param}{ai_params}    [string]$Version = "v1",
    [string]$Registry = "nicregistery",
    [string]$ResourceGroup = "NIC_System",
    [string]$Environment = "nic-system-env"
)

$ErrorActionPreference = "Stop"

# Always ensure execution is rooted in the repository directory
$ProjectRoot = (Resolve-Path "$PSScriptRoot\\..").Path
Set-Location $ProjectRoot

# Ensure Azure CLI is discoverable in PATH
if (-not (Get-Command az -ErrorAction SilentlyContinue)) {{
    if (Test-Path "C:\\Program Files\\Microsoft SDKs\\Azure\\CLI2\\wbin\\az.cmd") {{
        $env:PATH += ";C:\\Program Files\\Microsoft SDKs\\Azure\\CLI2\\wbin"
    }} elseif (Test-Path "C:\\Program Files (x86)\\Microsoft SDKs\\Azure\\CLI2\\wbin\\az.cmd") {{
        $env:PATH += ";C:\\Program Files (x86)\\Microsoft SDKs\\Azure\\CLI2\\wbin"
    }} else {{
        Write-Host "ERROR: Azure CLI ('az') is not installed or not found." -ForegroundColor Red
        exit 1
    }}
}}

Write-Host "=========================================================" -ForegroundColor Cyan
Write-Host " DEPLOYING: {name.upper()}" -ForegroundColor Green
Write-Host " Registry:        $Registry.azurecr.io" -ForegroundColor Gray
Write-Host " Resource Group:  $ResourceGroup" -ForegroundColor Gray
Write-Host " Environment:     $Environment" -ForegroundColor Gray
Write-Host " Target Port:     {port} (Ingress: {ingress})" -ForegroundColor Gray
Write-Host " Resources:       {cpu} CPU | {mem} RAM" -ForegroundColor Gray
Write-Host "=========================================================" -ForegroundColor Cyan

# 1. Verify Azure CLI login
Write-Host "`n[1/4] Checking Azure authentication..." -ForegroundColor Cyan
$Account = az account show -o json 2>$null | ConvertFrom-Json
if (-not $Account) {{
    Write-Host "You are not logged into Azure CLI. Please run 'az login' first." -ForegroundColor Red
    exit 1
}}
Write-Host "Logged in as: $($Account.user.name)" -ForegroundColor Green
{eureka_discovery}
# 3. Authenticate Docker with Container Registry
Write-Host "`n[3/5] Authenticating Docker with Container Registry ($Registry.azurecr.io)..." -ForegroundColor Cyan
$AcrPassword = az acr credential show --name $Registry --query "passwords[0].value" -o tsv
if (-not $AcrPassword) {{
    Write-Host "Enabling admin credentials on $Registry..." -ForegroundColor Yellow
    az acr update --name $Registry --admin-enabled true | Out-Null
    $AcrPassword = az acr credential show --name $Registry --query "passwords[0].value" -o tsv
}}

$DockerAuthOk = $false
for ($attempt = 1; $attempt -le 3; $attempt++) {{
    $AcrPassword | docker login "$Registry.azurecr.io" -u $Registry --password-stdin
    if ($LASTEXITCODE -eq 0) {{
        $DockerAuthOk = $true
        break
    }}
    Write-Host "Docker login attempt $attempt failed, retrying in 3 seconds..." -ForegroundColor Yellow
    Start-Sleep -Seconds 3
}}
if (-not $DockerAuthOk) {{
    Write-Host "Docker login to $Registry.azurecr.io failed after 3 attempts." -ForegroundColor Red
    exit 1
}}
{java_build_step}
# 5. Build Image locally with Docker and Push to ACR
Write-Host "`n[5/5] Building {name} locally with Docker..." -ForegroundColor Cyan
docker build -t "$Registry.azurecr.io/{name}:$Version" -f "{file}" .
if ($LASTEXITCODE -ne 0) {{
    Write-Host "Docker build failed for {name}." -ForegroundColor Red
    exit 1
}}

Write-Host "Pushing image to $Registry.azurecr.io..." -ForegroundColor Cyan
$PushOk = $false
for ($attempt = 1; $attempt -le 3; $attempt++) {{
    docker push "$Registry.azurecr.io/{name}:$Version"
    if ($LASTEXITCODE -eq 0) {{
        $PushOk = $true
        break
    }}
    Write-Host "Push attempt $attempt failed, retrying in 3 seconds..." -ForegroundColor Yellow
    Start-Sleep -Seconds 3
}}
if (-not $PushOk) {{
    Write-Host "Failed to push image to ACR after 3 attempts." -ForegroundColor Red
    exit 1
}}
{env_setup}
# 6. Deploy / Update Container App
Write-Host "`nDeploying Container App '{name}' to Azure Container Apps..." -ForegroundColor Cyan
$AppExists = az containerapp list --resource-group $ResourceGroup --query "[?name=='{name}'].name" -o tsv

if (-not $AppExists) {{
    Write-Host "Creating Container App '{name}'..." -ForegroundColor Yellow
    az containerapp create `
        --name "{name}" `
        --resource-group $ResourceGroup `
        --environment $Environment `
        --image "$Registry.azurecr.io/{name}:$Version" `
        --registry-server "$Registry.azurecr.io" `
        --registry-username $Registry `
        --registry-password $AcrPassword `
        --target-port {port} `
        --ingress {ingress} `
        --min-replicas 1 `
        --cpu {cpu} `
        --memory {mem} {create_env_str}
}} else {{
    Write-Host "Updating Container App '{name}'..." -ForegroundColor Yellow
    az containerapp update `
        --name "{name}" `
        --resource-group $ResourceGroup `
        --image "$Registry.azurecr.io/{name}:$Version" `
        --cpu {cpu} `
        --memory {mem} {update_env_str}
}}

if ($LASTEXITCODE -ne 0) {{
    Write-Host "Failed to deploy {name}." -ForegroundColor Red
    exit 1
}}

$Fqdn = az containerapp show --name "{name}" --resource-group $ResourceGroup --query "properties.configuration.ingress.fqdn" -o tsv 2>$null

Write-Host "`n=========================================================" -ForegroundColor Green
Write-Host " {name.upper()} DEPLOYED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "=========================================================" -ForegroundColor Green
if ($Fqdn) {{
    Write-Host " Endpoint URL: https://$Fqdn" -ForegroundColor Yellow
}}
"""

def generate_bat(svc):
    name = svc["name"]
    return f"""@echo off
REM ==============================================================================
REM Batch wrapper for deploy-{name}.ps1
REM ==============================================================================
cd /d "%~dp0.."
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0deploy-{name}.ps1" %*
"""

def generate_sh(svc):
    name = svc["name"]
    file = svc["file"]
    path = svc["path"]
    port = svc["port"]
    ingress = svc["ingress"]
    cpu = svc["cpu"]
    mem = svc["mem"]
    is_java = svc["is_java"]
    has_eureka = svc["has_eureka"]
    is_ai = svc["is_ai"]

    eureka_discovery = f"""
# 2. Resolve Eureka Server URL
if [ -z "$EUREKA_URL" ]; then
    echo "Resolving Eureka Discovery Server URL..."
    EUREKA_FQDN=$(az containerapp show --name "discovery-server" --resource-group "$RESOURCE_GROUP" --query "properties.configuration.ingress.fqdn" -o tsv 2>/dev/null || true)
    if [ -n "$EUREKA_FQDN" ]; then
        EUREKA_URL="https://${{EUREKA_FQDN}}/eureka/"
        echo "Auto-discovered Eureka Server: ${{EUREKA_URL}}"
    else
        echo "ERROR: Eureka Server ('discovery-server') is not deployed in ${{RESOURCE_GROUP}}."
        echo "Please deploy discovery-server first: ./scripts/deploy-discovery-server.sh"
        exit 1
    fi
fi
""" if has_eureka else ""

    java_build_step = f"""
# 4. Check / Build JAR with Maven
JAR_EXISTS=$(find "{path}/target" -maxdepth 1 -name "*.jar" ! -name "*sources*" 2>/dev/null | head -n 1)
if [ "$BUILD_JAR" = true ] || [ -z "$JAR_EXISTS" ]; then
    echo "Compiling {name} with Maven..."
    if [ -f "./mvnw" ]; then
        ./mvnw package -DskipTests -pl "{path}" -am
    else
        mvn package -DskipTests -pl "{path}" -am
    fi
else
    echo "Found precompiled JAR: ${{JAR_EXISTS}}"
fi
""" if is_java else ""

    if is_ai:
        create_env_str = '--env-vars "EUREKA_SERVER_URL=${EUREKA_URL}" "USE_GEMINI_API=true" "GEMINI_API_KEY=AIzaSyCwVuiV4796KTvQ8CFj2BBBQ-4z6WwJQAg" "GEMINI_MODEL=gemini-3.5-flash"'
        update_env_str = '--set-env-vars "EUREKA_SERVER_URL=${EUREKA_URL}" "USE_GEMINI_API=true" "GEMINI_API_KEY=AIzaSyCwVuiV4796KTvQ8CFj2BBBQ-4z6WwJQAg" "GEMINI_MODEL=gemini-3.5-flash"'
    elif has_eureka:
        create_env_str = '--env-vars "EUREKA_SERVER_URL=${EUREKA_URL}" "FRONTEND_URL=https://wonderful-wave-0320abf00.3.azurestaticapps.net" "API_GATEWAY_URL=https://api-gateway.happybush-76206934.centralindia.azurecontainerapps.io" "CERTIFICATE_BASE_URL=https://api-gateway.happybush-76206934.centralindia.azurecontainerapps.io"'
        update_env_str = '--set-env-vars "EUREKA_SERVER_URL=${EUREKA_URL}" "FRONTEND_URL=https://wonderful-wave-0320abf00.3.azurestaticapps.net" "API_GATEWAY_URL=https://api-gateway.happybush-76206934.centralindia.azurecontainerapps.io" "CERTIFICATE_BASE_URL=https://api-gateway.happybush-76206934.centralindia.azurecontainerapps.io"'
    else:
        create_env_str = ""
        update_env_str = ""

    return f"""#!/usr/bin/env bash
# ==============================================================================
# Deploy {name} to Azure Container Apps
# ==============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${{BASH_SOURCE[0]}}")" && pwd)"
cd "$SCRIPT_DIR/.."

VERSION="${{VERSION:-v1}}"
REGISTRY="${{REGISTRY:-nicregistery}}"
RESOURCE_GROUP="${{RESOURCE_GROUP:-NIC_System}}"
ENVIRONMENT="${{ENVIRONMENT:-nic-system-env}}"
EUREKA_URL="${{EUREKA_URL:-}}"
BUILD_JAR=false

for arg in "$@"; do
    if [ "$arg" == "--build-jar" ]; then
        BUILD_JAR=true
    fi
done

echo "========================================================="
echo " DEPLOYING: {name.upper()}"
echo "========================================================="

# 1. Verify Azure CLI login
az account show > /dev/null 2>&1 || {{ echo "Please run 'az login' first."; exit 1; }}
{eureka_discovery}
# 3. Authenticate Docker
ACR_PASSWORD=$(az acr credential show --name "$REGISTRY" --query "passwords[0].value" -o tsv)
echo "$ACR_PASSWORD" | docker login "${{REGISTRY}}.azurecr.io" -u "$REGISTRY" --password-stdin
{java_build_step}
# 5. Build and Push
echo "Building {name} with Docker..."
docker build -t "${{REGISTRY}}.azurecr.io/{name}:${{VERSION}}" -f "{file}" .

echo "Pushing image to ACR..."
docker push "${{REGISTRY}}.azurecr.io/{name}:${{VERSION}}"

# 6. Deploy or Update
APP_EXISTS=$(az containerapp list --resource-group "$RESOURCE_GROUP" --query "[?name=='{name}'].name" -o tsv)

if [ -z "$APP_EXISTS" ]; then
    echo "Creating Container App '{name}'..."
    az containerapp create \\
        --name "{name}" \\
        --resource-group "$RESOURCE_GROUP" \\
        --environment "$ENVIRONMENT" \\
        --image "${{REGISTRY}}.azurecr.io/{name}:${{VERSION}}" \\
        --registry-server "${{REGISTRY}}.azurecr.io" \\
        --registry-username "$REGISTRY" \\
        --registry-password "$ACR_PASSWORD" \\
        --target-port {port} \\
        --ingress {ingress} \\
        --min-replicas 1 \\
        --cpu {cpu} \\
        --memory {mem} \\
        {create_env_str}
else
    echo "Updating Container App '{name}'..."
    az containerapp update \\
        --name "{name}" \\
        --resource-group "$RESOURCE_GROUP" \\
        --image "${{REGISTRY}}.azurecr.io/{name}:${{VERSION}}" \\
        --cpu {cpu} \\
        --memory {mem} \\
        {update_env_str}
fi

FQDN=$(az containerapp show --name "{name}" --resource-group "$RESOURCE_GROUP" --query "properties.configuration.ingress.fqdn" -o tsv 2>/dev/null || true)
echo "========================================================="
echo " {name.upper()} DEPLOYED SUCCESSFULLY!"
if [ -n "$FQDN" ]; then
    echo " Endpoint: https://${{FQDN}}"
fi
echo "========================================================="
"""

for svc in SERVICES:
    name = svc["name"]
    ps1_path = SCRIPTS_DIR / f"deploy-{name}.ps1"
    bat_path = SCRIPTS_DIR / f"deploy-{name}.bat"
    sh_path = SCRIPTS_DIR / f"deploy-{name}.sh"

    ps1_path.write_text(generate_ps1(svc), encoding="utf-8")
    bat_path.write_text(generate_bat(svc), encoding="utf-8")
    sh_path.write_text(generate_sh(svc), encoding="utf-8")
    print(f"Generated: deploy-{name} (.ps1, .bat, .sh)")

print("All 13 microservice deployment scripts generated successfully!")
