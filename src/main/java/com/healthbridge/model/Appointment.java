package com.healthbridge.model;

/**
 * Appointment entity.
 * Smell: Data Clumps — fee, discount, netFee always travel together.
 * Smell: Primitive Obsession — status stored as raw char instead of enum.
 */
public class Appointment {

    private int apptId;
    private int patientId;
    private String patientName;     // duplicated from Patient
    private String patientPhone;    // duplicated from Patient
    private int docId;
    private String docName;         // duplicated from Doctor
    private String apptDate;        // 'YYYY-MM-DD HH:MM' as plain text
    private char status;            // 'P','C','X','H','R' — magic value

    // ---- Data Clump: fee + discount + netFee always appear together ----
    private double fee;
    private double discount;
    private double netFee;          // derived: fee - discount (never recomputed)

    private String room;            // 'Room 3 Block B' — two facts in one field

    public Appointment() {}

    public Appointment(int apptId, int patientId, String patientName, String patientPhone,
                       int docId, String docName, String apptDate, char status,
                       double fee, double discount, double netFee, String room) {
        this.apptId      = apptId;
        this.patientId   = patientId;
        this.patientName = patientName;
        this.patientPhone= patientPhone;
        this.docId       = docId;
        this.docName     = docName;
        this.apptDate    = apptDate;
        this.status      = status;
        this.fee         = fee;
        this.discount    = discount;
        this.netFee      = netFee;
        this.room        = room;
    }

    public int    getApptId()        { return apptId; }
    public int    getPatientId()     { return patientId; }
    public String getPatientName()   { return patientName; }
    public String getPatientPhone()  { return patientPhone; }
    public int    getDocId()         { return docId; }
    public String getDocName()       { return docName; }
    public String getApptDate()      { return apptDate; }
    public char   getStatus()        { return status; }
    public double getFee()           { return fee; }
    public double getDiscount()      { return discount; }
    public double getNetFee()        { return netFee; }
    public String getRoom()          { return room; }

    public void setApptId(int apptId)            { this.apptId = apptId; }
    public void setPatientId(int patientId)       { this.patientId = patientId; }
    public void setPatientName(String n)          { this.patientName = n; }
    public void setPatientPhone(String p)         { this.patientPhone = p; }
    public void setDocId(int docId)               { this.docId = docId; }
    public void setDocName(String docName)        { this.docName = docName; }
    public void setApptDate(String apptDate)      { this.apptDate = apptDate; }
    public void setStatus(char status)            { this.status = status; }
    public void setFee(double fee)                { this.fee = fee; }
    public void setDiscount(double discount)      { this.discount = discount; }
    public void setNetFee(double netFee)          { this.netFee = netFee; }
    public void setRoom(String room)              { this.room = room; }
}
