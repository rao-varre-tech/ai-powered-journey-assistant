ALTER TABLE journeys
    ADD COLUMN arrived_at TIMESTAMPTZ;

ALTER TABLE journeys
    DROP CONSTRAINT ck_journeys_state;

ALTER TABLE journeys
    ADD CONSTRAINT ck_journeys_state CHECK (state IN (
        'TRACKING', 'ARRIVED', 'CALLING', 'RETRY_SCHEDULED', 'ACCEPTED', 'REJECTED'
    ));

CREATE TABLE location_updates (
    id UUID PRIMARY KEY,
    journey_id UUID NOT NULL REFERENCES journeys (id),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    location_timestamp TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_location_updates_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT ck_location_updates_longitude CHECK (longitude BETWEEN -180 AND 180)
);

CREATE INDEX ix_location_updates_journey_timestamp
    ON location_updates (journey_id, location_timestamp DESC);
