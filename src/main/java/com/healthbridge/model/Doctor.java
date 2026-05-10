package com.healthbridge.model;

/**
 * Doctor entity — naming inconsistency mirrors the legacy DB schema.
 * Smell: Inconsistent Naming (mixed case, abbreviations)
 * Smell: Data Class (no behaviour, only fields + getters/setters)
 */
public class Doctor {

    // ---- Inconsistent Naming: mix of PascalCase, camelCase, abbreviations ----
    private int    DoctorID;          // should be doctorId
    private String FullName;          // should be fullName
    private String Speciality;        // misspelling of "Specialty", also wrong case
    private String ContactNo;         // should be contactNumber
    private String JoinDt;            // abbreviated, should be joinDate, and be LocalDate
    private double Salary;            // FLOAT for monetary — precision loss
    private int    dept_id;           // snake_case mixed with PascalCase
    private char   isActive;          // 'Y','N','1' — magic boolean flag

    public Doctor() {}

    public Doctor(int DoctorID, String FullName, String Speciality,
                  String ContactNo, String JoinDt, double Salary,
                  int dept_id, char isActive) {
        this.DoctorID   = DoctorID;
        this.FullName   = FullName;
        this.Speciality = Speciality;
        this.ContactNo  = ContactNo;
        this.JoinDt     = JoinDt;
        this.Salary     = Salary;
        this.dept_id    = dept_id;
        this.isActive   = isActive;
    }

    public int    getDoctorID()           { return DoctorID; }
    public void   setDoctorID(int id)     { this.DoctorID = id; }

    public String getFullName()           { return FullName; }
    public void   setFullName(String n)   { this.FullName = n; }

    public String getSpeciality()         { return Speciality; }
    public void   setSpeciality(String s) { this.Speciality = s; }

    public String getContactNo()          { return ContactNo; }
    public void   setContactNo(String c)  { this.ContactNo = c; }

    public String getJoinDt()             { return JoinDt; }
    public void   setJoinDt(String d)     { this.JoinDt = d; }

    public double getSalary()             { return Salary; }
    public void   setSalary(double s)     { this.Salary = s; }

    public int    getDept_id()            { return dept_id; }
    public void   setDept_id(int id)      { this.dept_id = id; }

    public char   getIsActive()           { return isActive; }
    public void   setIsActive(char a)     { this.isActive = a; }
}
