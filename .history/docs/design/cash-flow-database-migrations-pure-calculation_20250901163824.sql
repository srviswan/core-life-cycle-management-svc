-- =====================================================
-- Cash Flow Management Service Database Migrations
-- =====================================================

-- Migration: V1.0.0 - Pure Calculation Service Schema
-- Date: 2024-01-15
-- Description: Create pure calculation database schema for Cash Flow Management Service
-- Technology: MS SQL Server with Temporal Tables for automatic versioning
-- Architecture: Pure Calculation - Only store what we generate

-- =====================================================
-- ARCHITECTURE PRINCIPLES
-- =====================================================
-- 1. Pure Calculation Service: Only own cash flow calculations
-- 2. No Input Data Storage: Don't store contracts, positions, lots, or contract references
-- 3. Performance First: Zero storage overhead for maximum calculation speed
-- 4. Clear Boundaries: Each service owns its data
-- 5. Regulatory Compliance: Store calculation metadata and input data hashing for audit trails

-- =====================================================
-- TEMPORAL TABLES BENEFITS
-- =====================================================
-- 1. Automatic Versioning: All changes are automatically tracked without triggers
-- 2. Point-in-Time Recovery: Query data as it existed at any point in time
-- 3. Audit Trail: Complete history of all changes with no additional code
-- 4. Performance: Optimized for temporal queries with minimal overhead
-- 5. Regulatory Compliance: Built-in support for MiFID and other regulatory requirements
-- 6. Data Integrity: Automatic maintenance of valid_from and valid_to timestamps

-- =====================================================
-- JSON DATA TYPE BENEFITS
-- =====================================================
-- 1. Native JSON Support: Built-in JSON data type with validation
-- 2. JSON Functions: ISJSON(), JSON_VALUE(), JSON_QUERY(), JSON_MODIFY()
-- 3. Indexing: Can create indexes on JSON properties
-- 4. Performance: Optimized storage and querying of JSON data
-- 5. Validation: Automatic JSON syntax validation
-- 6. Querying: SQL queries can directly access JSON properties

-- =====================================================
-- 1. CALCULATION REQUESTS - AUDIT TRAIL
-- =====================================================

-- Calculation Requests table (Temporal Table) - Track calculation requests
-- Note: This provides audit trail for regulatory compliance
-- We store input data hashing and optional snapshots for debugging
CREATE TABLE calculation_requests (
    request_id VARCHAR(50) NOT NULL PRIMARY KEY,
    calculation_type VARCHAR(20) NOT NULL,
    from_date DATE NOT NULL,
    to_date DATE NOT NULL,
    calculation_frequency VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    progress_percentage INT DEFAULT 0,
    error_message NVARCHAR(MAX),
    input_data_hash VARCHAR(64), -- Hash of input data for audit trail
    input_data_snapshot JSON, -- Optional: Full request snapshot for debugging
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    -- Temporal table columns
    valid_from DATETIME2 GENERATED ALWAYS AS ROW START,
    valid_to DATETIME2 GENERATED ALWAYS AS ROW END,
    PERIOD FOR SYSTEM_TIME (valid_from, valid_to)
) WITH (SYSTEM_VERSIONING = ON (HISTORY_TABLE = calculation_requests_history));

-- Indexes for calculation_requests table
CREATE INDEX idx_calc_requests_type ON calculation_requests(calculation_type);
CREATE INDEX idx_calc_requests_status ON calculation_requests(status);
CREATE INDEX idx_calc_requests_dates ON calculation_requests(from_date, to_date);
CREATE INDEX idx_calc_requests_hash ON calculation_requests(input_data_hash);

-- =====================================================
-- 2. CASH FLOWS - OUR PRIMARY OUTPUT
-- =====================================================

