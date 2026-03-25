ALTER TABLE transactions
    ADD COLUMN initiated_by VARCHAR(255),
    ADD COLUMN ip_address  VARCHAR(45);

COMMENT ON COLUMN transactions.initiated_by IS 'User or system that initiated the transaction';
COMMENT ON COLUMN transactions.ip_address IS 'IP address of the request origin (IPv4 or IPv6)';
