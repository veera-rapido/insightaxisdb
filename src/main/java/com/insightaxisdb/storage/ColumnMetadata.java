package com.insightaxisdb.storage;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Metadata for a column in NCF.
 */
public class ColumnMetadata {
    
    private final String name;
    private final DataType dataType;
    private final long offset;
    private final long length;
    private final boolean nullable;
    
    /**
     * Initialize column metadata.
     *
     * @param name Column name
     * @param dataType Data type of the column
     * @param offset Byte offset in the file
     * @param length Length of the column data in bytes
     * @param nullable Whether the column can contain null values
     */
    public ColumnMetadata(String name, DataType dataType, long offset, long length, boolean nullable) {
        this.name = name;
        this.dataType = dataType;
        this.offset = offset;
        this.length = length;
        this.nullable = nullable;
    }
    
    /**
     * Serialize column metadata to bytes.
     */
    public byte[] serialize() {
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        
        ByteBuffer buffer = ByteBuffer.allocate(2 + nameBytes.length + 1 + 8 + 8 + 1);
        
        // Name length and name
        buffer.putShort((short) nameBytes.length);
        buffer.put(nameBytes);
        
        // Data type
        buffer.put((byte) dataType.getValue());
        
        // Offset and length
        buffer.putLong(offset);
        buffer.putLong(length);
        
        // Nullable
        buffer.put((byte) (nullable ? 1 : 0));
        
        return buffer.array();
    }
    
    /**
     * Deserialize column metadata from bytes.
     *
     * @param data Byte array containing serialized metadata
     * @param offset Offset in the byte array to start reading from
     * @return Tuple of (ColumnMetadata, next offset)
     */
    public static DeserializeResult deserialize(byte[] data, int offset) {
        ByteBuffer buffer = ByteBuffer.wrap(data, offset, data.length - offset);
        
        // Read name length and name
        short nameLength = buffer.getShort();
        byte[] nameBytes = new byte[nameLength];
        buffer.get(nameBytes);
        String name = new String(nameBytes, StandardCharsets.UTF_8);
        
        // Read data type
        byte dataTypeValue = buffer.get();
        DataType dataType = DataType.fromValue(dataTypeValue);
        
        // Read offset and length
        long columnOffset = buffer.getLong();
        long length = buffer.getLong();
        
        // Read nullable flag
        boolean nullable = buffer.get() != 0;
        
        // Calculate next offset
        int nextOffset = offset + 2 + nameLength + 1 + 8 + 8 + 1;
        
        return new DeserializeResult(
            new ColumnMetadata(name, dataType, columnOffset, length, nullable),
            nextOffset
        );
    }
    
    // Getters
    
    public String getName() {
        return name;
    }
    
    public DataType getDataType() {
        return dataType;
    }
    
    public long getOffset() {
        return offset;
    }
    
    public long getLength() {
        return length;
    }
    
    public boolean isNullable() {
        return nullable;
    }
    
    /**
     * Result of deserializing column metadata.
     */
    public static class DeserializeResult {
        private final ColumnMetadata metadata;
        private final int nextOffset;
        
        public DeserializeResult(ColumnMetadata metadata, int nextOffset) {
            this.metadata = metadata;
            this.nextOffset = nextOffset;
        }
        
        public ColumnMetadata getMetadata() {
            return metadata;
        }
        
        public int getNextOffset() {
            return nextOffset;
        }
    }
}
