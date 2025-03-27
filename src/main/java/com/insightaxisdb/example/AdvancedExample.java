package com.insightaxisdb.example;

import com.insightaxisdb.ml.PredictiveModel;
import com.insightaxisdb.ml.RecommendationEngine;
import com.insightaxisdb.query.Query;
import com.insightaxisdb.query.QueryCondition;
import com.insightaxisdb.query.QueryEngine;
import com.insightaxisdb.query.QueryResult;
import com.insightaxisdb.segmentation.CohortAnalysis;
import com.insightaxisdb.segmentation.RFMAnalysis;
import com.insightaxisdb.storage.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Advanced example demonstrating the new features of InsightAxisDB.
 */
public class AdvancedExample {
    
    public static void main(String[] args) {
        try {
            // Create stores
            UserProfileStore userProfileStore = new UserProfileStore();
            EventStore eventStore = new EventStore(userProfileStore);
            
            // Create sample data
            createSampleData(userProfileStore, eventStore);
            
            // Demonstrate query engine
            demonstrateQueryEngine(userProfileStore, eventStore);
            
            // Demonstrate persistence
            demonstratePersistence(userProfileStore, eventStore);
            
            // Demonstrate segmentation
            demonstrateSegmentation(userProfileStore, eventStore);
            
            // Demonstrate ML
            demonstrateML(userProfileStore, eventStore);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Create sample data.
     */
    private static void createSampleData(UserProfileStore userProfileStore, EventStore eventStore) {
        System.out.println("\n=== Creating Sample Data ===");
        
        // Create users
        for (int i = 1; i <= 5; i++) {
            String userId = "user" + i;
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("name", "User " + i);
            properties.put("email", "user" + i + "@example.com");
            properties.put("age", 20 + i);
            
            UserProfile profile = userProfileStore.createProfile(userId, properties);
            System.out.println("Created user: " + profile.getUserId());
        }
        
        // Create events
        Random random = new Random(42);
        
        for (int i = 1; i <= 5; i++) {
            String userId = "user" + i;
            
            // Add login events
            for (int j = 0; j < 5; j++) {
                Map<String, Object> properties = new HashMap<>();
                properties.put("device", random.nextBoolean() ? "mobile" : "desktop");
                properties.put("os", random.nextBoolean() ? "iOS" : "Android");
                
                long timestamp = System.currentTimeMillis() - (j * 24 * 60 * 60 * 1000L);
                
                Event event = eventStore.addEvent("login", userId, properties, timestamp);
                System.out.println("Created login event for " + userId + ": " + event.getEventId());
            }
            
            // Add view_item events
            for (int j = 0; j < 10; j++) {
                Map<String, Object> properties = new HashMap<>();
                properties.put("item_id", "item" + (j % 5 + 1));
                properties.put("category", "category" + (j % 3 + 1));
                properties.put("price", 10.0 + j);
                
                long timestamp = System.currentTimeMillis() - (j * 12 * 60 * 60 * 1000L);
                
                Event event = eventStore.addEvent("view_item", userId, properties, timestamp);
                System.out.println("Created view_item event for " + userId + ": " + event.getEventId());
            }
            
            // Add purchase_item events
            for (int j = 0; j < i; j++) {
                Map<String, Object> properties = new HashMap<>();
                properties.put("item_id", "item" + (j % 5 + 1));
                properties.put("category", "category" + (j % 3 + 1));
                properties.put("price", 10.0 + j);
                properties.put("quantity", j + 1);
                
                long timestamp = System.currentTimeMillis() - (j * 48 * 60 * 60 * 1000L);
                
                Event event = eventStore.addEvent("purchase_item", userId, properties, timestamp);
                System.out.println("Created purchase_item event for " + userId + ": " + event.getEventId());
            }
        }
    }
    
    /**
     * Demonstrate query engine.
     */
    private static void demonstrateQueryEngine(UserProfileStore userProfileStore, EventStore eventStore) {
        System.out.println("\n=== Demonstrating Query Engine ===");
        
        // Create query engine
        QueryEngine queryEngine = new QueryEngine(userProfileStore, eventStore);
        
        // Query users by age
        Query userQuery = new Query()
                .where(QueryCondition.gte("age", 23))
                .select("userId", "name", "age")
                .orderBy("age", Query.SortOrder.DESCENDING);
        
        QueryResult userResult = queryEngine.queryUserProfiles(userQuery);
        
        System.out.println("\nUsers with age >= 23:");
        for (Map<String, Object> row : userResult.getRows()) {
            System.out.println("  " + row);
        }
        
        // Query events by type and price
        Query eventQuery = new Query()
                .where(Arrays.asList(
                        QueryCondition.eq("eventName", "purchase_item"),
                        QueryCondition.gte("price", 12.0)
                ))
                .select("eventId", "userId", "item_id", "price")
                .orderBy("price", Query.SortOrder.ASCENDING)
                .limit(5);
        
        QueryResult eventResult = queryEngine.queryEvents(eventQuery);
        
        System.out.println("\nPurchase events with price >= 12.0:");
        for (Map<String, Object> row : eventResult.getRows()) {
            System.out.println("  " + row);
        }
        
        // Query events for a specific user with aggregation
        Query userEventQuery = new Query()
                .where(QueryCondition.eq("eventName", "purchase_item"))
                .aggregate("price", Query.AggregationType.SUM, "total_spent")
                .aggregate("price", Query.AggregationType.AVG, "avg_price")
                .aggregate("eventId", Query.AggregationType.COUNT, "purchase_count");
        
        QueryResult userEventResult = queryEngine.queryUserEvents("user3", userEventQuery);
        
        System.out.println("\nAggregations for user3 purchases:");
        System.out.println("  " + userEventResult.getAggregations());
    }
    
    /**
     * Demonstrate persistence.
     */
    private static void demonstratePersistence(UserProfileStore userProfileStore, EventStore eventStore) throws IOException {
        System.out.println("\n=== Demonstrating Persistence ===");
        
        // Create temporary directory
        File tempDir = new File("temp_data");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        
        // Create persistence manager
        PersistenceManager persistenceManager = new PersistenceManager(
                tempDir.getAbsolutePath(), userProfileStore, eventStore, 60000);
        
        // Save data
        persistenceManager.saveAll();
        System.out.println("Saved data to " + tempDir.getAbsolutePath());
        
        // Save events in NCF format
        persistenceManager.saveEventsNCF();
        System.out.println("Saved events in NCF format");
        
        // Apply retention policy
        persistenceManager.applyRetentionPolicy(30);
        System.out.println("Applied retention policy (30 days)");
        
        // Shutdown persistence manager
        persistenceManager.shutdown();
        System.out.println("Persistence manager shut down");
    }
    
    /**
     * Demonstrate segmentation.
     */
    private static void demonstrateSegmentation(UserProfileStore userProfileStore, EventStore eventStore) {
        System.out.println("\n=== Demonstrating Segmentation ===");
        
        // RFM Analysis
        RFMAnalysis rfmAnalysis = new RFMAnalysis(
                userProfileStore, eventStore, "purchase_item", "price");
        
        Map<String, RFMAnalysis.RFMScore> rfmScores = rfmAnalysis.calculateRFMScores(30, 5);
        
        System.out.println("\nRFM Scores:");
        for (Map.Entry<String, RFMAnalysis.RFMScore> entry : rfmScores.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
        
        // Get high-value customers
        List<String> highValueCustomers = rfmAnalysis.getHighValueCustomers(rfmScores, 5);
        System.out.println("\nHigh-value customers: " + highValueCustomers);
        
        // Cohort Analysis
        CohortAnalysis cohortAnalysis = new CohortAnalysis(userProfileStore, eventStore);
        
        CohortAnalysis.CohortResult cohortResult = cohortAnalysis.calculateRetentionCohorts(
                CohortAnalysis.TimePeriod.WEEK, 4, "login");
        
        System.out.println("\nCohort Analysis (Weekly, 4 periods, login event):");
        System.out.println("  Cohort sizes:");
        for (int i = 0; i < cohortResult.getNumPeriods(); i++) {
            System.out.println("    Cohort " + i + ": " + cohortResult.getCohortSize(i));
        }
        
        System.out.println("  Retention matrix:");
        int[][] retentionMatrix = cohortResult.getRetentionMatrix();
        for (int i = 0; i < retentionMatrix.length; i++) {
            System.out.print("    Cohort " + i + ":");
            for (int j = 0; j < retentionMatrix[i].length; j++) {
                if (j < cohortResult.getNumPeriods() - i) {
                    System.out.print(" " + retentionMatrix[i][j]);
                }
            }
            System.out.println();
        }
        
        System.out.println("  Retention percentages:");
        double[][] retentionPercentages = cohortResult.getRetentionPercentages();
        for (int i = 0; i < retentionPercentages.length; i++) {
            System.out.print("    Cohort " + i + ":");
            for (int j = 0; j < retentionPercentages[i].length; j++) {
                if (j < cohortResult.getNumPeriods() - i) {
                    System.out.printf(" %.2f", retentionPercentages[i][j]);
                }
            }
            System.out.println();
        }
    }
    
    /**
     * Demonstrate ML.
     */
    private static void demonstrateML(UserProfileStore userProfileStore, EventStore eventStore) {
        System.out.println("\n=== Demonstrating ML ===");
        
        // Recommendation Engine
        RecommendationEngine recommendationEngine = new RecommendationEngine(
                userProfileStore, eventStore, "view_item", "purchase_item", "item_id");
        
        List<RecommendationEngine.ScoredItem> recommendations = 
                recommendationEngine.getRecommendations("user2", 3);
        
        System.out.println("\nRecommendations for user2:");
        for (RecommendationEngine.ScoredItem item : recommendations) {
            System.out.printf("  %s (score: %.2f)%n", item.getItemId(), item.getScore());
        }
        
        // Popular items
        List<RecommendationEngine.ScoredItem> popularItems = 
                recommendationEngine.getPopularItems(3);
        
        System.out.println("\nPopular items:");
        for (RecommendationEngine.ScoredItem item : popularItems) {
            System.out.printf("  %s (score: %.2f)%n", item.getItemId(), item.getScore());
        }
        
        // Predictive Model
        PredictiveModel predictiveModel = new PredictiveModel(userProfileStore, eventStore);
        
        double likelihood = predictiveModel.predictEventLikelihood("user3", "purchase_item");
        System.out.printf("\nLikelihood of user3 performing purchase_item: %.2f%n", likelihood);
        
        int bestHour = predictiveModel.predictBestTimeToSend("user4");
        System.out.println("Best hour to send notification to user4: " + bestHour);
    }
}
