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

ALTER TABLE supplier_mappings
    ADD CONSTRAINT fk_supplier
        FOREIGN KEY (supplier_id) REFERENCES suppliers (supplier_id);