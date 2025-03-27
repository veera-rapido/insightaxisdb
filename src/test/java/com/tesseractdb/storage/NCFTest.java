package com.tesseractdb.storage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test cases for the Network Columnar Format (NCF) implementation.
 */
public class NCFTest {
    
    private File tempFile;
    
    @Before
    public void setUp() throws IOException {
        // Create a temporary file for testing
        tempFile = File.createTempFile("ncf-test", ".ncf");
        tempFile.deleteOnExit();
    }
    
    @After
    public void tearDown() {
        // Delete the temporary file
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }
    
    @Test
    public void testWriteAndReadSimpleData() throws IOException {
        // Create NCF writer
        NCF.Writer writer = new NCF.Writer("lz4");
        
        // Add some rows
        for (int i = 0; i < 5; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", i);
            row.put("name", "User " + i);
            row.put("active", i % 2 == 0);
            
            writer.addRow(row);
        }
        
        // Write to file
        try (RandomAccessFile raf = new RandomAccessFile(tempFile, "rw")) {
            writer.write(raf);
        }
        
        // Read from file
        try (RandomAccessFile raf = new RandomAccessFile(tempFile, "r");
             NCF.Reader reader = new NCF.Reader(raf)) {
            
            // Check header
            assertEquals(3, reader.getHeader().getColumnCount());
            assertEquals(5, reader.getHeader().getRowCount());
            assertEquals("lz4", reader.getHeader().getCompression());
            
            // Check columns
            assertTrue(reader.getColumns().containsKey("id"));
            assertTrue(reader.getColumns().containsKey("name"));
            assertTrue(reader.getColumns().containsKey("active"));
            
            // Read all rows
            List<Map<String, Object>> rows = reader.readRows(0, null);
            assertEquals(5, rows.size());
            
            // Check row values
            for (int i = 0; i < 5; i++) {
                Map<String, Object> row = rows.get(i);
                assertEquals(i, ((Number) row.get("id")).intValue());
                assertEquals("User " + i, row.get("name"));
                assertEquals(i % 2 == 0, row.get("active"));
            }
        }
    }
    
    @Test
    public void testWriteAndReadComplexData() throws IOException {
        // Create NCF writer
        NCF.Writer writer = new NCF.Writer("lz4");
        
        // Add some rows with complex data types
        for (int i = 0; i < 3; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", i);
            row.put("name", "User " + i);
            
            // Add nested data
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("created_at", System.currentTimeMillis());
            metadata.put("tags", List.of("tag1", "tag2"));
            row.put("metadata", metadata);
            
            // Add null value for some rows
            if (i % 2 == 0) {
                row.put("optional", "value" + i);
            } else {
                row.put("optional", null);
            }
            
            writer.addRow(row);
        }
        
        // Write to file
        try (RandomAccessFile raf = new RandomAccessFile(tempFile, "rw")) {
            writer.write(raf);
        }
        
        // Read from file
        try (RandomAccessFile raf = new RandomAccessFile(tempFile, "r");
             NCF.Reader reader = new NCF.Reader(raf)) {
            
            // Check header
            assertEquals(4, reader.getHeader().getColumnCount());
            assertEquals(3, reader.getHeader().getRowCount());
            
            // Read all rows
            List<Map<String, Object>> rows = reader.readRows(0, null);
            assertEquals(3, rows.size());
            
            // Check row values
            for (int i = 0; i < 3; i++) {
                Map<String, Object> row = rows.get(i);
                assertEquals(i, ((Number) row.get("id")).intValue());
                assertEquals("User " + i, row.get("name"));
                
                // Check metadata
                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = (Map<String, Object>) row.get("metadata");
                assertNotNull(metadata);
                assertTrue(metadata.containsKey("created_at"));
                assertTrue(metadata.containsKey("tags"));
                
                @SuppressWarnings("unchecked")
                List<String> tags = (List<String>) metadata.get("tags");
                assertEquals(2, tags.size());
                assertEquals("tag1", tags.get(0));
                assertEquals("tag2", tags.get(1));
                
                // Check optional field
                if (i % 2 == 0) {
                    assertEquals("value" + i, row.get("optional"));
                } else {
                    assertNull(row.get("optional"));
                }
            }
        }
    }
    
    @Test
    public void testReadSpecificColumns() throws IOException {
        // Create NCF writer
        NCF.Writer writer = new NCF.Writer("lz4");
        
        // Add some rows
        for (int i = 0; i < 5; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", i);
            row.put("name", "User " + i);
            row.put("email", "user" + i + "@example.com");
            row.put("age", 20 + i);
            
            writer.addRow(row);
        }
        
        // Write to file
        try (RandomAccessFile raf = new RandomAccessFile(tempFile, "rw")) {
            writer.write(raf);
        }
        
        // Read from file
        try (RandomAccessFile raf = new RandomAccessFile(tempFile, "r");
             NCF.Reader reader = new NCF.Reader(raf)) {
            
            // Read specific columns
            List<Object> ids = reader.readColumn("id");
            List<Object> names = reader.readColumn("name");
            
            assertEquals(5, ids.size());
            assertEquals(5, names.size());
            
            for (int i = 0; i < 5; i++) {
                assertEquals(i, ((Number) ids.get(i)).intValue());
                assertEquals("User " + i, names.get(i));
            }
        }
    }
    
    @Test
    public void testReadRowRange() throws IOException {
        // Create NCF writer
        NCF.Writer writer = new NCF.Writer("lz4");
        
        // Add some rows
        for (int i = 0; i < 10; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", i);
            row.put("name", "User " + i);
            
            writer.addRow(row);
        }
        
        // Write to file
        try (RandomAccessFile raf = new RandomAccessFile(tempFile, "rw")) {
            writer.write(raf);
        }
        
        // Read from file
        try (RandomAccessFile raf = new RandomAccessFile(tempFile, "r");
             NCF.Reader reader = new NCF.Reader(raf)) {
            
            // Read a range of rows
            List<Map<String, Object>> rows = reader.readRows(3, 4);
            assertEquals(4, rows.size());
            
            for (int i = 0; i < 4; i++) {
                Map<String, Object> row = rows.get(i);
                assertEquals(i + 3, ((Number) row.get("id")).intValue());
                assertEquals("User " + (i + 3), row.get("name"));
            }
        }
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRowRange() throws IOException {
        // Create NCF writer
        NCF.Writer writer = new NCF.Writer("lz4");
        
        // Add some rows
        for (int i = 0; i < 5; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", i);
            
            writer.addRow(row);
        }
        
        // Write to file
        try (RandomAccessFile raf = new RandomAccessFile(tempFile, "rw")) {
            writer.write(raf);
        }
        
        // Read from file with invalid range
        try (RandomAccessFile raf = new RandomAccessFile(tempFile, "r");
             NCF.Reader reader = new NCF.Reader(raf)) {
            
            // This should throw an exception
            reader.readRows(6, 1);
        }
    }
}
