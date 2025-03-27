package com.insightaxisdb.segmentation;

import com.insightaxisdb.storage.Event;
import com.insightaxisdb.storage.EventStore;
import com.insightaxisdb.storage.UserProfile;
import com.insightaxisdb.storage.UserProfileStore;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Cohort analysis for user segmentation.
 */
public class CohortAnalysis {
    
    private final UserProfileStore userProfileStore;
    private final EventStore eventStore;
    
    /**
     * Create a new cohort analysis.
     *
     * @param userProfileStore User profile store
     * @param eventStore Event store
     */
    public CohortAnalysis(UserProfileStore userProfileStore, EventStore eventStore) {
        this.userProfileStore = userProfileStore;
        this.eventStore = eventStore;
    }
    
    /**
     * Time period for cohort analysis.
     */
    public enum TimePeriod {
        DAY,
        WEEK,
        MONTH,
        QUARTER,
        YEAR
    }
    
    /**
     * Calculate retention cohorts.
     *
     * @param timePeriod Time period for cohorts
     * @param numPeriods Number of periods to analyze
     * @param targetEventName Name of the event to track for retention
     * @return Cohort analysis results
     */
    public CohortResult calculateRetentionCohorts(TimePeriod timePeriod, int numPeriods, String targetEventName) {
        // Get current time
        long currentTime = System.currentTimeMillis();
        
        // Calculate period length in milliseconds
        long periodLength = getPeriodLength(timePeriod);
        
        // Calculate start time for analysis
        long startTime = currentTime - (numPeriods * periodLength);
        
        // Get all user profiles
        // In a real implementation, we would need a way to get all profiles from the store
        // For now, we'll just use a placeholder
        List<UserProfile> profiles = new ArrayList<>();
        
        // Group users by acquisition period
        Map<Integer, List<String>> cohorts = new HashMap<>();
        
        for (UserProfile profile : profiles) {
            long firstSeen = profile.getFirstSeenAt();
            
            // Skip users acquired before the start time
            if (firstSeen < startTime) {
                continue;
            }
            
            // Calculate period index
            int periodIndex = (int) ((firstSeen - startTime) / periodLength);
            
            // Skip users acquired after the last period
            if (periodIndex >= numPeriods) {
                continue;
            }
            
            // Add user to cohort
            cohorts.computeIfAbsent(periodIndex, k -> new ArrayList<>()).add(profile.getUserId());
        }
        
        // Calculate retention for each cohort
        int[][] retentionMatrix = new int[numPeriods][numPeriods];
        
        for (int cohortIndex = 0; cohortIndex < numPeriods; cohortIndex++) {
            List<String> cohortUsers = cohorts.getOrDefault(cohortIndex, Collections.emptyList());
            
            if (cohortUsers.isEmpty()) {
                continue;
            }
            
            // Set cohort size
            retentionMatrix[cohortIndex][0] = cohortUsers.size();
            
            // Calculate retention for each period
            for (int periodIndex = 1; periodIndex < numPeriods - cohortIndex; periodIndex++) {
                long periodStart = startTime + (cohortIndex + periodIndex) * periodLength;
                long periodEnd = periodStart + periodLength;
                
                // Count users who performed the target event in this period
                int activeUsers = 0;
                
                for (String userId : cohortUsers) {
                    List<Event> userEvents = eventStore.getUserEventsByName(userId, targetEventName);
                    
                    boolean activeInPeriod = userEvents.stream()
                            .anyMatch(event -> event.getTimestamp() >= periodStart && 
                                              event.getTimestamp() < periodEnd);
                    
                    if (activeInPeriod) {
                        activeUsers++;
                    }
                }
                
                retentionMatrix[cohortIndex][periodIndex] = activeUsers;
            }
        }
        
        // Calculate retention percentages
        double[][] retentionPercentages = new double[numPeriods][numPeriods];
        
        for (int cohortIndex = 0; cohortIndex < numPeriods; cohortIndex++) {
            int cohortSize = retentionMatrix[cohortIndex][0];
            
            if (cohortSize > 0) {
                for (int periodIndex = 0; periodIndex < numPeriods - cohortIndex; periodIndex++) {
                    retentionPercentages[cohortIndex][periodIndex] = 
                            (double) retentionMatrix[cohortIndex][periodIndex] / cohortSize;
                }
            }
        }
        
        return new CohortResult(timePeriod, numPeriods, cohorts, retentionMatrix, retentionPercentages);
    }
    
