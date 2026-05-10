package com.healthbridge.pharmacy;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory inventory for medications.
 */
public class PharmacyInventory {

    private List<Medication> medications = new ArrayList<>();

    public void addMedication(Medication med) {
        if (med == null) return;
        if (findMedicationById(med.getMedicationId()) != null) {
            updateStock(med.getMedicationId(), med.getStockUnits());
            return;
        }
        medications.add(med);
    }

    public Medication findMedicationById(int id) {
        for (Medication m : medications) {
            if (m.getMedicationId() == id) return m;
        }
        return null;
    }

    public Medication findMedicationByName(String name) {
        if (name == null) return null;
        for (Medication m : medications) {
            if (name.equalsIgnoreCase(m.getName())) return m;
        }
        return null;
    }

    public boolean updateStock(int id, int delta) {
        Medication m = findMedicationById(id);
        if (m == null) return false;
        int newStock = m.getStockUnits() + delta;
        if (newStock < 0) return false;
        m.setStockUnits(newStock);
        return true;
    }

    public boolean reduceStock(int id, int quantity) {
        if (quantity <= 0) return false;
        Medication m = findMedicationById(id);
        if (m == null) return false;
        if (m.getStockUnits() < quantity) return false;
        m.setStockUnits(m.getStockUnits() - quantity);
        return true;
    }

    public List<Medication> listLowStock(int threshold) {
        List<Medication> result = new ArrayList<>();
        for (Medication m : medications) {
            if (m.isLowStock(threshold)) result.add(m);
        }
        return result;
    }

    public List<Medication> listExpired(String today) {
        List<Medication> result = new ArrayList<>();
        for (Medication m : medications) {
            if (m.isExpired(today)) result.add(m);
        }
        return result;
    }

    public double getTotalInventoryValue() {
        double total = 0;
        for (Medication m : medications) {
            total += m.getUnitPrice() * m.getStockUnits();
        }
        return total;
    }

    public int getMedicationCount() {
        return medications.size();
    }

    public List<Medication> getAllMedications() {
        return medications;
    }

    public boolean removeMedication(int id) {
        Medication m = findMedicationById(id);
        if (m == null) return false;
        return medications.remove(m);
    }
}
