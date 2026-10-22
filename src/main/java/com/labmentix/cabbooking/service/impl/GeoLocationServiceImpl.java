package com.labmentix.cabbooking.service.impl;

import com.google.maps.DistanceMatrixApi;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.DistanceMatrixElement;
import com.google.maps.model.DistanceMatrixElementStatus;
import com.google.maps.model.DistanceMatrixRow;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import com.google.maps.model.TravelMode;
import com.labmentix.cabbooking.service.GeoLocationService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Implementation of GeoLocationService using Google Maps Distance Matrix and Geocoding APIs,
 * with complete graceful fallback to spherical Haversine trigonometric calculation.
 */
@Service
public class GeoLocationServiceImpl implements GeoLocationService {

    private static final Logger log = LoggerFactory.getLogger(GeoLocationServiceImpl.class);
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double AVERAGE_CITY_SPEED_KMH = 30.0;

    @Value("${google.maps.api.key:}")
    private String apiKey;

    private GeoApiContext geoApiContext;

    @PostConstruct
    public void init() {
        if (apiKey != null && !apiKey.isBlank()) {
            this.geoApiContext = new GeoApiContext.Builder()
                    .apiKey(apiKey)
                    .build();
            log.info("Google Maps GeoApiContext successfully initialized.");
        } else {
            log.warn("Google Maps API key is missing. System will operate in Haversine fallback mode.");
        }
    }

    @Override
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        if (geoApiContext != null) {
            try {
                DistanceMatrix matrix = DistanceMatrixApi.newRequest(geoApiContext)
                        .origins(new LatLng(lat1, lon1))
                        .destinations(new LatLng(lat2, lon2))
                        .mode(TravelMode.DRIVING)
                        .await();

                if (matrix.rows != null && matrix.rows.length > 0) {
                    DistanceMatrixRow row = matrix.rows[0];
                    if (row.elements != null && row.elements.length > 0) {
                        DistanceMatrixElement element = row.elements[0];
                        if (element.status == DistanceMatrixElementStatus.OK && element.distance != null) {
                            double distanceKm = element.distance.inMeters / 1000.0;
                            log.debug("Google Maps road distance calculated: {} km", distanceKm);
                            return Math.round(distanceKm * 100.0) / 100.0;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Google Maps API distance calculation failed: {}. Falling back to Haversine.", e.getMessage());
            }
        }

        return calculateHaversineDistance(lat1, lon1, lat2, lon2);
    }

    @Override
    public int calculateEta(double lat1, double lon1, double lat2, double lon2) {
        if (geoApiContext != null) {
            try {
                DistanceMatrix matrix = DistanceMatrixApi.newRequest(geoApiContext)
                        .origins(new LatLng(lat1, lon1))
                        .destinations(new LatLng(lat2, lon2))
                        .mode(TravelMode.DRIVING)
                        .await();

                if (matrix.rows != null && matrix.rows.length > 0) {
                    DistanceMatrixRow row = matrix.rows[0];
                    if (row.elements != null && row.elements.length > 0) {
                        DistanceMatrixElement element = row.elements[0];
                        if (element.status == DistanceMatrixElementStatus.OK && element.duration != null) {
                            int minutes = (int) Math.max(1, Math.round(element.duration.inSeconds / 60.0));
                            log.debug("Google Maps driving ETA calculated: {} minutes", minutes);
                            return minutes;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Google Maps API ETA calculation failed: {}. Falling back to speed estimate.", e.getMessage());
            }
        }

        double distanceKm = calculateHaversineDistance(lat1, lon1, lat2, lon2);
        return (int) Math.max(5, Math.round((distanceKm / AVERAGE_CITY_SPEED_KMH) * 60.0));
    }

    @Override
    public double[] geocodeAddress(String address) {
        if (address == null || address.isBlank()) {
            return new double[]{0.0, 0.0};
        }

        if (geoApiContext != null) {
            try {
                GeocodingResult[] results = GeocodingApi.geocode(geoApiContext, address).await();
                if (results != null && results.length > 0 && results[0].geometry != null) {
                    LatLng location = results[0].geometry.location;
                    log.debug("Geocoded address '{}' to [{}, {}]", address, location.lat, location.lng);
                    return new double[]{location.lat, location.lng};
                }
            } catch (Exception e) {
                log.warn("Geocoding failed for address '{}': {}", address, e.getMessage());
            }
        }

        log.debug("Defaulting geocode coordinates for address: {}", address);
        return new double[]{0.0, 0.0};
    }

    @Override
    public String reverseGeocode(double lat, double lng) {
        if (geoApiContext != null) {
            try {
                GeocodingResult[] results = GeocodingApi.reverseGeocode(geoApiContext, new LatLng(lat, lng)).await();
                if (results != null && results.length > 0 && results[0].formattedAddress != null) {
                    log.debug("Reverse geocoded [{}, {}] to '{}'", lat, lng, results[0].formattedAddress);
                    return results[0].formattedAddress;
                }
            } catch (Exception e) {
                log.warn("Reverse geocoding failed for [{}, {}]: {}", lat, lng, e.getMessage());
            }
        }

        return String.format("Location [%.5f, %.5f]", lat, lng);
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS_KM * c;

        return Math.round(distance * 100.0) / 100.0;
    }
}
