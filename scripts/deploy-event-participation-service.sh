#!/usr/bin/env bash
# ==============================================================================
# Deploy event-participation-service to Azure Container Apps
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
echo " DEPLOYING: EVENT-PARTICIPATION-SERVICE"
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

# 4. Check / Build JAR with Maven
JAR_EXISTS=$(find "services/event-participation-service/target" -maxdepth 1 -name "*.jar" ! -name "*sources*" 2>/dev/null | head -n 1)
if [ "$BUILD_JAR" = true ] || [ -z "$JAR_EXISTS" ]; then
    echo "Compiling event-participation-service with Maven..."
    if [ -f "./mvnw" ]; then
        ./mvnw package -DskipTests -pl "services/event-participation-service" -am
    else
        mvn package -DskipTests -pl "services/event-participation-service" -am
    fi
else
    echo "Found precompiled JAR: ${JAR_EXISTS}"
fi

# 5. Build and Push
echo "Building event-participation-service with Docker..."
docker build -t "${REGISTRY}.azurecr.io/event-participation-service:${VERSION}" -f "services/event-participation-service/Dockerfile" .

echo "Pushing image to ACR..."
docker push "${REGISTRY}.azurecr.io/event-participation-service:${VERSION}"

# 6. Deploy or Update
APP_EXISTS=$(az containerapp list --resource-group "$RESOURCE_GROUP" --query "[?name=='event-participation-service'].name" -o tsv)

if [ -z "$APP_EXISTS" ]; then
    echo "Creating Container App 'event-participation-service'..."
    az containerapp create \
        --name "event-participation-service" \
        --resource-group "$RESOURCE_GROUP" \
        --environment "$ENVIRONMENT" \
        --image "${REGISTRY}.azurecr.io/event-participation-service:${VERSION}" \
        --registry-server "${REGISTRY}.azurecr.io" \
        --registry-username "$REGISTRY" \
        --registry-password "$ACR_PASSWORD" \
        --target-port 8083 \
        --ingress internal \
        --min-replicas 1 \
        --cpu 0.25 \
        --memory 0.5Gi \
        --env-vars "EUREKA_SERVER_URL=${EUREKA_URL}" "FRONTEND_URL=https://wonderful-wave-0320abf00.3.azurestaticapps.net" "API_GATEWAY_URL=https://api-gateway.happybush-76206934.centralindia.azurecontainerapps.io" "CERTIFICATE_BASE_URL=https://api-gateway.happybush-76206934.centralindia.azurecontainerapps.io"
else
    echo "Updating Container App 'event-participation-service'..."
    az containerapp update \
        --name "event-participation-service" \
        --resource-group "$RESOURCE_GROUP" \
        --image "${REGISTRY}.azurecr.io/event-participation-service:${VERSION}" \
        --cpu 0.25 \
        --memory 0.5Gi \
        --set-env-vars "EUREKA_SERVER_URL=${EUREKA_URL}" "FRONTEND_URL=https://wonderful-wave-0320abf00.3.azurestaticapps.net" "API_GATEWAY_URL=https://api-gateway.happybush-76206934.centralindia.azurecontainerapps.io" "CERTIFICATE_BASE_URL=https://api-gateway.happybush-76206934.centralindia.azurecontainerapps.io"
fi

FQDN=$(az containerapp show --name "event-participation-service" --resource-group "$RESOURCE_GROUP" --query "properties.configuration.ingress.fqdn" -o tsv 2>/dev/null || true)
echo "========================================================="
echo " EVENT-PARTICIPATION-SERVICE DEPLOYED SUCCESSFULLY!"
if [ -n "$FQDN" ]; then
    echo " Endpoint: https://${FQDN}"
fi
echo "========================================================="
