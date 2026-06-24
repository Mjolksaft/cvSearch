@echo off
echo ========================================
echo   Setting up CV Search - Your Data
echo ========================================
echo.

:: --- 1. Create a user ---
echo [1/3] Creating user...
curl -s -X POST http://localhost:8080/api/users ^
  -H "Content-Type: application/json" ^
  -d "{\"name\": \"David Kalla\", \"email\": \"Kalla01@hotmail.se\", \"password\": \"pass123\", \"role\": \"USER\"}"

echo.
echo.

:: --- 2. Create your profile (reads project.json, includes coursework) ---
echo [2/3] Creating profile from project.json + coursework...
powershell -ExecutionPolicy Bypass -File "%~dp0setup-profile.ps1"

echo.
echo.

:: --- 3. Fetch some jobs from the API ---
echo [3/3] Fetching job listings...
curl -s "http://localhost:8080/api/jobs/fetch?q=java" > nul
echo       ✔ Jobs fetched (output suppressed)

echo.
echo.


echo.
echo.
echo ========================================
echo   Done!:
echo ========================================
pause
