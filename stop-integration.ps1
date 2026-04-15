# Stop Integration Environment

Write-Host "🛑 Stopping integration environment..." -ForegroundColor Yellow

docker-compose -f docker-compose.integration.yml down
docker-compose down

Write-Host "✅ Integration environment stopped!" -ForegroundColor Green
Write-Host "💾 Data preserved in Docker volumes" -ForegroundColor Cyan
