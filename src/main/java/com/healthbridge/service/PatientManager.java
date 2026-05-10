package com.healthbridge.service;

import com.healthbridge.model.Patient;
import com.healthbridge.model.Appointment;
import com.healthbridge.model.Billing;
import com.healthbridge.model.Doctor;
import com.healthbridge.model.Department;

import java.util.ArrayList;
import java.util.List;

/**
 * PatientManager — GOD CLASS / LARGE CLASS smell.
 *
 * This class handles patient registration, appointment scheduling,
 * billing, reporting, doctor assignment, and department lookups.
 * It has far too many responsibilities and violates SRP heavily.
 *
 * Smells present:
 *   1. Large Class (God Class) — Category 1 Bloater
 *   2. Long Method — processFullAdmission() exceeds 60 lines
 *   3. Duplicate Code — validation logic repeated in multiple methods
 *   4. Divergent Change — any schema change forces changes here
 */
public class PatientManager {

    // ---- internal "databases" (lists simulating persistence) ----
    private List<Patient>     patients     = new ArrayList<>();
    private List<Appointment> appointments = new ArrayList<>();
    private List<Billing>     billings     = new ArrayList<>();
    private List<Doctor>      doctors      = new ArrayList<>();
    private List<Department>  departments  = new ArrayList<>();

    // ======================================================================
    // PATIENT OPERATIONS
    // ======================================================================

    public void registerPatient(int id, String name, String dob, String gender,
                                String ph1, String ph2, String ph3,
                                String addr1, String addr2, String city,
                                String regDocName, String regDocId,
                                int totalVisits, double lastBill, String notes) {
        // ---- Duplicate Code: same null/empty check copy-pasted below ----
        if (name == null || name.trim().isEmpty()) {
            System.out.println("ERROR: Patient name is required.");
            return;
        }
        if (dob == null || dob.trim().isEmpty()) {
            System.out.println("ERROR: Date of birth is required.");
            return;
        }
        Patient p = new Patient(id, name, dob, gender, ph1, ph2, ph3,
                                addr1, addr2, city, regDocName, regDocId,
                                totalVisits, lastBill, notes);
        patients.add(p);
        System.out.println("Patient registered: " + name);
    }

    public Patient findPatientById(int id) {
        for (Patient p : patients) {
            if (p.getPatientId() == id) return p;
        }
        return null;
    }

    public void updatePatientPhone(int id, String phone1, String phone2, String phone3) {
        Patient p = findPatientById(id);
        if (p != null) {
            p.setPhone1(phone1);
            p.setPhone2(phone2);
            p.setPhone3(phone3);
        }
    }

    public void deletePatient(int id) {
        patients.removeIf(p -> p.getPatientId() == id);
    }

    public List<Patient> getAllPatients() {
        return patients;
    }

    public java.util.List<com.healthbridge.model.Billing> getAllBillings() {
        return billings;
    }

    // ======================================================================
    // APPOINTMENT OPERATIONS — Feature Envy: accesses Appointment fields directly
    // ======================================================================

    /**
     * Smell: Long Method — this method does scheduling, fee calc, and notification.
     * Smell: Feature Envy — directly manipulates Appointment internals instead of
     *        delegating to the Appointment class itself.
     */
    public void scheduleAppointment(int apptId, int patientId, int docId,
                                    String apptDate, char status,
                                    double fee, double discount, String room) {

        // ---- Duplicate Code: same null/empty check copy-pasted from registerPatient ----
        if (apptDate == null || apptDate.trim().isEmpty()) {
            System.out.println("ERROR: Appointment date is required.");
            return;
        }

        // Feature Envy: looks up patient and doctor to copy their names into Appointment
        Patient p = findPatientById(patientId);
        Doctor  d = findDoctorById(docId);

        String patientName  = (p != null) ? p.getName()         : "Unknown";
        String patientPhone = (p != null) ? p.getPhone1()       : "";
        String docName      = (d != null) ? d.getFullName()     : "Unknown";

        double netFee = fee - discount;   // derived value — never kept in sync

        Appointment appt = new Appointment(apptId, patientId, patientName, patientPhone,
                                           docId, docName, apptDate, status,
                                           fee, discount, netFee, room);
        appointments.add(appt);

        // ---- Inline notification logic — should be in NotificationService ----
        System.out.println("SMS sent to " + patientPhone + ": Appointment on " + apptDate);
        System.out.println("Email sent to doctor " + docName + ": New appointment at " + apptDate);

        // ---- Inline audit log — should be in AuditService ----
        System.out.println("AUDIT: appt_id=" + apptId + " created by system at " + java.time.LocalDateTime.now());
    }

