package com.labmentix.cabbooking.controller;

import com.labmentix.cabbooking.dto.DriverLocationUpdate;
import com.labmentix.cabbooking.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * WebSocket STOMP controller handling real-time duplex communication for driver telemetry, ride status, and SOS events.
 */
@Controller
public class WebSocketController {

    private static final Logger log = LoggerFactory.getLogger(WebSocketController.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    @Autowired
    public WebSocketController(SimpMessagingTemplate messagingTemplate,
                               NotificationService notificationService) {
        this.messagingTemplate = messagingTemplate;
        this.notificationService = notificationService;
    }

    /**
     * Handle driver location updates sent to /app/driver/location and broadcast to /topic/driver-location/{driverId}.
     */
    @MessageMapping("/driver/location")
    public void handleDriverLocation(@Payload DriverLocationUpdate update) {
        log.debug("Received STOMP driver location update for driver: {}", update.getDriverId());
        if (update != null && update.getDriverId() != null) {
            messagingTemplate.convertAndSend("/topic/driver-location/" + update.getDriverId(), update);
            messagingTemplate.convertAndSend("/topic/driver-location", update);
        }
    }

    /**
     * Handle driver location updates with explicit path variable /app/driver/location/{driverId}.
     */
    @MessageMapping("/driver/location/{driverId}")
    @SendTo("/topic/driver-location/{driverId}")
    public DriverLocationUpdate broadcastDriverLocation(@DestinationVariable String driverId,
                                                        @Payload DriverLocationUpdate update) {
        log.debug("Broadcasting STOMP location for driverId: {}", driverId);
        return update;
    }

    /**
     * Handle ride status update events sent to /app/ride/status.
     */
    @MessageMapping("/ride/status")
    public void handleRideStatus(@Payload Map<String, Object> payload) {
        log.info("Received STOMP ride status update: {}", payload);
        Object rideIdObj = payload.get("rideId");
        if (rideIdObj != null) {
            String rideIdStr = rideIdObj.toString();
            messagingTemplate.convertAndSend("/topic/ride/" + rideIdStr, payload);
        }
        messagingTemplate.convertAndSend("/topic/ride/status", payload);
    }

    /**
     * Handle ride status update events with explicit path variable /app/ride/status/{rideId}.
     */
    @MessageMapping("/ride/status/{rideId}")
    @SendTo("/topic/ride/{rideId}")
    public Map<String, Object> broadcastRideStatus(@DestinationVariable String rideId,
                                                  @Payload Map<String, Object> payload) {
        log.info("Broadcasting STOMP ride status for rideId {}: {}", rideId, payload);
        return payload;
    }

    /**
     * Handle critical SOS alerts sent to /app/sos and broadcast to /topic/sos.
     */
    @MessageMapping("/sos")
    @SendTo("/topic/sos")
    public Map<String, Object> handleSosAlert(@Payload Map<String, Object> sosPayload) {
        log.warn("CRITICAL: Received WebSocket SOS alert: {}", sosPayload);

        Map<String, Object> enrichedPayload = new HashMap<>(sosPayload);
        enrichedPayload.putIfAbsent("receivedAt", LocalDateTime.now().toString());
        enrichedPayload.putIfAbsent("status", "DISPATCHED");

        Object rideId = sosPayload.get("rideId");
        String message = (String) sosPayload.getOrDefault("message", "Emergency SOS triggered");

        if (rideId != null) {
            try {
                notificationService.sendSosAlert(UUID.fromString(rideId.toString()), message);
            } catch (Exception e) {
                log.error("Failed to propagate SOS alert via notification service: {}", e.getMessage());
            }
        }

        return enrichedPayload;
    }
}
