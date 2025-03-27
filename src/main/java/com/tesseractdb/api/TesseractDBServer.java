package com.tesseractdb.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tesseractdb.core.Config;
import com.tesseractdb.ml.PredictiveModel;
import com.tesseractdb.ml.RecommendationEngine;
import com.tesseractdb.query.Query;
import com.tesseractdb.query.QueryCondition;
import com.tesseractdb.query.QueryEngine;
import com.tesseractdb.query.QueryResult;
import com.tesseractdb.segmentation.CohortAnalysis;
import com.tesseractdb.segmentation.RFMAnalysis;
import com.tesseractdb.storage.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.util.*;

import static spark.Spark.*;

/**
 * REST API server for TesseractDB.
 */
public class TesseractDBServer {

    private static final Logger logger = LoggerFactory.getLogger(TesseractDBServer.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static UserProfileStore userProfileStore;
    private static EventStore eventStore;
    private static QueryEngine queryEngine;
    private static PersistenceManager persistenceManager;
    private static RecommendationEngine recommendationEngine;
    private static PredictiveModel predictiveModel;
    private static RFMAnalysis rfmAnalysis;
    private static CohortAnalysis cohortAnalysis;

    public static void main(String[] args) {
        // Initialize components
        initializeComponents();

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

        // Define routes

        // User profile routes
        get("/api/users", TesseractDBServer::getAllUsers);
        get("/api/users/:userId", TesseractDBServer::getUser);
        post("/api/users", TesseractDBServer::createUser);
        put("/api/users/:userId", TesseractDBServer::updateUser);
        delete("/api/users/:userId", TesseractDBServer::deleteUser);

        // Event routes
        get("/api/events", TesseractDBServer::getAllEvents);
        get("/api/events/:eventId", TesseractDBServer::getEvent);
        post("/api/events", TesseractDBServer::createEvent);
        get("/api/users/:userId/events", TesseractDBServer::getUserEvents);

        // Query routes
        post("/api/query/users", TesseractDBServer::queryUsers);
        post("/api/query/events", TesseractDBServer::queryEvents);
        post("/api/query/users/:userId/events", TesseractDBServer::queryUserEvents);

        // Segmentation routes
        get("/api/segmentation/rfm", TesseractDBServer::getRFMSegmentation);
        get("/api/segmentation/cohorts", TesseractDBServer::getCohortAnalysis);

        // ML routes
        get("/api/ml/recommendations/:userId", TesseractDBServer::getRecommendations);
        get("/api/ml/predictions/:userId/:eventName", TesseractDBServer::getPrediction);
        get("/api/ml/best-time/:userId", TesseractDBServer::getBestTimeToSend);

        // System routes
        get("/api/system/config", TesseractDBServer::getConfig);
        post("/api/system/save", TesseractDBServer::saveData);
        post("/api/system/load", TesseractDBServer::loadData);

        // Add shutdown hook to save data
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down TesseractDB server");
            persistenceManager.shutdown();
        }));

        logger.info("TesseractDB server started on port 8080");
    }

    /**
     * Initialize all components.
     */
    private static void initializeComponents() {
        // Initialize stores
        userProfileStore = new UserProfileStore();
        eventStore = new EventStore(userProfileStore);

        // Initialize query engine
        queryEngine = new QueryEngine(userProfileStore, eventStore);

        // Initialize persistence manager
        persistenceManager = new PersistenceManager("data", userProfileStore, eventStore, 60000);

        // Initialize ML components
        recommendationEngine = new RecommendationEngine(userProfileStore, eventStore,
                "view_item", "purchase_item", "item_id");
        predictiveModel = new PredictiveModel(userProfileStore, eventStore);

        // Initialize segmentation components
        rfmAnalysis = new RFMAnalysis(userProfileStore, eventStore, "purchase_item", "price");
        cohortAnalysis = new CohortAnalysis(userProfileStore, eventStore);

        // Load data
        try {
            persistenceManager.loadAll();
        } catch (Exception e) {
            logger.error("Failed to load data", e);
        }
    }

    /**
     * Get all users.
     */
    private static Object getAllUsers(Request request, Response response) {
        try {
            // In a real implementation, we would need a way to get all profiles from the store
            // For now, we'll just return an empty list
            return objectMapper.writeValueAsString(Collections.emptyList());
        } catch (Exception e) {
            logger.error("Failed to get all users", e);
            response.status(500);
            return "{\"error\": \"Internal server error\"}";
        }
    }

    /**
     * Get a user.
     */
    private static Object getUser(Request request, Response response) {
        try {
            String userId = request.params(":userId");
            UserProfile profile = userProfileStore.getProfile(userId);

            if (profile == null) {
                response.status(404);
                return "{\"error\": \"User not found\"}";
            }

            return objectMapper.writeValueAsString(profile.toMap());
        } catch (Exception e) {
            logger.error("Failed to get user", e);
            response.status(500);
            return "{\"error\": \"Internal server error\"}";
        }
    }

    /**
     * Create a user.
     */
    private static Object createUser(Request request, Response response) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> userData = objectMapper.readValue(request.body(), Map.class);

            String userId = (String) userData.get("userId");
            if (userId == null) {
                response.status(400);
                return "{\"error\": \"User ID is required\"}";
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) userData.get("properties");

            UserProfile profile = userProfileStore.createProfile(userId, properties);

            response.status(201);
            return objectMapper.writeValueAsString(profile.toMap());
        } catch (IllegalArgumentException e) {
            response.status(400);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        } catch (Exception e) {
            logger.error("Failed to create user", e);
            response.status(500);
            return "{\"error\": \"Internal server error\"}";
        }
    }

    /**
     * Update a user.
     */
    private static Object updateUser(Request request, Response response) {
        try {
            String userId = request.params(":userId");

            @SuppressWarnings("unchecked")
            Map<String, Object> userData = objectMapper.readValue(request.body(), Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) userData.get("properties");

            UserProfile profile = userProfileStore.updateProfile(userId, properties);

            if (profile == null) {
                response.status(404);
                return "{\"error\": \"User not found\"}";
            }

            return objectMapper.writeValueAsString(profile.toMap());
        } catch (Exception e) {
            logger.error("Failed to update user", e);
            response.status(500);
            return "{\"error\": \"Internal server error\"}";
        }
    }

    /**
     * Delete a user.
     */
    private static Object deleteUser(Request request, Response response) {
        try {
            String userId = request.params(":userId");
            boolean deleted = userProfileStore.deleteProfile(userId);

            if (!deleted) {
                response.status(404);
                return "{\"error\": \"User not found\"}";
            }

            return "{\"success\": true}";
        } catch (Exception e) {
            logger.error("Failed to delete user", e);
            response.status(500);
            return "{\"error\": \"Internal server error\"}";
        }
    }

    /**
     * Get all events.
     */
    private static Object getAllEvents(Request request, Response response) {
        try {
            // In a real implementation, we would need a way to get all events from the store
            // For now, we'll just return an empty list
            return objectMapper.writeValueAsString(Collections.emptyList());
        } catch (Exception e) {
            logger.error("Failed to get all events", e);
            response.status(500);
            return "{\"error\": \"Internal server error\"}";
        }
    }

    /**
     * Get an event.
     */
    private static Object getEvent(Request request, Response response) {
        try {
            String eventId = request.params(":eventId");
            Event event = eventStore.getEvent(eventId);

            if (event == null) {
                response.status(404);
                return "{\"error\": \"Event not found\"}";
            }

            return objectMapper.writeValueAsString(event.toMap());
        } catch (Exception e) {
            logger.error("Failed to get event", e);
            response.status(500);
            return "{\"error\": \"Internal server error\"}";
        }
    }

    /**
     * Create an event.
     */
    private static Object createEvent(Request request, Response response) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> eventData = objectMapper.readValue(request.body(), Map.class);

            String eventName = (String) eventData.get("eventName");
            if (eventName == null) {
                response.status(400);
                return "{\"error\": \"Event name is required\"}";
            }

            String userId = (String) eventData.get("userId");
            if (userId == null) {
                response.status(400);
                return "{\"error\": \"User ID is required\"}";
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) eventData.get("properties");

            Long timestamp = null;
            if (eventData.containsKey("timestamp")) {
                timestamp = ((Number) eventData.get("timestamp")).longValue();
            }

            Event event = eventStore.addEvent(eventName, userId, properties, timestamp);

            response.status(201);
            return objectMapper.writeValueAsString(event.toMap());
        } catch (Exception e) {
            logger.error("Failed to create event", e);
            response.status(500);
            return "{\"error\": \"Internal server error\"}";
        }
    }

    /**
     * Get events for a user.
     */
    private static Object getUserEvents(Request request, Response response) {
        try {
            String userId = request.params(":userId");
            List<Event> events = eventStore.getUserEvents(userId);

            List<Map<String, Object>> eventMaps = new ArrayList<>();
            for (Event event : events) {
                eventMaps.add(event.toMap());
            }

            return objectMapper.writeValueAsString(eventMaps);
        } catch (Exception e) {
            logger.error("Failed to get user events", e);
            response.status(500);
            return "{\"error\": \"Internal server error\"}";
        }
    }

    /**
     * Query users.
     */
    private static Object queryUsers(Request request, Response response) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> queryData = objectMapper.readValue(request.body(), Map.class);

            Query query = parseQuery(queryData);
            QueryResult result = queryEngine.queryUserProfiles(query);

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            logger.error("Failed to query users", e);
            response.status(500);
            return "{\"error\": \"Internal server error\"}";
        }
    }

    /**
     * Query events.
     */
    private static Object queryEvents(Request request, Response response) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> queryData = objectMapper.readValue(request.body(), Map.class);

            Query query = parseQuery(queryData);
            QueryResult result = queryEngine.queryEvents(query);

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            logger.error("Failed to query events", e);
            response.status(500);
            return "{\"error\": \"Internal server error\"}";
        }
    }

    /**
     * Query events for a user.
     */
    private static Object queryUserEvents(Request request, Response response) {
        try {
            String userId = request.params(":userId");

            @SuppressWarnings("unchecked")
            Map<String, Object> queryData = objectMapper.readValue(request.body(), Map.class);

            Query query = parseQuery(queryData);
            QueryResult result = queryEngine.queryUserEvents(userId, query);

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            logger.error("Failed to query user events", e);
            response.status(500);
            return "{\"error\": \"Internal server error\"}";
        }
    }

    /**
     * Parse a query from JSON data.
     */
    private static Query parseQuery(Map<String, Object> queryData) {
        Query query = new Query();

        // Parse conditions
        if (queryData.containsKey("conditions")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> conditions = (List<Map<String, Object>>) queryData.get("conditions");

            for (Map<String, Object> condition : conditions) {
                String field = (String) condition.get("field");
                String operator = (String) condition.get("operator");
                Object value = condition.get("value");

                QueryCondition queryCondition = createQueryCondition(field, operator, value);
                query.where(queryCondition);
            }
        }

        // Parse select fields
        if (queryData.containsKey("select")) {
            @SuppressWarnings("unchecked")
            List<String> select = (List<String>) queryData.get("select");
            query.select(select.toArray(new String[0]));
        }

        // Parse sort fields
        if (queryData.containsKey("sort")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sort = (List<Map<String, Object>>) queryData.get("sort");

            for (Map<String, Object> sortField : sort) {
                String field = (String) sortField.get("field");
                String order = (String) sortField.get("order");

                Query.SortOrder sortOrder = "desc".equalsIgnoreCase(order) ?
                        Query.SortOrder.DESCENDING : Query.SortOrder.ASCENDING;

                query.orderBy(field, sortOrder);
            }
        }

        // Parse limit and offset
        if (queryData.containsKey("limit")) {
            int limit = ((Number) queryData.get("limit")).intValue();
            query.limit(limit);
        }

        if (queryData.containsKey("offset")) {
            int offset = ((Number) queryData.get("offset")).intValue();
            query.offset(offset);
        }

        // Parse aggregations
        if (queryData.containsKey("aggregations")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> aggregations = (List<Map<String, Object>>) queryData.get("aggregations");

            for (Map<String, Object> aggregation : aggregations) {
                String field = (String) aggregation.get("field");
                String type = (String) aggregation.get("type");
                String alias = (String) aggregation.get("alias");

                Query.AggregationType aggregationType = Query.AggregationType.valueOf(type.toUpperCase());
                query.aggregate(field, aggregationType, alias);
            }
        }

        return query;
    }

    /**
     * Create a query condition from field, operator, and value.
     */
    private static QueryCondition createQueryCondition(String field, String operator, Object value) {
        switch (operator.toLowerCase()) {
            case "eq":
                return QueryCondition.eq(field, value);
            case "ne":
                return QueryCondition.ne(field, value);
            case "gt":
                return QueryCondition.gt(field, value);
            case "gte":
                return QueryCondition.gte(field, value);
            case "lt":
                return QueryCondition.lt(field, value);
            case "lte":
                return QueryCondition.lte(field, value);
            case "contains":
                return QueryCondition.contains(field, (String) value);
            case "startswith":
                return QueryCondition.startsWith(field, (String) value);
            case "endswith":
                return QueryCondition.endsWith(field, (String) value);
            case "in":
                @SuppressWarnings("unchecked")
                List<Object> values = (List<Object>) value;
                return QueryCondition.in(field, values);
            case "notin":
                @SuppressWarnings("unchecked")
                List<Object> notValues = (List<Object>) value;
                return QueryCondition.notIn(field, notValues);
            case "exists":
                return QueryCondition.exists(field);
            case "notexists":
                return QueryCondition.notExists(field);
            default:
                throw new IllegalArgumentException("Unknown operator: " + operator);
        }
    }

    /**
     * Get RFM segmentation.
     */
    private static Object getRFMSegmentation(Request request, Response response) {
        try {
            int recencyDays = Integer.parseInt(request.queryParams("recencyDays"));
            int numSegments = Integer.parseInt(request.queryParams("numSegments"));

            Map<String, RFMAnalysis.RFMScore> scores = rfmAnalysis.calculateRFMScores(recencyDays, numSegments);

            // Convert scores to a format suitable for JSON
            Map<String, Object> result = new HashMap<>();

            for (Map.Entry<String, RFMAnalysis.RFMScore> entry : scores.entrySet()) {
                String userId = entry.getKey();
                RFMAnalysis.RFMScore score = entry.getValue();

                Map<String, Object> scoreMap = new HashMap<>();
                scoreMap.put("recency", score.getRecencyScore());
                scoreMap.put("frequency", score.getFrequencyScore());
                scoreMap.put("monetary", score.getMonetaryScore());
                scoreMap.put("combined", score.getCombinedScore());

                result.put(userId, scoreMap);
            }

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            logger.error("Failed to get RFM segmentation", e);
            response.status(500);
            return "{\"error\": \"Internal server error\"}";
        }
    }

    /**
     * Get cohort analysis.
     */
    private static Object getCohortAnalysis(Request request, Response response) {
        try {
            String timePeriodStr = request.queryParams("timePeriod");
            int numPeriods = Integer.parseInt(request.queryParams("numPeriods"));
            String targetEventName = request.queryParams("targetEventName");

            CohortAnalysis.TimePeriod timePeriod = CohortAnalysis.TimePeriod.valueOf(timePeriodStr.toUpperCase());

            CohortAnalysis.CohortResult result = cohortAnalysis.calculateRetentionCohorts(
                    timePeriod, numPeriods, targetEventName);

            // Convert result to a format suitable for JSON
            Map<String, Object> jsonResult = new HashMap<>();
            jsonResult.put("timePeriod", result.getTimePeriod().name());
            jsonResult.put("numPeriods", result.getNumPeriods());

            // Convert retention matrix
            int[][] retentionMatrix = result.getRetentionMatrix();
            jsonResult.put("retentionMatrix", retentionMatrix);

            // Convert retention percentages
            double[][] retentionPercentages = result.getRetentionPercentages();
            jsonResult.put("retentionPercentages", retentionPercentages);

            // Convert cohort sizes
            Map<String, Integer> cohortSizes = new HashMap<>();
            for (int i = 0; i < result.getNumPeriods(); i++) {
                cohortSizes.put(String.valueOf(i), result.getCohortSize(i));
            }
            jsonResult.put("cohortSizes", cohortSizes);

            return objectMapper.writeValueAsString(jsonResult);
        } catch (Exception e) {
            logger.error("Failed to get cohort analysis", e);
            response.status(500);
            return "{\"error\": \"Internal server error\"}";
        }
    }

    /**
     * Get recommendations for a user.
     */
    private static Object getRecommendations(Request request, Response response) {
        try {
            String userId = request.params(":userId");
            int maxRecommendations = Integer.parseInt(request.queryParams("max"));

            List<RecommendationEngine.ScoredItem> recommendations =
                    recommendationEngine.getRecommendations(userId, maxRecommendations);

            // Convert recommendations to a format suitable for JSON
            List<Map<String, Object>> result = new ArrayList<>();

            for (RecommendationEngine.ScoredItem item : recommendations) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("itemId", item.getItemId());
                itemMap.put("score", item.getScore());

                result.add(itemMap);
            }

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            logger.error("Failed to get recommendations", e);
            response.status(500);
            return "{\"error\": \"Internal server error\"}";
        }
    }

    /**
     * Get prediction for a user and event.
     */
    private static Object getPrediction(Request request, Response response) {
        try {
            String userId = request.params(":userId");
            String eventName = request.params(":eventName");

            double likelihood = predictiveModel.predictEventLikelihood(userId, eventName);

            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("eventName", eventName);
            result.put("likelihood", likelihood);

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            logger.error("Failed to get prediction", e);
            response.status(500);
            return "{\"error\": \"Internal server error\"}";
        }
    }

    /**
     * Get best time to send a notification to a user.
     */
    private static Object getBestTimeToSend(Request request, Response response) {
        try {
            String userId = request.params(":userId");

            int bestHour = predictiveModel.predictBestTimeToSend(userId);

            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("bestHour", bestHour);

            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            logger.error("Failed to get best time to send", e);
            response.status(500);
            return "{\"error\": \"Internal server error\"}";
        }
    }

    /**
     * Get system configuration.
     */
    private static Object getConfig(Request request, Response response) {
        try {
            return objectMapper.writeValueAsString(Config.getConfig());
        } catch (Exception e) {
            logger.error("Failed to get config", e);
            response.status(500);
            return "{\"error\": \"Internal server error\"}";
        }
    }

    /**
     * Save data to disk.
     */
    private static Object saveData(Request request, Response response) {
        try {
            persistenceManager.saveAll();
            return "{\"success\": true}";
        } catch (Exception e) {
            logger.error("Failed to save data", e);
            response.status(500);
            return "{\"error\": \"Internal server error\"}";
        }
    }

    /**
     * Load data from disk.
     */
    private static Object loadData(Request request, Response response) {
        try {
            persistenceManager.loadAll();
            return "{\"success\": true}";
        } catch (Exception e) {
            logger.error("Failed to load data", e);
            response.status(500);
            return "{\"error\": \"Internal server error\"}";
        }
    }
}
