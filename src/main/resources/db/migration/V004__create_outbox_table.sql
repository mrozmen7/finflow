CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP,
    retry_count INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_status ON outbox_events(status);
CREATE INDEX idx_outbox_created_at ON outbox_events(created_at ASC);

COMMENT ON TABLE outbox_events IS 'Transactional outbox for reliable event publishing to Kafka';
COMMENT ON COLUMN outbox_events.aggregate_type IS 'Domain entity type, e.g. Transaction';
COMMENT ON COLUMN outbox_events.aggregate_id IS 'ID of the domain entity that triggered the event';
COMMENT ON COLUMN outbox_events.event_type IS 'Event name, e.g. TRANSACTION_COMPLETED';
COMMENT ON COLUMN outbox_events.payload IS 'JSON payload of the event';
COMMENT ON COLUMN outbox_events.status IS 'PENDING, SENT, or FAILED';
COMMENT ON COLUMN outbox_events.sent_at IS 'Timestamp when the event was successfully published';
COMMENT ON COLUMN outbox_events.retry_count IS 'Number of failed publish attempts';
