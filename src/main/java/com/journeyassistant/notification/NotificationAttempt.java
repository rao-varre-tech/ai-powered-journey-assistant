package com.journeyassistant.notification;

import com.journeyassistant.journey.Journey;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_attempts")
public class NotificationAttempt {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journey_id", nullable = false)
    private Journey journey;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "provider_call_id", unique = true, length = 128)
    private String providerCallId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationAttemptStatus status;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "last_processed_digit", length = 1)
    private String lastProcessedDigit;

    @Column(name = "invalid_digit_count", nullable = false)
    private int invalidDigitCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected NotificationAttempt() {
    }

    public NotificationAttempt(Journey journey, int attemptNumber, Instant scheduledAt) {
        this.journey = journey;
        this.attemptNumber = attemptNumber;
        this.scheduledAt = scheduledAt;
        this.status = NotificationAttemptStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Journey getJourney() {
        return journey;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public String getProviderCallId() {
        return providerCallId;
    }

    public NotificationAttemptStatus getStatus() {
        return status;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getLastProcessedDigit() { return lastProcessedDigit; }

    public int getInvalidDigitCount() { return invalidDigitCount; }

    public void assignProviderCallId(String callSid) {
        if (providerCallId != null && !providerCallId.equals(callSid)) {
            throw new IllegalStateException("Call SID is already assigned");
        }
        providerCallId = callSid;
        status = NotificationAttemptStatus.IN_PROGRESS;
        startedAt = Instant.now();
    }

    public void updateStatus(NotificationAttemptStatus status) {
        this.status = status;
        if (status == NotificationAttemptStatus.COMPLETED || status == NotificationAttemptStatus.FAILED) {
            completedAt = Instant.now();
        }
    }

    public void recordDigit(String digit) {
        lastProcessedDigit = digit;
        if (!"1".equals(digit) && !"2".equals(digit) && !"3".equals(digit)) invalidDigitCount++;
    }
}
