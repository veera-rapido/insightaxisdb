package com.insightaxisdb.query;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test cases for the QueryCondition class.
 */
public class QueryConditionTest {
    
    @Test
    public void testEqualsCondition() {
        // Create a condition
        QueryCondition condition = QueryCondition.eq("name", "John");
        
        // Create a row that matches
        Map<String, Object> matchingRow = new HashMap<>();
        matchingRow.put("name", "John");
        matchingRow.put("age", 30);
        
        // Create a row that doesn't match
        Map<String, Object> nonMatchingRow = new HashMap<>();
        nonMatchingRow.put("name", "Jane");
        nonMatchingRow.put("age", 25);
        
        // Test matching
        assertTrue(condition.matches(matchingRow));
        assertFalse(condition.matches(nonMatchingRow));
    }
    
    @Test
    public void testNotEqualsCondition() {
        // Create a condition
        QueryCondition condition = QueryCondition.ne("name", "John");
        
        // Create a row that matches
        Map<String, Object> matchingRow = new HashMap<>();
        matchingRow.put("name", "Jane");
        matchingRow.put("age", 25);
        
        // Create a row that doesn't match
        Map<String, Object> nonMatchingRow = new HashMap<>();
        nonMatchingRow.put("name", "John");
        nonMatchingRow.put("age", 30);
        
        // Test matching
        assertTrue(condition.matches(matchingRow));
        assertFalse(condition.matches(nonMatchingRow));
    }
    
    @Test
    public void testGreaterThanCondition() {
        // Create a condition
        QueryCondition condition = QueryCondition.gt("age", 25);
        
        // Create a row that matches
        Map<String, Object> matchingRow = new HashMap<>();
        matchingRow.put("name", "John");
        matchingRow.put("age", 30);
        
        // Create a row that doesn't match
        Map<String, Object> nonMatchingRow = new HashMap<>();
        nonMatchingRow.put("name", "Jane");
        nonMatchingRow.put("age", 25);
        
        // Test matching
        assertTrue(condition.matches(matchingRow));
        assertFalse(condition.matches(nonMatchingRow));
    }
    
    @Test
    public void testGreaterThanOrEqualsCondition() {
        // Create a condition
        QueryCondition condition = QueryCondition.gte("age", 25);
        
        // Create rows
        Map<String, Object> greaterRow = new HashMap<>();
        greaterRow.put("age", 30);
        
        Map<String, Object> equalRow = new HashMap<>();
        equalRow.put("age", 25);
        
        Map<String, Object> lesserRow = new HashMap<>();
        lesserRow.put("age", 20);
        
        // Test matching
        assertTrue(condition.matches(greaterRow));
        assertTrue(condition.matches(equalRow));
        assertFalse(condition.matches(lesserRow));
    }
    
    @Test
    public void testLessThanCondition() {
        // Create a condition
        QueryCondition condition = QueryCondition.lt("age", 25);
        
        // Create a row that matches
        Map<String, Object> matchingRow = new HashMap<>();
        matchingRow.put("name", "Jane");
        matchingRow.put("age", 20);
        
        // Create a row that doesn't match
        Map<String, Object> nonMatchingRow = new HashMap<>();
        nonMatchingRow.put("name", "John");
        nonMatchingRow.put("age", 25);
        
        // Test matching
        assertTrue(condition.matches(matchingRow));
        assertFalse(condition.matches(nonMatchingRow));
    }
    
    @Test
    public void testLessThanOrEqualsCondition() {
        // Create a condition
        QueryCondition condition = QueryCondition.lte("age", 25);
        
        // Create rows
        Map<String, Object> lesserRow = new HashMap<>();
        lesserRow.put("age", 20);
        
        Map<String, Object> equalRow = new HashMap<>();
        equalRow.put("age", 25);
        
        Map<String, Object> greaterRow = new HashMap<>();
        greaterRow.put("age", 30);
        
        // Test matching
        assertTrue(condition.matches(lesserRow));
        assertTrue(condition.matches(equalRow));
        assertFalse(condition.matches(greaterRow));
    }
    
    @Test
    public void testContainsCondition() {
        // Create a condition
        QueryCondition condition = QueryCondition.contains("name", "oh");
        
        // Create a row that matches
        Map<String, Object> matchingRow = new HashMap<>();
        matchingRow.put("name", "John");
        
        // Create a row that doesn't match
        Map<String, Object> nonMatchingRow = new HashMap<>();
        nonMatchingRow.put("name", "Jane");
        
        // Test matching
        assertTrue(condition.matches(matchingRow));
        assertFalse(condition.matches(nonMatchingRow));
    }
    
