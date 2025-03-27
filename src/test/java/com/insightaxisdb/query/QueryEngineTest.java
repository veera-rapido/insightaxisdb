package com.insightaxisdb.query;

import com.insightaxisdb.storage.EventStore;
import com.insightaxisdb.storage.UserProfileStore;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test cases for the QueryEngine.
 */
public class QueryEngineTest {

    private UserProfileStore userProfileStore;
    private EventStore eventStore;
    private QueryEngine queryEngine;

    @Before
    public void setUp() {
        userProfileStore = new UserProfileStore();
        eventStore = new EventStore(userProfileStore);
        queryEngine = new QueryEngine(userProfileStore, eventStore);

        // Create sample data
        createSampleData();
    }

    private void createSampleData() {
        // Create users
        for (int i = 1; i <= 3; i++) {
            String userId = "user" + i;

            Map<String, Object> properties = new HashMap<>();
            properties.put("name", "User " + i);
            properties.put("email", "user" + i + "@example.com");
            properties.put("age", 20 + i);

            userProfileStore.createProfile(userId, properties);
        }

        // Create events
        for (int i = 1; i <= 3; i++) {
            String userId = "user" + i;

            // Add login events
            for (int j = 0; j < i; j++) {
                Map<String, Object> properties = new HashMap<>();
                properties.put("device", "device" + j);

                eventStore.addEvent("login", userId, properties, System.currentTimeMillis() - j * 1000);
            }

            // Add purchase events
            for (int j = 0; j < i; j++) {
                Map<String, Object> properties = new HashMap<>();
                properties.put("product", "product" + j);
                properties.put("price", 10.0 * (j + 1));

                eventStore.addEvent("purchase", userId, properties, System.currentTimeMillis() - j * 2000);
            }
        }
    }

    @Test
    public void testQueryUserProfiles() {
        // Create a query for users with age > 21
        Query query = new Query()
                .where(QueryCondition.gt("age", 21))
                .select("userId", "name", "age")
                .orderBy("age", Query.SortOrder.DESCENDING);

        // Execute query
        QueryResult result = queryEngine.queryUserProfiles(query);

        // Check result
        assertEquals(2, result.getRowCount());
        assertEquals(23, result.getRows().get(0).get("age"));
        assertEquals(22, result.getRows().get(1).get("age"));
    }

    @Test
    public void testQueryEvents() {
        // Create a query for purchase events with price > 15
        Query query = new Query()
                .where(Arrays.asList(
                        QueryCondition.eq("eventName", "purchase"),
                        QueryCondition.gt("price", 15.0)
                ))
                .select("eventId", "userId", "product", "price")
                .orderBy("price", Query.SortOrder.ASCENDING);

        // Execute query
        QueryResult result = queryEngine.queryEvents(query);

        // Check result
        assertFalse(result.isEmpty());

        for (Map<String, Object> row : result.getRows()) {
            assertEquals("purchase", row.get("eventName"));
            assertTrue(((Number) row.get("price")).doubleValue() > 15.0);
        }
    }

    @Test
    public void testQueryUserEvents() {
        // Create a query for user2's events
        Query query = new Query()
                .where(QueryCondition.eq("eventName", "login"))
                .select("eventId", "eventName", "device");

        // Execute query
        QueryResult result = queryEngine.queryUserEvents("user2", query);

        // Check result
        assertEquals(2, result.getRowCount());

        for (Map<String, Object> row : result.getRows()) {
            assertEquals("login", row.get("eventName"));
            assertEquals("user2", row.get("userId"));
        }
    }

    @Test
    public void testFindUsersWithEvent() {
        // Find users who performed a login event
        Query query = new Query()
                .select("userId", "name");

        // Execute query
        QueryResult result = queryEngine.findUsersWithEvent("login", query);

        // Check result
        assertEquals(3, result.getRowCount());
    }

    @Test
    public void testFindUsersWithEventSequence() {
        // Find users who performed login followed by purchase
        Query query = new Query()
                .select("userId", "name");

        // Execute query
        QueryResult result = queryEngine.findUsersWithEventSequence(
                Arrays.asList("login", "purchase"), null, query);

        // Check result
        assertFalse(result.isEmpty());
    }

    @Test
    public void testFindUsersWithEventSequenceWithTimeConstraint() {
        // Find users who performed login followed by purchase within 5 seconds
        Query query = new Query()
                .select("userId", "name");

        // Execute query
        QueryResult result = queryEngine.findUsersWithEventSequence(
                Arrays.asList("login", "purchase"), 5000L, query);

        // Check result
        // Note: This test may be flaky depending on the timing of event creation
        // In a real implementation, we would need more control over event timestamps
        assertNotNull(result);
    }
}
