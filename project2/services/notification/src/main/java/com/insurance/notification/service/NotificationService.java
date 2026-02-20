package com.insurance.notification.service;

import com.insurance.notification.model.NotificationRequest;
import com.insurance.notification.model.NotificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private static final String CHANNEL_EMAIL = "EMAIL";
    private static final Set<String> VALID_NOTIFICATION_TYPES = Set.of(
            "SUBMITTED", "APPROVED", "REJECTED", "PAYMENT_SENT"
    );
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Delivery rules:
     *   recipientEmail contains "@" -> SENT
     *   otherwise -> FAILED (invalid email)
     */
    public NotificationResponse send(NotificationRequest request) {
        log.debug("Processing notification for claimId={}, type={}, recipient={}",
                request.claimId(), request.notificationType(), request.recipientEmail());

        validateNotificationType(request.notificationType());

        String notificationId = UUID.randomUUID().toString();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);

        boolean isValidEmail = request.recipientEmail().contains("@");

        if (isValidEmail) {
            log.info("Notification SENT for claimId={}, type={}, to={}",
                    request.claimId(), request.notificationType(), request.recipientEmail());
            return new NotificationResponse(notificationId, "SENT", CHANNEL_EMAIL, timestamp);
        }

        log.warn("Notification FAILED for claimId={}: invalid email address '{}'",
                request.claimId(), request.recipientEmail());
        return new NotificationResponse(notificationId, "FAILED", CHANNEL_EMAIL, timestamp);
    }

    private void validateNotificationType(String notificationType) {
        if (!VALID_NOTIFICATION_TYPES.contains(notificationType)) {
            throw new IllegalArgumentException(
                    "Invalid notification type '" + notificationType
                            + "'. Accepted values: " + VALID_NOTIFICATION_TYPES
            );
        }
    }
}
