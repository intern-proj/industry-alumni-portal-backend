# ==============================================================================
# Deploy ai-service to Azure Container Apps
# Port: 8000 | Ingress: external | CPU: 0.5 | Memory: 1.0Gi
# ==============================================================================

[CmdletBinding()]
param (
    [string]$EurekaUrl = "",
    [string]$GeminiApiKey = "AIzaSyCwVuiV4796KTvQ8CFj2BBBQ-4z6WwJQAg",
    [string]$GeminiModel = "gemini-3.1-flash-lite",
    [string]$Version = "v1",
    [string]$Registry = "nicregistery",
    [string]$ResourceGroup = "NIC_System",
    [string]$Environment = "nic-system-env"
)

$ErrorActionPreference = "Stop"

# Always ensure execution is rooted in the repository directory
$ProjectRoot = (Resolve-Path "$PSScriptRoot\..").Path
Set-Location $ProjectRoot

# Ensure Azure CLI is discoverable in PATH
if (-not (Get-Command az -ErrorAction SilentlyContinue)) {
    if (Test-Path "C:\Program Files\Microsoft SDKs\Azure\CLI2\wbin\az.cmd") {
        $env:PATH += ";C:\Program Files\Microsoft SDKs\Azure\CLI2\wbin"
    } elseif (Test-Path "C:\Program Files (x86)\Microsoft SDKs\Azure\CLI2\wbin\az.cmd") {
        $env:PATH += ";C:\Program Files (x86)\Microsoft SDKs\Azure\CLI2\wbin"
    } else {
        Write-Host "ERROR: Azure CLI ('az') is not installed or not found." -ForegroundColor Red
        exit 1
    }
}

Write-Host "=========================================================" -ForegroundColor Cyan
Write-Host " DEPLOYING: AI-SERVICE" -ForegroundColor Green
Write-Host " Registry:        $Registry.azurecr.io" -ForegroundColor Gray
Write-Host " Resource Group:  $ResourceGroup" -ForegroundColor Gray
Write-Host " Environment:     $Environment" -ForegroundColor Gray
Write-Host " Target Port:     8000 (Ingress: external)" -ForegroundColor Gray
Write-Host " Resources:       0.5 CPU | 1.0Gi RAM" -ForegroundColor Gray
Write-Host "=========================================================" -ForegroundColor Cyan

# 1. Verify Azure CLI login
Write-Host "`n[1/4] Checking Azure authentication..." -ForegroundColor Cyan
$Account = az account show -o json 2>$null | ConvertFrom-Json
if (-not $Account) {
    Write-Host "You are not logged into Azure CLI. Please run 'az login' first." -ForegroundColor Red
    exit 1
}
Write-Host "Logged in as: $($Account.user.name)" -ForegroundColor Green

# 2. Resolve Eureka Server URL
Write-Host "`n[2/4] Resolving Eureka Discovery Server URL..." -ForegroundColor Cyan
if (-not $EurekaUrl) {
    $EurekaFqdn = az containerapp show --name "discovery-server" --resource-group $ResourceGroup --query "properties.configuration.ingress.fqdn" -o tsv 2>$null
    if ($EurekaFqdn) {
        $EurekaUrl = "https://$EurekaFqdn/eureka/"
        Write-Host "Auto-discovered Eureka Server at: $EurekaUrl" -ForegroundColor Green
    } else {
        Write-Host "ERROR: Eureka Server ('discovery-server') is not deployed or accessible in $ResourceGroup." -ForegroundColor Red
        Write-Host "Please deploy discovery-server first: .\scripts\deploy-discovery-server.bat" -ForegroundColor Yellow
        exit 1
    }
} else {
    Write-Host "Using specified Eureka URL: $EurekaUrl" -ForegroundColor Green
}

# 3. Authenticate Docker with Container Registry
Write-Host "`n[3/5] Authenticating Docker with Container Registry ($Registry.azurecr.io)..." -ForegroundColor Cyan
$AcrPassword = az acr credential show --name $Registry --query "passwords[0].value" -o tsv
if (-not $AcrPassword) {
    Write-Host "Enabling admin credentials on $Registry..." -ForegroundColor Yellow
    az acr update --name $Registry --admin-enabled true | Out-Null
    $AcrPassword = az acr credential show --name $Registry --query "passwords[0].value" -o tsv
}

