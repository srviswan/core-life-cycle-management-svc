-- Initialize test database for Cash Flow Management Service integration tests

-- Create schema
IF NOT EXISTS (SELECT * FROM sys.schemas WHERE name = 'cashflow')
BEGIN
    CREATE SCHEMA cashflow
END

-- Create cash_flows table
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'cashflow.cash_flows') AND type in (N'U'))
BEGIN
    CREATE TABLE cashflow.cash_flows (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        cash_flow_id VARCHAR(100) NOT NULL UNIQUE,
        contract_id VARCHAR(100) NOT NULL,
        lot_id VARCHAR(100) NOT NULL,
        cash_flow_type VARCHAR(50) NOT NULL,
        cash_flow_date DATE NOT NULL,
        amount DECIMAL(19,4) NOT NULL,
        currency VARCHAR(3) NOT NULL,
        status VARCHAR(50) NOT NULL,
        calculation_basis VARCHAR(50) NOT NULL,
        accrual_start_date DATE,
        accrual_end_date DATE,
        settlement_date DATE,
        metadata NVARCHAR(MAX),
        request_id VARCHAR(100) NOT NULL,
        calculation_id VARCHAR(100) NOT NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        created_by VARCHAR(100),
        updated_by VARCHAR(100),
        version BIGINT NOT NULL DEFAULT 0
    )
END

-- Create settlement_instructions table
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'cashflow.settlement_instructions') AND type in (N'U'))
BEGIN
    CREATE TABLE cashflow.settlement_instructions (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        settlement_id VARCHAR(100) NOT NULL UNIQUE,
        contract_id VARCHAR(100) NOT NULL,
        cash_flow_id VARCHAR(100) NOT NULL,
        settlement_date DATE NOT NULL,
        settlement_type VARCHAR(50) NOT NULL,
        amount DECIMAL(19,4) NOT NULL,
        currency VARCHAR(3) NOT NULL,
        status VARCHAR(50) NOT NULL,
        priority VARCHAR(20) NOT NULL,
        settlement_method VARCHAR(50) NOT NULL,
        reference_number VARCHAR(100) NOT NULL,
        bank_name VARCHAR(200),
        account_number VARCHAR(50),
        routing_number VARCHAR(50),
        swift_code VARCHAR(20),
        iban VARCHAR(50),
        account_from VARCHAR(100),
        account_to VARCHAR(100),
        request_id VARCHAR(100) NOT NULL,
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        created_by VARCHAR(100),
        updated_by VARCHAR(100),
        version BIGINT NOT NULL DEFAULT 0
    )
END

-- Create calculation_requests table
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'cashflow.calculation_requests') AND type in (N'U'))
BEGIN
    CREATE TABLE cashflow.calculation_requests (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        request_id VARCHAR(100) NOT NULL UNIQUE,
        contract_id VARCHAR(100) NOT NULL,
        from_date DATE NOT NULL,
        to_date DATE NOT NULL,
        calculation_type VARCHAR(50) NOT NULL,
        calculation_id VARCHAR(100) NOT NULL,
        input_data_hash VARCHAR(64) NOT NULL,
        input_data_snapshot NVARCHAR(MAX),
        status VARCHAR(50) NOT NULL,
        cash_flow_count INT,
        settlement_count INT,
        total_amount DECIMAL(19,4),
        currency VARCHAR(3),
        calculation_duration BIGINT,
        error_message NVARCHAR(MAX),
        created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
        created_by VARCHAR(100),
        updated_by VARCHAR(100),
        version BIGINT NOT NULL DEFAULT 0
    )
END

-- Create indexes for performance
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_cash_flows_contract_id')
BEGIN
    CREATE INDEX idx_cash_flows_contract_id ON cashflow.cash_flows (contract_id)
END

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_cash_flows_request_id')
BEGIN
    CREATE INDEX idx_cash_flows_request_id ON cashflow.cash_flows (request_id)
END

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_settlement_instructions_contract_id')
BEGIN
    CREATE INDEX idx_settlement_instructions_contract_id ON cashflow.settlement_instructions (contract_id)
END

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_settlement_instructions_request_id')
BEGIN
    CREATE INDEX idx_settlement_instructions_request_id ON cashflow.settlement_instructions (request_id)
END

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_calculation_requests_contract_id')
BEGIN
    CREATE INDEX idx_calculation_requests_contract_id ON cashflow.calculation_requests (contract_id)
END

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_calculation_requests_request_id')
BEGIN
    CREATE INDEX idx_calculation_requests_request_id ON cashflow.calculation_requests (request_id)
END
