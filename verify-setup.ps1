# Study Buddy - Setup Verification Script
# This script verifies that all essential files are present for the application to run

Write-Host "Verifying Study Buddy Setup..." -ForegroundColor Green
Write-Host "=================================" -ForegroundColor Green

$errors = @()
$warnings = @()

# Check essential project files
$essentialFiles = @(
    "build.gradle.kts",
    "settings.gradle.kts", 
    "gradle.properties",
    "local.properties",
    "app/build.gradle.kts",
    "app/google-services.json",
    "app/src/main/AndroidManifest.xml",
    "gradle/wrapper/gradle-wrapper.jar",
    "gradle/wrapper/gradle-wrapper.properties",
    "gradle/libs.versions.toml"
)

Write-Host "`nChecking essential project files..." -ForegroundColor Yellow
foreach ($file in $essentialFiles) {
    if (Test-Path $file) {
        Write-Host "OK $file" -ForegroundColor Green
    } else {
        Write-Host "MISSING $file" -ForegroundColor Red
        $errors += $file
    }
}

# Check Kotlin source files
Write-Host "`nChecking Kotlin source files..." -ForegroundColor Yellow
$kotlinFiles = Get-ChildItem -Recurse -Name -Include "*.kt"
if ($kotlinFiles.Count -gt 0) {
    Write-Host "Found $($kotlinFiles.Count) Kotlin files" -ForegroundColor Green
} else {
    Write-Host "No Kotlin files found!" -ForegroundColor Red
    $errors += "Kotlin source files"
}

# Check XML resource files
Write-Host "`nChecking XML resource files..." -ForegroundColor Yellow
$xmlFiles = Get-ChildItem -Recurse -Name -Include "*.xml"
if ($xmlFiles.Count -gt 0) {
    Write-Host "Found $($xmlFiles.Count) XML files" -ForegroundColor Green
} else {
    Write-Host "No XML files found!" -ForegroundColor Red
    $errors += "XML resource files"
}

# Check JSON files
Write-Host "`nChecking JSON configuration files..." -ForegroundColor Yellow
$jsonFiles = Get-ChildItem -Recurse -Name -Include "*.json"
if ($jsonFiles.Count -gt 0) {
    Write-Host "Found $($jsonFiles.Count) JSON files" -ForegroundColor Green
} else {
    Write-Host "No JSON files found!" -ForegroundColor Red
    $errors += "JSON configuration files"
}

# Check documentation files
Write-Host "`nChecking documentation files..." -ForegroundColor Yellow
$docFiles = @("README.md", "LICENSE", "SETUP.md", "CONTRIBUTING.md", "CHANGELOG.md")
foreach ($file in $docFiles) {
    if (Test-Path $file) {
        Write-Host "OK $file" -ForegroundColor Green
    } else {
        Write-Host "MISSING $file (optional)" -ForegroundColor Yellow
        $warnings += $file
    }
}

# Check GitHub templates
Write-Host "`nChecking GitHub templates..." -ForegroundColor Yellow
$githubFiles = @(
    ".github/ISSUE_TEMPLATE/bug_report.md",
    ".github/ISSUE_TEMPLATE/feature_request.md",
    ".github/pull_request_template.md"
)
foreach ($file in $githubFiles) {
    if (Test-Path $file) {
        Write-Host "OK $file" -ForegroundColor Green
    } else {
        Write-Host "MISSING $file (optional)" -ForegroundColor Yellow
        $warnings += $file
    }
}

# Check .gitignore
Write-Host "`nChecking .gitignore..." -ForegroundColor Yellow
if (Test-Path ".gitignore") {
    Write-Host "OK .gitignore present" -ForegroundColor Green
} else {
    Write-Host "MISSING .gitignore" -ForegroundColor Red
    $errors += ".gitignore"
}

# Summary
Write-Host "`nSummary" -ForegroundColor Green
Write-Host "=========" -ForegroundColor Green

if ($errors.Count -eq 0) {
    Write-Host "All essential files are present! The project is ready to run." -ForegroundColor Green
    Write-Host "`nNext steps:" -ForegroundColor Yellow
    Write-Host "1. Open the project in Android Studio" -ForegroundColor White
    Write-Host "2. Sync the project with Gradle files" -ForegroundColor White
    Write-Host "3. Build and run the application" -ForegroundColor White
} else {
    Write-Host "Found $($errors.Count) missing essential files:" -ForegroundColor Red
    foreach ($error in $errors) {
        Write-Host "   - $error" -ForegroundColor Red
    }
    Write-Host "`nPlease ensure all essential files are present before running the application." -ForegroundColor Red
}

if ($warnings.Count -gt 0) {
    Write-Host "`nFound $($warnings.Count) missing optional files:" -ForegroundColor Yellow
    foreach ($warning in $warnings) {
        Write-Host "   - $warning" -ForegroundColor Yellow
    }
    Write-Host "`nThese files are optional but recommended for a complete project." -ForegroundColor Yellow
}

Write-Host "`nVerification complete!" -ForegroundColor Green