package com.healthbridge.util;

import com.healthbridge.model.Appointment;
import com.healthbridge.model.Patient;

/**
 * HospitalUtils — demonstrates Switch Statements and Long Method.
 *
 * Smells:
 *   - Switch Statement (Category 2 OO Abuser): two large switch blocks
 *     that should be replaced by polymorphism / Strategy pattern.
 *   - Long Method: computeAppointmentPriority() exceeds 60 lines.
 *   - Comments (Category 4): excessive comments masking bad structure.
 */
public class HospitalUtils {

    // =========================================================
    // SWITCH STATEMENT SMELL: status label
    // Should be replaced by an enum with a getLabel() method.
    // =========================================================

    /**
     * Switch Statement smell — 'P','C','X','H','R' hard-coded.
     * Every time a new status is added, this switch AND PatientManager
     * AND AppointmentService ALL have to change (Shotgun Surgery).
     */
    public static String statusLabel(char status) {
        switch (status) {
            case 'P': return "Pending";
            case 'C': return "Completed";
            case 'X': return "Cancelled";
            case 'H': return "On Hold";
            case 'R': return "Rescheduled";
            default:  return "Unknown Status";
        }
    }

    /**
     * Switch Statement smell — doctor speciality mapped to ward codes.
     * Again, a Strategy or Map would be cleaner and open/closed.
     */
    public static String getWardForSpeciality(String speciality) {
        if (speciality == null) return "GENERAL";
        switch (speciality.toUpperCase()) {
            case "CARDIOLOGY":   return "WARD-C";
            case "NEUROLOGY":    return "WARD-N";
            case "ORTHOPEDICS":  return "WARD-O";
            case "PEDIATRICS":   return "WARD-P";
            case "DERMATOLOGY":  return "WARD-D";
            case "ONCOLOGY":     return "WARD-ON";
            case "PSYCHIATRY":   return "WARD-PS";
            case "ENT":          return "WARD-E";
            case "OPHTHALMOLOGY":return "WARD-OP";
            default:             return "GENERAL";
        }
    }

    // =========================================================
    // LONG METHOD SMELL: computeAppointmentPriority
    // This method has 4 local variables + 3 conditionals — good
    // for Part D (dynamic analysis / Python Tutor trace).
    // =========================================================

    /**
     * Long Method smell — should be decomposed into:
     *   - calculateBasePriority()
     *   - applyAgeAdjustment()
     *   - applyVisitAdjustment()
     *   - applyStatusAdjustment()
     *
     * This method is also the TARGET for Part D (execution trace).
     *
     * @param appointment  the appointment to score
     * @param patient      the associated patient
     * @return priority score (higher = more urgent)
     */
    public static int computeAppointmentPriority(Appointment appointment, Patient patient) {

        // Local variable 1: base priority derived from fee
        int basePriority = 0;

        // Local variable 2: age-based adjustment
        int ageAdjustment = 0;

        // Local variable 3: visit frequency adjustment
        int visitAdjustment = 0;

        // Local variable 4: final computed score
        int finalScore = 0;

        // --- Step 1: compute base priority from fee bracket ---
        double fee = appointment.getFee();
        if (fee >= 5000) {
            basePriority = 30;           // premium patient
        } else if (fee >= 2000) {
            basePriority = 20;
        } else {
            basePriority = 10;           // standard
        }

        // --- Step 2: age adjustment (requires parsing DOB string) ---
        // Magic value: hardcoded age thresholds
        String dob = patient.getDateOfBirth();   // format 'DD/MM/YYYY'
        int age = 0;
        if (dob != null && dob.length() == 10) {
            try {
                int birthYear = Integer.parseInt(dob.substring(6, 10));
                int currentYear = java.time.LocalDate.now().getYear();
                age = currentYear - birthYear;
            } catch (NumberFormatException e) {
                age = 0;  // silently swallows parse error
            }
        }

        if (age >= 65) {
            ageAdjustment = 20;    // senior citizens get higher priority
        } else if (age >= 45) {
            ageAdjustment = 10;
        } else {
            ageAdjustment = 0;
        }

        // --- Step 3: frequent-visitor adjustment ---
        int visits = patient.getTotalVisits();
        if (visits > 20) {
            visitAdjustment = 15;   // loyal patient
        } else if (visits > 10) {
            visitAdjustment = 8;
        } else {
            visitAdjustment = 0;
        }

        // --- Step 4: emergency/status override ---
        char status = appointment.getStatus();
        // Switch statement smell again — same status chars hard-coded
        switch (status) {
            case 'P':
                finalScore = basePriority + ageAdjustment + visitAdjustment;
                break;
            case 'H':
                finalScore = basePriority + ageAdjustment + visitAdjustment + 25;
                break;
            case 'R':
                finalScore = basePriority + ageAdjustment;  // rescheduled = lower priority
                break;
            default:
                finalScore = 0;   // completed or cancelled = no priority
        }

        // Comments smell: verbose comment masking obvious code
        // Return the final priority score for this appointment
        // Higher scores will be serviced first by the scheduling queue
        return finalScore;
    }

    // =========================================================
    // DEAD CODE: formatDate never called after a migration
    // =========================================================

    /** @deprecated replaced by DateTimeFormatter — never removed */
    public static String formatDate(String ddmmyyyy) {
        if (ddmmyyyy == null || ddmmyyyy.length() < 10) return ddmmyyyy;
        // Convert DD/MM/YYYY to YYYY-MM-DD
        String dd   = ddmmyyyy.substring(0, 2);
        String mm   = ddmmyyyy.substring(3, 5);
        String yyyy = ddmmyyyy.substring(6, 10);
        return yyyy + "-" + mm + "-" + dd;
    }
}
