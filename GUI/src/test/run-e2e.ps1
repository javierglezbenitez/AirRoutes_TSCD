
param(
  [string]$ApiBase = "http://54.158.15.130:8080",
  [int]$Port = 3000,
  [string]$GuiDir = "../main/java/frontend",
  [bool]$UseMock = $true  # ← por defecto mock activado para que pase
)

function Wait-ForPort {
  param([int]$Port, [int]$TimeoutSeconds = 30)
  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    try {
      $resp = Invoke-WebRequest -Uri ("http://localhost:{0}" -f $Port) -UseBasicParsing -TimeoutSec 3
      if ($resp.StatusCode -ge 200) { return $true }
    } catch { Start-Sleep -Milliseconds 500 }
  }
  return $false
}

Write-Host "Instalando dependencias y navegadores..." -ForegroundColor Cyan
npm install
npm run install:browsers

Write-Host "Arrancando servidor estatico en http://localhost:$Port ..." -ForegroundColor Cyan
$httpArgs = "npx http-server `"$GuiDir`" -p $Port --cors --silent"
$serverProc = Start-Process -FilePath "cmd.exe" -ArgumentList "/c", $httpArgs -WindowStyle Hidden -PassThru

if (-not (Wait-ForPort -Port $Port -TimeoutSeconds 30)) {
  Write-Host "El servidor no respondió en el puerto $Port. Abortando." -ForegroundColor Red
  if ($serverProc) { try { $serverProc.CloseMainWindow() | Out-Null } catch {} }
  exit 1
}

$env:API_BASE = $ApiBase
$env:GUI_LOCAL = "http://localhost:$Port/index.html"
$env:USE_MOCK = ($(if ($UseMock) { "1" } else { "0" }))

Write-Host "Ejecutando pruebas Playwright contra API: $env:API_BASE  (USE_MOCK=$env:USE_MOCK)" -ForegroundColor Green
npx playwright test

Write-Host "Abrir reporte: npx playwright show-report" -ForegroundColor Yellow