    /**
     * Get the length of a time period in milliseconds.
     *
     * @param timePeriod Time period
     * @return Period length in milliseconds
     */
    private long getPeriodLength(TimePeriod timePeriod) {
        switch (timePeriod) {
            case DAY:
                return 24 * 60 * 60 * 1000L;
            case WEEK:
                return 7 * 24 * 60 * 60 * 1000L;
            case MONTH:
                return 30 * 24 * 60 * 60 * 1000L;
            case QUARTER:
                return 90 * 24 * 60 * 60 * 1000L;
            case YEAR:
                return 365 * 24 * 60 * 60 * 1000L;
            default:
                throw new IllegalArgumentException("Unknown time period: " + timePeriod);
        }
    }
    
    /**
     * Format a timestamp as a period label.
     *
     * @param timestamp Timestamp
     * @param timePeriod Time period
     * @return Period label
     */
    private String formatPeriodLabel(long timestamp, TimePeriod timePeriod) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        
        switch (timePeriod) {
            case DAY:
                return String.format("%04d-%02d-%02d", 
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH) + 1,
                        calendar.get(Calendar.DAY_OF_MONTH));
            case WEEK:
                return String.format("%04d-W%02d", 
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.WEEK_OF_YEAR));
            case MONTH:
                return String.format("%04d-%02d", 
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH) + 1);
            case QUARTER:
                int quarter = (calendar.get(Calendar.MONTH) / 3) + 1;
                return String.format("%04d-Q%d", 
                        calendar.get(Calendar.YEAR),
                        quarter);
            case YEAR:
                return String.format("%04d", 
                        calendar.get(Calendar.YEAR));
            default:
                throw new IllegalArgumentException("Unknown time period: " + timePeriod);
        }
    }
    
    /**
     * Result of a cohort analysis.
     */
    public static class CohortResult {
        private final TimePeriod timePeriod;
        private final int numPeriods;
        private final Map<Integer, List<String>> cohorts;
        private final int[][] retentionMatrix;
        private final double[][] retentionPercentages;
        
        public CohortResult(TimePeriod timePeriod, int numPeriods, 
                           Map<Integer, List<String>> cohorts,
                           int[][] retentionMatrix, 
                           double[][] retentionPercentages) {
            this.timePeriod = timePeriod;
            this.numPeriods = numPeriods;
            this.cohorts = cohorts;
            this.retentionMatrix = retentionMatrix;
            this.retentionPercentages = retentionPercentages;
        }
        
        public TimePeriod getTimePeriod() {
            return timePeriod;
        }
        
        public int getNumPeriods() {
            return numPeriods;
        }
        
        public Map<Integer, List<String>> getCohorts() {
            return cohorts;
        }
        
        public int[][] getRetentionMatrix() {
            return retentionMatrix;
        }
        
        public double[][] getRetentionPercentages() {
            return retentionPercentages;
        }
        
        public int getCohortSize(int cohortIndex) {
            if (cohortIndex < 0 || cohortIndex >= numPeriods) {
                return 0;
            }
            
            return retentionMatrix[cohortIndex][0];
        }
        
        public int getRetention(int cohortIndex, int periodIndex) {
            if (cohortIndex < 0 || cohortIndex >= numPeriods || 
                periodIndex < 0 || periodIndex >= numPeriods - cohortIndex) {
                return 0;
            }
            
            return retentionMatrix[cohortIndex][periodIndex];
        }
        
        public double getRetentionPercentage(int cohortIndex, int periodIndex) {
            if (cohortIndex < 0 || cohortIndex >= numPeriods || 
                periodIndex < 0 || periodIndex >= numPeriods - cohortIndex) {
                return 0.0;
            }
            
            return retentionPercentages[cohortIndex][periodIndex];
        }
        
        public List<String> getUsersInCohort(int cohortIndex) {
            return cohorts.getOrDefault(cohortIndex, Collections.emptyList());
        }
    }
}
