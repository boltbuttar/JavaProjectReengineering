-- ============================================================
-- legacy_schema.sql — Original HealthBridge Legacy Schema
-- For Part E analysis reference
-- ============================================================

CREATE DATABASE IF NOT EXISTS healthbridge;
USE healthbridge;

CREATE TABLE IF NOT EXISTS pat_master (
    pid         INT,
    p_name      VARCHAR(255),
    dob         VARCHAR(50),            -- stored as 'DD/MM/YYYY' plain text
    sex         CHAR(1),                -- 'M', 'F', or '3' for non-binary
    ph1         VARCHAR(255),
    ph2         VARCHAR(255),
    ph3         VARCHAR(255),           -- repeating phone group
    addr1       VARCHAR(255),
    addr2       VARCHAR(255),
    city        VARCHAR(255),
    reg_doc     VARCHAR(255),           -- doctor full name stored as plain text
    reg_doc_id  VARCHAR(255),           -- sometimes INT string, sometimes 'DR-042'
    total_visits INT,                   -- updated manually, not via trigger
    last_bill   FLOAT,                  -- stores PKR amounts; FLOAT used for currency
    notes       TEXT                    -- JSON blobs, free text, and CSV tags all mixed
);

CREATE TABLE IF NOT EXISTS appointments (
    appt_id     INT,
    patient_id  INT,
    patient_nm  VARCHAR(255),           -- duplicated from pat_master
    patient_ph  VARCHAR(255),           -- duplicated from pat_master
    doc_id      INT,
    doc_name    VARCHAR(255),           -- duplicated from doctors table
    appt_date   VARCHAR(50),            -- 'YYYY-MM-DD HH:MM' stored as text
    status      CHAR(1),                -- 'P'=Pending 'C'=Complete 'X'=Cancel 'H'=Hold 'R'=Rescheduled
    fee         FLOAT,
    discount    FLOAT,
    net_fee     FLOAT,                  -- always = fee - discount (derived value)
    room        VARCHAR(255)            -- 'Room 3 Block B' — two facts in one column
);

CREATE TABLE IF NOT EXISTS doctors (
    DoctorID    INT PRIMARY KEY,
    FullName    VARCHAR(255),
    Speciality  VARCHAR(255),
    ContactNo   VARCHAR(255),
    JoinDt      VARCHAR(50),            -- date stored as text
    Salary      FLOAT,                  -- monthly salary stored as FLOAT
    dept_id     INT,                    -- references departments but no FK defined
    isActive    CHAR(1)                 -- 'Y', 'N', or sometimes '1'
);

CREATE TABLE IF NOT EXISTS billing (
    bill_no     VARCHAR(50),            -- intended as PK but no constraint defined
    pid         INT,
    pname       VARCHAR(255),           -- patient name duplicated again
    services    TEXT,                   -- 'Lab,Xray,OPD' — comma-separated list
    svc_cost    FLOAT,
    tax_pct     FLOAT,
    tax_amt     FLOAT,                  -- derived: svc_cost * tax_pct / 100
    grand_total FLOAT,                  -- derived: svc_cost + tax_amt
    paid        FLOAT,
    balance     FLOAT,                  -- derived: grand_total - paid
    created     VARCHAR(50),            -- date stored as text
    created_by  VARCHAR(255)            -- username as free text; no FK to users
);

CREATE TABLE IF NOT EXISTS departments (
    dept_id     INT PRIMARY KEY,
    dept_nm     VARCHAR(255),
    hod         VARCHAR(255),           -- head-of-department stored as plain name
    budget      FLOAT
);

-- Sample data
INSERT INTO doctors VALUES (12, 'Dr. Kamran Raza', 'Cardiology', '0300-1234567', '01/06/2015', 150000.0, 1, 'Y');
INSERT INTO doctors VALUES (7,  'Dr. Ayesha Noor', 'Pediatrics', '0321-9876543', '15/09/2018', 120000.0, 2, 'Y');

INSERT INTO departments VALUES (1, 'Cardiology', 'Dr. Ahmed Khan', 500000.0);
INSERT INTO departments VALUES (2, 'Pediatrics', 'Dr. Sara Ali',   300000.0);

INSERT INTO pat_master VALUES (5,  'Ali Hassan',  '12/05/1985', 'M', '0312-9876543', '', '', 'House 12', 'Street 4', 'Lahore',  'Dr. Kamran Raza', '12',     8, 15000.0, 'diabetic,hypertension');
INSERT INTO pat_master VALUES (8,  'Sara Malik',  '22/11/1990', 'F', '0333-1234567', '', '', 'Block B',  'Phase 5',  'Karachi', 'Dr. Ayesha Noor',  'DR-007', 3, 2000.0,  '');
INSERT INTO pat_master VALUES (21, 'Hina Iqbal',  '05/07/1978', 'F', '0321-5556789', '', '', 'House 5',  'DHA',      'Lahore',  'Dr. Ayesha Noor',  '7',      12, 8500.0, 'asthma');
