package com.tesseractdb.query;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Represents a query for retrieving and filtering data.
 */
public class Query {
    
    private final List<QueryCondition> conditions = new ArrayList<>();
    private final List<String> selectFields = new ArrayList<>();
    private final Map<String, SortOrder> sortFields = new LinkedHashMap<>();
    private Integer limit;
    private Integer offset;
    private final List<Aggregation> aggregations = new ArrayList<>();
    
    /**
     * Sort order for query results.
     */
    public enum SortOrder {
        ASCENDING,
        DESCENDING
    }
    
    /**
     * Aggregation types for query results.
     */
    public enum AggregationType {
        COUNT,
        SUM,
        AVG,
        MIN,
        MAX,
        COUNT_DISTINCT
    }
    
    /**
     * Represents an aggregation in a query.
     */
    public static class Aggregation {
        private final String field;
        private final AggregationType type;
        private final String alias;
        
        public Aggregation(String field, AggregationType type, String alias) {
            this.field = field;
            this.type = type;
            this.alias = alias;
        }
        
        public String getField() {
            return field;
        }
        
        public AggregationType getType() {
            return type;
        }
        
        public String getAlias() {
            return alias;
        }
    }
    
    /**
     * Create a new query.
     */
    public Query() {
    }
    
    /**
     * Add a condition to the query.
     *
     * @param condition Condition to add
     * @return This query for chaining
     */
    public Query where(QueryCondition condition) {
        conditions.add(condition);
        return this;
    }
    
    /**
     * Add multiple conditions to the query.
     *
     * @param conditions Conditions to add
     * @return This query for chaining
     */
    public Query where(List<QueryCondition> conditions) {
        this.conditions.addAll(conditions);
        return this;
    }
    
    /**
     * Specify fields to select.
     *
     * @param fields Fields to select
     * @return This query for chaining
     */
    public Query select(String... fields) {
        selectFields.addAll(Arrays.asList(fields));
        return this;
    }
    
    /**
     * Add a sort field to the query.
     *
     * @param field Field to sort by
     * @param order Sort order
     * @return This query for chaining
     */
    public Query orderBy(String field, SortOrder order) {
        sortFields.put(field, order);
        return this;
    }
    
    /**
     * Set the maximum number of results to return.
     *
     * @param limit Maximum number of results
     * @return This query for chaining
     */
    public Query limit(int limit) {
        this.limit = limit;
        return this;
    }
    
    /**
     * Set the number of results to skip.
     *
     * @param offset Number of results to skip
     * @return This query for chaining
     */
    public Query offset(int offset) {
        this.offset = offset;
        return this;
    }
    
    /**
     * Add an aggregation to the query.
     *
     * @param field Field to aggregate
     * @param type Aggregation type
     * @param alias Alias for the aggregation result
     * @return This query for chaining
     */
    public Query aggregate(String field, AggregationType type, String alias) {
        aggregations.add(new Aggregation(field, type, alias));
        return this;
    }
    
