package com.healthbridge.service;

import com.healthbridge.model.Billing;
import com.healthbridge.model.Patient;

/**
 * BillingProcessor — demonstrates Long Parameter List and Duplicate Code.
 *
 * Smells:
 *   - Long Parameter List (Category 1): processBill() has 12 parameters
 *   - Duplicate Code (Category 4): validation and computation copied from PatientManager
 *   - Speculative Generality (Category 4): unused discount tiers never called
 */
public class BillingProcessor {

    private PatientManager manager;

    public BillingProcessor(PatientManager manager) {
        this.manager = manager;
    }

    /**
     * Long Parameter List smell: 12 parameters.
     * A "MoneyTransaction" parameter object should group fee + discount + taxPct.
     */
    public Billing processBill(String billNo,       // param 1
                               int patientId,        // param 2
                               String services,      // param 3
                               double svcCost,       // param 4
                               double taxPct,        // param 5
                               double discount,      // param 6
                               double paid,          // param 7
                               String createdBy,     // param 8
                               String wardCode,      // param 9
                               String referralCode,  // param 10
                               boolean isEmergency,  // param 11
                               boolean isInsured) {  // param 12

        // ---- Duplicate Code: same null check as PatientManager.createBill ----
        if (billNo == null || billNo.trim().isEmpty()) {
            System.out.println("ERROR: Bill number is required.");
            return null;
        }

        // ---- Duplicate Code: same derivation logic as PatientManager ----
        double taxAmt     = svcCost * taxPct / 100.0;
        double grandTotal = svcCost + taxAmt - discount;
        double balance    = grandTotal - paid;
        String today      = java.time.LocalDate.now().toString();

        Patient p = manager.findPatientById(patientId);
        String pname = (p != null) ? p.getName() : "Unknown";

        // Emergency surcharge — adds complexity without helper method
        if (isEmergency) {
            grandTotal = grandTotal * 1.15;
            balance    = grandTotal - paid;
            System.out.println("Emergency surcharge of 15% applied.");
        }

        // Insurance discount — another if-block adding complexity
        if (isInsured) {
            grandTotal = grandTotal * 0.80;
            balance    = grandTotal - paid;
            System.out.println("Insurance discount of 20% applied.");
        }

        Billing bill = new Billing(billNo, patientId, pname, services,
                                   svcCost, taxPct, taxAmt, grandTotal,
                                   paid, balance, today, createdBy);

        System.out.println("Bill " + billNo + " processed. Grand Total: " + grandTotal);
        return bill;
    }

    // ---- Speculative Generality: discount tiers defined but never called anywhere ----
    public double applyTierOneDiscount(double amount)  { return amount * 0.95; }
    public double applyTierTwoDiscount(double amount)  { return amount * 0.90; }
    public double applyTierThreeDiscount(double amount){ return amount * 0.80; }
    public double applyVIPDiscount(double amount)      { return amount * 0.70; }

    /**
     * Duplicate Code: same outstanding-balance calculation as PatientManager.calculateOutstanding()
     */
    public double getPatientOutstanding(int patientId) {
        // This logic is an exact copy of PatientManager.calculateOutstanding()
        // — classic Duplicate Code smell
        double total = 0;
        for (Billing b : manager.getAllBillings()) {
            if (b.getPid() == patientId) {
                total += b.getBalance();
            }
        }
        return total;
    }
}
