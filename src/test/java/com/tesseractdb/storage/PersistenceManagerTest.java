package com.tesseractdb.storage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test cases for the PersistenceManager.
 */
public class PersistenceManagerTest {
    
    private File tempDir;
    private UserProfileStore userProfileStore;
    private EventStore eventStore;
    private PersistenceManager persistenceManager;
    
    @Before
    public void setUp() throws IOException {
        // Create a temporary directory for testing
        tempDir = new File("temp-test-data");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        
        // Create stores
        userProfileStore = new UserProfileStore();
        eventStore = new EventStore(userProfileStore);
        
        // Create persistence manager
        persistenceManager = new PersistenceManager(
                tempDir.getAbsolutePath(), userProfileStore, eventStore, 60000);
        
        // Create sample data
        createSampleData();
    }
    
    @After
    public void tearDown() {
        // Shutdown persistence manager
        persistenceManager.shutdown();
        
        // Delete temporary directory
        deleteDirectory(tempDir);
    }
    
    private void createSampleData() {
        // Create users
        for (int i = 1; i <= 3; i++) {
            String userId = "user" + i;
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("name", "User " + i);
            properties.put("email", "user" + i + "@example.com");
            
            userProfileStore.createProfile(userId, properties);
        }
        
        // Create events
        for (int i = 1; i <= 3; i++) {
            String userId = "user" + i;
            
            for (int j = 0; j < i; j++) {
                Map<String, Object> properties = new HashMap<>();
                properties.put("device", "device" + j);
                
                eventStore.addEvent("login", userId, properties, System.currentTimeMillis() - j * 1000);
            }
        }
    }
    
    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
    
    @Test
    public void testSaveAndLoadAll() throws IOException {
        // Save all data
        persistenceManager.saveAll();
        
        // Check that files were created
        File profilesDir = new File(tempDir, "profiles");
        File eventsDir = new File(tempDir, "events");
        
        assertTrue(profilesDir.exists());
        assertTrue(eventsDir.exists());
        
        // Create new stores
        UserProfileStore newUserProfileStore = new UserProfileStore();
        EventStore newEventStore = new EventStore(newUserProfileStore);
        
        // Create new persistence manager
        PersistenceManager newPersistenceManager = new PersistenceManager(
                tempDir.getAbsolutePath(), newUserProfileStore, newEventStore, 60000);
        
        // Load all data
        newPersistenceManager.loadAll();
        
        // Check that data was loaded
        // Note: In a real implementation, we would need a way to get all profiles and events
        // For now, we'll just check that we can get the profiles we created
        for (int i = 1; i <= 3; i++) {
            String userId = "user" + i;
            
            UserProfile profile = newUserProfileStore.getProfile(userId);
            assertNotNull(profile);
            assertEquals(userId, profile.getUserId());
            assertEquals("User " + i, profile.getProperties().get("name"));
            assertEquals("user" + i + "@example.com", profile.getProperties().get("email"));
        }
        
        // Shutdown new persistence manager
        newPersistenceManager.shutdown();
    }
    
    @Test
    public void testSaveEventsNCF() throws IOException {
        // Save events in NCF format
        persistenceManager.saveEventsNCF();
        
        // Check that NCF directory was created
        File ncfDir = new File(tempDir, "ncf");
        assertTrue(ncfDir.exists());
        
        // Check that NCF files were created
        File[] ncfFiles = ncfDir.listFiles((dir, name) -> name.endsWith(".ncf"));
        assertNotNull(ncfFiles);
        assertTrue(ncfFiles.length > 0);
    }
    
    @Test
    public void testApplyRetentionPolicy() throws IOException {
        // Save events in NCF format
        persistenceManager.saveEventsNCF();
        
        // Apply retention policy
        persistenceManager.applyRetentionPolicy(30);
        
        // Check that NCF directory still exists
        File ncfDir = new File(tempDir, "ncf");
        assertTrue(ncfDir.exists());
    }
}
