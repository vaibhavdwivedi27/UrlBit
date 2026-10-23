-- Migration V2: Add url_click_referrer table for referrer domain aggregations

CREATE TABLE url_click_referrer (
    short_url_id  BIGINT       NOT NULL REFERENCES short_urls(id),
    day           DATE         NOT NULL,
    referrer_host VARCHAR(255) NOT NULL,
    click_count   BIGINT       NOT NULL DEFAULT 0,

    PRIMARY KEY (short_url_id, day, referrer_host)
);

CREATE INDEX idx_url_click_referrer_query
    ON url_click_referrer (short_url_id, day);
