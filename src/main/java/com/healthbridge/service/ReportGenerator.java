package com.healthbridge.service;

import com.healthbridge.model.Billing;
import java.util.List;
import java.util.ArrayList;

/**
 * Add getAllBillings() to PatientManager — adding to the existing list.
 * This is a helper class to expose the billing list from PatientManager.
 *
 * ReportGenerator — demonstrates Duplicate Code (Category 4) and Lazy Class (Category 4).
 *
 * Smells:
 *   - Duplicate Code: generatePatientReport() repeats logic from PatientManager.printPatientSummary()
 *   - Lazy Class: this class does almost nothing that PatientManager cannot do
 *   - Dead Code: exportToCSV() and exportToPDF() are never called
 */
public class ReportGenerator {

    private PatientManager manager;

    public ReportGenerator(PatientManager manager) {
        this.manager = manager;
    }

    /**
     * Duplicate Code: almost identical to PatientManager.printPatientSummary()
     * — same fields printed in the same order.
     */
    public void generatePatientReport(int patientId) {
        com.healthbridge.model.Patient p = manager.findPatientById(patientId);
        if (p == null) {
            System.out.println("Report Error: Patient not found.");
            return;
        }
        // ---- DUPLICATE of PatientManager.printPatientSummary() lines ----
        System.out.println("=== Patient Report ===");
        System.out.println("Name       : " + p.getName());
        System.out.println("DOB        : " + p.getDateOfBirth());
        System.out.println("Gender     : " + p.getGender());
        System.out.println("Phone 1    : " + p.getPhone1());
        System.out.println("Phone 2    : " + p.getPhone2());
        System.out.println("Phone 3    : " + p.getPhone3());
        System.out.println("Address    : " + p.getAddressLine1() + ", " +
                                             p.getAddressLine2() + ", " + p.getCity());
        System.out.println("Visits     : " + p.getTotalVisits());
        System.out.println("Last Bill  : PKR " + p.getLastBill());
    }

    /**
     * Duplicate Code: same outstanding-balance loop as PatientManager and BillingProcessor.
     */
    public void generateBillingReport(int patientId) {
        System.out.println("=== Billing Report ===");
        double total = 0;
        for (Billing b : manager.getAllBillings()) {
            if (b.getPid() == patientId) {
                System.out.println("Bill: " + b.getBillNo() +
                                   " | Total: " + b.getGrandTotal() +
                                   " | Paid: " + b.getPaid() +
                                   " | Balance: " + b.getBalance());
                total += b.getBalance();
            }
        }
        System.out.println("Outstanding: PKR " + total);
    }

    // ---- Dead Code: these methods are never invoked anywhere ----
    /** @deprecated never called */
    public String exportToCSV(int patientId) {
        StringBuilder sb = new StringBuilder();
        sb.append("patient_id,name,dob,phone\n");
        com.healthbridge.model.Patient p = manager.findPatientById(patientId);
        if (p != null) {
            sb.append(p.getPatientId()).append(",")
              .append(p.getName()).append(",")
              .append(p.getDateOfBirth()).append(",")
              .append(p.getPhone1()).append("\n");
        }
        return sb.toString();
    }

    /** @deprecated never called */
    public byte[] exportToPDF(int patientId) {
        // stub — always returns empty bytes
        return new byte[0];
    }

    /** @deprecated never called */
    private void sendReportByEmail(String email, String reportContent) {
        System.out.println("Sending report to " + email);
        // implementation never completed
    }
}
