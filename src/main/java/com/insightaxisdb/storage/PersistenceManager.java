package com.insightaxisdb.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Manager for persisting InsightAxisDB data to disk.
 */
public class PersistenceManager {
    
    private static final Logger logger = LoggerFactory.getLogger(PersistenceManager.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final String dataDirectory;
    private final UserProfileStore userProfileStore;
    private final EventStore eventStore;
    private final ScheduledExecutorService scheduler;
    private final Map<String, Long> lastSavedTimestamps = new ConcurrentHashMap<>();
    private final long saveIntervalMillis;
    
    /**
     * Create a new persistence manager.
     *
     * @param dataDirectory Directory to store data files
     * @param userProfileStore User profile store
     * @param eventStore Event store
     * @param saveIntervalMillis Interval between automatic saves in milliseconds
     */
    public PersistenceManager(String dataDirectory, UserProfileStore userProfileStore, 
                             EventStore eventStore, long saveIntervalMillis) {
        this.dataDirectory = dataDirectory;
        this.userProfileStore = userProfileStore;
        this.eventStore = eventStore;
        this.saveIntervalMillis = saveIntervalMillis;
        
        // Create data directory if it doesn't exist
        File directory = new File(dataDirectory);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new RuntimeException("Failed to create data directory: " + dataDirectory);
            }
        }
        
