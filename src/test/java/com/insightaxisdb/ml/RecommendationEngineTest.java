package com.insightaxisdb.ml;

import com.insightaxisdb.storage.EventStore;
import com.insightaxisdb.storage.UserProfileStore;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test cases for the RecommendationEngine class.
 */
public class RecommendationEngineTest {
    
    private UserProfileStore userProfileStore;
    private EventStore eventStore;
    private RecommendationEngine recommendationEngine;
    
    @Before
    public void setUp() {
        userProfileStore = new UserProfileStore();
        eventStore = new EventStore(userProfileStore);
        recommendationEngine = new RecommendationEngine(
                userProfileStore, eventStore, "view_item", "purchase_item", "item_id");
        
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
        
        // Create item view and purchase events
        
        // User 1: Views and purchases items 1, 2, 3
        for (int i = 1; i <= 3; i++) {
            String itemId = "item" + i;
            
            Map<String, Object> viewProperties = new HashMap<>();
            viewProperties.put("item_id", itemId);
            viewProperties.put("category", "category" + (i % 2 + 1));
            
            eventStore.addEvent("view_item", "user1", viewProperties, System.currentTimeMillis() - i * 1000);
            
            Map<String, Object> purchaseProperties = new HashMap<>();
            purchaseProperties.put("item_id", itemId);
            purchaseProperties.put("category", "category" + (i % 2 + 1));
            purchaseProperties.put("price", 10.0 * i);
            
            eventStore.addEvent("purchase_item", "user1", purchaseProperties, System.currentTimeMillis() - i * 500);
        }
        
        // User 2: Views items 1, 2, 3, 4 and purchases items 1, 3
        for (int i = 1; i <= 4; i++) {
            String itemId = "item" + i;
            
            Map<String, Object> viewProperties = new HashMap<>();
            viewProperties.put("item_id", itemId);
            viewProperties.put("category", "category" + (i % 2 + 1));
            
            eventStore.addEvent("view_item", "user2", viewProperties, System.currentTimeMillis() - i * 1000);
            
            if (i == 1 || i == 3) {
                Map<String, Object> purchaseProperties = new HashMap<>();
                purchaseProperties.put("item_id", itemId);
                purchaseProperties.put("category", "category" + (i % 2 + 1));
                purchaseProperties.put("price", 10.0 * i);
                
                eventStore.addEvent("purchase_item", "user2", purchaseProperties, System.currentTimeMillis() - i * 500);
            }
        }
        
        // User 3: Views and purchases items 3, 4, 5
        for (int i = 3; i <= 5; i++) {
            String itemId = "item" + i;
            
            Map<String, Object> viewProperties = new HashMap<>();
            viewProperties.put("item_id", itemId);
            viewProperties.put("category", "category" + (i % 2 + 1));
            
            eventStore.addEvent("view_item", "user3", viewProperties, System.currentTimeMillis() - i * 1000);
            
            Map<String, Object> purchaseProperties = new HashMap<>();
            purchaseProperties.put("item_id", itemId);
            purchaseProperties.put("category", "category" + (i % 2 + 1));
            purchaseProperties.put("price", 10.0 * i);
            
            eventStore.addEvent("purchase_item", "user3", purchaseProperties, System.currentTimeMillis() - i * 500);
        }
        
        // User 4: Views items 1, 5 and purchases item 5
        for (int i : new int[]{1, 5}) {
            String itemId = "item" + i;
            
            Map<String, Object> viewProperties = new HashMap<>();
            viewProperties.put("item_id", itemId);
            viewProperties.put("category", "category" + (i % 2 + 1));
            
            eventStore.addEvent("view_item", "user4", viewProperties, System.currentTimeMillis() - i * 1000);
            
            if (i == 5) {
                Map<String, Object> purchaseProperties = new HashMap<>();
                purchaseProperties.put("item_id", itemId);
                purchaseProperties.put("category", "category" + (i % 2 + 1));
                purchaseProperties.put("price", 10.0 * i);
                
                eventStore.addEvent("purchase_item", "user4", purchaseProperties, System.currentTimeMillis() - i * 500);
            }
        }
        
        // User 5: No views or purchases
    }
    
    @Test
    public void testGetRecommendations() {
        // Get recommendations for user2
        List<RecommendationEngine.ScoredItem> recommendations = recommendationEngine.getRecommendations("user2", 3);
        
        // Check recommendations
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
        
        // User2 has viewed items 1, 2, 3, 4 and purchased items 1, 3
        // So recommendations should include items 5 and possibly 2, 4 (already viewed but not purchased)
        
        // Check that recommendations don't include items the user has purchased
        for (RecommendationEngine.ScoredItem item : recommendations) {
            assertFalse(item.getItemId().equals("item1"));
            assertFalse(item.getItemId().equals("item3"));
        }
    }
    
    @Test
    public void testGetRecommendationsForNewUser() {
        // Get recommendations for user5 (no views or purchases)
        List<RecommendationEngine.ScoredItem> recommendations = recommendationEngine.getRecommendations("user5", 3);
        
        // Check recommendations
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
        
        // For a new user, recommendations should be popular items
        // Items 3 and 5 are purchased by 2 users each, so they should be recommended
    }
    
    @Test
    public void testGetPopularItems() {
        // Get popular items
        List<RecommendationEngine.ScoredItem> popularItems = recommendationEngine.getPopularItems(5);
        
        // Check popular items
        assertNotNull(popularItems);
        assertFalse(popularItems.isEmpty());
        
        // Items 3 and 5 are purchased by 2 users each, so they should be at the top
        assertEquals("item3", popularItems.get(0).getItemId());
        assertEquals("item5", popularItems.get(1).getItemId());
        
        // Check scores
        assertTrue(popularItems.get(0).getScore() >= 2.0);
        assertTrue(popularItems.get(1).getScore() >= 2.0);
    }
}
