-- Create test database
IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'cashflow_test')
BEGIN
    CREATE DATABASE cashflow_test
END
GO

USE cashflow_test
GO

-- Create schema
IF NOT EXISTS (SELECT * FROM sys.schemas WHERE name = 'cashflow')
BEGIN
    CREATE SCHEMA cashflow
END
GO
