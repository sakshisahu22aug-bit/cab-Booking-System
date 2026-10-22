package com.labmentix.cabbooking.repository;

import com.labmentix.cabbooking.entity.Driver;
import com.labmentix.cabbooking.entity.Ride;
import com.labmentix.cabbooking.entity.User;
import com.labmentix.cabbooking.enums.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Ride entity.
 */
@Repository
public interface RideRepository extends JpaRepository<Ride, UUID> {

    List<Ride> findByRiderAndStatusIn(User rider, Collection<RideStatus> statuses);

    List<Ride> findByDriverAndStatusIn(Driver driver, Collection<RideStatus> statuses);

    List<Ride> findByRiderOrderByCreatedAtDesc(User rider);

    List<Ride> findByDriverOrderByCreatedAtDesc(Driver driver);

    List<Ride> findByStatus(RideStatus status);

    List<Ride> findByRiderIdOrderByCreatedAtDesc(UUID riderId);

    List<Ride> findByDriverIdOrderByCreatedAtDesc(UUID driverId);
}
