@echo off
setlocal
echo ========================================================================
echo   PHAROAH Multi-Agent Orchestration System - Java 21 Build
echo ========================================================================
if not exist "target\classes" mkdir target\classes

echo [1/2] Compiling Java 21 LTS source files...
powershell -NoProfile -ExecutionPolicy Bypass -Command "javac --release 21 -cp 'lib/*' -d target/classes (Get-ChildItem -Path src/main/java -Filter *.java -Recurse | Select-Object -ExpandProperty FullName)"

if %ERRORLEVEL% neq 0 (
    echo [PHAROAH BUILD ERROR] Compilation failed with exit code %ERRORLEVEL%.
    exit /b %ERRORLEVEL%
)

echo [2/2] Synchronizing classpath resources (prompts, db.properties)...
powershell -NoProfile -ExecutionPolicy Bypass -Command "if (Test-Path src/main/resources) { Copy-Item -Recurse -Force src/main/resources/* target/classes/ }"

echo [PHAROAH] Build successful! Bytecode and resources ready in target\classes.
endlocal
