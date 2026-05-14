$env:JAVA_HOME="C:\tools\jdk17\jdk-17.0.10+7"
$services = @("ServiceRegistry", "config-server", "auth-service", "delivery-service", "tracking-service", "admin-service", "api-gateway")

Write-Host "Killing project-related Java processes..." -ForegroundColor Yellow
Get-Process java -ErrorAction SilentlyContinue | Where-Object { 
    try {
        $cmdLine = (Get-CimInstance Win32_Process -Filter "ProcessId = $($_.Id)").CommandLine
        $cmdLine -like "*D:\SprintEvaluation*"
    } catch { $false }
} | Stop-Process -Force

Write-Host "Starting Service Registry (Eureka)..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd ServiceRegistry; `$env:JAVA_HOME='C:\tools\jdk17\jdk-17.0.10+7'; mvn spring-boot:run" -WindowStyle Minimized
Write-Host "Waiting for Service Registry to initialize..."
Start-Sleep -Seconds 15

Write-Host "Starting Config Server..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd config-server; `$env:JAVA_HOME='C:\tools\jdk17\jdk-17.0.10+7'; mvn spring-boot:run" -WindowStyle Minimized
Write-Host "Waiting for Config Server to fetch properties from GitHub..."
Start-Sleep -Seconds 20

foreach ($service in @("auth-service", "delivery-service", "tracking-service", "admin-service")) {
    Write-Host "Starting $service..." -ForegroundColor Green
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd $service; `$env:JAVA_HOME='C:\tools\jdk17\jdk-17.0.10+7'; mvn spring-boot:run" -WindowStyle Minimized
    Start-Sleep -Seconds 10
}

Write-Host "Starting API Gateway..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd api-gateway; `$env:JAVA_HOME='C:\tools\jdk17\jdk-17.0.10+7'; mvn spring-boot:run" -WindowStyle Minimized

Write-Host "All services startup commands issued." -ForegroundColor White
