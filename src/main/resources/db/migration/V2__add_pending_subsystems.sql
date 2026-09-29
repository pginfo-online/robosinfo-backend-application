-- =====================================================================
-- V2__add_pending_subsystems.sql
-- Migration for Fulfillment, Delivery, WMS, Returns, Reviews,
-- Customer Support, Promotions/Coupons, and Notifications
-- =====================================================================

-- ═══════════════════════════════════════════════════════════════
-- 0. SCHEMAS
-- ═══════════════════════════════════════════════════════════════
CREATE SCHEMA IF NOT EXISTS shipping_schema;
CREATE SCHEMA IF NOT EXISTS delivery_schema;
CREATE SCHEMA IF NOT EXISTS warehouse_schema;
CREATE SCHEMA IF NOT EXISTS review_schema;
CREATE SCHEMA IF NOT EXISTS support_schema;
CREATE SCHEMA IF NOT EXISTS promotion_schema;
CREATE SCHEMA IF NOT EXISTS notification_schema;

-- Fix outbox_events missing retry_count column
ALTER TABLE outbox_events ADD COLUMN IF NOT EXISTS retry_count INT NOT NULL DEFAULT 0;

-- ═══════════════════════════════════════════════════════════════
-- 1. FULFILLMENT & DELIVERY (shipping_schema / delivery_schema)
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE shipping_schema.shipments (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id                UUID NOT NULL,
    seller_id               UUID NOT NULL,
    warehouse_id            UUID,
    awb_number              VARCHAR(100) UNIQUE NOT NULL,
    status                  VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    carrier_name            VARCHAR(100) DEFAULT 'INTERNAL_FLEET',
    tracking_url            VARCHAR(500),
    delivery_partner_id     UUID,
    delivery_otp            VARCHAR(10),
    otp_attempts            INT NOT NULL DEFAULT 0,
    max_otp_attempts        INT NOT NULL DEFAULT 3,
    estimated_delivery_at   TIMESTAMPTZ,
    delivered_at            TIMESTAMPTZ,
    failed_reason           TEXT,
    recipient_name          VARCHAR(255),
    recipient_phone         VARCHAR(20),
    shipping_address_snapshot JSONB,
    cod_amount_paisa        BIGINT DEFAULT 0,
    is_cod                  BOOLEAN DEFAULT FALSE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                 BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_shipments_order ON shipping_schema.shipments(order_id);
CREATE INDEX idx_shipments_seller ON shipping_schema.shipments(seller_id);
CREATE INDEX idx_shipments_delivery_partner ON shipping_schema.shipments(delivery_partner_id, status);
CREATE INDEX idx_shipments_status ON shipping_schema.shipments(status);

CREATE TABLE shipping_schema.shipment_items (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shipment_id             UUID NOT NULL REFERENCES shipping_schema.shipments(id) ON DELETE CASCADE,
    order_item_id           UUID NOT NULL,
    variant_id              UUID NOT NULL,
    qty                     INT NOT NULL CHECK (qty > 0)
);
CREATE INDEX idx_shipment_items_shipment ON shipping_schema.shipment_items(shipment_id);

CREATE TABLE delivery_schema.delivery_partner_locations (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    delivery_partner_id     UUID NOT NULL,
    shipment_id             UUID,
    lat                     DECIMAL(10, 7) NOT NULL,
    lng                     DECIMAL(10, 7) NOT NULL,
    speed                   DECIMAL(5, 2),
    heading                 DECIMAL(5, 2),
    battery_level           INT,
    recorded_at             TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_partner_locations_partner ON delivery_schema.delivery_partner_locations(delivery_partner_id, recorded_at DESC);
CREATE INDEX idx_partner_locations_shipment ON delivery_schema.delivery_partner_locations(shipment_id, recorded_at DESC);

-- ═══════════════════════════════════════════════════════════════
-- 2. WAREHOUSE MANAGEMENT SYSTEM (warehouse_schema)
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE warehouse_schema.warehouses (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                    VARCHAR(255) NOT NULL,
    code                    VARCHAR(50) UNIQUE NOT NULL,
    address_line1           VARCHAR(255) NOT NULL,
    address_line2           VARCHAR(255),
    city                    VARCHAR(100) NOT NULL,
    state                   VARCHAR(100) NOT NULL,
    pincode                 VARCHAR(10) NOT NULL,
    contact_number          VARCHAR(20),
    email                   VARCHAR(255),
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE warehouse_schema.warehouse_zones (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warehouse_id            UUID NOT NULL REFERENCES warehouse_schema.warehouses(id) ON DELETE CASCADE,
    name                    VARCHAR(100) NOT NULL,
    code                    VARCHAR(50) NOT NULL,
    zone_type               VARCHAR(50) NOT NULL,
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE(warehouse_id, code)
);

CREATE TABLE warehouse_schema.warehouse_locations (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    warehouse_id            UUID NOT NULL REFERENCES warehouse_schema.warehouses(id) ON DELETE CASCADE,
    zone_id                 UUID NOT NULL REFERENCES warehouse_schema.warehouse_zones(id) ON DELETE CASCADE,
    aisle                   VARCHAR(20) NOT NULL,
    shelf                   VARCHAR(20) NOT NULL,
    bin                     VARCHAR(20) NOT NULL,
    barcode                 VARCHAR(100) UNIQUE NOT NULL,
    max_weight_grams        INT,
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE(warehouse_id, aisle, shelf, bin)
);

CREATE TABLE warehouse_schema.goods_receipt_notes (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    grn_number              VARCHAR(100) UNIQUE NOT NULL,
    warehouse_id            UUID NOT NULL REFERENCES warehouse_schema.warehouses(id),
    seller_id               UUID NOT NULL,
    status                  VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    consignment_reference   VARCHAR(100),
    received_by             UUID NOT NULL,
    received_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    notes                   TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE warehouse_schema.goods_receipt_items (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    grn_id                  UUID NOT NULL REFERENCES warehouse_schema.goods_receipt_notes(id) ON DELETE CASCADE,
    variant_id              UUID NOT NULL,
    expected_qty            INT NOT NULL DEFAULT 0,
    received_qty            INT NOT NULL DEFAULT 0,
    passed_qty              INT NOT NULL DEFAULT 0,
    failed_qty              INT NOT NULL DEFAULT 0,
    damaged_qty             INT NOT NULL DEFAULT 0,
    disposition             VARCHAR(30) NOT NULL DEFAULT 'ACCEPTED',
    location_id             UUID REFERENCES warehouse_schema.warehouse_locations(id),
    notes                   TEXT
);

CREATE TABLE warehouse_schema.picklists (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    picklist_number         VARCHAR(100) UNIQUE NOT NULL,
    warehouse_id            UUID NOT NULL REFERENCES warehouse_schema.warehouses(id),
    status                  VARCHAR(30) NOT NULL DEFAULT 'GENERATED',
    assigned_staff_id       UUID,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE warehouse_schema.picklist_items (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    picklist_id             UUID NOT NULL REFERENCES warehouse_schema.picklists(id) ON DELETE CASCADE,
    shipment_id             UUID NOT NULL REFERENCES shipping_schema.shipments(id),
    order_item_id           UUID NOT NULL,
    variant_id              UUID NOT NULL,
    location_id             UUID REFERENCES warehouse_schema.warehouse_locations(id),
    qty_to_pick             INT NOT NULL,
    qty_picked              INT NOT NULL DEFAULT 0,
    is_verified             BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE warehouse_schema.packing_slips (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slip_number             VARCHAR(100) UNIQUE NOT NULL,
    shipment_id             UUID NOT NULL REFERENCES shipping_schema.shipments(id),
    package_weight_grams    INT NOT NULL,
    length_cm               DECIMAL(6,2),
    width_cm                DECIMAL(6,2),
    height_cm               DECIMAL(6,2),
    box_type                VARCHAR(50),
    packed_by               UUID NOT NULL,
    packed_at               TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE warehouse_schema.dispatch_manifests (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    manifest_number         VARCHAR(100) UNIQUE NOT NULL,
    warehouse_id            UUID NOT NULL REFERENCES warehouse_schema.warehouses(id),
    carrier_name            VARCHAR(100) NOT NULL,
    vehicle_number          VARCHAR(50),
    driver_name             VARCHAR(100),
    driver_phone            VARCHAR(20),
    status                  VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    total_shipments         INT NOT NULL DEFAULT 0,
    dispatched_at           TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ═══════════════════════════════════════════════════════════════
-- 3. RETURNS & REVERSE LOGISTICS (order_schema)
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE order_schema.return_requests (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    return_number           VARCHAR(100) UNIQUE NOT NULL,
    customer_id             UUID NOT NULL,
    order_id                UUID NOT NULL,
    order_item_id           UUID NOT NULL,
    variant_id              UUID NOT NULL,
    seller_id               UUID NOT NULL,
    return_type             VARCHAR(30) NOT NULL DEFAULT 'REFUND',
    reason                  VARCHAR(50) NOT NULL,
    customer_notes          TEXT,
    photo_urls              JSONB,
    status                  VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    pickup_address_snapshot JSONB,
    rejection_reason        TEXT,
    approved_by             UUID,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                 BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_returns_customer ON order_schema.return_requests(customer_id);
CREATE INDEX idx_returns_order ON order_schema.return_requests(order_id);
CREATE INDEX idx_returns_seller ON order_schema.return_requests(seller_id);
CREATE INDEX idx_returns_status ON order_schema.return_requests(status);

CREATE TABLE order_schema.return_inspections (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    return_request_id       UUID NOT NULL REFERENCES order_schema.return_requests(id) ON DELETE CASCADE,
    warehouse_id            UUID NOT NULL,
    inspected_by            UUID NOT NULL,
    qc_passed               BOOLEAN NOT NULL,
    disposition             VARCHAR(50) NOT NULL,
    inspector_notes         TEXT,
    defect_description      TEXT,
    inspected_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ═══════════════════════════════════════════════════════════════
-- 4. REVIEWS & RATINGS (review_schema)
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE review_schema.reviews (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id             UUID NOT NULL,
    product_id              UUID NOT NULL,
    variant_id              UUID,
    order_id                UUID NOT NULL,
    order_item_id           UUID UNIQUE NOT NULL,
    rating                  INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    title                   VARCHAR(255),
    comment                 TEXT,
    photo_urls              JSONB,
    is_verified_buyer       BOOLEAN NOT NULL DEFAULT TRUE,
    status                  VARCHAR(30) NOT NULL DEFAULT 'APPROVED',
    helpful_votes           INT NOT NULL DEFAULT 0,
    seller_id               UUID,
    seller_response         TEXT,
    seller_responded_at     TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_reviews_product ON review_schema.reviews(product_id, status);
CREATE INDEX idx_reviews_customer ON review_schema.reviews(customer_id);
CREATE INDEX idx_reviews_seller ON review_schema.reviews(seller_id);

CREATE TABLE review_schema.review_helpful_votes (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id               UUID NOT NULL REFERENCES review_schema.reviews(id) ON DELETE CASCADE,
    user_id                 UUID NOT NULL,
    voted_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(review_id, user_id)
);

-- ═══════════════════════════════════════════════════════════════
-- 5. CUSTOMER SUPPORT & HELPDESK (support_schema)
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE support_schema.support_tickets (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_number           VARCHAR(100) UNIQUE NOT NULL,
    customer_id             UUID NOT NULL,
    order_id                UUID,
    order_item_id           UUID,
    category                VARCHAR(50) NOT NULL,
    priority                VARCHAR(30) NOT NULL DEFAULT 'MEDIUM',
    status                  VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    subject                 VARCHAR(255) NOT NULL,
    description             TEXT NOT NULL,
    assigned_agent_id       UUID,
    sla_due_at              TIMESTAMPTZ NOT NULL,
    is_escalated            BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at             TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_tickets_customer ON support_schema.support_tickets(customer_id);
CREATE INDEX idx_tickets_status ON support_schema.support_tickets(status);
CREATE INDEX idx_tickets_agent ON support_schema.support_tickets(assigned_agent_id);
CREATE INDEX idx_tickets_sla ON support_schema.support_tickets(sla_due_at, is_escalated) WHERE status IN ('OPEN', 'IN_PROGRESS');

CREATE TABLE support_schema.ticket_messages (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id               UUID NOT NULL REFERENCES support_schema.support_tickets(id) ON DELETE CASCADE,
    sender_id               UUID NOT NULL,
    sender_role             VARCHAR(50) NOT NULL,
    message                 TEXT NOT NULL,
    attachment_urls         JSONB,
    is_internal_note        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_ticket_messages_ticket ON support_schema.ticket_messages(ticket_id, created_at ASC);

-- ═══════════════════════════════════════════════════════════════
-- 6. PROMOTIONS & COUPONS (promotion_schema)
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE promotion_schema.coupons (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                    VARCHAR(50) UNIQUE NOT NULL,
    description             VARCHAR(255),
    discount_type           VARCHAR(30) NOT NULL,
    discount_value          BIGINT NOT NULL,
    min_order_value_paisa   BIGINT NOT NULL DEFAULT 0,
    max_discount_cap_paisa  BIGINT,
    start_date              TIMESTAMPTZ NOT NULL,
    end_date                TIMESTAMPTZ NOT NULL,
    usage_limit_per_user    INT NOT NULL DEFAULT 1,
    total_usage_limit       INT NOT NULL DEFAULT 1000,
    times_used              INT NOT NULL DEFAULT 0,
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                 BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_coupons_code ON promotion_schema.coupons(code, is_active);

CREATE TABLE promotion_schema.coupon_usages (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    coupon_id               UUID NOT NULL REFERENCES promotion_schema.coupons(id),
    customer_id             UUID NOT NULL,
    order_id                UUID NOT NULL,
    discount_paisa          BIGINT NOT NULL,
    used_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_coupon_usages_coupon_customer ON promotion_schema.coupon_usages(coupon_id, customer_id);

-- ═══════════════════════════════════════════════════════════════
-- 7. NOTIFICATIONS & DEVICE TOKENS (notification_schema)
-- ═══════════════════════════════════════════════════════════════

CREATE TABLE notification_schema.device_tokens (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL,
    token                   VARCHAR(500) NOT NULL,
    platform                VARCHAR(20) NOT NULL DEFAULT 'ANDROID',
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, token)
);
CREATE INDEX idx_device_tokens_user ON notification_schema.device_tokens(user_id);

CREATE TABLE notification_schema.notification_logs (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL,
    type                    VARCHAR(50) NOT NULL,
    channel                 VARCHAR(20) NOT NULL,
    title                   VARCHAR(255) NOT NULL,
    body                    TEXT NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'SENT',
    sent_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notification_logs_user ON notification_schema.notification_logs(user_id, sent_at DESC);
