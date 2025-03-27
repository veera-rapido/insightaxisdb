package com.tesseractdb.segmentation;

import com.tesseractdb.storage.Event;
import com.tesseractdb.storage.EventStore;
import com.tesseractdb.storage.UserProfile;
import com.tesseractdb.storage.UserProfileStore;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RFM (Recency, Frequency, Monetary) analysis for user segmentation.
 */
public class RFMAnalysis {

    private final UserProfileStore userProfileStore;
    private final EventStore eventStore;
    private final String purchaseEventName;
    private final String monetaryValueField;

    /**
     * Create a new RFM analysis.
     *
     * @param userProfileStore User profile store
     * @param eventStore Event store
     * @param purchaseEventName Name of the purchase event
     * @param monetaryValueField Name of the field containing the monetary value
     */
    public RFMAnalysis(UserProfileStore userProfileStore, EventStore eventStore,
                      String purchaseEventName, String monetaryValueField) {
        this.userProfileStore = userProfileStore;
        this.eventStore = eventStore;
        this.purchaseEventName = purchaseEventName;
        this.monetaryValueField = monetaryValueField;
    }

    /**
     * Calculate RFM scores for all users.
     *
     * @param recencyDays Number of days to consider for recency
     * @param numSegments Number of segments to divide users into (typically 5)
     * @return Map of user ID to RFM score
     */
    public Map<String, RFMScore> calculateRFMScores(int recencyDays, int numSegments) {
        // Get current time
        long currentTime = System.currentTimeMillis();
        long recencyCutoff = currentTime - (recencyDays * 24 * 60 * 60 * 1000L);

        // Get all purchase events
        List<Event> purchaseEvents = eventStore.getEventsByName(purchaseEventName);

        // Group events by user
        Map<String, List<Event>> eventsByUser = purchaseEvents.stream()
                .collect(Collectors.groupingBy(Event::getUserId));

        // Calculate raw RFM values for each user
        Map<String, RFMValues> rfmValuesByUser = new HashMap<>();

        for (Map.Entry<String, List<Event>> entry : eventsByUser.entrySet()) {
            String userId = entry.getKey();
            List<Event> userEvents = entry.getValue();

            // Calculate recency (time since last purchase)
            long lastPurchaseTime = userEvents.stream()
                    .mapToLong(Event::getTimestamp)
                    .max()
                    .orElse(0);
            long recency = currentTime - lastPurchaseTime;

            // Calculate frequency (number of purchases)
            int frequency = userEvents.size();

            // Calculate monetary value (total amount spent)
            double monetary = userEvents.stream()
                    .mapToDouble(event -> {
                        Object value = event.getProperties().get(monetaryValueField);
                        if (value instanceof Number) {
                            return ((Number) value).doubleValue();
                        }
                        return 0.0;
                    })
                    .sum();

            rfmValuesByUser.put(userId, new RFMValues(recency, frequency, monetary));
        }

        // Calculate RFM scores
        return calculateScores(rfmValuesByUser, numSegments);
    }

    /**
     * Calculate RFM scores from raw values.
     *
     * @param rfmValuesByUser Map of user ID to raw RFM values
     * @param numSegments Number of segments to divide users into
     * @return Map of user ID to RFM score
     */
    private Map<String, RFMScore> calculateScores(Map<String, RFMValues> rfmValuesByUser, int numSegments) {
        // Extract values for each dimension
        List<Long> recencyValues = new ArrayList<>();
        List<Integer> frequencyValues = new ArrayList<>();
        List<Double> monetaryValues = new ArrayList<>();

        for (RFMValues values : rfmValuesByUser.values()) {
            recencyValues.add(values.getRecency());
            frequencyValues.add(values.getFrequency());
            monetaryValues.add(values.getMonetary());
        }

        // Sort values
        Collections.sort(recencyValues);
        Collections.sort(frequencyValues);
        Collections.sort(monetaryValues);

        // Calculate segment boundaries
        Long[] recencyBoundaries = calculateBoundariesLong(recencyValues, numSegments);
        Integer[] frequencyBoundaries = calculateBoundariesInteger(frequencyValues, numSegments);
        Double[] monetaryBoundaries = calculateBoundariesDouble(monetaryValues, numSegments);

        // Calculate scores for each user
        Map<String, RFMScore> scores = new HashMap<>();

        for (Map.Entry<String, RFMValues> entry : rfmValuesByUser.entrySet()) {
            String userId = entry.getKey();
            RFMValues values = entry.getValue();

            // Calculate scores (1 to numSegments)
            // Note: Recency is inverted (lower is better)
            int recencyScore = numSegments - getSegment(values.getRecency(), recencyBoundaries, numSegments) + 1;
            int frequencyScore = getSegment(values.getFrequency(), frequencyBoundaries, numSegments);
            int monetaryScore = getSegment(values.getMonetary(), monetaryBoundaries, numSegments);

            scores.put(userId, new RFMScore(recencyScore, frequencyScore, monetaryScore));
        }

        return scores;
    }

