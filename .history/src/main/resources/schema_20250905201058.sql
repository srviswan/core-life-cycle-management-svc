-- Cash Flow Management Service Database Schema
-- MS SQL Server with Temporal Tables

-- Create database if not exists
IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = 'cashflow_db')
BEGIN
    CREATE DATABASE cashflow_db;
END
GO

USE cashflow_db;
GO

-- Cash Flows table with temporal support
CREATE TABLE cash_flows (
    cash_flow_id NVARCHAR(50) NOT NULL,
    request_id NVARCHAR(50) NOT NULL,
    contract_id NVARCHAR(50) NOT NULL,
    position_id NVARCHAR(50),
    lot_id NVARCHAR(50),
    schedule_id NVARCHAR(50),
    calculation_date DATE NOT NULL,
    cash_flow_type NVARCHAR(50) NOT NULL,
    equity_leg_amount DECIMAL(18,2),
    interest_leg_amount DECIMAL(18,2),
    total_amount DECIMAL(18,2) NOT NULL,
    currency NVARCHAR(3) NOT NULL DEFAULT 'USD',
    state NVARCHAR(20) NOT NULL DEFAULT 'PENDING',
    
    -- Equity details
    equity_unrealized_pnl DECIMAL(18,2),
    equity_realized_pnl DECIMAL(18,2),
    equity_total_pnl DECIMAL(18,2),
    equity_dividend_amount DECIMAL(18,2),
    equity_withholding_tax DECIMAL(18,2),
    equity_net_dividend DECIMAL(18,2),
    
    -- Interest details
    interest_accrued_amount DECIMAL(18,2),
    interest_rate DECIMAL(8,4),
    interest_notional_amount DECIMAL(18,2),
    
    -- Audit fields
    created_timestamp DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    updated_timestamp DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    
    -- Temporal table fields
    valid_from DATETIME2 GENERATED ALWAYS AS ROW START HIDDEN NOT NULL,
    valid_to DATETIME2 GENERATED ALWAYS AS ROW END HIDDEN NOT NULL,
    PERIOD FOR SYSTEM_TIME (valid_from, valid_to),
    
    CONSTRAINT PK_cash_flows PRIMARY KEY (cash_flow_id),
    CONSTRAINT FK_cash_flows_request FOREIGN KEY (request_id) REFERENCES calculation_requests(request_id)
) WITH (SYSTEM_VERSIONING = ON (HISTORY_TABLE = dbo.cash_flows_history));
GO

-- Calculation Requests table
CREATE TABLE calculation_requests (
    request_id NVARCHAR(50) NOT NULL,
    calculation_type NVARCHAR(50) NOT NULL,
    calculation_date DATE NOT NULL,
    from_date DATE NOT NULL,
    to_date DATE NOT NULL,
    underlying NVARCHAR(50),
    index_name NVARCHAR(50),
    market_data_strategy NVARCHAR(50),
    status NVARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_timestamp DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    completed_timestamp DATETIME2,
    error_message NVARCHAR(MAX),
    
    CONSTRAINT PK_calculation_requests PRIMARY KEY (request_id)
);
GO

-- Settlement Instructions table
CREATE TABLE settlement_instructions (
    instruction_id NVARCHAR(50) NOT NULL,
    contract_id NVARCHAR(50) NOT NULL,
    counterparty NVARCHAR(100) NOT NULL,
    currency NVARCHAR(3) NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    settlement_date DATE NOT NULL,
    status NVARCHAR(20) NOT NULL DEFAULT 'PENDING',
    settlement_type NVARCHAR(50),
    account_number NVARCHAR(50),
    bank_code NVARCHAR(20),
    reference NVARCHAR(100),
    created_timestamp DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    updated_timestamp DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    error_message NVARCHAR(MAX),
    
    CONSTRAINT PK_settlement_instructions PRIMARY KEY (instruction_id)
);
GO

-- Market Data Cache table
CREATE TABLE market_data_cache (
    cache_key NVARCHAR(100) NOT NULL,
    symbol NVARCHAR(50) NOT NULL,
    data_type NVARCHAR(50) NOT NULL,
    data_content NVARCHAR(MAX) NOT NULL,
    created_timestamp DATETIME2 NOT NULL DEFAULT GETUTCDATE(),
    expires_at DATETIME2 NOT NULL,
    
    CONSTRAINT PK_market_data_cache PRIMARY KEY (cache_key)
);
GO

-- Indexes for performance
CREATE INDEX IX_cash_flows_contract_date ON cash_flows (contract_id, calculation_date);
CREATE INDEX IX_cash_flows_request_id ON cash_flows (request_id);
CREATE INDEX IX_cash_flows_type_state ON cash_flows (cash_flow_type, state);

CREATE INDEX IX_settlement_instructions_status ON settlement_instructions (status);
CREATE INDEX IX_settlement_instructions_counterparty ON settlement_instructions (counterparty);
CREATE INDEX IX_settlement_instructions_currency ON settlement_instructions (currency);

CREATE INDEX IX_market_data_cache_expires ON market_data_cache (expires_at);
CREATE INDEX IX_market_data_cache_symbol ON market_data_cache (symbol);

-- Stored procedures for common operations
CREATE PROCEDURE sp_cleanup_expired_cache
AS
BEGIN
    DELETE FROM market_data_cache 
    WHERE expires_at < GETUTCDATE();
END
GO

-- Create cleanup job (if SQL Server Agent is available)
-- EXEC msdb.dbo.sp_add_job
--     @job_name = 'Cleanup Expired Cache',
--     @enabled = 1;
-- EXEC msdb.dbo.sp_add_jobstep
--     @job_name = 'Cleanup Expired Cache',
--     @step_name = 'Cleanup',
--     @command = 'EXEC cashflow_db.dbo.sp_cleanup_expired_cache';
-- EXEC msdb.dbo.sp_add_schedule
--     @schedule_name = 'Hourly Cleanup',
--     @freq_type = 4,
--     @freq_interval = 1,
--     @freq_subday_type = 8,
--     @freq_subday_interval = 1;
-- EXEC msdb.dbo.sp_attach_schedule
--     @job_name = 'Cleanup Expired Cache',
--     @schedule_name = 'Hourly Cleanup';
-- EXEC msdb.dbo.sp_add_jobserver
--     @job_name = 'Cleanup Expired Cache';
