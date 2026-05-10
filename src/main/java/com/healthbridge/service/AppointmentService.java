package com.healthbridge.service;

import com.healthbridge.model.Appointment;
import com.healthbridge.model.Patient;
import com.healthbridge.model.Doctor;

/**
 * AppointmentService — demonstrates Feature Envy and Inappropriate Intimacy.
 *
 * Smells:
 *   - Feature Envy: methods here are more interested in PatientManager's data
 *     than in their own. They reach deep into PatientManager to pull fields.
 *   - Inappropriate Intimacy: directly accesses Patient and Doctor internals
 *     instead of asking those objects to do work themselves.
 *   - Message Chain: getManager().findPatientById(id).getPhone1()
 */
public class AppointmentService {

    // Inappropriate Intimacy: holds a direct reference to PatientManager
    // and calls its internals freely
    private PatientManager manager;

    public AppointmentService(PatientManager manager) {
        this.manager = manager;
    }

    /**
     * Feature Envy: this method is more interested in PatientManager data
     * than in Appointment. Most logic belongs in PatientManager or Patient.
     */
    public void rescheduleAppointment(int apptId, String newDate, String newRoom) {
        // Message Chain smell: a.getPatientId() -> manager.findPatientById() -> .getPhone1()
        Appointment a = manager.findAppointmentById(apptId);
        if (a == null) return;

        String oldDate  = a.getApptDate();
        String oldRoom  = a.getRoom();

        // Feature Envy: reaches into Patient for phone number
        Patient patient = manager.findPatientById(a.getPatientId());
        String phone    = (patient != null) ? patient.getPhone1() : "";
        String pName    = (patient != null) ? patient.getName()   : "Unknown";

        // Feature Envy: reaches into Doctor for name and specialty
        Doctor doctor   = manager.findDoctorById(a.getDocId());
        String dName    = (doctor != null) ? doctor.getFullName()   : "Unknown";
        String dSpec    = (doctor != null) ? doctor.getSpeciality() : "";

        a.setApptDate(newDate);
        a.setRoom(newRoom);
        a.setStatus('R');

        System.out.println("--- Reschedule Notice ---");
        System.out.println("Patient  : " + pName + " (" + phone + ")");
        System.out.println("Doctor   : Dr " + dName + " | " + dSpec);
        System.out.println("Old slot : " + oldDate + " @ " + oldRoom);
        System.out.println("New slot : " + newDate + " @ " + newRoom);
        System.out.println("SMS sent to " + phone + ": Your appt moved to " + newDate);
    }

    /**
     * Inappropriate Intimacy: directly reads Patient's internal fields
     * to validate business rules that should live in Patient itself.
     */
    public boolean isPatientEligibleForAppointment(int patientId) {
        Patient p = manager.findPatientById(patientId);
        if (p == null) return false;

        // Reaches into patient internals — should be p.isEligible()
        if (p.getTotalVisits() > 50) {
            System.out.println("Patient " + p.getName() + " flagged: >50 visits. Needs review.");
            return false;
        }
        if (p.getLastBill() > 100000) {
            System.out.println("Patient " + p.getName() + " has large outstanding: " + p.getLastBill());
            return false;
        }
        // Magic value smell: 'M', 'F' hard-coded
        if (!p.getGender().equals("M") && !p.getGender().equals("F") && !p.getGender().equals("X")) {
            System.out.println("Unknown gender code: " + p.getGender());
        }
        return true;
    }

    /**
     * Message Chain smell: deeply chained calls to navigate object graph.
     * manager -> findDoctorById -> getDept_id -> findDepartmentById -> getDeptName
     */
    public String getDoctorDepartmentName(int docId) {
        // Long chain of calls — fragile and hard to change
        Doctor d = manager.findDoctorById(docId);
        if (d == null) return "N/A";
        int deptId = d.getDept_id();
        com.healthbridge.model.Department dept = manager.findDepartmentById(deptId);
        if (dept == null) return "N/A";
        return dept.getDeptName();  // three hops: AppointmentService -> Doctor -> Department
    }
}
