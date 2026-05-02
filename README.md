# 🏥 Hospital Management System

**Java · SQL · System Design · Swing**

A desktop application that digitalises hospital patient management — providing real-time appointment scheduling with conflict detection, full medical records, and billing. Built to eliminate the administrative bottlenecks caused by manual, paper-based workflows.

---

## Use Cases

| Scenario | How the system helps |
|---|---|
| **Receptionist books an appointment** | System checks the doctor's full calendar in real time — if the slot is taken, a clear conflict message appears immediately with no double-booking possible |
| **Two receptionists book the same slot simultaneously** | Database-level `SELECT FOR UPDATE` row locking prevents race conditions — only one booking succeeds, the other receives a conflict error |
| **Doctor reviews a patient's history** | Full chronological medical record — diagnosis, treatment plan, prescriptions, and follow-up dates — retrieved in one query sorted by visit date |
| **Patient is discharged** | Soft-delete preserves all medical records and billing history for legal and audit purposes while removing them from active patient lists |
| **Billing after a consultation** | System auto-calculates the patient-due amount after applying insurance coverage, and tracks payment status through to completion |
| **Admin generates a daily schedule** | All confirmed appointments for a doctor or department retrieved and displayed by time slot, filtering out cancellations |

---

## System Architecture

```
┌─────────────────────────────────────────────────────────┐
│                   Swing GUI Layer                       │
│   MainWindow → PatientPanel, SchedulerPanel,           │
│               MedicalRecordsPanel, BillingPanel         │
└──────────────────┬──────────────────────────────────────┘
                   │ calls
┌──────────────────▼──────────────────────────────────────┐
│                  Service Layer                          │
│   PatientService         SchedulerService               │
│   • registerPatient()    • bookAppointment()            │
│   • searchPatients()       ↳ checkDoctorConflict()      │
│   • addMedicalRecord()     ↳ checkPatientConflict()     │
│   • getPatientHistory()  • getDoctorSchedule()          │
└──────────────────┬──────────────────────────────────────┘
                   │ PreparedStatements
┌──────────────────▼──────────────────────────────────────┐
│               Database Layer (MySQL / PostgreSQL)        │
│   patients · doctors · appointments                     │
│   medical_records · billing                             │
└─────────────────────────────────────────────────────────┘
```

---

## Demo — Sample Output

**Booking an appointment — success:**
```
[Scheduler] Checking doctor availability for Dr. Patel on 2024-03-20 14:00–14:30...
[Scheduler] Checking patient schedule for Patient ID 1042...
[Scheduler] No conflicts found.
[Scheduler] Appointment booked — ID: 3847
[UI] ✓ Appointment confirmed: Patient Ahmed Hassan with Dr. Patel at 14:00
```

**Booking an appointment — conflict detected:**
```
[Scheduler] Checking doctor availability for Dr. Patel on 2024-03-20 14:00–14:30...
[Scheduler] ✗ CONFLICT: Dr. Patel already has appointment from 14:00 to 14:30
[UI] ⚠ Scheduling conflict: Doctor already has an appointment at this time.
         Please choose a different slot.
```

**Patient medical history query:**
```
Patient: Ahmed Hassan (ID: 1042)
──────────────────────────────────────────────────────
2024-03-05 | Dr. Patel (Cardiology)
  Diagnosis      : Hypertension, Stage 1
  Treatment Plan : Lifestyle modification, medication review
  Prescription   : Amlodipine 5mg daily
  Follow-up      : 2024-04-05

2024-01-18 | Dr. Lim (General Practice)
  Diagnosis      : Upper respiratory infection
  Treatment Plan : Rest, fluids, symptomatic relief
  Prescription   : Paracetamol 500mg as needed
  Follow-up      : None required
```

**Billing summary:**
```
Bill ID : 5291
Patient : Ahmed Hassan
Date    : 2024-03-05

  Consultation fee  :  £120.00
  Medication cost   :   £18.50
  Lab tests         :   £45.00
  ─────────────────────────────
  Total             :  £183.50
  Insurance covered :  £120.00
  Patient due       :   £63.50

  Status: PENDING
```

