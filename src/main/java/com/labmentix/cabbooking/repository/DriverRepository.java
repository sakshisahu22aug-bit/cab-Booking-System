package com.labmentix.cabbooking.repository;

import com.labmentix.cabbooking.entity.Driver;
import com.labmentix.cabbooking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Driver entity with geospatial Haversine search queries.
 */
@Repository
public interface DriverRepository extends JpaRepository<Driver, UUID> {

    Optional<Driver> findByUser(User user);

    Optional<Driver> findByUserId(UUID userId);

    List<Driver> findByAvailableTrueAndVerifiedTrue();

    Optional<Driver> findByLicenseNumber(String licenseNumber);

    Optional<Driver> findByVehicleNumber(String vehicleNumber);

    boolean existsByLicenseNumber(String licenseNumber);

    boolean existsByVehicleNumber(String vehicleNumber);

    /**
     * Find available and verified drivers within a given radius using the Haversine formula.
     * Earth radius is estimated at 6371 kilometers.
     */
    @Query(value = "SELECT d.* FROM drivers d " +
            "WHERE d.available = true " +
            "AND d.verified = true " +
            "AND d.current_latitude IS NOT NULL " +
            "AND d.current_longitude IS NOT NULL " +
            "AND (6371 * acos(cos(radians(:latitude)) * cos(radians(d.current_latitude)) * " +
            "cos(radians(d.current_longitude) - radians(:longitude)) + " +
            "sin(radians(:latitude)) * sin(radians(d.current_latitude)))) <= :radiusInKm " +
            "ORDER BY (6371 * acos(cos(radians(:latitude)) * cos(radians(d.current_latitude)) * " +
            "cos(radians(d.current_longitude) - radians(:longitude)) + " +
            "sin(radians(:latitude)) * sin(radians(d.current_latitude)))) ASC",
            nativeQuery = true)
    List<Driver> findNearbyDrivers(@Param("latitude") Double latitude,
                                  @Param("longitude") Double longitude,
                                  @Param("radiusInKm") Double radiusInKm);

    /**
     * Find available and verified drivers filtered by vehicle type within radius.
     */
    @Query(value = "SELECT d.* FROM drivers d " +
            "WHERE d.available = true " +
            "AND d.verified = true " +
            "AND d.vehicle_type = :vehicleType " +
            "AND d.current_latitude IS NOT NULL " +
            "AND d.current_longitude IS NOT NULL " +
            "AND (6371 * acos(cos(radians(:latitude)) * cos(radians(d.current_latitude)) * " +
            "cos(radians(d.current_longitude) - radians(:longitude)) + " +
            "sin(radians(:latitude)) * sin(radians(d.current_latitude)))) <= :radiusInKm " +
            "ORDER BY (6371 * acos(cos(radians(:latitude)) * cos(radians(d.current_latitude)) * " +
            "cos(radians(d.current_longitude) - radians(:longitude)) + " +
            "sin(radians(:latitude)) * sin(radians(d.current_latitude)))) ASC",
            nativeQuery = true)
    List<Driver> findNearbyDriversByVehicleType(@Param("latitude") Double latitude,
                                               @Param("longitude") Double longitude,
                                               @Param("radiusInKm") Double radiusInKm,
                                               @Param("vehicleType") String vehicleType);
}
