# InsightAxisDB API Documentation

InsightAxisDB provides a comprehensive REST API for interacting with the database. This document outlines all available endpoints, their parameters, and example responses.

## Table of Contents

1. [API Overview](#api-overview)
2. [Authentication](#authentication)
3. [User Management](#user-management)
4. [Event Tracking](#event-tracking)
5. [Query API](#query-api)
6. [Segmentation API](#segmentation-api)
7. [Machine Learning API](#machine-learning-api)
8. [System API](#system-api)
9. [Error Handling](#error-handling)
10. [Rate Limiting](#rate-limiting)

## API Overview

The InsightAxisDB API is a RESTful API that uses JSON for request and response bodies. All endpoints are prefixed with `/api`.

Base URL: `http://your-server:8080/api`

## Authentication

Currently, InsightAxisDB does not implement authentication. For production use, it is recommended to implement an authentication layer using JWT, OAuth, or API keys.

## User Management

### Get All Users

Retrieves a list of all user profiles.

**Endpoint:** `GET /users`

**Response:**
```json
[
  {
    "userId": "user1",
    "properties": {
      "name": "John Doe",
      "email": "john@example.com",
      "country": "USA",
      "age": 30
    },
    "firstSeenAt": 1643075638889,
    "lastSeenAt": 1643075638889,
    "eventCount": 5,
    "events": ["event1", "event2", "event3", "event4", "event5"]
  },
  {
    "userId": "user2",
    "properties": {
      "name": "Jane Smith",
      "email": "jane@example.com",
      "country": "UK",
      "age": 25
    },
    "firstSeenAt": 1643075648889,
    "lastSeenAt": 1643075648889,
    "eventCount": 3,
    "events": ["event6", "event7", "event8"]
  }
]
```

### Get User by ID

Retrieves a specific user profile by ID.

**Endpoint:** `GET /users/{userId}`

**Parameters:**
- `userId` (path): The ID of the user to retrieve

**Response:**
```json
{
  "userId": "user1",
  "properties": {
    "name": "John Doe",
    "email": "john@example.com",
    "country": "USA",
    "age": 30
  },
  "firstSeenAt": 1643075638889,
  "lastSeenAt": 1643075638889,
  "eventCount": 5,
  "events": ["event1", "event2", "event3", "event4", "event5"]
}
```

### Create User

Creates a new user profile.

**Endpoint:** `POST /users`

**Request Body:**
```json
{
  "userId": "user3",
  "properties": {
    "name": "Bob Johnson",
    "email": "bob@example.com",
    "country": "Canada",
    "age": 35
  }
}
```

**Response:**
```json
{
  "userId": "user3",
  "properties": {
    "name": "Bob Johnson",
    "email": "bob@example.com",
    "country": "Canada",
    "age": 35
  },
  "firstSeenAt": 1643075658889,
  "lastSeenAt": 1643075658889,
  "eventCount": 0,
  "events": []
}
```

### Update User

Updates an existing user profile.

**Endpoint:** `PUT /users/{userId}`

**Parameters:**
- `userId` (path): The ID of the user to update

**Request Body:**
```json
{
  "properties": {
    "name": "Bob Johnson Jr.",
    "premium": true
  }
}
```

**Response:**
```json
{
  "userId": "user3",
  "properties": {
    "name": "Bob Johnson Jr.",
    "email": "bob@example.com",
    "country": "Canada",
    "age": 35,
    "premium": true
  },
  "firstSeenAt": 1643075658889,
  "lastSeenAt": 1643075658889,
  "eventCount": 0,
  "events": []
}
```

### Delete User

Deletes a user profile.

**Endpoint:** `DELETE /users/{userId}`

**Parameters:**
- `userId` (path): The ID of the user to delete

**Response:**
```json
{
  "success": true,
  "message": "User deleted successfully"
}
```

## Event Tracking

### Get All Events

Retrieves a list of all events.

**Endpoint:** `GET /events`

**Response:**
```json
[
  {
    "eventId": "event1",
    "eventName": "login",
    "userId": "user1",
    "timestamp": 1643075638889,
    "properties": {
      "device": "desktop",
      "os": "macOS"
    }
  },
  {
    "eventId": "event2",
    "eventName": "view_item",
    "userId": "user1",
    "timestamp": 1643075639889,
    "properties": {
      "device": "desktop",
      "item_id": "item1",
      "category": "electronics",
      "price": 99.99
    }
  }
]
```

### Get Event by ID

Retrieves a specific event by ID.

**Endpoint:** `GET /events/{eventId}`

**Parameters:**
- `eventId` (path): The ID of the event to retrieve

**Response:**
```json
{
  "eventId": "event1",
  "eventName": "login",
  "userId": "user1",
  "timestamp": 1643075638889,
  "properties": {
    "device": "desktop",
    "os": "macOS"
  }
}
```

### Create Event

Creates a new event.

**Endpoint:** `POST /events`

**Request Body:**
```json
{
  "eventName": "purchase",
  "userId": "user1",
  "properties": {
    "device": "desktop",
    "item_id": "item1",
    "category": "electronics",
    "price": 99.99,
    "quantity": 1
  }
}
```

**Response:**
```json
{
  "eventId": "event9",
  "eventName": "purchase",
  "userId": "user1",
  "timestamp": 1643075668889,
  "properties": {
    "device": "desktop",
    "item_id": "item1",
    "category": "electronics",
    "price": 99.99,
    "quantity": 1
  }
}
```

### Get User Events

Retrieves all events for a specific user.

**Endpoint:** `GET /users/{userId}/events`

**Parameters:**
- `userId` (path): The ID of the user

**Response:**
```json
[
  {
    "eventId": "event1",
    "eventName": "login",
    "userId": "user1",
    "timestamp": 1643075638889,
    "properties": {
      "device": "desktop",
      "os": "macOS"
    }
  },
  {
    "eventId": "event2",
    "eventName": "view_item",
    "userId": "user1",
    "timestamp": 1643075639889,
    "properties": {
      "device": "desktop",
      "item_id": "item1",
      "category": "electronics",
      "price": 99.99
    }
  }
]
```

## Query API

### Query Users

Executes a query against user profiles.

**Endpoint:** `POST /query/users`

**Request Body:**
```json
{
  "where": [
    {
      "field": "country",
      "operator": "EQ",
      "value": "USA"
    },
    {
      "field": "age",
      "operator": "GT",
      "value": 25
    }
  ],
  "select": ["userId", "name", "age", "email"],
  "orderBy": [
    {
      "field": "age",
      "order": "DESC"
    }
  ],
  "limit": 10,
  "offset": 0
}
```

**Response:**
```json
{
  "rows": [
    {
      "userId": "user1",
      "name": "John Doe",
      "age": 30,
      "email": "john@example.com"
    }
  ],
  "rowCount": 1,
  "aggregations": {}
}
```

### Query Events

Executes a query against events.

**Endpoint:** `POST /query/events`

**Request Body:**
```json
{
  "where": [
    {
      "field": "eventName",
      "operator": "EQ",
      "value": "purchase"
    },
    {
      "field": "price",
      "operator": "GT",
      "value": 50.0
    }
  ],
  "select": ["eventId", "userId", "item_id", "price", "category"],
  "orderBy": [
    {
      "field": "price",
      "order": "DESC"
    }
  ],
  "limit": 5,
  "offset": 0
}
```

**Response:**
```json
{
  "rows": [
    {
      "eventId": "event9",
      "userId": "user1",
      "item_id": "item1",
      "price": 99.99,
      "category": "electronics"
    }
  ],
  "rowCount": 1,
  "aggregations": {}
}
```

### Query User Events

Executes a query against events for a specific user.

**Endpoint:** `POST /query/users/{userId}/events`

**Parameters:**
- `userId` (path): The ID of the user

**Request Body:**
```json
{
  "where": [
    {
      "field": "eventName",
      "operator": "EQ",
      "value": "purchase"
    }
  ],
  "aggregate": [
    {
      "field": "price",
      "type": "SUM",
      "alias": "total_spent"
    },
    {
      "field": "price",
      "type": "AVG",
      "alias": "avg_price"
    },
    {
      "field": "eventId",
      "type": "COUNT",
      "alias": "purchase_count"
    }
  ]
}
```

**Response:**
```json
{
  "rows": [],
  "rowCount": 0,
  "aggregations": {
    "total_spent": 99.99,
    "avg_price": 99.99,
    "purchase_count": 1
  }
}
```

## Segmentation API

### RFM Analysis

Performs RFM (Recency, Frequency, Monetary) analysis on users.

**Endpoint:** `GET /segmentation/rfm`

**Parameters:**
- `recencyDays` (query): Number of days to consider for recency (default: 30)
- `numSegments` (query): Number of segments to create (default: 5)

**Response:**
```json
{
  "user1": {
    "recencyScore": 5,
    "frequencyScore": 4,
    "monetaryScore": 5
  },
  "user2": {
    "recencyScore": 3,
    "frequencyScore": 2,
    "monetaryScore": 3
  },
  "user3": {
    "recencyScore": 1,
    "frequencyScore": 1,
    "monetaryScore": 2
  }
}
```

### Cohort Analysis

Performs cohort analysis on users.

**Endpoint:** `GET /segmentation/cohorts`

**Parameters:**
- `timePeriod` (query): Time period for cohorts (DAY, WEEK, MONTH) (default: WEEK)
- `numPeriods` (query): Number of periods to analyze (default: 4)
- `targetEventName` (query): Event name to track for retention (default: login)

**Response:**
```json
{
  "timePeriod": "WEEK",
  "numPeriods": 4,
  "cohortSizes": {
    "0": 3,
    "1": 4,
    "2": 5
  },
  "retention": {
    "0": [3, 2, 1, 0],
    "1": [4, 3, 2, 0],
    "2": [5, 4, 0, 0]
  },
  "retentionPercentages": {
    "0": [1.0, 0.67, 0.33, 0.0],
    "1": [1.0, 0.75, 0.5, 0.0],
    "2": [1.0, 0.8, 0.0, 0.0]
  },
  "usersInCohort": {
    "0": ["user1", "user2", "user3"],
    "1": ["user4", "user5", "user6", "user7"],
    "2": ["user8", "user9", "user10", "user11", "user12"]
  }
}
```

## Machine Learning API

### Get Recommendations

Generates personalized recommendations for a user.

**Endpoint:** `GET /ml/recommendations/{userId}`

**Parameters:**
- `userId` (path): The ID of the user
- `max` (query): Maximum number of recommendations to return (default: 5)

**Response:**
```json
[
  {
    "itemId": "item2",
    "score": 0.85
  },
  {
    "itemId": "item3",
    "score": 0.72
  },
  {
    "itemId": "item4",
    "score": 0.65
  }
]
```

### Get Popular Items

Retrieves the most popular items.

**Endpoint:** `GET /ml/recommendations/popular`

**Parameters:**
- `max` (query): Maximum number of items to return (default: 5)

**Response:**
```json
[
  {
    "itemId": "item1",
    "score": 3.0
  },
  {
    "itemId": "item2",
    "score": 2.5
  },
  {
    "itemId": "item3",
    "score": 2.0
  }
]
```

### Get Event Likelihood

Predicts the likelihood of a user performing a specific event.

**Endpoint:** `GET /ml/predictions/{userId}/{eventName}`

**Parameters:**
- `userId` (path): The ID of the user
- `eventName` (path): The name of the event

**Response:**
```json
{
  "userId": "user1",
  "eventName": "purchase",
  "likelihood": 0.75
}
```

### Get Best Time to Send

Predicts the best time to send notifications to a user.

**Endpoint:** `GET /ml/best-time/{userId}`

**Parameters:**
- `userId` (path): The ID of the user

**Response:**
```json
{
  "userId": "user1",
  "bestHour": 9
}
```

## System API

### Get Configuration

Retrieves the current system configuration.

**Endpoint:** `GET /system/config`

**Response:**
```json
{
  "version": "1.0.0",
  "compression": "lz4",
  "dataDirectory": "data",
  "retentionDays": 365,
  "maxEventsPerUser": 10000,
  "saveIntervalMillis": 60000
}
```

### Save Data

Triggers a manual save of all data to disk.

**Endpoint:** `POST /system/save`

**Response:**
```json
{
  "success": true,
  "message": "Data saved successfully"
}
```

### Load Data

Triggers a manual load of all data from disk.

**Endpoint:** `POST /system/load`

**Response:**
```json
{
  "success": true,
  "message": "Data loaded successfully"
}
```

## Error Handling

InsightAxisDB uses standard HTTP status codes to indicate the success or failure of an API request.

### Common Status Codes

- `200 OK`: The request was successful
- `201 Created`: The resource was successfully created
- `400 Bad Request`: The request was invalid or cannot be served
- `404 Not Found`: The requested resource does not exist
- `500 Internal Server Error`: An error occurred on the server

### Error Response Format

```json
{
  "error": true,
  "message": "Error message describing what went wrong",
  "status": 400,
  "timestamp": 1643075668889
}
```

## Rate Limiting

Currently, InsightAxisDB does not implement rate limiting. For production use, it is recommended to implement rate limiting to prevent abuse of the API.
