package com.tesseractdb.ml;

import com.tesseractdb.storage.Event;
import com.tesseractdb.storage.EventStore;
import com.tesseractdb.storage.UserProfile;
import com.tesseractdb.storage.UserProfileStore;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple predictive model for user behavior.
 */
public class PredictiveModel {
    
    private final UserProfileStore userProfileStore;
    private final EventStore eventStore;
    
    /**
     * Create a new predictive model.
     *
     * @param userProfileStore User profile store
     * @param eventStore Event store
     */
    public PredictiveModel(UserProfileStore userProfileStore, EventStore eventStore) {
        this.userProfileStore = userProfileStore;
        this.eventStore = eventStore;
    }
    
    /**
     * Predict the likelihood of a user performing an event.
     *
     * @param userId User ID
     * @param targetEventName Target event name
     * @return Probability between 0 and 1
     */
    public double predictEventLikelihood(String userId, String targetEventName) {
        UserProfile profile = userProfileStore.getProfile(userId);
        if (profile == null) {
            return 0.0;
        }
        
        // Get user's events
        List<Event> userEvents = eventStore.getUserEvents(userId);
        
        // Check if the user has already performed the target event
        boolean hasPerformedTarget = userEvents.stream()
                .anyMatch(event -> event.getEventName().equals(targetEventName));
        
        if (hasPerformedTarget) {
            // If the user has already performed the event, predict likelihood of repeating
            return predictRepeatLikelihood(userId, targetEventName);
        } else {
            // If the user hasn't performed the event, predict likelihood of first occurrence
            return predictFirstOccurrenceLikelihood(userId, targetEventName);
        }
    }
    
    /**
     * Predict the likelihood of a user repeating an event.
     *
     * @param userId User ID
     * @param eventName Event name
     * @return Probability between 0 and 1
     */
    private double predictRepeatLikelihood(String userId, String eventName) {
        // Get user's events of the target type
        List<Event> targetEvents = eventStore.getUserEventsByName(userId, eventName);
        
        if (targetEvents.isEmpty()) {
            return 0.0;
        }
        
        // Calculate frequency (events per day)
        long firstEventTime = targetEvents.stream()
                .mapToLong(Event::getTimestamp)
                .min()
                .orElse(0);
        
        long lastEventTime = targetEvents.stream()
                .mapToLong(Event::getTimestamp)
                .max()
                .orElse(0);
        
        long timeSpanDays = (lastEventTime - firstEventTime) / (24 * 60 * 60 * 1000L);
        timeSpanDays = Math.max(1, timeSpanDays); // Avoid division by zero
        
        double eventsPerDay = (double) targetEvents.size() / timeSpanDays;
        
        // Calculate recency (days since last event)
        long currentTime = System.currentTimeMillis();
        long daysSinceLastEvent = (currentTime - lastEventTime) / (24 * 60 * 60 * 1000L);
        
        // Calculate likelihood based on frequency and recency
        // Higher frequency and lower recency increase likelihood
        double frequencyFactor = Math.min(1.0, eventsPerDay);
        double recencyFactor = Math.exp(-0.1 * daysSinceLastEvent); // Exponential decay
        
        return 0.7 * frequencyFactor + 0.3 * recencyFactor;
    }
    
    /**
     * Predict the likelihood of a user performing an event for the first time.
     *
     * @param userId User ID
     * @param targetEventName Target event name
     * @return Probability between 0 and 1
     */
    private double predictFirstOccurrenceLikelihood(String userId, String targetEventName) {
        // Get similar users who have performed the target event
        List<UserSimilarity> similarUsers = findSimilarUsers(userId, 10);
        
        // Count how many similar users have performed the target event
        int performedCount = 0;
        double totalSimilarity = 0.0;
        
        for (UserSimilarity similarity : similarUsers) {
            String similarUserId = similarity.getUserId();
            double similarityScore = similarity.getSimilarity();
            
            List<Event> userEvents = eventStore.getUserEventsByName(similarUserId, targetEventName);
            
            if (!userEvents.isEmpty()) {
                performedCount++;
                totalSimilarity += similarityScore;
            }
        }
        
        if (similarUsers.isEmpty()) {
            return 0.0;
        }
        
        // Calculate likelihood based on similar users
        double overallSimilarity = similarUsers.stream()
                .mapToDouble(UserSimilarity::getSimilarity)
                .sum();
        
        return totalSimilarity / overallSimilarity;
    }
    
    /**
     * Find users similar to a given user.
     *
     * @param userId User ID
     * @param maxUsers Maximum number of similar users to return
     * @return List of similar users with similarity scores
     */
    private List<UserSimilarity> findSimilarUsers(String userId, int maxUsers) {
        UserProfile targetProfile = userProfileStore.getProfile(userId);
        if (targetProfile == null) {
            return Collections.emptyList();
        }
        
        // Get target user's events
        List<Event> targetEvents = eventStore.getUserEvents(userId);
        
        // Extract event names
        Set<String> targetEventNames = targetEvents.stream()
                .map(Event::getEventName)
                .collect(Collectors.toSet());
        
        // Get all user profiles
        // In a real implementation, we would need a way to get all profiles from the store
        // For now, we'll just use a placeholder
        List<UserProfile> allProfiles = new ArrayList<>();
        
        // Calculate similarity for each user
        List<UserSimilarity> similarities = new ArrayList<>();
        
        for (UserProfile profile : allProfiles) {
            String otherUserId = profile.getUserId();
            
            // Skip the target user
            if (otherUserId.equals(userId)) {
                continue;
            }
            
            // Get other user's events
            List<Event> otherEvents = eventStore.getUserEvents(otherUserId);
            
            // Extract event names
            Set<String> otherEventNames = otherEvents.stream()
                    .map(Event::getEventName)
                    .collect(Collectors.toSet());
            
            // Calculate Jaccard similarity: |A ∩ B| / |A ∪ B|
            Set<String> intersection = new HashSet<>(targetEventNames);
            intersection.retainAll(otherEventNames);
            
            Set<String> union = new HashSet<>(targetEventNames);
            union.addAll(otherEventNames);
            
            double similarity = (double) intersection.size() / union.size();
            
            similarities.add(new UserSimilarity(otherUserId, similarity));
        }
        
        // Sort by similarity and limit
        return similarities.stream()
                .sorted(Comparator.comparing(UserSimilarity::getSimilarity).reversed())
                .limit(maxUsers)
                .collect(Collectors.toList());
    }
    
    /**
     * Predict the best time to send a notification to a user.
     *
     * @param userId User ID
     * @return Best hour of day (0-23) or -1 if unknown
     */
    public int predictBestTimeToSend(String userId) {
        List<Event> userEvents = eventStore.getUserEvents(userId);
        
        if (userEvents.isEmpty()) {
            return -1;
        }
        
        // Count events by hour of day
        int[] hourCounts = new int[24];
        
        for (Event event : userEvents) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(event.getTimestamp());
            
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            hourCounts[hour]++;
        }
        
        // Find hour with most events
        int bestHour = 0;
        int maxCount = hourCounts[0];
        
        for (int hour = 1; hour < 24; hour++) {
            if (hourCounts[hour] > maxCount) {
                maxCount = hourCounts[hour];
                bestHour = hour;
            }
        }
        
        return bestHour;
    }
    
    /**
     * User with a similarity score.
     */
    private static class UserSimilarity {
        private final String userId;
        private final double similarity;
        
        public UserSimilarity(String userId, double similarity) {
            this.userId = userId;
            this.similarity = similarity;
        }
        
        public String getUserId() {
            return userId;
        }
        
        public double getSimilarity() {
            return similarity;
        }
    }
}
