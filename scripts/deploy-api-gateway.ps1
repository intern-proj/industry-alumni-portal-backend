# ==============================================================================
# Deploy api-gateway to Azure Container Apps
# Port: 8080 | Ingress: external | CPU: 0.5 | Memory: 1.0Gi
# ==============================================================================

[CmdletBinding()]
param (
    [string]$EurekaUrl = "",
    [switch]$BuildJar,
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
Write-Host " DEPLOYING: API-GATEWAY" -ForegroundColor Green
Write-Host " Registry:        $Registry.azurecr.io" -ForegroundColor Gray
Write-Host " Resource Group:  $ResourceGroup" -ForegroundColor Gray
Write-Host " Environment:     $Environment" -ForegroundColor Gray
Write-Host " Target Port:     8080 (Ingress: external)" -ForegroundColor Gray
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

# 4. Check / Compile JAR with Maven
Write-Host "`n[4/5] Checking compiled JAR artifact for api-gateway..." -ForegroundColor Cyan
$JarExists = Get-ChildItem -Path "$ProjectRoot\api-gateway\target\*.jar" -ErrorAction SilentlyContinue | Where-Object { $_.Name -notmatch "sources|original" }
if ($BuildJar -or (-not $JarExists)) {
    Write-Host "Building api-gateway with Maven..." -ForegroundColor Yellow
    if (Test-Path "$ProjectRoot\mvnw.cmd") {
        & "$ProjectRoot\mvnw.cmd" package -DskipTests -pl "api-gateway" -am
    } elseif (Get-Command mvn -ErrorAction SilentlyContinue) {
        mvn package -DskipTests -pl "api-gateway" -am
    } else {
        Write-Host "ERROR: Maven ('mvn') or Maven wrapper ('mvnw.cmd') not found." -ForegroundColor Red
        exit 1
    }
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Maven build failed for api-gateway." -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "Found precompiled JAR: $($JarExists.Name)" -ForegroundColor Green
}

# 5. Build Image locally with Docker and Push to ACR
Write-Host "`n[5/5] Building api-gateway locally with Docker..." -ForegroundColor Cyan
docker build -t "$Registry.azurecr.io/api-gateway:$Version" -f "api-gateway/Dockerfile" .
if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker build failed for api-gateway." -ForegroundColor Red
    exit 1
}

Write-Host "Pushing image to $Registry.azurecr.io..." -ForegroundColor Cyan
$PushOk = $false
for ($attempt = 1; $attempt -le 3; $attempt++) {
    docker push "$Registry.azurecr.io/api-gateway:$Version"
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

$EnvVars = @(
    "EUREKA_SERVER_URL=$EurekaUrl",
    "FRONTEND_URL=https://wonderful-wave-0320abf00.3.azurestaticapps.net",
    "API_GATEWAY_URL=https://api-gateway.happybush-76206934.centralindia.azurecontainerapps.io",
    "CERTIFICATE_BASE_URL=https://api-gateway.happybush-76206934.centralindia.azurecontainerapps.io"
)

# 6. Deploy / Update Container App
Write-Host "`nDeploying Container App 'api-gateway' to Azure Container Apps..." -ForegroundColor Cyan
$AppExists = az containerapp list --resource-group $ResourceGroup --query "[?name=='api-gateway'].name" -o tsv

if (-not $AppExists) {
    Write-Host "Creating Container App 'api-gateway'..." -ForegroundColor Yellow
    az containerapp create `
        --name "api-gateway" `
        --resource-group $ResourceGroup `
        --environment $Environment `
        --image "$Registry.azurecr.io/api-gateway:$Version" `
        --registry-server "$Registry.azurecr.io" `
        --registry-username $Registry `
        --registry-password $AcrPassword `
        --target-port 8080 `
        --ingress external `
        --min-replicas 1 `
        --cpu 0.5 `
        --memory 1.0Gi --env-vars $EnvVars
} else {
    Write-Host "Updating Container App 'api-gateway'..." -ForegroundColor Yellow
    az containerapp update `
        --name "api-gateway" `
        --resource-group $ResourceGroup `
        --image "$Registry.azurecr.io/api-gateway:$Version" `
        --cpu 0.5 `
        --memory 1.0Gi --set-env-vars $EnvVars
}

if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to deploy api-gateway." -ForegroundColor Red
    exit 1
}

$Fqdn = az containerapp show --name "api-gateway" --resource-group $ResourceGroup --query "properties.configuration.ingress.fqdn" -o tsv 2>$null

Write-Host "`n=========================================================" -ForegroundColor Green
Write-Host " API-GATEWAY DEPLOYED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "=========================================================" -ForegroundColor Green
if ($Fqdn) {
    Write-Host " Endpoint URL: https://$Fqdn" -ForegroundColor Yellow
}
