param([ValidateSet('Preview','Run','Test','Package')][string]$Mode = 'Run')
$ErrorActionPreference = 'Stop'
$backendRoot = Split-Path $PSScriptRoot -Parent
$projectRoot = Split-Path $backendRoot -Parent
Set-Location $backendRoot
if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath "$env:JAVA_HOME/bin/java.exe")) {
    $javaCandidate = Get-ChildItem -LiteralPath "$env:ProgramFiles/Java" -Directory -ErrorAction SilentlyContinue | Where-Object Name -Like 'jdk*' | Sort-Object Name -Descending | Select-Object -First 1
    if ($javaCandidate) { $env:JAVA_HOME = $javaCandidate.FullName }
}
$mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if (-not $mavenCommand) { $mavenCommand = Get-Command mvn -ErrorAction SilentlyContinue }
if ($mavenCommand) { $mavenExecutable = $mavenCommand.Source }
else {
    $localMaven = Get-ChildItem -LiteralPath "$backendRoot/.tools" -Filter mvn.cmd -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $localMaven) { throw 'Install Maven 3.6.3+ and Java 17+ before starting the API. See README.md in the project folder.' }
    $mavenExecutable = $localMaven.FullName
}
# Local bootstrap secrets are optional, ignored and never packaged.
$adminEnv = Join-Path $backendRoot '.local-data/admin-bootstrap.env'
if ($Mode -in @('Preview','Run') -and (Test-Path -LiteralPath $adminEnv)) {
    foreach ($line in Get-Content -LiteralPath $adminEnv) {
        if ($line -match '^(TRUTHLENS_ADMIN_EMAIL|TRUTHLENS_ADMIN_PASSWORD)=(.*)$') { [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process') }
    }
}
if ($Mode -eq 'Preview') { & $mavenExecutable spring-boot:test-run '-Dspring-boot.run.profiles=preview' }
elseif ($Mode -eq 'Test') { & $mavenExecutable test }
elseif ($Mode -eq 'Package') { & $mavenExecutable package }
else {
    $databaseEnv = Join-Path $projectRoot 'database/.env'
    if (Test-Path -LiteralPath $databaseEnv) {
        foreach ($line in Get-Content -LiteralPath $databaseEnv) {
            if ($line -match '^(DB_URL|DB_USER|DB_PASSWORD)=(.*)$') { [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process') }
        }
    }
    if (-not $env:DB_PASSWORD -or $env:DB_PASSWORD -eq 'replace-with-a-unique-local-password') { throw 'Set DB_PASSWORD in database/.env before starting with PostgreSQL, or use -Mode Preview.' }
    & $mavenExecutable spring-boot:run
}
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
