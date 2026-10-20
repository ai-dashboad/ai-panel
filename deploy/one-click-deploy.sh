#!/bin/bash
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}   AI Panel One-Click Deployment${NC}"
echo -e "${GREEN}========================================${NC}"

# Check prerequisites
check_prerequisites() {
    echo -e "\n${YELLOW}Checking prerequisites...${NC}"

    if ! command -v docker &> /dev/null; then
        echo -e "${RED}Docker is not installed. Please install Docker first.${NC}"
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null; then
        echo -e "${RED}Docker Compose is not installed. Please install Docker Compose first.${NC}"
        exit 1
    fi

    if ! command -v mvn &> /dev/null; then
        echo -e "${RED}Maven is not installed. Please install Maven first.${NC}"
        exit 1
    fi

    if ! command -v node &> /dev/null; then
        echo -e "${RED}Node.js is not installed. Please install Node.js first.${NC}"
        exit 1
    fi

    echo -e "${GREEN}All prerequisites met!${NC}"
}

# Load environment variables
load_env() {
    if [ -f .env ]; then
        echo -e "\n${YELLOW}Loading environment variables...${NC}"
        export $(cat .env | grep -v '#' | xargs)
    else
        echo -e "\n${YELLOW}Creating default .env file...${NC}"
        cat > .env << EOF
MYSQL_PASSWORD=$(openssl rand -hex 16)
EUREKA_PASSWORD=$(openssl rand -hex 16)
JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n')
REDIS_PASSWORD=
OPENAI_API_KEY=
ANTHROPIC_API_KEY=
DOCKER_REGISTRY=
VERSION=latest
EOF
        echo -e "${YELLOW}Please edit .env file and add your API keys, then run again.${NC}"
        exit 0
    fi
}

# Build backend
build_backend() {
    echo -e "\n${YELLOW}Building backend services...${NC}"
    mvn clean package -DskipTests -q

    if [ $? -ne 0 ]; then
        echo -e "${RED}Backend build failed!${NC}"
        exit 1
    fi
    echo -e "${GREEN}Backend build completed!${NC}"
}

# Build frontend
build_frontend() {
    echo -e "\n${YELLOW}Building frontend...${NC}"
    cd ai-panel-ui
    npm install -q
    npm run build

    if [ $? -ne 0 ]; then
        echo -e "${RED}Frontend build failed!${NC}"
        exit 1
    fi
    cd ..
    echo -e "${GREEN}Frontend build completed!${NC}"
}

# Deploy with Docker Compose
deploy() {
    local env=${1:-dev}

    echo -e "\n${YELLOW}Deploying to ${env} environment...${NC}"

    if [ "$env" == "dev" ]; then
        docker-compose -f docker-compose.dev.yml up -d --build
    else
        docker-compose -f docker-compose.prod.yml up -d
    fi

    if [ $? -ne 0 ]; then
        echo -e "${RED}Deployment failed!${NC}"
        exit 1
    fi

    echo -e "${GREEN}Deployment completed!${NC}"
}

# Health check
health_check() {
    echo -e "\n${YELLOW}Running health checks...${NC}"

    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if curl -s http://localhost:8761/actuator/health | grep -q "UP"; then
            echo -e "${GREEN}Eureka is healthy!${NC}"
            break
        fi
        echo "Waiting for Eureka... (attempt $attempt/$max_attempts)"
        sleep 5
        ((attempt++))
    done

    if [ $attempt -gt $max_attempts ]; then
        echo -e "${RED}Health check failed!${NC}"
        exit 1
    fi

    echo -e "${GREEN}All services are healthy!${NC}"
}

# Print access info
print_info() {
    echo -e "\n${GREEN}========================================${NC}"
    echo -e "${GREEN}   Deployment Complete!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo -e "\nAccess URLs:"
    echo -e "  - Frontend:  ${YELLOW}http://localhost:3000${NC}"
    echo -e "  - API:       ${YELLOW}http://localhost:8080${NC}"
    echo -e "  - Eureka:    ${YELLOW}http://localhost:8761${NC}"
    echo -e "  - Swagger:   ${YELLOW}http://localhost:8082/swagger-ui.html${NC}"
    echo -e "\nDefault admin account is seeded by scripts/sql/01-user-service-init.sql."
    echo -e "  ${YELLOW}Change the admin password immediately after first login.${NC}"
    echo -e "\n${GREEN}Happy coding!${NC}"
}

# Main
main() {
    local env=${1:-dev}

    check_prerequisites
    load_env
    build_backend
    build_frontend
    deploy $env
    health_check
    print_info
}

# Run
main "$@"
