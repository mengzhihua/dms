-- 增量升级：dms_purchase_receipt_line.remark + dms_daily_report（对应 schema-mysql.sql 相同定义）
-- MySQL 8 不支持 ADD COLUMN IF NOT EXISTS：对已有该列的库执行前请先确认。

ALTER TABLE dms_purchase_receipt_line ADD COLUMN remark VARCHAR(255);

CREATE TABLE IF NOT EXISTS dms_daily_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_date DATE NOT NULL,
    dealer_code VARCHAR(32),
    dealer_name VARCHAR(128),
    check_ins INT DEFAULT 0,
    delivered INT DEFAULT 0,
    settled_orders INT DEFAULT 0,
    revenue DECIMAL(18,2),
    labor_amount DECIMAL(18,2),
    parts_amount DECIMAL(18,2),
    sales_orders INT DEFAULT 0,
    sales_delivered INT DEFAULT 0,
    sales_amount DECIMAL(18,2),
    parts_in INT DEFAULT 0,
    parts_out INT DEFAULT 0,
    shortage_count INT DEFAULT 0,
    surveys INT DEFAULT 0,
    nps DECIMAL(5,1),
    complaints INT DEFAULT 0,
    claims INT DEFAULT 0,
    claim_amount DECIMAL(18,2),
    po_count INT DEFAULT 0,
    po_amount DECIMAL(18,2),
    pending_tasks INT DEFAULT 0,
    generated_at TIMESTAMP NULL,
    remark VARCHAR(255),
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    UNIQUE(report_date, dealer_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