    public Appointment findAppointmentById(int apptId) {
        for (Appointment a : appointments) {
            if (a.getApptId() == apptId) return a;
        }
        return null;
    }

    /**
     * Smell: Switch Statement — status handling with switch instead of polymorphism.
     * Smell: Magic Values — 'P','C','X','H','R' hard-coded throughout.
     */
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

    public void cancelAppointment(int apptId) {
        Appointment a = findAppointmentById(apptId);
        if (a != null) {
            a.setStatus('X');
            System.out.println("Appointment " + apptId + " cancelled.");
        }
    }

    public List<Appointment> getAppointmentsForPatient(int patientId) {
        List<Appointment> result = new ArrayList<>();
        for (Appointment a : appointments) {
            if (a.getPatientId() == patientId) result.add(a);
        }
        return result;
    }

    // ======================================================================
    // BILLING OPERATIONS
    // ======================================================================

    /**
     * Smell: Long Parameter List — 9 parameters.
     * Smell: Derived Data — taxAmt, grandTotal, balance passed in instead of computed.
     */
    public void createBill(String billNo, int pid, String pname, String services,
                           double svcCost, double taxPct,
                           double taxAmt, double grandTotal,
                           double paid, double balance,
                           String created, String createdBy) {

        // ---- Duplicate Code: same null/empty check for third time ----
        if (billNo == null || billNo.trim().isEmpty()) {
            System.out.println("ERROR: Bill number is required.");
            return;
        }

        Billing b = new Billing(billNo, pid, pname, services,
                                svcCost, taxPct, taxAmt, grandTotal,
                                paid, balance, created, createdBy);
        billings.add(b);
        System.out.println("Bill " + billNo + " created for patient " + pid);
    }

    public Billing findBillByNo(String billNo) {
        for (Billing b : billings) {
            if (b.getBillNo().equals(billNo)) return b;
        }
        return null;
    }

    public double calculateOutstanding(int pid) {
        double total = 0;
        for (Billing b : billings) {
            if (b.getPid() == pid) {
                total += b.getBalance();   // relies on stored derived value
            }
        }
        return total;
    }

    // ======================================================================
    // DOCTOR OPERATIONS
    // ======================================================================

    public void addDoctor(int doctorId, String fullName, String speciality,
                          String contactNo, String joinDt, double salary,
                          int deptId, char isActive) {
        Doctor d = new Doctor(doctorId, fullName, speciality, contactNo,
                              joinDt, salary, deptId, isActive);
        doctors.add(d);
    }

    public Doctor findDoctorById(int id) {
        for (Doctor d : doctors) {
            if (d.getDoctorID() == id) return d;
        }
        return null;
    }

    public List<Doctor> getActiveDoctors() {
        List<Doctor> active = new ArrayList<>();
        for (Doctor d : doctors) {
            // ---- Magic Boolean Flag: 'Y', 'N', '1' all mean active ----
            if (d.getIsActive() == 'Y' || d.getIsActive() == '1') {
                active.add(d);
            }
        }
        return active;
    }

    // ======================================================================
    // DEPARTMENT OPERATIONS
    // ======================================================================

    public void addDepartment(int deptId, String deptName, String hod, double budget) {
        departments.add(new Department(deptId, deptName, hod, budget));
    }

    public Department findDepartmentById(int id) {
        for (Department dept : departments) {
            if (dept.getDeptId() == id) return dept;
        }
        return null;
    }

    // ======================================================================
    // REPORTING — should be in a separate ReportService (Shotgun Surgery / God Class)
    // ======================================================================

