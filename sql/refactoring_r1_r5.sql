-- ============================================================
-- refactoring_r1_r5.sql — Five Refactoring Scripts
-- Part F2
-- ============================================================

USE healthbridge;

-- ============================================================
-- R1: Fix Derived Data in billing
-- Removes tax_amt, grand_total, balance; adds view for compute-on-read
-- ============================================================

-- Drop derived columns (safe if they can always be recomputed)
ALTER TABLE billing DROP COLUMN IF EXISTS tax_amt;
ALTER TABLE billing DROP COLUMN IF EXISTS grand_total;
ALTER TABLE billing DROP COLUMN IF EXISTS balance;

-- View computes all derived values on read
CREATE OR REPLACE VIEW v_billing_summary AS
SELECT
    bill_no,
    pid,
    pname,
    services,
    svc_cost,
    tax_pct,
    ROUND(svc_cost * tax_pct / 100, 2)                    AS tax_amt,
    ROUND(svc_cost + svc_cost * tax_pct / 100, 2)         AS grand_total,
    paid,
    ROUND(svc_cost + svc_cost * tax_pct / 100 - paid, 2)  AS balance,
    created,
    created_by
FROM billing;


-- ============================================================
-- R2: Fix Overloaded Column — appointments.status
-- Replace opaque CHAR(1) with FK-enforced reference table
-- ============================================================

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

-- Add FK (will fail if invalid codes exist — clean first)
ALTER TABLE appointments
    ADD CONSTRAINT IF NOT EXISTS fk_appt_status
    FOREIGN KEY (status) REFERENCES appt_status_ref(status_code);


-- ============================================================
-- R3: Fix Inconsistent Naming — doctors table
-- Standardise all columns to lowercase snake_case
-- ============================================================

ALTER TABLE doctors RENAME COLUMN DoctorID   TO doctor_id;
ALTER TABLE doctors RENAME COLUMN FullName   TO full_name;
ALTER TABLE doctors RENAME COLUMN Speciality TO speciality;
ALTER TABLE doctors RENAME COLUMN ContactNo  TO contact_no;
ALTER TABLE doctors RENAME COLUMN JoinDt     TO join_date;
ALTER TABLE doctors RENAME COLUMN Salary     TO salary_monthly;
ALTER TABLE doctors RENAME COLUMN isActive   TO is_active;


-- ============================================================
-- R4: Fix Missing Constraints — billing and appointments
-- ============================================================

-- Step 1: Add PK to billing
ALTER TABLE billing ADD PRIMARY KEY (bill_no);

-- Step 2: Archive orphan billing rows before delete
CREATE TABLE IF NOT EXISTS billing_orphan_archive AS
    SELECT * FROM billing WHERE pid NOT IN (SELECT pid FROM pat_master);

-- Step 3: Delete orphan billing rows
DELETE FROM billing WHERE pid NOT IN (SELECT pid FROM pat_master);

-- Step 4: Add FK from billing to pat_master
ALTER TABLE billing
    ADD CONSTRAINT fk_billing_patient
    FOREIGN KEY (pid) REFERENCES pat_master(pid);

-- Step 5: Delete orphan appointments (doc_id not in doctors)
DELETE FROM appointments WHERE doc_id NOT IN (SELECT DoctorID FROM doctors);

-- Step 6: Add FK from appointments to doctors
ALTER TABLE appointments
    ADD CONSTRAINT fk_appt_doctor
    FOREIGN KEY (doc_id) REFERENCES doctors(DoctorID);


-- ============================================================
-- R5: Add Audit Trail to appointments
-- ============================================================

ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS created_by  VARCHAR(100) NULL,
    ADD COLUMN IF NOT EXISTS updated_by  VARCHAR(100) NULL;


-- ============================================================
-- Post-migration validation queries (G4)
-- ============================================================

-- V1: Row count
SELECT COUNT(*) AS migrated_rows FROM appointments_new;

-- V2: Null datetime check
SELECT COUNT(*) AS null_dates FROM appointments_new WHERE appt_datetime IS NULL;

-- V3: Distinct valid statuses
SELECT DISTINCT status FROM appointments_new;

-- V4: Orphan check
SELECT COUNT(*) AS orphans
FROM appointments_new a
LEFT JOIN patients p ON a.patient_id = p.patient_id
WHERE p.patient_id IS NULL;
