@echo off
echo ========================================================================
echo   PHAROAH Multi-Agent Orchestration System - Java 21 Build
echo ========================================================================
if not exist "target\classes" mkdir target\classes

powershell -NoProfile -ExecutionPolicy Bypass -Command "javac --release 21 -cp 'lib/*' -d target/classes (Get-ChildItem -Path src/main/java -Filter *.java -Recurse | Select-Object -ExpandProperty FullName)"

if %ERRORLEVEL% equ 0 (
    echo [PHAROAH] Build successful! Target bytecode: Java 21 LTS.
) else (
    echo [PHAROAH] Build failed with exit code %ERRORLEVEL%.
)
