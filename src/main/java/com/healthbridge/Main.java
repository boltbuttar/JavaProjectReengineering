package com.healthbridge;

import com.healthbridge.service.PatientManager;
import com.healthbridge.service.AppointmentService;
import com.healthbridge.service.BillingProcessor;
import com.healthbridge.service.ReportGenerator;
import com.healthbridge.service.NotificationService;
import com.healthbridge.model.Patient;
import com.healthbridge.model.Appointment;
import com.healthbridge.util.HospitalUtils;

/**
 * Main entry point for HealthBridge Hospital Management System.
 * Demonstrates all services working together.
 */
public class Main {

    public static void main(String[] args) {

        // -- Setup services --
        PatientManager    manager      = new PatientManager();
        AppointmentService apptService = new AppointmentService(manager);
        BillingProcessor  billing      = new BillingProcessor(manager);
        ReportGenerator   report       = new ReportGenerator(manager);
        NotificationService notif       = new NotificationService();

        // -- Add a department --
        manager.addDepartment(1, "Cardiology", "Dr. Ahmed Khan", 500000.0);
        manager.addDepartment(2, "Pediatrics", "Dr. Sara Ali", 300000.0);

        // -- Add doctors --
        manager.addDoctor(12, "Dr. Kamran Raza", "Cardiology", "0300-1234567",
                          "01/06/2015", 150000.0, 1, 'Y');
        manager.addDoctor(7, "Dr. Ayesha Noor", "Pediatrics", "0321-9876543",
                          "15/09/2018", 120000.0, 2, 'Y');

        // -- Register patients --
        manager.registerPatient(5, "Ali Hassan", "12/05/1985", "M",
                "0312-9876543", "", "", "House 12", "Street 4", "Lahore",
                "Dr. Kamran Raza", "12", 8, 15000.0, "diabetic,hypertension");

        manager.registerPatient(8, "Sara Malik", "22/11/1990", "F",
                "0333-1234567", "", "", "Block B", "Phase 5", "Karachi",
                "Dr. Ayesha Noor", "DR-007", 3, 2000.0, "");

        // -- Schedule appointments --
        manager.scheduleAppointment(1001, 5, 12, "15/03/2024 09:30",
                'P', 1500.0, 0.0, "Room 3 Block B");

        manager.scheduleAppointment(1002, 8, 7, "15/03/2024 10:00",
                'C', 2000.0, 200.0, "Room 7 Block A");

        // -- Compute priority for the appointment (used in Part D trace) --
        Appointment appt = manager.findAppointmentById(1001);
        Patient patient  = manager.findPatientById(5);
        if (appt != null && patient != null) {
            int priority = HospitalUtils.computeAppointmentPriority(appt, patient);
            System.out.println("Priority score for appt 1001: " + priority);
        }

        // -- Print reports --
        manager.printPatientSummary(5);
        report.generatePatientReport(5);
        report.generateBillingReport(5);

        // -- Reschedule --
        apptService.rescheduleAppointment(1001, "18/03/2024 10:00", "Room 5 Block A");

        // -- Print doctor workload --
        manager.printDoctorWorkload();
        manager.printDepartmentBudgets();

        // -- Status label demo --
        System.out.println("Status 'H' => " + HospitalUtils.statusLabel('H'));
        System.out.println("Ward for Cardiology => " + HospitalUtils.getWardForSpeciality("Cardiology"));
    }
}
