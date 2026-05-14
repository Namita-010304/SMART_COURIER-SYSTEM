# Create admin and test customer user via the real API
# Run AFTER all services are started

Write-Host "Creating admin user..." -ForegroundColor Yellow
$adminBody = '{"username":"admin","email":"admin@smartcourier.com","password":"Admin@123","fullName":"System Admin","role":"ADMIN"}'
try {
    $res = Invoke-RestMethod -Uri "http://localhost:9090/gateway/auth/signup" -Method POST -Body $adminBody -ContentType "application/json"
    Write-Host "Admin created: $($res.username) / role: $($res.role)" -ForegroundColor Green
} catch {
    Write-Host "Admin may already exist: $($_.Exception.Message)" -ForegroundColor DarkYellow
}

Write-Host "`nCreating customer user..." -ForegroundColor Yellow
$customerBody = '{"username":"customer1","email":"customer1@smartcourier.com","password":"Customer@123","fullName":"John Doe","role":"CUSTOMER"}'
try {
    $res = Invoke-RestMethod -Uri "http://localhost:9090/gateway/auth/signup" -Method POST -Body $customerBody -ContentType "application/json"
    Write-Host "Customer created: $($res.username) / role: $($res.role)" -ForegroundColor Green
} catch {
    Write-Host "Customer may already exist: $($_.Exception.Message)" -ForegroundColor DarkYellow
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host " Login Credentials:" -ForegroundColor Cyan
Write-Host " Admin    -> username: admin      password: Admin@123" -ForegroundColor White
Write-Host " Customer -> username: customer1  password: Customer@123" -ForegroundColor White
Write-Host "========================================" -ForegroundColor Cyan
