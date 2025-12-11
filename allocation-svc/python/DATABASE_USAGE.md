# Database Integration Guide

## Overview

The allocation system can save results to SQL Server database in addition to Excel output. Database saving is **optional** and controlled via environment variables.

## Current Status

**Default Behavior**: Allocations are saved to **Excel only** (not database)

**Database Saving**: Can be enabled via environment variable `SAVE_TO_DB=true`

## Database Schema

The system uses the schema defined in `schema.sql`:
- `employee` - Employee master data
- `project` - Project master data
- `scenario` - Scenario definitions
- `allocation` - Allocation records (saved here)
- `history_log` - Audit trail
- `project_region_distribution` - Region distributions

## Enabling Database Saving

### Option 1: Environment Variables

Set these environment variables before running:

```bash
export SAVE_TO_DB=true
export DB_SERVER=localhost\\SQLEXPRESS
export DB_NAME=budgetdb
export DB_USER=sa                    # Optional, uses trusted connection if not set
export DB_PASSWORD=YourPassword      # Optional, uses trusted connection if not set
export CREATED_BY=your_name          # Optional, defaults to 'system'

python3 run_demo.py
```

### Option 2: Use Dedicated Script

Use `run_demo_with_db.py` which always saves to database:

```bash
python3 run_demo_with_db.py
```

## What Gets Saved

### Saved to Database:
- ✅ **Actual allocations** (project_id is not None)
- ✅ **Scenario records** (created if they don't exist)
- ✅ **History logs** (audit trail)

### NOT Saved to Database:
- ❌ Available capacity records (these are for planning only)
- ❌ Excel-specific formatting

## Database Functions

### `db.py` Functions

1. **`connect()`** - Connect to SQL Server
   ```python
   conn = connect(server='localhost\\SQLEXPRESS', database='budgetdb')
   ```

2. **`load_table()`** - Load data from database
   ```python
   employees = load_table(conn, 'employee')
   ```

3. **`write_allocations()`** - Save allocations to database
   ```python
   write_allocations(conn, allocations_df)
   ```

### `scenario.py` Functions

1. **`create_scenario()`** - Create new scenario
   ```python
   scenario_id = create_scenario(conn, 'My Scenario', 'user_name')
   ```

2. **`record_history()`** - Record changes in audit log
   ```python
   record_history(conn, scenario_id, 'allocation', None, old_val, new_val, 'user_name')
   ```

## Usage Examples

### Example 1: Save to Database

```bash
# Set environment variables
export SAVE_TO_DB=true
export DB_SERVER=localhost\\SQLEXPRESS
export DB_NAME=budgetdb
export CREATED_BY=john_doe

# Run demo
python3 run_demo.py
```

### Example 2: Programmatic Usage

```python
from db import connect, write_allocations, load_table
from scenario import create_scenario
from allocate_fully_optimized import fully_optimized_allocator
import pandas as pd

# Connect to database
conn = connect(server='localhost\\SQLEXPRESS', database='budgetdb')

# Load data from database
employees = load_table(conn, 'employee')
projects = load_table(conn, 'project')

# Create scenario
scenario_id = create_scenario(conn, 'Q1 2025 Plan', 'john_doe')

# Run allocator
allocations = fully_optimized_allocator(employees, projects, scenario_id)

# Save to database
allocations_df = pd.DataFrame(allocations)
actual_allocations = allocations_df[allocations_df['project_id'].notna()]
write_allocations(conn, actual_allocations)

conn.close()
```

### Example 3: Load from Database

```python
from db import connect, load_table

conn = connect(server='localhost\\SQLEXPRESS', database='budgetdb')

# Load existing allocations
allocations = load_table(conn, 'allocation')
scenarios = load_table(conn, 'scenario')
employees = load_table(conn, 'employee')
projects = load_table(conn, 'project')

# Filter by scenario
scenario_1_allocations = allocations[allocations['scenario_id'] == 1]

conn.close()
```

## Database Setup

### 1. Create Database

```sql
CREATE DATABASE budgetdb;
```

### 2. Run Schema

```bash
# Using sqlcmd or SQL Server Management Studio
sqlcmd -S localhost\SQLEXPRESS -d budgetdb -i schema.sql
```

Or run `schema.sql` in SQL Server Management Studio.

### 3. Verify Connection

```python
from db import connect, load_table

try:
    conn = connect()
    tables = load_table(conn, 'scenario')  # Test query
    print("✓ Database connection successful")
    conn.close()
except Exception as e:
    print(f"✗ Database connection failed: {e}")
```

## Troubleshooting

### Connection Errors

**Error**: `Could not open a connection to SQL Server`

**Solutions**:
1. Verify SQL Server is running
2. Check server name (use `localhost\\SQLEXPRESS` for Express)
3. Enable TCP/IP in SQL Server Configuration Manager
4. Check firewall settings

### Authentication Errors

**Error**: `Login failed for user`

**Solutions**:
1. Use trusted connection: Don't set `DB_USER` and `DB_PASSWORD`
2. Verify SQL Server authentication is enabled
3. Check username/password

### Database Not Found

**Error**: `Cannot open database "budgetdb"`

**Solutions**:
1. Create database: `CREATE DATABASE budgetdb;`
2. Run schema: Execute `schema.sql`
3. Verify database name in connection string

### ODBC Driver Missing

**Error**: `[Microsoft][ODBC Driver Manager] Data source name not found`

**Solutions**:
1. Install ODBC Driver 17 for SQL Server
2. On macOS: `brew install msodbcsql17 mssql-tools`
3. On Linux: Follow Microsoft installation guide
4. On Windows: Usually pre-installed

## Scripts with Database Support

1. **`run_demo.py`** - Optional DB saving (via `SAVE_TO_DB=true`)
2. **`run_demo_with_db.py`** - Always saves to DB
3. **`run_scenario_comparison.py`** - Optional DB saving (via `SAVE_TO_DB=true`)

## Best Practices

1. **Always save to Excel first** - Excel is the primary output format
2. **Enable DB saving for production** - Use database for persistence
3. **Use scenarios** - Create separate scenarios for different planning runs
4. **Record history** - Enable history logging for audit trails
5. **Test connection** - Verify database connection before running allocations

## Data Flow

```
Excel Template → Load Data → Run Allocator → Generate Allocations
                                              ↓
                                    ┌─────────┴─────────┐
                                    ↓                   ↓
                              Excel Output      Database (optional)
```

## Notes

- **Available capacity** records are NOT saved to database (they're planning tools)
- **Only actual allocations** (with project_id) are saved
- **Scenarios are created automatically** if they don't exist
- **History logs** are created for audit purposes
- **Database saving is optional** - Excel is always generated

