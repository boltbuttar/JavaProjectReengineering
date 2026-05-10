package com.healthbridge.model;

/**
 * Billing entity.
 * Smell: Derived Data — tax_amt, grand_total, balance are all computable.
 * Smell: Duplicate Data — pname duplicated from Patient.
 */
public class Billing {

    private String billNo;      // intended PK — no constraint enforced in code
    private int    pid;
    private String pname;       // patient name duplicated from Patient — Redundancy smell
    private String services;    // 'Lab,Xray,OPD' — Non-Atomic Fields smell
    private double svcCost;
    private double taxPct;
    private double taxAmt;      // DERIVED: svcCost * taxPct / 100
    private double grandTotal;  // DERIVED: svcCost + taxAmt
    private double paid;
    private double balance;     // DERIVED: grandTotal - paid
    private String created;     // date as text
    private String createdBy;   // username free text — no FK to users

    public Billing() {}

    public Billing(String billNo, int pid, String pname, String services,
                   double svcCost, double taxPct,
                   double taxAmt, double grandTotal,
                   double paid, double balance,
                   String created, String createdBy) {
        this.billNo     = billNo;
        this.pid        = pid;
        this.pname      = pname;
        this.services   = services;
        this.svcCost    = svcCost;
        this.taxPct     = taxPct;
        this.taxAmt     = taxAmt;
        this.grandTotal = grandTotal;
        this.paid       = paid;
        this.balance    = balance;
        this.created    = created;
        this.createdBy  = createdBy;
    }

    public String getBillNo()                   { return billNo; }
    public void   setBillNo(String billNo)       { this.billNo = billNo; }

    public int    getPid()                      { return pid; }
    public void   setPid(int pid)               { this.pid = pid; }

    public String getPname()                    { return pname; }
    public void   setPname(String pname)        { this.pname = pname; }

    public String getServices()                 { return services; }
    public void   setServices(String services)  { this.services = services; }

    public double getSvcCost()                  { return svcCost; }
    public void   setSvcCost(double svcCost)    { this.svcCost = svcCost; }

    public double getTaxPct()                   { return taxPct; }
    public void   setTaxPct(double taxPct)      { this.taxPct = taxPct; }

    public double getTaxAmt()                   { return taxAmt; }
    public void   setTaxAmt(double taxAmt)      { this.taxAmt = taxAmt; }

    public double getGrandTotal()               { return grandTotal; }
    public void   setGrandTotal(double gt)      { this.grandTotal = gt; }

    public double getPaid()                     { return paid; }
    public void   setPaid(double paid)          { this.paid = paid; }

    public double getBalance()                  { return balance; }
    public void   setBalance(double balance)    { this.balance = balance; }

    public String getCreated()                  { return created; }
    public void   setCreated(String created)    { this.created = created; }

    public String getCreatedBy()                { return createdBy; }
    public void   setCreatedBy(String cb)       { this.createdBy = cb; }
}
