#!/bin/sh

echo "Starting build..."

TASK="build"

if [ "$1" = "--clean" ]; then
    TASK="clean build"
fi

if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    ./gradlew $TASK
elif [ -f "./gradlew.bat" ]; then
    ./gradlew.bat $TASK
else
    echo "Error: gradlew or gradlew.bat not found."
    exit 1
fi

if [ $? -ne 0 ]; then
    echo "Build failed."
    exit 1
fi

JAR=$(find build/libs -maxdepth 1 -type f -name "*.jar" ! -name "*-sources.jar" ! -name "*-javadoc.jar" | head -n 1)

if [ -z "$JAR" ]; then
    echo "Build completed but no JAR was found."
    exit 1
fi

echo "Build successful."
echo "Jar: $JAR"

if command -v jar >/dev/null 2>&1; then
    jar tf "$JAR" >/dev/null 2>&1 || exit 1
elif command -v unzip >/dev/null 2>&1; then
    unzip -t "$JAR" >/dev/null 2>&1 || exit 1
fi

echo "Verification successful."