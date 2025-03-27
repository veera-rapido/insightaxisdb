package com.tesseractdb.storage;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test cases for the EventStore.
 */
public class EventStoreTest {
    
    private UserProfileStore userProfileStore;
    private EventStore eventStore;
    
    @Before
    public void setUp() {
        userProfileStore = new UserProfileStore();
        eventStore = new EventStore(userProfileStore);
        
        // Create a user profile
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", "John Doe");
        userProfileStore.createProfile("user1", properties);
    }
    
    @Test
    public void testAddEvent() {
        // Add an event
        String eventName = "login";
        String userId = "user1";
        Map<String, Object> properties = new HashMap<>();
        properties.put("device", "mobile");
        properties.put("os", "iOS");
        
        Event event = eventStore.addEvent(eventName, userId, properties, null);
        
        // Check event
        assertNotNull(event);
        assertEquals(eventName, event.getEventName());
        assertEquals(userId, event.getUserId());
        assertEquals("mobile", event.getProperties().get("device"));
        assertEquals("iOS", event.getProperties().get("os"));
        assertNotNull(event.getEventId());
        assertTrue(event.getTimestamp() > 0);
        
        // Check user profile
        UserProfile profile = userProfileStore.getProfile(userId);
        assertEquals(1, profile.getEventCount());
        assertEquals(1, profile.getEvents().size());
        assertEquals(event.getEventId(), profile.getEvents().get(0));
    }
    
    @Test
    public void testAddEventWithTimestamp() {
        // Add an event with a specific timestamp
        String eventName = "login";
        String userId = "user1";
        Map<String, Object> properties = new HashMap<>();
        properties.put("device", "mobile");
        
        long timestamp = 1000000L;
        Event event = eventStore.addEvent(eventName, userId, properties, timestamp);
        
        // Check event
        assertNotNull(event);
        assertEquals(timestamp, event.getTimestamp());
    }
    
    @Test
    public void testAddEventForNonExistentUser() {
        // Add an event for a non-existent user
        String eventName = "login";
        String userId = "nonexistent";
        Map<String, Object> properties = new HashMap<>();
        
        Event event = eventStore.addEvent(eventName, userId, properties, null);
        
        // Check event
        assertNotNull(event);
        assertEquals(eventName, event.getEventName());
        assertEquals(userId, event.getUserId());
        
        // Check user profile
        UserProfile profile = userProfileStore.getProfile(userId);
        assertNotNull(profile);
        assertEquals(1, profile.getEventCount());
        assertEquals(1, profile.getEvents().size());
        assertEquals(event.getEventId(), profile.getEvents().get(0));
    }
    
    @Test
    public void testGetEvent() {
        // Add an event
        Event event = eventStore.addEvent("login", "user1", null, null);
        
        // Get the event
        Event retrievedEvent = eventStore.getEvent(event.getEventId());
        
        // Check event
        assertNotNull(retrievedEvent);
        assertEquals(event.getEventId(), retrievedEvent.getEventId());
        assertEquals(event.getEventName(), retrievedEvent.getEventName());
        assertEquals(event.getUserId(), retrievedEvent.getUserId());
        assertEquals(event.getTimestamp(), retrievedEvent.getTimestamp());
    }
    
    @Test
    public void testGetNonExistentEvent() {
        // Get a non-existent event
        Event event = eventStore.getEvent("nonexistent");
        
        // Check event
        assertNull(event);
    }
    
    @Test
    public void testGetEventsByName() {
        // Add events
        eventStore.addEvent("login", "user1", null, null);
        eventStore.addEvent("login", "user1", null, null);
        eventStore.addEvent("purchase", "user1", null, null);
        
        // Get events by name
        List<Event> events = eventStore.getEventsByName("login");
        
        // Check events
        assertNotNull(events);
        assertEquals(2, events.size());
        assertEquals("login", events.get(0).getEventName());
        assertEquals("login", events.get(1).getEventName());
    }
    
    @Test
    public void testGetEventsByNameNonExistent() {
        // Get events by non-existent name
        List<Event> events = eventStore.getEventsByName("nonexistent");
        
        // Check events
        assertNotNull(events);
        assertTrue(events.isEmpty());
    }
    
    @Test
    public void testGetUserEvents() {
        // Add events for different users
        eventStore.addEvent("login", "user1", null, null);
        eventStore.addEvent("purchase", "user1", null, null);
        eventStore.addEvent("login", "user2", null, null);
        
        // Get events for user1
        List<Event> events = eventStore.getUserEvents("user1");
        
        // Check events
        assertNotNull(events);
        assertEquals(2, events.size());
        assertEquals("user1", events.get(0).getUserId());
        assertEquals("user1", events.get(1).getUserId());
    }
    
    @Test
    public void testGetUserEventsNonExistent() {
        // Get events for non-existent user
        List<Event> events = eventStore.getUserEvents("nonexistent");
        
        // Check events
        assertNotNull(events);
        assertTrue(events.isEmpty());
    }
    
    @Test
    public void testGetUserEventsByName() {
        // Add events
        eventStore.addEvent("login", "user1", null, null);
        eventStore.addEvent("login", "user1", null, null);
        eventStore.addEvent("purchase", "user1", null, null);
        
        // Get events by name for user1
        List<Event> events = eventStore.getUserEventsByName("user1", "login");
        
        // Check events
        assertNotNull(events);
        assertEquals(2, events.size());
        assertEquals("login", events.get(0).getEventName());
        assertEquals("login", events.get(1).getEventName());
    }
    
    @Test
    public void testGetUserEventsInTimerange() {
        // Add events with different timestamps
        long now = System.currentTimeMillis();
        eventStore.addEvent("login", "user1", null, now - 1000);
        eventStore.addEvent("login", "user1", null, now - 2000);
        eventStore.addEvent("login", "user1", null, now - 3000);
        
        // Get events in timerange
        List<Event> events = eventStore.getUserEventsInTimerange("user1", now - 2500, now - 500);
        
        // Check events
        assertNotNull(events);
        assertEquals(2, events.size());
    }
    
    @Test
    public void testEventToMap() {
        // Create an event
        String eventName = "login";
        String userId = "user1";
        Map<String, Object> properties = new HashMap<>();
        properties.put("device", "mobile");
        
        Event event = eventStore.addEvent(eventName, userId, properties, 1000L);
        
        // Convert to map
        Map<String, Object> map = event.toMap();
        
        // Check map
        assertNotNull(map);
        assertEquals(event.getEventId(), map.get("eventId"));
        assertEquals(eventName, map.get("eventName"));
        assertEquals(userId, map.get("userId"));
        assertEquals(1000L, map.get("timestamp"));
        assertEquals("mobile", ((Map<?, ?>) map.get("properties")).get("device"));
    }
    
    @Test
    public void testEventFromMap() {
        // Create a map
        Map<String, Object> map = new HashMap<>();
        map.put("eventId", "event1");
        map.put("eventName", "login");
        map.put("userId", "user1");
        map.put("timestamp", 1000L);
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("device", "mobile");
        map.put("properties", properties);
        
        // Create event from map
        Event event = Event.fromMap(map);
        
        // Check event
        assertNotNull(event);
        assertEquals("event1", event.getEventId());
        assertEquals("login", event.getEventName());
        assertEquals("user1", event.getUserId());
        assertEquals(1000L, event.getTimestamp());
        assertEquals("mobile", event.getProperties().get("device"));
    }
}
