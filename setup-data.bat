@echo off
echo ========================================
echo   Setting up CV Search - Your Data
echo ========================================
echo.

:: --- 1. Create a user ---
echo [1/5] Creating user...
curl -s -X POST http://localhost:8080/api/users ^
  -H "Content-Type: application/json" ^
  -d "{\"name\": \"David Kalla\", \"email\": \"Kalla01@hotmail.se\", \"password\": \"pass123\", \"role\": \"USER\"}"

echo.
echo.

:: --- 2. Create your profile (reads project.json) ---
echo [2/5] Creating profile from project.json...
powershell -ExecutionPolicy Bypass -File "%~dp0setup-profile.ps1"

echo.
echo.

:: --- 3. Fetch some jobs from the API ---
echo [3/5] Fetching job listings...
curl -s "http://localhost:8080/api/jobs/fetch?q=java" > nul
echo       ✔ Jobs fetched (output suppressed)

echo.
echo.

:: --- 4. Update a job status to "Saved" ---
echo [4/5] Marking job #1 as saved/applied...
curl -s -X PATCH http://localhost:8080/api/jobs/1 ^
  -H "Content-Type: application/json" ^
  -d "{\"status\": \"Saved\"}"

echo.
echo.

:: --- 5. Generate a CV prompt for job #1 ---
echo [5/5] Generating CV prompt...
curl -s "http://localhost:8080/api/jobs/1/cv-prompt?userId=1"

echo.
echo.
echo ========================================
echo   Done! Copy the CV prompt from step 5
echo   and paste it into ChatGPT.
echo ========================================
pause