    public void printPatientSummary(int patientId) {
        Patient p = findPatientById(patientId);
        if (p == null) {
            System.out.println("Patient not found.");
            return;
        }
        System.out.println("=== Patient Summary ===");
        System.out.println("Name       : " + p.getName());
        System.out.println("DOB        : " + p.getDateOfBirth());
        System.out.println("Gender     : " + p.getGender());
        System.out.println("Phone 1    : " + p.getPhone1());
        System.out.println("Phone 2    : " + p.getPhone2());
        System.out.println("Phone 3    : " + p.getPhone3());
        System.out.println("Address    : " + p.getAddressLine1() + ", " +
                                             p.getAddressLine2() + ", " + p.getCity());
        System.out.println("Reg Doctor : " + p.getRegisteredDoctorName());
        System.out.println("Visits     : " + p.getTotalVisits());
        System.out.println("Last Bill  : PKR " + p.getLastBill());
        System.out.println("Notes      : " + p.getNotes());
        System.out.println();
        // ---- Appointments sub-report ----
        List<Appointment> appts = getAppointmentsForPatient(patientId);
        System.out.println("Appointments (" + appts.size() + "):");
        for (Appointment a : appts) {
            System.out.println("  [" + a.getApptId() + "] " +
                    a.getApptDate() + " | " +
                    getAppointmentStatusLabel(a.getStatus()) + " | Dr " + a.getDocName() +
                    " | Fee: " + a.getFee() + " | Room: " + a.getRoom());
        }
    }

    public void printDoctorWorkload() {
        System.out.println("=== Doctor Workload ===");
        for (Doctor d : doctors) {
            long count = appointments.stream()
                    .filter(a -> a.getDocId() == d.getDoctorID())
                    .count();
            System.out.println("Dr " + d.getFullName() + " (" + d.getSpeciality() + "): " + count + " appointments");
        }
    }

    public void printDepartmentBudgets() {
        System.out.println("=== Department Budgets ===");
        for (Department dept : departments) {
            System.out.println(dept.getDeptName() + " | HOD: " + dept.getHod() +
                               " | Budget: PKR " + dept.getBudget());
        }
    }

    /**
     * LONG METHOD smell — processFullAdmission does registration, scheduling,
     * billing, notification, and audit in a single 80+ line method.
     */
    public void processFullAdmission(
            // patient params
            int patientId, String patientName, String dob, String gender,
            String ph1, String ph2, String ph3,
            String addr1, String addr2, String city,
            String regDocName, String regDocId,
            // appointment params
            int apptId, int docId, String apptDate, char apptStatus, String room,
            // billing params
            String billNo, String services,
            double fee, double discount,
            double svcCost, double taxPct,
            String createdBy) {

        // Step 1: Register patient
        if (patientName == null || patientName.trim().isEmpty()) {
            System.out.println("ERROR: Cannot admit — patient name is required.");
            return;
        }
        Patient existingPatient = findPatientById(patientId);
        if (existingPatient == null) {
            registerPatient(patientId, patientName, dob, gender,
                            ph1, ph2, ph3, addr1, addr2, city,
                            regDocName, regDocId, 0, 0.0, "");
        }

        // Step 2: Validate doctor
        Doctor doctor = findDoctorById(docId);
        if (doctor == null) {
            System.out.println("ERROR: Doctor ID " + docId + " not found. Aborting admission.");
            return;
        }

        // Step 3: Check appointment slot (naive — just checks same date/room)
        for (Appointment existing : appointments) {
            if (existing.getRoom().equals(room) && existing.getApptDate().equals(apptDate)) {
                System.out.println("WARNING: Room " + room + " already booked at " + apptDate);
                // continue anyway — no hard block (intentional design debt)
            }
        }

        // Step 4: Schedule appointment
        double netFee = fee - discount;
        Appointment appt = new Appointment(apptId, patientId, patientName, ph1,
                                           docId, doctor.getFullName(), apptDate, apptStatus,
                                           fee, discount, netFee, room);
        appointments.add(appt);

        // Step 5: Create bill with derived fields manually computed
        double taxAmt    = svcCost * taxPct / 100.0;
        double grandTotal = svcCost + taxAmt;
        double paid      = 0.0;
        double balance   = grandTotal - paid;
        String today     = java.time.LocalDate.now().toString();

        Billing bill = new Billing(billNo, patientId, patientName, services,
                                   svcCost, taxPct, taxAmt, grandTotal,
                                   paid, balance, today, createdBy);
        billings.add(bill);

        // Step 6: Update visit count manually
        Patient pt = findPatientById(patientId);
        if (pt != null) {
            pt.setTotalVisits(pt.getTotalVisits() + 1);
            pt.setLastBill(grandTotal);
        }

        // Step 7: Notification (inline — should be NotificationService)
        System.out.println("SMS to " + ph1 + ": Admitted. Appt on " + apptDate + " with Dr " + doctor.getFullName());

        // Step 8: Audit (inline — should be AuditService)
        System.out.println("AUDIT: Full admission processed for patient " + patientId +
                           " by " + createdBy + " at " + java.time.LocalDateTime.now());

        System.out.println("Admission complete for patient: " + patientName);
    }
}
