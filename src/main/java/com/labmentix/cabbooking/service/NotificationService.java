package com.labmentix.cabbooking.service;

import com.labmentix.cabbooking.dto.DriverLocationUpdate;
import com.labmentix.cabbooking.enums.RideStatus;

import java.util.UUID;

/**
 * Service interface for real-time WebSocket notifications and email communication.
 */
public interface NotificationService {

    /**
     * Broadcast a ride status change to subscribed riders and drivers over WebSocket.
     *
     * @param rideId UUID of the ride.
     * @param status New RideStatus.
     */
    void sendRideStatusUpdate(UUID rideId, RideStatus status);

    /**
     * Broadcast a driver's live GPS coordinates over WebSocket.
     *
     * @param update Location update payload containing driver ID and coordinates.
     */
    void sendDriverLocationUpdate(DriverLocationUpdate update);

    /**
     * Broadcast an urgent SOS alert notification over WebSocket to emergency channels.
     *
     * @param rideId  UUID of the ride.
     * @param message SOS alert message or context.
     */
    void sendSosAlert(UUID rideId, String message);

    /**
     * Dispatch an email notification asynchronously using JavaMailSender.
     *
     * @param to      Recipient email address.
     * @param subject Email subject line.
     * @param body    Plain text or HTML email body.
     */
    void sendEmail(String to, String subject, String body);
}