$DockerAuthOk = $false
for ($attempt = 1; $attempt -le 3; $attempt++) {
    $AcrPassword | docker login "$Registry.azurecr.io" -u $Registry --password-stdin
    if ($LASTEXITCODE -eq 0) {
        $DockerAuthOk = $true
        break
    }
    Write-Host "Docker login attempt $attempt failed, retrying in 3 seconds..." -ForegroundColor Yellow
    Start-Sleep -Seconds 3
}
if (-not $DockerAuthOk) {
    Write-Host "Docker login to $Registry.azurecr.io failed after 3 attempts." -ForegroundColor Red
    exit 1
}

# 5. Build Image locally with Docker and Push to ACR
Write-Host "`n[5/5] Building ai-service locally with Docker..." -ForegroundColor Cyan
docker build -t "$Registry.azurecr.io/ai-service:$Version" -f "services/ai-service/Dockerfile" .
if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker build failed for ai-service." -ForegroundColor Red
    exit 1
}

Write-Host "Pushing image to $Registry.azurecr.io..." -ForegroundColor Cyan
$PushOk = $false
for ($attempt = 1; $attempt -le 3; $attempt++) {
    docker push "$Registry.azurecr.io/ai-service:$Version"
    if ($LASTEXITCODE -eq 0) {
        $PushOk = $true
        break
    }
    Write-Host "Push attempt $attempt failed, retrying in 3 seconds..." -ForegroundColor Yellow
    Start-Sleep -Seconds 3
}
if (-not $PushOk) {
    Write-Host "Failed to push image to ACR after 3 attempts." -ForegroundColor Red
    exit 1
}

# Prepare environment variables
$EnvVars = @(
    "EUREKA_SERVER_URL=$EurekaUrl",
    "USE_GEMINI_API=true",
    "GEMINI_API_KEY=$GeminiApiKey",
    "GEMINI_MODEL=$GeminiModel",
    "API_GATEWAY_URL=https://api-gateway.happybush-76206934.centralindia.azurecontainerapps.io/api/v1",
    "BACKEND_API_BASE_URL=https://api-gateway.happybush-76206934.centralindia.azurecontainerapps.io/api/v1",
    "VACANCY_SERVICE_BASE_URL=https://vacancy-service.internal.happybush-76206934.centralindia.azurecontainerapps.io/api/v1/vacancies/partner",
    "APPLICATION_SERVICE_BASE_URL=https://application-service.internal.happybush-76206934.centralindia.azurecontainerapps.io/api/v1/applications"
)

# 6. Deploy / Update Container App
Write-Host "`nDeploying Container App 'ai-service' to Azure Container Apps..." -ForegroundColor Cyan
$AppExists = az containerapp list --resource-group $ResourceGroup --query "[?name=='ai-service'].name" -o tsv

if (-not $AppExists) {
    Write-Host "Creating Container App 'ai-service'..." -ForegroundColor Yellow
    az containerapp create `
        --name "ai-service" `
        --resource-group $ResourceGroup `
        --environment $Environment `
        --image "$Registry.azurecr.io/ai-service:$Version" `
        --registry-server "$Registry.azurecr.io" `
        --registry-username $Registry `
        --registry-password $AcrPassword `
        --target-port 8000 `
        --ingress external `
        --min-replicas 1 `
        --cpu 0.5 `
        --memory 1.0Gi --env-vars $EnvVars
} else {
    Write-Host "Updating Container App 'ai-service'..." -ForegroundColor Yellow
    az containerapp update `
        --name "ai-service" `
        --resource-group $ResourceGroup `
        --image "$Registry.azurecr.io/ai-service:$Version" `
        --cpu 0.5 `
        --memory 1.0Gi --set-env-vars $EnvVars
}

if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to deploy ai-service." -ForegroundColor Red
    exit 1
}

$Fqdn = az containerapp show --name "ai-service" --resource-group $ResourceGroup --query "properties.configuration.ingress.fqdn" -o tsv 2>$null

Write-Host "`n=========================================================" -ForegroundColor Green
Write-Host " AI-SERVICE DEPLOYED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "=========================================================" -ForegroundColor Green
if ($Fqdn) {
    Write-Host " Endpoint URL: https://$Fqdn" -ForegroundColor Yellow
}
