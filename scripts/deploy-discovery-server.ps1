# ==============================================================================
# Deploy discovery-server to Azure Container Apps
# Port: 8761 | Ingress: external | CPU: 0.5 | Memory: 1.0Gi
# ==============================================================================

[CmdletBinding()]
param (
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
Write-Host " DEPLOYING: DISCOVERY-SERVER" -ForegroundColor Green
Write-Host " Registry:        $Registry.azurecr.io" -ForegroundColor Gray
Write-Host " Resource Group:  $ResourceGroup" -ForegroundColor Gray
Write-Host " Environment:     $Environment" -ForegroundColor Gray
Write-Host " Target Port:     8761 (Ingress: external)" -ForegroundColor Gray
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
Write-Host "`n[4/5] Checking compiled JAR artifact for discovery-server..." -ForegroundColor Cyan
$JarExists = Get-ChildItem -Path "$ProjectRoot\discovery-server\target\*.jar" -ErrorAction SilentlyContinue | Where-Object { $_.Name -notmatch "sources|original" }
if ($BuildJar -or (-not $JarExists)) {
    Write-Host "Building discovery-server with Maven..." -ForegroundColor Yellow
    if (Test-Path "$ProjectRoot\mvnw.cmd") {
        & "$ProjectRoot\mvnw.cmd" package -DskipTests -pl "discovery-server" -am
    } elseif (Get-Command mvn -ErrorAction SilentlyContinue) {
        mvn package -DskipTests -pl "discovery-server" -am
    } else {
        Write-Host "ERROR: Maven ('mvn') or Maven wrapper ('mvnw.cmd') not found." -ForegroundColor Red
        exit 1
    }
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Maven build failed for discovery-server." -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "Found precompiled JAR: $($JarExists.Name)" -ForegroundColor Green
}

# 5. Build Image locally with Docker and Push to ACR
Write-Host "`n[5/5] Building discovery-server locally with Docker..." -ForegroundColor Cyan
docker build -t "$Registry.azurecr.io/discovery-server:$Version" -f "discovery-server/Dockerfile" .
if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker build failed for discovery-server." -ForegroundColor Red
    exit 1
}

Write-Host "Pushing image to $Registry.azurecr.io..." -ForegroundColor Cyan
$PushOk = $false
for ($attempt = 1; $attempt -le 3; $attempt++) {
    docker push "$Registry.azurecr.io/discovery-server:$Version"
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

# 6. Deploy / Update Container App
Write-Host "`nDeploying Container App 'discovery-server' to Azure Container Apps..." -ForegroundColor Cyan
$AppExists = az containerapp list --resource-group $ResourceGroup --query "[?name=='discovery-server'].name" -o tsv

if (-not $AppExists) {
    Write-Host "Creating Container App 'discovery-server'..." -ForegroundColor Yellow
    az containerapp create `
        --name "discovery-server" `
        --resource-group $ResourceGroup `
        --environment $Environment `
        --image "$Registry.azurecr.io/discovery-server:$Version" `
        --registry-server "$Registry.azurecr.io" `
        --registry-username $Registry `
        --registry-password $AcrPassword `
        --target-port 8761 `
        --ingress external `
        --min-replicas 1 `
        --cpu 0.5 `
        --memory 1.0Gi 
} else {
    Write-Host "Updating Container App 'discovery-server'..." -ForegroundColor Yellow
    az containerapp update `
        --name "discovery-server" `
        --resource-group $ResourceGroup `
        --image "$Registry.azurecr.io/discovery-server:$Version" `
        --cpu 0.5 `
        --memory 1.0Gi 
}

if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to deploy discovery-server." -ForegroundColor Red
    exit 1
}

$Fqdn = az containerapp show --name "discovery-server" --resource-group $ResourceGroup --query "properties.configuration.ingress.fqdn" -o tsv 2>$null

Write-Host "`n=========================================================" -ForegroundColor Green
Write-Host " DISCOVERY-SERVER DEPLOYED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "=========================================================" -ForegroundColor Green
if ($Fqdn) {
    Write-Host " Endpoint URL: https://$Fqdn" -ForegroundColor Yellow
}
