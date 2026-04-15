# Integration Startup Script - Start ADS service with essential team services
# RAM Conscious: Only starts what's needed for ads-service testing

Write-Host "Starting ADS-Service Integration Test Environment" -ForegroundColor Cyan
Write-Host ""

# Create network if it doesn't exist
Write-Host "[1/5] Creating Docker network..." -ForegroundColor Yellow
docker network create microservices-net 2>$null

# Start core infrastructure
Write-Host "[2/5] Starting core infrastructure (MySQL, Eureka)..." -ForegroundColor Yellow
docker-compose up -d mysql eureka-server

Write-Host "      Waiting for infrastructure to be healthy (30s)..." -ForegroundColor Gray
Start-Sleep -Seconds 30

# Start API Gateway
Write-Host "[3/5] Starting API Gateway..." -ForegroundColor Yellow
docker-compose up -d api-gateway

# Start User Service (needed for auth)
Write-Host "[4/5] Starting User Service..." -ForegroundColor Yellow
docker-compose up -d user-service

# Start ALL ads-service infrastructure
Write-Host "[5/5] Starting ADS-Service stack (Postgres, Kafka, Qdrant, Ads-Service)..." -ForegroundColor Yellow
docker-compose up -d ads-db zookeeper kafka qdrant

Write-Host "      Waiting for ads infrastructure (25s)..." -ForegroundColor Gray
Start-Sleep -Seconds 25

# Start ads-service itself
docker-compose up -d ads-service

Write-Host ""
Write-Host "==========================================" -ForegroundColor Green
Write-Host " INTEGRATION ENVIRONMENT READY" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Running Services:" -ForegroundColor Cyan
Write-Host "  [CORE]" -ForegroundColor Yellow
Write-Host "    - MySQL Database:  localhost:3307" -ForegroundColor White
Write-Host "    - Eureka Server:   http://localhost:8762" -ForegroundColor White
Write-Host "    - API Gateway:     http://localhost:8223" -ForegroundColor White
Write-Host "    - User Service:    localhost:8091" -ForegroundColor White
Write-Host ""
Write-Host "  [ADS-SERVICE]" -ForegroundColor Yellow
Write-Host "    - Ads PostgreSQL:  localhost:5434" -ForegroundColor White
Write-Host "    - Kafka:           localhost:9092" -ForegroundColor White
Write-Host "    - Qdrant:          localhost:6333" -ForegroundColor White
Write-Host "    - Ads Service:     http://localhost:8090" -ForegroundColor White
Write-Host ""
Write-Host "Quick Tests:" -ForegroundColor Cyan
Write-Host "  curl http://localhost:8223/api/plans" -ForegroundColor Gray
Write-Host "  curl http://localhost:8223/api/campaigns" -ForegroundColor Gray
Write-Host ""
Write-Host "Useful Commands:" -ForegroundColor Cyan
Write-Host "  docker-compose ps                    # Check status" -ForegroundColor Gray
Write-Host "  docker-compose logs -f ads-service   # View ads logs" -ForegroundColor Gray
Write-Host "  .\stop-integration.ps1               # Stop all services" -ForegroundColor Gray
Write-Host ""
Write-Host "Note: Other team services are stopped to save RAM (~3-4GB usage)" -ForegroundColor Magenta
Write-Host ""
