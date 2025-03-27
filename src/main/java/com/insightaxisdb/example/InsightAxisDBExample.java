package com.insightaxisdb.example;

import com.insightaxisdb.storage.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Example demonstrating how to use InsightAxisDB.
 */
public class InsightAxisDBExample {
    
    public static void main(String[] args) {
        try {
            // Create a temporary file for our example
            File tempFile = File.createTempFile("tesseractdb-example", ".ncf");
            tempFile.deleteOnExit();
            
            // Write data to the file
            writeExampleData(tempFile);
            
            // Read data from the file
            readExampleData(tempFile);
            
            // User profile and event example
            userProfileAndEventExample();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void writeExampleData(File file) throws IOException {
        System.out.println("Writing example data to " + file.getAbsolutePath());
        
        // Create NCF writer
        NCF.Writer writer = new NCF.Writer("lz4");
        
        // Add some rows
        for (int i = 0; i < 10; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", i);
            row.put("name", "User " + i);
            row.put("active", i % 2 == 0);
            row.put("score", i * 10.5);
            
            if (i % 3 == 0) {
                // Add some nested data for some rows
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("created_at", System.currentTimeMillis());
                metadata.put("tags", List.of("tag1", "tag2"));
                row.put("metadata", metadata);
            }
            
            writer.addRow(row);
        }
        
        // Write to file
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            writer.write(raf);
        }
        
        System.out.println("Successfully wrote data to file");
    }
    
    private static void readExampleData(File file) throws IOException {
        System.out.println("\nReading data from " + file.getAbsolutePath());
        
        // Create NCF reader
        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             NCF.Reader reader = new NCF.Reader(raf)) {
            
            // Print header info
            System.out.println("File header:");
            System.out.println("  Column count: " + reader.getHeader().getColumnCount());
            System.out.println("  Row count: " + reader.getHeader().getRowCount());
            System.out.println("  Created at: " + reader.getHeader().getCreatedAt());
            System.out.println("  Compression: " + reader.getHeader().getCompression());
            
            // Print column info
            System.out.println("\nColumns:");
            for (Map.Entry<String, ColumnMetadata> entry : reader.getColumns().entrySet()) {
                ColumnMetadata metadata = entry.getValue();
                System.out.println("  " + entry.getKey() + ": " + metadata.getDataType() + 
                        " (offset=" + metadata.getOffset() + ", length=" + metadata.getLength() + ")");
            }
            
            // Read all rows
            System.out.println("\nRows:");
            List<Map<String, Object>> rows = reader.readRows(0, null);
            for (Map<String, Object> row : rows) {
                System.out.println("  " + row);
            }
        }
    }
    
    private static void userProfileAndEventExample() {
        System.out.println("\nUser profile and event example:");
        
        // Create stores
        UserProfileStore profileStore = new UserProfileStore();
        EventStore eventStore = new EventStore(profileStore);
        
        // Create a user
        Map<String, Object> userProperties = new HashMap<>();
        userProperties.put("name", "John Doe");
        userProperties.put("email", "john@example.com");
        userProperties.put("age", 30);
        
        UserProfile profile = profileStore.createProfile("user1", userProperties);
        System.out.println("Created user profile: " + profile.toMap());
        
        // Add some events
        Map<String, Object> eventProps1 = new HashMap<>();
        eventProps1.put("page", "home");
        Event event1 = eventStore.addEvent("page_view", "user1", eventProps1, System.currentTimeMillis());
        
        Map<String, Object> eventProps2 = new HashMap<>();
        eventProps2.put("product_id", "prod123");
        eventProps2.put("price", 99.99);
        Event event2 = eventStore.addEvent("add_to_cart", "user1", eventProps2, System.currentTimeMillis());
        
        // Get user events
        List<Event> userEvents = eventStore.getUserEvents("user1");
        System.out.println("\nUser events:");
        for (Event event : userEvents) {
            System.out.println("  " + event.toMap());
        }
        
        // Get user profile again
        profile = profileStore.getProfile("user1");
        System.out.println("\nUpdated user profile: " + profile.toMap());
    }
}
