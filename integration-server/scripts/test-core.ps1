$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$outDir = Join-Path $root 'build/core-checks'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$sources = @(Get-ChildItem (Join-Path $root 'src/main/java/com/safehome/integration/building') -Filter '*.java' | ForEach-Object FullName)
$sources += Join-Path $root 'src/test/java/com/safehome/integration/building/CoreChecks.java'
& javac -encoding UTF-8 -d $outDir @sources
if ($LASTEXITCODE -ne 0) { throw 'Core compilation failed' }
& java -cp $outDir com.safehome.integration.building.CoreChecks
if ($LASTEXITCODE -ne 0) { throw 'Core checks failed' }
