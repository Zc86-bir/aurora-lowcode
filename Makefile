# Aurora Low-Code Platform — Makefile
# Usage: make [target]

.PHONY: all dev test build docker-build docker-up docker-down deploy clean help

# Variables
JAVA_HOME ?= $(shell dirname $$(dirname $$(readlink -f $$(which java))))
APP_NAME := aurora-lowcode
IMAGE_NAME := aurora-lowcode
IMAGE_TAG := latest
HELM_RELEASE := aurora
HELM_NAMESPACE := aurora

# Colors for output
BLUE := \033[0;34m
GREEN := \033[0;32m
YELLOW := \033[1;33m
NC := \033[0m

all: test build ## Run tests and build (default)

help: ## Show this help message
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "$(BLUE)%-20s$(NC) %s\n", $$1, $$2}'

# ===========================
# Development
# ===========================

dev: ## Start development environment (PostgreSQL + Redis)
	@echo "$(GREEN)Starting development environment...$(NC)"
	docker compose -f docker-compose.dev.yml up -d
	@echo "$(GREEN)Infrastructure ready. Start backend:$(NC)"
	@echo "$(YELLOW)  export JAVA_HOME=/path/to/jdk-25 && mvn spring-boot:run -Dspring.profiles.active=dev$(NC)"

dev-down: ## Stop development environment
	docker compose -f docker-compose.dev.yml down

dev-logs: ## Show development logs
	docker compose -f docker-compose.dev.yml logs -f

# ===========================
# Testing
# ===========================

test: ## Run all tests
	@echo "$(GREEN)Running unit tests...$(NC)"
	JAVA_HOME="$(JAVA_HOME)" mvn test -DskipITs -Djacoco.skip=true
	@echo "$(GREEN)Running integration tests...$(NC)"
	JAVA_HOME="$(JAVA_HOME)" mvn verify -DskipUTs -Djacoco.skip=true
	@echo "$(GREEN)All tests passed!$(NC)"

test-unit: ## Run unit tests only
	JAVA_HOME="$(JAVA_HOME)" mvn test -DskipITs -Djacoco.skip=true

test-integration: ## Run integration tests only
	JAVA_HOME="$(JAVA_HOME)" mvn verify -DskipUTs -Djacoco.skip=true

test-coverage: ## Run tests with JaCoCo coverage
	@echo "$(GREEN)Running tests with coverage...$(NC)"
	JAVA_HOME="$(JAVA_HOME)" mvn verify -DskipITs
	@echo "$(GREEN)Coverage report: target/site/jacoco/index.html$(NC)"

test-security: ## Run security checks
	@echo "$(GREEN)Running OWASP dependency check...$(NC)"
	JAVA_HOME="$(JAVA_HOME)" mvn org.owasp:dependency-check-maven:check -DskipTests -DfailBuildOnCVSS=7

test-spotbugs: ## Run SpotBugs static analysis
	JAVA_HOME="$(JAVA_HOME)" mvn com.github.spotbugs:spotbugs-maven-plugin:spotbugs -DskipTests

# ===========================
# Building
# ===========================

build: ## Build the application JAR
	@echo "$(GREEN)Building $(APP_NAME)...$(NC)"
	JAVA_HOME="$(JAVA_HOME)" mvn clean package -DskipTests -Djacoco.skip=true
	@echo "$(GREEN)Build complete: target/*.jar$(NC)"

compile-only: ## Compile source without packaging (no JAR produced)
	JAVA_HOME="$(JAVA_HOME)" mvn clean compile

# ===========================
# Docker
# ===========================

docker-build: ## Build Docker image
	@echo "$(GREEN)Building Docker image $(IMAGE_NAME):$(IMAGE_TAG)...$(NC)"
	docker build -f Dockerfile.prod -t $(IMAGE_NAME):$(IMAGE_TAG) .
	@echo "$(GREEN)Docker image built: $(IMAGE_NAME):$(IMAGE_TAG)$(NC)"

docker-up: ## Start production environment (Aurora + PostgreSQL + Redis + MinIO)
	@echo "$(GREEN)Starting production environment...$(NC)"
	docker compose -f docker-compose.prod.yml up -d
	@echo "$(GREEN)Aurora running on http://localhost:8080$(NC)"
	@echo "$(YELLOW)Required: DATABASE_PASSWORD, REDIS_PASSWORD, JWT_SECRET, MINIO_SECRET_KEY$(NC)"

docker-down: ## Stop production environment
	docker compose -f docker-compose.prod.yml down

docker-logs: ## Show production logs
	docker compose -f docker-compose.prod.yml logs -f

docker-clean: ## Remove all Docker images and volumes
	docker compose -f docker-compose.prod.yml down -v --rmi all

# ===========================
# Deployment
# ===========================

deploy: ## Deploy to Kubernetes via Helm
	@echo "$(GREEN)Deploying $(HELM_RELEASE) to $(HELM_NAMESPACE)...$(NC)"
	helm upgrade --install $(HELM_RELEASE) ./deploy/helm/aurora \
		--namespace $(HELM_NAMESPACE) \
		--create-namespace \
		--set image.tag=$(IMAGE_TAG) \
		--wait --timeout 300s
	@echo "$(GREEN)Deployment complete!$(NC)"

deploy-dry-run: ## Dry-run Helm deployment
	helm upgrade --install $(HELM_RELEASE) ./deploy/helm/aurora \
		--namespace $(HELM_NAMESPACE) \
		--create-namespace \
		--dry-run --debug

deploy-rollback: ## Rollback last Helm release
	helm rollback $(HELM_RELEASE) -n $(HELM_NAMESPACE)
	@echo "$(GREEN)Rollback complete!$(NC)"

deploy-status: ## Show Helm release status
	helm status $(HELM_RELEASE) -n $(HELM_NAMESPACE)

# ===========================
# Verification
# ===========================

verify: ## Run full verification (tests + security + lint)
	@echo "$(GREEN)Running full verification pipeline...$(NC)"
	$(MAKE) test
	$(MAKE) test-security
	$(MAKE) test-spotbugs
	@echo "$(GREEN)All verification checks passed!$(NC)"

# ===========================
# Cleanup
# ===========================

clean: ## Clean build artifacts
	@echo "$(YELLOW)Cleaning build artifacts...$(NC)"
	JAVA_HOME="$(JAVA_HOME)" mvn clean
	rm -rf target/
	rm -rf charts/
	@echo "$(GREEN)Clean complete.$(NC)"
