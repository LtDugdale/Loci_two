package com.lauriedugdale.loci.utils;

/**
 * Created by mnt_x on 05/07/2017.
 */

public final class GeofencingUtils {

    /**
     * Prevents instantiation.
     */
    private GeofencingUtils(){}

    private static final long GEOFENCE_EXPIRATION_IN_HOURS = 12;

    public static final float GEOFENCE_RADIUS_IN_METERS = 500;
    public static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS =
            GEOFENCE_EXPIRATION_IN_HOURS * 60 * 60 * 1000;

}
