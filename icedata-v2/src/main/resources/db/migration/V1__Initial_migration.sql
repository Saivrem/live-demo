CREATE TABLE IF NOT EXISTS suppliers
(
    supplier_id   BIGINT NOT NULL PRIMARY KEY,
    supplier_name VARCHAR(255),
    is_sponsor    INT,
    brand_logo    VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS supplier_mappings
(
    record_id       BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    supplier_id     BIGINT NOT NULL,
    m_supplier_name VARCHAR(255),
    INDEX supplier_id (supplier_id),
    INDEX m_supplier_name (m_supplier_name)
);

CREATE TABLE IF NOT EXISTS languages
(
    lang_id       BIGINT       NOT NULL PRIMARY KEY,
    int_lang_name VARCHAR(255) NOT NULL,
    short_code    VARCHAR(10)  NOT NULL,
    updated       BIGINT
);

CREATE TABLE IF NOT EXISTS language_names
(
    lang_name_id        BIGINT NOT NULL PRIMARY KEY,
    translation_lang_id BIGINT NOT NULL,
    target_lang_id      BIGINT,
    name_translation    VARCHAR(255),
    updated             BIGINT,
    FOREIGN KEY (target_lang_id) REFERENCES languages (lang_id)
);

ALTER TABLE supplier_mappings
    ADD CONSTRAINT fk_supplier
        FOREIGN KEY (supplier_id) REFERENCES suppliers (supplier_id);