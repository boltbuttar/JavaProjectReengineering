package com.healthbridge.pharmacy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Formulary maps medical specialties to allowed medication IDs.
 */
public class Formulary {

    private Map<String, List<Integer>> allowed = new HashMap<>();

    public void allowForSpeciality(String speciality, int medicationId) {
        String key = normalize(speciality);
        List<Integer> meds = allowed.get(key);
        if (meds == null) {
            meds = new ArrayList<>();
            allowed.put(key, meds);
        }
        if (!meds.contains(medicationId)) {
            meds.add(medicationId);
        }
    }

    public void revokeForSpeciality(String speciality, int medicationId) {
        String key = normalize(speciality);
        List<Integer> meds = allowed.get(key);
        if (meds != null) {
            meds.remove(Integer.valueOf(medicationId));
        }
    }

    public boolean isAllowed(String speciality, int medicationId) {
        String key = normalize(speciality);
        List<Integer> meds = allowed.get(key);
        if (meds == null) return false;
        return meds.contains(medicationId);
    }

    public List<Integer> getAllowedMedicationIds(String speciality) {
        String key = normalize(speciality);
        List<Integer> meds = allowed.get(key);
        if (meds == null) return new ArrayList<>();
        return new ArrayList<>(meds);
    }

    private String normalize(String speciality) {
        if (speciality == null) return "GENERAL";
        return speciality.trim().toUpperCase();
    }
}
