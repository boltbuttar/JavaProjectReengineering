package com.healthbridge;

import com.healthbridge.model.Patient;
import com.healthbridge.model.Appointment;
import com.healthbridge.service.PatientManager;
import com.healthbridge.util.HospitalUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HealthBridge HMS.
 * These tests confirm that refactoring will not break external behaviour.
 */
public class HealthBridgeTest {

    private PatientManager manager;

    @BeforeEach
    void setUp() {
        manager = new PatientManager();
        manager.addDoctor(12, "Dr. Kamran Raza", "Cardiology", "0300-1234567",
                          "01/06/2015", 150000.0, 1, 'Y');
        manager.addDepartment(1, "Cardiology", "Dr. Kamran Raza", 500000.0);
        manager.registerPatient(5, "Ali Hassan", "12/05/1985", "M",
                "0312-9876543", "", "", "House 12", "Street 4", "Lahore",
                "Dr. Kamran Raza", "12", 8, 15000.0, "diabetic");
    }

    @Test
    void testRegisterPatient_found() {
        Patient p = manager.findPatientById(5);
        assertNotNull(p);
        assertEquals("Ali Hassan", p.getName());
    }

    @Test
    void testRegisterPatient_notFound() {
        assertNull(manager.findPatientById(999));
    }

    @Test
    void testScheduleAppointment_statusPending() {
        manager.scheduleAppointment(1001, 5, 12, "15/03/2024 09:30",
                'P', 1500.0, 0.0, "Room 3 Block B");
        Appointment a = manager.findAppointmentById(1001);
        assertNotNull(a);
        assertEquals('P', a.getStatus());
        assertEquals(1500.0, a.getFee(), 0.001);
        assertEquals(1500.0, a.getNetFee(), 0.001);
    }

    @Test
    void testCancelAppointment() {
        manager.scheduleAppointment(1001, 5, 12, "15/03/2024 09:30",
                'P', 1500.0, 0.0, "Room 3 Block B");
        manager.cancelAppointment(1001);
        assertEquals('X', manager.findAppointmentById(1001).getStatus());
    }

    @Test
    void testStatusLabel() {
        assertEquals("Pending",     HospitalUtils.statusLabel('P'));
        assertEquals("Completed",   HospitalUtils.statusLabel('C'));
        assertEquals("Cancelled",   HospitalUtils.statusLabel('X'));
        assertEquals("On Hold",     HospitalUtils.statusLabel('H'));
        assertEquals("Rescheduled", HospitalUtils.statusLabel('R'));
        assertEquals("Unknown Status", HospitalUtils.statusLabel('Z'));
    }

    @Test
    void testComputePriority_seniorHighFeeOnHold() {
        // Patient born 1950 = age ~75, fee=6000, status='H', visits=25
        manager.registerPatient(99, "Old Patient", "01/01/1950", "M",
                "0300-0000000","","","A","B","Lahore","Dr.X","12",25,0.0,"");
        manager.scheduleAppointment(2001, 99, 12, "20/03/2024 09:00",
                'H', 6000.0, 0.0, "Room 1 Block C");
        Patient p = manager.findPatientById(99);
        Appointment a = manager.findAppointmentById(2001);
        int score = HospitalUtils.computeAppointmentPriority(a, p);
        // basePriority=30(fee>=5000), ageAdj=20(>=65), visitAdj=15(>20), +25(status H)
        assertEquals(90, score);
    }

    @Test
    void testDeletePatient() {
        manager.deletePatient(5);
        assertNull(manager.findPatientById(5));
    }

    @Test
    void testWardForSpeciality() {
        assertEquals("WARD-C",   HospitalUtils.getWardForSpeciality("Cardiology"));
        assertEquals("GENERAL",  HospitalUtils.getWardForSpeciality("UnknownSpec"));
        assertEquals("GENERAL",  HospitalUtils.getWardForSpeciality(null));
    }
}
