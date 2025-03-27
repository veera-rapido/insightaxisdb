package com.tesseractdb.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event class.
 */
public class Event {
    private final String eventId;
    private final String eventName;
    private final String userId;
    private final Map<String, Object> properties;
    private final long timestamp;
    
    /**
     * Initialize event.
     *
     * @param eventName Name of the event
     * @param userId ID of the user who triggered the event
     * @param properties Event properties
     * @param timestamp Event timestamp (Unix timestamp)
     * @param eventId Unique identifier for the event
     */
    public Event(String eventName, String userId, Map<String, Object> properties, Long timestamp, String eventId) {
        this.eventName = eventName;
        this.userId = userId;
        this.properties = properties != null ? properties : new HashMap<>();
        this.timestamp = timestamp != null ? timestamp : System.currentTimeMillis();
        this.eventId = eventId != null ? eventId : UUID.randomUUID().toString();
    }
    
    /**
     * Convert event to map.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("eventId", eventId);
        map.put("eventName", eventName);
        map.put("userId", userId);
        map.put("properties", properties);
        map.put("timestamp", timestamp);
        return map;
    }
    
    /**
     * Create event from map.
     */
    public static Event fromMap(Map<String, Object> map) {
        String eventName = (String) map.get("eventName");
        String userId = (String) map.get("userId");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) map.get("properties");
        
        Long timestamp = (Long) map.get("timestamp");
        String eventId = (String) map.get("eventId");
        
        return new Event(eventName, userId, properties, timestamp, eventId);
    }
    
    // Getters
    
    public String getEventId() {
        return eventId;
    }
    
    public String getEventName() {
        return eventName;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
}
