#!/bin/bash
set -e

echo "Building GridX..."

# Clean and create output directory
rm -rf out
mkdir -p out

# Compile all Java files
echo "Compiling..."
find src/main/java -name "*.java" | xargs javac -d out -sourcepath src/main/java

echo "Compilation successful!"
echo ""
echo "Run with: java -cp out com.gridx.ui.DashboardFrame"
