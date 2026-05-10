// prisma_queries.js — Sample Prisma Client queries for Parts E, F, G
// Run: node prisma_queries.js
// Demonstrates schema.prisma validation and migration results

const { PrismaClient } = require('@prisma/client');
const prisma = new PrismaClient();

async function main() {

  console.log('\n=== G4 Validation Queries via Prisma ===\n');

  // V1: Row count — must match valid rows in CSV
  const rowCount = await prisma.appointment.count();
  console.log(`V1 — Migrated rows: ${rowCount}`);

  // V2: Null datetime check
  const nullDates = await prisma.appointment.count({
    where: { apptDatetime: null }
  });
  console.log(`V2 — Null datetimes: ${nullDates} (expected: 0)`);

  // V3: Distinct status codes
  const statuses = await prisma.appointment.findMany({
    select: { status: true },
    distinct: ['status']
  });
  console.log(`V3 — Distinct statuses: ${statuses.map(s => s.status).join(', ')}`);

  // V4: Orphan appointments (patient_id not in patients)
  const orphans = await prisma.appointment.count({
    where: {
      patient: null
    }
  });
  console.log(`V4 — Orphan appointments: ${orphans} (expected: 0)`);

  console.log('\n=== Sample Data Queries ===\n');

  // List all patients with their phone numbers
  const patients = await prisma.patient.findMany({
    include: {
      phones:    true,
      addresses: true,
      registeredDoctor: { select: { fullName: true, speciality: true } }
    }
  });

  for (const p of patients) {
    console.log(`Patient: ${p.fullName} (DOB: ${p.dateOfBirth.toISOString().split('T')[0]})`);
    console.log(`  Doctor: ${p.registeredDoctor?.fullName ?? 'None'} — ${p.registeredDoctor?.speciality ?? ''}`);
    console.log(`  Phones: ${p.phones.map(ph => ph.phoneNumber).join(', ') || 'None'}`);
    console.log(`  City  : ${p.addresses[0]?.city ?? 'N/A'}`);
  }

  // List appointments with room split info
  console.log('\n--- Appointments ---');
  const appts = await prisma.appointment.findMany({
    include: {
      patient: { select: { fullName: true } },
      doctor:  { select: { fullName: true } },
      statusRef: true
    },
    orderBy: { apptDatetime: 'asc' }
  });

  for (const a of appts) {
    console.log(`  [${a.apptId}] ${a.patient.fullName} | Dr ${a.doctor.fullName}`);
    console.log(`    Date: ${a.apptDatetime} | Status: ${a.statusRef.description}`);
    console.log(`    Room: ${a.roomNumber} | Block: ${a.buildingBlock}`);
    console.log(`    Fee: ${a.fee} | Discount: ${a.discount}`);
  }

  // Billing summary (derived values computed by Prisma, not stored)
  console.log('\n--- Billing Summary (derived on query) ---');
  const bills = await prisma.billing.findMany({
    include: { patient: { select: { fullName: true } } }
  });
  for (const b of bills) {
    const taxAmt    = Number(b.svcCost) * Number(b.taxPct) / 100;
    const grandTotal = Number(b.svcCost) + taxAmt;
    const balance    = grandTotal - Number(b.paid);
    console.log(`  ${b.billNo} | ${b.patient.fullName} | Total: ${grandTotal.toFixed(2)} | Balance: ${balance.toFixed(2)}`);
  }
}

main()
  .catch(console.error)
  .finally(() => prisma.$disconnect());
