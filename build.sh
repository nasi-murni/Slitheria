#!/bin/bash

echo "=== Building Slitheria ==="

# Clean and create directories
echo "Cleaning previous build..."
rm -rf bin
mkdir -p bin/maps
mkdir -p maps

# Compile
echo "Compiling Java files..."
javac -d bin src/*.java
if [ $? -ne 0 ]; then
    echo "Error during compilation!"
    exit 1
fi

# Copy resources
echo "Copying resource files..."
if ls maps/*.txt 1> /dev/null 2>&1; then
    cp -r maps/*.txt bin/maps/
fi

# Create JAR
echo "Creating JAR file..."
jar cfm Slitheria.jar META-INF/MANIFEST.MF -C bin .
if [ $? -ne 0 ]; then
    echo "Error creating JAR!"
    exit 1
fi

# Generate initial maps if they don't exist
echo "Checking maps..."
if [ ! -f maps/map1.txt ]; then
    echo "Generating initial maps..."
    java -cp bin MapGenerator
fi

echo "=== Build Successful! ==="
echo "You can run the game with: java -jar Slitheria.jar"