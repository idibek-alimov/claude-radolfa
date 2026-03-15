-- V18__add_discount_percentage.sql
-- Adds ERP-sourced discount percentage to SKUs.
-- Synced directly from ERPNext — source of truth for discount %.

ALTER TABLE skus ADD COLUMN discount_percentage NUMERIC(5,2);
