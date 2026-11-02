package com.journeyassistant.notification;

public enum MessageDeliveryStatus {
    QUEUED,
    SENT,
    FAILED,
    UNDELIVERED,
    DELIVERED,
    READ;

    public static MessageDeliveryStatus fromTwilio(String status) {
        if (status == null) return null;
        return switch (status.toLowerCase()) {
            case "queued" -> QUEUED;
            case "sent" -> SENT;
            case "failed" -> FAILED;
            case "undelivered" -> UNDELIVERED;
            case "delivered" -> DELIVERED;
            case "read" -> READ;
            default -> null;
        };
    }

    int rank() {
        return switch (this) {
            case QUEUED -> 0;
            case SENT -> 1;
            case FAILED, UNDELIVERED -> 2;
            case DELIVERED -> 3;
            case READ -> 4;
        };
    }
}
