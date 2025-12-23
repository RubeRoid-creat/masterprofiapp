@echo off
echo ====================================
echo Installing BestApp with MLM button fix
echo ====================================
echo.

echo Step 1: Checking ADB connection...
adb devices
echo.

echo Step 2: Uninstalling old version...
adb uninstall com.example.bestapp
echo.

echo Step 3: Building new version...
call gradlew.bat :app:assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo BUILD FAILED!
    pause
    exit /b 1
)
echo.

echo Step 4: Installing new version...
adb install -r app\build\outputs\apk\debug\app-debug.apk
if %ERRORLEVEL% NEQ 0 (
    echo INSTALL FAILED!
    pause
    exit /b 1
)
echo.

echo Step 5: Starting application...
adb shell am start -n com.example.bestapp/.MainActivity
echo.

echo ====================================
echo Installation completed!
echo ====================================
pause




