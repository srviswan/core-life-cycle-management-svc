"""db.py - SQL Server helper (pyodbc) for Budget Planner"""
import pyodbc
import pandas as pd

def connect(server='localhost\\SQLEXPRESS', database='budgetdb', trusted=True, user=None, password=None):
    if trusted:
        conn_str = f"DRIVER={{ODBC Driver 17 for SQL Server}};SERVER={server};DATABASE={database};Trusted_Connection=yes;"
    else:
        conn_str = f"DRIVER={{ODBC Driver 17 for SQL Server}};SERVER={server};DATABASE={database};UID={user};PWD={password};"
    return pyodbc.connect(conn_str, autocommit=True)

def load_table(conn, table_name):
    return pd.read_sql(f"SELECT * FROM [{table_name}]", conn)

def write_allocations(conn, allocations):
    """Write allocations (list of dicts or DataFrame) into allocation table using executemany.
    Only writes actual allocations (project_id is not None).
    """
    df = allocations if hasattr(allocations, 'to_dict') else pd.DataFrame(allocations)
    
    # Filter out available capacity records (project_id is None)
    df = df[df['project_id'].notna()].copy()
    
    if len(df) == 0:
        return
    
    cur = conn.cursor()
    sql = "INSERT INTO allocation (scenario_id, employee_id, project_id, month, allocation_fraction, cost) VALUES (?,?,?,?,?,?)"
    data = []
    for idx, row in df.iterrows():
        try:
            data.append((
                int(row['scenario_id']), 
                int(row['employee_id']), 
                int(row['project_id']) if pd.notna(row['project_id']) else None,
                str(row['month']), 
                float(row['allocation_fraction']), 
                float(row['cost'])
            ))
        except Exception as e:
            print(f"Warning: Skipping row {idx} due to error: {e}")
            continue
    
    if len(data) > 0:
        cur.fast_executemany = True
        cur.executemany(sql, data)
        cur.commit()
    cur.close()

