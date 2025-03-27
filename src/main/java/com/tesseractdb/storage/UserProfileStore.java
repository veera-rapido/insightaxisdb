package com.tesseractdb.storage;

import java.util.HashMap;
import java.util.Map;

/**
 * Storage for user profiles.
 */
public class UserProfileStore {
    private final Map<String, UserProfile> profiles = new HashMap<>();
    
    /**
     * Get a user profile.
     *
     * @param userId User ID
     * @return User profile or null if not found
     */
    public UserProfile getProfile(String userId) {
        return profiles.get(userId);
    }
    
    /**
     * Create a new user profile.
     *
     * @param userId User ID
     * @param properties Initial user properties
     * @return Created user profile
     */
    public UserProfile createProfile(String userId, Map<String, Object> properties) {
        if (profiles.containsKey(userId)) {
            throw new IllegalArgumentException("User profile already exists: " + userId);
        }
        
        UserProfile profile = new UserProfile(userId, properties);
        profiles.put(userId, profile);
        return profile;
    }
    
    /**
     * Get or create a user profile.
     *
     * @param userId User ID
     * @param properties Initial user properties if creating
     * @return User profile and whether it was created
     */
    public ProfileResult getOrCreateProfile(String userId, Map<String, Object> properties) {
        UserProfile profile = getProfile(userId);
        if (profile != null) {
            return new ProfileResult(profile, false);
        }
        
        profile = createProfile(userId, properties);
        return new ProfileResult(profile, true);
    }
    
    /**
     * Update a user profile.
     *
     * @param userId User ID
     * @param properties Properties to update
     * @return Updated user profile or null if not found
     */
    public UserProfile updateProfile(String userId, Map<String, Object> properties) {
        UserProfile profile = getProfile(userId);
        if (profile == null) {
            return null;
        }
        
        profile.updateProperties(properties);
        return profile;
    }
    
    /**
     * Delete a user profile.
     *
     * @param userId User ID
     * @return Whether the profile was deleted
     */
    public boolean deleteProfile(String userId) {
        if (!profiles.containsKey(userId)) {
            return false;
        }
        
        profiles.remove(userId);
        return true;
    }
    
    /**
     * Result of getting or creating a profile.
     */
    public static class ProfileResult {
        private final UserProfile profile;
        private final boolean created;
        
        public ProfileResult(UserProfile profile, boolean created) {
            this.profile = profile;
            this.created = created;
        }
        
        public UserProfile getProfile() {
            return profile;
        }
        
        public boolean isCreated() {
            return created;
        }
    }
}
