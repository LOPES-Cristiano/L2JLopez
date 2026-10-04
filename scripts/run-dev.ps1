# Sobe o L2JLopez para desenvolvimento (login 2106 + game 7777 + HTTP 18080).
# Uso: .\scripts\run-dev.ps1            (pergunta a senha do banco se L2_DB_PASSWORD nao estiver definida)
#      .\scripts\run-dev.ps1 -SkipBuild (usa o jar ja gerado)
param([switch]$SkipBuild)

$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)

if (-not $env:L2_DB_USER) { $env:L2_DB_USER = 'root' }
if (-not $env:L2_DB_PASSWORD) {
    $secure = Read-Host "Senha do MySQL para $($env:L2_DB_USER)" -AsSecureString
    $env:L2_DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))
}
if (-not $env:SERVER_PORT) { $env:SERVER_PORT = '18080' }

if (-not $SkipBuild) {
    & .\mvnw.cmd -q -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw "build falhou" }
}

$jar = Get-ChildItem target\l2jlopez-*.jar | Where-Object { $_.Name -notlike '*plain*' } | Select-Object -First 1
Write-Host "Iniciando $($jar.Name) - Ctrl+C para parar"
java -jar $jar.FullName
