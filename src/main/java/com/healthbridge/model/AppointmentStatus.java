package com.healthbridge.model;

/**
 * AppointmentStatus enum — REFACTORED replacement for raw char status codes.
 *
 * Before refactoring: char literals 'P','C','X','H','R' were scattered across
 * HospitalUtils, PatientManager, and AppointmentService with switch blocks.
 *
 * After refactoring: a single enum encapsulates both the code and the label.
 * All switch blocks are eliminated.
 */
public enum AppointmentStatus {
    PENDING('P',      "Pending"),
    COMPLETED('C',    "Completed"),
    CANCELLED('X',    "Cancelled"),
    ON_HOLD('H',      "On Hold"),
    RESCHEDULED('R',  "Rescheduled");

    private final char   code;
    private final String label;

    AppointmentStatus(char code, String label) {
        this.code  = code;
        this.label = label;
    }

    public char   getCode()  { return code; }
    public String getLabel() { return label; }

    /** Factory: convert legacy char code to enum safely. */
    public static AppointmentStatus fromCode(char code) {
        for (AppointmentStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("Unknown appointment status code: " + code);
    }

    @Override
    public String toString() { return label; }
}
