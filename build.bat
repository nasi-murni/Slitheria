@echo off
echo === Building Text Game ===

:: Clean and create bin directory
echo Cleaning previous build...
rmdir /s /q bin 2>nul
mkdir bin

:: Compile
echo Compiling Java files...
javac -d bin src\*.java
if errorlevel 1 (
    echo Error during compilation!
    pause
    exit /b 1
)

:: Copy resources
echo Copying map files...
xcopy /s /y src\maps bin\maps\
if errorlevel 1 (
    echo Error copying resources!
    pause
    exit /b 1
)

:: Create JAR
echo Creating JAR file...
jar cfm TextGame.jar META-INF\MANIFEST.MF -C bin .
if errorlevel 1 (
    echo Error creating JAR!
    pause
    exit /b 1
)

echo === Build Successful! ===
echo You can run the game with: java -jar TextGame.jar
pause