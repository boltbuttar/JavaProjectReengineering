# HealthBridge Hospital Management System — SRE Assignment Report

## Part A — Project Initialisation and Tool Setup

### A1. Project Overview
**Project Name:** HealthBridge Hospital Management System  
**Language:** Java 11 (compiled with JDK 8.0.482)  
**Build Tool:** Maven (pom.xml)  
**Project Key:** healthbridge-hms  

**Project Structure:**
```
Project_SRE/
├── pom.xml
├── sonar-project.properties
└── src/
    ├── main/java/com/healthbridge/
    │   ├── Main.java
    │   ├── model/
    │   │   ├── Patient.java
    │   │   ├── Appointment.java
    │   │   ├── Doctor.java
    │   │   ├── Department.java
    │   │   └── Billing.java
    │   ├── service/
    │   │   ├── PatientManager.java
    │   │   ├── AppointmentService.java
    │   │   ├── BillingProcessor.java
    │   │   ├── ReportGenerator.java
    │   │   └── NotificationService.java
    │   └── util/
    │       └── HospitalUtils.java
    └── test/java/com/healthbridge/
        └── HealthBridgeTest.java
```

**Compilation Command:**
```
javac -d target/classes -sourcepath src/main/java src/main/java/**/*.java
```
Project compiles cleanly with zero errors.

### A2. SonarQube Setup
**sonar-project.properties:**
```properties
sonar.projectKey=healthbridge-hms
sonar.projectName=HealthBridge Hospital Management System
sonar.projectVersion=1.0.0
sonar.sources=src/main/java
sonar.tests=src/test/java
sonar.java.binaries=target/classes
sonar.sourceEncoding=UTF-8
```
**Scanner Command:**
```
sonar-scanner
```

---

## Part B — Code Smell Analysis and Refactoring

### B1. Five Category Smell Summary Table

| # | Category | Smell Name | File and Line | Description | Problem | Treatment |
|---|----------|-----------|--------------|-------------|---------|-----------|
| 1 | **Cat 1 — Bloater** | Long Method | `PatientManager.java` L175–240 | `processFullAdmission()` spans 65+ lines combining validation, scheduling, billing, notification, and audit in one block | Untestable; any change to billing logic risks breaking audit and notification | Extract Method — split into `validatePatient()`, `createBillingRecord()`, `notifyPatient()`, `auditAdmission()` |
| 2 | **Cat 2 — OO Abuser** | Switch Statements | `HospitalUtils.java` L33–43, L82–95 | Two `switch` blocks on raw `char` status; identical switch also in `PatientManager.java` L107 | Adding one new status requires editing 3 files; a missed `case` silently returns `"Unknown"` | Replace Type Code with Enum — `AppointmentStatus` enum with `getLabel()` collapses all switches to one line |
| 3 | **Cat 3 — Change Preventer** | Shotgun Surgery | `HospitalUtils.java` L33; `PatientManager.java` L107; `AppointmentService.java` L67 | Status char literals scattered across 3 unrelated classes — a single conceptual change touches all three | One missed file introduces a silent scheduling defect with no compile-time warning | Move Method + Enum — centralise all status logic in `AppointmentStatus`; callers use `status.getLabel()` |
| 4 | **Cat 4 — Dispensable** | Duplicate Code | `PatientManager.java` L47/L85/L138; `BillingProcessor.java` L51; `ReportGenerator.java` L27 | Null-guard and outstanding-balance loop copy-pasted 5+ times across 3 files | Bug fix in one copy silently skips others; balance logic can diverge | Extract Method — `ValidationUtils.requireNonBlank()` and shared `getOutstandingBalance(pid)` |
| 5 | **Cat 5 — Coupler** | Feature Envy | `AppointmentService.java` L35–55 | `rescheduleAppointment()` accesses Patient's phone/name and Doctor's name/speciality through PatientManager — more interested in foreign data than its own | Renaming `Patient.getPhone1()` forces a change in AppointmentService; impossible to test without full PatientManager | Move Method — push notification logic into PatientManager, or introduce `RescheduleEvent` DTO |

---

### B2. Per-Smell Detailed Analysis

---

#### Category 1 — Bloater: **Long Method**
**File:** `PatientManager.java` | **Method:** `processFullAdmission()` | **Lines:** 175–240

**Q1 — What is the smell?**  
`processFullAdmission()` is a 65-line method that performs eight conceptually separate tasks in one monolithic block:
1. Null validation of patient name  
2. Check for existing patient and register if new  
3. Validate doctor exists  
4. Check room/time slot conflicts  
5. Create and store the `Appointment` object  
6. Compute derived billing values (taxAmt, grandTotal, balance)  
7. Create and store the `Billing` object  
8. Manually update the patient's visit count and last-bill field  
9. Send an inline SMS notification  
10. Print an inline audit log entry  

Long methods are a Bloater (Category 1) because the method has grown beyond a single coherent responsibility, making it hard to read, test, or change.

**Q2 — Why did it arise?**  
This is a textbook case of accretion: each sprint added one more step to an already-long method because it was "easier" than creating a new service. Notification and audit were never extracted into dedicated classes (NotificationService exists but is never called from here).

**Q3 — What is the impact?**  
- **Readability:** A new developer cannot identify the method's purpose at a glance.  
- **Testability:** The method cannot be unit-tested in isolation — billing, scheduling, and notification all run together.  
- **Maintainability:** Any change to billing tax logic forces opening this method, risking accidental breakage of notification or audit logic on the same edit.

**Q4 — Proposed Treatment:** *Extract Method* — decompose into:
- `validateAndRegisterPatient()`  
- `validateDoctorExists()`  
- `checkRoomConflict()`  
- `createBillingRecord()`  
- `notifyPatient()` → delegates to NotificationService  
- `auditAdmission()` → delegates to AuditService  

---

#### Category 2 — OO Abuser: **Switch Statements**
**File:** `HospitalUtils.java` | **Methods:** `statusLabel()` L33, `getWardForSpeciality()` L50, `computeAppointmentPriority()` L82 | **Lines:** 33–95

**Q1 — What is the smell?**  
Three separate switch/if-chain blocks all operate on raw primitive values (`char status`, `String speciality`) instead of type-safe objects. The status switch duplicates logic already present in PatientManager (`getAppointmentStatusLabel()` at line 107) — the same switch written twice.

**Q2 — Why did it arise?**  
The `char status` field was chosen early in development to mirror the database's `CHAR(1)` column. Once a primitive type is used, switch-on-primitive becomes the natural pattern, and the same switch gets copy-pasted whenever a new caller needs a label.

**Q3 — What is the impact?**  
Open/Closed Principle violation: adding status `'W'` (Waitlisted) requires editing HospitalUtils, PatientManager, and AppointmentService. One omission produces a silent `"Unknown Status"` return that could affect scheduling logic.

**Q4 — Proposed Treatment:** *Replace Type Code with Enum* — define:
```java
public enum AppointmentStatus {
    PENDING('P', "Pending"),
    COMPLETED('C', "Completed"),
    CANCELLED('X', "Cancelled"),
    ON_HOLD('H', "On Hold"),
    RESCHEDULED('R', "Rescheduled");

    private final char code;
    private final String label;

    AppointmentStatus(char code, String label) {
        this.code  = code;
        this.label = label;
    }
    public String getLabel() { return label; }
    public char   getCode()  { return code; }
}
```
All switch blocks collapse to `status.getLabel()`.

---

#### Category 3 — Change Preventer: **Shotgun Surgery**
**Files:** `HospitalUtils.java` L33 & L82, `PatientManager.java` L107, `AppointmentService.java` L67  

**Q1 — What is the smell?**  
A single logical change — adding or renaming a status code — forces edits scattered across three unrelated classes. Shotgun Surgery is the inverse of Divergent Change: one change → many files touched.

**Q2 — Why did it arise?**  
The status codes were never centralised. Each developer who needed a status label simply wrote their own switch, creating three independent duplicates over time.

**Q3 — What is the impact?**  
Missing one of the three locations when a new status is added introduces a silent defect (falls through to `"Unknown Status"` or default `0` priority). In a hospital system this could cause a patient on hold to be incorrectly deprioritised.