    @Test
    public void testStartsWithCondition() {
        // Create a condition
        QueryCondition condition = QueryCondition.startsWith("name", "Jo");
        
        // Create a row that matches
        Map<String, Object> matchingRow = new HashMap<>();
        matchingRow.put("name", "John");
        
        // Create a row that doesn't match
        Map<String, Object> nonMatchingRow = new HashMap<>();
        nonMatchingRow.put("name", "Jane");
        
        // Test matching
        assertTrue(condition.matches(matchingRow));
        assertFalse(condition.matches(nonMatchingRow));
    }
    
    @Test
    public void testEndsWithCondition() {
        // Create a condition
        QueryCondition condition = QueryCondition.endsWith("name", "hn");
        
        // Create a row that matches
        Map<String, Object> matchingRow = new HashMap<>();
        matchingRow.put("name", "John");
        
        // Create a row that doesn't match
        Map<String, Object> nonMatchingRow = new HashMap<>();
        nonMatchingRow.put("name", "Jane");
        
        // Test matching
        assertTrue(condition.matches(matchingRow));
        assertFalse(condition.matches(nonMatchingRow));
    }
    
    @Test
    public void testInCondition() {
        // Create a condition
        QueryCondition condition = QueryCondition.in("name", Arrays.asList("John", "Jane"));
        
        // Create rows
        Map<String, Object> matchingRow1 = new HashMap<>();
        matchingRow1.put("name", "John");
        
        Map<String, Object> matchingRow2 = new HashMap<>();
        matchingRow2.put("name", "Jane");
        
        Map<String, Object> nonMatchingRow = new HashMap<>();
        nonMatchingRow.put("name", "Bob");
        
        // Test matching
        assertTrue(condition.matches(matchingRow1));
        assertTrue(condition.matches(matchingRow2));
        assertFalse(condition.matches(nonMatchingRow));
    }
    
    @Test
    public void testNotInCondition() {
        // Create a condition
        QueryCondition condition = QueryCondition.notIn("name", Arrays.asList("John", "Jane"));
        
        // Create rows
        Map<String, Object> nonMatchingRow1 = new HashMap<>();
        nonMatchingRow1.put("name", "John");
        
        Map<String, Object> nonMatchingRow2 = new HashMap<>();
        nonMatchingRow2.put("name", "Jane");
        
        Map<String, Object> matchingRow = new HashMap<>();
        matchingRow.put("name", "Bob");
        
        // Test matching
        assertFalse(condition.matches(nonMatchingRow1));
        assertFalse(condition.matches(nonMatchingRow2));
        assertTrue(condition.matches(matchingRow));
    }
    
    @Test
    public void testExistsCondition() {
        // Create a condition
        QueryCondition condition = QueryCondition.exists("name");
        
        // Create rows
        Map<String, Object> matchingRow = new HashMap<>();
        matchingRow.put("name", "John");
        
        Map<String, Object> nonMatchingRow = new HashMap<>();
        nonMatchingRow.put("age", 30);
        
        // Test matching
        assertTrue(condition.matches(matchingRow));
        assertFalse(condition.matches(nonMatchingRow));
    }
    
    @Test
    public void testNotExistsCondition() {
        // Create a condition
        QueryCondition condition = QueryCondition.notExists("name");
        
        // Create rows
        Map<String, Object> nonMatchingRow = new HashMap<>();
        nonMatchingRow.put("name", "John");
        
        Map<String, Object> matchingRow = new HashMap<>();
        matchingRow.put("age", 30);
        
        // Test matching
        assertFalse(condition.matches(nonMatchingRow));
        assertTrue(condition.matches(matchingRow));
    }
    
    @Test
    public void testNullValue() {
        // Create conditions
        QueryCondition eqCondition = QueryCondition.eq("name", null);
        QueryCondition neCondition = QueryCondition.ne("name", null);
        
        // Create rows
        Map<String, Object> nullRow = new HashMap<>();
        nullRow.put("name", null);
        
        Map<String, Object> nonNullRow = new HashMap<>();
        nonNullRow.put("name", "John");
        
        // Test matching
        assertTrue(eqCondition.matches(nullRow));
        assertFalse(eqCondition.matches(nonNullRow));
        
        assertFalse(neCondition.matches(nullRow));
        assertTrue(neCondition.matches(nonNullRow));
    }
    
    @Test
    public void testToPredicate() {
        // Create a condition
        QueryCondition condition = QueryCondition.eq("name", "John");
        
        // Convert to predicate
        java.util.function.Predicate<Map<String, Object>> predicate = condition.toPredicate();
        
        // Create rows
        Map<String, Object> matchingRow = new HashMap<>();
        matchingRow.put("name", "John");
        
        Map<String, Object> nonMatchingRow = new HashMap<>();
        nonMatchingRow.put("name", "Jane");
        
        // Test predicate
        assertTrue(predicate.test(matchingRow));
        assertFalse(predicate.test(nonMatchingRow));
    }
}
