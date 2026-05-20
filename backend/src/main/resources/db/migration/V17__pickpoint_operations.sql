-- ----------------------------------------------------------------
-- Pickup point operations: customer returns
-- ----------------------------------------------------------------

CREATE TABLE customer_returns (
    id                          BIGSERIAL    PRIMARY KEY,
    order_id                    BIGINT       NOT NULL REFERENCES orders(id),
    pickpoint_id                BIGINT       NOT NULL REFERENCES pickpoint(id),
    received_by_staff_id        BIGINT       NOT NULL REFERENCES users(id),
    received_at                 TIMESTAMPTZ  NOT NULL,
    status                      VARCHAR(30)  NOT NULL DEFAULT 'RECEIVED',
    sent_to_warehouse_at        TIMESTAMPTZ,
    sent_confirmed_by_staff_id  BIGINT       REFERENCES users(id),
    notes                       VARCHAR(500),
    refund_approved_at          TIMESTAMPTZ,
    refund_approved_by_admin_id BIGINT       REFERENCES users(id),
    gateway_refund_id           VARCHAR(100),
    refunded_at                 TIMESTAMPTZ,
    version                     BIGINT       NOT NULL DEFAULT 0,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE customer_return_items (
    id              BIGSERIAL    PRIMARY KEY,
    return_id       BIGINT       NOT NULL REFERENCES customer_returns(id) ON DELETE CASCADE,
    order_item_id   BIGINT       NOT NULL REFERENCES order_items(id),
    quantity        INT          NOT NULL CHECK (quantity > 0),
    reason          VARCHAR(30)  NOT NULL,
    notes           VARCHAR(500),
    resellability   VARCHAR(20)  NOT NULL DEFAULT 'PENDING_REVIEW',
    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_customer_returns_order_id     ON customer_returns(order_id);
CREATE INDEX idx_customer_returns_pickpoint_id ON customer_returns(pickpoint_id);
CREATE INDEX idx_customer_returns_status       ON customer_returns(status);
CREATE INDEX idx_customer_return_items_return  ON customer_return_items(return_id);
