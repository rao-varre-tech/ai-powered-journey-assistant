package com.journeyassistant.location;

import com.journeyassistant.journey.Journey;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "location_updates")
public class LocationUpdate {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journey_id", nullable = false)
    private Journey journey;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "location_timestamp", nullable = false)
    private Instant timestamp;

    protected LocationUpdate() {
    }

    public LocationUpdate(Journey journey, double latitude, double longitude, Instant timestamp) {
        this.journey = journey;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }

    public UUID getId() {
        return id;
    }

    public Journey getJourney() {
        return journey;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
