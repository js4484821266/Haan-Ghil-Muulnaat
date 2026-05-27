$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

$localProperties = Join-Path $projectRoot "local.properties"
if (-not (Test-Path $localProperties)) {
    throw "local.properties not found."
}

$sdkLine = Get-Content $localProperties | Where-Object { $_ -like "sdk.dir=*" } | Select-Object -First 1
if (-not $sdkLine) {
    throw "sdk.dir is missing in local.properties"
}

$sdkDir = ($sdkLine.Split("=")[1]) -replace "\\\\", "\"
$adbPath = Join-Path $sdkDir "platform-tools\adb.exe"
if (-not (Test-Path $adbPath)) {
    throw "adb.exe not found at $adbPath"
}

$apkPath = Join-Path $projectRoot "app\build\outputs\apk\release\app-release.apk"
if (-not (Test-Path $apkPath)) {
    throw "Release APK not found at $apkPath. Run .\build-android.ps1 first."
}

& $adbPath start-server | Out-Null
$devices = & $adbPath devices
$onlineDevice = $devices | Where-Object { $_ -match "\tdevice$" }
if (-not $onlineDevice) {
    throw "No online Android device found. Enable USB debugging and run again."
}

Write-Host "Installing: $apkPath"
& $adbPath install -r $apkPath
if ($LASTEXITCODE -ne 0) {
    throw "APK install failed."
}

Write-Host "Install completed successfully."
