@echo off
echo === Building Slitheria ===

:: Clean and create directories
echo Cleaning previous build...
rmdir /s /q bin 2>nul
mkdir bin
mkdir bin\maps 2>nul
mkdir maps 2>nul

:: Compile all Java files
echo Compiling Java files...
javac -d bin src\*.java
if errorlevel 1 (
    echo Error during compilation!
    pause
    exit /b 1
)

:: Copy map files if they exist
echo Copying resource files...
if exist maps\*.txt (
    xcopy /s /y maps\*.txt bin\maps\
)

:: Create JAR
echo Creating JAR file...
jar cfm Slitheria.jar META-INF\MANIFEST.MF -C bin .
if errorlevel 1 (
    echo Error creating JAR!
    pause
    exit /b 1
)

:: Generate initial maps if they don't exist
echo Checking maps...
if not exist maps\map1.txt (
    echo Generating initial maps...
    java -cp bin MapGenerator
)

echo === Build Successful! ===
echo You can run the game with: java -jar Slitheria.jar
pause