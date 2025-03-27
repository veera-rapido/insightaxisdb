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
import java.io.RandomAccessFile;
import java.util.*;

/**
 * Comprehensive example demonstrating all features of InsightAxisDB.
 */
public class ComprehensiveExample {
    
    public static void main(String[] args) {
        try {
            System.out.println("=== InsightAxisDB Comprehensive Example ===\n");
            
            // Create stores
            UserProfileStore userProfileStore = new UserProfileStore();
            EventStore eventStore = new EventStore(userProfileStore);
            
            // Create sample data
            createSampleData(userProfileStore, eventStore);
            
            // Demonstrate NCF storage
            demonstrateNCFStorage();
            
            // Demonstrate user profile and event management
            demonstrateUserProfileAndEventManagement(userProfileStore, eventStore);
            
            // Demonstrate query engine
            demonstrateQueryEngine(userProfileStore, eventStore);
            
            // Demonstrate persistence
            demonstratePersistence(userProfileStore, eventStore);
            
            // Demonstrate segmentation
            demonstrateSegmentation(userProfileStore, eventStore);
            
            // Demonstrate ML
            demonstrateML(userProfileStore, eventStore);
            
            System.out.println("\n=== Example Complete ===");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Create sample data for the example.
     */
    private static void createSampleData(UserProfileStore userProfileStore, EventStore eventStore) {
        System.out.println("Creating sample data...");
        
        // Create users
        for (int i = 1; i <= 10; i++) {
            String userId = "user" + i;
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("name", "User " + i);
            properties.put("email", "user" + i + "@example.com");
            properties.put("age", 20 + i);
            properties.put("country", i % 3 == 0 ? "USA" : (i % 3 == 1 ? "UK" : "Canada"));
            
            userProfileStore.createProfile(userId, properties);
        }
        
        // Create events
        Random random = new Random(42);
        String[] eventTypes = {"login", "view_item", "add_to_cart", "purchase", "logout"};
        String[] deviceTypes = {"desktop", "mobile", "tablet"};
        String[] categories = {"electronics", "clothing", "books", "home", "sports"};
        
        for (int i = 1; i <= 10; i++) {
            String userId = "user" + i;
            
            // Number of events depends on user ID (some users are more active)
            int numEvents = 10 + i * 2;
            
            for (int j = 0; j < numEvents; j++) {
                String eventType = eventTypes[random.nextInt(eventTypes.length)];
                
                Map<String, Object> properties = new HashMap<>();
                properties.put("device", deviceTypes[random.nextInt(deviceTypes.length)]);
                
                if (eventType.equals("view_item") || eventType.equals("add_to_cart") || eventType.equals("purchase")) {
                    String itemId = "item" + (random.nextInt(20) + 1);
                    String category = categories[random.nextInt(categories.length)];
                    double price = 10.0 + random.nextInt(90);
                    
                    properties.put("item_id", itemId);
                    properties.put("category", category);
                    properties.put("price", price);
                    
                    if (eventType.equals("purchase")) {
                        properties.put("quantity", random.nextInt(3) + 1);
                    }
                }
                
                // Event timestamp (more recent for higher user IDs)
                long timestamp = System.currentTimeMillis() - (30 - i) * 24 * 60 * 60 * 1000L - j * 3600 * 1000L;
                
                eventStore.addEvent(eventType, userId, properties, timestamp);
            }
        }
        
        System.out.println("Created " + userProfileStore.getProfile("user1").getEventCount() + " events for user1");
        System.out.println("Created " + userProfileStore.getProfile("user10").getEventCount() + " events for user10");
    }
    
    /**
     * Demonstrate NCF storage.
     */
    private static void demonstrateNCFStorage() throws IOException {
        System.out.println("\n=== Demonstrating NCF Storage ===");
        
        // Create a temporary file
        File tempFile = File.createTempFile("ncf-example", ".ncf");
        tempFile.deleteOnExit();
        
        System.out.println("Writing data to NCF file: " + tempFile.getAbsolutePath());
        
        // Create NCF writer
        NCF.Writer writer = new NCF.Writer("lz4");
        
        // Add some rows
        for (int i = 0; i < 5; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", i);
            row.put("name", "User " + i);
            row.put("active", i % 2 == 0);
            row.put("score", i * 10.5);
            
            if (i % 2 == 0) {
                // Add nested data for some rows
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("created_at", System.currentTimeMillis());
                metadata.put("tags", Arrays.asList("tag1", "tag2"));
                row.put("metadata", metadata);
            }
            
            writer.addRow(row);
        }
        
        // Write to file
        try (RandomAccessFile raf = new RandomAccessFile(tempFile, "rw")) {
            writer.write(raf);
        }
        
        System.out.println("Reading data from NCF file...");
        
        // Read from file
        try (RandomAccessFile raf = new RandomAccessFile(tempFile, "r");
             NCF.Reader reader = new NCF.Reader(raf)) {
            
            // Print header info
            System.out.println("File header:");
            System.out.println("  Column count: " + reader.getHeader().getColumnCount());
            System.out.println("  Row count: " + reader.getHeader().getRowCount());
            
            // Read all rows
            List<Map<String, Object>> rows = reader.readRows(0, null);
            
            System.out.println("Read " + rows.size() + " rows");
            System.out.println("First row: " + rows.get(0));
            System.out.println("Last row: " + rows.get(rows.size() - 1));
        }
    }
    
    /**
     * Demonstrate user profile and event management.
     */
    private static void demonstrateUserProfileAndEventManagement(UserProfileStore userProfileStore, EventStore eventStore) {
        System.out.println("\n=== Demonstrating User Profile and Event Management ===");
        
        // Get a user profile
        UserProfile user1 = userProfileStore.getProfile("user1");
        System.out.println("User profile for user1:");
        System.out.println("  Name: " + user1.getProperties().get("name"));
        System.out.println("  Email: " + user1.getProperties().get("email"));
        System.out.println("  Age: " + user1.getProperties().get("age"));
        System.out.println("  Country: " + user1.getProperties().get("country"));
        System.out.println("  Event count: " + user1.getEventCount());
        
        // Update user properties
        Map<String, Object> newProperties = new HashMap<>();
        newProperties.put("premium", true);
        newProperties.put("last_login", System.currentTimeMillis());
        
        userProfileStore.updateProfile("user1", newProperties);
        
        user1 = userProfileStore.getProfile("user1");
        System.out.println("\nUpdated user profile for user1:");
        System.out.println("  Premium: " + user1.getProperties().get("premium"));
        System.out.println("  Last login: " + new Date((Long) user1.getProperties().get("last_login")));
        
        // Get user events
        List<Event> user1Events = eventStore.getUserEvents("user1");
        System.out.println("\nEvents for user1:");
        System.out.println("  Total events: " + user1Events.size());
        
        // Count events by type
        Map<String, Integer> eventCounts = new HashMap<>();
        for (Event event : user1Events) {
            eventCounts.merge(event.getEventName(), 1, Integer::sum);
        }
        
        System.out.println("  Event counts by type:");
        for (Map.Entry<String, Integer> entry : eventCounts.entrySet()) {
            System.out.println("    " + entry.getKey() + ": " + entry.getValue());
        }
        
        // Add a new event
        Map<String, Object> eventProperties = new HashMap<>();
        eventProperties.put("device", "mobile");
        eventProperties.put("os", "iOS");
        
        Event newEvent = eventStore.addEvent("login", "user1", eventProperties, System.currentTimeMillis());
        
        System.out.println("\nAdded new login event for user1:");
        System.out.println("  Event ID: " + newEvent.getEventId());
        System.out.println("  Timestamp: " + new Date(newEvent.getTimestamp()));
        System.out.println("  Device: " + newEvent.getProperties().get("device"));
        System.out.println("  OS: " + newEvent.getProperties().get("os"));
        
        // Get events by name
        List<Event> loginEvents = eventStore.getUserEventsByName("user1", "login");
        System.out.println("\nLogin events for user1: " + loginEvents.size());
        
        // Get events in timerange
        long now = System.currentTimeMillis();
        long oneDayAgo = now - 24 * 60 * 60 * 1000L;
        
        List<Event> recentEvents = eventStore.getUserEventsInTimerange("user1", oneDayAgo, now);
        System.out.println("\nEvents for user1 in the last 24 hours: " + recentEvents.size());
    }
    
    /**
     * Demonstrate query engine.
     */
    private static void demonstrateQueryEngine(UserProfileStore userProfileStore, EventStore eventStore) {
        System.out.println("\n=== Demonstrating Query Engine ===");
        
        // Create query engine
        QueryEngine queryEngine = new QueryEngine(userProfileStore, eventStore);
        
        // Query 1: Find users from the UK who are older than 25
        Query userQuery = new Query()
                .where(Arrays.asList(
                        QueryCondition.eq("country", "UK"),
                        QueryCondition.gt("age", 25)
                ))
                .select("userId", "name", "age", "email")
                .orderBy("age", Query.SortOrder.ASCENDING);
        
        QueryResult userResult = queryEngine.queryUserProfiles(userQuery);
        
        System.out.println("\nUsers from the UK who are older than 25:");
        for (Map<String, Object> row : userResult.getRows()) {
            System.out.println("  " + row.get("name") + " (Age: " + row.get("age") + ")");
        }
        
        // Query 2: Find purchase events with price > 50
        Query purchaseQuery = new Query()
                .where(Arrays.asList(
                        QueryCondition.eq("eventName", "purchase"),
                        QueryCondition.gt("price", 50.0)
                ))
                .select("eventId", "userId", "item_id", "price", "category")
                .orderBy("price", Query.SortOrder.DESCENDING)
                .limit(5);
        
        QueryResult purchaseResult = queryEngine.queryEvents(purchaseQuery);
        
        System.out.println("\nTop 5 most expensive purchases:");
        for (Map<String, Object> row : purchaseResult.getRows()) {
            System.out.println("  User " + row.get("userId") + " purchased " + row.get("item_id") + 
                    " for $" + row.get("price") + " (Category: " + row.get("category") + ")");
        }
        
        // Query 3: Calculate purchase statistics for user10
        Query statsQuery = new Query()
                .where(QueryCondition.eq("eventName", "purchase"))
                .aggregate("price", Query.AggregationType.SUM, "total_spent")
                .aggregate("price", Query.AggregationType.AVG, "avg_price")
                .aggregate("price", Query.AggregationType.MIN, "min_price")
                .aggregate("price", Query.AggregationType.MAX, "max_price")
                .aggregate("eventId", Query.AggregationType.COUNT, "purchase_count");
        
        QueryResult statsResult = queryEngine.queryUserEvents("user10", statsQuery);
        
        System.out.println("\nPurchase statistics for user10:");
        Map<String, Object> aggregations = statsResult.getAggregations();
        System.out.println("  Total spent: $" + aggregations.get("total_spent"));
        System.out.println("  Average price: $" + aggregations.get("avg_price"));
        System.out.println("  Minimum price: $" + aggregations.get("min_price"));
        System.out.println("  Maximum price: $" + aggregations.get("max_price"));
        System.out.println("  Purchase count: " + aggregations.get("purchase_count"));
        
        // Query 4: Find users who viewed an item and then purchased it
        Query sequenceQuery = new Query()
                .select("userId", "name");
        
        QueryResult sequenceResult = queryEngine.findUsersWithEventSequence(
                Arrays.asList("view_item", "purchase"), 3600 * 1000L, sequenceQuery);
        
        System.out.println("\nUsers who viewed an item and then purchased it within an hour:");
        for (Map<String, Object> row : sequenceResult.getRows()) {
            System.out.println("  " + row.get("name") + " (ID: " + row.get("userId") + ")");
        }
    }
    
    /**
     * Demonstrate persistence.
     */
    private static void demonstratePersistence(UserProfileStore userProfileStore, EventStore eventStore) throws IOException {
        System.out.println("\n=== Demonstrating Persistence ===");
        
        // Create temporary directory
        File tempDir = File.createTempFile("tesseractdb-example", "");
        tempDir.delete();
        tempDir.mkdir();
        tempDir.deleteOnExit();
        
        System.out.println("Using temporary directory: " + tempDir.getAbsolutePath());
        
        // Create persistence manager
        PersistenceManager persistenceManager = new PersistenceManager(
                tempDir.getAbsolutePath(), userProfileStore, eventStore, 60000);
        
        // Save all data
        persistenceManager.saveAll();
        System.out.println("Saved all data to disk");
        
        // Save events in NCF format
        persistenceManager.saveEventsNCF();
        System.out.println("Saved events in NCF format");
        
        // Check created files
        File profilesDir = new File(tempDir, "profiles");
        File eventsDir = new File(tempDir, "events");
        File ncfDir = new File(tempDir, "ncf");
        
        System.out.println("\nCreated directories:");
        System.out.println("  Profiles directory: " + profilesDir.exists());
        System.out.println("  Events directory: " + eventsDir.exists());
        System.out.println("  NCF directory: " + ncfDir.exists());
        
        // Count files
        int profileCount = profilesDir.listFiles() != null ? profilesDir.listFiles().length : 0;
        int eventCount = eventsDir.listFiles() != null ? eventsDir.listFiles().length : 0;
        int ncfCount = ncfDir.listFiles() != null ? ncfDir.listFiles().length : 0;
        
        System.out.println("\nFile counts:");
        System.out.println("  Profile files: " + profileCount);
        System.out.println("  Event files: " + eventCount);
        System.out.println("  NCF files: " + ncfCount);
        
        // Apply retention policy
        persistenceManager.applyRetentionPolicy(30);
        System.out.println("\nApplied retention policy (30 days)");
        
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
                userProfileStore, eventStore, "purchase", "price");
        
        System.out.println("\nPerforming RFM Analysis...");
        Map<String, RFMAnalysis.RFMScore> rfmScores = rfmAnalysis.calculateRFMScores(30, 5);
        
        System.out.println("RFM Scores:");
        for (Map.Entry<String, RFMAnalysis.RFMScore> entry : rfmScores.entrySet()) {
            RFMAnalysis.RFMScore score = entry.getValue();
            System.out.println("  " + entry.getKey() + ": R=" + score.getRecencyScore() + 
                    ", F=" + score.getFrequencyScore() + ", M=" + score.getMonetaryScore());
        }
        
        // Get high-value customers
        List<String> highValueCustomers = rfmAnalysis.getHighValueCustomers(rfmScores, 5);
        System.out.println("\nHigh-value customers: " + highValueCustomers);
        
        // Get at-risk customers
        List<String> atRiskCustomers = rfmAnalysis.getAtRiskCustomers(rfmScores, 5);
        System.out.println("At-risk customers: " + atRiskCustomers);
        
        // Get new customers
        List<String> newCustomers = rfmAnalysis.getNewCustomers(rfmScores, 5);
        System.out.println("New customers: " + newCustomers);
        
        // Cohort Analysis
        CohortAnalysis cohortAnalysis = new CohortAnalysis(userProfileStore, eventStore);
        
        System.out.println("\nPerforming Cohort Analysis...");
        CohortAnalysis.CohortResult cohortResult = cohortAnalysis.calculateRetentionCohorts(
                CohortAnalysis.TimePeriod.WEEK, 4, "login");
        
        System.out.println("Weekly Cohort Analysis (login event):");
        System.out.println("  Cohort sizes:");
        for (int i = 0; i < cohortResult.getNumPeriods(); i++) {
            System.out.println("    Cohort " + i + ": " + cohortResult.getCohortSize(i));
        }
        
        System.out.println("  Retention percentages:");
        double[][] retentionPercentages = cohortResult.getRetentionPercentages();
        for (int i = 0; i < retentionPercentages.length; i++) {
            System.out.print("    Cohort " + i + ":");
            for (int j = 0; j < retentionPercentages[i].length; j++) {
                if (j < cohortResult.getNumPeriods() - i) {
                    System.out.printf(" %.2f", retentionPercentages[i][j] * 100);
                }
            }
            System.out.println("%");
        }
    }
    
    /**
     * Demonstrate ML.
     */
    private static void demonstrateML(UserProfileStore userProfileStore, EventStore eventStore) {
        System.out.println("\n=== Demonstrating ML ===");
        
        // Recommendation Engine
        RecommendationEngine recommendationEngine = new RecommendationEngine(
                userProfileStore, eventStore, "view_item", "purchase", "item_id");
        
        System.out.println("\nGenerating Recommendations...");
        
        // Get recommendations for user5
        List<RecommendationEngine.ScoredItem> recommendations = 
                recommendationEngine.getRecommendations("user5", 5);
        
        System.out.println("Recommendations for user5:");
        for (RecommendationEngine.ScoredItem item : recommendations) {
            System.out.printf("  %s (score: %.2f)%n", item.getItemId(), item.getScore());
        }
        
        // Get popular items
        List<RecommendationEngine.ScoredItem> popularItems = 
                recommendationEngine.getPopularItems(5);
        
        System.out.println("\nPopular items:");
        for (RecommendationEngine.ScoredItem item : popularItems) {
            System.out.printf("  %s (score: %.2f)%n", item.getItemId(), item.getScore());
        }
        
        // Predictive Model
        PredictiveModel predictiveModel = new PredictiveModel(userProfileStore, eventStore);
        
        System.out.println("\nGenerating Predictions...");
        
        // Predict purchase likelihood for different users
        String[] userIds = {"user1", "user5", "user10"};
        for (String userId : userIds) {
            double likelihood = predictiveModel.predictEventLikelihood(userId, "purchase");
            System.out.printf("  Likelihood of %s performing purchase: %.2f%%%n", 
                    userId, likelihood * 100);
        }
        
        // Predict best time to send notifications
        for (String userId : userIds) {
            int bestHour = predictiveModel.predictBestTimeToSend(userId);
            if (bestHour >= 0) {
                System.out.printf("  Best hour to send notification to %s: %d:00%n", 
                        userId, bestHour);
            } else {
                System.out.printf("  Unable to determine best time for %s%n", userId);
            }
        }
    }
}
