CREATE TABLE journey_messages (
    id UUID PRIMARY KEY,
    journey_id UUID NOT NULL REFERENCES journeys (id),
    channel VARCHAR(16) NOT NULL,
    provider_message_id VARCHAR(128),
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_journey_message_channel UNIQUE (journey_id, channel),
    CONSTRAINT uq_journey_message_provider_id UNIQUE (provider_message_id),
    CONSTRAINT ck_journey_message_channel CHECK (channel IN ('SMS', 'WHATSAPP')),
    CONSTRAINT ck_journey_message_status CHECK (status IN (
        'QUEUED', 'SENT', 'DELIVERED', 'READ', 'FAILED', 'UNDELIVERED'
    ))
);

CREATE INDEX ix_journey_messages_journey ON journey_messages (journey_id);