-- Cash Flows table (Temporal Table) - Our primary output
-- Note: This is what our service actually owns and generates
-- We don't store contract data - contract_id is just a string reference
CREATE TABLE cash_flows (
    cash_flow_id VARCHAR(50) NOT NULL PRIMARY KEY,
    request_id VARCHAR(50) NOT NULL,
    contract_id VARCHAR(50) NOT NULL, -- Just a string, no FK to contract data
    calculation_date DATE NOT NULL,
    cash_flow_type VARCHAR(30) NOT NULL,
    equity_leg_amount DECIMAL(20,2) NOT NULL,
    interest_leg_amount DECIMAL(20,2) NOT NULL,
    total_amount DECIMAL(20,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    state VARCHAR(20) NOT NULL DEFAULT 'REALIZED_UNSETTLED',
    equity_unrealized_pnl DECIMAL(20,2),
    equity_realized_pnl DECIMAL(20,2),
    equity_total_pnl DECIMAL(20,2),
    equity_dividend_amount DECIMAL(20,2),
    equity_withholding_tax DECIMAL(20,2),
    equity_net_dividend DECIMAL(20,2),
    interest_accrued_amount DECIMAL(20,2),
    interest_rate DECIMAL(10,6),
    interest_notional_amount DECIMAL(20,2),
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    -- Temporal table columns
    valid_from DATETIME2 GENERATED ALWAYS AS ROW START,
    valid_to DATETIME2 GENERATED ALWAYS AS ROW END,
    PERIOD FOR SYSTEM_TIME (valid_from, valid_to),
    FOREIGN KEY (request_id) REFERENCES calculation_requests(request_id)
) WITH (SYSTEM_VERSIONING = ON (HISTORY_TABLE = cash_flows_history));

-- Indexes for cash_flows table
CREATE INDEX idx_cash_flows_contract ON cash_flows(contract_id);
CREATE INDEX idx_cash_flows_date ON cash_flows(calculation_date);
CREATE INDEX idx_cash_flows_type ON cash_flows(cash_flow_type);
CREATE INDEX idx_cash_flows_state ON cash_flows(state);
CREATE INDEX idx_cash_flows_request ON cash_flows(request_id);

-- =====================================================
-- 3. SETTLEMENT INSTRUCTIONS - OUR SECONDARY OUTPUT
-- =====================================================

-- Settlement Instructions table (Temporal Table) - Settlement instructions
-- Note: This is our secondary output for settlement processing
-- Natural Key: (contract_id, cash_flow_id, settlement_date, settlement_type)
CREATE TABLE settlement_instructions (
    settlement_id VARCHAR(50) NOT NULL PRIMARY KEY,
    contract_id VARCHAR(50) NOT NULL, -- Natural key component
    cash_flow_id VARCHAR(50) NOT NULL, -- Natural key component
    settlement_date DATE NOT NULL, -- Natural key component
    settlement_type VARCHAR(20) NOT NULL, -- Natural key component
    counterparty VARCHAR(50) NOT NULL,
    amount DECIMAL(20,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT DEFAULT 0,
    last_retry_date DATETIME2,
    next_retry_date DATETIME2,
    error_message NVARCHAR(MAX),
    actual_settlement_date DATE,
    settlement_reference VARCHAR(100),
    notes NVARCHAR(500),
    cancelled_by VARCHAR(50),
    cancellation_reason NVARCHAR(500),
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    -- Temporal table columns
    valid_from DATETIME2 GENERATED ALWAYS AS ROW START,
    valid_to DATETIME2 GENERATED ALWAYS AS ROW END,
    PERIOD FOR SYSTEM_TIME (valid_from, valid_to),
    FOREIGN KEY (cash_flow_id) REFERENCES cash_flows(cash_flow_id),
    -- Natural key constraint
    CONSTRAINT uk_settlement_natural_key UNIQUE (contract_id, cash_flow_id, settlement_date, settlement_type)
) WITH (SYSTEM_VERSIONING = ON (HISTORY_TABLE = settlement_instructions_history));

-- Indexes for settlement_instructions table
CREATE INDEX idx_settlements_cash_flow ON settlement_instructions(cash_flow_id);
CREATE INDEX idx_settlements_date ON settlement_instructions(settlement_date);
CREATE INDEX idx_settlements_status ON settlement_instructions(status);
CREATE INDEX idx_settlements_counterparty ON settlement_instructions(counterparty);

-- =====================================================
-- 4. MARKET DATA CACHE - PERFORMANCE OPTIMIZATION
-- =====================================================

-- Market Data Cache table - Cache frequently accessed market data
-- Note: This is for performance optimization, not primary storage
CREATE TABLE market_data_cache (
    cache_key VARCHAR(100) NOT NULL PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    data_type VARCHAR(20) NOT NULL,
    data JSON NOT NULL, -- Native JSON data type in MS SQL Server
    valid_until DATETIME2 NOT NULL,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE()
);

-- Indexes for market_data_cache table
CREATE INDEX idx_market_cache_symbol ON market_data_cache(symbol);
CREATE INDEX idx_market_cache_type ON market_data_cache(data_type);
CREATE INDEX idx_market_cache_valid_until ON market_data_cache(valid_until);

-- =====================================================
-- 5. AUDIT AND MONITORING TABLES
-- =====================================================

-- Performance Metrics table - Track service performance
CREATE TABLE performance_metrics (
    metric_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    metric_name VARCHAR(50) NOT NULL,
    metric_value DECIMAL(10,2) NOT NULL,
    tags JSON, -- Native JSON data type in MS SQL Server
    recorded_at DATETIME2 DEFAULT GETDATE()
);

-- Indexes for performance_metrics table
CREATE INDEX idx_perf_metrics_name ON performance_metrics(metric_name);
CREATE INDEX idx_perf_metrics_date ON performance_metrics(recorded_at);

-- Maintenance Log table - Track maintenance operations
CREATE TABLE maintenance_log (
    log_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    operation VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    details JSON, -- Native JSON data type in MS SQL Server
    started_at DATETIME2 DEFAULT GETDATE(),
    completed_at DATETIME2,
    duration_ms INT
);

-- Indexes for maintenance_log table
CREATE INDEX idx_maintenance_operation ON maintenance_log(operation);
CREATE INDEX idx_maintenance_status ON maintenance_log(status);
CREATE INDEX idx_maintenance_date ON maintenance_log(started_at);

-- =====================================================
-- 6. CONFIGURATION TABLES
-- =====================================================

-- Configuration table - Service configuration
CREATE TABLE configuration (
    config_key VARCHAR(100) NOT NULL PRIMARY KEY,
    config_value NVARCHAR(MAX) NOT NULL,
    config_type VARCHAR(20) NOT NULL DEFAULT 'STRING',
    description NVARCHAR(500),
    is_active BIT DEFAULT 1,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE()
);

-- Indexes for configuration table
CREATE INDEX idx_config_active ON configuration(is_active);

-- =====================================================
-- 7. MATERIALIZED VIEWS FOR PERFORMANCE
-- =====================================================

-- Daily Cash Flow Summary view
CREATE VIEW v_daily_cash_flow_summary AS
SELECT 
    calculation_date,
    contract_id,
    cash_flow_type,
    currency,
    COUNT(*) as cash_flow_count,
    SUM(total_amount) as total_amount,
    SUM(equity_leg_amount) as total_equity_amount,
    SUM(interest_leg_amount) as total_interest_amount,
    SUM(equity_total_pnl) as total_pnl
FROM cash_flows
WHERE state IN ('REALIZED_SETTLED', 'REALIZED_UNSETTLED')
GROUP BY calculation_date, contract_id, cash_flow_type, currency;

-- Contract Cash Flow Summary view
CREATE VIEW v_contract_cash_flow_summary AS
SELECT 
    contract_id,
    COUNT(cash_flow_id) as total_cash_flows,
    SUM(total_amount) as total_amount,
    SUM(equity_total_pnl) as total_pnl,
    MAX(calculation_date) as last_calculation_date,
    currency
FROM cash_flows
GROUP BY contract_id, currency;

-- =====================================================
-- 8. STORED PROCEDURES
-- =====================================================

-- Procedure to archive old cash flows (MS SQL Server)
CREATE PROCEDURE archive_cash_flows
    @retention_months INT,
    @archival_location NVARCHAR(200)
AS
BEGIN
    DECLARE @archive_date DATE = DATEADD(MONTH, -@retention_months, GETDATE());
    DECLARE @archived_count INT = 0;
    
    -- Archive old cash flows
    INSERT INTO @archival_location
    SELECT cfh.* 
    FROM cash_flows_history cfh
    WHERE cfh.valid_to < @archive_date;
    
    SET @archived_count = @@ROWCOUNT;
    
    -- Log the archival operation
    INSERT INTO maintenance_log (operation, status, details)
    VALUES ('ARCHIVE_CASH_FLOWS', 'COMPLETED', 
            JSON_OBJECT(
                'retention_months', @retention_months, 
                'archived_date', @archive_date,
                'archived_count', @archived_count,
                'note', 'Archived old cash flow data based on retention policy.'
            ));
END;

-- Procedure to clean up expired market data cache
CREATE PROCEDURE cleanup_market_data_cache
AS
BEGIN
    DECLARE @deleted_count INT = 0;
    
    DELETE FROM market_data_cache 
    WHERE valid_until < GETDATE();
    
    SET @deleted_count = @@ROWCOUNT;
    
    -- Log the cleanup operation
    INSERT INTO maintenance_log (operation, status, details)
    VALUES ('CLEANUP_MARKET_DATA_CACHE', 'COMPLETED', 
            JSON_OBJECT(
                'deleted_count', @deleted_count,
                'cleanup_date', GETDATE()
            ));
END;

-- Procedure to get cash flows by contract and date range
CREATE PROCEDURE get_cash_flows_by_contract
    @contract_id VARCHAR(50),
    @from_date DATE,
    @to_date DATE,
    @cash_flow_type VARCHAR(30) = NULL,
    @state VARCHAR(20) = NULL
AS
BEGIN
    SELECT 
        cf.cash_flow_id,
        cf.request_id,
        cf.contract_id,
        cf.calculation_date,
        cf.cash_flow_type,
        cf.equity_leg_amount,
        cf.interest_leg_amount,
        cf.total_amount,
        cf.currency,
        cf.state,
        cf.equity_total_pnl,
        cf.equity_dividend_amount,
        cf.interest_accrued_amount,
        cf.created_at
    FROM cash_flows cf
    WHERE cf.contract_id = @contract_id
    AND cf.calculation_date BETWEEN @from_date AND @to_date
    AND (@cash_flow_type IS NULL OR cf.cash_flow_type = @cash_flow_type)
    AND (@state IS NULL OR cf.state = @state)
    ORDER BY cf.calculation_date;
END;

-- Procedure to reproduce calculation from audit trail
CREATE PROCEDURE reproduce_calculation
    @request_id VARCHAR(50)
AS
BEGIN
    -- Get the original calculation request
    SELECT 
        cr.request_id,
        cr.calculation_type,
        cr.from_date,
        cr.to_date,
        cr.input_data_hash,
        cr.input_data_snapshot,
        cr.status,
        cr.created_at
    FROM calculation_requests cr
    WHERE cr.request_id = @request_id;
    
    -- Get the cash flows generated from this request
    SELECT 
        cf.cash_flow_id,
        cf.contract_id,
        cf.calculation_date,
        cf.cash_flow_type,
        cf.total_amount,
        cf.currency,
        cf.state
    FROM cash_flows cf
    WHERE cf.request_id = @request_id
    ORDER BY cf.calculation_date;
END;

-- =====================================================
-- 9. INITIAL DATA
-- =====================================================

-- Insert default configuration
INSERT INTO configuration (config_key, config_value, config_type, description) VALUES
('calculation.max_contracts_per_request', '1000', 'INTEGER', 'Maximum number of contracts per calculation request'),
('calculation.max_date_range_days', '1825', 'INTEGER', 'Maximum date range in days (5 years)'),
('calculation.timeout_seconds', '300', 'INTEGER', 'Calculation timeout in seconds'),
('market_data.cache_ttl_hours', '24', 'INTEGER', 'Market data cache TTL in hours'),
('market_data.max_cache_size_mb', '1000', 'INTEGER', 'Maximum market data cache size in MB'),
('market_data.external_timeout_seconds', '10', 'INTEGER', 'External market data timeout in seconds'),
('archival.retention_months', '84', 'INTEGER', 'Data retention period in months (7 years)'),
('archival.frequency', 'MONTHLY', 'STRING', 'Archival frequency'),
('audit.input_data_snapshot_enabled', 'true', 'BOOLEAN', 'Enable full input data snapshot for debugging'),
('audit.input_data_hash_enabled', 'true', 'BOOLEAN', 'Enable input data hashing for integrity verification');

-- =====================================================
-- 10. COMMENTS AND DOCUMENTATION
-- =====================================================

-- This schema implements a pure calculation service approach for the Cash Flow Management Service
-- Key principles:
-- 1. Only store what we generate (cash flows, settlement instructions)
-- 2. Don't store any input data (contracts, positions, lots)
-- 3. Use temporal tables for automatic versioning and audit trails
-- 4. Implement performance optimizations (caching, indexing)
-- 5. Provide comprehensive monitoring and maintenance capabilities
-- 6. Store calculation metadata and input data hashing for regulatory compliance

-- Performance considerations:
-- 1. Zero storage overhead for input data - maximum calculation speed
-- 2. All tables have appropriate indexes for common query patterns
-- 3. Temporal tables provide efficient point-in-time queries
-- 4. JSON data type for flexible metadata storage
-- 5. Materialized views for aggregated data
-- 6. Stored procedures for common operations

-- Regulatory compliance:
-- 1. Temporal tables provide automatic audit trails
-- 2. Calculation requests table tracks all calculation activities
-- 3. Input data hashing for data integrity verification
-- 4. Optional input data snapshots for debugging and reproduction
-- 5. Maintenance log for operational transparency
-- 6. Performance metrics for service monitoring

-- Service boundaries:
-- 1. Contract data belongs to Contract Management Service
-- 2. Position data belongs to Position Management Service
-- 3. Trade/Lot data belongs to Trade Capture Service
-- 4. Market data belongs to Market Data Service
-- 5. We only own cash flow calculations and settlement instructions

-- Benefits of pure calculation approach:
-- 1. Maximum calculation performance (no storage overhead)
-- 2. Cleaner service boundaries (no data duplication)
-- 3. Simpler data model (easier to maintain)
-- 4. Better scalability (less database load)
-- 5. Regulatory compliance (audit trail through calculation requests)
