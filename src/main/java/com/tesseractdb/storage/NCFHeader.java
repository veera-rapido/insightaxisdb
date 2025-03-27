package com.tesseractdb.storage;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Header for NCF files.
 */
public class NCFHeader {
    
    private static final byte[] MAGIC = "NCF1".getBytes(StandardCharsets.UTF_8);
    
    private final int columnCount;
    private final int rowCount;
    private final long createdAt;
    private final String compression;
    
    /**
     * Initialize NCF header.
     *
     * @param columnCount Number of columns in the file
     * @param rowCount Number of rows in the file
     * @param createdAt Timestamp when the file was created
     * @param compression Compression algorithm used
     */
    public NCFHeader(int columnCount, int rowCount, long createdAt, String compression) {
        this.columnCount = columnCount;
        this.rowCount = rowCount;
        this.createdAt = createdAt;
        this.compression = compression;
    }
    
    /**
     * Serialize header to bytes.
     */
    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(28);
        
        // Magic bytes
        buffer.put(MAGIC);
        
        // Column count, row count, created at
        buffer.putInt(columnCount);
        buffer.putInt(rowCount);
        buffer.putLong(createdAt);
        
        // Compression algorithm
        byte[] compressionBytes = compression.getBytes(StandardCharsets.UTF_8);
        buffer.putShort((short) compressionBytes.length);
        
        // Ensure compression algorithm name fits in 10 bytes
        byte[] paddedCompressionBytes = new byte[10];
        System.arraycopy(compressionBytes, 0, paddedCompressionBytes, 0, 
                Math.min(compressionBytes.length, 10));
        
        buffer.put(paddedCompressionBytes);
        
        return buffer.array();
    }
    
    /**
     * Deserialize header from bytes.
     */
    public static NCFHeader deserialize(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        
        // Check magic bytes
        byte[] magic = new byte[4];
        buffer.get(magic);
        
        if (!Arrays.equals(magic, MAGIC)) {
            throw new IllegalArgumentException("Invalid NCF file: magic bytes mismatch");
        }
        
        // Read header fields
        int columnCount = buffer.getInt();
        int rowCount = buffer.getInt();
        long createdAt = buffer.getLong();
        
        // Read compression algorithm
        short compressionLength = buffer.getShort();
        byte[] compressionBytes = new byte[10];
        buffer.get(compressionBytes);
        
        String compression = new String(compressionBytes, 0, compressionLength, StandardCharsets.UTF_8);
        
        return new NCFHeader(columnCount, rowCount, createdAt, compression);
    }
    
    // Getters
    
    public int getColumnCount() {
        return columnCount;
    }
    
    public int getRowCount() {
        return rowCount;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public String getCompression() {
        return compression;
    }
}
