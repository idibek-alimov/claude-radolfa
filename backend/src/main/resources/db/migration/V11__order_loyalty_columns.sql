-- Track loyalty points redeemed at checkout and awarded after payment
-- so that cancellations and refunds can correctly reverse both flows.
ALTER TABLE orders
    ADD COLUMN loyalty_points_redeemed INT NOT NULL DEFAULT 0,
    ADD COLUMN loyalty_points_awarded  INT NOT NULL DEFAULT 0;
