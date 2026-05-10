package com.healthbridge.pharmacy;

/**
 * MedicationOrder line item inside a prescription.
 */
public class MedicationOrder {

    private int orderId;
    private int medicationId;
    private String medicationName;
    private String dosage;          // "1 tablet"
    private String frequency;       // OD, BID, TID, QID
    private int durationDays;
    private int quantity;
    private boolean substitutionAllowed;
    private String startDate;       // "DD/MM/YYYY"
    private String instructions;

    public MedicationOrder() {}

    public MedicationOrder(int orderId, int medicationId, String medicationName, String dosage,
                           String frequency, int durationDays, int quantity,
                           boolean substitutionAllowed, String startDate, String instructions) {
        this.orderId = orderId;
        this.medicationId = medicationId;
        this.medicationName = medicationName;
        this.dosage = dosage;
        this.frequency = frequency;
        this.durationDays = durationDays;
        this.quantity = quantity;
        this.substitutionAllowed = substitutionAllowed;
        this.startDate = startDate;
        this.instructions = instructions;
    }

    public int getOrderId() { return orderId; }
    public int getMedicationId() { return medicationId; }
    public String getMedicationName() { return medicationName; }
    public String getDosage() { return dosage; }
    public String getFrequency() { return frequency; }
    public int getDurationDays() { return durationDays; }
    public int getQuantity() { return quantity; }
    public boolean isSubstitutionAllowed() { return substitutionAllowed; }
    public String getStartDate() { return startDate; }
    public String getInstructions() { return instructions; }

    public void setOrderId(int orderId) { this.orderId = orderId; }
    public void setMedicationId(int medicationId) { this.medicationId = medicationId; }
    public void setMedicationName(String medicationName) { this.medicationName = medicationName; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public void setDurationDays(int durationDays) { this.durationDays = durationDays; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setSubstitutionAllowed(boolean substitutionAllowed) { this.substitutionAllowed = substitutionAllowed; }
    public void setStartDate(String startDate) { this.startDate = startDate; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public int getTotalUnits() {
        if (quantity > 0) return quantity;
        if (durationDays <= 0) return 0;
        if (frequency == null) return 0;

        String freq = frequency.trim().toUpperCase();
        int unitsPerDay = 1;
        if ("OD".equals(freq)) unitsPerDay = 1;
        else if ("BID".equals(freq)) unitsPerDay = 2;
        else if ("TID".equals(freq)) unitsPerDay = 3;
        else if ("QID".equals(freq)) unitsPerDay = 4;
        else if ("HS".equals(freq)) unitsPerDay = 1;
        return unitsPerDay * durationDays;
    }

    public boolean isValidQuantity() {
        return getTotalUnits() > 0;
    }

    @Override
    public String toString() {
        return "MedicationOrder{" +
                "orderId=" + orderId +
                ", medicationId=" + medicationId +
                ", medicationName='" + medicationName + '\'' +
                ", dosage='" + dosage + '\'' +
                ", frequency='" + frequency + '\'' +
                ", durationDays=" + durationDays +
                ", quantity=" + quantity +
                ", substitutionAllowed=" + substitutionAllowed +
                ", startDate='" + startDate + '\'' +
                ", instructions='" + instructions + '\'' +
                '}';
    }
}