    /**
     * Calculate segment boundaries for a list of Long values.
     *
     * @param values List of values
     * @param numSegments Number of segments
     * @return Array of segment boundaries
     */
    private Long[] calculateBoundariesLong(List<Long> values, int numSegments) {
        if (values.isEmpty()) {
            return new Long[numSegments - 1];
        }

        Long[] boundaries = new Long[numSegments - 1];

        for (int i = 1; i < numSegments; i++) {
            int index = (int) Math.ceil((double) i * values.size() / numSegments) - 1;
            boundaries[i - 1] = values.get(Math.min(index, values.size() - 1));
        }

        return boundaries;
    }

    /**
     * Calculate segment boundaries for a list of Integer values.
     *
     * @param values List of values
     * @param numSegments Number of segments
     * @return Array of segment boundaries
     */
    private Integer[] calculateBoundariesInteger(List<Integer> values, int numSegments) {
        if (values.isEmpty()) {
            return new Integer[numSegments - 1];
        }

        Integer[] boundaries = new Integer[numSegments - 1];

        for (int i = 1; i < numSegments; i++) {
            int index = (int) Math.ceil((double) i * values.size() / numSegments) - 1;
            boundaries[i - 1] = values.get(Math.min(index, values.size() - 1));
        }

        return boundaries;
    }

    /**
     * Calculate segment boundaries for a list of Double values.
     *
     * @param values List of values
     * @param numSegments Number of segments
     * @return Array of segment boundaries
     */
    private Double[] calculateBoundariesDouble(List<Double> values, int numSegments) {
        if (values.isEmpty()) {
            return new Double[numSegments - 1];
        }

        Double[] boundaries = new Double[numSegments - 1];

        for (int i = 1; i < numSegments; i++) {
            int index = (int) Math.ceil((double) i * values.size() / numSegments) - 1;
            boundaries[i - 1] = values.get(Math.min(index, values.size() - 1));
        }

        return boundaries;
    }

    /**
     * Get the segment for a value.
     *
     * @param value Value to get segment for
     * @param boundaries Segment boundaries
     * @param numSegments Number of segments
     * @return Segment (1 to numSegments)
     */
    private <T extends Comparable<T>> int getSegment(T value, T[] boundaries, int numSegments) {
        for (int i = 0; i < boundaries.length; i++) {
            if (value.compareTo(boundaries[i]) <= 0) {
                return i + 1;
            }
        }
        return numSegments;
    }

