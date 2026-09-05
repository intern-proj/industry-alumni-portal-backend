#!/usr/bin/env bash
# ==============================================================================
# Deploy ai-service to Azure Container Apps
# ==============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."

VERSION="${VERSION:-v1}"
REGISTRY="${REGISTRY:-nicregistery}"
RESOURCE_GROUP="${RESOURCE_GROUP:-NIC_System}"
ENVIRONMENT="${ENVIRONMENT:-nic-system-env}"
EUREKA_URL="${EUREKA_URL:-}"
BUILD_JAR=false

for arg in "$@"; do
    if [ "$arg" == "--build-jar" ]; then
        BUILD_JAR=true
    fi
done

echo "========================================================="
echo " DEPLOYING: AI-SERVICE"
echo "========================================================="

# 1. Verify Azure CLI login
az account show > /dev/null 2>&1 || { echo "Please run 'az login' first."; exit 1; }

# 2. Resolve Eureka Server URL
if [ -z "$EUREKA_URL" ]; then
    echo "Resolving Eureka Discovery Server URL..."
    EUREKA_FQDN=$(az containerapp show --name "discovery-server" --resource-group "$RESOURCE_GROUP" --query "properties.configuration.ingress.fqdn" -o tsv 2>/dev/null || true)
    if [ -n "$EUREKA_FQDN" ]; then
        EUREKA_URL="https://${EUREKA_FQDN}/eureka/"
        echo "Auto-discovered Eureka Server: ${EUREKA_URL}"
    else
        echo "ERROR: Eureka Server ('discovery-server') is not deployed in ${RESOURCE_GROUP}."
        echo "Please deploy discovery-server first: ./scripts/deploy-discovery-server.sh"
        exit 1
    fi
fi

# 3. Authenticate Docker
ACR_PASSWORD=$(az acr credential show --name "$REGISTRY" --query "passwords[0].value" -o tsv)
echo "$ACR_PASSWORD" | docker login "${REGISTRY}.azurecr.io" -u "$REGISTRY" --password-stdin

# 5. Build and Push
echo "Building ai-service with Docker..."
docker build -t "${REGISTRY}.azurecr.io/ai-service:${VERSION}" -f "services/ai-service/Dockerfile" .

echo "Pushing image to ACR..."
docker push "${REGISTRY}.azurecr.io/ai-service:${VERSION}"

# 6. Deploy or Update
APP_EXISTS=$(az containerapp list --resource-group "$RESOURCE_GROUP" --query "[?name=='ai-service'].name" -o tsv)

if [ -z "$APP_EXISTS" ]; then
    echo "Creating Container App 'ai-service'..."
    az containerapp create \
        --name "ai-service" \
        --resource-group "$RESOURCE_GROUP" \
        --environment "$ENVIRONMENT" \
        --image "${REGISTRY}.azurecr.io/ai-service:${VERSION}" \
        --registry-server "${REGISTRY}.azurecr.io" \
        --registry-username "$REGISTRY" \
        --registry-password "$ACR_PASSWORD" \
        --target-port 8000 \
        --ingress external \
        --min-replicas 1 \
        --cpu 0.5 \
        --memory 1.0Gi \
        --env-vars "EUREKA_SERVER_URL=${EUREKA_URL}" "USE_GEMINI_API=true" "GEMINI_API_KEY=AIzaSyCwVuiV4796KTvQ8CFj2BBBQ-4z6WwJQAg" "GEMINI_MODEL=gemini-3.5-flash"
else
    echo "Updating Container App 'ai-service'..."
    az containerapp update \
        --name "ai-service" \
        --resource-group "$RESOURCE_GROUP" \
        --image "${REGISTRY}.azurecr.io/ai-service:${VERSION}" \
        --cpu 0.5 \
        --memory 1.0Gi \
        --set-env-vars "EUREKA_SERVER_URL=${EUREKA_URL}" "USE_GEMINI_API=true" "GEMINI_API_KEY=AIzaSyCwVuiV4796KTvQ8CFj2BBBQ-4z6WwJQAg" "GEMINI_MODEL=gemini-3.5-flash"
fi

FQDN=$(az containerapp show --name "ai-service" --resource-group "$RESOURCE_GROUP" --query "properties.configuration.ingress.fqdn" -o tsv 2>/dev/null || true)
echo "========================================================="
echo " AI-SERVICE DEPLOYED SUCCESSFULLY!"
if [ -n "$FQDN" ]; then
    echo " Endpoint: https://${FQDN}"
fi
echo "========================================================="
