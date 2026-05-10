-- ============================================================
-- setup_full.sql — Complete database setup for HealthBridge
-- Creates all tables, seeds reference data, and loads sample data
-- Run this BEFORE executing migration_etl.py
-- ============================================================

USE healthbridge;

-- ── 0. Drop tables in safe order (child first) ──────────────
DROP TABLE IF EXISTS appointments;
DROP TABLE IF EXISTS patient_phones;
DROP TABLE IF EXISTS patient_addresses;
DROP TABLE IF EXISTS appointments_new;
DROP TABLE IF EXISTS billing;
DROP TABLE IF EXISTS patients;
DROP TABLE IF EXISTS appt_status_ref;
DROP TABLE IF EXISTS doctors;
DROP TABLE IF EXISTS departments;

-- ── 1. Departments ──────────────────────────────────────────
CREATE TABLE departments (
    dept_id   INT           NOT NULL,
    dept_name VARCHAR(255)  NOT NULL,
    budget    DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (dept_id)
);

INSERT INTO departments VALUES (1, 'General Medicine', 500000.00);
INSERT INTO departments VALUES (2, 'Cardiology',       800000.00);

-- ── 2. Doctors ──────────────────────────────────────────────
CREATE TABLE doctors (
    doctor_id      INT           NOT NULL,
    full_name      VARCHAR(255)  NOT NULL,
    speciality     VARCHAR(255)  NOT NULL DEFAULT 'General',
    contact_no     VARCHAR(20)   NOT NULL DEFAULT '0000-0000000',
    join_date      DATE          NOT NULL DEFAULT '2020-01-01',
    salary_monthly DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    dept_id        INT           NOT NULL DEFAULT 1,
    is_active      TINYINT(1)    NOT NULL DEFAULT 1,
    PRIMARY KEY (doctor_id),
    CONSTRAINT fk_doc_dept FOREIGN KEY (dept_id) REFERENCES departments(dept_id)
);

INSERT INTO doctors VALUES (7,  'Dr. Ayesha Noor',  'Cardiology',      '0300-1111111', '2018-03-15', 150000.00, 2, 1);
INSERT INTO doctors VALUES (12, 'Dr. Kamran Raza',  'General Medicine', '0300-2222222', '2015-06-01', 120000.00, 1, 1);

-- ── 3. Patients ─────────────────────────────────────────────
CREATE TABLE patients (
    patient_id        INT           NOT NULL AUTO_INCREMENT,
    full_name         VARCHAR(255)  NOT NULL,
    date_of_birth     DATE          NOT NULL,
    gender            CHAR(1)       NOT NULL,
    registered_doc_id INT           NULL,
    total_visits      INT           NOT NULL DEFAULT 0,
    notes             TEXT          NULL,
    PRIMARY KEY (patient_id),
    CONSTRAINT fk_patient_doctor
        FOREIGN KEY (registered_doc_id) REFERENCES doctors(doctor_id)
);

INSERT INTO patients (patient_id, full_name, date_of_birth, gender, registered_doc_id, total_visits, notes) VALUES
    (5,  'Ali Hassan', '1985-05-12', 'M', 12,  8, 'diabetic,hypertension'),
    (8,  'Sara Malik', '1990-11-22', 'F',  7,  3, NULL),
    (21, 'Hina Iqbal', '1978-07-05', 'F',  7, 12, 'asthma');

-- ── 4. Patient phones (1NF fix) ─────────────────────────────
CREATE TABLE patient_phones (
    phone_id     INT         NOT NULL AUTO_INCREMENT,
    patient_id   INT         NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    phone_type   VARCHAR(20) NOT NULL DEFAULT 'Mobile',
    PRIMARY KEY (phone_id),
    CONSTRAINT fk_phone_patient
        FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE
);

INSERT INTO patient_phones (patient_id, phone_number, phone_type) VALUES
    (5,  '0312-9876543', 'Mobile'),
    (8,  '0333-1234567', 'Mobile'),
    (21, '0321-5556789', 'Mobile');

-- ── 5. Patient addresses (3NF fix) ──────────────────────────
CREATE TABLE patient_addresses (
    address_id    INT          NOT NULL AUTO_INCREMENT,
    patient_id    INT          NOT NULL,
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255) NULL,
    city          VARCHAR(100) NOT NULL,
    address_type  VARCHAR(20)  NOT NULL DEFAULT 'Home',
    PRIMARY KEY (address_id),
    CONSTRAINT fk_addr_patient
        FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE
);

INSERT INTO patient_addresses (patient_id, address_line1, address_line2, city) VALUES
    (5,  'House 12', 'Street 4, Gulberg', 'Lahore'),
    (8,  'Block B',  'Phase 5, DHA',      'Karachi'),
    (21, 'House 5',  'DHA Phase 1',       'Lahore');

-- ── 6. Status reference table (R2 fix) ──────────────────────
CREATE TABLE appt_status_ref (
    status_code CHAR(1)     NOT NULL,
    description VARCHAR(50) NOT NULL,
    PRIMARY KEY (status_code)
);

INSERT INTO appt_status_ref VALUES
    ('P', 'Pending'),
    ('C', 'Completed'),
    ('X', 'Cancelled'),
    ('H', 'On Hold'),
    ('R', 'Rescheduled');

-- ── 7. Refactored appointments table (migration target) ──────
CREATE TABLE appointments (
    appt_id        INT           NOT NULL,
    patient_id     INT           NOT NULL,
    doc_id         INT           NOT NULL,
    appt_datetime  DATETIME      NOT NULL,
    status         CHAR(1)       NOT NULL,
    fee            DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    discount       DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    room_number    INT           NOT NULL,
    building_block VARCHAR(20)   NOT NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (appt_id),
    CONSTRAINT fk_appt_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    CONSTRAINT fk_appt_doctor  FOREIGN KEY (doc_id)     REFERENCES doctors(doctor_id),
    CONSTRAINT fk_appt_status  FOREIGN KEY (status)     REFERENCES appt_status_ref(status_code)
);

-- ── 8. Billing (for view dependency) ────────────────────────
CREATE TABLE billing (
    bill_no    VARCHAR(50)   NOT NULL,
    pid        INT           NOT NULL,
    services   TEXT          NULL,
    svc_cost   DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    tax_pct    DECIMAL(5,2)  NOT NULL DEFAULT 0.00,
    paid       DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    created    DATE          NOT NULL,
    created_by VARCHAR(100)  NULL,
    PRIMARY KEY (bill_no),
    CONSTRAINT fk_bill_patient FOREIGN KEY (pid) REFERENCES patients(patient_id)
);

-- ── 9. Derived billing view (R1 fix) ────────────────────────
CREATE OR REPLACE VIEW v_billing_summary AS
SELECT
    bill_no,
    pid,
    services,
    svc_cost,
    tax_pct,
    ROUND(svc_cost * tax_pct / 100, 2)                   AS tax_amt,
    ROUND(svc_cost + svc_cost * tax_pct / 100, 2)        AS grand_total,
    paid,
    ROUND(svc_cost + svc_cost * tax_pct / 100 - paid, 2) AS balance,
    created,
    created_by
FROM billing;

SELECT 'Schema setup complete. All tables created.' AS result;