    /**
     * Get users in a specific RFM segment.
     *
     * @param scores Map of user ID to RFM score
     * @param recencyScore Recency score to match
     * @param frequencyScore Frequency score to match
     * @param monetaryScore Monetary score to match
     * @return List of user IDs in the segment
     */
    public List<String> getUsersInSegment(Map<String, RFMScore> scores,
                                         int recencyScore, int frequencyScore, int monetaryScore) {
        return scores.entrySet().stream()
                .filter(entry -> {
                    RFMScore score = entry.getValue();
                    return score.getRecencyScore() == recencyScore &&
                           score.getFrequencyScore() == frequencyScore &&
                           score.getMonetaryScore() == monetaryScore;
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Get users in a specific RFM segment range.
     *
     * @param scores Map of user ID to RFM score
     * @param minRecencyScore Minimum recency score
     * @param maxRecencyScore Maximum recency score
     * @param minFrequencyScore Minimum frequency score
     * @param maxFrequencyScore Maximum frequency score
     * @param minMonetaryScore Minimum monetary score
     * @param maxMonetaryScore Maximum monetary score
     * @return List of user IDs in the segment range
     */
    public List<String> getUsersInSegmentRange(Map<String, RFMScore> scores,
                                              int minRecencyScore, int maxRecencyScore,
                                              int minFrequencyScore, int maxFrequencyScore,
                                              int minMonetaryScore, int maxMonetaryScore) {
        return scores.entrySet().stream()
                .filter(entry -> {
                    RFMScore score = entry.getValue();
                    return score.getRecencyScore() >= minRecencyScore &&
                           score.getRecencyScore() <= maxRecencyScore &&
                           score.getFrequencyScore() >= minFrequencyScore &&
                           score.getFrequencyScore() <= maxFrequencyScore &&
                           score.getMonetaryScore() >= minMonetaryScore &&
                           score.getMonetaryScore() <= maxMonetaryScore;
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Get high-value customers (high in all dimensions).
     *
     * @param scores Map of user ID to RFM score
     * @param numSegments Number of segments
     * @return List of high-value user IDs
     */
    public List<String> getHighValueCustomers(Map<String, RFMScore> scores, int numSegments) {
        int threshold = (int) Math.ceil(0.8 * numSegments);

        return scores.entrySet().stream()
                .filter(entry -> {
                    RFMScore score = entry.getValue();
                    return score.getRecencyScore() >= threshold &&
                           score.getFrequencyScore() >= threshold &&
                           score.getMonetaryScore() >= threshold;
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Get at-risk customers (high monetary and frequency, but low recency).
     *
     * @param scores Map of user ID to RFM score
     * @param numSegments Number of segments
     * @return List of at-risk user IDs
     */
    public List<String> getAtRiskCustomers(Map<String, RFMScore> scores, int numSegments) {
        int highThreshold = (int) Math.ceil(0.8 * numSegments);
        int lowThreshold = (int) Math.ceil(0.2 * numSegments);

        return scores.entrySet().stream()
                .filter(entry -> {
                    RFMScore score = entry.getValue();
                    return score.getRecencyScore() <= lowThreshold &&
                           score.getFrequencyScore() >= highThreshold &&
                           score.getMonetaryScore() >= highThreshold;
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Get new customers (high recency, but low frequency and monetary).
     *
     * @param scores Map of user ID to RFM score
     * @param numSegments Number of segments
     * @return List of new user IDs
     */
    public List<String> getNewCustomers(Map<String, RFMScore> scores, int numSegments) {
        int highThreshold = (int) Math.ceil(0.8 * numSegments);
        int lowThreshold = (int) Math.ceil(0.2 * numSegments);

        return scores.entrySet().stream()
                .filter(entry -> {
                    RFMScore score = entry.getValue();
                    return score.getRecencyScore() >= highThreshold &&
                           score.getFrequencyScore() <= lowThreshold &&
                           score.getMonetaryScore() <= lowThreshold;
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Raw RFM values for a user.
     */
    public static class RFMValues {
        private final long recency;
        private final int frequency;
        private final double monetary;

        public RFMValues(long recency, int frequency, double monetary) {
            this.recency = recency;
            this.frequency = frequency;
            this.monetary = monetary;
        }

        public long getRecency() {
            return recency;
        }

        public int getFrequency() {
            return frequency;
        }

        public double getMonetary() {
            return monetary;
        }
    }

    /**
     * RFM score for a user.
     */
    public static class RFMScore {
        private final int recencyScore;
        private final int frequencyScore;
        private final int monetaryScore;

        public RFMScore(int recencyScore, int frequencyScore, int monetaryScore) {
            this.recencyScore = recencyScore;
            this.frequencyScore = frequencyScore;
            this.monetaryScore = monetaryScore;
        }

        public int getRecencyScore() {
            return recencyScore;
        }

        public int getFrequencyScore() {
            return frequencyScore;
        }

        public int getMonetaryScore() {
            return monetaryScore;
        }

        public int getCombinedScore() {
            return recencyScore * 100 + frequencyScore * 10 + monetaryScore;
        }

        @Override
        public String toString() {
            return recencyScore + "-" + frequencyScore + "-" + monetaryScore;
        }
    }
}
