<#
.SYNOPSIS
    Разворачивает локальное окружение сборки Android для проекта «Питомец Финни»
    без установки Android Studio: JDK 17 (Temurin) и Android SDK command-line tools,
    оба — в каталог tools/ внутри репозитория, изолированно от системы.

.DESCRIPTION
    Используется, если на машине нет Android Studio или не подходит системная
    версия JDK. Android Gradle Plugin требует JDK 17 (или новее, но не выше
    поддерживаемого диапазона); свежая системная JDK может быть несовместима.

.USAGE
    powershell -ExecutionPolicy Bypass -File scripts/setup-android-env.ps1

    После выполнения переменные окружения для текущей сессии PowerShell:
        $env:JAVA_HOME    -> tools/jdk17
        $env:ANDROID_HOME -> tools/android-sdk
    Для новой сессии их нужно установить заново либо использовать
    scripts/env.ps1 (создаётся этим скриптом).
#>

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$toolsDir = Join-Path $root "tools"
$jdkDir = Join-Path $toolsDir "jdk17"
$sdkDir = Join-Path $toolsDir "android-sdk"

New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null

# --- JDK 17 (Eclipse Temurin, автоматически подбирается под ОС/архитектуру) ---
if (-not (Test-Path (Join-Path $jdkDir "bin\java.exe"))) {
    Write-Host "Скачивание JDK 17 (Temurin)..."
    $jdkZip = Join-Path $toolsDir "jdk17.zip"
    Invoke-WebRequest -Uri "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse" -OutFile $jdkZip
    $extractTmp = Join-Path $toolsDir "_jdk_extract"
    Expand-Archive -Path $jdkZip -DestinationPath $extractTmp -Force
    $extracted = Get-ChildItem $extractTmp | Select-Object -First 1
    Move-Item $extracted.FullName $jdkDir
    Remove-Item $extractTmp -Recurse -Force
    Remove-Item $jdkZip -Force
    Write-Host "JDK 17 установлена: $jdkDir"
} else {
    Write-Host "JDK 17 уже установлена: $jdkDir"
}

# --- Android SDK command-line tools ---
$cmdlineToolsLatest = Join-Path $sdkDir "cmdline-tools\latest\bin\sdkmanager.bat"
if (-not (Test-Path $cmdlineToolsLatest)) {
    Write-Host "Определение актуальной версии Android command-line tools..."
    $repoXml = Invoke-WebRequest -Uri "https://dl.google.com/android/repository/repository2-3.xml" -UseBasicParsing
    $match = [regex]::Match($repoXml.Content, "commandlinetools-win-\d+_latest\.zip")
    if (-not $match.Success) { throw "Не удалось определить версию Android command-line tools." }
    $cmdName = $match.Value

    Write-Host "Скачивание $cmdName ..."
    $cmdZip = Join-Path $toolsDir "cmdline-tools.zip"
    Invoke-WebRequest -Uri "https://dl.google.com/android/repository/$cmdName" -OutFile $cmdZip

    $extractTmp = Join-Path $toolsDir "_cmd_extract"
    Expand-Archive -Path $cmdZip -DestinationPath $extractTmp -Force
    New-Item -ItemType Directory -Force -Path (Join-Path $sdkDir "cmdline-tools") | Out-Null
    Move-Item (Join-Path $extractTmp "cmdline-tools") (Join-Path $sdkDir "cmdline-tools\latest")
    Remove-Item $extractTmp -Recurse -Force
    Remove-Item $cmdZip -Force
    Write-Host "Android command-line tools установлены: $sdkDir"
} else {
    Write-Host "Android command-line tools уже установлены: $sdkDir"
}

$env:JAVA_HOME = $jdkDir
$env:ANDROID_HOME = $sdkDir
$sdkmanager = $cmdlineToolsLatest

Write-Host "Принятие лицензий SDK..."
$answers = Join-Path $toolsDir "_licenses_yes.txt"
[System.IO.File]::WriteAllText($answers, ("y`r`n" * 20))
cmd /c "`"$sdkmanager`" --licenses < `"$answers`"" | Out-Null
Remove-Item $answers -Force

Write-Host "Установка platform-tools, platform 37.0, build-tools 37.0.0..."
# Уровень API соответствует compileSdk проекта (см. docs/01-проектные-решения.md).
& $sdkmanager "platform-tools" "platforms;android-37.0" "build-tools;37.0.0"

# --- Сохраняем переменные окружения для последующих сессий ---
$envScript = Join-Path $PSScriptRoot "env.ps1"
@"
# Подключить перед сборкой: . scripts/env.ps1
`$env:JAVA_HOME = "$jdkDir"
`$env:ANDROID_HOME = "$sdkDir"
`$env:Path = "`$env:JAVA_HOME\bin;`$env:ANDROID_HOME\platform-tools;`$env:Path"
"@ | Set-Content -Path $envScript -Encoding utf8

Write-Host ""
Write-Host "Готово. Перед сборкой в новой сессии выполните:"
Write-Host "    . scripts/env.ps1"
