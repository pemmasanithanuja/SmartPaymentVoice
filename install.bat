@echo off
setlocal
set "APK=%~dp0SmartPaymentVoice-v1.0.apk"
set "ADB=adb"

where adb >nul 2>nul
if errorlevel 1 (
  if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
    set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
  ) else (
    echo ADB was not found. Install Android platform-tools and run this file again.
    pause
    exit /b 1
  )
)

if not exist "%APK%" (
  echo Put SmartPaymentVoice-v1.0.apk in the same folder as install.bat and run again.
  pause
  exit /b 1
)

echo Connect the phone with a USB cable and allow USB debugging on the phone.
"%ADB%" devices
"%ADB%" install -r "%APK%"
echo.
echo Done. Now open the app on the phone and turn on Notification access.
pause
