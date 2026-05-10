package com.healthbridge.pharmacy;

import java.util.List;

/**
 * Simple report generator for pharmacy inventory.
 */
public class PharmacyReportGenerator {

    public void printInventorySummary(PharmacyInventory inventory, String today) {
        if (inventory == null) return;
        System.out.println("--- Pharmacy Inventory Summary ---");
        System.out.println("Total items : " + inventory.getMedicationCount());
        System.out.println("Total value : " + inventory.getTotalInventoryValue());
        System.out.println("Expired     : " + inventory.listExpired(today).size());
        System.out.println("Low stock   : " + inventory.listLowStock(20).size());
    }

    public void printLowStockReport(PharmacyInventory inventory, int threshold) {
        if (inventory == null) return;
        List<Medication> low = inventory.listLowStock(threshold);
        System.out.println("--- Low Stock Report ---");
        for (Medication m : low) {
            System.out.println(m.getName() + " | stock=" + m.getStockUnits());
        }
    }

    public void printExpiredReport(PharmacyInventory inventory, String today) {
        if (inventory == null) return;
        List<Medication> expired = inventory.listExpired(today);
        System.out.println("--- Expired Medications ---");
        for (Medication m : expired) {
            System.out.println(m.getName() + " | exp=" + m.getExpiryDate());
        }
    }
}
