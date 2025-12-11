-- Expanded SQL Server schema for Budget Planning with Scenarios and History
-- Run in SQL Server Express (create database 'budgetdb' or update connection string)

CREATE TABLE employee (
    employee_id INT IDENTITY(1,1) PRIMARY KEY,
    employee_name NVARCHAR(255) NOT NULL,
    status NVARCHAR(50) CHECK (status IN ('active', 'non_operational', 'long_holiday')) DEFAULT 'active',
    grade NVARCHAR(50) NULL,
    location NVARCHAR(255) NULL,
    country NVARCHAR(255) NULL,
    gender NVARCHAR(20) CHECK (gender IN ('male','female','other')) DEFAULT 'other',
    technical_skills NVARCHAR(MAX),   -- comma-separated e.g. "python,sql,cloud"
    functional_skills NVARCHAR(MAX),  -- comma-separated e.g. "equity swaps,pricing,component X"
    cost_per_month DECIMAL(12,2) DEFAULT 0.0,
    region NVARCHAR(100) NULL,
    fte_capacity DECIMAL(5,2) DEFAULT 1.00,
    created_at DATETIME DEFAULT GETDATE()
);

CREATE TABLE project (
    project_id INT IDENTITY(1,1) PRIMARY KEY,
    project_name NVARCHAR(255) NOT NULL,
    funding_source NVARCHAR(255) NULL,
    project_driver NVARCHAR(255) NULL,
    stakeholders NVARCHAR(MAX) NULL,   -- comma-separated list
    impact NVARCHAR(MAX) NULL,
    metrics NVARCHAR(MAX) NULL,
    comments NVARCHAR(MAX) NULL,
    max_budget DECIMAL(14,2) DEFAULT 0.0,
    region_preference NVARCHAR(255) NULL,
    required_skills NVARCHAR(MAX) NULL,       -- JSON: {"technical": ["python"], "functional": ["pricing"]}
    start_month CHAR(7) NULL,                 -- YYYY-MM
    end_month CHAR(7) NULL,
    created_at DATETIME DEFAULT GETDATE()
);

CREATE TABLE scenario (
    scenario_id INT IDENTITY(1,1) PRIMARY KEY,
    scenario_name NVARCHAR(255) NOT NULL,
    created_by NVARCHAR(255) NULL,
    created_date DATETIME DEFAULT GETDATE(),
    base_scenario_id INT NULL REFERENCES scenario(scenario_id)
);

CREATE TABLE allocation (
    allocation_id INT IDENTITY(1,1) PRIMARY KEY,
    scenario_id INT REFERENCES scenario(scenario_id),
    employee_id INT REFERENCES employee(employee_id),
    project_id INT REFERENCES project(project_id),
    month CHAR(7) NOT NULL,   -- YYYY-MM
    allocation_fraction DECIMAL(5,4) NOT NULL,  -- 0.0000–1.0000
    cost DECIMAL(14,2) NOT NULL,
    created_at DATETIME DEFAULT GETDATE()
);

CREATE TABLE history_log (
    history_id INT IDENTITY(1,1) PRIMARY KEY,
    scenario_id INT NULL,
    table_name NVARCHAR(100),
    record_id INT NULL,
    changed_by NVARCHAR(255) NULL,
    timestamp DATETIME DEFAULT GETDATE(),
    old_value NVARCHAR(MAX) NULL,
    new_value NVARCHAR(MAX) NULL
);

CREATE TABLE project_region_distribution (
    prd_id INT IDENTITY(1,1) PRIMARY KEY,
    project_id INT REFERENCES project(project_id),
    region NVARCHAR(50) NOT NULL,
    share DECIMAL(5,4) -- e.g., 0.50 for 50%
);

-- Indexes
CREATE INDEX IX_allocation_scenario_month ON allocation(scenario_id, month);
CREATE INDEX IX_allocation_employee_month ON allocation(employee_id, month);

