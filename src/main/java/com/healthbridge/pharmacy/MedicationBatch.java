package com.healthbridge.pharmacy;

/**
 * MedicationBatch tracks batch-level stock for compliance and traceability.
 */
public class MedicationBatch {

    private int batchId;
    private int medicationId;
    private String batchNumber;
    private String receivedDate;     // DD/MM/YYYY
    private String expiryDate;       // DD/MM/YYYY
    private int unitsReceived;
    private int unitsRemaining;
    private String supplierName;
    private String storageLocation;
    private String notes;

    public MedicationBatch() {}

    public MedicationBatch(int batchId, int medicationId, String batchNumber,
                           String receivedDate, String expiryDate,
                           int unitsReceived, String supplierName,
                           String storageLocation, String notes) {
        this.batchId = batchId;
        this.medicationId = medicationId;
        this.batchNumber = batchNumber;
        this.receivedDate = receivedDate;
        this.expiryDate = expiryDate;
        this.unitsReceived = unitsReceived;
        this.unitsRemaining = unitsReceived;
        this.supplierName = supplierName;
        this.storageLocation = storageLocation;
        this.notes = notes;
    }

    public int getBatchId() { return batchId; }
    public int getMedicationId() { return medicationId; }
    public String getBatchNumber() { return batchNumber; }
    public String getReceivedDate() { return receivedDate; }
    public String getExpiryDate() { return expiryDate; }
    public int getUnitsReceived() { return unitsReceived; }
    public int getUnitsRemaining() { return unitsRemaining; }
    public String getSupplierName() { return supplierName; }
    public String getStorageLocation() { return storageLocation; }
    public String getNotes() { return notes; }

    public void setBatchId(int batchId) { this.batchId = batchId; }
    public void setMedicationId(int medicationId) { this.medicationId = medicationId; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
    public void setReceivedDate(String receivedDate) { this.receivedDate = receivedDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
    public void setUnitsReceived(int unitsReceived) { this.unitsReceived = unitsReceived; }
    public void setUnitsRemaining(int unitsRemaining) { this.unitsRemaining = unitsRemaining; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public void setStorageLocation(String storageLocation) { this.storageLocation = storageLocation; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean adjustUnits(int delta) {
        int newUnits = unitsRemaining + delta;
        if (newUnits < 0) return false;
        unitsRemaining = newUnits;
        return true;
    }

    public boolean isExpired(String today) {
        int exp = parseDateToInt(expiryDate);
        int now = parseDateToInt(today);
        if (exp == 0 || now == 0) return false;
        return exp < now;
    }

    public boolean isQuarantined() {
        if (notes == null) return false;
        return notes.toLowerCase().contains("quarantine");
    }

    private int parseDateToInt(String value) {
        if (value == null) return 0;
        String v = value.trim();
        if (v.length() < 8) return 0;
        if (v.contains("/")) {
            if (v.length() < 10) return 0;
            String dd = v.substring(0, 2);
            String mm = v.substring(3, 5);
            String yyyy = v.substring(6, 10);
            return safeDateInt(yyyy, mm, dd);
        }
        if (v.contains("-")) {
            if (v.length() < 10) return 0;
            String yyyy = v.substring(0, 4);
            String mm = v.substring(5, 7);
            String dd = v.substring(8, 10);
            return safeDateInt(yyyy, mm, dd);
        }
        return 0;
    }

    private int safeDateInt(String yyyy, String mm, String dd) {
        try {
            int y = Integer.parseInt(yyyy);
            int m = Integer.parseInt(mm);
            int d = Integer.parseInt(dd);
            return (y * 10000) + (m * 100) + d;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public String toString() {
        return "MedicationBatch{" +
                "batchId=" + batchId +
                ", medicationId=" + medicationId +
                ", batchNumber='" + batchNumber + '\'' +
                ", receivedDate='" + receivedDate + '\'' +
                ", expiryDate='" + expiryDate + '\'' +
                ", unitsReceived=" + unitsReceived +
                ", unitsRemaining=" + unitsRemaining +
                ", supplierName='" + supplierName + '\'' +
                ", storageLocation='" + storageLocation + '\'' +
                ", notes='" + notes + '\'' +
                '}';
    }
}
