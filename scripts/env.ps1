# Подключить перед сборкой: . scripts/env.ps1
$root = Split-Path -Parent $PSScriptRoot
$env:JAVA_HOME = Join-Path $root "tools\jdk17"
$env:ANDROID_HOME = Join-Path $root "tools\android-sdk"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
