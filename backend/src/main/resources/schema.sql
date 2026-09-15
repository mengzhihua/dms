-- DMS 经销商管理系统 schema (H2 / MySQL 兼容)
CREATE TABLE IF NOT EXISTS seq_no (
    name VARCHAR(64) PRIMARY KEY,
    seq_value BIGINT NOT NULL
);

-- ============ network 经销商网络 ============
CREATE TABLE IF NOT EXISTS dms_dealer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    type VARCHAR(16) NOT NULL,               -- DEALER 经销商 / DIRECT 直营店
    level VARCHAR(8),                        -- A/B/C
    region VARCHAR(64),
    province VARCHAR(64),
    city VARCHAR(64),
    address VARCHAR(255),
    contact VARCHAR(64),
    phone VARCHAR(32),
    status VARCHAR(16),                      -- ACTIVE/SUSPENDED/TERMINATED
    contract_start DATE,
    contract_end DATE,
    credit_limit DECIMAL(18,2),
    labor_rate DECIMAL(10,2),                -- 工时单价 元/小时
    tax_no VARCHAR(64),
    bank_account VARCHAR(128),
    parent_dealer_code VARCHAR(32),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS dms_dealer_target (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dealer_code VARCHAR(32) NOT NULL,
    year_month VARCHAR(8) NOT NULL,          -- yyyy-MM
    sales_target INT,
    service_target INT,
    revenue_target DECIMAL(18,2),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE(dealer_code, year_month)
);

CREATE TABLE IF NOT EXISTS dms_dealer_assessment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dealer_code VARCHAR(32) NOT NULL,
    year_month VARCHAR(8) NOT NULL,
    sales_score DECIMAL(6,2),
    service_score DECIMAL(6,2),
    csi_score DECIMAL(6,2),
    compliance_score DECIMAL(6,2),
    total DECIMAL(6,2),
    grade VARCHAR(4),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE(dealer_code, year_month)
);

CREATE TABLE IF NOT EXISTS dms_technician (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dealer_code VARCHAR(32) NOT NULL,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(64),
    level VARCHAR(16),                       -- 初级/中级/高级/技师长
    skills VARCHAR(255),
    status VARCHAR(16),                      -- IDLE/BUSY/OFF
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS dms_bay (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dealer_code VARCHAR(32) NOT NULL,
    code VARCHAR(32) NOT NULL,
    type VARCHAR(16),                        -- 机修/钣金/喷漆/快保
    status VARCHAR(16),                      -- IDLE/BUSY/OFF
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE(dealer_code, code)
);

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
    status VARCHAR(16),                      -- NEW/ALLOCATED/INVOICED/DELIVERED/CANCELLED
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS dms_vehicle_stock (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dealer_code VARCHAR(32) NOT NULL,
    vin VARCHAR(32) NOT NULL UNIQUE,
    model_code VARCHAR(32),
    color VARCHAR(32),
    status VARCHAR(16),                      -- IN_STOCK/ALLOCATED/SOLD
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- ============ customer 客户与车辆 ============
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
    updated_at TIMESTAMP
);

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
    updated_at TIMESTAMP
);

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
);

-- ============ parts 备件库存 ============
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
);

CREATE TABLE IF NOT EXISTS dms_part_stock (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dealer_code VARCHAR(32) NOT NULL,
    part_no VARCHAR(32) NOT NULL,
    location VARCHAR(32),                    -- 库位 A-01-02
    batch_no VARCHAR(32),
    qty INT DEFAULT 0,
    reserved_qty INT DEFAULT 0,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE(dealer_code, part_no, location, batch_no)
);

CREATE TABLE IF NOT EXISTS dms_stock_movement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dealer_code VARCHAR(32) NOT NULL,
    part_no VARCHAR(32) NOT NULL,
    type VARCHAR(16),                        -- IN/OUT/RESERVE/RELEASE/ADJUST
    qty INT,
    ref_type VARCHAR(32),
    ref_no VARCHAR(40),
    location VARCHAR(32),
    batch_no VARCHAR(32),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- ============ guide 维修指导 ============
CREATE TABLE IF NOT EXISTS dms_labor_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    standard_hours DECIMAL(8,2),
    category VARCHAR(64),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS dms_repair_guide (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    model_codes VARCHAR(255),                -- 逗号分隔或 ALL
    dtc_codes VARCHAR(255),
    symptoms VARCHAR(255),
    diagnosis_steps CLOB,
    repair_steps CLOB,
    labor_item_codes VARCHAR(255),
    part_nos VARCHAR(255),
    difficulty VARCHAR(16),
    safety_notes VARCHAR(512),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS dms_technical_bulletin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    title VARCHAR(255),
    model_codes VARCHAR(255),
    content CLOB,
    issue_date DATE,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- ============ workshop 维修工单 ============
CREATE TABLE IF NOT EXISTS dms_appointment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dealer_code VARCHAR(32) NOT NULL,
    customer_id BIGINT,
    vehicle_id BIGINT,
    appointment_time TIMESTAMP,
    service_type VARCHAR(16),                -- 保养/维修/事故/召回
    status VARCHAR(16),                      -- BOOKED/ARRIVED/CANCELLED/NO_SHOW
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

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
    order_type VARCHAR(16),                  -- REGULAR/WARRANTY/INSURANCE/INTERNAL
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
);

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
);

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
);

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
);

