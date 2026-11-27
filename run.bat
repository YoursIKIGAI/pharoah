@echo off
setlocal
if not exist "target\classes\com\pharoah\app\PharoahApplication.class" (
    echo [PHAROAH] Compiled classes not found. Triggering build.bat...
    call build.bat
    if %ERRORLEVEL% neq 0 (
        echo [PHAROAH ERROR] Build failed. Cannot run application.
        exit /b %ERRORLEVEL%
    )
)

java -cp "target/classes;lib/*" com.pharoah.app.PharoahApplication %*
endlocal