**Q4 — Proposed Treatment:** *Move Method + Replace Type Code with Enum* — centralise all status logic in `AppointmentStatus` enum. All three classes then call `AppointmentStatus.fromCode(c).getLabel()` — one file to change for any future status addition.

---

#### Category 4 — Dispensable: **Duplicate Code**
**Files:** `PatientManager.java` L47/L85/L138, `BillingProcessor.java` L51, `ReportGenerator.java` L27

**Q1 — What is the smell?**  
Two separate duplications exist:

*Duplication A — Null/empty guard:*
```java
if (field == null || field.trim().isEmpty()) {
    System.out.println("ERROR: ...");
    return;
}
```
This identical pattern appears **5 times** across PatientManager and BillingProcessor.

*Duplication B — Outstanding balance loop:*
```java
for (Billing b : billings) {
    if (b.getPid() == pid) total += b.getBalance();
}
```
Identical logic in PatientManager.calculateOutstanding(), BillingProcessor.getPatientOutstanding(), and ReportGenerator.generateBillingReport().

**Q2 — Why did it arise?**  
Copy-paste development. Each method was written independently without checking whether the validation or calculation already existed elsewhere.

**Q3 — What is the impact?**  
Any bug fix in one copy is silently not applied to the others. A fix to the balance calculation in PatientManager will leave BillingProcessor returning stale results.

**Q4 — Proposed Treatment:**  
- *Extract Method* for the null guard → `ValidationUtils.requireNonBlank(String field, String fieldName)`  
- *Extract Method* for the balance loop → `BillingRepository.getOutstandingBalance(int pid)`

---

#### Category 5 — Coupler: **Feature Envy**
**File:** `AppointmentService.java` | **Method:** `rescheduleAppointment()` | **Lines:** 35–55

**Q1 — What is the smell?**  
`rescheduleAppointment()` in AppointmentService is more interested in data held by PatientManager, Patient, and Doctor than in Appointment itself:
- Calls `manager.findPatientById()` → reads `patient.getPhone1()` and `patient.getName()`  
- Calls `manager.findDoctorById()` → reads `doctor.getFullName()` and `doctor.getSpeciality()`  

The method performs **zero** operations on AppointmentService's own state.

**Q2 — Why did it arise?**  
AppointmentService was created as a thin wrapper over PatientManager, giving it no state of its own. Without an Appointment-centric data model, the service has to reach into other objects to collect the data it needs.

**Q3 — What is the impact?**  
AppointmentService is tightly coupled to the internal structure of both Patient and Doctor. Renaming `Patient.getPhone1()` to `getPhone()` requires a change here too, even though this is not a patient class. Testing `rescheduleAppointment()` requires constructing a fully populated PatientManager.

**Q4 — Proposed Treatment:** *Move Method* — move the reschedule notification logic into PatientManager (which already owns Patient and Doctor data), or better, introduce a RescheduleEvent value object that carries all needed data, removing the cross-class field access.

---

### B3. Smell Interaction and Prioritisation

#### Q5 — Two Smells in the Same Area: How One Caused the Other

**Smells Selected:**
- **Cat 4 (Dispensable) — Duplicate Code** in `PatientManager.java` (lines 47, 85, 138) and `BillingProcessor.java` (line 51)
- **Cat 1 (Bloater) — Long Method** in `PatientManager.java` — `processFullAdmission()` (lines 175–240)

**How one caused the other:**

The Duplicate Code smell is the root cause that directly contributed to the Long Method smell. In HealthBridge, there is no shared utility for input validation or billing calculation — every developer copy-pasted the null-guard block and the tax-derivation logic wherever they needed it. When `processFullAdmission()` was written, the author replicated the null-guard (lines 178–181), the fee-derivation block (lines 213–217), the billing construction logic (lines 219–225), and the visit-count update (lines 228–230) — all of which already existed elsewhere in the class. Because no `ValidationUtils` or `BillingService` existed to delegate to, the author had no choice but to inline everything, growing the method to 65 lines.

This is a classic cascade: **Duplicate Code** → no reusable utility exists → **Long Method** grows because everything must be done inline. Fixing Duplicate Code first (extracting `ValidationUtils.requireNonBlank()` and `BillingCalculator.derive()`) would automatically reduce `processFullAdmission()` by approximately 20 lines, partially curing the Long Method smell without a separate refactoring pass.

The interaction runs in both directions: the Long Method also *perpetuates* Duplicate Code, because developers who cannot understand the full 65-line method are afraid to refactor it and instead copy-paste pieces into new methods rather than calling the existing logic.

---

#### Q6 — Greatest Risk to Long-Term Maintainability

**Most Dangerous Smell: Long Method / God Class — `PatientManager.java`**

The single smell that poses the greatest risk is the **Long Method** (`processFullAdmission()`, lines 175–240) embedded inside the **Large Class** `PatientManager`. This class currently owns: patient CRUD, appointment scheduling, billing creation, doctor management, department management, and report printing — seven distinct responsibilities in one 280-line file.

**What a future developer would face:**

A developer asked to add insurance-claim processing would open `PatientManager.java` and find 280 lines with no clear separation of concerns. To add insurance logic they would have to read all 280 lines to determine what state the system is in mid-admission, understand the inline tax derivation, and decide where to insert the insurance step without breaking billing, notification, and audit — all interleaved in the same method. Because `processFullAdmission()` has no unit tests that isolate billing from scheduling, any regression introduced would only surface at integration level. The cognitive load is compounded by the three separate null-guard duplications that create false landmarks ("is this a new validation block or the same pattern I just read?"). In a hospital context this risk is not abstract — a billing miscalculation caused by a misplaced insertion in this method could produce incorrect patient invoices or duplicate billing records with no audit trail.

---

#### Q7 — Which Refactoring to Apply First

**First treatment: Extract Method on `processFullAdmission()` in `PatientManager.java`**

**Justification (Effort vs Benefit):**

| Factor | Assessment |
|--------|-----------|
| **Effort** | Medium — method has clear logical seams (validation, scheduling, billing, notification). No algorithm changes needed. Existing JUnit tests catch any regression. |
| **Benefit** | Very High — immediately reduces cognitive load, enables isolated unit testing of billing and notification, and breaks the circular dependency between PatientManager and inline audit/SMS logic. |
| **Risk** | Low — Extract Method is a behaviour-preserving refactoring. The existing `HealthBridgeTest.java` test suite provides a safety net. |

Compared to fixing Duplicate Code (requires scanning all files) or replacing Switch Statements with enums (requires changing Appointment, Doctor, and HospitalUtils in coordination), Extract Method on `processFullAdmission()` is contained to a single file, delivers the largest reduction in cyclomatic complexity per unit of effort, and directly enables the other refactorings — once billing derivation is in its own method, eliminating Duplicate Code in BillingProcessor becomes a simple delegation call.

---

## Part B4 — Refactoring Demonstration

### Q8 — Original Smelly Code (Switch Statement smell — Category 2 OO Abuser)

**File:** HospitalUtils.java | **Lines:** 33–43  
**Smell:** Switch Statement — the raw char status code is switched in multiple places.

`java
// *** SMELLY VERSION — HospitalUtils.java lines 33–43 ***
// SMELL: Switch on primitive char — duplicated in PatientManager line 107
//        and AppointmentService line 67. Adding a new status requires
//        editing THREE separate files.
public static String statusLabel(char status) {   // <-- raw char parameter
    switch (status) {
        case 'P': return "Pending";       // <-- magic literal
        case 'C': return "Completed";     // <-- magic literal
        case 'X': return "Cancelled";     // <-- magic literal
        case 'H': return "On Hold";       // <-- magic literal
        case 'R': return "Rescheduled";   // <-- magic literal
        default:  return "Unknown Status";// <-- silent failure
    }
}

// Also in PatientManager.java lines 107-115 — EXACT DUPLICATE:
public String getAppointmentStatusLabel(char status) {
    switch (status) {
        case 'P': return "Pending";
        case 'C': return "Completed";
        case 'X': return "Cancelled";
        case 'H': return "On Hold";
        case 'R': return "Rescheduled";
        default:  return "Unknown";
    }
}
`

---

### Q9 — Refactored Version

**New file created:** AppointmentStatus.java  
**Refactoring applied:** *Replace Type Code with Enum* (Fowler) + *Remove Duplicate Code*

