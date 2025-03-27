package com.insightaxisdb.ml;

import com.insightaxisdb.storage.Event;
import com.insightaxisdb.storage.EventStore;
import com.insightaxisdb.storage.UserProfile;
import com.insightaxisdb.storage.UserProfileStore;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple recommendation engine based on collaborative filtering.
 */
public class RecommendationEngine {
    
    private final UserProfileStore userProfileStore;
    private final EventStore eventStore;
    private final String itemViewEventName;
    private final String itemPurchaseEventName;
    private final String itemIdField;
    
    // Cache for item similarity
    private Map<String, Map<String, Double>> itemSimilarityCache;
    private long lastCacheUpdateTime;
    private static final long CACHE_TTL_MILLIS = 3600 * 1000; // 1 hour
    
    /**
     * Create a new recommendation engine.
     *
     * @param userProfileStore User profile store
     * @param eventStore Event store
     * @param itemViewEventName Name of the item view event
     * @param itemPurchaseEventName Name of the item purchase event
     * @param itemIdField Name of the field containing the item ID
     */
    public RecommendationEngine(UserProfileStore userProfileStore, EventStore eventStore,
                               String itemViewEventName, String itemPurchaseEventName,
                               String itemIdField) {
        this.userProfileStore = userProfileStore;
        this.eventStore = eventStore;
        this.itemViewEventName = itemViewEventName;
        this.itemPurchaseEventName = itemPurchaseEventName;
        this.itemIdField = itemIdField;
        
        this.itemSimilarityCache = new HashMap<>();
        this.lastCacheUpdateTime = 0;
    }
    
    /**
     * Get item recommendations for a user.
     *
     * @param userId User ID
     * @param maxRecommendations Maximum number of recommendations to return
     * @return List of recommended item IDs with scores
     */
    public List<ScoredItem> getRecommendations(String userId, int maxRecommendations) {
        // Get user's viewed and purchased items
        Set<String> viewedItems = getUserItems(userId, itemViewEventName);
        Set<String> purchasedItems = getUserItems(userId, itemPurchaseEventName);
        
        // Combine viewed and purchased items, with purchased items having higher weight
        Map<String, Double> userItems = new HashMap<>();
        
        for (String itemId : viewedItems) {
            userItems.put(itemId, 1.0);
        }
        
        for (String itemId : purchasedItems) {
            userItems.put(itemId, 2.0); // Higher weight for purchased items
        }
        
        // If the user has no items, return popular items
        if (userItems.isEmpty()) {
            return getPopularItems(maxRecommendations);
        }
        
        // Update item similarity cache if needed
        updateItemSimilarityCache();
        
        // Calculate recommendations based on item similarity
        Map<String, Double> recommendations = new HashMap<>();
        
        for (Map.Entry<String, Double> entry : userItems.entrySet()) {
            String userItemId = entry.getKey();
            double userItemWeight = entry.getValue();
            
            Map<String, Double> similarItems = itemSimilarityCache.getOrDefault(userItemId, Collections.emptyMap());
            
            for (Map.Entry<String, Double> similarEntry : similarItems.entrySet()) {
                String similarItemId = similarEntry.getKey();
                double similarity = similarEntry.getValue();
                
                // Skip items the user has already interacted with
                if (userItems.containsKey(similarItemId)) {
                    continue;
                }
                
                // Add to recommendations with weighted score
                double score = similarity * userItemWeight;
                recommendations.merge(similarItemId, score, Double::sum);
            }
        }
        
        // Sort recommendations by score
        return recommendations.entrySet().stream()
                .map(entry -> new ScoredItem(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(ScoredItem::getScore).reversed())
                .limit(maxRecommendations)
                .collect(Collectors.toList());
    }
    
    /**
     * Get popular items.
     *
     * @param maxItems Maximum number of items to return
     * @return List of popular item IDs with scores
     */
    public List<ScoredItem> getPopularItems(int maxItems) {
        // Get all purchase events
        List<Event> purchaseEvents = eventStore.getEventsByName(itemPurchaseEventName);
        
        // Count purchases for each item
        Map<String, Integer> itemCounts = new HashMap<>();
        
        for (Event event : purchaseEvents) {
            Object itemIdObj = event.getProperties().get(itemIdField);
            if (itemIdObj instanceof String) {
                String itemId = (String) itemIdObj;
                itemCounts.merge(itemId, 1, Integer::sum);
            }
        }
        
        // Sort items by popularity
        return itemCounts.entrySet().stream()
                .map(entry -> new ScoredItem(entry.getKey(), entry.getValue().doubleValue()))
                .sorted(Comparator.comparing(ScoredItem::getScore).reversed())
                .limit(maxItems)
                .collect(Collectors.toList());
    }
    
    /**
     * Get items a user has interacted with.
     *
     * @param userId User ID
     * @param eventName Event name
     * @return Set of item IDs
     */
    private Set<String> getUserItems(String userId, String eventName) {
        List<Event> events = eventStore.getUserEventsByName(userId, eventName);
        
        return events.stream()
                .map(event -> event.getProperties().get(itemIdField))
                .filter(itemId -> itemId instanceof String)
                .map(itemId -> (String) itemId)
                .collect(Collectors.toSet());
    }
    
    /**
     * Update the item similarity cache if it's expired.
     */
    private synchronized void updateItemSimilarityCache() {
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastCacheUpdateTime < CACHE_TTL_MILLIS) {
            return;
        }
        
        // Calculate item similarity
        itemSimilarityCache = calculateItemSimilarity();
        lastCacheUpdateTime = currentTime;
    }
    
