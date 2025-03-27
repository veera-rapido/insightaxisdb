# InsightAxisDB API Endpoints

This document provides a quick reference for all available API endpoints in InsightAxisDB.

## User Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users` | Get all users |
| GET | `/api/users/{userId}` | Get user by ID |
| POST | `/api/users` | Create a new user |
| PUT | `/api/users/{userId}` | Update a user |
| DELETE | `/api/users/{userId}` | Delete a user |

## Event Tracking

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/events` | Get all events |
| GET | `/api/events/{eventId}` | Get event by ID |
| POST | `/api/events` | Create a new event |
| GET | `/api/users/{userId}/events` | Get events for a user |

## Query API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/query/users` | Query user profiles |
| POST | `/api/query/events` | Query events |
| POST | `/api/query/users/{userId}/events` | Query events for a user |

## Segmentation API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/segmentation/rfm` | Perform RFM analysis |
| GET | `/api/segmentation/cohorts` | Perform cohort analysis |

## Machine Learning API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/ml/recommendations/{userId}` | Get recommendations for a user |
| GET | `/api/ml/recommendations/popular` | Get popular items |
| GET | `/api/ml/predictions/{userId}/{eventName}` | Predict event likelihood |
| GET | `/api/ml/best-time/{userId}` | Predict best time to send |

## System API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/system/config` | Get system configuration |
| POST | `/api/system/save` | Save data to disk |
| POST | `/api/system/load` | Load data from disk |

For detailed information about request and response formats, see the [API Documentation](api_documentation.md).