---

## Scheduling — Conflict Detection

The core scheduling logic uses **database-level row locking** to prevent double-booking even when multiple receptionists are logged in simultaneously:

```java
// Checks doctor's calendar within the requested time window
// SELECT ... FOR UPDATE acquires a row lock — concurrent transactions
// must wait, eliminating race conditions entirely
SELECT appointment_id, scheduled_start, scheduled_end
FROM appointments
WHERE doctor_id = ?
  AND status NOT IN ('CANCELLED', 'COMPLETED')
  AND scheduled_start < ?    -- proposed end time
  AND scheduled_end   > ?    -- proposed start time
FOR UPDATE;
```

If any row is returned → `ConflictException` is thrown → transaction rolls back → UI shows an immediate, descriptive error message.

---

## How to Run

### Prerequisites

- Java 17+
- MySQL 8+ or PostgreSQL 14+

### 1. Create the database and run the schema

```bash
# MySQL
mysql -u root -p -e "CREATE DATABASE hospital_db;"
mysql -u root -p hospital_db < src/db/schema.sql

# PostgreSQL
psql -U postgres -c "CREATE DATABASE hospital_db;"
psql -U postgres -d hospital_db -f src/db/schema.sql
```

### 2. Configure the database connection

Edit `config/db.properties`:
```properties
db.url=jdbc:mysql://localhost:3306/hospital_db
db.user=your_username
db.password=your_password
db.pool.size=10
```

### 3. Compile and run

```bash
# Compile
javac -d out src/com/hms/**/*.java

# Run
java -cp out com.hms.HospitalManagementSystem
```

### 4. Using an IDE (IntelliJ / Eclipse)

Import as a standard Java project, add your JDBC driver (MySQL Connector or PostgreSQL JDBC) to the classpath, update `db.properties`, and run `HospitalManagementSystem.java`.

---

## Database Schema

```
patients
  ├── patient_id (PK)
  ├── first_name, last_name, date_of_birth
  ├── phone, email, address
  ├── blood_type, insurance_number
  └── is_active  (soft delete)

doctors
  ├── doctor_id (PK)
  ├── full_name, specialisation, department
  └── license_number (UNIQUE)

appointments
  ├── appointment_id (PK)
  ├── patient_id (FK) · doctor_id (FK)
  ├── scheduled_start · scheduled_end
  ├── status: SCHEDULED | COMPLETED | CANCELLED | NO_SHOW
  └── INDEX (doctor_id, scheduled_start, scheduled_end)

medical_records
  ├── record_id (PK)
  ├── patient_id (FK) · doctor_id (FK) · appointment_id (FK)
  ├── diagnosis · treatment_plan · prescription
  └── follow_up_date

billing
  ├── bill_id (PK)
  ├── patient_id (FK) · appointment_id (FK)
  ├── consultation_fee · medication_cost · lab_cost
  ├── total_amount · insurance_covered · patient_due
  └── payment_status: PENDING | PAID | PARTIAL | WAIVED
```

---

## Project Structure

```
hospital-management-system/
├── src/
│   ├── com/hms/
│   │   ├── HospitalManagementSystem.java   # Entry point
│   │   ├── model/
│   │   │   ├── Patient.java
│   │   │   ├── Appointment.java
│   │   │   └── MedicalRecord.java
│   │   ├── service/
│   │   │   ├── PatientService.java         # Patient + medical record CRUD
│   │   │   └── SchedulerService.java       # Scheduling + conflict detection
│   │   ├── db/
│   │   │   └── DatabaseManager.java        # Connection pool
│   │   └── ui/
│   │       └── MainWindow.java             # Swing GUI
│   └── db/
│       └── schema.sql                      # Full DDL
├── config/
│   └── db.properties
└── README.md
```

---

## Contributors

Individual project — designed and built the full system including schema, service layer, conflict detection logic, and GUI.
