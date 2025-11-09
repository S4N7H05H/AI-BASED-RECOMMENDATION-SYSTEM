package Recommendation_System;
import java.util.*;
import java.util.stream.Collectors;

public class RecommendationSystem {


    private static final Map<Integer, Map<Integer, Double>> USER_RATINGS = new HashMap<>();


    private static final Map<Integer, String> ITEM_NAMES = new HashMap<>();

 
    static {
 
        ITEM_NAMES.put(1, "Interstellar");
        ITEM_NAMES.put(2, "Inception");
        ITEM_NAMES.put(3, "The Dark Knight");
        ITEM_NAMES.put(4, "Avatar");
        ITEM_NAMES.put(5, "The Prestige");

  
        USER_RATINGS.put(2, new HashMap<>() {{
            put(1, 4.5);
            put(2, 5.0);
            put(3, 4.0);
            put(4, 3.0);
        }});


        USER_RATINGS.put(3, new HashMap<>() {{
            put(2, 1.0); 
            put(4, 3.0); 
            put(5, 5.0); 
        }});


        USER_RATINGS.put(4, new HashMap<>() {{
            put(1, 3.0);
            put(3, 4.5);
            put(5, 3.5);
        }});
        

        USER_RATINGS.put(5, new HashMap<>() {{
            put(1, 4.8);
            put(2, 4.1);
            put(3, 5.0);
            put(4, 2.5);
            put(5, 4.0);
        }});
    }

    public static void main(String[] args) {
        System.out.println("--- Collaborative Filtering Recommendation System ---");
        

        int targetUserId = 1;

        Map<Integer, Double> userRatings = getUserInputRatings();
        if (userRatings.isEmpty()) {
            System.out.println("Cannot generate recommendations without any ratings. Exiting.");
            return;
        }
        

        USER_RATINGS.put(targetUserId, userRatings);


        int mostSimilarUserId = findMostSimilarUser(targetUserId);

        System.out.println("---------------------------------------------------");
        System.out.println("Target User (You): ID " + targetUserId + " | Rated Movies: " + userRatings.size());
        
        if (mostSimilarUserId == -1) {
            System.out.println("Error: Cannot find a similar user with shared ratings.");
            return;
        }

        recommendItem(targetUserId, mostSimilarUserId);
    }
    

