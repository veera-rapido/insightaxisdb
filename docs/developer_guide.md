# InsightAxisDB Developer Guide

This guide provides information for developers who want to use InsightAxisDB in their applications or contribute to the project.

## Table of Contents

- [Using InsightAxisDB in Your Application](#using-insightaxisdb-in-your-application)
- [Core Components](#core-components)
- [Advanced Features](#advanced-features)
- [REST API](#rest-api)
- [Extending InsightAxisDB](#extending-insightaxisdb)
- [Best Practices](#best-practices)

## Using InsightAxisDB in Your Application

### Maven Dependency

To use InsightAxisDB in your Maven project, add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.insightaxisdb</groupId>
    <artifactId>insightaxisdb</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Basic Usage

Here's a simple example of how to use InsightAxisDB in your application:

```java
// Create stores
UserProfileStore userProfileStore = new UserProfileStore();
EventStore eventStore = new EventStore(userProfileStore);

// Create a user profile
Map<String, Object> properties = new HashMap<>();
properties.put("name", "John Doe");
properties.put("email", "john@example.com");

UserProfile profile = userProfileStore.createProfile("user1", properties);

// Track an event
Map<String, Object> eventProperties = new HashMap<>();
eventProperties.put("device", "desktop");
eventProperties.put("os", "macOS");

Event event = eventStore.addEvent("login", "user1", eventProperties, System.currentTimeMillis());

// Query users
QueryEngine queryEngine = new QueryEngine(userProfileStore, eventStore);

Query query = new Query()
    .where(QueryCondition.eq("name", "John Doe"))
    .select("userId", "email");

QueryResult result = queryEngine.queryUserProfiles(query);

// Process results
for (Map<String, Object> row : result.getRows()) {
    System.out.println("User ID: " + row.get("userId"));
    System.out.println("Email: " + row.get("email"));
}
```

## Core Components

### NCF (Network Columnar Format)

NCF is the storage format used by TesseractDB. It provides efficient columnar storage for user data.

```java
// Create NCF writer
NCF.Writer writer = new NCF.Writer("lz4");

// Add rows
for (int i = 0; i < 5; i++) {
    Map<String, Object> row = new HashMap<>();
    row.put("id", i);
    row.put("name", "User " + i);
    row.put("active", i % 2 == 0);
    row.put("score", i * 10.5);

    writer.addRow(row);
}

// Write to file
try (RandomAccessFile raf = new RandomAccessFile("data.ncf", "rw")) {
    writer.write(raf);
}

// Read from file
try (RandomAccessFile raf = new RandomAccessFile("data.ncf", "r");
     NCF.Reader reader = new NCF.Reader(raf)) {

    List<Map<String, Object>> rows = reader.readRows(0, null);

    for (Map<String, Object> row : rows) {
        System.out.println(row);
    }
}
```

### UserProfileStore

UserProfileStore manages user profiles and their properties.

```java
// Create user profile store
UserProfileStore userProfileStore = new UserProfileStore();

// Create a user profile
Map<String, Object> properties = new HashMap<>();
properties.put("name", "John Doe");
properties.put("email", "john@example.com");

UserProfile profile = userProfileStore.createProfile("user1", properties);

// Get a user profile
UserProfile retrievedProfile = userProfileStore.getProfile("user1");

// Update a user profile
Map<String, Object> newProperties = new HashMap<>();
newProperties.put("premium", true);

userProfileStore.updateProfile("user1", newProperties);

// Delete a user profile
userProfileStore.deleteProfile("user1");
```

### EventStore

EventStore tracks user events and their properties.

```java
// Create event store
EventStore eventStore = new EventStore(userProfileStore);

// Track an event
Map<String, Object> eventProperties = new HashMap<>();
eventProperties.put("device", "desktop");
eventProperties.put("os", "macOS");

Event event = eventStore.addEvent("login", "user1", eventProperties, System.currentTimeMillis());

// Get events for a user
List<Event> userEvents = eventStore.getUserEvents("user1");

// Get events by name
List<Event> loginEvents = eventStore.getEventsByName("login");

// Get events in a timerange
long now = System.currentTimeMillis();
long oneDayAgo = now - 24 * 60 * 60 * 1000L;

List<Event> recentEvents = eventStore.getUserEventsInTimerange("user1", oneDayAgo, now);
```

### PersistenceManager

PersistenceManager handles saving and loading data to/from disk.

```java
// Create persistence manager
PersistenceManager persistenceManager = new PersistenceManager(
        "data", userProfileStore, eventStore, 60000);

// Save all data
persistenceManager.saveAll();

// Save events in NCF format
persistenceManager.saveEventsNCF();

// Apply retention policy
persistenceManager.applyRetentionPolicy(30);

// Shutdown persistence manager
persistenceManager.shutdown();
```

## Advanced Features

### QueryEngine

QueryEngine provides SQL-like querying capabilities.

```java
// Create query engine
QueryEngine queryEngine = new QueryEngine(userProfileStore, eventStore);

// Query users
Query userQuery = new Query()
    .where(QueryCondition.eq("country", "USA"))
    .where(QueryCondition.gt("age", 25))
    .select("userId", "name", "age")
    .orderBy("age", Query.SortOrder.DESCENDING)
    .limit(10);

QueryResult userResult = queryEngine.queryUserProfiles(userQuery);

// Query events
Query eventQuery = new Query()
    .where(QueryCondition.eq("eventName", "purchase"))
    .where(QueryCondition.gt("price", 50.0))
    .select("eventId", "userId", "item_id", "price")
    .orderBy("price", Query.SortOrder.DESCENDING);

QueryResult eventResult = queryEngine.queryEvents(eventQuery);

// Query with aggregations
Query statsQuery = new Query()
    .where(QueryCondition.eq("eventName", "purchase"))
    .aggregate("price", Query.AggregationType.SUM, "total_spent")
    .aggregate("price", Query.AggregationType.AVG, "avg_price")
    .aggregate("eventId", Query.AggregationType.COUNT, "purchase_count");

QueryResult statsResult = queryEngine.queryUserEvents("user1", statsQuery);
Map<String, Object> aggregations = statsResult.getAggregations();
```

### RFMAnalysis

RFMAnalysis segments users based on Recency, Frequency, and Monetary value.

```java
// Create RFM analysis
RFMAnalysis rfmAnalysis = new RFMAnalysis(userProfileStore, eventStore, "purchase", "price");

// Calculate RFM scores
Map<String, RFMAnalysis.RFMScore> scores = rfmAnalysis.calculateRFMScores(30, 5);

// Get high-value customers
List<String> highValueCustomers = rfmAnalysis.getHighValueCustomers(scores, 5);

// Get at-risk customers
List<String> atRiskCustomers = rfmAnalysis.getAtRiskCustomers(scores, 5);

// Get new customers
List<String> newCustomers = rfmAnalysis.getNewCustomers(scores, 5);

// Get users in a specific segment
List<String> usersInSegment = rfmAnalysis.getUsersInSegment(scores, 5, 5, 5);

// Get users in a segment range
List<String> usersInRange = rfmAnalysis.getUsersInSegmentRange(scores, 4, 5, 4, 5, 4, 5);
```

### CohortAnalysis

CohortAnalysis tracks user retention over time.

```java
// Create cohort analysis
CohortAnalysis cohortAnalysis = new CohortAnalysis(userProfileStore, eventStore);

// Calculate retention cohorts
CohortAnalysis.CohortResult cohortResult = cohortAnalysis.calculateRetentionCohorts(
        CohortAnalysis.TimePeriod.WEEK, 4, "login");

// Get cohort sizes
int cohort0Size = cohortResult.getCohortSize(0);

// Get retention
int retention = cohortResult.getRetention(0, 1);

// Get retention percentage
double retentionPercentage = cohortResult.getRetentionPercentage(0, 1);

// Get users in a cohort
List<String> usersInCohort = cohortResult.getUsersInCohort(0);
```

### RecommendationEngine

RecommendationEngine generates personalized recommendations for users.

```java
// Create recommendation engine
RecommendationEngine recommendationEngine = new RecommendationEngine(
        userProfileStore, eventStore, "view_item", "purchase", "item_id");

// Get recommendations for a user
List<RecommendationEngine.ScoredItem> recommendations =
        recommendationEngine.getRecommendations("user1", 5);

// Get popular items
List<RecommendationEngine.ScoredItem> popularItems =
        recommendationEngine.getPopularItems(5);
```

### PredictiveModel

PredictiveModel predicts user behavior and engagement.

```java
// Create predictive model
PredictiveModel predictiveModel = new PredictiveModel(userProfileStore, eventStore);

// Predict event likelihood
double likelihood = predictiveModel.predictEventLikelihood("user1", "purchase");

// Predict best time to send
int bestHour = predictiveModel.predictBestTimeToSend("user1");
```

## REST API

InsightAxisDB provides a REST API for external access. See the [API Documentation](api_documentation.md) for details.

```java
// Create and start the server
InsightAxisDBServer server = new InsightAxisDBServer();
server.start();
```

## Extending InsightAxisDB

### Creating Custom Query Conditions

You can create custom query conditions by extending the `QueryCondition` class:

```java
public class CustomQueryCondition extends QueryCondition {

    private final String field;
    private final Object value;

    public CustomQueryCondition(String field, Object value) {
        super(field, Operator.CUSTOM, value);
        this.field = field;
        this.value = value;
    }

    @Override
    public boolean matches(Map<String, Object> row) {
        Object fieldValue = row.get(field);

        if (fieldValue == null) {
            return false;
        }

        // Custom matching logic
        return customMatchingLogic(fieldValue, value);
    }

    private boolean customMatchingLogic(Object fieldValue, Object value) {
        // Implement your custom matching logic here
        return true;
    }
}
```

### Creating Custom Aggregations

You can create custom aggregations by extending the `Query.AggregationType` enum:

```java
public enum CustomAggregationType implements Query.AggregationType {
    CUSTOM_AGG {
        @Override
        public Object aggregate(List<Object> values) {
            // Implement your custom aggregation logic here
            return values.size();
        }
    }
}
```

## Best Practices

### Performance Optimization

- Use appropriate indexes for frequently queried fields
- Limit the number of events stored per user
- Use compression for NCF files
- Apply retention policies to remove old data

### Memory Management

- Use pagination when querying large datasets
- Limit the number of concurrent queries
- Monitor memory usage and adjust JVM parameters accordingly

### Security

- Implement authentication and authorization for the REST API
- Encrypt sensitive data before storing
- Validate input data to prevent injection attacks

### Scalability

- Shard data across multiple instances for horizontal scaling
- Use a load balancer for the REST API
- Implement caching for frequently accessed data
