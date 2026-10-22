package com.labmentix.cabbooking.service.impl;

import com.labmentix.cabbooking.dto.DriverLocationUpdate;
import com.labmentix.cabbooking.enums.RideStatus;
import com.labmentix.cabbooking.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of NotificationService providing WebSocket broadcasting via SimpMessagingTemplate and email dispatch via JavaMailSender.
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final JavaMailSender mailSender;

    @Autowired
    public NotificationServiceImpl(SimpMessagingTemplate messagingTemplate,
                                   @Autowired(required = false) JavaMailSender mailSender) {
        this.messagingTemplate = messagingTemplate;
        this.mailSender = mailSender;
    }

    @Override
    public void sendRideStatusUpdate(UUID rideId, RideStatus status) {
        log.info("Broadcasting ride status update for ride {}: {}", rideId, status);

        Map<String, Object> payload = new HashMap<>();
        payload.put("rideId", rideId.toString());
        payload.put("status", status.name());
        payload.put("timestamp", LocalDateTime.now().toString());

        try {
            messagingTemplate.convertAndSend("/topic/ride/" + rideId, payload);
            messagingTemplate.convertAndSend("/topic/ride/status", payload);
        } catch (Exception e) {
            log.error("Failed to broadcast WebSocket ride status update for ride {}: {}", rideId, e.getMessage(), e);
        }
    }

    @Override
    public void sendDriverLocationUpdate(DriverLocationUpdate update) {
        if (update == null || update.getDriverId() == null) {
            log.warn("Cannot broadcast driver location update with null payload or driverId");
            return;
        }

        try {
            messagingTemplate.convertAndSend("/topic/driver-location/" + update.getDriverId(), update);
            messagingTemplate.convertAndSend("/topic/driver-location", update);
        } catch (Exception e) {
            log.error("Failed to broadcast driver location update for driver {}: {}", update.getDriverId(), e.getMessage(), e);
        }
    }

    @Override
    public void sendSosAlert(UUID rideId, String message) {
        log.warn("Broadcasting URGENT SOS ALERT for ride {}: {}", rideId, message);

        Map<String, Object> payload = new HashMap<>();
        payload.put("rideId", rideId != null ? rideId.toString() : "UNKNOWN");
        payload.put("message", message);
        payload.put("alertLevel", "CRITICAL");
        payload.put("timestamp", LocalDateTime.now().toString());

        try {
            messagingTemplate.convertAndSend("/topic/sos", payload);
        } catch (Exception e) {
            log.error("Failed to broadcast SOS alert via WebSocket: {}", e.getMessage(), e);
        }
    }

    @Override
    @Async
    public void sendEmail(String to, String subject, String body) {
        if (mailSender == null) {
            log.warn("JavaMailSender is not configured. Email to {} with subject '{}' will not be sent.", to, subject);
            return;
        }

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(to);
            mailMessage.setSubject(subject);
            mailMessage.setText(body);
            mailSender.send(mailMessage);
            log.info("Email successfully sent to {} with subject: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }
}
