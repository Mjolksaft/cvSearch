param(
    [int]$userId = 1
)

$baseUrl = "http://localhost:8080"

# Check if project.json exists
$projectFile = Join-Path $PSScriptRoot "project.json"
if (-not (Test-Path $projectFile)) {
    Write-Host "project.json not found at $projectFile" -ForegroundColor Red
    exit 1
}

# Read and convert projects to a JSON string
$projectData = Get-Content $projectFile -Raw | ConvertFrom-Json
$projectsJson = $projectData.projects | ConvertTo-Json -Compress -Depth 10

# Define the rest of your profile (edit these to match your info)
$body = @{
    summary = "Developer with strong foundations in systems programming, fullstack web development, and Android engineering."
    skills  = '[ "Rust", "Kotlin", "Java", "Python", "React", "Node.js", "Spring Boot", "PostgreSQL", "MongoDB", "Docker", "Git", "REST APIs", "Clean Architecture", "JWT" ]'
    projects = $projectsJson
    education = '[ { "degree": "BSc Computer Science", "school": "University", "year": "2024" } ]'
    languages = '[ "Swedish (native)", "English (fluent)" ]'
    certifications = '[ ]'
} | ConvertTo-Json -Compress -Depth 10

Write-Host "[2/5] Creating profile..." -ForegroundColor Cyan
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/api/profile?userId=$userId" `
        -Method Put `
        -ContentType "application/json" `
        -Body $body

    Write-Host "      ✔ Profile created with $($projectData.projects.Count) projects" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 5
}
catch {
    Write-Host "      ✖ Failed to create profile!" -ForegroundColor Red
    Write-Host "      Status code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $errorBody = $reader.ReadToEnd()
        $reader.Close()
        Write-Host "      Server response: $errorBody" -ForegroundColor Red
    }
    exit 1
}
