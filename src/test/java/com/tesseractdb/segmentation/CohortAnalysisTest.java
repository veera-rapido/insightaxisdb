package com.tesseractdb.segmentation;

import com.tesseractdb.storage.EventStore;
import com.tesseractdb.storage.UserProfileStore;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test cases for the CohortAnalysis class.
 */
public class CohortAnalysisTest {
    
    private UserProfileStore userProfileStore;
    private EventStore eventStore;
    private CohortAnalysis cohortAnalysis;
    
    @Before
    public void setUp() {
        userProfileStore = new UserProfileStore();
        eventStore = new EventStore(userProfileStore);
        cohortAnalysis = new CohortAnalysis(userProfileStore, eventStore);
        
        // Create sample data
        createSampleData();
    }
    
    private void createSampleData() {
        // Create users in different cohorts
        long now = System.currentTimeMillis();
        long dayInMillis = 24 * 60 * 60 * 1000L;
        
        // Cohort 1 (Week 1)
        for (int i = 1; i <= 3; i++) {
            String userId = "cohort1_user" + i;
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("name", "Cohort 1 User " + i);
            
            // Create profile with first seen in week 1
            userProfileStore.createProfile(userId, properties);
            
            // Override first seen time
            userProfileStore.getProfile(userId).updateProperty("firstSeenAt", now - 3 * 7 * dayInMillis);
            
            // Add login events in week 1
            eventStore.addEvent("login", userId, null, now - 3 * 7 * dayInMillis);
            
            // Add login events in week 2
            if (i <= 2) {
                eventStore.addEvent("login", userId, null, now - 2 * 7 * dayInMillis);
            }
            
            // Add login events in week 3
            if (i <= 1) {
                eventStore.addEvent("login", userId, null, now - 1 * 7 * dayInMillis);
            }
        }
        
        // Cohort 2 (Week 2)
        for (int i = 1; i <= 4; i++) {
            String userId = "cohort2_user" + i;
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("name", "Cohort 2 User " + i);
            
            // Create profile with first seen in week 2
            userProfileStore.createProfile(userId, properties);
            
            // Override first seen time
            userProfileStore.getProfile(userId).updateProperty("firstSeenAt", now - 2 * 7 * dayInMillis);
            
            // Add login events in week 2
            eventStore.addEvent("login", userId, null, now - 2 * 7 * dayInMillis);
            
            // Add login events in week 3
            if (i <= 3) {
                eventStore.addEvent("login", userId, null, now - 1 * 7 * dayInMillis);
            }
        }
        
        // Cohort 3 (Week 3)
        for (int i = 1; i <= 5; i++) {
            String userId = "cohort3_user" + i;
            
            Map<String, Object> properties = new HashMap<>();
            properties.put("name", "Cohort 3 User " + i);
            
            // Create profile with first seen in week 3
            userProfileStore.createProfile(userId, properties);
            
            // Override first seen time
            userProfileStore.getProfile(userId).updateProperty("firstSeenAt", now - 1 * 7 * dayInMillis);
            
            // Add login events in week 3
            eventStore.addEvent("login", userId, null, now - 1 * 7 * dayInMillis);
        }
    }
    
    @Test
    public void testCalculateRetentionCohorts() {
        // Calculate retention cohorts
        CohortAnalysis.CohortResult result = cohortAnalysis.calculateRetentionCohorts(
                CohortAnalysis.TimePeriod.WEEK, 4, "login");
        
        // Check result
        assertNotNull(result);
        assertEquals(CohortAnalysis.TimePeriod.WEEK, result.getTimePeriod());
        assertEquals(4, result.getNumPeriods());
        
        // Check cohort sizes
        assertEquals(3, result.getCohortSize(0));
        assertEquals(4, result.getCohortSize(1));
        assertEquals(5, result.getCohortSize(2));
        
        // Check retention matrix
        // Cohort 1, Week 1: 3 users
        assertEquals(3, result.getRetention(0, 0));
        
        // Cohort 1, Week 2: 2 users
        assertEquals(2, result.getRetention(0, 1));
        
        // Cohort 1, Week 3: 1 user
        assertEquals(1, result.getRetention(0, 2));
        
        // Cohort 2, Week 2: 4 users
        assertEquals(4, result.getRetention(1, 0));
        
        // Cohort 2, Week 3: 3 users
        assertEquals(3, result.getRetention(1, 1));
        
        // Cohort 3, Week 3: 5 users
        assertEquals(5, result.getRetention(2, 0));
        
        // Check retention percentages
        // Cohort 1, Week 1: 100%
        assertEquals(1.0, result.getRetentionPercentage(0, 0), 0.01);
        
        // Cohort 1, Week 2: 67%
        assertEquals(0.67, result.getRetentionPercentage(0, 1), 0.01);
        
        // Cohort 1, Week 3: 33%
        assertEquals(0.33, result.getRetentionPercentage(0, 2), 0.01);
        
        // Cohort 2, Week 2: 100%
        assertEquals(1.0, result.getRetentionPercentage(1, 0), 0.01);
        
        // Cohort 2, Week 3: 75%
        assertEquals(0.75, result.getRetentionPercentage(1, 1), 0.01);
        
        // Cohort 3, Week 3: 100%
        assertEquals(1.0, result.getRetentionPercentage(2, 0), 0.01);
    }
    
    @Test
    public void testGetUsersInCohort() {
        // Calculate retention cohorts
        CohortAnalysis.CohortResult result = cohortAnalysis.calculateRetentionCohorts(
                CohortAnalysis.TimePeriod.WEEK, 4, "login");
        
        // Get users in cohort 0
        assertEquals(3, result.getUsersInCohort(0).size());
        
        // Get users in cohort 1
        assertEquals(4, result.getUsersInCohort(1).size());
        
        // Get users in cohort 2
        assertEquals(5, result.getUsersInCohort(2).size());
        
        // Get users in non-existent cohort
        assertTrue(result.getUsersInCohort(10).isEmpty());
    }
}
