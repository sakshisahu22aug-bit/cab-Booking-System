package com.labmentix.cabbooking.repository;

import com.labmentix.cabbooking.entity.Rating;
import com.labmentix.cabbooking.entity.Ride;
import com.labmentix.cabbooking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Rating entity.
 */
@Repository
public interface RatingRepository extends JpaRepository<Rating, UUID> {

    Optional<Rating> findByRide(Ride ride);

    Optional<Rating> findByRideId(UUID rideId);

    List<Rating> findByRatedFor(User ratedFor);

    List<Rating> findByRatedForId(UUID ratedForId);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Rating r WHERE r.ratedFor = :ratedFor")
    Double findAverageRatingByRatedFor(@Param("ratedFor") User ratedFor);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Rating r WHERE r.ratedFor.id = :ratedForId")
    Double findAverageRatingByRatedForId(@Param("ratedForId") UUID ratedForId);
}
