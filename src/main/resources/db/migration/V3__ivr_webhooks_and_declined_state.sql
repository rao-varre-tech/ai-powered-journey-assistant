ALTER TABLE journeys DROP CONSTRAINT ck_journeys_state;
UPDATE journeys SET state = 'DECLINED' WHERE state = 'REJECTED';
ALTER TABLE journeys ADD CONSTRAINT ck_journeys_state CHECK (state IN (
    'TRACKING', 'ARRIVED', 'CALLING', 'RETRY_SCHEDULED', 'ACCEPTED', 'DECLINED', 'REJECTED'
));

ALTER TABLE notification_attempts ADD COLUMN last_processed_digit VARCHAR(1);
ALTER TABLE notification_attempts ADD COLUMN invalid_digit_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE notification_attempts ADD CONSTRAINT ck_notification_invalid_digit_count CHECK (invalid_digit_count >= 0);
