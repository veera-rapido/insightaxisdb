package com.insightaxisdb.storage;

/**
 * Enum for supported data types in NCF.
 */
public enum DataType {
    NULL(0),
    BOOLEAN(1),
    INTEGER(2),
    FLOAT(3),
    STRING(4),
    ARRAY(5),
    OBJECT(6),
    TIMESTAMP(7);
    
    private final int value;
    
    DataType(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
    
    public static DataType fromValue(int value) {
        for (DataType type : DataType.values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown data type value: " + value);
    }
}
