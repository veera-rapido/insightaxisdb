package com.insightaxisdb.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Network Columnar Format (NCF) implementation.
 *
 * This class implements the NCF storage format, which is a columnar storage format
 * optimized for storing user behavior data efficiently.
 */
public class NCF {
    private static final Logger logger = LoggerFactory.getLogger(NCF.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Writer for NCF files.
     */
    public static class Writer {
        private final String compression;
        private final Map<String, ColumnData> columns = new HashMap<>();
        private int rowCount = 0;

        /**
         * Initialize NCF writer.
         *
         * @param compression Compression algorithm to use
         */
        public Writer(String compression) {
            this.compression = compression;
        }

        /**
         * Add a new column.
         *
         * @param name Column name
         * @param dataType Data type of the column
         */
        public void addColumn(String name, DataType dataType) {
            if (columns.containsKey(name)) {
                throw new IllegalArgumentException("Column '" + name + "' already exists");
            }

            columns.put(name, new ColumnData(dataType, new ArrayList<>()));
        }

        /**
         * Add a row of data.
         *
         * @param rowData Map of column name to value
         */
        public void addRow(Map<String, Object> rowData) {
            // Add any new columns that don't exist yet
            for (Map.Entry<String, Object> entry : rowData.entrySet()) {
                String colName = entry.getKey();
                Object value = entry.getValue();

                if (!columns.containsKey(colName)) {
                    DataType dataType = inferDataType(value);
                    addColumn(colName, dataType);
                }
            }

            // Add values to each column
            for (Map.Entry<String, ColumnData> entry : columns.entrySet()) {
                String colName = entry.getKey();
                List<Object> values = entry.getValue().values;

                if (rowData.containsKey(colName)) {
                    values.add(rowData.get(colName));
                } else {
                    values.add(null);
                }
            }

            rowCount++;
        }

        /**
         * Infer the data type of a value.
         */
        private DataType inferDataType(Object value) {
            if (value == null) {
                return DataType.NULL;
            } else if (value instanceof Boolean) {
                return DataType.BOOLEAN;
            } else if (value instanceof Integer || value instanceof Long) {
                return DataType.INTEGER;
            } else if (value instanceof Float || value instanceof Double) {
                return DataType.FLOAT;
            } else if (value instanceof String) {
                return DataType.STRING;
            } else if (value instanceof List) {
                return DataType.ARRAY;
            } else if (value instanceof Map) {
                return DataType.OBJECT;
            } else {
                // Default to string for unknown types
                return DataType.STRING;
            }
        }

        /**
         * Compress a column of values.
         */
        private byte[] compressColumn(List<Object> values, DataType dataType) throws IOException {
            // This is a simplified implementation
            // In a real implementation, we would use the specified compression algorithm

            // Serialize values based on data type
            ByteArrayOutputStream serialized = new ByteArrayOutputStream();

            // Write null bitmap if needed
            boolean hasNulls = values.stream().anyMatch(Objects::isNull);
            if (hasNulls) {
                int bitmapSize = (values.size() + 7) / 8;
                byte[] nullBitmap = new byte[bitmapSize];

                for (int i = 0; i < values.size(); i++) {
                    if (values.get(i) == null) {
                        int byteIndex = i / 8;
                        int bitIndex = i % 8;
                        nullBitmap[byteIndex] |= (1 << bitIndex);
                    }
                }

                serialized.write(nullBitmap);
            }

            // Write non-null values
            for (int i = 0; i < values.size(); i++) {
                Object value = values.get(i);
                if (value == null) {
                    continue;
                }

                switch (dataType) {
                    case BOOLEAN:
                        serialized.write(((Boolean) value) ? 1 : 0);
                        break;
                    case INTEGER:
                        ByteBuffer intBuffer = ByteBuffer.allocate(8);
                        if (value instanceof Integer) {
                            intBuffer.putLong(((Integer) value).longValue());
                        } else {
                            intBuffer.putLong((Long) value);
                        }
                        serialized.write(intBuffer.array());
                        break;
                    case FLOAT:
                        ByteBuffer floatBuffer = ByteBuffer.allocate(8);
                        if (value instanceof Float) {
                            floatBuffer.putDouble(((Float) value).doubleValue());
                        } else {
                            floatBuffer.putDouble((Double) value);
                        }
                        serialized.write(floatBuffer.array());
                        break;
                    case STRING:
                        byte[] stringBytes = ((String) value).getBytes(StandardCharsets.UTF_8);
                        ByteBuffer stringLenBuffer = ByteBuffer.allocate(4);
                        stringLenBuffer.putInt(stringBytes.length);
                        serialized.write(stringLenBuffer.array());
                        serialized.write(stringBytes);
                        break;
                    case ARRAY:
                    case OBJECT:
                        byte[] jsonBytes = objectMapper.writeValueAsBytes(value);
                        ByteBuffer jsonLenBuffer = ByteBuffer.allocate(4);
                        jsonLenBuffer.putInt(jsonBytes.length);
                        serialized.write(jsonLenBuffer.array());
                        serialized.write(jsonBytes);
                        break;
                    case TIMESTAMP:
                        ByteBuffer timestampBuffer = ByteBuffer.allocate(8);
                        timestampBuffer.putLong((Long) value);
                        serialized.write(timestampBuffer.array());
                        break;
                }
            }

            // In a real implementation, we would compress the serialized data here
            // For simplicity, we're just returning the serialized data
            return serialized.toByteArray();
        }

        /**
         * Write NCF data to a file.
         */
        public void write(RandomAccessFile file) throws IOException {
            // Write placeholder for header (we'll come back and fill this in)
            long headerPos = file.getFilePointer();
            file.write(new byte[28]); // Placeholder for header

            // Write column metadata
            List<ColumnMetadataInfo> columnMetadataList = new ArrayList<>();
            long currentOffset = file.getFilePointer();

            // Write placeholder for column metadata size
            long metadataSizePos = currentOffset;
            file.write(new byte[4]); // Placeholder for metadata size
            currentOffset += 4;

            // Write column count
            ByteBuffer columnCountBuffer = ByteBuffer.allocate(4);
            columnCountBuffer.putInt(columns.size());
            file.write(columnCountBuffer.array());
            currentOffset += 4;

            // Write each column's metadata
            for (Map.Entry<String, ColumnData> entry : columns.entrySet()) {
                String colName = entry.getKey();
                ColumnData columnData = entry.getValue();

                // For now, just record the metadata; we'll fill in offsets later
                columnMetadataList.add(new ColumnMetadataInfo(colName, columnData.dataType, columnData.values));

                // Write placeholder for this column's metadata
                file.write(new byte[100]); // Generous placeholder
                currentOffset += 100;
            }

            // Remember where column data starts
            long columnDataStart = file.getFilePointer();

            // Write each column's data and update metadata
            List<ColumnMetadata> realColumnMetadata = new ArrayList<>();
            for (ColumnMetadataInfo info : columnMetadataList) {
                long columnOffset = file.getFilePointer();
                byte[] columnData = compressColumn(info.values, info.dataType);
                file.write(columnData);
                long columnLength = columnData.length;

                realColumnMetadata.add(new ColumnMetadata(
                    info.name,
                    info.dataType,
                    columnOffset,
                    columnLength,
                    true // For simplicity, all columns are nullable
                ));
            }

            // Go back and write the real header
            file.seek(headerPos);
            NCFHeader header = new NCFHeader(
                columns.size(),
                rowCount,
                System.currentTimeMillis(),
                compression
            );
            file.write(header.serialize());

            // Go back and write the real column metadata
            file.seek(metadataSizePos);

            // Calculate metadata size
            ByteArrayOutputStream metadataBuffer = new ByteArrayOutputStream();
            for (ColumnMetadata metadata : realColumnMetadata) {
                metadataBuffer.write(metadata.serialize());
            }
            byte[] metadataBytes = metadataBuffer.toByteArray();

            // Write metadata size
            ByteBuffer metadataSizeBuffer = ByteBuffer.allocate(4);
            metadataSizeBuffer.putInt(metadataBytes.length);
            file.write(metadataSizeBuffer.array());

            // Skip column count, which we already wrote
            file.skipBytes(4);

            // Write actual column metadata
            file.write(metadataBytes);

            // Return to the end of the file
            file.seek(file.length());
        }

        /**
         * Column data class.
         */
        private static class ColumnData {
            private final DataType dataType;
            private final List<Object> values;

            public ColumnData(DataType dataType, List<Object> values) {
                this.dataType = dataType;
                this.values = values;
            }
        }

        /**
         * Column metadata info class.
         */
        private static class ColumnMetadataInfo {
            private final String name;
            private final DataType dataType;
            private final List<Object> values;

            public ColumnMetadataInfo(String name, DataType dataType, List<Object> values) {
                this.name = name;
                this.dataType = dataType;
                this.values = values;
            }
        }
    }

    /**
     * Reader for NCF files.
     */
    public static class Reader implements AutoCloseable {
        private final RandomAccessFile file;
        private NCFHeader header;
        private Map<String, ColumnMetadata> columns = new HashMap<>();

        /**
         * Initialize NCF reader.
         *
         * @param file File to read from
         */
        public Reader(RandomAccessFile file) throws IOException {
            this.file = file;
            readHeader();
            readColumnMetadata();
        }

        /**
         * Read NCF header from file.
         */
        private void readHeader() throws IOException {
            byte[] headerData = new byte[28];
            file.readFully(headerData);
            header = NCFHeader.deserialize(headerData);
        }

        /**
         * Read column metadata from file.
         */
        private void readColumnMetadata() throws IOException {
            // Read metadata size
            byte[] metadataSizeBytes = new byte[4];
            file.readFully(metadataSizeBytes);
            int metadataSize = ByteBuffer.wrap(metadataSizeBytes).getInt();

            // Read column count
            byte[] columnCountBytes = new byte[4];
            file.readFully(columnCountBytes);
            int columnCount = ByteBuffer.wrap(columnCountBytes).getInt();

            // Read column metadata
            byte[] metadataData = new byte[metadataSize];
            file.readFully(metadataData);

            columns = new HashMap<>();
            int offset = 0;
            for (int i = 0; i < columnCount; i++) {
                ColumnMetadata.DeserializeResult result = ColumnMetadata.deserialize(metadataData, offset);
                ColumnMetadata columnMetadata = result.getMetadata();
                columns.put(columnMetadata.getName(), columnMetadata);
                offset = result.getNextOffset();
            }
        }

        /**
         * Decompress a column of values.
         */
        private List<Object> decompressColumn(ColumnMetadata columnMetadata) throws IOException {
            // Seek to column data
            file.seek(columnMetadata.getOffset());

            // Read compressed data
            byte[] compressedData = new byte[(int) columnMetadata.getLength()];
            file.readFully(compressedData);

            // In a real implementation, we would decompress the data here
            // For simplicity, we're just parsing the serialized data

            ByteBuffer buffer = ByteBuffer.wrap(compressedData);
            List<Object> values = new ArrayList<>(header.getRowCount());

            // Check if we have nulls
            boolean hasNulls = columnMetadata.isNullable();
            byte[] nullBitmap = null;

            if (hasNulls) {
                // Read null bitmap
                int nullBitmapSize = (header.getRowCount() + 7) / 8;
                nullBitmap = new byte[nullBitmapSize];
                buffer.get(nullBitmap);
            }

            // Read values
            for (int i = 0; i < header.getRowCount(); i++) {
                // Check if this value is null
                if (nullBitmap != null) {
                    int byteIndex = i / 8;
                    int bitIndex = i % 8;
                    boolean isNull = (nullBitmap[byteIndex] & (1 << bitIndex)) != 0;

                    if (isNull) {
                        values.add(null);
                        continue;
                    }
                }

                // Read non-null value based on data type
                Object value = null;
                switch (columnMetadata.getDataType()) {
                    case BOOLEAN:
                        value = buffer.get() != 0;
                        break;
                    case INTEGER:
                        value = buffer.getLong();
                        break;
                    case FLOAT:
                        value = buffer.getDouble();
                        break;
                    case STRING:
                        int stringLength = buffer.getInt();
                        byte[] stringBytes = new byte[stringLength];
                        buffer.get(stringBytes);
                        value = new String(stringBytes, StandardCharsets.UTF_8);
                        break;
                    case ARRAY:
                    case OBJECT:
                        int jsonLength = buffer.getInt();
                        byte[] jsonBytes = new byte[jsonLength];
                        buffer.get(jsonBytes);
                        try {
                            if (columnMetadata.getDataType() == DataType.ARRAY) {
                                value = objectMapper.readValue(jsonBytes, List.class);
                            } else {
                                value = objectMapper.readValue(jsonBytes, Map.class);
                            }
                        } catch (IOException e) {
                            logger.error("Error parsing JSON data", e);
                            value = null;
                        }
                        break;
                    case TIMESTAMP:
                        value = buffer.getLong();
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported data type: " + columnMetadata.getDataType());
                }

                values.add(value);
            }

            return values;
        }

        /**
         * Read a column of values.
         *
         * @param columnName Name of the column to read
         * @return List of values in the column
         */
        public List<Object> readColumn(String columnName) throws IOException {
            if (!columns.containsKey(columnName)) {
                throw new IllegalArgumentException("Column '" + columnName + "' does not exist");
            }

            ColumnMetadata columnMetadata = columns.get(columnName);
            return decompressColumn(columnMetadata);
        }

        /**
         * Read rows from the file.
         *
         * @param start Starting row index
         * @param count Number of rows to read, or null to read all
         * @return List of maps, each representing a row
         */
        public List<Map<String, Object>> readRows(int start, Integer count) throws IOException {
            if (count == null) {
                count = header.getRowCount() - start;
            }

            if (start < 0 || start >= header.getRowCount()) {
                throw new IllegalArgumentException("Invalid start index: " + start);
            }

            if (count < 0 || start + count > header.getRowCount()) {
                throw new IllegalArgumentException("Invalid count: " + count);
            }

            // Read all columns
            Map<String, List<Object>> columnData = new HashMap<>();
            for (String columnName : columns.keySet()) {
                columnData.put(columnName, readColumn(columnName));
            }

            // Construct rows
            List<Map<String, Object>> rows = new ArrayList<>(count);
            for (int i = start; i < start + count; i++) {
                Map<String, Object> row = new HashMap<>();
                for (String columnName : columnData.keySet()) {
                    row.put(columnName, columnData.get(columnName).get(i));
                }
                rows.add(row);
            }

            return rows;
        }

        /**
         * Get the NCF header.
         */
        public NCFHeader getHeader() {
            return header;
        }

        /**
         * Get the column metadata.
         */
        public Map<String, ColumnMetadata> getColumns() {
            return columns;
        }

        /**
         * Close the reader.
         */
        public void close() throws IOException {
            file.close();
        }
    }
}
