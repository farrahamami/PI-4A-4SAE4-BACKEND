# Full Ads Stack Startup Script (Warning: High RAM usage)
# Starts core integration + ALL ads-service extra infrastructure

Write-Host "==========================================" -ForegroundColor Red
Write-Host " ⚠️ WARNING: HIGH RAM USAGE DETECTED ⚠️ " -ForegroundColor Red
Write-Host " Starting the full Ads Service stack will use " -ForegroundColor Yellow
Write-Host " ~6-8GB of RAM. Your system may become slow." -ForegroundColor Yellow
Write-Host "==========================================" -ForegroundColor Red
Write-Host ""
Write-Host "Starting Full Ads-Service Test Environment in 5 seconds..." -ForegroundColor Cyan
Start-Sleep -Seconds 5

# Create network if it doesn't exist
Write-Host "[1/6] Creating Docker network..." -ForegroundColor Yellow
docker network create microservices-net 2>$null

# Start core infrastructure
Write-Host "[2/6] Starting core infrastructure (MySQL, Eureka)..." -ForegroundColor Yellow
docker-compose up -d mysql eureka-server

Write-Host "      Waiting for infrastructure to be healthy (20s)..." -ForegroundColor Gray
Start-Sleep -Seconds 20

# Start API Gateway & User Service
Write-Host "[3/6] Starting API Gateway & User Service..." -ForegroundColor Yellow
docker-compose up -d api-gateway user-service

# Start core ads-service infrastructure
Write-Host "[4/6] Starting core Ads-Service stack (Postgres, Kafka, Zookeeper, Qdrant)..." -ForegroundColor Yellow
docker-compose up -d ads-db zookeeper kafka qdrant

# Start extra ads-service infrastructure (Ollama, ClickHouse, Grafana, SonarQube, PgAdmin, Kafka-UI)
Write-Host "[5/6] Starting EXTRA Ads-Service infrastructure (Heavy)..." -ForegroundColor Magenta
docker-compose -f docker-compose.yml -f docker-compose.ads-extras.yml up -d pgadmin kafka-ui ollama clickhouse grafana sonar-db sonarqube

Write-Host "      Waiting for databases and heavy containers (30s)..." -ForegroundColor Gray
Start-Sleep -Seconds 30

# Start ads-service itself
Write-Host "[6/6] Starting Ads-Service application..." -ForegroundColor Yellow
docker-compose up -d ads-service

Write-Host ""
Write-Host "==========================================" -ForegroundColor Green
Write-Host " FULL ADS-SERVICE ENVIRONMENT READY" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Core Services:" -ForegroundColor Cyan
Write-Host "  - API Gateway:     http://localhost:8223" -ForegroundColor White
Write-Host "  - User Service:    localhost:8091" -ForegroundColor White
Write-Host "  - Ads Service:     http://localhost:8090" -ForegroundColor White
Write-Host ""
Write-Host "Ads Extra Interfaces (The Heavy Stuff):" -ForegroundColor Magenta
Write-Host "  - PgAdmin:         http://localhost:5050 (fawzi-admin@pidev4sae4.com / fawzi)" -ForegroundColor White
Write-Host "  - Kafka UI:        http://localhost:8085" -ForegroundColor White
Write-Host "  - Grafana:         http://localhost:3000 (admin / admin)" -ForegroundColor White
Write-Host "  - SonarQube:       http://localhost:9001 (admin / admin)" -ForegroundColor White
Write-Host "  - Ollama API:      http://localhost:11435" -ForegroundColor White
Write-Host ""
Write-Host "To stop EVERYTHING safely, run:" -ForegroundColor Yellow
Write-Host "  docker-compose -f docker-compose.yml -f docker-compose.ads-extras.yml down" -ForegroundColor White
Write-Host ""
