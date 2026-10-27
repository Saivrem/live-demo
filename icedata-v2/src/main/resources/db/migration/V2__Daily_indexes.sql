CREATE TABLE daily_indexes (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    repository VARCHAR(16) NOT NULL,
    lang_code VARCHAR(10) NOT NULL,
    format VARCHAR(3) NOT NULL,
    last_modified DATETIME(6),
    checked_at DATETIME(6) NOT NULL,
    http_status INT,
    result VARCHAR(32) NOT NULL,
    CONSTRAINT uk_daily_index UNIQUE (repository, lang_code, format)
);
