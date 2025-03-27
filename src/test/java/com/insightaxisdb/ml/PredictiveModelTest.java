package com.insightaxisdb.ml;

import com.insightaxisdb.storage.EventStore;
import com.insightaxisdb.storage.UserProfileStore;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test cases for the PredictiveModel class.
 */
public class PredictiveModelTest {
    
    private UserProfileStore userProfileStore;
    private EventStore eventStore;
    private PredictiveModel predictiveModel;
    
    @Before
    public void setUp() {
        userProfileStore = new UserProfileStore();
        eventStore = new EventStore(userProfileStore);
        predictiveModel = new PredictiveModel(userProfileStore, eventStore);
        
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
        
        // User 1: Frequent purchaser
        for (int i = 0; i < 5; i++) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("item_id", "item" + i);
            properties.put("price", 10.0 * i);
            
            // Recent purchases
            eventStore.addEvent("purchase", "user1", properties, System.currentTimeMillis() - i * 24 * 60 * 60 * 1000L);
        }
        
        // User 2: Infrequent purchaser
        for (int i = 0; i < 2; i++) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("item_id", "item" + i);
            properties.put("price", 10.0 * i);
            
            // Less recent purchases
            eventStore.addEvent("purchase", "user2", properties, System.currentTimeMillis() - (i + 10) * 24 * 60 * 60 * 1000L);
        }
        
        // User 3: Frequent viewer, infrequent purchaser
        for (int i = 0; i < 10; i++) {
            Map<String, Object> viewProperties = new HashMap<>();
            viewProperties.put("item_id", "item" + i);
            
            // Recent views
            eventStore.addEvent("view", "user3", viewProperties, System.currentTimeMillis() - i * 12 * 60 * 60 * 1000L);
            
            if (i < 2) {
                Map<String, Object> purchaseProperties = new HashMap<>();
                purchaseProperties.put("item_id", "item" + i);
                purchaseProperties.put("price", 10.0 * i);
                
                // Recent purchases
                eventStore.addEvent("purchase", "user3", purchaseProperties, System.currentTimeMillis() - i * 24 * 60 * 60 * 1000L);
            }
        }
        
        // User 4: Active at specific hours
        for (int i = 0; i < 10; i++) {
            Map<String, Object> properties = new HashMap<>();
            
            // Set timestamp to a specific hour (e.g., 9 AM)
            long timestamp = System.currentTimeMillis() - i * 24 * 60 * 60 * 1000L;
            timestamp = timestamp - (timestamp % (24 * 60 * 60 * 1000L)) + 9 * 60 * 60 * 1000L;
            
            eventStore.addEvent("login", "user4", properties, timestamp);
        }
        
        // User 5: No events
    }
    
    @Test
    public void testPredictEventLikelihood() {
        // Predict likelihood for user1 (frequent purchaser)
        double user1Likelihood = predictiveModel.predictEventLikelihood("user1", "purchase");
        
        // Predict likelihood for user2 (infrequent purchaser)
        double user2Likelihood = predictiveModel.predictEventLikelihood("user2", "purchase");
        
        // Predict likelihood for user5 (no events)
        double user5Likelihood = predictiveModel.predictEventLikelihood("user5", "purchase");
        
        // Check predictions
        assertTrue(user1Likelihood > 0.5); // High likelihood
        assertTrue(user2Likelihood < user1Likelihood); // Lower than user1
        assertEquals(0.0, user5Likelihood, 0.01); // Zero likelihood
    }
    
    @Test
    public void testPredictEventLikelihoodForNewEvent() {
        // Predict likelihood for user1 for an event they haven't performed
        double likelihood = predictiveModel.predictEventLikelihood("user1", "signup");
        
        // Check prediction
        assertTrue(likelihood >= 0.0 && likelihood <= 1.0);
    }
    
    @Test
    public void testPredictBestTimeToSend() {
        // Predict best time for user4 (active at 9 AM)
        int bestHour = predictiveModel.predictBestTimeToSend("user4");
        
        // Check prediction
        assertEquals(9, bestHour);
        
        // Predict best time for user5 (no events)
        int unknownHour = predictiveModel.predictBestTimeToSend("user5");
        
        // Check prediction
        assertEquals(-1, unknownHour);
    }
}
