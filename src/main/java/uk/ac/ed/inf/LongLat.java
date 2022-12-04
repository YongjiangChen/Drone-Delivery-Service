package uk.ac.ed.inf;

/**
 * LongLat provides representation of the drone's position
 * using attribute longitude, latitude and angle.
 */
public class LongLat {

    public static final double FALSE_LONGITUDE = -3.183666;

    public static final double FALSE_LATITUDE = 55.944066;

    /** The longitude for appleton tower, home to the drone  */
    public static final double APPLETON_LONGITUDE = -3.186874;

    /** The latitude for appleton tower, home to the drone  */
    public static final double APPLETON_LATITUDE = 55.944494;

    /** The west side longitude border for the drone confinement area  */
    private static final double WESTMOST_LONGITUDE = -3.192473;

    /** The east side longitude border for the drone confinement area  */
    private static final double EASTMOST_LONGITUDE = -3.184319;

    /** The north side latitude border for the drone confinement area  */
    private static final double NORTHMOST_LATITUDE = 55.946233;

    /** The south side latitude border for the drone confinement area  */
    private static final double SOUTHMOST_LATITUDE = 55.942617;

    /** The standard distance in degrees that the drone makes for a single move */
    public static final double DISTANCE_TOLERANCE = 0.00015;

    /** The angle that's used to command the drone to hover */
    public static final int HOVERING_ANGLE = -999;

    /** Longitude is the measurement east or west of the prime meridian.
     * it's used here in combination with latitude to represent a geographic location*/
    public double longitude;

    /** Latitude is the measurement of distance north or south of the Equator
     * it's used here in combination with longitude to represent a geographic location*/
    public double latitude;

    /**
     * constructor LongLat
     * A geographic point can be located with a longitude and a latitude.
     * @param longitude - Longitude is the measurement east or west of the prime meridian
     * @param latitude  - Latitude is the measurement of distance north or south of the Equator
     *
     */
    public LongLat(double longitude, double latitude){
        this.longitude = longitude;
        this.latitude = latitude;
    }

    /**
     * Calculate the distance between two points
     * @param coordinate the position of the other point.
     * @return distance between two the points
     */
    public double distanceTo(LongLat coordinate) {
        double longDiff = coordinate.longitude - longitude;
        double latDiff = coordinate.latitude  - latitude;
        //
        return Math.sqrt(Math.pow(longDiff,2) + Math.pow(latDiff,2));
    }

    /**
     * Determine whether the distance between two points is within the standard distance tolerance
     *
     * @param coordinate the position of the next point
     * @return True if the points are within the distance tolerance, False if not.
     */
    public boolean closeTo(LongLat coordinate) {
        return distanceTo(coordinate) < DISTANCE_TOLERANCE;
    }

    /**
     * A helper function to parse the input angle for the nextPosition() method
     * @param angle indicates the drone direction for the next move.
     * @return Ture if the input angle indicates a valid drone movement direction, False if it represents drone-hovering.
     * @throws IllegalArgumentException If the angle input is neither a valid movement angle nor a hovering angle.
     */
    public boolean parseAngle(int angle){
        if ((angle >= 0) && (angle <= 350) && (angle%10 == 0)){
            return true;
        } else if(angle == HOVERING_ANGLE ) {
            return false;
        } else {
            throw new IllegalArgumentException("The input angle should be a multiples of ten between 0 and 350 " +
                    "or -999 for representing hovering");
        }
    }

    /**
     * Calculate the next position using based on the input angle.
     * @param angle indicates the drone direction for the next move.
     * @return the new position of the drone after making one move in the input angle.
     */
    public LongLat nextPosition(int angle){
        if (parseAngle(angle)) {
            double radian = Math.toRadians(angle);
            // Calculate the distance moved in the longitude and latitude directions and
            // add up with the original longitude and latitude.
            double nextLongitude = longitude + (DISTANCE_TOLERANCE * Math.cos(radian));
            double nextLatitude = latitude + (DISTANCE_TOLERANCE * Math.sin(radian));
            return new LongLat(nextLongitude, nextLatitude);
        } else {
            return new LongLat(longitude, latitude);
        }
    }

}

