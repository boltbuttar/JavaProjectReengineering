# migration_etl.py — Load legacy CSV into refactored appointments schema
# Implements:
#   T1 — Convert appt_date from 'DD/MM/YYYY HH:MM' string to DATETIME
#   T2 — Split room column 'Room 3 Block B' into room_number + building_block
#   T3 — Omit patient_nm, patient_ph, doc_name, net_fee (redundant after normalisation)
#   T4 — Validate status against VALID_STATUSES; log and skip any invalid code

import csv
import mysql.connector
from datetime import datetime

# T4: valid codes that exist in appt_status_ref table
VALID_STATUSES = {'P', 'C', 'X', 'H', 'R'}


def parse_appt_date(raw):
    """
    T1: Convert legacy date string to Python datetime.
    Supports 'DD/MM/YYYY HH:MM' (primary), 'YYYY-MM-DD HH:MM', 'DD-MM-YYYY HH:MM'.
    Raises ValueError if none of the formats match.
    """
    raw = raw.strip()
    for fmt in ('%d/%m/%Y %H:%M', '%Y-%m-%d %H:%M', '%d-%m-%Y %H:%M'):
        try:
            return datetime.strptime(raw, fmt)
        except ValueError:
            continue
    raise ValueError(f"Unrecognised date format: '{raw}'")


def split_room(raw):
    """
    T2: Split 'Room 3 Block B' into (3, 'Block B').
    Returns (room_number: int, building_block: str).
    Raises ValueError if format cannot be parsed.
    """
    raw = raw.strip()
    parts = raw.split()                      # ['Room', '3', 'Block', 'B']
    if len(parts) < 3 or parts[0].lower() != 'room':
        raise ValueError(f"Unexpected room format: '{raw}'")
    room_number    = int(parts[1])           # '3' -> 3
    building_block = ' '.join(parts[2:])    # 'Block B'
    return room_number, building_block


def migrate(csv_path, db_config):
    """
    Main ETL function.
    Opens the legacy CSV, applies all four transformations,
    and inserts valid rows into the refactored appointments table.
    """
    conn   = mysql.connector.connect(**db_config)
    cursor = conn.cursor()
    conn.autocommit = False     # all inserts in one atomic transaction

    skipped    = []   # appt_ids skipped due to invalid status (T4)
    error_rows = []   # appt_ids skipped due to date/room parse errors
    inserted   = 0

    with open(csv_path, newline='', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for row in reader:
            appt_id = row['appt_id']

            # ── T4: Validate status code ─────────────────────────────────
            status = row['status'].strip()
            if status not in VALID_STATUSES:
                print(f"  [SKIP] appt_id={appt_id}: invalid status='{status}'")
                skipped.append(appt_id)
                continue

            # ── T1: Parse appointment datetime ───────────────────────────
            try:
                appt_dt = parse_appt_date(row['appt_date'])
            except ValueError as e:
                print(f"  [ERROR] appt_id={appt_id}: date parse failed — {e}")
                error_rows.append(appt_id)
                continue

            # ── T2: Split room string ────────────────────────────────────
            try:
                room_no, block = split_room(row['room'])
            except ValueError as e:
                print(f"  [ERROR] appt_id={appt_id}: room parse failed — {e}")
                error_rows.append(appt_id)
                continue

            # ── T3: Insert — patient_nm, patient_ph, doc_name intentionally omitted
            cursor.execute(
                '''
                INSERT INTO appointments
                    (appt_id, patient_id, doc_id, appt_datetime,
                     status, fee, discount, room_number, building_block)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
                ''',
                (
                    int(row['appt_id']),
                    int(row['patient_id']),
                    int(row['doc_id']),
                    appt_dt,               # T1: proper DATETIME
                    status,                # T4: validated
                    float(row['fee']),
                    float(row['discount']),
                    room_no,               # T2: INT
                    block                  # T2: VARCHAR
                )
            )
            inserted += 1

    conn.commit()
    cursor.close()
    conn.close()

    print(f"\n=== Migration Complete ===")
    print(f"  Rows inserted : {inserted}")
    print(f"  Rows skipped  : {len(skipped)} (invalid status) -> {skipped}")
    print(f"  Rows errored  : {len(error_rows)} (parse errors) -> {error_rows}")


# ── Run the migration ─────────────────────────────────────────────────────────
if __name__ == '__main__':
    DB_CONFIG = {
        'host'    : 'localhost',
        'port'    : 3306,
        'user'    : 'root',
        'password': 'root',
        'database': 'healthbridge'
    }
    migrate('legacy_appointments.csv', DB_CONFIG)
