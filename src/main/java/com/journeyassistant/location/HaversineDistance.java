package com.journeyassistant.location;

/** Computes great-circle distance between two latitude/longitude points. */
public final class HaversineDistance {

    private static final double EARTH_MEAN_RADIUS_METERS = 6_371_000.0;

    private HaversineDistance() {
    }

    public static double betweenMeters(double latitude1, double longitude1,
                                       double latitude2, double longitude2) {
        validateCoordinates(latitude1, longitude1);
        validateCoordinates(latitude2, longitude2);

        double lat1Radians = Math.toRadians(latitude1);
        double lat2Radians = Math.toRadians(latitude2);
        double latitudeDifference = Math.toRadians(latitude2 - latitude1);
        double longitudeDifference = Math.toRadians(longitude2 - longitude1);

        double haversine = Math.pow(Math.sin(latitudeDifference / 2), 2)
                + Math.cos(lat1Radians) * Math.cos(lat2Radians)
                * Math.pow(Math.sin(longitudeDifference / 2), 2);
        double centralAngle = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
        return EARTH_MEAN_RADIUS_METERS * centralAngle;
    }

    private static void validateCoordinates(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees");
        }
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees");
        }
    }
}
