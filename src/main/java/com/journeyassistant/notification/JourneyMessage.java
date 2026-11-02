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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "journey_messages")
public class JourneyMessage {
    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journey_id", nullable = false)
    private Journey journey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MessageChannelType channel;

    @Column(name = "provider_message_id", unique = true, length = 128)
    private String providerMessageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MessageDeliveryStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected JourneyMessage() { }

    public JourneyMessage(Journey journey, MessageChannelType channel) {
        this.journey = journey;
        this.channel = channel;
        this.status = MessageDeliveryStatus.QUEUED;
    }

    @PrePersist void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public Journey getJourney() { return journey; }
    public MessageChannelType getChannel() { return channel; }
    public String getProviderMessageId() { return providerMessageId; }
    public MessageDeliveryStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void assignProviderMessageId(String messageId) {
        if (providerMessageId != null && !providerMessageId.equals(messageId))
            throw new IllegalStateException("Message SID is already assigned");
        providerMessageId = messageId;
    }

    public boolean updateStatus(MessageDeliveryStatus next) {
        if (next == null || next.rank() <= status.rank()) return false;
        status = next;
        return true;
    }
}