    /**
     * Calculate item similarity based on co-occurrence in user interactions.
     *
     * @return Map of item ID to similar items with similarity scores
     */
    private Map<String, Map<String, Double>> calculateItemSimilarity() {
        // Get all purchase events
        List<Event> purchaseEvents = eventStore.getEventsByName(itemPurchaseEventName);
        
        // Group purchases by user
        Map<String, Set<String>> userItems = new HashMap<>();
        
        for (Event event : purchaseEvents) {
            String userId = event.getUserId();
            Object itemIdObj = event.getProperties().get(itemIdField);
            
            if (itemIdObj instanceof String) {
                String itemId = (String) itemIdObj;
                userItems.computeIfAbsent(userId, k -> new HashSet<>()).add(itemId);
            }
        }
        
        // Count co-occurrences of items
        Map<String, Map<String, Integer>> coOccurrences = new HashMap<>();
        Map<String, Integer> itemCounts = new HashMap<>();
        
        for (Set<String> items : userItems.values()) {
            // Update item counts
            for (String itemId : items) {
                itemCounts.merge(itemId, 1, Integer::sum);
            }
            
            // Update co-occurrences
            for (String itemId1 : items) {
                for (String itemId2 : items) {
                    if (itemId1.equals(itemId2)) {
                        continue;
                    }
                    
                    coOccurrences.computeIfAbsent(itemId1, k -> new HashMap<>())
                            .merge(itemId2, 1, Integer::sum);
                }
            }
        }
        
        // Calculate similarity using Jaccard similarity
        Map<String, Map<String, Double>> similarity = new HashMap<>();
        
        for (Map.Entry<String, Map<String, Integer>> entry : coOccurrences.entrySet()) {
            String itemId1 = entry.getKey();
            Map<String, Integer> coOccurrenceMap = entry.getValue();
            
            int count1 = itemCounts.getOrDefault(itemId1, 0);
            
            Map<String, Double> itemSimilarity = new HashMap<>();
            
            for (Map.Entry<String, Integer> coEntry : coOccurrenceMap.entrySet()) {
                String itemId2 = coEntry.getKey();
                int coCount = coEntry.getValue();
                
                int count2 = itemCounts.getOrDefault(itemId2, 0);
                
                // Jaccard similarity: |A ∩ B| / |A ∪ B|
                double jaccardSimilarity = (double) coCount / (count1 + count2 - coCount);
                
                itemSimilarity.put(itemId2, jaccardSimilarity);
            }
            
            similarity.put(itemId1, itemSimilarity);
        }
        
        return similarity;
    }
    
    /**
     * Item with a score.
     */
    public static class ScoredItem {
        private final String itemId;
        private final double score;
        
        public ScoredItem(String itemId, double score) {
            this.itemId = itemId;
            this.score = score;
        }
        
        public String getItemId() {
            return itemId;
        }
        
        public double getScore() {
            return score;
        }
    }
}
