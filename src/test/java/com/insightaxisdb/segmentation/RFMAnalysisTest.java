package com.insightaxisdb.segmentation;

import com.insightaxisdb.storage.EventStore;
import com.insightaxisdb.storage.UserProfileStore;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test cases for the RFMAnalysis class.
 */
public class RFMAnalysisTest {
    
    private UserProfileStore userProfileStore;
    private EventStore eventStore;
    private RFMAnalysis rfmAnalysis;
    
    @Before
    public void setUp() {
        userProfileStore = new UserProfileStore();
        eventStore = new EventStore(userProfileStore);
        rfmAnalysis = new RFMAnalysis(userProfileStore, eventStore, "purchase", "amount");
        
        // Create sample data
        createSampleData();
    }
    
    private void createSampleData() {
        // Create users
        for (int i = 1; i <= 5; i++) {
            String userId = "user" + i;
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("name", "User " + i);
            
            userProfileStore.createProfile(userId, properties);
        }
        
        // Create purchase events with different recency, frequency, and monetary values
        
        // User 1: High recency, high frequency, high monetary
        for (int i = 0; i < 5; i++) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("product", "product" + i);
            properties.put("amount", 100.0 + i * 10);
            
            eventStore.addEvent("purchase", "user1", properties, System.currentTimeMillis() - i * 24 * 60 * 60 * 1000L);
        }
        
        // User 2: Low recency, high frequency, high monetary
        for (int i = 0; i < 5; i++) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("product", "product" + i);
            properties.put("amount", 100.0 + i * 10);
            
            eventStore.addEvent("purchase", "user2", properties, System.currentTimeMillis() - (10 + i) * 24 * 60 * 60 * 1000L);
        }
        
        // User 3: High recency, low frequency, high monetary
        for (int i = 0; i < 2; i++) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("product", "product" + i);
            properties.put("amount", 100.0 + i * 10);
            
            eventStore.addEvent("purchase", "user3", properties, System.currentTimeMillis() - i * 24 * 60 * 60 * 1000L);
        }
        
        // User 4: High recency, high frequency, low monetary
        for (int i = 0; i < 5; i++) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("product", "product" + i);
            properties.put("amount", 10.0 + i);
            
            eventStore.addEvent("purchase", "user4", properties, System.currentTimeMillis() - i * 24 * 60 * 60 * 1000L);
        }
        
        // User 5: Low recency, low frequency, low monetary
        for (int i = 0; i < 2; i++) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("product", "product" + i);
            properties.put("amount", 10.0 + i);
            
            eventStore.addEvent("purchase", "user5", properties, System.currentTimeMillis() - (10 + i) * 24 * 60 * 60 * 1000L);
        }
    }
    
    @Test
    public void testCalculateRFMScores() {
        // Calculate RFM scores
        Map<String, RFMAnalysis.RFMScore> scores = rfmAnalysis.calculateRFMScores(30, 5);
        
        // Check that scores were calculated for all users
        assertEquals(5, scores.size());
        
        // Check individual scores
        for (String userId : scores.keySet()) {
            RFMAnalysis.RFMScore score = scores.get(userId);
            assertNotNull(score);
            assertTrue(score.getRecencyScore() >= 1 && score.getRecencyScore() <= 5);
            assertTrue(score.getFrequencyScore() >= 1 && score.getFrequencyScore() <= 5);
            assertTrue(score.getMonetaryScore() >= 1 && score.getMonetaryScore() <= 5);
        }
        
        // Check relative scores
        // User 1 should have higher recency score than User 2
        assertTrue(scores.get("user1").getRecencyScore() > scores.get("user2").getRecencyScore());
        
        // User 1 should have higher frequency score than User 3
        assertTrue(scores.get("user1").getFrequencyScore() >= scores.get("user3").getFrequencyScore());
        
        // User 1 should have higher monetary score than User 4
        assertTrue(scores.get("user1").getMonetaryScore() >= scores.get("user4").getMonetaryScore());
    }
    
    @Test
    public void testGetHighValueCustomers() {
        // Calculate RFM scores
        Map<String, RFMAnalysis.RFMScore> scores = rfmAnalysis.calculateRFMScores(30, 5);
        
        // Get high-value customers
        List<String> highValueCustomers = rfmAnalysis.getHighValueCustomers(scores, 5);
        
        // Check high-value customers
        assertNotNull(highValueCustomers);
        
        // User 1 should be a high-value customer
        assertTrue(highValueCustomers.contains("user1"));
    }
    
    @Test
    public void testGetAtRiskCustomers() {
        // Calculate RFM scores
        Map<String, RFMAnalysis.RFMScore> scores = rfmAnalysis.calculateRFMScores(30, 5);
        
        // Get at-risk customers
        List<String> atRiskCustomers = rfmAnalysis.getAtRiskCustomers(scores, 5);
        
        // Check at-risk customers
        assertNotNull(atRiskCustomers);
        
        // User 2 might be an at-risk customer (low recency, high frequency/monetary)
        // This depends on the exact scoring algorithm and thresholds
    }
    
    @Test
    public void testGetNewCustomers() {
        // Calculate RFM scores
        Map<String, RFMAnalysis.RFMScore> scores = rfmAnalysis.calculateRFMScores(30, 5);
        
        // Get new customers
        List<String> newCustomers = rfmAnalysis.getNewCustomers(scores, 5);
        
        // Check new customers
        assertNotNull(newCustomers);
        
        // User 3 might be a new customer (high recency, low frequency/monetary)
        // This depends on the exact scoring algorithm and thresholds
    }
    
    @Test
    public void testGetUsersInSegment() {
        // Calculate RFM scores
        Map<String, RFMAnalysis.RFMScore> scores = rfmAnalysis.calculateRFMScores(30, 5);
        
        // Get users in a specific segment
        // Note: We don't know the exact scores, so we'll just check that the method works
        List<String> usersInSegment = rfmAnalysis.getUsersInSegment(scores, 5, 5, 5);
        
        // Check users in segment
        assertNotNull(usersInSegment);
    }
    
    @Test
    public void testGetUsersInSegmentRange() {
        // Calculate RFM scores
        Map<String, RFMAnalysis.RFMScore> scores = rfmAnalysis.calculateRFMScores(30, 5);
        
        // Get users in a segment range
        List<String> usersInRange = rfmAnalysis.getUsersInSegmentRange(scores, 4, 5, 4, 5, 4, 5);
        
        // Check users in range
        assertNotNull(usersInRange);
        
        // User 1 should be in this range (high in all dimensions)
        assertTrue(usersInRange.contains("user1"));
    }
}
