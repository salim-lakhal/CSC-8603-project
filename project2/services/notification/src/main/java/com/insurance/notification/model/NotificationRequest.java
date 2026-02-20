package com.insurance.notification.model;

import jakarta.validation.constraints.NotBlank;

public record NotificationRequest(
        @NotBlank(message = "Claim ID is required")
        String claimId,

        @NotBlank(message = "Recipient email is required")
        String recipientEmail,

        @NotBlank(message = "Notification type is required")
        String notificationType,

        @NotBlank(message = "Message is required")
        String message
) {}
