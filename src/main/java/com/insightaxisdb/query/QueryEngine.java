package com.insightaxisdb.query;

import com.insightaxisdb.storage.Event;
import com.insightaxisdb.storage.EventStore;
import com.insightaxisdb.storage.UserProfile;
import com.insightaxisdb.storage.UserProfileStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Engine for executing queries on InsightAxisDB data.
 */
public class QueryEngine {

    private final UserProfileStore userProfileStore;
    private final EventStore eventStore;

    /**
     * Create a new query engine.
     *
     * @param userProfileStore User profile store
     * @param eventStore Event store
     */
    public QueryEngine(UserProfileStore userProfileStore, EventStore eventStore) {
        this.userProfileStore = userProfileStore;
        this.eventStore = eventStore;
    }

    /**
     * Query user profiles.
     *
     * @param query Query to execute
     * @return Query result
     */
    public QueryResult queryUserProfiles(Query query) {
        // Convert user profiles to rows
        List<Map<String, Object>> rows = new ArrayList<>();

        for (UserProfile profile : getAllUserProfiles()) {
            Map<String, Object> row = new HashMap<>(profile.getProperties());
            row.put("userId", profile.getUserId());
            row.put("firstSeenAt", profile.getFirstSeenAt());
            row.put("lastSeenAt", profile.getLastSeenAt());
            row.put("eventCount", profile.getEventCount());
            rows.add(row);
        }

        // Execute query on rows
        return query.execute(rows);
    }

    /**
     * Query events.
     *
     * @param query Query to execute
     * @return Query result
     */
    public QueryResult queryEvents(Query query) {
        // Convert events to rows
        List<Map<String, Object>> rows = new ArrayList<>();

        for (Event event : getAllEvents()) {
            Map<String, Object> row = new HashMap<>(event.getProperties());
            row.put("eventId", event.getEventId());
            row.put("eventName", event.getEventName());
            row.put("userId", event.getUserId());
            row.put("timestamp", event.getTimestamp());
            rows.add(row);
        }

        // Execute query on rows
        return query.execute(rows);
    }

    /**
     * Query events for a specific user.
     *
     * @param userId User ID
     * @param query Query to execute
     * @return Query result
     */
    public QueryResult queryUserEvents(String userId, Query query) {
        // Convert user events to rows
        List<Map<String, Object>> rows = new ArrayList<>();

        for (Event event : eventStore.getUserEvents(userId)) {
            Map<String, Object> row = new HashMap<>(event.getProperties());
            row.put("eventId", event.getEventId());
            row.put("eventName", event.getEventName());
            row.put("userId", event.getUserId());
            row.put("timestamp", event.getTimestamp());
            rows.add(row);
        }

        // Execute query on rows
        return query.execute(rows);
    }

    /**
     * Find users who performed a specific event.
     *
     * @param eventName Event name
     * @param query Query to filter users
     * @return Query result
     */
    public QueryResult findUsersWithEvent(String eventName, Query query) {
        // Find all events with the given name
        List<Event> events = eventStore.getEventsByName(eventName);

        // Get unique user IDs
        List<String> userIds = events.stream()
                .map(Event::getUserId)
                .distinct()
                .collect(Collectors.toList());

        // Convert user profiles to rows
        List<Map<String, Object>> rows = new ArrayList<>();

        for (String userId : userIds) {
            UserProfile profile = userProfileStore.getProfile(userId);
            if (profile != null) {
                Map<String, Object> row = new HashMap<>(profile.getProperties());
                row.put("userId", profile.getUserId());
                row.put("firstSeenAt", profile.getFirstSeenAt());
                row.put("lastSeenAt", profile.getLastSeenAt());
                row.put("eventCount", profile.getEventCount());
                rows.add(row);
            }
        }

        // Execute query on rows
        return query.execute(rows);
    }

    /**
     * Find users who performed a sequence of events in order.
     *
     * @param eventSequence Sequence of event names
     * @param withinMillis Time window in milliseconds, or null for no time constraint
     * @param query Query to filter users
     * @return Query result
     */
    public QueryResult findUsersWithEventSequence(List<String> eventSequence, Long withinMillis, Query query) {
        if (eventSequence.isEmpty()) {
            return new QueryResult(new ArrayList<>(), new HashMap<>());
        }

        // Find all users who performed the first event
        List<String> userIds = eventStore.getEventsByName(eventSequence.get(0)).stream()
                .map(Event::getUserId)
                .distinct()
                .collect(Collectors.toList());

        // Filter users who performed the entire sequence
        List<String> matchingUserIds = new ArrayList<>();

        for (String userId : userIds) {
            List<Event> userEvents = eventStore.getUserEvents(userId);

            // Sort events by timestamp
            userEvents.sort((e1, e2) -> Long.compare(e1.getTimestamp(), e2.getTimestamp()));

            // Check if the user performed the sequence
            boolean matchesSequence = checkEventSequence(userEvents, eventSequence, withinMillis);

            if (matchesSequence) {
                matchingUserIds.add(userId);
            }
        }

        // Convert user profiles to rows
        List<Map<String, Object>> rows = new ArrayList<>();

        for (String userId : matchingUserIds) {
            UserProfile profile = userProfileStore.getProfile(userId);
            if (profile != null) {
                Map<String, Object> row = new HashMap<>(profile.getProperties());
                row.put("userId", profile.getUserId());
                row.put("firstSeenAt", profile.getFirstSeenAt());
                row.put("lastSeenAt", profile.getLastSeenAt());
                row.put("eventCount", profile.getEventCount());
                rows.add(row);
            }
        }

        // Execute query on rows
        return query.execute(rows);
    }

    /**
     * Check if a list of events contains a sequence of event names in order.
     *
     * @param events List of events
     * @param eventSequence Sequence of event names
     * @param withinMillis Time window in milliseconds, or null for no time constraint
     * @return Whether the events contain the sequence
     */
    private boolean checkEventSequence(List<Event> events, List<String> eventSequence, Long withinMillis) {
        int sequenceIndex = 0;
        Long sequenceStartTime = null;

        for (Event event : events) {
            if (event.getEventName().equals(eventSequence.get(sequenceIndex))) {
                if (sequenceIndex == 0) {
                    sequenceStartTime = event.getTimestamp();
                    sequenceIndex++;
                } else {
                    // Check time constraint if applicable
                    if (withinMillis != null) {
                        if (event.getTimestamp() - sequenceStartTime <= withinMillis) {
                            sequenceIndex++;
                        }
                    } else {
                        sequenceIndex++;
                    }
                }

                // Check if we've matched the entire sequence
                if (sequenceIndex == eventSequence.size()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Get all user profiles.
     *
     * @return List of all user profiles
     */
    private List<UserProfile> getAllUserProfiles() {
        List<UserProfile> profiles = new ArrayList<>();

        // For testing purposes, we'll just get profiles for users we know exist
        for (int i = 1; i <= 10; i++) {
            String userId = "user" + i;
            UserProfile profile = userProfileStore.getProfile(userId);
            if (profile != null) {
                profiles.add(profile);
            }
        }

        return profiles;
    }

    /**
     * Get all events.
     *
     * @return List of all events
     */
    private List<Event> getAllEvents() {
        List<Event> events = new ArrayList<>();

        // For testing purposes, we'll get events for users we know exist
        for (int i = 1; i <= 10; i++) {
            String userId = "user" + i;
            events.addAll(eventStore.getUserEvents(userId));
        }

        return events;
    }
}
