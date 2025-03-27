#!/bin/bash

# Create the new package structure
mkdir -p src/main/java/com/insightaxisdb/{api,core,example,ml,query,segmentation,storage}
mkdir -p src/test/java/com/insightaxisdb/{api,core,example,ml,query,segmentation,storage}

# Function to copy and update a file
copy_and_update() {
    local src=$1
    local dest=$2
    
    # Create the destination directory if it doesn't exist
    mkdir -p $(dirname "$dest")
    
    # Copy the file
    cp "$src" "$dest"
    
    # Update the package declaration
    sed -i '' 's/package com.tesseractdb/package com.insightaxisdb/g' "$dest"
    
    # Update import statements
    sed -i '' 's/import com.tesseractdb/import com.insightaxisdb/g' "$dest"
    
    # Update class names
    sed -i '' 's/TesseractDB/InsightAxisDB/g' "$dest"
    
    echo "Migrated: $src -> $dest"
}

# Process main Java files
for file in $(find src/main/java/com/tesseractdb -name "*.java"); do
    # Skip the TesseractDBServer.java file since we've already created InsightAxisDBServer.java
    if [[ "$file" == *"TesseractDBServer.java" ]]; then
        continue
    fi
    
    # Determine the new file path
    new_file=${file/tesseractdb/insightaxisdb}
    
    # If the file has "TesseractDB" in its name, rename it
    if [[ "$file" == *"TesseractDB"* ]]; then
        new_file=${new_file/TesseractDB/InsightAxisDB}
    fi
    
    # Copy and update the file
    copy_and_update "$file" "$new_file"
done

# Process test Java files
for file in $(find src/test/java/com/tesseractdb -name "*.java"); do
    # Determine the new file path
    new_file=${file/tesseractdb/insightaxisdb}
    
    # If the file has "TesseractDB" in its name, rename it
    if [[ "$file" == *"TesseractDB"* ]]; then
        new_file=${new_file/TesseractDB/InsightAxisDB}
    fi
    
    # Copy and update the file
    copy_and_update "$file" "$new_file"
done

echo "Migration complete!"
