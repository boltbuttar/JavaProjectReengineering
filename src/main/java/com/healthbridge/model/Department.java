package com.healthbridge.model;

/**
 * Department entity.
 * Smell: Primitive Obsession — hod stored as plain String name instead of Doctor reference.
 */
public class Department {

    private int    deptId;
    private String deptName;
    private String hod;        // head-of-department as plain text name — hidden relationship
    private double budget;     // FLOAT for budget — precision loss

    public Department() {}

    public Department(int deptId, String deptName, String hod, double budget) {
        this.deptId   = deptId;
        this.deptName = deptName;
        this.hod      = hod;
        this.budget   = budget;
    }

    public int    getDeptId()             { return deptId; }
    public void   setDeptId(int id)       { this.deptId = id; }

    public String getDeptName()           { return deptName; }
    public void   setDeptName(String n)   { this.deptName = n; }

    public String getHod()               { return hod; }
    public void   setHod(String hod)     { this.hod = hod; }

    public double getBudget()             { return budget; }
    public void   setBudget(double b)     { this.budget = b; }
}
