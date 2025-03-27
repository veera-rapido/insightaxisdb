package com.insightaxisdb.storage;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test cases for the UserProfileStore.
 */
public class UserProfileStoreTest {
    
    private UserProfileStore store;
    
    @Before
    public void setUp() {
        store = new UserProfileStore();
    }
    
    @Test
    public void testCreateProfile() {
        // Create a user profile
        String userId = "user1";
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", "John Doe");
        properties.put("email", "john@example.com");
        
        UserProfile profile = store.createProfile(userId, properties);
        
        // Check profile
        assertNotNull(profile);
        assertEquals(userId, profile.getUserId());
        assertEquals("John Doe", profile.getProperties().get("name"));
        assertEquals("john@example.com", profile.getProperties().get("email"));
        assertEquals(0, profile.getEventCount());
        assertTrue(profile.getEvents().isEmpty());
    }
    
    @Test
    public void testGetProfile() {
        // Create a user profile
        String userId = "user1";
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", "John Doe");
        
        store.createProfile(userId, properties);
        
        // Get the profile
        UserProfile profile = store.getProfile(userId);
        
        // Check profile
        assertNotNull(profile);
        assertEquals(userId, profile.getUserId());
        assertEquals("John Doe", profile.getProperties().get("name"));
    }
    
    @Test
    public void testGetNonExistentProfile() {
        // Get a non-existent profile
        UserProfile profile = store.getProfile("nonexistent");
        
        // Check profile
        assertNull(profile);
    }
    
    @Test
    public void testGetOrCreateProfile() {
        // Get or create a profile
        String userId = "user1";
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", "John Doe");
        
        UserProfileStore.ProfileResult result = store.getOrCreateProfile(userId, properties);
        
        // Check result
        assertNotNull(result);
        assertTrue(result.isCreated());
        assertNotNull(result.getProfile());
        assertEquals(userId, result.getProfile().getUserId());
        
        // Get or create the same profile again
        result = store.getOrCreateProfile(userId, null);
        
        // Check result
        assertNotNull(result);
        assertFalse(result.isCreated());
        assertNotNull(result.getProfile());
        assertEquals(userId, result.getProfile().getUserId());
    }
    
    @Test
    public void testUpdateProfile() {
        // Create a user profile
        String userId = "user1";
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", "John Doe");
        
        store.createProfile(userId, properties);
        
        // Update the profile
        Map<String, Object> newProperties = new HashMap<>();
        newProperties.put("email", "john@example.com");
        newProperties.put("age", 30);
        
        UserProfile updatedProfile = store.updateProfile(userId, newProperties);
        
        // Check updated profile
        assertNotNull(updatedProfile);
        assertEquals(userId, updatedProfile.getUserId());
        assertEquals("John Doe", updatedProfile.getProperties().get("name"));
        assertEquals("john@example.com", updatedProfile.getProperties().get("email"));
        assertEquals(30, updatedProfile.getProperties().get("age"));
    }
    
    @Test
    public void testUpdateNonExistentProfile() {
        // Update a non-existent profile
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", "John Doe");
        
        UserProfile profile = store.updateProfile("nonexistent", properties);
        
        // Check result
        assertNull(profile);
    }
    
    @Test
    public void testDeleteProfile() {
        // Create a user profile
        String userId = "user1";
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", "John Doe");
        
        store.createProfile(userId, properties);
        
        // Delete the profile
        boolean deleted = store.deleteProfile(userId);
        
        // Check result
        assertTrue(deleted);
        assertNull(store.getProfile(userId));
    }
    
    @Test
    public void testDeleteNonExistentProfile() {
        // Delete a non-existent profile
        boolean deleted = store.deleteProfile("nonexistent");
        
        // Check result
        assertFalse(deleted);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCreateDuplicateProfile() {
        // Create a user profile
        String userId = "user1";
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", "John Doe");
        
        store.createProfile(userId, properties);
        
        // Try to create a duplicate profile
        store.createProfile(userId, properties);
    }
    
    @Test
    public void testProfileToMap() {
        // Create a user profile
        String userId = "user1";
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", "John Doe");
        properties.put("email", "john@example.com");
        
        UserProfile profile = store.createProfile(userId, properties);
        
        // Convert to map
        Map<String, Object> map = profile.toMap();
        
        // Check map
        assertNotNull(map);
        assertEquals(userId, map.get("userId"));
        assertEquals("John Doe", ((Map<?, ?>) map.get("properties")).get("name"));
        assertEquals("john@example.com", ((Map<?, ?>) map.get("properties")).get("email"));
        assertEquals(0, map.get("eventCount"));
    }
    
    @Test
    public void testProfileFromMap() {
        // Create a map
        Map<String, Object> map = new HashMap<>();
        map.put("userId", "user1");
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", "John Doe");
        properties.put("email", "john@example.com");
        map.put("properties", properties);
        
        map.put("firstSeenAt", 1000L);
        map.put("lastSeenAt", 2000L);
        map.put("eventCount", 5);
        map.put("events", java.util.Arrays.asList("event1", "event2"));
        
        // Create profile from map
        UserProfile profile = UserProfile.fromMap(map);
        
        // Check profile
        assertNotNull(profile);
        assertEquals("user1", profile.getUserId());
        assertEquals("John Doe", profile.getProperties().get("name"));
        assertEquals("john@example.com", profile.getProperties().get("email"));
        assertEquals(1000L, profile.getFirstSeenAt());
        assertEquals(2000L, profile.getLastSeenAt());
        assertEquals(5, profile.getEventCount());
        assertEquals(2, profile.getEvents().size());
        assertEquals("event1", profile.getEvents().get(0));
        assertEquals("event2", profile.getEvents().get(1));
    }
}