CREATE TABLE IF NOT EXISTS dms_warranty_claim (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    claim_no VARCHAR(40) UNIQUE,
    order_id BIGINT,
    dealer_code VARCHAR(32),
    amount DECIMAL(18,2),
    status VARCHAR(16),                      -- SUBMITTED/APPROVED/REJECTED/PAID
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- ============ survey 满意度调研 ============
CREATE TABLE IF NOT EXISTS dms_survey_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(128),
    type VARCHAR(16),                        -- SERVICE/SALES
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS dms_survey_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    seq INT,
    text VARCHAR(512),
    type VARCHAR(16),                        -- SCORE/NPS/TEXT
    weight DECIMAL(6,3) DEFAULT 1,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS dms_survey (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    survey_no VARCHAR(40) UNIQUE,
    template_code VARCHAR(32),
    dealer_code VARCHAR(32),
    customer_id BIGINT,
    order_id BIGINT,
    sales_order_id BIGINT,
    channel VARCHAR(16),                     -- SMS/APP/PHONE
    status VARCHAR(16),                      -- PENDING/ANSWERED/EXPIRED
    sent_time TIMESTAMP,
    answered_time TIMESTAMP,
    total_score DECIMAL(6,2),
    nps_score DECIMAL(6,2),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS dms_survey_answer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    survey_id BIGINT NOT NULL,
    question_id BIGINT,
    score DECIMAL(6,2),
    text VARCHAR(1024),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS dms_complaint (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    complaint_no VARCHAR(40) UNIQUE,
    dealer_code VARCHAR(32),
    customer_id BIGINT,
    survey_id BIGINT,
    order_id BIGINT,
    content VARCHAR(1024),
    level VARCHAR(16),                       -- HIGH/MEDIUM
    status VARCHAR(16),                      -- OPEN/PROCESSING/RESOLVED/CLOSED
    handler VARCHAR(64),
    resolution VARCHAR(1024),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- ============ invoice 发票 ============
CREATE TABLE IF NOT EXISTS dms_invoice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_no VARCHAR(40) UNIQUE,           -- 内部申请号
    order_id BIGINT,
    sales_order_id BIGINT,
    dealer_code VARCHAR(32),
    invoice_type VARCHAR(16),                -- NORMAL/SPECIAL/ELECTRONIC
    buyer_name VARCHAR(255),
    buyer_tax_no VARCHAR(64),
    buyer_address VARCHAR(255),
    buyer_bank VARCHAR(255),
    seller_name VARCHAR(255),
    seller_tax_no VARCHAR(64),
    amount DECIMAL(18,2),                    -- 含税
    tax_rate DECIMAL(5,4) DEFAULT 0.13,
    tax_amount DECIMAL(18,2),
    net_amount DECIMAL(18,2),
    status VARCHAR(16),                      -- DRAFT/ISSUING/ISSUED/RED_FLUSHING/RED_FLUSHED/FAILED
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
);

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
);

CREATE TABLE IF NOT EXISTS dms_tax_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dealer_code VARCHAR(32) NOT NULL UNIQUE,
    tax_rate DECIMAL(5,4) DEFAULT 0.13,
    seller_name VARCHAR(255),
    seller_tax_no VARCHAR(64),
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- ============ oms 备件补货(DMS -> OMS 渠道订单) ============
CREATE TABLE IF NOT EXISTS dms_replenish_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    replenish_no VARCHAR(40) NOT NULL UNIQUE,
    dealer_code VARCHAR(32) NOT NULL,
    shop_code VARCHAR(32),
    status VARCHAR(16) NOT NULL,             -- DRAFT/PUSHING/PUSHED/SHIPPED/RECEIVED/CANCELLED
    source VARCHAR(16),                      -- MANUAL/SHORTAGE
    items VARCHAR(4000),                     -- JSON [{partNo,name,qty,price}]
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
);

-- ============ 系统用户（登录/角色/数据范围） ============
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    real_name VARCHAR(64),
    role VARCHAR(24) NOT NULL,               -- ADMIN/OEM/DEALER_MANAGER/ADVISOR/TECHNICIAN/FINANCE
    dealer_code VARCHAR(32),
    enabled BOOLEAN DEFAULT TRUE,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
