package com.healthbridge.pharmacy;

import com.healthbridge.model.Patient;
import com.healthbridge.service.PatientManager;

import java.util.List;

/**
 * DispenseService coordinates inventory checks, interactions, and policy rules.
 */
public class DispenseService {

    private PharmacyInventory inventory;
    private Formulary formulary;
    private DrugInteractionChecker interactionChecker;
    private PatientManager manager;

    public DispenseService(PharmacyInventory inventory,
                           Formulary formulary,
                           DrugInteractionChecker interactionChecker,
                           PatientManager manager) {
        this.inventory = inventory;
        this.formulary = formulary;
        this.interactionChecker = interactionChecker;
        this.manager = manager;
    }

    public boolean dispensePrescription(Prescription rx, String doctorSpeciality,
                                        List<String> currentMedicationNames, String today) {
        if (rx == null) {
            System.out.println("ERROR: Prescription is required.");
            return false;
        }
        if (!rx.isActive()) {
            System.out.println("ERROR: Prescription is cancelled.");
            return false;
        }
        if (rx.getQuantity() <= 0) {
            System.out.println("ERROR: Quantity must be positive.");
            return false;
        }

        Patient patient = (manager != null) ? manager.findPatientById(rx.getPatientId()) : null;
        if (patient == null) {
            System.out.println("ERROR: Patient not found for prescription " + rx.getPrescriptionId());
            return false;
        }

        Medication med = (inventory != null) ? inventory.findMedicationById(rx.getMedicationId()) : null;
        if (med == null) {
            System.out.println("ERROR: Medication not found for id " + rx.getMedicationId());
            return false;
        }
        if (med.isExpired(today)) {
            System.out.println("ERROR: Medication is expired: " + med.getName());
            return false;
        }

        if (formulary != null && !formulary.isAllowed(doctorSpeciality, med.getMedicationId())) {
            System.out.println("ERROR: Medication not allowed for speciality: " + doctorSpeciality);
            return false;
        }

        if (interactionChecker != null) {
            List<String> warnings = interactionChecker.checkInteractions(currentMedicationNames, med.getName());
            if (!warnings.isEmpty()) {
                System.out.println("ERROR: Interaction detected. Dispense blocked.");
                for (String w : warnings) {
                    System.out.println("  - " + w);
                }
                return false;
            }
        }

        if (med.getStockUnits() < rx.getQuantity()) {
            System.out.println("ERROR: Insufficient stock for " + med.getName());
            return false;
        }

        boolean updated = (inventory != null) && inventory.reduceStock(med.getMedicationId(), rx.getQuantity());
        if (!updated) {
            System.out.println("ERROR: Stock update failed for " + med.getName());
            return false;
        }

        rx.markDispensed(today);
        double cost = rx.getQuantity() * med.getUnitPrice();

        System.out.println("--- Dispense Summary ---");
        System.out.println("Patient : " + patient.getName());
        System.out.println("Medication : " + med.getName() + " " + med.getStrengthMg() + "mg");
        System.out.println("Quantity : " + rx.getQuantity());
        System.out.println("Cost     : " + cost);
        System.out.println("Status   : " + rx.getStatus());

        return true;
    }
}
