#!/bin/bash

echo "=== Building Text Game ==="

# Clean and create bin directory
echo "Cleaning previous build..."
rm -rf bin
mkdir bin

# Compile
echo "Compiling Java files..."
javac -d bin src/*.java
if [ $? -ne 0 ]; then
    echo "Error during compilation!"
    exit 1
fi

# Copy resources
echo "Copying map files..."
cp -r src/maps bin/maps
if [ $? -ne 0 ]; then
    echo "Error copying resources!"
    exit 1
fi

# Create JAR
echo "Creating JAR file..."
jar cfm TextGame.jar META-INF/MANIFEST.MF -C bin .
if [ $? -ne 0 ]; then
    echo "Error creating JAR!"
    exit 1
fi

echo "=== Build Successful! ==="
echo "You can run the game with: java -jar TextGame.jar"