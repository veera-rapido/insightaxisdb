package com.insightaxisdb.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightaxisdb.core.Config;
import com.insightaxisdb.ml.PredictiveModel;
import com.insightaxisdb.ml.RecommendationEngine;
import com.insightaxisdb.query.Query;
import com.insightaxisdb.query.QueryCondition;
import com.insightaxisdb.query.QueryEngine;
import com.insightaxisdb.query.QueryResult;
import com.insightaxisdb.segmentation.CohortAnalysis;
import com.insightaxisdb.segmentation.RFMAnalysis;
import com.insightaxisdb.storage.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.util.*;

import static spark.Spark.*;

/**
 * REST API server for InsightAxisDB.
 */
public class InsightAxisDBServer {
    
    private static final Logger logger = LoggerFactory.getLogger(InsightAxisDBServer.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final UserProfileStore userProfileStore;
    private final EventStore eventStore;
    private final QueryEngine queryEngine;
    private final PersistenceManager persistenceManager;
    private final RFMAnalysis rfmAnalysis;
    private final CohortAnalysis cohortAnalysis;
    private final RecommendationEngine recommendationEngine;
    private final PredictiveModel predictiveModel;
    
    /**
     * Create a new InsightAxisDBServer with default configuration.
     */
    public InsightAxisDBServer() {
        this.userProfileStore = new UserProfileStore();
        this.eventStore = new EventStore(userProfileStore);
        this.queryEngine = new QueryEngine(userProfileStore, eventStore);
        this.persistenceManager = new PersistenceManager(
                Config.getString("dataDirectory", "data"),
                userProfileStore,
                eventStore,
                Config.getLong("saveIntervalMillis", 60000)
        );
        this.rfmAnalysis = new RFMAnalysis(userProfileStore, eventStore, "purchase", "price");
        this.cohortAnalysis = new CohortAnalysis(userProfileStore, eventStore);
        this.recommendationEngine = new RecommendationEngine(
                userProfileStore, eventStore, "view_item", "purchase", "item_id");
        this.predictiveModel = new PredictiveModel(userProfileStore, eventStore);
    }
    
    /**
     * Start the server.
     */
    public void start() {
        // Configure Spark
        port(8080);
        
        // Serve static files from the public directory
        staticFiles.location("/public");
        
        // Enable CORS
        options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }
            
            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }
            
