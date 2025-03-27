package com.tesseractdb.storage;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Storage for events.
 */
public class EventStore {
    private final UserProfileStore userProfileStore;
    private final Map<String, Event> events = new HashMap<>();
    private final Map<String, Set<String>> eventIndex = new HashMap<>();
    private final Map<String, List<String>> userEvents = new HashMap<>();
    
    /**
     * Initialize event store.
     *
     * @param userProfileStore User profile store
     */
    public EventStore(UserProfileStore userProfileStore) {
        this.userProfileStore = userProfileStore;
    }
    
    /**
     * Add an event.
     *
     * @param eventName Name of the event
     * @param userId ID of the user who triggered the event
     * @param properties Event properties
     * @param timestamp Event timestamp (Unix timestamp)
     * @return Created event
     */
    public Event addEvent(String eventName, String userId, Map<String, Object> properties, Long timestamp) {
        // Create event
        Event event = new Event(eventName, userId, properties, timestamp, null);
        
        // Store event
        events.put(event.getEventId(), event);
        
        // Update event index
        eventIndex.computeIfAbsent(eventName, k -> new HashSet<>()).add(event.getEventId());
        
        // Update user events
        userEvents.computeIfAbsent(userId, k -> new ArrayList<>()).add(event.getEventId());
        
        // Update user profile
        UserProfileStore.ProfileResult result = userProfileStore.getOrCreateProfile(userId, null);
        result.getProfile().addEvent(event.getEventId());
        
        return event;
    }
    
    /**
     * Get an event.
     *
     * @param eventId Event ID
     * @return Event or null if not found
     */
    public Event getEvent(String eventId) {
        return events.get(eventId);
    }
    
    /**
     * Get events by name.
     *
     * @param eventName Event name
     * @return List of events with the given name
     */
    public List<Event> getEventsByName(String eventName) {
        if (!eventIndex.containsKey(eventName)) {
            return Collections.emptyList();
        }
        
        return eventIndex.get(eventName).stream()
                .map(events::get)
                .collect(Collectors.toList());
    }
    
    /**
     * Get events for a user.
     *
     * @param userId User ID
     * @return List of events for the user
     */
    public List<Event> getUserEvents(String userId) {
        if (!userEvents.containsKey(userId)) {
            return Collections.emptyList();
        }
        
        return userEvents.get(userId).stream()
                .map(events::get)
                .collect(Collectors.toList());
    }
    
    /**
     * Get events for a user with a specific name.
     *
     * @param userId User ID
     * @param eventName Event name
     * @return List of events for the user with the given name
     */
    public List<Event> getUserEventsByName(String userId, String eventName) {
        return getUserEvents(userId).stream()
                .filter(event -> event.getEventName().equals(eventName))
                .collect(Collectors.toList());
    }
    
    /**
     * Get events for a user in a time range.
     *
     * @param userId User ID
     * @param startTime Start time (Unix timestamp)
     * @param endTime End time (Unix timestamp)
     * @return List of events for the user in the given time range
     */
    public List<Event> getUserEventsInTimerange(String userId, long startTime, long endTime) {
        return getUserEvents(userId).stream()
                .filter(event -> startTime <= event.getTimestamp() && event.getTimestamp() <= endTime)
                .collect(Collectors.toList());
    }
}
