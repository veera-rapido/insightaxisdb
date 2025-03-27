package com.insightaxisdb.query;

import java.util.List;
import java.util.Map;

/**
 * Represents the result of a query.
 */
public class QueryResult {
    
    private final List<Map<String, Object>> rows;
    private final Map<String, Object> aggregations;
    
    /**
     * Create a new query result.
     *
     * @param rows Result rows
     * @param aggregations Aggregation results
     */
    public QueryResult(List<Map<String, Object>> rows, Map<String, Object> aggregations) {
        this.rows = rows;
        this.aggregations = aggregations;
    }
    
    /**
     * Get the result rows.
     *
     * @return Result rows
     */
    public List<Map<String, Object>> getRows() {
        return rows;
    }
    
    /**
     * Get the aggregation results.
     *
     * @return Aggregation results
     */
    public Map<String, Object> getAggregations() {
        return aggregations;
    }
    
    /**
     * Get the number of rows in the result.
     *
     * @return Number of rows
     */
    public int getRowCount() {
        return rows.size();
    }
    
    /**
     * Check if the result is empty.
     *
     * @return Whether the result is empty
     */
    public boolean isEmpty() {
        return rows.isEmpty();
    }
}
