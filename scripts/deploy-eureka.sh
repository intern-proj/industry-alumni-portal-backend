#!/bin/bash
# ==============================================================================
# Deploy Eureka Server (Discovery Server) to Azure Container Apps (Bash)
# Stage 1: Builds Docker image locally, pushes to ACR, and deploys Eureka Server
# ==============================================================================

set -e

VERSION="${1:-v1}"
REGISTRY="nicregistery"
RESOURCE_GROUP="NIC_System"
ENVIRONMENT="nic-system-env"
LOCATION="centralindia"

echo -e "\e[36m=========================================================\e[0m"
echo -e "\e[32m STAGE 1: Deploying Discovery Server (Eureka)\e[0m"
echo -e "\e[33m Build Mode:      Local Docker Build -> Push to ACR\e[0m"
echo -e "\e[90m Target Registry: ${REGISTRY}.azurecr.io\e[0m"
echo -e "\e[90m Resource Group:  ${RESOURCE_GROUP} (${LOCATION})\e[0m"
echo -e "\e[90m Environment:     ${ENVIRONMENT}\e[0m"
echo -e "\e[90m Image Version:   ${VERSION}\e[0m"
echo -e "\e[36m=========================================================\e[0m"

# 1. Check Azure authentication
echo -e "\n\e[36m[1/5] Checking Azure authentication...\e[0m"
ACCOUNT=$(az account show -o json 2>/dev/null || true)
if [ -z "$ACCOUNT" ]; then
    echo -e "\e[31mYou are not logged into Azure CLI. Please run 'az login' first.\e[0m"
    exit 1
fi
USER_NAME=$(az account show --query "user.name" -o tsv)
echo -e "\e[32mLogged in as: ${USER_NAME}\e[0m"

# 2. Ensure Container Apps Environment exists
echo -e "\n\e[36m[2/5] Ensuring Container Apps Environment '${ENVIRONMENT}' exists...\e[0m"
ENV_CHECK=$(az containerapp env show --name "$ENVIRONMENT" --resource-group "$RESOURCE_GROUP" --query "name" -o tsv 2>/dev/null || true)
if [ -z "$ENV_CHECK" ]; then
    echo -e "\e[33mCreating Container Apps Environment '${ENVIRONMENT}' in ${LOCATION}...\e[0m"
    az containerapp env create \
        --name "$ENVIRONMENT" \
        --resource-group "$RESOURCE_GROUP" \
        --location "$LOCATION"
    echo -e "\e[32mContainer Apps Environment created successfully.\e[0m"
else
    echo -e "\e[32mContainer Apps Environment '${ENVIRONMENT}' is ready.\e[0m"
fi

# 3. Retrieve ACR credentials and authenticate Docker
echo -e "\n\e[36m[3/5] Authenticating Docker with Container Registry (${REGISTRY}.azurecr.io)...\e[0m"
ACR_PASSWORD=$(az acr credential show --name "$REGISTRY" --query "passwords[0].value" -o tsv 2>/dev/null || true)
if [ -z "$ACR_PASSWORD" ]; then
    echo -e "\e[33mEnabling admin credentials on ${REGISTRY}...\e[0m"
    az acr update --name "$REGISTRY" --admin-enabled true >/dev/null
    ACR_PASSWORD=$(az acr credential show --name "$REGISTRY" --query "passwords[0].value" -o tsv)
fi
echo "$ACR_PASSWORD" | docker login "${REGISTRY}.azurecr.io" -u "$REGISTRY" --password-stdin

# 4. Build Discovery Server locally and push to ACR
echo -e "\n\e[36m[4/5] Preparing and building discovery-server...\e[0m"
if [ ! -f "discovery-server/target/discovery-server-1.0.0-SNAPSHOT.jar" ]; then
    echo -e "\e[36mCompiling discovery-server JAR using Maven...\e[0m"
    if [ -f "./mvnw" ]; then
        ./mvnw -f discovery-server/pom.xml clean package -DskipTests
    else
        mvn -f discovery-server/pom.xml clean package -DskipTests
    fi
fi
echo -e "\e[36mBuilding discovery-server image locally with Docker...\e[0m"
docker build -t "${REGISTRY}.azurecr.io/discovery-server:${VERSION}" -f "discovery-server/Dockerfile" .

echo -e "\e[36mPushing discovery-server image to ${REGISTRY}.azurecr.io...\e[0m"
docker push "${REGISTRY}.azurecr.io/discovery-server:${VERSION}"
echo -e "\e[32mImage ${REGISTRY}.azurecr.io/discovery-server:${VERSION} pushed successfully.\e[0m"

# 5. Deploy / Update Discovery Server Container App
echo -e "\n\e[36m[5/5] Deploying discovery-server to Azure Container Apps...\e[0m"
APP_EXISTS=$(az containerapp show --name "discovery-server" --resource-group "$RESOURCE_GROUP" --query "name" -o tsv 2>/dev/null || true)

if [ -z "$APP_EXISTS" ]; then
    echo -e "\e[33mCreating Container App 'discovery-server' (Port: 8761, Ingress: External)...\e[0m"
    az containerapp create \
        --name "discovery-server" \
        --resource-group "$RESOURCE_GROUP" \
        --environment "$ENVIRONMENT" \
        --image "${REGISTRY}.azurecr.io/discovery-server:${VERSION}" \
        --registry-server "${REGISTRY}.azurecr.io" \
        --registry-username "$REGISTRY" \
        --registry-password "$ACR_PASSWORD" \
        --target-port 8761 \
        --ingress external \
        --min-replicas 1 \
        --cpu 0.5 \
        --memory 1.0Gi
else
    echo -e "\e[33mUpdating existing Container App 'discovery-server'...\e[0m"
    az containerapp update \
        --name "discovery-server" \
        --resource-group "$RESOURCE_GROUP" \
        --image "${REGISTRY}.azurecr.io/discovery-server:${VERSION}"
fi

# Fetch FQDN
FQDN=$(az containerapp show --name "discovery-server" --resource-group "$RESOURCE_GROUP" --query "properties.configuration.ingress.fqdn" -o tsv)

if [ -n "$FQDN" ]; then
    EUREKA_URL="https://${FQDN}/eureka/"
    echo -e "\n\e[32m=========================================================\e[0m"
    echo -e "\e[32m SUCCESS: Discovery Server (Eureka) is LIVE!\e[0m"
    echo -e "\e[33m Dashboard URL: https://${FQDN}\e[0m"
    echo -e "\e[33m Eureka Server URL: ${EUREKA_URL}\e[0m"
    echo -e "\e[32m=========================================================\e[0m"
    echo -e "\n\e[36mNext Step: Run Stage 2 to deploy the remaining 12 services:\e[0m"
    echo -e "  ./scripts/deploy-services.sh \"${EUREKA_URL}\""
    echo -e "  (Or simply: ./scripts/deploy-services.sh)"
else
    echo -e "\e[31mCould not automatically fetch FQDN. Please check Azure Portal.\e[0m"
fi
