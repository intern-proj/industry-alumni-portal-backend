# ==============================================================================
# Deploy Eureka Server (Discovery Server) to Azure Container Apps
# Stage 1: Builds Docker image locally, pushes to ACR, and deploys Eureka Server
# ==============================================================================

[CmdletBinding()]
param (
    [string]$Version = "v1",
    [string]$Registry = "nicregistery",
    [string]$ResourceGroup = "NIC_System",
    [string]$Environment = "nic-system-env",
    [string]$Location = "centralindia"
)

$ErrorActionPreference = "Stop"

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
Write-Host " STAGE 1: Deploying Discovery Server (Eureka)" -ForegroundColor Green
Write-Host " Build Mode:      Local Docker Build -> Push to ACR" -ForegroundColor Yellow
Write-Host " Target Registry: $Registry.azurecr.io" -ForegroundColor Gray
Write-Host " Resource Group:  $ResourceGroup ($Location)" -ForegroundColor Gray
Write-Host " Environment:     $Environment" -ForegroundColor Gray
Write-Host " Image Version:   $Version" -ForegroundColor Gray
Write-Host "=========================================================" -ForegroundColor Cyan

# 1. Verify Azure CLI login
Write-Host "`n[1/5] Checking Azure authentication..." -ForegroundColor Cyan
$Account = az account show -o json 2>$null | ConvertFrom-Json
if (-not $Account) {
    Write-Host "You are not logged into Azure CLI. Please run 'az login' first." -ForegroundColor Red
    exit 1
}
Write-Host "Logged in as: $($Account.user.name) (Subscription: $($Account.name))" -ForegroundColor Green

# 2. Ensure Container Apps Environment exists
Write-Host "`n[2/5] Ensuring Container Apps Environment '$Environment' exists..." -ForegroundColor Cyan
$EnvCheck = az containerapp env show --name $Environment --resource-group $ResourceGroup --query "name" -o tsv 2>$null
if (-not $EnvCheck) {
    Write-Host "Creating Container Apps Environment '$Environment' in $Location..." -ForegroundColor Yellow
    az containerapp env create `
        --name $Environment `
        --resource-group $ResourceGroup `
        --location $Location
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Failed to create Container Apps Environment." -ForegroundColor Red
        exit 1
    }
    Write-Host "Container Apps Environment created successfully." -ForegroundColor Green
} else {
    Write-Host "Container Apps Environment '$Environment' is ready." -ForegroundColor Green
}

# 3. Retrieve ACR credentials and authenticate Docker
Write-Host "`n[3/5] Authenticating Docker with Container Registry ($Registry.azurecr.io)..." -ForegroundColor Cyan
$AcrPassword = az acr credential show --name $Registry --query "passwords[0].value" -o tsv
if (-not $AcrPassword) {
    Write-Host "Enabling admin credentials on $Registry..." -ForegroundColor Yellow
    az acr update --name $Registry --admin-enabled true | Out-Null
    $AcrPassword = az acr credential show --name $Registry --query "passwords[0].value" -o tsv
}
$AcrPassword | docker login "$Registry.azurecr.io" -u $Registry --password-stdin
if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker login to $Registry.azurecr.io failed." -ForegroundColor Red
    exit 1
}

# 4. Build Discovery Server locally and push to ACR
Write-Host "`n[4/5] Preparing and building discovery-server..." -ForegroundColor Cyan
$JarPath = "discovery-server/target/discovery-server-1.0.0-SNAPSHOT.jar"
if (-not (Test-Path $JarPath)) {
    Write-Host "Compiling discovery-server JAR using Maven..." -ForegroundColor Cyan
    if (Test-Path ".\mvnw.cmd") {
        .\mvnw.cmd -f discovery-server\pom.xml clean package -DskipTests
    } else {
        mvn -f discovery-server/pom.xml clean package -DskipTests
    }
}
Write-Host "Building discovery-server image locally with Docker..." -ForegroundColor Cyan
docker build -t "$Registry.azurecr.io/discovery-server:$Version" -f "discovery-server/Dockerfile" .
if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to build discovery-server locally." -ForegroundColor Red
    exit 1
}

Write-Host "Pushing discovery-server image to $Registry.azurecr.io..." -ForegroundColor Cyan
docker push "$Registry.azurecr.io/discovery-server:$Version"
if ($LASTEXITCODE -ne 0) {
    Write-Host "Retrying docker login and push..." -ForegroundColor Yellow
    $AcrPassword | docker login "$Registry.azurecr.io" -u $Registry --password-stdin
    docker push "$Registry.azurecr.io/discovery-server:$Version"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Failed to push discovery-server to ACR." -ForegroundColor Red
        exit 1
    }
}
Write-Host "Image $Registry.azurecr.io/discovery-server:$Version pushed successfully." -ForegroundColor Green

# 5. Deploy / Update Discovery Server Container App
Write-Host "`n[5/5] Deploying discovery-server to Azure Container Apps..." -ForegroundColor Cyan
$AppExists = az containerapp list --resource-group $ResourceGroup --query "[?name=='discovery-server'].name" -o tsv

if (-not $AppExists) {
    Write-Host "Creating Container App 'discovery-server' (Port: 8761, Ingress: External)..." -ForegroundColor Yellow
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
    Write-Host "Updating existing Container App 'discovery-server'..." -ForegroundColor Yellow
    az containerapp update `
        --name "discovery-server" `
        --resource-group $ResourceGroup `
        --image "$Registry.azurecr.io/discovery-server:$Version"
}

if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to deploy discovery-server Container App." -ForegroundColor Red
    exit 1
}

# Fetch the live FQDN
$Fqdn = az containerapp show --name "discovery-server" --resource-group $ResourceGroup --query "properties.configuration.ingress.fqdn" -o tsv

if ($Fqdn) {
    $EurekaUrl = "https://$Fqdn/eureka/"
    Write-Host "`n=========================================================" -ForegroundColor Green
    Write-Host " SUCCESS: Discovery Server (Eureka) is LIVE!" -ForegroundColor Green
    Write-Host " Dashboard URL: https://$Fqdn" -ForegroundColor Yellow
    Write-Host " Eureka Server URL: $EurekaUrl" -ForegroundColor Yellow
    Write-Host "=========================================================" -ForegroundColor Green
    Write-Host "`nNext Step: Run Stage 2 to deploy the remaining 12 services:" -ForegroundColor Cyan
    Write-Host "  .\scripts\deploy-services.ps1 -EurekaUrl `"$EurekaUrl`"" -ForegroundColor White
    Write-Host "  (Or simply: .\scripts\deploy-services.ps1)" -ForegroundColor Gray
} else {
    Write-Host "Could not automatically fetch FQDN. Please check the Azure Portal." -ForegroundColor Red
}