        // Initialize scheduler for automatic saving
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.scheduler.scheduleAtFixedRate(this::saveAll, saveIntervalMillis, saveIntervalMillis, 
                TimeUnit.MILLISECONDS);
    }
    
    /**
     * Save all data to disk.
     */
    public synchronized void saveAll() {
        try {
            saveUserProfiles();
            saveEvents();
            logger.info("Saved all data to disk");
        } catch (IOException e) {
            logger.error("Failed to save data to disk", e);
        }
    }
    
    /**
     * Save user profiles to disk.
     */
    private void saveUserProfiles() throws IOException {
        // Create profiles directory if it doesn't exist
        File profilesDir = new File(dataDirectory, "profiles");
        if (!profilesDir.exists()) {
            if (!profilesDir.mkdirs()) {
                throw new IOException("Failed to create profiles directory: " + profilesDir);
            }
        }
        
        // Get all user profiles
        // In a real implementation, we would need a way to get all profiles from the store
        // For now, we'll just use a placeholder
        List<UserProfile> profiles = new ArrayList<>();
        
        // Save each profile to a separate file
        for (UserProfile profile : profiles) {
            String userId = profile.getUserId();
            File profileFile = new File(profilesDir, userId + ".json");
            
            // Check if the profile has been modified since last save
            long lastModified = profile.getLastSeenAt();
            Long lastSaved = lastSavedTimestamps.get("profile:" + userId);
            
            if (lastSaved == null || lastModified > lastSaved) {
                // Save profile to file
                objectMapper.writeValue(profileFile, profile.toMap());
                
                // Update last saved timestamp
                lastSavedTimestamps.put("profile:" + userId, System.currentTimeMillis());
            }
        }
    }
    
    /**
     * Save events to disk.
     */
    private void saveEvents() throws IOException {
        // Create events directory if it doesn't exist
        File eventsDir = new File(dataDirectory, "events");
        if (!eventsDir.exists()) {
            if (!eventsDir.mkdirs()) {
                throw new IOException("Failed to create events directory: " + eventsDir);
            }
        }
        
        // Get all events
        // In a real implementation, we would need a way to get all events from the store
        // For now, we'll just use a placeholder
        List<Event> events = new ArrayList<>();
        
        // Group events by user ID
        Map<String, List<Event>> eventsByUser = events.stream()
                .collect(Collectors.groupingBy(Event::getUserId));
        
        // Save events for each user
        for (Map.Entry<String, List<Event>> entry : eventsByUser.entrySet()) {
            String userId = entry.getKey();
            List<Event> userEvents = entry.getValue();
            
            // Sort events by timestamp
            userEvents.sort(Comparator.comparingLong(Event::getTimestamp));
            
            // Convert events to maps
            List<Map<String, Object>> eventMaps = userEvents.stream()
                    .map(Event::toMap)
                    .collect(Collectors.toList());
            
            // Save events to file
            File eventsFile = new File(eventsDir, userId + ".json");
            objectMapper.writeValue(eventsFile, eventMaps);
            
            // Update last saved timestamp
            lastSavedTimestamps.put("events:" + userId, System.currentTimeMillis());
        }
    }
    
    /**
     * Save events in NCF format.
     */
    public void saveEventsNCF() throws IOException {
        // Create NCF directory if it doesn't exist
        File ncfDir = new File(dataDirectory, "ncf");
        if (!ncfDir.exists()) {
            if (!ncfDir.mkdirs()) {
                throw new IOException("Failed to create NCF directory: " + ncfDir);
            }
        }
        
        // Get all events
        // In a real implementation, we would need a way to get all events from the store
        // For now, we'll just use a placeholder
        List<Event> events = new ArrayList<>();
        
        // Group events by day
        Map<String, List<Event>> eventsByDay = events.stream()
                .collect(Collectors.groupingBy(event -> {
                    // Format timestamp as YYYY-MM-DD
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(event.getTimestamp());
                    return String.format("%04d-%02d-%02d", 
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH) + 1,
                            calendar.get(Calendar.DAY_OF_MONTH));
                }));
        
        // Save events for each day
        for (Map.Entry<String, List<Event>> entry : eventsByDay.entrySet()) {
            String day = entry.getKey();
            List<Event> dayEvents = entry.getValue();
            
            // Create NCF writer
            NCF.Writer writer = new NCF.Writer("lz4");
            
            // Add events to writer
            for (Event event : dayEvents) {
                Map<String, Object> row = new HashMap<>(event.getProperties());
                row.put("eventId", event.getEventId());
                row.put("eventName", event.getEventName());
                row.put("userId", event.getUserId());
                row.put("timestamp", event.getTimestamp());
                
                writer.addRow(row);
            }
            
            // Write to file
            File ncfFile = new File(ncfDir, "events-" + day + ".ncf");
            try (RandomAccessFile raf = new RandomAccessFile(ncfFile, "rw")) {
                writer.write(raf);
            }
        }
    }
    
    /**
     * Load all data from disk.
     */
    public synchronized void loadAll() {
        try {
            loadUserProfiles();
            loadEvents();
            logger.info("Loaded all data from disk");
        } catch (IOException e) {
            logger.error("Failed to load data from disk", e);
        }
    }
    
    /**
     * Load user profiles from disk.
     */
    private void loadUserProfiles() throws IOException {
        File profilesDir = new File(dataDirectory, "profiles");
        if (!profilesDir.exists()) {
            return;
        }
        
        // Load each profile file
        File[] profileFiles = profilesDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (profileFiles != null) {
            for (File profileFile : profileFiles) {
                try {
                    // Read profile from file
                    @SuppressWarnings("unchecked")
                    Map<String, Object> profileMap = objectMapper.readValue(profileFile, Map.class);
                    
                    // Create profile
                    UserProfile profile = UserProfile.fromMap(profileMap);
                    
                    // Add profile to store
                    // In a real implementation, we would need a way to add the profile to the store
                    // For now, we'll just log it
                    logger.info("Loaded profile: {}", profile.getUserId());
                } catch (IOException e) {
                    logger.error("Failed to load profile from file: " + profileFile, e);
                }
            }
        }
    }
    
    /**
     * Load events from disk.
     */
    private void loadEvents() throws IOException {
        File eventsDir = new File(dataDirectory, "events");
        if (!eventsDir.exists()) {
            return;
        }
        
        // Load each events file
        File[] eventsFiles = eventsDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (eventsFiles != null) {
            for (File eventsFile : eventsFiles) {
                try {
                    // Read events from file
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> eventMaps = objectMapper.readValue(eventsFile, List.class);
                    
                    // Create events
                    for (Map<String, Object> eventMap : eventMaps) {
                        Event event = Event.fromMap(eventMap);
                        
                        // Add event to store
                        // In a real implementation, we would need a way to add the event to the store
                        // For now, we'll just log it
                        logger.info("Loaded event: {}", event.getEventId());
                    }
                } catch (IOException e) {
                    logger.error("Failed to load events from file: " + eventsFile, e);
                }
            }
        }
    }
    
    /**
     * Load events from NCF files.
     */
    public void loadEventsNCF() throws IOException {
        File ncfDir = new File(dataDirectory, "ncf");
        if (!ncfDir.exists()) {
            return;
        }
        
        // Load each NCF file
        File[] ncfFiles = ncfDir.listFiles((dir, name) -> name.endsWith(".ncf"));
        if (ncfFiles != null) {
            for (File ncfFile : ncfFiles) {
                try (RandomAccessFile raf = new RandomAccessFile(ncfFile, "r");
                     NCF.Reader reader = new NCF.Reader(raf)) {
                    
                    // Read all rows
                    List<Map<String, Object>> rows = reader.readRows(0, null);
                    
                    // Create events
                    for (Map<String, Object> row : rows) {
                        String eventId = (String) row.get("eventId");
                        String eventName = (String) row.get("eventName");
                        String userId = (String) row.get("userId");
                        Long timestamp = (Long) row.get("timestamp");
                        
                        // Remove metadata fields
                        Map<String, Object> properties = new HashMap<>(row);
                        properties.remove("eventId");
                        properties.remove("eventName");
                        properties.remove("userId");
                        properties.remove("timestamp");
                        
                        // Create event
                        Event event = new Event(eventName, userId, properties, timestamp, eventId);
                        
                        // Add event to store
                        // In a real implementation, we would need a way to add the event to the store
                        // For now, we'll just log it
                        logger.info("Loaded event from NCF: {}", event.getEventId());
                    }
                } catch (IOException e) {
                    logger.error("Failed to load events from NCF file: " + ncfFile, e);
                }
            }
        }
    }
    
    /**
     * Implement data retention policy.
     *
     * @param retentionDays Number of days to retain data
     */
    public void applyRetentionPolicy(int retentionDays) {
        long cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L);
        
        try {
            // Delete old NCF files
            File ncfDir = new File(dataDirectory, "ncf");
            if (ncfDir.exists()) {
                File[] ncfFiles = ncfDir.listFiles((dir, name) -> name.endsWith(".ncf"));
                if (ncfFiles != null) {
                    for (File ncfFile : ncfFiles) {
                        // Extract date from filename (format: events-YYYY-MM-DD.ncf)
                        String filename = ncfFile.getName();
                        if (filename.startsWith("events-") && filename.length() >= 16) {
                            String dateStr = filename.substring(7, 17); // YYYY-MM-DD
                            try {
                                Calendar calendar = Calendar.getInstance();
                                String[] dateParts = dateStr.split("-");
                                calendar.set(
                                        Integer.parseInt(dateParts[0]),
                                        Integer.parseInt(dateParts[1]) - 1,
                                        Integer.parseInt(dateParts[2])
                                );
                                
                                if (calendar.getTimeInMillis() < cutoffTime) {
                                    if (ncfFile.delete()) {
                                        logger.info("Deleted old NCF file: {}", ncfFile);
                                    } else {
                                        logger.warn("Failed to delete old NCF file: {}", ncfFile);
                                    }
                                }
                            } catch (Exception e) {
                                logger.error("Failed to parse date from filename: " + filename, e);
                            }
                        }
                    }
                }
            }
            
            // In a real implementation, we would also need to delete old events and profiles
            // from the in-memory stores
            
        } catch (Exception e) {
            logger.error("Failed to apply retention policy", e);
        }
    }
    
    /**
     * Shut down the persistence manager.
     */
    public void shutdown() {
        // Save all data before shutting down
        saveAll();
        
        // Shut down scheduler
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
