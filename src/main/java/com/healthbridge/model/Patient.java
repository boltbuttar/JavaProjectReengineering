package com.healthbridge.model;

/**
 * Patient entity — uses primitive obsession (no Address or PhoneNumber value objects).
 * All personal data stored as raw Strings/ints.
 */
public class Patient {

    // ---- Primitive Obsession smell: address should be an Address object ----
    public int patientId;
    public String name;
    public String dateOfBirth;   // should be LocalDate
    public String gender;
    public String phone1;
    public String phone2;
    public String phone3;        // repeating group — not normalised
    public String addressLine1;
    public String addressLine2;
    public String city;
    public String registeredDoctorName;   // plain text, not FK
    public String registeredDoctorId;     // sometimes "DR-042", sometimes "12"
    public int totalVisits;
    public double lastBill;               // FLOAT for currency — loss of precision
    public String notes;                  // JSON blobs / CSV tags mixed

    public Patient() {}

    public Patient(int patientId, String name, String dateOfBirth, String gender,
                   String phone1, String phone2, String phone3,
                   String addressLine1, String addressLine2, String city,
                   String registeredDoctorName, String registeredDoctorId,
                   int totalVisits, double lastBill, String notes) {
        this.patientId = patientId;
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.phone1 = phone1;
        this.phone2 = phone2;
        this.phone3 = phone3;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.registeredDoctorName = registeredDoctorName;
        this.registeredDoctorId = registeredDoctorId;
        this.totalVisits = totalVisits;
        this.lastBill = lastBill;
        this.notes = notes;
    }

    // ---- Data Class smell: only getters/setters, zero behaviour ----
    public int getPatientId()                   { return patientId; }
    public void setPatientId(int id)            { this.patientId = id; }

    public String getName()                     { return name; }
    public void setName(String name)            { this.name = name; }

    public String getDateOfBirth()              { return dateOfBirth; }
    public void setDateOfBirth(String dob)      { this.dateOfBirth = dob; }

    public String getGender()                   { return gender; }
    public void setGender(String gender)        { this.gender = gender; }

    public String getPhone1()                   { return phone1; }
    public void setPhone1(String phone1)        { this.phone1 = phone1; }

    public String getPhone2()                   { return phone2; }
    public void setPhone2(String phone2)        { this.phone2 = phone2; }

    public String getPhone3()                   { return phone3; }
    public void setPhone3(String phone3)        { this.phone3 = phone3; }

    public String getAddressLine1()             { return addressLine1; }
    public void setAddressLine1(String a)       { this.addressLine1 = a; }

    public String getAddressLine2()             { return addressLine2; }
    public void setAddressLine2(String a)       { this.addressLine2 = a; }

    public String getCity()                     { return city; }
    public void setCity(String city)            { this.city = city; }

    public String getRegisteredDoctorName()     { return registeredDoctorName; }
    public void setRegisteredDoctorName(String n){ this.registeredDoctorName = n; }

    public String getRegisteredDoctorId()       { return registeredDoctorId; }
    public void setRegisteredDoctorId(String id){ this.registeredDoctorId = id; }

    public int getTotalVisits()                 { return totalVisits; }
    public void setTotalVisits(int v)           { this.totalVisits = v; }

    public double getLastBill()                 { return lastBill; }
    public void setLastBill(double lastBill)    { this.lastBill = lastBill; }

    public String getNotes()                    { return notes; }
    public void setNotes(String notes)          { this.notes = notes; }
}
