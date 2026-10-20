.PHONY: all build build-backend build-frontend clean dev prod stop logs test help

# Default target
all: help

# Build everything
build: build-backend build-frontend

# Build backend
build-backend:
	@echo "Building backend services..."
	mvn clean package -DskipTests

# Build frontend
build-frontend:
	@echo "Building frontend..."
	cd ai-panel-ui && npm install && npm run build

# Clean build artifacts
clean:
	@echo "Cleaning build artifacts..."
	mvn clean
	rm -rf ai-panel-ui/node_modules ai-panel-ui/dist

# Start development environment
dev: build
	@echo "Starting development environment..."
	docker-compose -f docker-compose.dev.yml up -d --build

# Start production environment
prod:
	@echo "Starting production environment..."
	docker-compose -f docker-compose.prod.yml up -d

# Stop all containers
stop:
	@echo "Stopping all containers..."
	docker-compose -f docker-compose.dev.yml down
	docker-compose -f docker-compose.prod.yml down

# View logs
logs:
	docker-compose -f docker-compose.dev.yml logs -f

# Run tests
test:
	@echo "Running tests..."
	mvn test
	cd ai-panel-ui && npm test

# Database init
db-init:
	@echo "Initializing databases..."
	docker-compose -f docker-compose.dev.yml exec mysql mysql -uroot -p$${MYSQL_PASSWORD:-root} < scripts/sql/init-all.sql

# Start only infrastructure
infra:
	@echo "Starting infrastructure (MySQL, Redis)..."
	docker-compose -f docker-compose.dev.yml up -d mysql redis

# Start Eureka only
eureka:
	@echo "Starting Eureka..."
	mvn -pl ai-panel-eureka spring-boot:run

# Lint code
lint:
	@echo "Linting code..."
	mvn spotless:check
	cd ai-panel-ui && npm run lint

# Format code
format:
	@echo "Formatting code..."
	mvn spotless:apply
	cd ai-panel-ui && npm run format

# Help
help:
	@echo "AI Panel Makefile"
	@echo ""
	@echo "Usage: make [target]"
	@echo ""
	@echo "Targets:"
	@echo "  build          - Build all services"
	@echo "  build-backend  - Build backend only"
	@echo "  build-frontend - Build frontend only"
	@echo "  clean          - Clean build artifacts"
	@echo "  dev            - Start development environment"
	@echo "  prod           - Start production environment"
	@echo "  stop           - Stop all containers"
	@echo "  logs           - View container logs"
	@echo "  test           - Run all tests"
	@echo "  db-init        - Initialize databases"
	@echo "  infra          - Start infrastructure only"
	@echo "  eureka         - Start Eureka only"
	@echo "  lint           - Lint code"
	@echo "  format         - Format code"
	@echo "  help           - Show this help"
