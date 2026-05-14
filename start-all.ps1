# SmartCourier - Build & Start All Services with Java 17
# Run from D:\SprintEvaluation as:  .\start-all.ps1

$JAVA_HOME = "C:\Users\acer\.jdks\ms-17.0.18"
$env:JAVA_HOME = $JAVA_HOME
$env:PATH = "$JAVA_HOME\bin;" + $env:PATH

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " SmartCourier - Build All Services" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$services = @("ServiceRegistry", "config-server", "auth-service", "delivery-service", "tracking-service", "admin-service", "api-gateway")

foreach ($svc in $services) {
    Write-Host "`nBuilding $svc..." -ForegroundColor Yellow
    Push-Location $svc
    & mvn clean package -DskipTests -q
    if ($LASTEXITCODE -ne 0) {
        Write-Host "FAILED to build $svc" -ForegroundColor Red
        Pop-Location
        exit 1
    }
    Write-Host "$svc built OK" -ForegroundColor Green
    Pop-Location
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host " Starting services in order..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$startOrder = @(
    @{ name="ServiceRegistry";  jar="ServiceRegistry\target\ServiceRegistry-0.0.1-SNAPSHOT.jar";  delay=12 },
    @{ name="Config Server";    jar="config-server\target\config-server-1.0.0.jar";               delay=10 },
    @{ name="Auth Service";     jar="auth-service\target\auth-service-1.0.0.jar";                 delay=10 },
    @{ name="Delivery Service"; jar="delivery-service\target\delivery-service-1.0.0.jar";         delay=10 },
    @{ name="Tracking Service"; jar="tracking-service\target\tracking-service-1.0.0.jar";         delay=10 },
    @{ name="Admin Service";    jar="admin-service\target\admin-service-1.0.0.jar";               delay=10 },
    @{ name="API Gateway";      jar="api-gateway\target\api-gateway-1.0.0.jar";                   delay=5  }
)

foreach ($svc in $startOrder) {
    if (Test-Path $svc.jar) {
        Write-Host "Starting $($svc.name)..." -ForegroundColor Yellow
        Start-Process "cmd" -ArgumentList "/k title $($svc.name) && java -jar $($svc.jar)" -WindowStyle Normal
        Write-Host "$($svc.name) started. Waiting $($svc.delay)s..." -ForegroundColor Green
        Start-Sleep -Seconds $svc.delay
    } else {
        Write-Host "JAR not found: $($svc.jar)" -ForegroundColor Red
        exit 1
    }
}

Write-Host "`n========================================" -ForegroundColor Green
Write-Host " All services running!" -ForegroundColor Green
Write-Host " Eureka Dashboard : http://localhost:8761" -ForegroundColor White
Write-Host " API Gateway      : http://localhost:9090" -ForegroundColor White
Write-Host " Auth Swagger     : http://localhost:8081/swagger-ui.html" -ForegroundColor White
Write-Host "========================================" -ForegroundColor Green
Write-Host "`n Now run frontend: cd smart-courier-frontend && npm start" -ForegroundColor Cyan
