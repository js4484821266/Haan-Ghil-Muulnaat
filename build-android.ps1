$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot

if (-not (Test-Path ".\gradlew.bat")) {
    throw "gradlew.bat not found in project root."
}

if (-not $env:JAVA_HOME -or -not (Test-Path $env:JAVA_HOME)) {
    $studioJbr = "C:\Program Files\Android\Android Studio\jbr"
    if (Test-Path $studioJbr) {
        $env:JAVA_HOME = $studioJbr
    } else {
        throw "JAVA_HOME is not set correctly. Install JDK 17+ (for example Temurin) and set JAVA_HOME."
    }
}

$env:Path = "$env:JAVA_HOME\bin;$env:Path"

Write-Host "Using JAVA_HOME=$env:JAVA_HOME"

$hasReleaseSigning =
    ($env:ANDROID_KEYSTORE_PATH) -and
    ($env:ANDROID_KEYSTORE_PASSWORD) -and
    ($env:ANDROID_KEY_ALIAS) -and
    ($env:ANDROID_KEY_PASSWORD)

if ($hasReleaseSigning) {
    Write-Host "Release signing env vars detected -> building signed release APK"
} else {
    Write-Host "Release signing env vars missing -> building release APK with debug signing (local test only)"
}

.\gradlew.bat clean assembleRelease

if ($LASTEXITCODE -ne 0) {
    throw "Build failed."
}

Write-Host "Build succeeded. APK output: app\build\outputs\apk\release\"
