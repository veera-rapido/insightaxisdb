package com.tesseractdb.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User profile class.
 */
public class UserProfile {
    private final String userId;
    private final Map<String, Object> properties;
    private long firstSeenAt;
    private long lastSeenAt;
    private final List<String> events;
    private int eventCount;
    
    /**
     * Initialize user profile.
     *
     * @param userId Unique identifier for the user
     * @param properties User properties
     */
    public UserProfile(String userId, Map<String, Object> properties) {
        this.userId = userId;
        this.properties = properties != null ? properties : new HashMap<>();
        this.firstSeenAt = System.currentTimeMillis();
        this.lastSeenAt = this.firstSeenAt;
        this.events = new ArrayList<>();
        this.eventCount = 0;
    }
    
    /**
     * Update a user property.
     *
     * @param key Property name
     * @param value Property value
     */
    public void updateProperty(String key, Object value) {
        properties.put(key, value);
        lastSeenAt = System.currentTimeMillis();
    }
    
    /**
     * Update multiple user properties.
     *
     * @param properties Map of property name to value
     */
    public void updateProperties(Map<String, Object> properties) {
        this.properties.putAll(properties);
        lastSeenAt = System.currentTimeMillis();
    }
    
    /**
     * Add an event to the user profile.
     *
     * @param eventId ID of the event
     */
    public void addEvent(String eventId) {
        events.add(eventId);
        eventCount++;
        lastSeenAt = System.currentTimeMillis();
    }
    
    /**
     * Convert user profile to map.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("properties", properties);
        map.put("firstSeenAt", firstSeenAt);
        map.put("lastSeenAt", lastSeenAt);
        map.put("eventCount", eventCount);
        map.put("events", events);
        return map;
    }
    
    /**
     * Create user profile from map.
     */
    public static UserProfile fromMap(Map<String, Object> map) {
        String userId = (String) map.get("userId");
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) map.get("properties");
        
        UserProfile profile = new UserProfile(userId, properties);
        profile.firstSeenAt = (Long) map.get("firstSeenAt");
        profile.lastSeenAt = (Long) map.get("lastSeenAt");
        
        @SuppressWarnings("unchecked")
        List<String> events = (List<String>) map.get("events");
        profile.events.addAll(events);
        
        profile.eventCount = (Integer) map.get("eventCount");
        
        return profile;
    }
    
    // Getters
    
    public String getUserId() {
        return userId;
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    public long getFirstSeenAt() {
        return firstSeenAt;
    }
    
    public long getLastSeenAt() {
        return lastSeenAt;
    }
    
    public List<String> getEvents() {
        return events;
    }
    
    public int getEventCount() {
        return eventCount;
    }
}
