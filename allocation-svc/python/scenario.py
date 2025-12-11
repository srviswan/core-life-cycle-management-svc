"""scenario.py - scenario creation and history utilities"""
import json
from datetime import datetime

def create_scenario(conn, name, created_by, base_scenario_id=None):
    cur = conn.cursor()
    cur.execute("INSERT INTO scenario (scenario_name, created_by, created_date, base_scenario_id) VALUES (?,?,?,?)", (name, created_by, datetime.utcnow(), base_scenario_id))
    # fetch id (SQL Server)
    cur.execute("SELECT SCOPE_IDENTITY() AS id")
    row = cur.fetchone()
    sid = int(row[0]) if row else None
    cur.commit()
    cur.close()
    return sid

def record_history(conn, scenario_id, table_name, record_id, old_value, new_value, changed_by):
    cur = conn.cursor()
    cur.execute("INSERT INTO history_log (scenario_id, table_name, record_id, changed_by, timestamp, old_value, new_value) VALUES (?,?,?,?,?,?,?)",
                (scenario_id, table_name, record_id, changed_by, datetime.utcnow(), json.dumps(old_value) if old_value is not None else None, json.dumps(new_value) if new_value is not None else None))
    cur.commit()
    cur.close()

