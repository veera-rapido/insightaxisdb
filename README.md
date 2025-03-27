# InsightAxisDB

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)]()

A high-performance, columnar database designed for user engagement and retention analytics.

## Overview

InsightAxisDB is a purpose-built database for storing and analyzing user behavior data at scale. It uses a columnar storage format (Network Columnar Format or NCF) to efficiently store and retrieve large amounts of user data, making it ideal for personalization and user engagement applications.

## Key Features

- **Network Columnar Format (NCF)**: Efficient columnar storage format optimized for user behavior data
- **High Data Granularity**: Store up to 2,000 data points per user per month
- **Extended Lookback Period**: Access up to 3 years of historical data by default
- **Advanced Query Engine**: SQL-like querying with filtering, sorting, and aggregation
- **User Segmentation**: RFM analysis and cohort analysis for user segmentation
- **Machine Learning**: Recommendation engine and predictive modeling
- **Persistence Layer**: Efficient storage with automatic data retention policies
- **REST API**: HTTP endpoints for data ingestion and retrieval
- **Real-time Analytics**: Query and analyze user data in real-time
- **Efficient Compression**: Minimize storage costs while maintaining query performance
- **Scalable Architecture**: Designed to handle billions of events per day
- **Web Dashboard**: Interactive UI for data visualization and management

## Architecture

InsightAxisDB follows a modular architecture with clear separation of concerns:

See the [Architecture Diagram](docs/images/architecture.md) for a visual representation.

InsightAxisDB consists of the following components:

1. **REST API Layer**: Handles HTTP requests and responses for all functionality
2. **Query Engine**: Processes queries with filtering, sorting, and aggregation
3. **Storage Layer**: Implements the Network Columnar Format (NCF) for efficient data storage
4. **User Profile Management**: Stores and manages user profiles and their associated events
5. **Persistence Layer**: Handles data persistence with automatic retention policies
6. **Segmentation Engine**: Provides RFM and cohort analysis for user segmentation
7. **ML Engine**: Integrates recommendation and predictive modeling capabilities

## Getting Started

### Prerequisites

- Java 11 or higher
- Maven 3.6 or higher

### Building the Project

```bash
git clone https://github.com/yourusername/insightaxisdb.git
cd insightaxisdb
mvn clean package
```

### Running the Server

```bash
# Run the REST API server
java -jar target/insightaxisdb-1.0-SNAPSHOT.jar
```

Once the server is running, you can access the dashboard at http://localhost:8080

### Quick Start

Once the server is running, you can interact with TesseractDB via its REST API:

1. Create a user:
```bash
curl -X POST -H "Content-Type: application/json" -d '{"userId":"user1","properties":{"name":"John Doe","email":"john@example.com"}}' http://localhost:8080/api/users
```

2. Track an event:
```bash
curl -X POST -H "Content-Type: application/json" -d '{"eventName":"login","userId":"user1","properties":{"device":"desktop"}}' http://localhost:8080/api/events
```

3. Query users:
```bash
curl -X POST -H "Content-Type: application/json" -d '{"select":["userId","name"]}' http://localhost:8080/api/query/users
```

### Running the Examples

```bash
# Run the comprehensive example
java -cp target/insightaxisdb-1.0-SNAPSHOT.jar com.insightaxisdb.example.ComprehensiveExample

# Run the advanced example
java -cp target/insightaxisdb-1.0-SNAPSHOT.jar com.insightaxisdb.example.AdvancedExample
```

The comprehensive example demonstrates:
1. Creating and writing data to an NCF file
2. Creating user profiles and tracking events
3. Using the query engine for complex data analysis
4. Persisting data to disk and applying retention policies
5. Segmenting users with RFM and cohort analysis
6. Generating recommendations and predictions with ML

The REST API server provides HTTP endpoints for:
1. Managing user profiles and events
2. Querying and analyzing data
3. Generating segments, recommendations, and predictions
4. Accessing the web dashboard

### Using TesseractDB in Your Project

To use TesseractDB in your own project, you can include the following components:

#### Core Components
- **NCF**: The Network Columnar Format storage engine
- **UserProfileStore**: For managing user profiles
- **EventStore**: For tracking and querying user events
- **PersistenceManager**: For saving and loading data

#### Advanced Features
- **QueryEngine**: For complex data analysis
- **RFMAnalysis**: For RFM-based user segmentation
- **CohortAnalysis**: For cohort-based user segmentation
- **RecommendationEngine**: For generating item recommendations
- **PredictiveModel**: For predicting user behavior

#### API
- **TesseractDBServer**: REST API server for external access

## API Documentation

For detailed API documentation, see the [API Documentation](docs/api_documentation.md) file.

## Dashboard

TesseractDB includes a web-based dashboard for visualizing data and insights. The dashboard is available at `http://localhost:8080` when the server is running.

The dashboard provides the following features:

- **User Management**: View and manage user profiles
- **Event Tracking**: View recent events and event distribution
- **Segmentation**: Perform RFM and cohort analysis
- **Recommendations**: Generate and view recommendations
- **Predictions**: Predict user behavior and engagement

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
