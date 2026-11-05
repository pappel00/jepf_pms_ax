# Secret encryption tool launcher. Usage: .\encrypt-secret.ps1 [encrypt | decrypt | generate-key]
# Run in the project folder. Type the value as a single line when prompted (it is not kept in shell history).
param([ValidateSet('encrypt', 'decrypt', 'generate-key')][string]$Command = 'encrypt')

Set-Location $PSScriptRoot
$jdk = Join-Path $PSScriptRoot '.tools\jdk21-dl\jdk-21.0.12.1+1'
if (Test-Path $jdk) { $env:JAVA_HOME = (Resolve-Path $jdk).Path }
& .\gradlew.bat -q :framework-core:secretTool "--args=$Command"
