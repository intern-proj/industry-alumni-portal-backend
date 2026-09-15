#!/usr/bin/env bash
# ==============================================================================
# Deploy discovery-server to Azure Container Apps
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
echo " DEPLOYING: DISCOVERY-SERVER"
echo "========================================================="

# 1. Verify Azure CLI login
az account show > /dev/null 2>&1 || { echo "Please run 'az login' first."; exit 1; }

# 3. Authenticate Docker
ACR_PASSWORD=$(az acr credential show --name "$REGISTRY" --query "passwords[0].value" -o tsv)
echo "$ACR_PASSWORD" | docker login "${REGISTRY}.azurecr.io" -u "$REGISTRY" --password-stdin

# 4. Check / Build JAR with Maven
JAR_EXISTS=$(find "discovery-server/target" -maxdepth 1 -name "*.jar" ! -name "*sources*" 2>/dev/null | head -n 1)
if [ "$BUILD_JAR" = true ] || [ -z "$JAR_EXISTS" ]; then
    echo "Compiling discovery-server with Maven..."
    if [ -f "./mvnw" ]; then
        ./mvnw package -DskipTests -pl "discovery-server" -am
    else
        mvn package -DskipTests -pl "discovery-server" -am
    fi
else
    echo "Found precompiled JAR: ${JAR_EXISTS}"
fi

# 5. Build and Push
echo "Building discovery-server with Docker..."
docker build -t "${REGISTRY}.azurecr.io/discovery-server:${VERSION}" -f "discovery-server/Dockerfile" .

echo "Pushing image to ACR..."
docker push "${REGISTRY}.azurecr.io/discovery-server:${VERSION}"

# 6. Deploy or Update
APP_EXISTS=$(az containerapp list --resource-group "$RESOURCE_GROUP" --query "[?name=='discovery-server'].name" -o tsv)

if [ -z "$APP_EXISTS" ]; then
    echo "Creating Container App 'discovery-server'..."
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
        --memory 1.0Gi \
        
else
    echo "Updating Container App 'discovery-server'..."
    az containerapp update \
        --name "discovery-server" \
        --resource-group "$RESOURCE_GROUP" \
        --image "${REGISTRY}.azurecr.io/discovery-server:${VERSION}" \
        --cpu 0.5 \
        --memory 1.0Gi \
        
fi

FQDN=$(az containerapp show --name "discovery-server" --resource-group "$RESOURCE_GROUP" --query "properties.configuration.ingress.fqdn" -o tsv 2>/dev/null || true)
echo "========================================================="
echo " DISCOVERY-SERVER DEPLOYED SUCCESSFULLY!"
if [ -n "$FQDN" ]; then
    echo " Endpoint: https://${FQDN}"
fi
echo "========================================================="
