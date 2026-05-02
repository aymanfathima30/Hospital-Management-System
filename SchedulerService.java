-- ============================================================
-- Hospital Management System — Database Schema
-- ============================================================

CREATE TABLE patients (
    patient_id          INT AUTO_INCREMENT PRIMARY KEY,
    first_name          VARCHAR(80) NOT NULL,
    last_name           VARCHAR(80) NOT NULL,
    date_of_birth       DATE NOT NULL,
    gender              ENUM('Male','Female','Other') NOT NULL,
    phone               VARCHAR(20),
    email               VARCHAR(150),
    address             TEXT,
    blood_type          VARCHAR(5),
    emergency_contact   VARCHAR(150),
    insurance_number    VARCHAR(50),
    registered_date     DATE NOT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    INDEX idx_name (last_name, first_name),
    INDEX idx_insurance (insurance_number)
);

CREATE TABLE doctors (
    doctor_id           INT AUTO_INCREMENT PRIMARY KEY,
    first_name          VARCHAR(80) NOT NULL,
    last_name           VARCHAR(80) NOT NULL,
    specialisation      VARCHAR(100) NOT NULL,
    phone               VARCHAR(20),
    email               VARCHAR(150),
    license_number      VARCHAR(50) UNIQUE,
    department          VARCHAR(80),
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    INDEX idx_spec (specialisation)
);

CREATE TABLE appointments (
    appointment_id      INT AUTO_INCREMENT PRIMARY KEY,
    patient_id          INT NOT NULL REFERENCES patients(patient_id),
    doctor_id           INT NOT NULL REFERENCES doctors(doctor_id),
    scheduled_start     DATETIME NOT NULL,
    scheduled_end       DATETIME NOT NULL,
    appointment_type    VARCHAR(60) NOT NULL,  -- Consultation, Follow-up, Emergency
    status              ENUM('SCHEDULED','COMPLETED','CANCELLED','NO_SHOW') NOT NULL DEFAULT 'SCHEDULED',
    notes               TEXT,
    cancellation_reason TEXT,
    cancelled_at        DATETIME,
    created_at          DATETIME NOT NULL DEFAULT NOW(),
    -- Prevent double-booking via application + DB index
    INDEX idx_doctor_time (doctor_id, scheduled_start, scheduled_end),
    INDEX idx_patient_time (patient_id, scheduled_start)
);

CREATE TABLE medical_records (
    record_id           INT AUTO_INCREMENT PRIMARY KEY,
    patient_id          INT NOT NULL REFERENCES patients(patient_id),
    doctor_id           INT NOT NULL REFERENCES doctors(doctor_id),
    appointment_id      INT REFERENCES appointments(appointment_id),
    visit_date          DATE NOT NULL,
    diagnosis           TEXT NOT NULL,
    treatment_plan      TEXT,
    prescription        TEXT,
    notes               TEXT,
    follow_up_date      DATE,
    created_at          DATETIME NOT NULL DEFAULT NOW(),
    INDEX idx_patient_history (patient_id, visit_date DESC)
);

CREATE TABLE billing (
    bill_id             INT AUTO_INCREMENT PRIMARY KEY,
    patient_id          INT NOT NULL REFERENCES patients(patient_id),
    appointment_id      INT NOT NULL REFERENCES appointments(appointment_id),
    consultation_fee    DECIMAL(10,2) NOT NULL,
    medication_cost     DECIMAL(10,2) NOT NULL DEFAULT 0,
    lab_cost            DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_amount        DECIMAL(10,2) NOT NULL,
    insurance_covered   DECIMAL(10,2) NOT NULL DEFAULT 0,
    patient_due         DECIMAL(10,2) NOT NULL,
    payment_status      ENUM('PENDING','PAID','PARTIAL','WAIVED') NOT NULL DEFAULT 'PENDING',
    billed_at           DATETIME NOT NULL DEFAULT NOW(),
    paid_at             DATETIME,
    INDEX idx_patient_bills (patient_id, billed_at DESC)
);
