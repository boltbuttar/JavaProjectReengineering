# HealthBridge Hospital Management System — SRE Assignment

## Project Overview
Legacy Java Hospital Management System used for Software Re-Engineering analysis.  
Covers code smells, dependency coupling, technical debt, data smell detection, schema normalisation, and data migration.

---

## Project Structure
```
Project_SRE/
├── pom.xml                          Maven build config
├── sonar-project.properties         SonarQube scanner config
├── migration_etl.py                 Part G — ETL migration script
├── legacy_appointments.csv          Part G — Sample legacy CSV (10 rows)
├── SRE_Report.md                    Full assignment report (Parts A-G)
├── sql/
│   ├── legacy_schema.sql            Part E — Original legacy schema + sample data
│   ├── normalised_schema.sql        Part F1 — 3NF normalised schema
│   └── refactoring_r1_r5.sql       Part F2 — Five refactoring scripts + G4 validation
└── src/
    ├── main/java/com/healthbridge/
    │   ├── Main.java
    │   ├── model/
    │   │   ├── Patient.java          Primitive Obsession, Data Class smells
    │   │   ├── Appointment.java      Data Clump, Primitive Obsession smells
    │   │   ├── Doctor.java           Inconsistent Naming, Data Class smells
    │   │   ├── Department.java       Hidden Relationship smell
    │   │   ├── Billing.java          Derived Data, Duplicate Data smells
    │   │   └── AppointmentStatus.java  REFACTORED enum (B4)
    │   ├── service/
    │   │   ├── PatientManager.java   GOD CLASS — Long Method, Feature Envy, Duplicate Code
    │   │   ├── AppointmentService.java  Feature Envy, Inappropriate Intimacy, Message Chain
    │   │   ├── BillingProcessor.java   Long Parameter List, Duplicate Code, Speculative Generality
    │   │   ├── ReportGenerator.java    Duplicate Code, Lazy Class, Dead Code
    │   │   └── NotificationService.java  Lazy Class, Dead Code
    │   └── util/
    │       └── HospitalUtils.java    Switch Statements, Long Method (Part D target)
    └── test/java/com/healthbridge/
        └── HealthBridgeTest.java     JUnit 5 test suite
```

---

## Setup Commands

### 1. Compile the Java Project
```powershell
# Windows (javac must be in PATH or use full path)
$javac = "C:\Program Files\Eclipse Adoptium\jdk-8.0.482.8-hotspot\bin\javac.exe"
$java  = "C:\Program Files\Eclipse Adoptium\jdk-8.0.482.8-hotspot\bin\java.exe"

New-Item -ItemType Directory -Force target\classes
$files = Get-ChildItem -Recurse -Filter "*.java" src\main\java | Select -ExpandProperty FullName
& $javac -d target\classes -sourcepath src\main\java $files
```

### 2. Run the Application
```powershell
& $java -cp target\classes com.healthbridge.Main
```

### 3. Start SonarQube (Docker required)
```bash
docker run -d --name sonarqube -p 9000:9000 sonarqube:community
# Wait ~60 seconds, then open http://localhost:9000
# Default login: admin / admin
```

### 4. Run SonarQube Scanner
```bash
# After SonarQube is up and project compiled:
sonar-scanner
# Or with Maven: mvn sonar:sonar -Dsonar.host.url=http://localhost:9000
```

### 5. Load Legacy Hospital Schema into MySQL
```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS healthbridge;"
mysql -u root -p healthbridge < sql/legacy_schema.sql
```

### 6. Apply Normalised Schema (Part F1)
```bash
mysql -u root -p healthbridge < sql/normalised_schema.sql
```

### 7. Apply Refactoring Scripts R1–R5 (Part F2)
```bash
mysql -u root -p healthbridge < sql/refactoring_r1_r5.sql
```

### 8. Run the ETL Migration Script (Part G)
```bash
pip install mysql-connector-python
python migration_etl.py
```

---

## Key Smells Reference

| Category | Smell | File | Lines |
|----------|-------|------|-------|
| Cat 1 — Bloater | Long Method | PatientManager.java | 175–240 |
| Cat 2 — OO Abuser | Switch Statements | HospitalUtils.java | 33–43, 82–95 |
| Cat 3 — Change Preventer | Shotgun Surgery | HospitalUtils + PatientManager + AppointmentService | Multiple |
| Cat 4 — Dispensable | Duplicate Code | PatientManager + BillingProcessor + ReportGenerator | Multiple |
| Cat 5 — Coupler | Feature Envy | AppointmentService.java | 35–55 |

## Part D Target Method
`HospitalUtils.computeAppointmentPriority()` — used for Python Tutor trace, CFG, and AST analysis.  
Trace inputs: `fee=1500, dob='12/05/1985', visits=8, status='P'` → returns priority score **10**.
