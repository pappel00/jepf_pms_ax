@echo off
rem Usage: encrypt-secret.bat [encrypt ^| decrypt ^| generate-key]
rem Run in the project folder. Type the value as a single line when prompted.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0encrypt-secret.ps1" %*