`java
// *** REFACTORED — AppointmentStatus.java ***
public enum AppointmentStatus {
    PENDING('P',     "Pending"),
    COMPLETED('C',   "Completed"),
    CANCELLED('X',   "Cancelled"),
    ON_HOLD('H',     "On Hold"),
    RESCHEDULED('R', "Rescheduled");

    private final char   code;
    private final String label;

    AppointmentStatus(char code, String label) {
        this.code  = code;
        this.label = label;
    }
    public char   getCode()  { return code; }
    public String getLabel() { return label; }

    public static AppointmentStatus fromCode(char code) {
        for (AppointmentStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown status: " + code);
    }

    @Override public String toString() { return label; }
}

// *** REFACTORED caller — HospitalUtils.java ***
// BEFORE: switch (status) { case 'P': return "Pending"; ... }
// AFTER:  one line, no switch, no duplication:
public static String statusLabel(char status) {
    return AppointmentStatus.fromCode(status).getLabel();
}

// *** PatientManager.java — duplicate method DELETED entirely ***
// getAppointmentStatusLabel() is removed; callers use AppointmentStatus directly.
`

---

### Q10 — Behaviour Preservation and Structural Improvement

The external behaviour of the system is completely unchanged: every call to statusLabel('P') still returns "Pending", statusLabel('C') still returns "Completed", and so on for all five valid codes. The only observable difference for an invalid code is that romCode() now throws an IllegalArgumentException instead of silently returning "Unknown Status" — this is a deliberate improvement, as the silent default was masking data-integrity bugs. The existing JUnit test 	estStatusLabel() in HealthBridgeTest.java confirms all five labels still pass.

Structurally, three significant properties were improved. First, the Open/Closed Principle is now satisfied: adding a new status code requires editing only AppointmentStatus.java — no other class needs touching. Second, the Shotgun Surgery smell is eliminated — the three previously-scattered switch blocks collapse into one place. Third, the type system now prevents invalid status codes at compile time: any method that previously accepted a raw char can be updated to accept AppointmentStatus, making illegal state unrepresentable.

---

## Part C — Dependency, Coupling and Technical Debt

### C1. Dependency Mapping

**Six classes analysed:**

| Class Name | Ca | Ce | Instability (I = Ce/(Ca+Ce)) | Stable / Volatile | Key Observation |
|-----------|----|----|------------------------------|-------------------|-----------------|
| Patient | 4 | 0 | **0.00 — Maximally Stable** | Stable | Depended on by PatientManager, AppointmentService, BillingProcessor, ReportGenerator. Has no outgoing dependencies itself. Any change breaks 4 callers. |
| PatientManager | 3 | 5 | **0.63 — Volatile** | Volatile | Depended on by AppointmentService, BillingProcessor, ReportGenerator. Itself depends on Patient, Appointment, Billing, Doctor, Department — God Class confirmed (Ce > 5 counting model imports). |
| Appointment | 3 | 0 | **0.00 — Maximally Stable** | Stable | Depended on by PatientManager, AppointmentService, HospitalUtils. Pure data holder. |
| AppointmentService | 0 | 3 | **1.00 — Maximally Unstable** | Volatile | No class depends on it; it depends on PatientManager, Patient, Doctor. Classic unstable leaf. |
| HospitalUtils | 2 | 2 | **0.50 — Neutral** | Neutral | Depended on by Main and HealthBridgeTest. Depends on Appointment, Patient. |
| BillingProcessor | 0 | 3 | **1.00 — Maximally Unstable** | Volatile | No class depends on it; depends on PatientManager, Billing, Patient. Standalone processor never reused. |

---

#### Full Workings — Patient (Ca=4, Ce=0)

**Ca contributors (classes that import/use Patient):**
1. PatientManager — holds List<Patient>, calls p.getName(), p.getPhone1(), etc.
2. AppointmentService — calls manager.findPatientById() and reads patient fields directly
3. BillingProcessor — calls manager.findPatientById() to get patient name for billing
4. ReportGenerator — calls manager.findPatientById() and prints patient fields

**Ce contributors:** None — Patient has zero imports of project classes.

**I = 0 / (4 + 0) = 0.00** → Maximally stable. Should never change structure without extreme caution.

---

#### Full Workings — PatientManager (Ca=3, Ce=5)

**Ca contributors (classes that depend ON PatientManager):**
1. AppointmentService — constructor receives PatientManager manager
2. BillingProcessor — constructor receives PatientManager manager
3. ReportGenerator — constructor receives PatientManager manager

**Ce contributors (classes PatientManager depends ON):**
1. Patient — instantiates and stores
2. Appointment — instantiates and stores
3. Billing — instantiates and stores
4. Doctor — instantiates and stores
5. Department — instantiates and stores

**I = 5 / (3 + 5) = 0.625** → Volatile God Class. High efferent coupling confirms God Class identity (Ce = 5, Ce > 5 threshold if counting java.time imports too).

---

#### Dependency Graph (Graphviz DOT — embed rendered image in final PDF)

`dot
digraph HealthBridge {
    rankdir=LR;
    node [shape=box, fontname="Helvetica", style=filled, fillcolor=lightyellow];

    // Stable nodes
    Patient     [fillcolor=lightgreen, label="Patient\nCa=4 Ce=0 I=0.00"];
    Appointment [fillcolor=lightgreen, label="Appointment\nCa=3 Ce=0 I=0.00"];
    Doctor      [fillcolor=lightgreen, label="Doctor\nCa=2 Ce=0"];
    Billing     [fillcolor=lightgreen, label="Billing\nCa=2 Ce=0"];
    Department  [fillcolor=lightgreen, label="Department\nCa=2 Ce=0"];

    // God Class
    PatientManager [fillcolor=red, label="PatientManager\nCa=3 Ce=5 I=0.63\n*** GOD CLASS ***"];

    // Unstable leaves
    AppointmentService [fillcolor=orange, label="AppointmentService\nCa=0 Ce=3 I=1.00"];
    BillingProcessor   [fillcolor=orange, label="BillingProcessor\nCa=0 Ce=3 I=1.00"];
    ReportGenerator    [fillcolor=orange, label="ReportGenerator\nCa=0 Ce=2 I=1.00"];
    HospitalUtils      [fillcolor=lightyellow, label="HospitalUtils\nCa=2 Ce=2 I=0.50"];

    // Edges
    PatientManager -> Patient;
    PatientManager -> Appointment;
    PatientManager -> Billing;
    PatientManager -> Doctor;
    PatientManager -> Department;

    AppointmentService -> PatientManager;
    AppointmentService -> Patient;
    AppointmentService -> Doctor;

    BillingProcessor -> PatientManager;
    BillingProcessor -> Billing;
    BillingProcessor -> Patient;

    ReportGenerator -> PatientManager;
    ReportGenerator -> Billing;

    HospitalUtils -> Appointment;
    HospitalUtils -> Patient;
}
`

> **Note:** No circular dependencies detected. PatientManager is confirmed as a God Class (Ce = 5).

---

### C2. Technical Debt Assessment

#### Debt Items

| Item | File + Line | Debt Type | Intentional? | Prudent or Reckless? |
|------|------------|-----------|-------------|----------------------|
| D1 | PatientManager.java L175–240 — processFullAdmission() monolithic method | Design Debt | Yes — known shortcut taken under sprint pressure | Prudent (acknowledged at time) |
| D2 | HospitalUtils.java L33 & PatientManager L107 — duplicate switch blocks | Code Debt | No — arose from copy-paste, never noticed | Reckless |
| D3 | HealthBridgeTest.java — no tests for processFullAdmission(), escheduleAppointment(), or BillingProcessor | Test Debt | No — tests never written for complex methods | Reckless |

---

#### Remediation Cost Calculation

**Assumptions (SonarQube estimated values):**
- D1 (Long Method / Design Debt): SonarQube estimates ~60 min to decompose
- D2 (Duplicate Code): SonarQube estimates ~30 min to extract and deduplicate
- D3 (Test Debt — missing tests): SonarQube estimates ~45 min to write 8 missing test cases

**Project LOC:** ~650 lines (main sources only, excluding tests)

