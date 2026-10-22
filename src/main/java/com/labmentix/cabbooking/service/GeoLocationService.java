package com.labmentix.cabbooking.service;

/**
 * Service interface for geographic computations, distance matrix calculations, and geocoding via Google Maps API.
 */
public interface GeoLocationService {

    /**
     * Calculate driving distance in kilometers between two coordinate pairs.
     *
     * @param lat1 Origin latitude.
     * @param lon1 Origin longitude.
     * @param lat2 Destination latitude.
     * @param lon2 Destination longitude.
     * @return Road distance in kilometers (or straight-line Haversine fallback).
     */
    double calculateDistance(double lat1, double lon1, double lat2, double lon2);

    /**
     * Calculate estimated time of arrival (ETA) in minutes for driving between two coordinate pairs.
     *
     * @param lat1 Origin latitude.
     * @param lon1 Origin longitude.
     * @param lat2 Destination latitude.
     * @param lon2 Destination longitude.
     * @return Estimated duration in minutes.
     */
    int calculateEta(double lat1, double lon1, double lat2, double lon2);

    /**
     * Convert a human-readable postal address string into latitude and longitude coordinates.
     *
     * @param address Physical street address or location name.
     * @return Array of two doubles: [latitude, longitude].
     */
    double[] geocodeAddress(String address);

    /**
     * Convert geographic coordinates into a human-readable street address.
     *
     * @param lat Latitude coordinate.
     * @param lng Longitude coordinate.
     * @return Formatted address string.
     */
    String reverseGeocode(double lat, double lng);
}
