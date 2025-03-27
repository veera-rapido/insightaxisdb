package com.insightaxisdb.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration settings for InsightAxisDB.
 */
public class Config {
    
    // Storage settings
    public static final int DEFAULT_LOOKBACK_PERIOD_DAYS = 1095; // 3 years
    public static final int MAX_DATA_POINTS_PER_USER_PER_MONTH = 2000;
    
    // Compression settings
    public static final boolean COMPRESSION_ENABLED = true;
    public static final String COMPRESSION_ALGORITHM = "lz4"; // Options: lz4, zstd, snappy
    
    // Sharding settings
    public static final boolean SHARDING_ENABLED = true;
    public static final int DEFAULT_SHARDS = 16;
    
    // Query settings
    public static final int QUERY_TIMEOUT_SECONDS = 30;
    public static final int MAX_CONCURRENT_QUERIES = 100;
    
    // ML settings
    public static final boolean ML_ENABLED = true;
    public static final int ML_MODEL_CACHE_SIZE = 1000; // Number of models to cache
    
    /**
     * Return the current configuration as a map.
     */
    public static Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        
        config.put("DEFAULT_LOOKBACK_PERIOD_DAYS", DEFAULT_LOOKBACK_PERIOD_DAYS);
        config.put("MAX_DATA_POINTS_PER_USER_PER_MONTH", MAX_DATA_POINTS_PER_USER_PER_MONTH);
        config.put("COMPRESSION_ENABLED", COMPRESSION_ENABLED);
        config.put("COMPRESSION_ALGORITHM", COMPRESSION_ALGORITHM);
        config.put("SHARDING_ENABLED", SHARDING_ENABLED);
        config.put("DEFAULT_SHARDS", DEFAULT_SHARDS);
        config.put("QUERY_TIMEOUT_SECONDS", QUERY_TIMEOUT_SECONDS);
        config.put("MAX_CONCURRENT_QUERIES", MAX_CONCURRENT_QUERIES);
        config.put("ML_ENABLED", ML_ENABLED);
        config.put("ML_MODEL_CACHE_SIZE", ML_MODEL_CACHE_SIZE);
        
        return config;
    }
}