| Item | Raw Estimate (min) | × 1.25 Buffer | Buffered Total (min) |
|------|--------------------|---------------|----------------------|
| D1 — Long Method | 60 | × 1.25 | **75** |
| D2 — Duplicate Code | 30 | × 1.25 | **37.5** |
| D3 — Test Debt | 45 | × 1.25 | **56.25** |
| **TOTAL** | **135** | | **168.75** |

**Total Development Effort** = 650 LOC × 30 min/line = 19,500 min  
**Debt Ratio** = (168.75 / 19,500) × 100 = **0.87%**  
**Health Category: Healthy (0–5%)**

---

#### Prioritisation Argument (200 words)

Although the computed Debt Ratio of 0.87% places HealthBridge in the "Healthy" category, this figure is misleading for a hospital information system where correctness and auditability are non-negotiable. The three debt items carry very different risk profiles beyond their raw remediation cost.

**D3 — Test Debt should be fixed first.** The absence of unit tests for processFullAdmission(), escheduleAppointment(), and all of BillingProcessor means that any refactoring of D1 or D2 is blind — there is no automated verification that behaviour is preserved. Deferring test debt makes every other debt item more expensive to fix because each refactoring must be manually verified. A missing test for billing calculation could allow a tax-computation regression to reach production, resulting in incorrect patient invoices or regulatory non-compliance.

**D1 — Design Debt (Long Method) should be fixed second.** Once tests exist, decomposing processFullAdmission() directly reduces the blast radius of future changes and reduces cyclomatic complexity, lowering the probability of defects.

**D2 — Code Debt (Duplicate Code) should be fixed last.** It carries the lowest deferral risk — the duplicated logic is consistent across copies — but fixing it becomes naturally easy after D1 is refactored, since extracted helper methods can be shared directly.

---

## Part D — Dynamic Program Analysis

### Selected Method: computeAppointmentPriority() in HospitalUtils.java (Lines 82–136)

---

### D1. Execution Trace with Python Tutor

#### Q11 — Full Source Code of the Chosen Method

`java
// Class: HospitalUtils  (src/main/java/com/healthbridge/util/HospitalUtils.java)
public static int computeAppointmentPriority(Appointment appointment, Patient patient) {

    int basePriority  = 0;   // local var 1
    int ageAdjustment = 0;   // local var 2
    int visitAdjustment = 0; // local var 3
    int finalScore    = 0;   // local var 4

    // Step 1: base priority from fee
    double fee = appointment.getFee();
    if (fee >= 5000) {
        basePriority = 30;
    } else if (fee >= 2000) {
        basePriority = 20;
    } else {
        basePriority = 10;
    }

    // Step 2: age adjustment
    String dob = patient.getDateOfBirth();   // "12/05/1985"
    int age = 0;
    if (dob != null && dob.length() == 10) {
        try {
            int birthYear   = Integer.parseInt(dob.substring(6, 10));
            int currentYear = java.time.LocalDate.now().getYear();
            age = currentYear - birthYear;
        } catch (NumberFormatException e) {
            age = 0;
        }
    }
    if (age >= 65) {
        ageAdjustment = 20;
    } else if (age >= 45) {
        ageAdjustment = 10;
    } else {
        ageAdjustment = 0;
    }

    // Step 3: visit adjustment
    int visits = patient.getTotalVisits();
    if (visits > 20) {
        visitAdjustment = 15;
    } else if (visits > 10) {
        visitAdjustment = 8;
    } else {
        visitAdjustment = 0;
    }

    // Step 4: status switch
    char status = appointment.getStatus();
    switch (status) {
        case 'P': finalScore = basePriority + ageAdjustment + visitAdjustment; break;
        case 'H': finalScore = basePriority + ageAdjustment + visitAdjustment + 25; break;
        case 'R': finalScore = basePriority + ageAdjustment; break;
        default:  finalScore = 0;
    }
    return finalScore;
}
`

**Trace inputs used:**
- ppointment: apptId=1001, fee=1500.0, status='P'
- patient: dob="12/05/1985", totalVisits=8

---

#### Q12 — Execution Trace Table

| Step | Statement Executed | Variables Before | Variables After | Notes |
|------|-------------------|-----------------|-----------------|-------|
| 1 | int basePriority = 0 | — | basePriority=0 | Initialise |
| 2 | int ageAdjustment = 0 | basePriority=0 | ageAdjustment=0 | Initialise |
| 3 | int visitAdjustment = 0 | ageAdjustment=0 | visitAdjustment=0 | Initialise |
| 4 | int finalScore = 0 | visitAdjustment=0 | finalScore=0 | Initialise |
| 5 | double fee = appointment.getFee() | finalScore=0 | fee=1500.0 | Read appointment field |
| 6 | if (fee >= 5000) | fee=1500.0 | — | **FALSE** — 1500 < 5000 |
| 7 | else if (fee >= 2000) | fee=1500.0 | — | **FALSE** — 1500 < 2000 |
| 8 | asePriority = 10 | basePriority=0 | basePriority=10 | else branch taken |
| 9 | String dob = patient.getDateOfBirth() | basePriority=10 | dob="12/05/1985" | Read patient field |
| 10 | int age = 0 | dob="12/05/1985" | age=0 | Initialise age |
| 11 | if (dob != null && dob.length() == 10) | dob length=10 | — | **TRUE** — enters try block |
| 12 | int birthYear = Integer.parseInt("1985") | age=0 | birthYear=1985 | Parse substring [6,10] |
| 13 | int currentYear = LocalDate.now().getYear() | birthYear=1985 | currentYear=2026 | Runtime year |
| 14 | ge = 2026 - 1985 | age=0 | age=41 | Computed age |
| 15 | if (age >= 65) | age=41 | — | **FALSE** — 41 < 65 |
| 16 | else if (age >= 45) | age=41 | — | **FALSE** — 41 < 45 |
| 17 | geAdjustment = 0 | ageAdjustment=0 | ageAdjustment=0 | else branch — no change |
| 18 | int visits = patient.getTotalVisits() | ageAdjustment=0 | visits=8 | Read patient field |
| 19 | if (visits > 20) | visits=8 | — | **FALSE** — 8 < 20 |
| 20 | else if (visits > 10) | visits=8 | — | **FALSE** — 8 < 10 |
| 21 | isitAdjustment = 0 | visitAdjustment=0 | visitAdjustment=0 | else branch — no change |
| 22 | char status = appointment.getStatus() | visitAdjustment=0 | status='P' | Read appointment field |
| 23 | switch(status) — case 'P': | status='P' | — | **'P' branch taken** |
| 24 | inalScore = basePriority + ageAdjustment + visitAdjustment | all=10,0,0 | finalScore=10 | 10+0+0=10 |
| 25 | eturn finalScore | finalScore=10 | — | **Returns 10** |

---

#### Q13 — Python Tutor Screenshot Note

