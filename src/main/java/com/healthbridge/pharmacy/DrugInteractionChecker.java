package com.healthbridge.pharmacy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple interaction checker based on medication name pairs.
 */
public class DrugInteractionChecker {

    private Map<String, List<String>> interactions = new HashMap<>();

    public void addInteraction(String med, String interactsWith) {
        if (med == null || interactsWith == null) return;
        String a = med.trim().toUpperCase();
        String b = interactsWith.trim().toUpperCase();
        addOneWay(a, b);
        addOneWay(b, a);
    }

    private void addOneWay(String a, String b) {
        List<String> list = interactions.get(a);
        if (list == null) {
            list = new ArrayList<>();
            interactions.put(a, list);
        }
        if (!list.contains(b)) {
            list.add(b);
        }
    }

    public List<String> checkInteractions(List<String> currentMeds, String newMed) {
        List<String> warnings = new ArrayList<>();
        if (newMed == null) return warnings;
        String target = newMed.trim().toUpperCase();
        List<String> interacts = interactions.get(target);
        if (interacts == null || currentMeds == null) return warnings;

        for (String med : currentMeds) {
            if (med == null) continue;
            String m = med.trim().toUpperCase();
            if (interacts.contains(m)) {
                warnings.add(target + " interacts with " + m);
            }
        }
        return warnings;
    }

    public boolean hasCriticalInteraction(List<String> currentMeds, String newMed) {
        List<String> warnings = checkInteractions(currentMeds, newMed);
        return !warnings.isEmpty();
    }
}
