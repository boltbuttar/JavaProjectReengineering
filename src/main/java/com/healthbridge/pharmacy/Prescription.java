package com.healthbridge.pharmacy;

/**
 * Prescription entity for pharmacy workflow.
 */
public class Prescription {

    private int prescriptionId;
    private int patientId;
    private int doctorId;
    private int medicationId;
    private int quantity;
    private String dosage;
    private String frequency;
    private int durationDays;
    private String instructions;
    private String status;          // NEW, DISPENSED, CANCELLED
    private String issuedDate;
    private String dispensedDate;

    public Prescription() {}

    public Prescription(int prescriptionId, int patientId, int doctorId, int medicationId,
                        int quantity, String dosage, String frequency, int durationDays,
                        String instructions, String issuedDate) {
        this.prescriptionId = prescriptionId;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.medicationId = medicationId;
        this.quantity = quantity;
        this.dosage = dosage;
        this.frequency = frequency;
        this.durationDays = durationDays;
        this.instructions = instructions;
        this.issuedDate = issuedDate;
        this.status = "NEW";
    }

    public int getPrescriptionId() { return prescriptionId; }
    public int getPatientId() { return patientId; }
    public int getDoctorId() { return doctorId; }
    public int getMedicationId() { return medicationId; }
    public int getQuantity() { return quantity; }
    public String getDosage() { return dosage; }
    public String getFrequency() { return frequency; }
    public int getDurationDays() { return durationDays; }
    public String getInstructions() { return instructions; }
    public String getStatus() { return status; }
    public String getIssuedDate() { return issuedDate; }
    public String getDispensedDate() { return dispensedDate; }

    public void setPrescriptionId(int prescriptionId) { this.prescriptionId = prescriptionId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }
    public void setDoctorId(int doctorId) { this.doctorId = doctorId; }
    public void setMedicationId(int medicationId) { this.medicationId = medicationId; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public void setDurationDays(int durationDays) { this.durationDays = durationDays; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public void setStatus(String status) { this.status = status; }
    public void setIssuedDate(String issuedDate) { this.issuedDate = issuedDate; }
    public void setDispensedDate(String dispensedDate) { this.dispensedDate = dispensedDate; }

    public boolean isActive() {
        if (status == null) return true;
        return !"CANCELLED".equalsIgnoreCase(status);
    }

    public int estimateTotalUnits(int unitsPerDose, int dosesPerDay) {
        if (durationDays <= 0) return 0;
        if (unitsPerDose <= 0 || dosesPerDay <= 0) return 0;
        return unitsPerDose * dosesPerDay * durationDays;
    }

    public void markDispensed(String date) {
        this.status = "DISPENSED";
        this.dispensedDate = date;
    }

    public void cancel(String reason) {
        this.status = "CANCELLED";
        if (reason != null && !reason.trim().isEmpty()) {
            this.instructions = appendReason(reason);
        }
    }

    private String appendReason(String reason) {
        String current = (instructions == null) ? "" : instructions;
        if (!current.isEmpty()) current = current + " | ";
        return current + "Cancelled: " + reason;
    }
}
