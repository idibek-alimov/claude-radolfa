-- ================================================================
-- V15__pickpoint_hours.sql
--
-- Weekly schedule rows for pickpoints. One row per (pickpoint, day).
-- Absence of a row means the pickpoint is closed that day.
-- day_of_week: 1=Monday … 7=Sunday  (Java DayOfWeek.getValue())
-- ================================================================

CREATE TABLE pickpoint_hours (
    id           BIGSERIAL PRIMARY KEY,
    pickpoint_id BIGINT    NOT NULL REFERENCES pickpoint(id) ON DELETE CASCADE,
    day_of_week  SMALLINT  NOT NULL,
    open_time    TIME      NOT NULL,
    close_time   TIME      NOT NULL,
    CONSTRAINT uq_pickpoint_day  UNIQUE (pickpoint_id, day_of_week),
    CONSTRAINT chk_day_of_week   CHECK  (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT chk_times         CHECK  (close_time > open_time)
);

CREATE INDEX idx_pickpoint_hours_pickpoint ON pickpoint_hours(pickpoint_id);
