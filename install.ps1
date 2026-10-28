[CmdletBinding()]
param([string]$Adb=(Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'))
$ErrorActionPreference='Stop'
$devices=& $Adb devices -l
if(($devices | Where-Object {$_ -match '\sdevice\s' -and $_ -match 'model:DLI_TL20'}).Count -ne 1){throw 'Expected exactly one authorized Honor DLI-TL20'}
$honorLine=$devices | Where-Object {$_ -match '\sdevice\s' -and $_ -match 'model:DLI_TL20'}
$serial=($honorLine -split '\s+')[0]
& $Adb -s $serial install -r (Join-Path $PSScriptRoot 'artifacts/pomodoro-honor-debug.apk')
if($LASTEXITCODE -ne 0){throw 'Installation failed'}
& $Adb -s $serial shell am start -n local.ivand.pomodoro/.MainActivity
if($LASTEXITCODE -ne 0){throw 'Launch failed'}
