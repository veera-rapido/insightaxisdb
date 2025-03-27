# InsightAxisDB Documentation

Welcome to the InsightAxisDB documentation. This documentation provides comprehensive information about InsightAxisDB, a high-performance, columnar database designed for user engagement and retention analytics.

## Table of Contents

### Getting Started
- [README](../README.md) - Overview and quick start guide
- [Architecture](images/architecture.md) - System architecture diagram

### User Guides
- [Dashboard Guide](dashboard_guide.md) - How to use the web dashboard
- [API Endpoints](api_endpoints.md) - Quick reference for API endpoints
- [API Documentation](api_documentation.md) - Detailed API documentation

### Developer Resources
- [Developer Guide](developer_guide.md) - Guide for developers using InsightAxisDB
- [Release Notes](release_notes.md) - Version history and upcoming features

## About InsightAxisDB

InsightAxisDB is a purpose-built database for storing and analyzing user behavior data at scale. It uses a columnar storage format (Network Columnar Format or NCF) to efficiently store and retrieve large amounts of user data, making it ideal for personalization and user engagement applications.

### Key Features

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

## License

InsightAxisDB is licensed under the MIT License. See the [LICENSE](../LICENSE) file for details.