    /**
     * Execute the query on a list of rows.
     *
     * @param rows Rows to query
     * @return Query results
     */
    public QueryResult execute(List<Map<String, Object>> rows) {
        // Filter rows based on conditions
        Predicate<Map<String, Object>> predicate = row -> {
            for (QueryCondition condition : conditions) {
                if (!condition.matches(row)) {
                    return false;
                }
            }
            return true;
        };
        
        List<Map<String, Object>> filteredRows = rows.stream()
                .filter(predicate)
                .collect(Collectors.toList());
        
        // Sort rows
        if (!sortFields.isEmpty()) {
            filteredRows.sort((row1, row2) -> {
                for (Map.Entry<String, SortOrder> entry : sortFields.entrySet()) {
                    String field = entry.getKey();
                    SortOrder order = entry.getValue();
                    
                    Object value1 = row1.get(field);
                    Object value2 = row2.get(field);
                    
                    // Handle null values
                    if (value1 == null && value2 == null) {
                        continue;
                    }
                    if (value1 == null) {
                        return order == SortOrder.ASCENDING ? -1 : 1;
                    }
                    if (value2 == null) {
                        return order == SortOrder.ASCENDING ? 1 : -1;
                    }
                    
                    // Compare values
                    int comparison;
                    if (value1 instanceof Comparable && value1.getClass().equals(value2.getClass())) {
                        @SuppressWarnings("unchecked")
                        Comparable<Object> comparable1 = (Comparable<Object>) value1;
                        comparison = comparable1.compareTo(value2);
                    } else {
                        // Fall back to string comparison
                        comparison = value1.toString().compareTo(value2.toString());
                    }
                    
                    if (comparison != 0) {
                        return order == SortOrder.ASCENDING ? comparison : -comparison;
                    }
                }
                
                return 0;
            });
        }
        
        // Apply offset and limit
        int startIndex = offset != null ? offset : 0;
        int endIndex = limit != null ? Math.min(startIndex + limit, filteredRows.size()) : filteredRows.size();
        
        if (startIndex >= filteredRows.size()) {
            filteredRows = Collections.emptyList();
        } else {
            filteredRows = filteredRows.subList(startIndex, endIndex);
        }
        
        // Select fields
        List<Map<String, Object>> resultRows;
        if (!selectFields.isEmpty()) {
            resultRows = filteredRows.stream()
                    .map(row -> {
                        Map<String, Object> resultRow = new HashMap<>();
                        for (String field : selectFields) {
                            if (row.containsKey(field)) {
                                resultRow.put(field, row.get(field));
                            }
                        }
                        return resultRow;
                    })
                    .collect(Collectors.toList());
        } else {
            resultRows = filteredRows;
        }
        
        // Calculate aggregations
        Map<String, Object> aggregationResults = new HashMap<>();
        if (!aggregations.isEmpty()) {
            for (Aggregation aggregation : aggregations) {
                String field = aggregation.getField();
                AggregationType type = aggregation.getType();
                String alias = aggregation.getAlias();
                
                switch (type) {
                    case COUNT:
                        long count = filteredRows.size();
                        aggregationResults.put(alias, count);
                        break;
                        
                    case COUNT_DISTINCT:
                        long distinctCount = filteredRows.stream()
                                .map(row -> row.get(field))
                                .filter(Objects::nonNull)
                                .distinct()
                                .count();
                        aggregationResults.put(alias, distinctCount);
                        break;
                        
                    case SUM:
                        double sum = filteredRows.stream()
                                .map(row -> row.get(field))
                                .filter(value -> value instanceof Number)
                                .mapToDouble(value -> ((Number) value).doubleValue())
                                .sum();
                        aggregationResults.put(alias, sum);
                        break;
                        
                    case AVG:
                        OptionalDouble avg = filteredRows.stream()
                                .map(row -> row.get(field))
                                .filter(value -> value instanceof Number)
                                .mapToDouble(value -> ((Number) value).doubleValue())
                                .average();
                        aggregationResults.put(alias, avg.orElse(0.0));
                        break;
                        
                    case MIN:
                        OptionalDouble min = filteredRows.stream()
                                .map(row -> row.get(field))
                                .filter(value -> value instanceof Number)
                                .mapToDouble(value -> ((Number) value).doubleValue())
                                .min();
                        aggregationResults.put(alias, min.orElse(0.0));
                        break;
                        
                    case MAX:
                        OptionalDouble max = filteredRows.stream()
                                .map(row -> row.get(field))
                                .filter(value -> value instanceof Number)
                                .mapToDouble(value -> ((Number) value).doubleValue())
                                .max();
                        aggregationResults.put(alias, max.orElse(0.0));
                        break;
                }
            }
        }
        
        return new QueryResult(resultRows, aggregationResults);
    }
    
    // Getters
    
    public List<QueryCondition> getConditions() {
        return conditions;
    }
    
    public List<String> getSelectFields() {
        return selectFields;
    }
    
    public Map<String, SortOrder> getSortFields() {
        return sortFields;
    }
    
    public Integer getLimit() {
        return limit;
    }
    
    public Integer getOffset() {
        return offset;
    }
    
    public List<Aggregation> getAggregations() {
        return aggregations;
    }
}
