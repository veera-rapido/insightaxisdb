package com.tesseractdb.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Represents a condition in a query.
 */
public class QueryCondition {
    
    private final String field;
    private final Operator operator;
    private final Object value;
    
    /**
     * Operators supported in query conditions.
     */
    public enum Operator {
        EQUALS,
        NOT_EQUALS,
        GREATER_THAN,
        GREATER_THAN_OR_EQUALS,
        LESS_THAN,
        LESS_THAN_OR_EQUALS,
        CONTAINS,
        STARTS_WITH,
        ENDS_WITH,
        IN,
        NOT_IN,
        EXISTS,
        NOT_EXISTS
    }
    
    /**
     * Create a new query condition.
     *
     * @param field Field name
     * @param operator Operator
     * @param value Value to compare against
     */
    public QueryCondition(String field, Operator operator, Object value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
    }
    
    /**
     * Check if a row matches this condition.
     *
     * @param row Row to check
     * @return Whether the row matches
     */
    public boolean matches(Map<String, Object> row) {
        if (!row.containsKey(field)) {
            return operator == Operator.NOT_EXISTS;
        }
        
        if (operator == Operator.EXISTS) {
            return true;
        }
        
        Object fieldValue = row.get(field);
        
        // Handle null values
        if (fieldValue == null) {
            return value == null && operator == Operator.EQUALS;
        }
        
        // Handle comparison based on operator
        switch (operator) {
            case EQUALS:
                return fieldValue.equals(value);
                
            case NOT_EQUALS:
                return !fieldValue.equals(value);
                
            case GREATER_THAN:
                if (fieldValue instanceof Number && value instanceof Number) {
                    return ((Number) fieldValue).doubleValue() > ((Number) value).doubleValue();
                }
                if (fieldValue instanceof String && value instanceof String) {
                    return ((String) fieldValue).compareTo((String) value) > 0;
                }
                return false;
                
            case GREATER_THAN_OR_EQUALS:
                if (fieldValue instanceof Number && value instanceof Number) {
                    return ((Number) fieldValue).doubleValue() >= ((Number) value).doubleValue();
                }
                if (fieldValue instanceof String && value instanceof String) {
                    return ((String) fieldValue).compareTo((String) value) >= 0;
                }
                return false;
                
            case LESS_THAN:
                if (fieldValue instanceof Number && value instanceof Number) {
                    return ((Number) fieldValue).doubleValue() < ((Number) value).doubleValue();
                }
                if (fieldValue instanceof String && value instanceof String) {
                    return ((String) fieldValue).compareTo((String) value) < 0;
                }
                return false;
                
            case LESS_THAN_OR_EQUALS:
                if (fieldValue instanceof Number && value instanceof Number) {
                    return ((Number) fieldValue).doubleValue() <= ((Number) value).doubleValue();
                }
                if (fieldValue instanceof String && value instanceof String) {
                    return ((String) fieldValue).compareTo((String) value) <= 0;
                }
                return false;
                
            case CONTAINS:
                if (fieldValue instanceof String && value instanceof String) {
                    return ((String) fieldValue).contains((String) value);
                }
                if (fieldValue instanceof List) {
                    return ((List<?>) fieldValue).contains(value);
                }
                return false;
                
            case STARTS_WITH:
                if (fieldValue instanceof String && value instanceof String) {
                    return ((String) fieldValue).startsWith((String) value);
                }
                return false;
                
            case ENDS_WITH:
                if (fieldValue instanceof String && value instanceof String) {
                    return ((String) fieldValue).endsWith((String) value);
                }
                return false;
                
            case IN:
                if (value instanceof List) {
                    return ((List<?>) value).contains(fieldValue);
                }
                return false;
                
            case NOT_IN:
                if (value instanceof List) {
                    return !((List<?>) value).contains(fieldValue);
                }
                return false;
                
            default:
                return false;
        }
    }
    
    /**
     * Create a predicate from this condition.
     *
     * @return Predicate that checks if a row matches this condition
     */
    public Predicate<Map<String, Object>> toPredicate() {
        return this::matches;
    }
    
    // Static factory methods for creating conditions
    
    public static QueryCondition eq(String field, Object value) {
        return new QueryCondition(field, Operator.EQUALS, value);
    }
    
    public static QueryCondition ne(String field, Object value) {
        return new QueryCondition(field, Operator.NOT_EQUALS, value);
    }
    
    public static QueryCondition gt(String field, Object value) {
        return new QueryCondition(field, Operator.GREATER_THAN, value);
    }
    
    public static QueryCondition gte(String field, Object value) {
        return new QueryCondition(field, Operator.GREATER_THAN_OR_EQUALS, value);
    }
    
    public static QueryCondition lt(String field, Object value) {
        return new QueryCondition(field, Operator.LESS_THAN, value);
    }
    
    public static QueryCondition lte(String field, Object value) {
        return new QueryCondition(field, Operator.LESS_THAN_OR_EQUALS, value);
    }
    
    public static QueryCondition contains(String field, String value) {
        return new QueryCondition(field, Operator.CONTAINS, value);
    }
    
    public static QueryCondition startsWith(String field, String value) {
        return new QueryCondition(field, Operator.STARTS_WITH, value);
    }
    
    public static QueryCondition endsWith(String field, String value) {
        return new QueryCondition(field, Operator.ENDS_WITH, value);
    }
    
    public static QueryCondition in(String field, List<?> values) {
        return new QueryCondition(field, Operator.IN, values);
    }
    
    public static QueryCondition notIn(String field, List<?> values) {
        return new QueryCondition(field, Operator.NOT_IN, values);
    }
    
    public static QueryCondition exists(String field) {
        return new QueryCondition(field, Operator.EXISTS, null);
    }
    
    public static QueryCondition notExists(String field) {
        return new QueryCondition(field, Operator.NOT_EXISTS, null);
    }
    
    // Getters
    
    public String getField() {
        return field;
    }
    
    public Operator getOperator() {
        return operator;
    }
    
    public Object getValue() {
        return value;
    }
}
