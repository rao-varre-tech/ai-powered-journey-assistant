package com.journeyassistant.notification;

public enum NotificationStrategy {
    SMS,
    WHATSAPP,
    WHATSAPP_THEN_SMS,
    BOTH;

    public static NotificationStrategy parse(String value) {
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("notification.strategy must be sms, whatsapp, whatsapp_then_sms, or both");
        }
    }
}