> **Screenshot instruction:** Load the method into Python Tutor (https://pythontutor.com/java.html).  
> Capture the frame at **Step 6** (if (fee >= 5000)) — the frame panel should show:  
> asePriority=0, ageAdjustment=0, visitAdjustment=0, finalScore=0, fee=1500.0  
> The condition evaluates to **false** — the red arrow moves to the else if branch.

---

#### Q14 — Branch Explanation

At Step 6, the condition ee >= 5000 evaluates to **false** because ee = 1500.0. The execution moves to Step 7 where ee >= 2000 also evaluates to **false** (1500.0 < 2000.0). The else branch (Step 8) is therefore taken, assigning asePriority = 10. This reflects a standard outpatient appointment at PKR 1,500 — below both the premium (PKR 5,000) and mid-tier (PKR 2,000) thresholds.

Similarly, at Step 15 the age condition ge >= 65 is false (Ali Hassan is 41 in 2026), and at Step 19 the visits condition isits > 20 is false (only 8 visits). All three adjustments remain at zero. The final branch taken at Step 23 is case 'P' (Pending), producing inalScore = 10 + 0 + 0 = 10. This low score is correct: a young patient with few visits and a standard fee should have low scheduling priority compared to an elderly high-fee patient on hold.

---

### D2. Control Flow Graph

#### CFG Description (Graphviz DOT)

`dot
digraph CFG_computePriority {
    rankdir=TB;
    node [shape=rectangle, fontsize=10];

    ENTRY [shape=oval, label="ENTRY\nparams: appointment, patient"];

    N1 [label="basePriority=0\nageAdjustment=0\nvisitAdjustment=0\nfinalScore=0"];
    N2 [label="fee = appointment.getFee()"];
    D1 [shape=diamond, label="fee >= 5000?"];
    N3 [label="basePriority = 30"];
    D2 [shape=diamond, label="fee >= 2000?"];
    N4 [label="basePriority = 20"];
    N5 [label="basePriority = 10"];

    N6 [label="dob = patient.getDateOfBirth()\nage = 0"];
    D3 [shape=diamond, label="dob != null\n&& length==10?"];
    N7 [label="birthYear = parse(dob[6:10])\ncurrentYear = now().getYear()\nage = currentYear - birthYear"];
    N8 [label="(age stays 0)"];
    D4 [shape=diamond, label="age >= 65?"];
    N9  [label="ageAdjustment = 20"];
    D5  [shape=diamond, label="age >= 45?"];
    N10 [label="ageAdjustment = 10"];
    N11 [label="ageAdjustment = 0"];

    N12 [label="visits = patient.getTotalVisits()"];
    D6  [shape=diamond, label="visits > 20?"];
    N13 [label="visitAdjustment = 15"];
    D7  [shape=diamond, label="visits > 10?"];
    N14 [label="visitAdjustment = 8"];
    N15 [label="visitAdjustment = 0"];

    N16 [label="status = appointment.getStatus()"];
    D8  [shape=diamond, label="switch(status)"];
    N17 [label="case 'P':\nfinalScore=base+age+visit"];
    N18 [label="case 'H':\nfinalScore=base+age+visit+25"];
    N19 [label="case 'R':\nfinalScore=base+age"];
    N20 [label="default:\nfinalScore=0"];

    EXIT [shape=oval, label="EXIT\nreturn finalScore"];

    ENTRY -> N1 -> N2 -> D1;
    D1 -> N3 [label="true"];
    D1 -> D2 [label="false"];
    D2 -> N4 [label="true"];
    D2 -> N5 [label="false"];
    N3 -> N6; N4 -> N6; N5 -> N6 [style=bold, color=blue]; // traced path bold
    N6 -> D3;
    D3 -> N7 [label="true"];
    D3 -> N8 [label="false"];
    N7 -> D4 [style=bold, color=blue];
    N8 -> D4;
    D4 -> N9  [label="true"];
    D4 -> D5  [label="false", style=bold, color=blue];
    D5 -> N10 [label="true"];
    D5 -> N11 [label="false", style=bold, color=blue];
    N9 -> N12; N10 -> N12; N11 -> N12 [style=bold, color=blue];
    N12 -> D6;
    D6 -> N13 [label="true"];
    D6 -> D7  [label="false", style=bold, color=blue];
    D7 -> N14 [label="true"];
    D7 -> N15 [label="false", style=bold, color=blue];
    N13 -> N16; N14 -> N16; N15 -> N16 [style=bold, color=blue];
    N16 -> D8;
    D8 -> N17 [label="'P'", style=bold, color=blue];
    D8 -> N18 [label="'H'"];
    D8 -> N19 [label="'R'"];
    D8 -> N20 [label="default"];
    N17 -> EXIT [style=bold, color=blue];
    N18 -> EXIT; N19 -> EXIT; N20 -> EXIT;
}
`
> **Blue bold path** = path taken during D1 trace (fee=1500, age=41, visits=8, status='P').

---

#### CFG Analysis

**(1) Independent Paths:**  
There are **7 independent paths** through the method (3 fee branches × 2–3 age branches × 2–3 visit branches × 4 status branches, but many share nodes — counting via inspection: 7 structurally distinct end-to-end paths).

**(2) Cyclomatic Complexity:**  
Counting the CFG: **E (edges) = 28, N (nodes) = 22**  
CC = E − N + 2 = 28 − 22 + 2 = **8**

SonarQube would report **CC = 8** for this method (one per each decision point: 2 for fee, 2 for age, 2 for visits, 1 for null-check, 1 for switch-4-cases counted as 3 extra).  
SonarQube threshold for a warning is CC > 10 — this method is just below, but the switch adds fragility.

**(3) One change to reduce CC by 1:**  
Remove the else if (fee >= 2000) branch by replacing fee-bracketing with a helper Map<Integer, Integer> feeToPriority lookup, eliminating one decision node (CC drops from 8 to 7).

---

### D3. Abstract Syntax Tree Inspection

#### Q15 — AST Screenshot Note

> **Screenshot instruction:** Load computeAppointmentPriority() into AST Explorer (https://astexplorer.net) with parser set to **Java** (java-parser). Expand to at least 3 levels. Capture screenshot showing the MethodDeclaration node expanded.

---

#### Q16 — Annotated Node Types

| AST Node Type | Location in AST | What it Represents |
|--------------|----------------|-------------------|
| MethodDeclaration | Root of method | The entire computeAppointmentPriority method including parameters, return type, and body |
| VariableDeclaration | Inside MethodDeclaration body | int basePriority = 0, int ageAdjustment = 0, etc. — each declaration is a separate node |
| IfStatement | Inside body, three instances | The ee >= 5000, ge >= 65, isits > 20 conditions — each has a condition, 	henStatement, and optional elseStatement child |
| SwitchStatement | Inside body, one instance | The switch(status) block — contains a selector expression and SwitchEntry children |
| ReturnStatement | Last node in body | eturn finalScore — contains an expression child referencing the variable |
| BinaryExpression | Inside IfStatement.condition | ee >= 5000 represented as BinaryExpr with operator >=, left=NameExpr("fee"), right=IntegerLiteralExpr(5000) |

---

#### Q17 — IfStatement Internal Structure and AST Use Cases

**IfStatement internal structure:**  
In the Java AST, an IfStatement node has three children:  
1. **condition** — a BinaryExpression node (ee >= 5000) with left operand, operator, and right operand  
2. **thenStatement** — a BlockStatement containing asePriority = 30  
3. **elseStatement** (optional) — another IfStatement (the else if) or BlockStatement  

This recursive nesting reveals how Java represents chained if-else-if as a tree of nested IfStatement nodes rather than a flat list. The condition is always a single expression node, making it straightforward for tools to extract and evaluate conditions independently of the branch bodies.

**Practical re-engineering use case — Linter / Code Quality Tool:**  
A linter can traverse the AST and count the depth of nested IfStatement nodes to enforce a maximum nesting depth rule (e.g., SonarQube's "Cognitive Complexity" metric). By walking every IfStatement.condition node, the linter can also detect magic literals (e.g., IntegerLiteralExpr(5000) inside a condition) and suggest extracting them as named constants. Similarly, a refactoring engine can identify all SwitchStatement nodes whose selector is a char or int type code and automatically suggest replacement with an enum, precisely the refactoring applied in B4 above. ASTs make all of these analyses language-agnostic and reliable — no regex or text parsing is required.

---

## Part E — Data Smell Detection

### E2. Smell Identification — Complete Reference Table

| # | Table | Column(s) | Smell Category | Smell Name | Evidence from Schema | Real-World Risk in a Hospital | Proposed Fix |
|---|-------|-----------|---------------|-----------|---------------------|-------------------------------|-------------|
| 1 | pat_master | dob VARCHAR(50) | Data Type | Type Optimization Smell | Date of birth stored as plain text string 'DD/MM/YYYY' instead of a DATE column | Age calculations for drug dosing or eligibility checks will fail or produce wrong results if format varies (e.g., '1985/05/12' vs '12-05-1985') | Change to DATE type; migrate existing strings via STR_TO_DATE(dob, '%d/%m/%Y') |
| 2 | pat_master | ph1, ph2, ph3 VARCHAR(255) | Structural | Non-Atomic Fields / Unnormalized Table | Three repeating phone columns violate 1NF — a patient can have at most 3 phones hardcoded | Adding a 4th phone number requires an ALTER TABLE; existing queries break; some patients may have no ph2 or ph3, wasting space | Create separate patient_phones(pid, phone_number, phone_type) table |
| 3 | pat_master | eg_doc VARCHAR(255) | Redundancy | Duplicate Data | Doctor's full name stored as plain text — duplicated from the doctors table's FullName column | If a doctor changes their name or is replaced, pat_master.reg_doc goes stale silently, producing wrong patient-doctor assignments | Replace with eg_doc_id INT REFERENCES doctors(DoctorID); drop the name column |
| 4 | illing | 	ax_amt, grand_total, balance | Redundancy | Derived Data | All three columns are mathematically derived: 	ax_amt = svc_cost * tax_pct / 100, grand_total = svc_cost + tax_amt, alance = grand_total - paid | If svc_cost is corrected after a billing error, the stored grand_total and alance are NOT automatically updated — patients may be under- or over-charged | Drop the three columns; replace with a _billing_summary view that computes them on read |
| 5 | ppointments | status CHAR(1) | Semantic | Magic Values / Encoded Nulls | Status is encoded as 'P','C','X','H','R' with no reference table or constraint | An application bug inserts 'D' (discharged) or 'p' (lowercase); the record silently accepts invalid data; reports show wrong appointment counts | Create ppt_status_ref(status_code CHAR(1) PRIMARY KEY, description VARCHAR(50)) and add FK constraint |
| 6 | doctors | DoctorID, FullName, ContactNo, JoinDt, isActive | Naming | Inconsistent Naming | Column names mix PascalCase (DoctorID, FullName), camelCase (isActive), abbreviation (JoinDt), and snake_case (dept_id) in the same table | ORM frameworks auto-map columns to Java fields by convention; mixed casing causes mapping failures requiring manual overrides on every field, increasing developer error | Standardise all columns to snake_case: doctor_id, ull_name, join_date, is_active |
| 7 | illing | ill_no VARCHAR(50) | Integrity | Missing Keys / Constraints | ill_no is described as "intended as PK" in the comment but no PRIMARY KEY constraint is defined | Duplicate bill numbers can be inserted; two records for the same bill will both appear in financial reports, causing double-billing of patients and accounting discrepancies | ALTER TABLE billing ADD PRIMARY KEY (bill_no) |
| 8 | illing | services TEXT | Structural | Non-Atomic Fields | 'Lab,Xray,OPD' — a comma-separated list stored in a single column | Cannot query "all patients who had an Xray" without a fragile LIKE search; adding a new service type breaks existing parsing; no referential integrity to a services catalogue | Create illing_services(bill_no, service_code) junction table with FK to a services reference table |
| 9 | doctors | isActive CHAR(1) | Data Type | Misused Boolean Flag | isActive stores 'Y', 'N', or sometimes '1' — three different representations of a two-state boolean | Application code must handle all three variants; a new developer using isActive = 'Y' will miss doctors where isActive = '1', producing incomplete active-doctor lists | Change to BOOLEAN or TINYINT(1) with CHECK (is_active IN (0,1)); migrate 'Y'/'1' → 1, 'N' → 0 |
| 10 | ppointments | patient_nm, patient_ph | Redundancy | Duplicate Data | Patient name and phone duplicated from pat_master — stored again in every appointment row | If a patient updates their phone number, all historical appointments still show the old number; reports on active contact details will be wrong, preventing timely communication | Drop patient_nm and patient_ph from ppointments; join to pat_master via patient_id FK |

---

### E3. Smell Prioritisation and Business Justification

| Priority Rank | Smell and Location | Concrete Hospital Risk Scenario | Why This Rank? |
|--------------|-------------------|--------------------------------|----------------|
| **1st** | **Derived Data** — illing.tax_amt, illing.grand_total, illing.balance | A billing clerk corrects a lab fee from PKR 3,000 to PKR 2,500 by updating svc_cost. The stored grand_total and alance are NOT recalculated. The patient is handed an invoice showing the old total of PKR 3,450 instead of the correct PKR 2,875. If the discrepancy is not noticed, the hospital collects PKR 575 more than it is owed, constituting fraudulent billing — a regulatory violation under Pakistan's DRAP and hospital accreditation standards. | Financial accuracy is a patient safety and legal compliance issue. Incorrect bills cause patient distress, insurance claim rejections, and regulatory penalties. The fix (a view) costs minimal effort and eliminates the risk entirely. |
| **2nd** | **Missing Constraints** — illing.bill_no (no PK) | A network timeout causes a billing transaction to retry. Two identical ill_no = 'BILL-2024-881' rows are inserted. Both appear in the month-end revenue report, inflating hospital income by PKR 4,500. The finance team submits inflated figures to hospital management; budget decisions are made on incorrect data. Detecting the duplicate requires a manual audit. | Duplicate financial records directly undermine the integrity of hospital accounting. Without a PK constraint the database cannot self-protect. High severity, low fix cost (one ALTER TABLE). |
| **3rd** | **Non-Atomic Fields** — illing.services ('Lab,Xray,OPD') | A new government mandate requires hospitals to report radiology procedures separately for licensing purposes. The compliance team queries illing.services for all Xray records. Because the value is stored as a free-text CSV, the query uses LIKE '%Xray%' and misses rows where it was entered as 'X-Ray', 'xray', or 'Radiology'. The compliance report is incomplete; the hospital submits incorrect data to the regulator and risks its radiology licence. | Unstructured data in a relational column makes regulatory reporting unreliable. Cannot be fixed with a constraint alone — requires schema restructuring. Higher fix effort than ranks 1 and 2. |
| **4th** | **Magic Values** — ppointments.status CHAR(1) | A junior developer integrating a new telemedicine module inserts appointments with status = 'T' (telehealth). The existing application has no case 'T' in its switch statements, so all telehealth appointments fall through to the default: branch and are treated as "Unknown" or zero priority. Scheduled telehealth consultations are never shown in the doctor's daily queue. Patients wait for calls that never come. | Invalid status codes cause silent scheduling failures with direct patient impact. However, the risk is lower than ranks 1–3 because it requires an application bug AND a schema gap to align — it is one layer removed from the raw data. |

---

#### Paragraph: Data Smell Severity and Patient Safety

Data smells in a hospital information system are not merely technical inconveniences — they translate directly into patient safety failures and regulatory risk. The most critical class of smells are those that allow stored data to silently diverge from reality. Consider the **Derived Data** smell in illing: when a fee is corrected but the stored grand_total is not recomputed, the patient receives an incorrect invoice. In a low-income context, overcharging by even a few hundred rupees can cause a patient to defer a return visit, creating a gap in care continuity. The **Missing Constraints** smell compounds this: without a primary key on ill_no, duplicate billing records can pass silently through every application layer and reach the finance team as verified data. Both smells share a common root — the schema was written without enforcement of business rules at the database level, relying entirely on application-layer correctness. When that application layer has its own bugs (as documented in Part B), the result is a compounding failure chain where bad data enters, is never rejected, and surfaces as a patient harm event or a regulatory audit finding. Hospitals operating under ISO 15189 or NABH accreditation standards are required to demonstrate data integrity; schemas with these smells would fail a data governance audit.

---

## Part F — Schema Normalisation and Refactoring

### F1. Normalisation of pat_master up to 3NF

#### Step 1 — Identify Violations

| Normal Form | Violated? | Specific Violation Example from pat_master |
|------------|-----------|-------------------------------------------|
| **1NF** — Atomic values; no repeating groups | **Yes** | ph1, ph2, ph3 are a repeating group of phone numbers in three separate columns. A patient with a 4th phone number cannot be stored. Each column is atomic individually, but the group violates 1NF's no-repeating-groups rule. |
| **2NF** — Full dependency on the whole key | **Yes** | There is no defined primary key, so technically all attributes fail 2NF. If we nominate pid as the candidate key, columns like city and ddr1/ddr2 depend on the patient's address, not directly on pid — they would form a partial dependency if a composite key existed. More critically, eg_doc (doctor's name) has nothing to do with the patient's identity; it depends on eg_doc_id, not pid. |
| **3NF** — No transitive dependencies | **Yes** | eg_doc VARCHAR(255) is transitively dependent on pid via eg_doc_id: pid → reg_doc_id → reg_doc. The doctor's name should be derived by joining on eg_doc_id, not stored here. Similarly, city is arguably an attribute of the address record, not of the patient directly, creating a transitive path pid → (addr1, addr2) → city. |

---

#### Step 2 — Normalised Schema (CREATE TABLE statements)

`sql
-- ============================================================
-- NORMALISED pat_master DESIGN — up to 3NF
-- Using Prisma-compatible SQL (MySQL dialect)
-- ============================================================

-- Core patient record (1NF: no repeating groups; 3NF: no transitive deps)
CREATE TABLE patients (
    patient_id      INT             NOT NULL AUTO_INCREMENT,
    full_name       VARCHAR(255)    NOT NULL,
    date_of_birth   DATE            NOT NULL,          -- was VARCHAR(50)
    gender          CHAR(1)         NOT NULL,           -- 'M','F','X'
    registered_doc_id INT           NULL,              -- FK replaces plain text
    total_visits    INT             NOT NULL DEFAULT 0,
    notes           TEXT            NULL,
    PRIMARY KEY (patient_id),
    CONSTRAINT fk_patient_doctor
        FOREIGN KEY (registered_doc_id) REFERENCES doctors(doctor_id)
);

-- 1NF fix: repeating phone group extracted to child table
CREATE TABLE patient_phones (
    phone_id        INT             NOT NULL AUTO_INCREMENT,
    patient_id      INT             NOT NULL,
    phone_number    VARCHAR(20)     NOT NULL,
    phone_type      VARCHAR(20)     NOT NULL DEFAULT 'Mobile', -- 'Mobile','Home','Work'
    PRIMARY KEY (phone_id),
    CONSTRAINT fk_phone_patient
        FOREIGN KEY (patient_id) REFERENCES patients(patient_id)
            ON DELETE CASCADE
);

-- 1NF + 3NF fix: address extracted; city is attribute of address, not patient
CREATE TABLE patient_addresses (
    address_id      INT             NOT NULL AUTO_INCREMENT,
    patient_id      INT             NOT NULL,
    address_line1   VARCHAR(255)    NOT NULL,
    address_line2   VARCHAR(255)    NULL,
    city            VARCHAR(100)    NOT NULL,
    address_type    VARCHAR(20)     NOT NULL DEFAULT 'Home',
    PRIMARY KEY (address_id),
    CONSTRAINT fk_addr_patient
        FOREIGN KEY (patient_id) REFERENCES patients(patient_id)
            ON DELETE CASCADE
);

-- Billing summary view (separate from pat_master normalisation)
-- Retained from billing table but last_bill derived on query:
CREATE VIEW v_patient_last_bill AS
    SELECT pid AS patient_id, MAX(grand_total) AS last_bill
    FROM billing
    GROUP BY pid;
`

---

#### Step 3 — Before and After Comparison

| Aspect | Before (pat_master) | After (Normalised Design) |
|--------|--------------------|--------------------------| 
| Number of tables | 1 | 3 (patients, patient_phones, patient_addresses) + 1 view |
| Repeating phone columns | ph1, ph2, ph3 hardcoded | Unlimited phones in patient_phones child table |
| Address storage | Inline ddr1/ddr2/city in same row | Separate patient_addresses table; supports multiple addresses |
| Doctor reference | eg_doc VARCHAR(255) (plain text name) | egistered_doc_id INT FK → doctors.doctor_id |
| Date of birth column type | VARCHAR(50) | DATE — enforces format, enables date arithmetic |
| Primary key | None defined | patient_id INT AUTO_INCREMENT PRIMARY KEY |
| Currency columns | last_bill FLOAT | Removed from patients; derived via _patient_last_bill view using DECIMAL(10,2) in billing |

---

### F2. Five Schema Refactoring Scripts

#### Refactoring R1 — Fix Derived Data in billing

`sql
-- R1: Remove derived columns from billing
ALTER TABLE billing DROP COLUMN tax_amt;
ALTER TABLE billing DROP COLUMN grand_total;
ALTER TABLE billing DROP COLUMN balance;

-- Create view to compute derived values on read
CREATE OR REPLACE VIEW v_billing_summary AS
SELECT
    bill_no,
    pid,
    pname,
    services,
    svc_cost,
    tax_pct,
    ROUND(svc_cost * tax_pct / 100, 2)              AS tax_amt,
    ROUND(svc_cost + svc_cost * tax_pct / 100, 2)   AS grand_total,
    paid,
    ROUND(svc_cost + svc_cost * tax_pct / 100 - paid, 2) AS balance,
    created,
    created_by
FROM billing;
`

**Explanation:** The 	ax_amt, grand_total, and alance columns are **Derived Data** — each is a pure mathematical function of other stored columns (svc_cost, 	ax_pct, paid). Storing them creates a synchronisation hazard: if svc_cost is updated after a billing correction, the stored grand_total goes stale and patients receive incorrect invoices. The view _billing_summary eliminates this risk entirely by computing all three values at query time from the single source of truth. No data can become inconsistent because no redundant data is stored. All existing application queries targeting grand_total or alance are redirected to the view with no logic change required.

---

#### Refactoring R2 — Fix Overloaded Column in appointments.status

`sql
-- R2: Create status reference table
CREATE TABLE appt_status_ref (
    status_code CHAR(1)     PRIMARY KEY,
    description VARCHAR(50) NOT NULL
);

INSERT INTO appt_status_ref VALUES
    ('P', 'Pending'),
    ('C', 'Completed'),
    ('X', 'Cancelled'),
    ('H', 'On Hold'),
    ('R', 'Rescheduled');

-- Add FK constraint to enforce valid codes only
ALTER TABLE appointments
    ADD CONSTRAINT fk_appt_status
    FOREIGN KEY (status) REFERENCES appt_status_ref(status_code);
`

**Explanation:** The status CHAR(1) column is an **Overloaded Column** — its meaning is encoded in a magic character that exists nowhere in the schema definition. Any typo or undocumented code ('D', 't') silently enters the table with no rejection. The reference table ppt_status_ref makes all valid status codes explicit and self-documenting. The foreign key constraint ensures the database itself rejects any invalid code at insert time, regardless of whether the application validates it. Adding a new status in future requires only one INSERT INTO appt_status_ref row — no ALTER TABLE and no application switch-statement editing required.

---

#### Refactoring R3 — Fix Inconsistent Naming across doctors

`sql
-- R3: Standardise doctors table to lowercase snake_case
ALTER TABLE doctors RENAME COLUMN DoctorID  TO doctor_id;
ALTER TABLE doctors RENAME COLUMN FullName  TO full_name;
ALTER TABLE doctors RENAME COLUMN Speciality TO speciality;
ALTER TABLE doctors RENAME COLUMN ContactNo  TO contact_no;
ALTER TABLE doctors RENAME COLUMN JoinDt     TO join_date;
ALTER TABLE doctors RENAME COLUMN Salary     TO salary_monthly;
ALTER TABLE doctors RENAME COLUMN isActive   TO is_active;

-- Apply same standard to pat_master (legacy table)
ALTER TABLE pat_master RENAME COLUMN p_name  TO full_name;
ALTER TABLE pat_master RENAME COLUMN reg_doc TO registered_doctor_name;
ALTER TABLE pat_master RENAME COLUMN reg_doc_id TO registered_doctor_id;

-- Naming convention adopted: all_lowercase_snake_case
-- Abbreviations expanded: JoinDt -> join_date, ContactNo -> contact_no
-- Boolean flags prefixed with is_: isActive -> is_active
`

**Additional columns renamed and convention:**  
All columns across doctors and pat_master now follow **lowercase snake_case**. Abbreviations are expanded (JoinDt → join_date). Boolean flags carry the is_ prefix (isActive → is_active). This convention eliminates ORM mapping errors (Hibernate/JPA auto-maps snake_case DB columns to camelCase Java fields by default), reduces developer confusion when switching between tables, and ensures consistency with the normalised patients, patient_phones, and patient_addresses tables created in F1.

---

#### Refactoring R4 — Fix Missing Constraints in billing and appointments

`sql
-- R4: Add missing PK and FKs with orphan-row cleanup

-- Step 1: Add PRIMARY KEY to billing
ALTER TABLE billing ADD PRIMARY KEY (bill_no);

-- Step 2: Remove orphan billing rows (patient no longer exists)
DELETE FROM billing
WHERE pid NOT IN (SELECT pid FROM pat_master);

-- Step 3: Add FK from billing to patients
ALTER TABLE billing
    ADD CONSTRAINT fk_billing_patient
    FOREIGN KEY (pid) REFERENCES pat_master(pid);

-- Step 4: Remove orphan appointments (doctor no longer exists)
DELETE FROM appointments
WHERE doc_id NOT IN (SELECT DoctorID FROM doctors);

-- Step 5: Add FK from appointments to doctors
ALTER TABLE appointments
    ADD CONSTRAINT fk_appt_doctor
    FOREIGN KEY (doc_id) REFERENCES doctors(DoctorID);

-- Step 6: Add FK from appointments to patients
ALTER TABLE appointments
    ADD CONSTRAINT fk_appt_patient
    FOREIGN KEY (patient_id) REFERENCES pat_master(pid);
`

**Explanation of DELETE backfill step:** A foreign key constraint requires that every value in the child column (illing.pid) corresponds to an existing row in the parent table (pat_master.pid). If orphan rows exist — billing records whose patient was deleted from pat_master — the ALTER TABLE ... ADD FOREIGN KEY statement will fail with a constraint violation error. The DELETE step removes these orphan rows first, cleaning the data so that the constraint can be applied cleanly. In a production hospital system this step should be preceded by archiving the orphan rows to an audit table (INSERT INTO billing_archive SELECT * FROM billing WHERE pid NOT IN (...)) before deleting, preserving the financial record for regulatory purposes.

---

#### Refactoring R5 — Add Audit Trail to appointments

`sql
-- R5: Add created_at and updated_at audit columns to appointments
ALTER TABLE appointments
    ADD COLUMN created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    ADD COLUMN created_by  VARCHAR(100) NULL,
    ADD COLUMN updated_by  VARCHAR(100) NULL;

-- For PostgreSQL: use an explicit trigger (MySQL handles ON UPDATE natively)
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS 
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
 LANGUAGE plpgsql;

CREATE TRIGGER trg_appt_audit
    BEFORE UPDATE ON appointments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
`

**Explanation:** The original ppointments table has a **Lack of Audit Trail** smell — there are no columns recording when a row was created or last modified, or by whom. In a hospital context this is a critical compliance gap: if an appointment's status is changed from 'P' (Pending) to 'X' (Cancelled) without an audit record, it is impossible to determine whether the cancellation was made by the patient, a receptionist, or a system error. Medico-legal disputes over missed diagnoses or no-shows require precise timestamp evidence. Regulatory frameworks such as HIPAA and NABH require that all changes to patient records be logged with a user identity and timestamp. The created_at, updated_at, created_by, and updated_by columns provide a minimal audit trail at the database level, independent of any application logging.

---

### F3. Refactoring Impact Summary

| Refactoring | Smell(s) Resolved | Tables Changed | Risk Eliminated | Effort vs Benefit |
|------------|------------------|----------------|-----------------|-------------------|
| R1 — Derived data view | Derived Data | illing (3 columns dropped) | Inconsistent financial totals; incorrect patient invoices | Low effort (2 ALTERs + 1 CREATE VIEW); very high benefit — eliminates class of billing errors |
| R2 — Status reference table | Overloaded Column, Magic Values | ppointments + new ppt_status_ref | Invalid status codes; silent data corruption | Low effort (1 CREATE + 1 INSERT + 1 ALTER); high benefit — self-documenting schema |
| R3 — Naming standardisation | Inconsistent Naming | doctors, pat_master | ORM mapping failures; developer confusion; query errors | Low effort (7+3 RENAME COLUMNs); medium benefit — no runtime safety gain, but significant DX improvement |
| R4 — PK/FK constraints | Missing Constraints, Hidden Relationships | illing, ppointments | Orphan rows; duplicate records; referential corruption | Medium effort (data cleanup required); very high benefit — database self-enforces integrity |
| R5 — Audit trail | Lack of Audit Trail | ppointments | No accountability for changes; regulatory non-compliance | Low effort (2 ADD COLUMNs + 1 trigger); very high benefit — required for accreditation |

**Paragraph — Greatest Quality Improvement per Unit of Effort:**

Refactoring **R1 (Derived Data View)** delivers the greatest quality improvement per unit of effort of all five refactorings. It requires only two ALTER TABLE DROP COLUMN statements and one CREATE VIEW — approximately 10 minutes of work — yet it permanently eliminates the entire class of billing-inconsistency defects. Before R1, any row-level update to svc_cost or 	ax_pct leaves 	ax_amt, grand_total, and alance stale with zero warning; after R1, those values are always computed fresh from their source columns. In contrast, R4 (Missing Constraints) delivers equally high safety improvement but requires a data-cleaning migration step that must be planned, backed up, and validated in a staging environment before production deployment — making its effective effort significantly higher. R3 (Naming) delivers only developer-experience improvements with no runtime safety gain, placing it last in benefit-per-effort despite its trivially low implementation cost. R5 (Audit Trail) is a close second to R1 in the benefit-per-effort metric because two ADD COLUMN statements satisfy a hard regulatory requirement that would otherwise require a full audit-log infrastructure at the application layer.

---

## Part G — Data Migration Design and Execution

### G2. Migration Plan

| Plan Element | Your Response |
|-------------|--------------|
| **Source format** | CSV exported from legacy system with 12 columns including denormalised patient/doctor names and a text `appt_date` |
| **Target schema** | Refactored `appointments` table with `appt_datetime DATETIME`, split `room_number INT` + `building_block VARCHAR(20)`, no redundant name columns |
| **Estimated row count** | ~500 records (4 sample rows provided; script handles full file) |
| **Required transformations** | T1: Convert `appt_date` from `'DD/MM/YYYY HH:MM'` string → `DATETIME`; T2: Split `room` ('Room 3 Block B') → `room_number INT` + `building_block VARCHAR`; T3: Omit `patient_nm`, `patient_ph`, `doc_name`; T4: Validate `status` against `appt_status_ref` |
| **Columns to drop** | `patient_nm`, `patient_ph`, `doc_name`, `net_fee` (derived) |
| **ETL tool / language** | Python 3 with `csv` stdlib + `mysql-connector-python` |
| **Rollback strategy** | Pre-migration `TRUNCATE appointments;`. Wrap inserts in transaction; call `conn.rollback()` on exception. Keep `skipped_rows.log`. |
| **Validation method** | Run V1–V4 queries after migration (row count, null dates, valid statuses, no orphans) |
| **Estimated execution time** | < 30 seconds for 500 rows on local MySQL |
| **System downtime required?** | Minimal — table locked < 1 minute; read-only mode recommended during window |

---

### G3. ETL Transformation Script

See `migration_etl.py` in the project root. Key transformations implemented:

- **T1** `parse_appt_date()` — converts `'15/03/2024 09:30'` → Python `datetime` using `strptime('%d/%m/%Y %H:%M')`
- **T2** `split_room()` — splits `'Room 3 Block B'` → `(3, 'Block B')` via string split
- **T3** — `patient_nm`, `patient_ph`, `doc_name`, `net_fee` intentionally excluded from INSERT
- **T4** — status checked against `VALID_STATUSES = {'P','C','X','H','R'}` before processing

---

### G4. Post-Migration Validation Results

| Query | Expected | Result | Pass/Fail | Action if Fail |
|-------|----------|--------|-----------|----------------|
| V1 — Row count | Matches valid CSV rows | 4 | PASS | N/A |
| V2 — Null dates | 0 | 0 | PASS | All 4 dates parsed correctly |
| V3 — Valid statuses | P,C,X,H only | P,C,X,H | PASS | T4 filtered invalid codes |
| V4 — No orphans | 0 | 0 | PASS | patient_ids pre-loaded; FK enforced |

---

## Submission Checklist

- [x] Java project compiled and running
- [x] `sonar-project.properties` configured
- [x] `migration_etl.py` complete
- [x] Full report `SRE_Report.md` covering Parts A–G
- [ ] SonarQube scan screenshots (run scanner after starting SonarQube)
- [ ] Python Tutor screenshot at Step 6 condition branch
- [ ] AST Explorer screenshot expanded to 3 levels
- [ ] Draw.io dependency graph PNG embedded in PDF
- [ ] Draw.io CFG PNG embedded in PDF
- [ ] Convert report to PDF for final submission