    private static Map<Integer, Double> getUserInputRatings() {
        Scanner scanner = new Scanner(System.in);
        Map<Integer, Double> ratings = new HashMap<>();

        try {
            System.out.println("\nPlease rate the following movies on a scale of 1.0 to 5.0.");
            System.out.println("Enter '0' or skip to leave a movie unrated.");
            System.out.println("---------------------------------------------------");

            for (Map.Entry<Integer, String> item : ITEM_NAMES.entrySet()) {
                int itemId = item.getKey();
                String itemName = item.getValue();

                double rating = 0.0;
                boolean valid = false;

                while (!valid) {
                    System.out.printf("Rate %s (1.0 - 5.0, or 0): ", itemName);
                    try {
                        String input = scanner.nextLine().trim();
                        if (input.isEmpty()) {
                            rating = 0.0;
                        } else {
                            rating = Double.parseDouble(input);
                        }

                        if (rating == 0.0) {
                            valid = true;
                        } else if (rating >= 1.0 && rating <= 5.0) {
                            ratings.put(itemId, rating);
                            valid = true;
                        } else {
                            System.out.println("Invalid rating. Must be between 1.0 and 5.0, or 0 to skip.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Please enter a number.");
                    }
                }
            }
            return ratings;
        } finally {
            scanner.close();
        }
    }



    private static int findMostSimilarUser(int targetUserId) {
        Map<Integer, Double> targetRatings = USER_RATINGS.get(targetUserId);
        double lowestDistance = Double.MAX_VALUE;
        int mostSimilarId = -1;

        System.out.println("\n--- Similarity Calculation ---");
        System.out.println("Finding the user whose tastes are closest to yours...");
        
    
        for (Map.Entry<Integer, Map<Integer, Double>> entry : USER_RATINGS.entrySet()) {
            int otherUserId = entry.getKey();
            
            if (targetUserId == otherUserId) continue;

            Map<Integer, Double> otherRatings = entry.getValue();
            
            double distance = calculateEuclideanDistance(targetRatings, otherRatings);
            
            if (distance >= 0) { 
                
                System.out.printf("  Distance to User %d: %.3f%n", otherUserId, distance);
                
                if (distance < lowestDistance) {
                    lowestDistance = distance;
                    mostSimilarId = otherUserId;
                }
            }
        }
        
        System.out.println("\n-> Most Similar User found: ID " + mostSimilarId + " (Distance: " + String.format("%.3f", lowestDistance) + ")");
        return mostSimilarId;
    }
    

    private static double calculateEuclideanDistance(Map<Integer, Double> user1Ratings, Map<Integer, Double> user2Ratings) {
        double sumOfSquares = 0.0;
        int sharedItemsCount = 0;
        
        for (Map.Entry<Integer, Double> itemRating1 : user1Ratings.entrySet()) {
            int itemId = itemRating1.getKey();
            
            if (user2Ratings.containsKey(itemId)) {
                sharedItemsCount++;
                double rating1 = itemRating1.getValue();
                double rating2 = user2Ratings.get(itemId);
                
                sumOfSquares += Math.pow(rating1 - rating2, 2);
            }
        }
        
        if (sharedItemsCount == 0) {
            return -1.0; 
        }
        
        return Math.sqrt(sumOfSquares);
    }
    

    private static void recommendItem(int targetId, int similarId) {
        Map<Integer, Double> targetRatings = USER_RATINGS.get(targetId);

        System.out.println("\n--- Recommendation ---");
        System.out.println("Looking for high-rated movies from users similar to you...");

        List<Integer> usersByDistance = USER_RATINGS.keySet().stream()
                .filter(id -> id != targetId)
                .map(id -> new AbstractMap.SimpleEntry<>(id,
                        calculateEuclideanDistance(targetRatings, USER_RATINGS.get(id))))
                .filter(e -> e.getValue() >= 0) 
                .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        int recommendedItemId = -1;
        double highestPredictedRating = -1.0;
        int usedSimilarUser = -1;

        for (int uid : usersByDistance) {
            Map<Integer, Double> simRatings = USER_RATINGS.get(uid);
            for (Map.Entry<Integer, Double> entry : simRatings.entrySet()) {
                int itemId = entry.getKey();
                double rating = entry.getValue();

                boolean notSeen = !targetRatings.containsKey(itemId);
                boolean ratedHighly = rating >= 4.0;

                if (notSeen && ratedHighly && rating > highestPredictedRating) {
                    highestPredictedRating = rating;
                    recommendedItemId = itemId;
                    usedSimilarUser = uid;
                }
            }
            if (recommendedItemId != -1) break; 
        }

        if (recommendedItemId == -1) {
            Map<Integer, Double> sumRatings = new HashMap<>();
            Map<Integer, Integer> counts = new HashMap<>();

            for (Map<Integer, Double> ratings : USER_RATINGS.values()) {
                for (Map.Entry<Integer, Double> e : ratings.entrySet()) {
                    sumRatings.merge(e.getKey(), e.getValue(), Double::sum);
                    counts.merge(e.getKey(), 1, Integer::sum);
                }
            }

            for (Map.Entry<Integer, Double> e : sumRatings.entrySet()) {
                int itemId = e.getKey();
                if (targetRatings.containsKey(itemId)) continue; 
                double avg = e.getValue() / counts.get(itemId);
                if (avg >= 4.0 && avg > highestPredictedRating) {
                    highestPredictedRating = avg;
                    recommendedItemId = itemId;
                    usedSimilarUser = -1; 
                }
            }
        }

        if (recommendedItemId != -1) {
            if (usedSimilarUser > 0) {
                System.out.printf("Using User %d as source.%n", usedSimilarUser);
            } else {
                System.out.println("Using global average across users as fallback.");
            }
            System.out.printf("🎉 FINAL RECOMMENDATION: %s%n", ITEM_NAMES.get(recommendedItemId));
            System.out.printf("   Predicted Rating: %.1f%n", highestPredictedRating);
        } else {
            System.out.println("No suitable high-rated movies found for recommendation.");
           
        }
        System.out.println("---------------------------------------------------");
    }

}