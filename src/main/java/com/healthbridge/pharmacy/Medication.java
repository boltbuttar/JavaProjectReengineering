package com.healthbridge.pharmacy;

/**
 * Medication entity for the pharmacy module.
 */
public class Medication {

    private int medicationId;
    private String name;
    private String form;          // Tablet, Syrup, Injection
    private int strengthMg;
    private String manufacturer;
    private double unitPrice;
    private int stockUnits;
    private String expiryDate;    // DD/MM/YYYY or YYYY-MM-DD

    public Medication() {}

    public Medication(int medicationId, String name, String form, int strengthMg,
                      String manufacturer, double unitPrice, int stockUnits, String expiryDate) {
        this.medicationId = medicationId;
        this.name = name;
        this.form = form;
        this.strengthMg = strengthMg;
        this.manufacturer = manufacturer;
        this.unitPrice = unitPrice;
        this.stockUnits = stockUnits;
        this.expiryDate = expiryDate;
    }

    public int getMedicationId() { return medicationId; }
    public String getName() { return name; }
    public String getForm() { return form; }
    public int getStrengthMg() { return strengthMg; }
    public String getManufacturer() { return manufacturer; }
    public double getUnitPrice() { return unitPrice; }
    public int getStockUnits() { return stockUnits; }
    public String getExpiryDate() { return expiryDate; }

    public void setMedicationId(int medicationId) { this.medicationId = medicationId; }
    public void setName(String name) { this.name = name; }
    public void setForm(String form) { this.form = form; }
    public void setStrengthMg(int strengthMg) { this.strengthMg = strengthMg; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
    public void setStockUnits(int stockUnits) { this.stockUnits = stockUnits; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public boolean isLowStock(int threshold) {
        return stockUnits < threshold;
    }

    public boolean isExpired(String today) {
        int exp = parseDateToInt(expiryDate);
        int now = parseDateToInt(today);
        if (exp == 0 || now == 0) return false;
        return exp < now;
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
}
