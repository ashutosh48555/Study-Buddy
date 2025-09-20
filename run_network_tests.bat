@echo off
REM End-to-End Network Connectivity Testing Script
REM This script runs comprehensive network tests for the StudyBuddy app

echo ===========================================
echo     StudyBuddy Network Connectivity Tests
echo ===========================================

REM Check if Android SDK is available
if not exist "%ANDROID_HOME%\platform-tools\adb.exe" (
    echo ERROR: Android SDK not found. Please set ANDROID_HOME environment variable.
    pause
    exit /b 1
)

REM Check if device is connected
"%ANDROID_HOME%\platform-tools\adb.exe" devices | findstr "device" > nul
if %errorlevel% neq 0 (
    echo ERROR: No Android device connected. Please connect a device or start an emulator.
    pause
    exit /b 1
)

echo Device connected successfully.
echo.

REM Build the app
echo Building the app...
call gradlew.bat assembleDebug
if %errorlevel% neq 0 (
    echo ERROR: Build failed.
    pause
    exit /b 1
)

echo Build successful.
echo.

REM Install the app
echo Installing the app...
"%ANDROID_HOME%\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
if %errorlevel% neq 0 (
    echo ERROR: Installation failed.
    pause
    exit /b 1
)

echo Installation successful.
echo.

REM Run instrumented tests
echo Running network connectivity tests...
echo.

echo === Test 1: API 21+ Compatibility ===
call gradlew.bat connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.studybuddy.NetworkConnectivityE2ETest#testAPI21PlusCompatibility

echo.
echo === Test 2: Wi-Fi Playback ===
call gradlew.bat connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.studybuddy.NetworkConnectivityE2ETest#testSeamlessPlaybackOnWiFi

echo.
echo === Test 3: Mobile Data Playback ===
call gradlew.bat connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.studybuddy.NetworkConnectivityE2ETest#testSeamlessPlaybackOn4G

echo.
echo === Test 4: Airplane Mode Scenario ===
call gradlew.bat connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.studybuddy.NetworkConnectivityE2ETest#testAirplaneModeAndRecovery

echo.
echo === Test 5: Network Connectivity Loss ===
call gradlew.bat connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.studybuddy.NetworkConnectivityE2ETest#testConnectivityLossAndRecovery

echo.
echo === Test 6: Buffering and Caching ===
call gradlew.bat connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.studybuddy.NetworkConnectivityE2ETest#testBufferingAndCaching

echo.
echo === Test 7: Track Switching ===
call gradlew.bat connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.studybuddy.NetworkConnectivityE2ETest#testTrackSwitchingUnderNetworkConditions

echo.
echo === Test 8: Retry Mechanism ===
call gradlew.bat connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.studybuddy.NetworkConnectivityE2ETest#testRetryMechanismAfterNetworkFailures

echo.
echo === Test 9: Notification Controls ===
call gradlew.bat connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.studybuddy.NetworkConnectivityE2ETest#testNotificationControlsDuringNetworkChanges

echo.
echo === All Tests Complete ===
echo.
echo Network connectivity tests finished.
echo Please review the test results above.
echo.
echo For comprehensive testing, also run the manual tests described in:
echo app/src/test/java/com/example/studybuddy/ManualNetworkTestingGuide.kt
echo.

pause
