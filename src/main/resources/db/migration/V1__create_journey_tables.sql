CREATE TABLE journeys (
    id UUID PRIMARY KEY,
    traveller_phone VARCHAR(32) NOT NULL,
    destination_latitude DOUBLE PRECISION NOT NULL,
    destination_longitude DOUBLE PRECISION NOT NULL,
    geofence_radius_meters INTEGER NOT NULL,
    state VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_journeys_latitude CHECK (destination_latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_journeys_longitude CHECK (destination_longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_journeys_geofence_radius CHECK (geofence_radius_meters > 0),
    CONSTRAINT ck_journeys_state CHECK (state IN (
        'TRACKING', 'CALLING', 'RETRY_SCHEDULED', 'ACCEPTED', 'REJECTED'
    ))
);

CREATE INDEX ix_journeys_state ON journeys (state);

CREATE TABLE notification_attempts (
    id UUID PRIMARY KEY,
    journey_id UUID NOT NULL REFERENCES journeys (id),
    attempt_number INTEGER NOT NULL,
    provider_call_id VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    scheduled_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_notification_attempt_journey_number UNIQUE (journey_id, attempt_number),
    CONSTRAINT uq_notification_attempt_provider_call UNIQUE (provider_call_id),
    CONSTRAINT ck_notification_attempt_number CHECK (attempt_number > 0),
    CONSTRAINT ck_notification_attempt_status CHECK (status IN (
        'PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED'
    ))
);

CREATE INDEX ix_notification_attempts_journey ON notification_attempts (journey_id);
CREATE INDEX ix_notification_attempts_status_scheduled ON notification_attempts (status, scheduled_at);
