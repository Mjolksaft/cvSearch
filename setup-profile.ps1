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
$courseworkJson = @(
    "Introduktion till datavetenskap, 7,5 hp",
    "Grundläggande programmering, 7,5 hp",
    "Objektorienterad programmering i Java, 7,5 hp",
    "Diskret matematik, 7,5 hp",
    "Matematik, grundkurs, 7,5 hp",
    "Databasteknik, 6 hp",
    "Datakommunikation, 7,5 hp",
    "Objektorienterad design, 9 hp",
    "Operativsystem, 7,5 hp",
    "Metoder för hållbar programmering, 7,5 hp",
    "Frontend-utveckling, 7,5 hp",
    "Datasäkerhet, 7,5 hp",
    "Backend-utveckling, 7,5 hp",
    "Algoritmer och datastrukturer, 7,5 hp",
    "Agil webbutveckling, 7,5 hp",
    "Matematisk statistik, 7,5 hp",
    "Maskininlärning, 7,5 hp",
    "Forskningsmetodik för datavetenskap, 7,5 hp",
    "Utveckling av mobila applikationer, 7,5 hp",
    "Analys av stora datamängder, 7,5 hp",
    "Software Engineering, 15 hp",
    "Examensarbete inom datavetenskap, 15 hp"
) | ConvertTo-Json -Compress

$body = @{
    summary = "Passionate fullstack developer with experience in Rust, Kotlin, Java, and modern web frameworks. Strong foundation in clean architecture, systems programming, API design, and Android development. Quick learner who thrives on building robust, maintainable solutions across the full stack."
    skills  = '[ "Rust", "Kotlin", "Java", "Python", "React", "Node.js", "Spring Boot", "PostgreSQL", "MongoDB", "Docker", "Git", "REST APIs", "Clean Architecture", "JWT", "TypeScript", "Android" ]'
    projects = $projectsJson
    education = '[ { "degree": "Bachelor Programme in Software Development (B.Sc.), 180 ECTS", "school": "Kristianstad University", "year": "Expected Graduation: 2026" } ]'
    languages = '[ "Swedish (native)", "English (fluent)" ]'
    certifications = '[ ]'
    coursework = $courseworkJson
    phone = "+46 73 572 52 92"
    github = "Mjolksaft"
    linkedin = "david-kalla-0072133ab"
    city = "Åhus"
    country = "Sweden"
} | ConvertTo-Json -Compress -Depth 10

Write-Host "[2/6] Creating profile..." -ForegroundColor Cyan
try {
    $utf8Body = [System.Text.Encoding]::UTF8.GetBytes($body)
    $response = Invoke-RestMethod -Uri "$baseUrl/api/profile?userId=$userId" `
        -Method Put `
        -ContentType "application/json" `
        -Body $utf8Body

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
