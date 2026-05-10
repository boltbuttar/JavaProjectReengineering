-- ============================================================
-- normalised_schema.sql — 3NF Normalised Schema for pat_master
-- Part F1 — Step 2
-- ============================================================

USE healthbridge;

-- Normalised patient table (no repeating groups, no transitive deps)
CREATE TABLE IF NOT EXISTS patients (
    patient_id        INT           NOT NULL AUTO_INCREMENT,
    full_name         VARCHAR(255)  NOT NULL,
    date_of_birth     DATE          NOT NULL,
    gender            CHAR(1)       NOT NULL,
    registered_doc_id INT           NULL,
    total_visits      INT           NOT NULL DEFAULT 0,
    notes             TEXT          NULL,
    PRIMARY KEY (patient_id),
    CONSTRAINT fk_patient_doctor
        FOREIGN KEY (registered_doc_id) REFERENCES doctors(DoctorID)
);

-- 1NF fix: repeating phone group extracted
CREATE TABLE IF NOT EXISTS patient_phones (
    phone_id      INT          NOT NULL AUTO_INCREMENT,
    patient_id    INT          NOT NULL,
    phone_number  VARCHAR(20)  NOT NULL,
    phone_type    VARCHAR(20)  NOT NULL DEFAULT 'Mobile',
    PRIMARY KEY (phone_id),
    CONSTRAINT fk_phone_patient
        FOREIGN KEY (patient_id) REFERENCES patients(patient_id)
            ON DELETE CASCADE
);

-- 3NF fix: address extracted (city is attribute of address, not patient)
CREATE TABLE IF NOT EXISTS patient_addresses (
    address_id    INT           NOT NULL AUTO_INCREMENT,
    patient_id    INT           NOT NULL,
    address_line1 VARCHAR(255)  NOT NULL,
    address_line2 VARCHAR(255)  NULL,
    city          VARCHAR(100)  NOT NULL,
    address_type  VARCHAR(20)   NOT NULL DEFAULT 'Home',
    PRIMARY KEY (address_id),
    CONSTRAINT fk_addr_patient
        FOREIGN KEY (patient_id) REFERENCES patients(patient_id)
            ON DELETE CASCADE
);

-- Status reference table (for R2)
CREATE TABLE IF NOT EXISTS appt_status_ref (
    status_code CHAR(1)     PRIMARY KEY,
    description VARCHAR(50) NOT NULL
);

INSERT IGNORE INTO appt_status_ref VALUES
    ('P', 'Pending'),
    ('C', 'Completed'),
    ('X', 'Cancelled'),
    ('H', 'On Hold'),
    ('R', 'Rescheduled');

-- Refactored appointments table (for migration target)
CREATE TABLE IF NOT EXISTS appointments_new (
    appt_id         INT             NOT NULL,
    patient_id      INT             NOT NULL,
    doc_id          INT             NOT NULL,
    appt_datetime   DATETIME        NOT NULL,       -- T1: proper DATETIME
    status          CHAR(1)         NOT NULL,
    fee             DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    discount        DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    room_number     INT             NOT NULL,        -- T2: split from room
    building_block  VARCHAR(20)     NOT NULL,        -- T2: split from room
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (appt_id),
    CONSTRAINT fk_appt_new_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    CONSTRAINT fk_appt_new_doctor  FOREIGN KEY (doc_id)     REFERENCES doctors(DoctorID),
    CONSTRAINT fk_appt_new_status  FOREIGN KEY (status)     REFERENCES appt_status_ref(status_code)
);

-- Rename for migration convenience
-- (In production: drop old appointments after migration and rename)
-- ALTER TABLE appointments RENAME TO appointments_legacy;
-- ALTER TABLE appointments_new RENAME TO appointments;

-- View: derived patient last bill (replaces last_bill FLOAT in pat_master)
CREATE OR REPLACE VIEW v_patient_last_bill AS
    SELECT pid AS patient_id, MAX(svc_cost) AS last_bill_amount
    FROM billing
    GROUP BY pid;

-- Migrate sample patients from pat_master
INSERT IGNORE INTO patients (patient_id, full_name, date_of_birth, gender, registered_doc_id, total_visits, notes)
VALUES
    (5,  'Ali Hassan', STR_TO_DATE('12/05/1985','%d/%m/%Y'), 'M', 12, 8,  'diabetic,hypertension'),
    (8,  'Sara Malik', STR_TO_DATE('22/11/1990','%d/%m/%Y'), 'F', 7,  3,  NULL),
    (21, 'Hina Iqbal', STR_TO_DATE('05/07/1978','%d/%m/%Y'), 'F', 7,  12, 'asthma');

INSERT IGNORE INTO patient_phones (patient_id, phone_number, phone_type) VALUES
    (5,  '0312-9876543', 'Mobile'),
    (8,  '0333-1234567', 'Mobile'),
    (21, '0321-5556789', 'Mobile');

INSERT IGNORE INTO patient_addresses (patient_id, address_line1, address_line2, city) VALUES
    (5,  'House 12', 'Street 4', 'Lahore'),
    (8,  'Block B',  'Phase 5',  'Karachi'),
    (21, 'House 5',  'DHA',      'Lahore');
