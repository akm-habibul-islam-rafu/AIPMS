@echo off
echo Compiling Java files...
javac -cp ".;lib/jfreechart-1.0.19.jar;lib/jcommon-1.0.23.jar" -d . src\*.java src\models\*.java src\services\*.java src\ui\*.java
if errorlevel 1 (
    echo Compilation failed!
    pause
    exit /b 1
)
echo Compilation successful!
echo Running application...
java -cp ".;lib/jfreechart-1.0.19.jar;lib/jcommon-1.0.23.jar" src.Main
pause 