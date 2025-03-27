package com.tesseractdb.query;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Test cases for the Query class.
 */
public class QueryTest {
    
    @Test
    public void testBasicQuery() {
        // Create a query
        Query query = new Query()
                .where(QueryCondition.eq("name", "John"))
                .select("name", "age")
                .orderBy("age", Query.SortOrder.ASCENDING)
                .limit(10)
                .offset(0);
        
        // Check query properties
        assertEquals(1, query.getConditions().size());
        assertEquals(2, query.getSelectFields().size());
        assertEquals(1, query.getSortFields().size());
        assertEquals(Integer.valueOf(10), query.getLimit());
        assertEquals(Integer.valueOf(0), query.getOffset());
    }
    
    @Test
    public void testQueryWithMultipleConditions() {
        // Create a query with multiple conditions
        Query query = new Query()
                .where(Arrays.asList(
                        QueryCondition.eq("name", "John"),
                        QueryCondition.gt("age", 25)
                ));
        
        // Check query properties
        assertEquals(2, query.getConditions().size());
    }
    
    @Test
    public void testQueryExecution() {
        // Create a query
        Query query = new Query()
                .where(QueryCondition.eq("name", "John"))
                .select("name", "age");
        
        // Create some test data
        List<Map<String, Object>> rows = new ArrayList<>();
        
        Map<String, Object> row1 = new HashMap<>();
        row1.put("name", "John");
        row1.put("age", 30);
        row1.put("email", "john@example.com");
        rows.add(row1);
        
        Map<String, Object> row2 = new HashMap<>();
        row2.put("name", "Jane");
        row2.put("age", 25);
        row2.put("email", "jane@example.com");
        rows.add(row2);
        
        // Execute query
        QueryResult result = query.execute(rows);
        
        // Check result
        assertNotNull(result);
        assertEquals(1, result.getRowCount());
        assertFalse(result.isEmpty());
        
        Map<String, Object> resultRow = result.getRows().get(0);
        assertEquals(2, resultRow.size());
        assertEquals("John", resultRow.get("name"));
        assertEquals(30, resultRow.get("age"));
        assertFalse(resultRow.containsKey("email"));
    }
    
    @Test
    public void testQueryWithSorting() {
        // Create a query with sorting
        Query query = new Query()
                .orderBy("age", Query.SortOrder.DESCENDING);
        
        // Create some test data
        List<Map<String, Object>> rows = new ArrayList<>();
        
        Map<String, Object> row1 = new HashMap<>();
        row1.put("name", "John");
        row1.put("age", 30);
        rows.add(row1);
        
        Map<String, Object> row2 = new HashMap<>();
        row2.put("name", "Jane");
        row2.put("age", 25);
        rows.add(row2);
        
        Map<String, Object> row3 = new HashMap<>();
        row3.put("name", "Bob");
        row3.put("age", 35);
        rows.add(row3);
        
        // Execute query
        QueryResult result = query.execute(rows);
        
        // Check result
        assertEquals(3, result.getRowCount());
        assertEquals(35, result.getRows().get(0).get("age"));
        assertEquals(30, result.getRows().get(1).get("age"));
        assertEquals(25, result.getRows().get(2).get("age"));
    }
    
    @Test
    public void testQueryWithMultipleSortFields() {
        // Create a query with multiple sort fields
        Query query = new Query()
                .orderBy("age", Query.SortOrder.ASCENDING)
                .orderBy("name", Query.SortOrder.DESCENDING);
        
        // Create some test data
        List<Map<String, Object>> rows = new ArrayList<>();
        
        Map<String, Object> row1 = new HashMap<>();
        row1.put("name", "John");
        row1.put("age", 30);
        rows.add(row1);
        
        Map<String, Object> row2 = new HashMap<>();
        row2.put("name", "Jane");
        row2.put("age", 30);
        rows.add(row2);
        
        Map<String, Object> row3 = new HashMap<>();
        row3.put("name", "Bob");
        row3.put("age", 25);
        rows.add(row3);
        
        // Execute query
        QueryResult result = query.execute(rows);
        
        // Check result
        assertEquals(3, result.getRowCount());
        assertEquals(25, result.getRows().get(0).get("age"));
        assertEquals("Bob", result.getRows().get(0).get("name"));
        assertEquals(30, result.getRows().get(1).get("age"));
        assertEquals("John", result.getRows().get(1).get("name"));
        assertEquals(30, result.getRows().get(2).get("age"));
        assertEquals("Jane", result.getRows().get(2).get("name"));
    }
    
    @Test
    public void testQueryWithLimitAndOffset() {
        // Create a query with limit and offset
        Query query = new Query()
                .orderBy("age", Query.SortOrder.ASCENDING)
                .limit(2)
                .offset(1);
        
        // Create some test data
        List<Map<String, Object>> rows = new ArrayList<>();
        
        for (int i = 0; i < 5; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("name", "User " + i);
            row.put("age", 20 + i);
            rows.add(row);
        }
        
        // Execute query
        QueryResult result = query.execute(rows);
        
        // Check result
        assertEquals(2, result.getRowCount());
        assertEquals(21, result.getRows().get(0).get("age"));
        assertEquals(22, result.getRows().get(1).get("age"));
    }
    
    @Test
    public void testQueryWithAggregations() {
        // Create a query with aggregations
        Query query = new Query()
                .aggregate("age", Query.AggregationType.SUM, "total_age")
                .aggregate("age", Query.AggregationType.AVG, "avg_age")
                .aggregate("age", Query.AggregationType.MIN, "min_age")
                .aggregate("age", Query.AggregationType.MAX, "max_age")
                .aggregate("name", Query.AggregationType.COUNT, "count")
                .aggregate("name", Query.AggregationType.COUNT_DISTINCT, "distinct_count");
        
        // Create some test data
        List<Map<String, Object>> rows = new ArrayList<>();
        
        Map<String, Object> row1 = new HashMap<>();
        row1.put("name", "John");
        row1.put("age", 30);
        rows.add(row1);
        
        Map<String, Object> row2 = new HashMap<>();
        row2.put("name", "Jane");
        row2.put("age", 25);
        rows.add(row2);
        
        Map<String, Object> row3 = new HashMap<>();
        row3.put("name", "John");
        row3.put("age", 35);
        rows.add(row3);
        
        // Execute query
        QueryResult result = query.execute(rows);
        
        // Check aggregation results
        Map<String, Object> aggregations = result.getAggregations();
        assertEquals(90.0, aggregations.get("total_age"));
        assertEquals(30.0, aggregations.get("avg_age"));
        assertEquals(25.0, aggregations.get("min_age"));
        assertEquals(35.0, aggregations.get("max_age"));
        assertEquals(3L, aggregations.get("count"));
        assertEquals(2L, aggregations.get("distinct_count"));
    }
    
    @Test
    public void testEmptyQueryResult() {
        // Create a query that matches nothing
        Query query = new Query()
                .where(QueryCondition.eq("name", "NonExistent"));
        
        // Create some test data
        List<Map<String, Object>> rows = new ArrayList<>();
        
        Map<String, Object> row = new HashMap<>();
        row.put("name", "John");
        row.put("age", 30);
        rows.add(row);
        
        // Execute query
        QueryResult result = query.execute(rows);
        
        // Check result
        assertTrue(result.isEmpty());
        assertEquals(0, result.getRowCount());
    }
}
