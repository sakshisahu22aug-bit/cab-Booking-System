package com.labmentix.cabbooking.repository;

import com.labmentix.cabbooking.entity.Payment;
import com.labmentix.cabbooking.entity.Ride;
import com.labmentix.cabbooking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Payment entity.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByRide(Ride ride);

    Optional<Payment> findByRideId(UUID rideId);

    List<Payment> findByRider(User rider);

    List<Payment> findByRiderId(UUID riderId);

    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);
}
