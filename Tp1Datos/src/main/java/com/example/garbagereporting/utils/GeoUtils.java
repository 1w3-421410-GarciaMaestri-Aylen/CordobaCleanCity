package com.example.garbagereporting.utils;

public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double CORDOBA_SOUTH = -31.56;
    private static final double CORDOBA_WEST = -64.33;
    private static final double CORDOBA_NORTH = -31.30;
    private static final double CORDOBA_EAST = -64.03;

    private GeoUtils() {
    }

    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    public static boolean isValidLatitude(Double latitude) {
        return latitude != null && latitude >= -90.0 && latitude <= 90.0;
    }

    public static boolean isValidLongitude(Double longitude) {
        return longitude != null && longitude >= -180.0 && longitude <= 180.0;
    }

    public static boolean isValidCoordinate(Double latitude, Double longitude) {
        return isValidLatitude(latitude) && isValidLongitude(longitude);
    }

    public static boolean isInsideCordobaCapital(Double latitude, Double longitude) {
        return isValidCoordinate(latitude, longitude)
                && latitude >= CORDOBA_SOUTH
                && latitude <= CORDOBA_NORTH
                && longitude >= CORDOBA_WEST
                && longitude <= CORDOBA_EAST;
    }
}
