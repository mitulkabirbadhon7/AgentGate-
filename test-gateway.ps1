# AgentGate — Quick Verification Script for Windows PowerShell
# Usage: .\test-gateway.ps1

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "       AgentGate Zero-Trust Gateway Verification          " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$baseUrl = "http://localhost:8080"
$ErrorActionPreference = "Stop"

# 1. Connectivity Check
Write-Host "`n[1/8] Checking server connectivity..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -Method Get -TimeoutSec 5
    if ($health.status -eq "UP") {
        Write-Host "  [OK] Server is UP at $baseUrl" -ForegroundColor Green
    } else {
        Write-Host "  [WARN] Server responded with status: $($health.status)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "`n  [ERROR] Unable to connect to AgentGate server at $baseUrl" -ForegroundColor Red
    Write-Host "  Please start the Spring Boot application first in another terminal:" -ForegroundColor Yellow
    Write-Host "    .\mvnw.cmd spring-boot:run" -ForegroundColor White
    Write-Host "  Wait until you see 'Started AgentGateApplication' before running this script.`n" -ForegroundColor Yellow
    exit 1
}

# 2. Verify Admin Web Routes
Write-Host "`n[2/8] Verifying Admin Dashboard and Web UI routes..." -ForegroundColor Yellow
$webRoutes = @("/admin", "/tools", "/agents", "/policies", "/approvals", "/audit", "/killswitches")
foreach ($route in $webRoutes) {
    try {
        $resp = Invoke-WebRequest -Uri "$baseUrl$route" -Method Get -UseBasicParsing
        if ($resp.StatusCode -eq 200) {
            Write-Host "  [OK] GET $route -> 200 OK (Rendered cleanly)" -ForegroundColor Green
        } else {
            Write-Host "  [WARN] GET $route -> HTTP $($resp.StatusCode)" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "  [FAIL] GET $route -> $($_.Exception.Message)" -ForegroundColor Red
    }
}

# 3. Acquire OAuth2 JWT Bearer Token
Write-Host "`n[3/8] Acquiring JWT Bearer token for demo-agent..." -ForegroundColor Yellow
try {
    $tokenResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/auth/token?clientId=demo-agent" -Method Get
    $token = $tokenResp.data.access_token
    if ($token) {
        Write-Host "  [OK] Token acquired successfully (Expires in: $($tokenResp.data.expires_in)s)" -ForegroundColor Green
    } else {
        Write-Host "  [FAIL] No access_token returned" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "  [FAIL] Token acquisition failed: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type"  = "application/json"
}

# 4. Verify REST API Endpoints
Write-Host "`n[4/8] Testing REST endpoints (Agents, Downstream Servers, Tools)..." -ForegroundColor Yellow
try {
    $agents = Invoke-RestMethod -Uri "$baseUrl/api/v1/agents" -Headers $headers -Method Get
    Write-Host "  [OK] GET /api/v1/agents -> Found $($agents.data.Count) registered agent(s)" -ForegroundColor Green

    $servers = Invoke-RestMethod -Uri "$baseUrl/api/v1/servers" -Headers $headers -Method Get
    Write-Host "  [OK] GET /api/v1/servers -> Found $($servers.data.Count) downstream server(s)" -ForegroundColor Green

    $tools = Invoke-RestMethod -Uri "$baseUrl/api/v1/tools" -Headers $headers -Method Get
    Write-Host "  [OK] GET /api/v1/tools -> Found $($tools.data.Count) registered tool(s)" -ForegroundColor Green
} catch {
    Write-Host "  [FAIL] REST queries failed: $($_.Exception.Message)" -ForegroundColor Red
}

# 5. Verify Mock Downstream Service
Write-Host "`n[5/8] Testing downstream mock service..." -ForegroundColor Yellow
try {
    $mockResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/mock/weather" -Method Post -ContentType "application/json" -Body '{"city":"London"}'
    Write-Host "  [OK] POST /api/v1/mock/weather -> Weather in $($mockResp.city): $($mockResp.temperature_c)C, $($mockResp.condition)" -ForegroundColor Green
} catch {
    Write-Host "  [FAIL] Mock service error: $($_.Exception.Message)" -ForegroundColor Red
}

# 6. Verify Governed Tool Invocation
Write-Host "`n[6/8] Invoking governed tool through AgentGate gateway..." -ForegroundColor Yellow
try {
    $invBody = @{
        toolName = "weather_service"
        arguments = @{ city = "Tokyo" }
    } | ConvertTo-Json

    $invResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/invocations" -Headers $headers -Method Post -Body $invBody
    Write-Host "  [OK] POST /api/v1/invocations -> Status: $($invResp.data.status), Tool: $($invResp.data.toolName)" -ForegroundColor Green
} catch {
    Write-Host "  [FAIL] Tool invocation failed: $($_.Exception.Message)" -ForegroundColor Red
}

# 7. Verify MCP Protocol Endpoint
Write-Host "`n[7/8] Testing MCP JSON-RPC protocol endpoint..." -ForegroundColor Yellow
try {
    $mcpBody = '{"jsonrpc":"2.0","method":"tools/list","id":1}'
    $mcpResp = Invoke-RestMethod -Uri "$baseUrl/mcp" -Headers $headers -Method Post -Body $mcpBody
    $mcpToolCount = $mcpResp.result.tools.Count
    Write-Host "  [OK] POST /mcp (tools/list) -> Discovered $mcpToolCount tool(s) via MCP" -ForegroundColor Green
} catch {
    Write-Host "  [FAIL] MCP protocol check failed: $($_.Exception.Message)" -ForegroundColor Red
}

# 8. Verify Audit Chain
Write-Host "`n[8/8] Verifying cryptographic SHA-256 audit chain..." -ForegroundColor Yellow
try {
    $auditResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/audit/verify" -Headers $headers -Method Post
    if ($auditResp.data.valid -eq $true) {
        Write-Host "  [OK] Audit verification: $($auditResp.message) ($($auditResp.data.eventsChecked) events checked)" -ForegroundColor Green
    } else {
        Write-Host "  [WARN] Audit integrity warning: $($auditResp.message)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  [FAIL] Audit verification failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host "         All AgentGate Gateway Checks Completed!          " -ForegroundColor Green
Write-Host "==========================================================`n" -ForegroundColor Cyan
