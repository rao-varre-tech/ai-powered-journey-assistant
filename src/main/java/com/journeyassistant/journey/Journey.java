package com.journeyassistant.journey;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "journeys")
public class Journey {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "traveller_phone", nullable = false, length = 32)
    private String travellerPhone;

    @Column(name = "destination_latitude", nullable = false)
    private double destinationLatitude;

    @Column(name = "destination_longitude", nullable = false)
    private double destinationLongitude;

    @Column(name = "geofence_radius_meters", nullable = false)
    private int geofenceRadiusMeters;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JourneyState state;

    @Column(name = "arrived_at")
    private Instant arrivedAt;

    @Column(name = "notification_consent", nullable = false)
    private boolean notificationConsent;

    @Column(name = "consent_timestamp")
    private Instant consentTimestamp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Journey() {
    }

    public Journey(String travellerPhone, double destinationLatitude, double destinationLongitude,
                   int geofenceRadiusMeters) {
        this(travellerPhone, destinationLatitude, destinationLongitude, geofenceRadiusMeters, false);
    }

    public Journey(String travellerPhone, double destinationLatitude, double destinationLongitude,
                   int geofenceRadiusMeters, boolean notificationConsent) {
        this.travellerPhone = travellerPhone;
        this.destinationLatitude = destinationLatitude;
        this.destinationLongitude = destinationLongitude;
        this.geofenceRadiusMeters = geofenceRadiusMeters;
        this.state = JourneyState.TRACKING;
        this.notificationConsent = notificationConsent;
        this.consentTimestamp = notificationConsent ? Instant.now() : null;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTravellerPhone() {
        return travellerPhone;
    }

    public double getDestinationLatitude() {
        return destinationLatitude;
    }

    public double getDestinationLongitude() {
        return destinationLongitude;
    }

    public int getGeofenceRadiusMeters() {
        return geofenceRadiusMeters;
    }

    public JourneyState getState() {
        return state;
    }

    public Instant getArrivedAt() {
        return arrivedAt;
    }

    public boolean hasNotificationConsent() { return notificationConsent; }

    public Instant getConsentTimestamp() { return consentTimestamp; }

    public JourneyState applyEvent(JourneyEvent event, JourneyStateMachine stateMachine) {
        state = stateMachine.transition(state, event);
        return state;
    }

    void markArrivedAt(Instant arrivedAt) {
        this.arrivedAt = arrivedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
