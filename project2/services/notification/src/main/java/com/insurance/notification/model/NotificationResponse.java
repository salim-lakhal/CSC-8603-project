package com.insurance.notification.model;

public record NotificationResponse(
        String notificationId,
        String status,
        String channel,
        String timestamp
) {}
