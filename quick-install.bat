@echo off
echo ========================================
echo   Быстрая установка BestApp v1.2.0
echo ========================================
echo.

REM Проверка подключенного устройства
echo Проверка подключенного устройства...
adb devices
echo.

REM Удаление старой версии
echo Удаление старой версии...
adb uninstall com.example.bestapp 2>nul
echo.

REM Сборка и установка
echo Сборка и установка новой версии...
call gradlew.bat :app:assembleDebug :app:installDebug
echo.

REM Запуск приложения
echo Запуск приложения...
adb shell am start -n com.example.bestapp/.MainActivity
echo.

echo ========================================
echo   Установка завершена!
echo   Версия: 1.2.0 (Build 12)
echo ========================================
pause