            return "OK";
        });
        
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Content-Length, Accept, Origin");
            
            // Set content type to JSON only for API requests
            if (request.pathInfo().startsWith("/api/")) {
                response.type("application/json");
            }
        });
        
        // User endpoints
        get("/api/users", this::getAllUsers);
        get("/api/users/:userId", this::getUser);
        post("/api/users", this::createUser);
        put("/api/users/:userId", this::updateUser);
        delete("/api/users/:userId", this::deleteUser);
        
        // Event endpoints
        get("/api/events", this::getAllEvents);
        get("/api/events/:eventId", this::getEvent);
        post("/api/events", this::createEvent);
        get("/api/users/:userId/events", this::getUserEvents);
        
        // Query endpoints
        post("/api/query/users", this::queryUsers);
        post("/api/query/events", this::queryEvents);
        post("/api/query/users/:userId/events", this::queryUserEvents);
        
        // Segmentation endpoints
        get("/api/segmentation/rfm", this::getRFMSegmentation);
        get("/api/segmentation/cohorts", this::getCohortAnalysis);
        
        // ML endpoints
        get("/api/ml/recommendations/:userId", this::getRecommendations);
        get("/api/ml/recommendations/popular", this::getPopularItems);
        get("/api/ml/predictions/:userId/:eventName", this::getPrediction);
        get("/api/ml/best-time/:userId", this::getBestTimeToSend);
        
        // System endpoints
        get("/api/system/config", this::getConfig);
        post("/api/system/save", this::saveData);
        post("/api/system/load", this::loadData);
        
        // Error handling
        exception(Exception.class, (exception, request, response) -> {
            logger.error("Error processing request", exception);
            response.status(500);
            response.body(toJson(Map.of(
                    "error", true,
                    "message", exception.getMessage(),
                    "status", 500,
                    "timestamp", System.currentTimeMillis()
            )));
        });
        
        // Start persistence manager
        persistenceManager.start();
        
        logger.info("InsightAxisDB server started on port 8080");
    }
    
    /**
     * Stop the server.
     */
    public void stop() {
        persistenceManager.shutdown();
        spark.Spark.stop();
        logger.info("InsightAxisDB server stopped");
    }
    
    /**
     * Get all users.
     */
    private Object getAllUsers(Request request, Response response) {
        List<UserProfile> profiles = userProfileStore.getAllProfiles();
        return profiles;
    }
    
    /**
     * Get a user by ID.
     */
    private Object getUser(Request request, Response response) {
        String userId = request.params(":userId");
        UserProfile profile = userProfileStore.getProfile(userId);
        
        if (profile == null) {
            response.status(404);
            return Map.of(
                    "error", true,
                    "message", "User not found: " + userId,
                    "status", 404,
                    "timestamp", System.currentTimeMillis()
            );
        }
        
        return profile;
    }
    
    /**
     * Create a new user.
     */
    private Object createUser(Request request, Response response) {
        try {
            Map<String, Object> requestBody = fromJson(request.body(), Map.class);
            
            String userId = (String) requestBody.get("userId");
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) requestBody.get("properties");
            
            if (userId == null || properties == null) {
                response.status(400);
                return Map.of(
                        "error", true,
                        "message", "Missing required fields: userId and properties",
                        "status", 400,
                        "timestamp", System.currentTimeMillis()
                );
            }
            
            UserProfile profile = userProfileStore.createProfile(userId, properties);
            return profile;
        } catch (Exception e) {
            response.status(400);
            return Map.of(
                    "error", true,
                    "message", e.getMessage(),
                    "status", 400,
                    "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * Update a user.
     */
    private Object updateUser(Request request, Response response) {
        try {
            String userId = request.params(":userId");
            Map<String, Object> requestBody = fromJson(request.body(), Map.class);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) requestBody.get("properties");
            
            if (properties == null) {
                response.status(400);
                return Map.of(
                        "error", true,
                        "message", "Missing required field: properties",
                        "status", 400,
                        "timestamp", System.currentTimeMillis()
                );
            }
            
            UserProfile profile = userProfileStore.updateProfile(userId, properties);
            
            if (profile == null) {
                response.status(404);
                return Map.of(
                        "error", true,
                        "message", "User not found: " + userId,
                        "status", 404,
                        "timestamp", System.currentTimeMillis()
                );
            }
            
            return profile;
        } catch (Exception e) {
            response.status(400);
            return Map.of(
                    "error", true,
                    "message", e.getMessage(),
                    "status", 400,
                    "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * Delete a user.
     */
    private Object deleteUser(Request request, Response response) {
        String userId = request.params(":userId");
        boolean deleted = userProfileStore.deleteProfile(userId);
        
        if (!deleted) {
            response.status(404);
            return Map.of(
                    "error", true,
                    "message", "User not found: " + userId,
                    "status", 404,
                    "timestamp", System.currentTimeMillis()
            );
        }
        
        return Map.of(
                "success", true,
                "message", "User deleted successfully"
        );
    }
    
    /**
     * Get all events.
     */
    private Object getAllEvents(Request request, Response response) {
        List<Event> events = eventStore.getAllEvents();
        return events;
    }
    
    /**
     * Get an event by ID.
     */
    private Object getEvent(Request request, Response response) {
        String eventId = request.params(":eventId");
        Event event = eventStore.getEvent(eventId);
        
        if (event == null) {
            response.status(404);
            return Map.of(
                    "error", true,
                    "message", "Event not found: " + eventId,
                    "status", 404,
                    "timestamp", System.currentTimeMillis()
            );
        }
        
        return event;
    }
    
    /**
     * Create a new event.
     */
    private Object createEvent(Request request, Response response) {
        try {
            Map<String, Object> requestBody = fromJson(request.body(), Map.class);
            
            String eventName = (String) requestBody.get("eventName");
            String userId = (String) requestBody.get("userId");
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) requestBody.get("properties");
            Long timestamp = (Long) requestBody.get("timestamp");
            
            if (eventName == null || userId == null) {
                response.status(400);
                return Map.of(
                        "error", true,
                        "message", "Missing required fields: eventName and userId",
                        "status", 400,
                        "timestamp", System.currentTimeMillis()
                );
            }
            
            if (properties == null) {
                properties = new HashMap<>();
            }
            
            Event event = eventStore.addEvent(eventName, userId, properties, timestamp);
            return event;
        } catch (Exception e) {
            response.status(400);
            return Map.of(
                    "error", true,
                    "message", e.getMessage(),
                    "status", 400,
                    "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * Get events for a user.
     */
    private Object getUserEvents(Request request, Response response) {
        String userId = request.params(":userId");
        List<Event> events = eventStore.getUserEvents(userId);
        return events;
    }
    
    /**
     * Query users.
     */
    private Object queryUsers(Request request, Response response) {
        try {
            Map<String, Object> requestBody = fromJson(request.body(), Map.class);
            Query query = buildQuery(requestBody);
            QueryResult result = queryEngine.queryUserProfiles(query);
            return result;
        } catch (Exception e) {
            response.status(400);
            return Map.of(
                    "error", true,
                    "message", e.getMessage(),
                    "status", 400,
                    "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * Query events.
     */
    private Object queryEvents(Request request, Response response) {
        try {
            Map<String, Object> requestBody = fromJson(request.body(), Map.class);
            Query query = buildQuery(requestBody);
            QueryResult result = queryEngine.queryEvents(query);
            return result;
        } catch (Exception e) {
            response.status(400);
            return Map.of(
                    "error", true,
                    "message", e.getMessage(),
                    "status", 400,
                    "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * Query events for a user.
     */
    private Object queryUserEvents(Request request, Response response) {
        try {
            String userId = request.params(":userId");
            Map<String, Object> requestBody = fromJson(request.body(), Map.class);
            Query query = buildQuery(requestBody);
            QueryResult result = queryEngine.queryUserEvents(userId, query);
            return result;
        } catch (Exception e) {
            response.status(400);
            return Map.of(
                    "error", true,
                    "message", e.getMessage(),
                    "status", 400,
                    "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * Build a query from a request body.
     */
    private Query buildQuery(Map<String, Object> requestBody) {
        Query query = new Query();
        
        // Add conditions
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) requestBody.get("where");
        if (conditions != null) {
            for (Map<String, Object> condition : conditions) {
                String field = (String) condition.get("field");
                String operator = (String) condition.get("operator");
                Object value = condition.get("value");
                
                if (field == null || operator == null) {
                    continue;
                }
                
                QueryCondition queryCondition = null;
                
                switch (operator.toUpperCase()) {
                    case "EQ":
                        queryCondition = QueryCondition.eq(field, value);
                        break;
                    case "NE":
                        queryCondition = QueryCondition.ne(field, value);
                        break;
                    case "GT":
                        queryCondition = QueryCondition.gt(field, value);
                        break;
                    case "GTE":
                        queryCondition = QueryCondition.gte(field, value);
                        break;
                    case "LT":
                        queryCondition = QueryCondition.lt(field, value);
                        break;
                    case "LTE":
                        queryCondition = QueryCondition.lte(field, value);
                        break;
                    case "CONTAINS":
                        queryCondition = QueryCondition.contains(field, value);
                        break;
                    case "STARTS_WITH":
                        queryCondition = QueryCondition.startsWith(field, value);
                        break;
                    case "ENDS_WITH":
                        queryCondition = QueryCondition.endsWith(field, value);
                        break;
                    case "IN":
                        @SuppressWarnings("unchecked")
                        List<Object> values = (List<Object>) value;
                        queryCondition = QueryCondition.in(field, values);
                        break;
                }
                
                if (queryCondition != null) {
                    query.where(queryCondition);
                }
            }
        }
        
        // Add select fields
        @SuppressWarnings("unchecked")
        List<String> select = (List<String>) requestBody.get("select");
        if (select != null) {
            query.select(select.toArray(new String[0]));
        }
        
        // Add order by
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> orderBy = (List<Map<String, Object>>) requestBody.get("orderBy");
        if (orderBy != null) {
            for (Map<String, Object> order : orderBy) {
                String field = (String) order.get("field");
                String direction = (String) order.get("order");
                
                if (field == null) {
                    continue;
                }
                
                Query.SortOrder sortOrder = Query.SortOrder.ASCENDING;
                if (direction != null && direction.equalsIgnoreCase("DESC")) {
                    sortOrder = Query.SortOrder.DESCENDING;
                }
                
                query.orderBy(field, sortOrder);
            }
        }
        
        // Add limit and offset
        Integer limit = (Integer) requestBody.get("limit");
        if (limit != null) {
            query.limit(limit);
        }
        
        Integer offset = (Integer) requestBody.get("offset");
        if (offset != null) {
            query.offset(offset);
        }
        
        // Add aggregations
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> aggregations = (List<Map<String, Object>>) requestBody.get("aggregate");
        if (aggregations != null) {
            for (Map<String, Object> aggregation : aggregations) {
                String field = (String) aggregation.get("field");
                String type = (String) aggregation.get("type");
                String alias = (String) aggregation.get("alias");
                
                if (field == null || type == null || alias == null) {
                    continue;
                }
                
                Query.AggregationType aggregationType = null;
                
                switch (type.toUpperCase()) {
                    case "SUM":
                        aggregationType = Query.AggregationType.SUM;
                        break;
                    case "AVG":
                        aggregationType = Query.AggregationType.AVG;
                        break;
                    case "MIN":
                        aggregationType = Query.AggregationType.MIN;
                        break;
                    case "MAX":
                        aggregationType = Query.AggregationType.MAX;
                        break;
                    case "COUNT":
                        aggregationType = Query.AggregationType.COUNT;
                        break;
                }
                
                if (aggregationType != null) {
                    query.aggregate(field, aggregationType, alias);
                }
            }
        }
        
        return query;
    }
    
    /**
     * Get RFM segmentation.
     */
    private Object getRFMSegmentation(Request request, Response response) {
        try {
            int recencyDays = Integer.parseInt(request.queryParamOrDefault("recencyDays", "30"));
            int numSegments = Integer.parseInt(request.queryParamOrDefault("numSegments", "5"));
            
            Map<String, RFMAnalysis.RFMScore> scores = rfmAnalysis.calculateRFMScores(recencyDays, numSegments);
            return scores;
        } catch (Exception e) {
            response.status(400);
            return Map.of(
                    "error", true,
                    "message", e.getMessage(),
                    "status", 400,
                    "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * Get cohort analysis.
     */
    private Object getCohortAnalysis(Request request, Response response) {
        try {
            String timePeriodStr = request.queryParamOrDefault("timePeriod", "WEEK");
            int numPeriods = Integer.parseInt(request.queryParamOrDefault("numPeriods", "4"));
            String targetEventName = request.queryParamOrDefault("targetEventName", "login");
            
            CohortAnalysis.TimePeriod timePeriod = CohortAnalysis.TimePeriod.valueOf(timePeriodStr);
            CohortAnalysis.CohortResult result = cohortAnalysis.calculateRetentionCohorts(timePeriod, numPeriods, targetEventName);
            
            return result;
        } catch (Exception e) {
            response.status(400);
            return Map.of(
                    "error", true,
                    "message", e.getMessage(),
                    "status", 400,
                    "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * Get recommendations for a user.
     */
    private Object getRecommendations(Request request, Response response) {
        try {
            String userId = request.params(":userId");
            int maxRecommendations = Integer.parseInt(request.queryParamOrDefault("max", "5"));
            
            List<RecommendationEngine.ScoredItem> recommendations = recommendationEngine.getRecommendations(userId, maxRecommendations);
            return recommendations;
        } catch (Exception e) {
            response.status(400);
            return Map.of(
                    "error", true,
                    "message", e.getMessage(),
                    "status", 400,
                    "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * Get popular items.
     */
    private Object getPopularItems(Request request, Response response) {
        try {
            int maxItems = Integer.parseInt(request.queryParamOrDefault("max", "5"));
            
            List<RecommendationEngine.ScoredItem> popularItems = recommendationEngine.getPopularItems(maxItems);
            return popularItems;
        } catch (Exception e) {
            response.status(400);
            return Map.of(
                    "error", true,
                    "message", e.getMessage(),
                    "status", 400,
                    "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * Get prediction for a user and event.
     */
    private Object getPrediction(Request request, Response response) {
        try {
            String userId = request.params(":userId");
            String eventName = request.params(":eventName");
            
            double likelihood = predictiveModel.predictEventLikelihood(userId, eventName);
            
            return Map.of(
                    "userId", userId,
                    "eventName", eventName,
                    "likelihood", likelihood
            );
        } catch (Exception e) {
            response.status(400);
            return Map.of(
                    "error", true,
                    "message", e.getMessage(),
                    "status", 400,
                    "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * Get best time to send for a user.
     */
    private Object getBestTimeToSend(Request request, Response response) {
        try {
            String userId = request.params(":userId");
            
            int bestHour = predictiveModel.predictBestTimeToSend(userId);
            
            return Map.of(
                    "userId", userId,
                    "bestHour", bestHour
            );
        } catch (Exception e) {
            response.status(400);
            return Map.of(
                    "error", true,
                    "message", e.getMessage(),
                    "status", 400,
                    "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * Get system configuration.
     */
    private Object getConfig(Request request, Response response) {
        return Config.getConfig();
    }
    
    /**
     * Save data to disk.
     */
    private Object saveData(Request request, Response response) {
        try {
            persistenceManager.saveAll();
            
            return Map.of(
                    "success", true,
                    "message", "Data saved successfully"
            );
        } catch (Exception e) {
            response.status(500);
            return Map.of(
                    "error", true,
                    "message", e.getMessage(),
                    "status", 500,
                    "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * Load data from disk.
     */
    private Object loadData(Request request, Response response) {
        try {
            persistenceManager.loadAll();
            
            return Map.of(
                    "success", true,
                    "message", "Data loaded successfully"
            );
        } catch (Exception e) {
            response.status(500);
            return Map.of(
                    "error", true,
                    "message", e.getMessage(),
                    "status", 500,
                    "timestamp", System.currentTimeMillis()
            );
        }
    }
    
    /**
     * Convert an object to JSON.
     */
    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            logger.error("Error converting object to JSON", e);
            return "{}";
        }
    }
    
    /**
     * Convert JSON to an object.
     */
    private <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            logger.error("Error converting JSON to object", e);
            throw new RuntimeException("Invalid JSON: " + e.getMessage());
        }
    }
    
    /**
     * Main method to start the server.
     */
    public static void main(String[] args) {
        InsightAxisDBServer server = new InsightAxisDBServer();
        server.start();
    }
}
