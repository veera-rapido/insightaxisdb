# InsightAxisDB Architecture

```
+---------------------------+
|     Client Application    |
+---------------------------+
             |
             v
+---------------------------+
|      REST API Layer       |
|---------------------------|
| - InsightAxisDBServer       |
| - API Endpoints           |
| - Request Handlers        |
| - Response Formatters     |
+---------------------------+
             |
             v
+---------------------------+     +---------------------------+     +---------------------------+
|      Query Engine         |     |    Segmentation Engine    |     |     Machine Learning     |
|---------------------------|     |---------------------------|     |---------------------------|
| - Query                   |     | - RFMAnalysis             |     | - RecommendationEngine   |
| - QueryCondition          |     | - CohortAnalysis          |     | - PredictiveModel        |
| - QueryResult             |     |                           |     |                           |
| - QueryEngine             |     |                           |     |                           |
+---------------------------+     +---------------------------+     +---------------------------+
             |                                |                                |
             |                                |                                |
             v                                v                                v
+-------------------------------------------------------------------------------------------+
|                                    Storage Layer                                           |
|-------------------------------------------------------------------------------------------|
| - UserProfileStore                                                                         |
| - EventStore                                                                               |
| - PersistenceManager                                                                       |
| - NCF (Network Columnar Format)                                                            |
+-------------------------------------------------------------------------------------------+
```
