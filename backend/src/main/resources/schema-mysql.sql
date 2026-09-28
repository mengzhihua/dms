-- DMS 经销商管理系统 schema（MySQL 8，全新库初始化；升级脚本放 deploy/mysql/upgrade/）;

CREATE TABLE IF NOT EXISTS seq_no (
    name VARCHAR(64) PRIMARY KEY,
    seq_value BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_dealer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    type VARCHAR(16) NOT NULL,
    level VARCHAR(8),
    region VARCHAR(64),
    province VARCHAR(64),
    city VARCHAR(64),
    address VARCHAR(255),
    contact VARCHAR(64),
    phone VARCHAR(32),
    status VARCHAR(16),
    contract_start DATE,
    contract_end DATE,
    credit_limit DECIMAL(18,2),
    labor_rate DECIMAL(10,2),
    tax_no VARCHAR(64),
    bank_account VARCHAR(128),
    parent_dealer_code VARCHAR(32),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_dealer_target (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dealer_code VARCHAR(32) NOT NULL,
    `year_month` VARCHAR(8) NOT NULL,
    sales_target INT,
    service_target INT,
    revenue_target DECIMAL(18,2),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE(dealer_code, `year_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_dealer_assessment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dealer_code VARCHAR(32) NOT NULL,
    `year_month` VARCHAR(8) NOT NULL,
    sales_score DECIMAL(6,2),
    service_score DECIMAL(6,2),
    csi_score DECIMAL(6,2),
    compliance_score DECIMAL(6,2),
    total DECIMAL(6,2),
    grade VARCHAR(4),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE(dealer_code, `year_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_technician (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dealer_code VARCHAR(32) NOT NULL,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(64),
    level VARCHAR(16),
    skills VARCHAR(255),
    status VARCHAR(16),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_bay (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dealer_code VARCHAR(32) NOT NULL,
    code VARCHAR(32) NOT NULL,
    type VARCHAR(16),
    status VARCHAR(16),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE(dealer_code, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_vehicle_sales_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(40) UNIQUE,
    dealer_code VARCHAR(32) NOT NULL,
    customer_id BIGINT,
    model_code VARCHAR(32),
    vin VARCHAR(32),
    color VARCHAR(32),
    price DECIMAL(18,2),
    deposit DECIMAL(18,2),
    status VARCHAR(16),
    payment_type VARCHAR(8) DEFAULT 'FULL',
    loan_provider VARCHAR(64),
    loan_amount DECIMAL(18,2),
    loan_term_months INT,
    loan_status VARCHAR(16) DEFAULT 'NONE',
    insurance_company VARCHAR(64),
    insurance_policy_no VARCHAR(64),
    insurance_amount DECIMAL(18,2),
    insurance_status VARCHAR(16) DEFAULT 'NONE',
    paid_amount DECIMAL(18,2) DEFAULT 0,
    invoice_id BIGINT,
    survey_id BIGINT,
    pdi_passed BOOLEAN,
    delivered_at TIMESTAMP,
    deliver_remark VARCHAR(255),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_sales_payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    dealer_code VARCHAR(32),
    pay_type VARCHAR(16),
    amount DECIMAL(18,2) NOT NULL,
    method VARCHAR(16),
    paid_at TIMESTAMP,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_vehicle_stock (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dealer_code VARCHAR(32) NOT NULL,
    vin VARCHAR(32) NOT NULL UNIQUE,
    model_code VARCHAR(32),
    color VARCHAR(32),
    status VARCHAR(16),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_customer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    phone VARCHAR(32),
    id_no VARCHAR(64),
    gender VARCHAR(8),
    level VARCHAR(16),
    dealer_code VARCHAR(32),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE(dealer_code, phone),
    UNIQUE KEY uk_customer_dealer_phone (dealer_code, phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_vehicle_model (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128),
    brand VARCHAR(64),
    series VARCHAR(64),
    warranty_months INT,
    warranty_km INT,
    maintenance_interval_km INT,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    maintenance_interval_months INT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_vehicle (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vin VARCHAR(32) NOT NULL UNIQUE,
    plate_no VARCHAR(32),
    model_code VARCHAR(32),
    customer_id BIGINT,
    dealer_code VARCHAR(32),
    mileage INT,
    purchase_date DATE,
    warranty_start DATE,
    warranty_end DATE,
    last_service_date DATE,
    next_service_mileage INT,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_part (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    part_no VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    category VARCHAR(64),
    unit VARCHAR(16),
    cost_price DECIMAL(18,2),
    sale_price DECIMAL(18,2),
    tax_rate DECIMAL(5,4) DEFAULT 0.13,
    min_stock INT DEFAULT 0,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_part_stock (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dealer_code VARCHAR(32) NOT NULL,
    part_no VARCHAR(32) NOT NULL,
    location VARCHAR(32),
    batch_no VARCHAR(32),
    qty INT DEFAULT 0,
    reserved_qty INT DEFAULT 0,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE(dealer_code, part_no, location, batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_stock_movement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dealer_code VARCHAR(32) NOT NULL,
    part_no VARCHAR(32) NOT NULL,
    type VARCHAR(16),
    qty INT,
    ref_type VARCHAR(32),
    ref_no VARCHAR(40),
    location VARCHAR(32),
    batch_no VARCHAR(32),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_labor_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    standard_hours DECIMAL(8,2),
    category VARCHAR(64),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_repair_guide (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    model_codes VARCHAR(255),
    dtc_codes VARCHAR(255),
    symptoms VARCHAR(255),
    diagnosis_steps TEXT,
    repair_steps TEXT,
    labor_item_codes VARCHAR(255),
    part_nos VARCHAR(255),
    difficulty VARCHAR(16),
    safety_notes VARCHAR(512),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_technical_bulletin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    title VARCHAR(255),
    model_codes VARCHAR(255),
    content TEXT,
    issue_date DATE,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_appointment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dealer_code VARCHAR(32) NOT NULL,
    customer_id BIGINT,
    vehicle_id BIGINT,
    appointment_time TIMESTAMP,
    service_type VARCHAR(16),
    status VARCHAR(16),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_work_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(40) UNIQUE,
    dealer_code VARCHAR(32) NOT NULL,
    appointment_id BIGINT,
    customer_id BIGINT,
    vehicle_id BIGINT,
    vin VARCHAR(32),
    plate_no VARCHAR(32),
    mileage_in INT,
    fuel_level VARCHAR(16),
    service_type VARCHAR(16),
    order_type VARCHAR(16),
    complaint VARCHAR(512),
    diagnosis VARCHAR(1024),
    technician_code VARCHAR(32),
    bay_code VARCHAR(32),
    advisor_name VARCHAR(64),
    status VARCHAR(16),
    check_in_time TIMESTAMP,
    quote_time TIMESTAMP,
    approve_time TIMESTAMP,
    dispatch_time TIMESTAMP,
    repair_start_time TIMESTAMP,
    repair_end_time TIMESTAMP,
    qc_time TIMESTAMP,
    settle_time TIMESTAMP,
    deliver_time TIMESTAMP,
    labor_amount DECIMAL(18,2) DEFAULT 0,
    parts_amount DECIMAL(18,2) DEFAULT 0,
    discount_amount DECIMAL(18,2) DEFAULT 0,
    tax_amount DECIMAL(18,2) DEFAULT 0,
    total_amount DECIMAL(18,2) DEFAULT 0,
    warranty_amount DECIMAL(18,2) DEFAULT 0,
    customer_payable DECIMAL(18,2) DEFAULT 0,
    qc_result VARCHAR(16),
    qc_remark VARCHAR(255),
    invoice_id BIGINT,
    survey_id BIGINT,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_work_order_labor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    labor_code VARCHAR(32),
    name VARCHAR(128),
    hours DECIMAL(8,2),
    rate DECIMAL(10,2),
    amount DECIMAL(18,2),
    is_warranty BOOLEAN DEFAULT FALSE,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_work_order_part (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    part_no VARCHAR(32),
    name VARCHAR(128),
    qty INT,
    unit_price DECIMAL(18,2),
    amount DECIMAL(18,2),
    is_warranty BOOLEAN DEFAULT FALSE,
    reserved_flag BOOLEAN DEFAULT FALSE,
    consumed_flag BOOLEAN DEFAULT FALSE,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_work_order_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    from_status VARCHAR(16),
    to_status VARCHAR(16),
    operator VARCHAR(64),
    log_time TIMESTAMP,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_warranty_claim (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    claim_no VARCHAR(40) UNIQUE,
    order_id BIGINT,
    dealer_code VARCHAR(32),
    vin VARCHAR(32),
    plate_no VARCHAR(16),
    mileage INT,
    repair_date DATE,
    fault_code VARCHAR(32),
    fault_desc VARCHAR(255),
    amount DECIMAL(18,2),
    labor_amount DECIMAL(18,2) DEFAULT 0,
    part_amount DECIMAL(18,2) DEFAULT 0,
    approved_amount DECIMAL(18,2),
    oem_remark VARCHAR(255),
    parts_return_required BOOLEAN DEFAULT FALSE,
    return_ship_no VARCHAR(64),
    return_shipped_at TIMESTAMP,
    return_received_at TIMESTAMP,
    settlement_id BIGINT,
    submitted_at TIMESTAMP,
    approved_at TIMESTAMP,
    status VARCHAR(16),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_warranty_claim_line (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    claim_id BIGINT NOT NULL,
    line_type VARCHAR(8),
    code VARCHAR(32),
    name VARCHAR(128),
    qty DECIMAL(10,2),
    unit_price DECIMAL(18,2),
    amount DECIMAL(18,2),
    approved_amount DECIMAL(18,2),
    return_required BOOLEAN DEFAULT FALSE,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_warranty_settlement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    settlement_no VARCHAR(40) UNIQUE,
    dealer_code VARCHAR(32),
    period VARCHAR(7),
    claim_count INT,
    total_amount DECIMAL(18,2),
    status VARCHAR(16),
    paid_at TIMESTAMP,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_survey_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128),
    type VARCHAR(16),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_survey_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    seq INT,
    text VARCHAR(512),
    type VARCHAR(16),
    weight DECIMAL(6,3) DEFAULT 1,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE(template_id, seq),
    UNIQUE KEY uk_question_template_seq (template_id, seq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_survey (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    survey_no VARCHAR(40) UNIQUE,
    template_code VARCHAR(32),
    dealer_code VARCHAR(32),
    customer_id BIGINT,
    order_id BIGINT,
    sales_order_id BIGINT,
    channel VARCHAR(16),
    status VARCHAR(16),
    sent_time TIMESTAMP,
    answered_time TIMESTAMP,
    total_score DECIMAL(6,2),
    nps_score DECIMAL(6,2),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_survey_answer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    survey_id BIGINT NOT NULL,
    question_id BIGINT,
    score DECIMAL(6,2),
    text VARCHAR(1024),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_complaint (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    complaint_no VARCHAR(40) UNIQUE,
    dealer_code VARCHAR(32),
    customer_id BIGINT,
    survey_id BIGINT,
    order_id BIGINT,
    content VARCHAR(1024),
    level VARCHAR(16),
    status VARCHAR(16),
    handler VARCHAR(64),
    resolution VARCHAR(1024),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_invoice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_no VARCHAR(40) UNIQUE,
    order_id BIGINT,
    sales_order_id BIGINT,
    dealer_code VARCHAR(32),
    invoice_type VARCHAR(16),
    buyer_name VARCHAR(255),
    buyer_tax_no VARCHAR(64),
    buyer_address VARCHAR(255),
    buyer_bank VARCHAR(255),
    seller_name VARCHAR(255),
    seller_tax_no VARCHAR(64),
    amount DECIMAL(18,2),
    tax_rate DECIMAL(5,4) DEFAULT 0.13,
    tax_amount DECIMAL(18,2),
    net_amount DECIMAL(18,2),
    status VARCHAR(16),
    tax_invoice_code VARCHAR(32),
    tax_invoice_number VARCHAR(32),
    check_code VARCHAR(64),
    pdf_url VARCHAR(512),
    issued_time TIMESTAMP,
    provider_ref VARCHAR(64),
    error_msg VARCHAR(512),
    red_of_invoice_id BIGINT,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_invoice_line (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    name VARCHAR(255),
    spec VARCHAR(128),
    unit VARCHAR(16),
    qty DECIMAL(10,2),
    unit_price DECIMAL(18,2),
    amount DECIMAL(18,2),
    tax_rate DECIMAL(5,4),
    tax_amount DECIMAL(18,2),
    tax_category_code VARCHAR(32),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_tax_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dealer_code VARCHAR(32) NOT NULL UNIQUE,
    tax_rate DECIMAL(5,4) DEFAULT 0.13,
    seller_name VARCHAR(255),
    seller_tax_no VARCHAR(64),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_replenish_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    replenish_no VARCHAR(40) NOT NULL UNIQUE,
    dealer_code VARCHAR(32) NOT NULL,
    shop_code VARCHAR(32),
    status VARCHAR(16) NOT NULL,
    source VARCHAR(16),
    items VARCHAR(4000),
    oms_order_no VARCHAR(40),
    oms_status VARCHAR(16),
    warehouse_code VARCHAR(32),
    carrier_code VARCHAR(32),
    tracking_no VARCHAR(64),
    location VARCHAR(32),
    last_error VARCHAR(500),
    pushed_at TIMESTAMP,
    shipped_at TIMESTAMP,
    received_at TIMESTAMP,
    synced_at TIMESTAMP,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    real_name VARCHAR(64),
    role VARCHAR(24) NOT NULL,
    dealer_code VARCHAR(32),
    enabled BOOLEAN DEFAULT TRUE,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_purchase_inquiry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inquiry_no VARCHAR(40) UNIQUE,
    dealer_code VARCHAR(32),
    title VARCHAR(255),
    status VARCHAR(16),
    expect_date DATE,
    quoted_at TIMESTAMP,
    oem_remark VARCHAR(255),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_purchase_inquiry_line (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inquiry_id BIGINT NOT NULL,
    part_no VARCHAR(32),
    name VARCHAR(128),
    qty INT,
    target_price DECIMAL(18,2),
    quoted_price DECIMAL(18,2),
    lead_days INT,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_purchase_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    po_no VARCHAR(40) UNIQUE,
    dealer_code VARCHAR(32),
    inquiry_id BIGINT,
    source VARCHAR(16),
    status VARCHAR(16),
    total_amount DECIMAL(18,2),
    received_amount DECIMAL(18,2) DEFAULT 0,
    expect_date DATE,
    oem_order_no VARCHAR(64),
    oem_remark VARCHAR(255),
    statement_id BIGINT,
    submitted_at TIMESTAMP,
    confirmed_at TIMESTAMP,
    closed_at TIMESTAMP,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_purchase_order_line (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    part_no VARCHAR(32),
    name VARCHAR(128),
    qty INT,
    received_qty INT DEFAULT 0,
    unit_price DECIMAL(18,2),
    amount DECIMAL(18,2),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_purchase_receipt (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    receipt_no VARCHAR(40) UNIQUE,
    order_id BIGINT,
    dealer_code VARCHAR(32),
    location VARCHAR(32),
    batch_no VARCHAR(32),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_purchase_receipt_line (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    receipt_id BIGINT NOT NULL,
    order_line_id BIGINT,
    part_no VARCHAR(32),
    qty INT,
    unit_price DECIMAL(18,2),
    amount DECIMAL(18,2),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_purchase_statement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    statement_no VARCHAR(40) UNIQUE,
    dealer_code VARCHAR(32),
    period VARCHAR(7),
    order_count INT,
    total_amount DECIMAL(18,2),
    status VARCHAR(16),
    paid_at TIMESTAMP,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_follow_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_no VARCHAR(40) UNIQUE,
    dealer_code VARCHAR(32),
    customer_id BIGINT,
    vehicle_id BIGINT,
    vin VARCHAR(32),
    plate_no VARCHAR(16),
    customer_name VARCHAR(64),
    phone VARCHAR(32),
    type VARCHAR(32),
    source VARCHAR(8),
    source_ref VARCHAR(64),
    title VARCHAR(128),
    content VARCHAR(512),
    due_date DATE,
    status VARCHAR(16),
    assignee VARCHAR(64),
    result VARCHAR(512),
    done_at TIMESTAMP,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE(dealer_code, type, source_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_notify_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dealer_code VARCHAR(32),
    channel VARCHAR(16),
    receiver VARCHAR(64),
    template_code VARCHAR(32),
    content VARCHAR(1024),
    biz_type VARCHAR(32),
    biz_id BIGINT,
    status VARCHAR(16),
    provider_ref VARCHAR(64),
    error_msg VARCHAR(255),
    sent_at TIMESTAMP,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dms_notify_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) UNIQUE,
    name VARCHAR(64),
    channel VARCHAR(16),
    content VARCHAR(1024),
    enabled BOOLEAN DEFAULT TRUE,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
