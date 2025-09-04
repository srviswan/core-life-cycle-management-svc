-- =====================================================
-- Cash Flow Management Service Database Migrations
-- =====================================================

-- Migration: V1.0.0 - Minimal Storage Schema
-- Date: 2024-01-15
-- Description: Create minimal database schema for Cash Flow Management Service
-- Technology: MS SQL Server with Temporal Tables for automatic versioning
-- Architecture: Minimal Storage - Only store what the service owns

-- =====================================================
-- ARCHITECTURE PRINCIPLES
-- =====================================================
-- 1. Single Responsibility: Cash Flow Service only owns cash flow calculations
-- 2. Minimal Storage: Don't store contracts, positions, lots (belong to other services)
-- 3. Performance First: Minimize storage overhead for faster calculations
-- 4. Clear Boundaries: Each service owns its data
-- 5. Regulatory Compliance: Store calculation metadata for audit trails

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
-- 1. CONTRACT REFERENCES - MINIMAL CONTRACT DATA
-- =====================================================

-- Contract References table (Temporal Table) - Minimal contract data for calculations
-- Note: We only store essential contract data needed for cash flow calculations
-- Full contract data belongs to Contract Management Service
CREATE TABLE contract_references (
    contract_id VARCHAR(50) NOT NULL PRIMARY KEY,
    underlying VARCHAR(20) NOT NULL,
    notional_amount DECIMAL(20,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    interest_rate_type VARCHAR(20) NOT NULL,
    interest_index VARCHAR(20) NOT NULL,
    interest_spread DECIMAL(10,6) NOT NULL,
    interest_reset_frequency VARCHAR(20) NOT NULL,
    interest_day_count_convention VARCHAR(20) NOT NULL,
    interest_currency VARCHAR(3) NOT NULL,
    interest_notional_amount DECIMAL(20,2) NOT NULL,
    equity_dividend_treatment VARCHAR(20) NOT NULL DEFAULT 'REINVEST',
    equity_corporate_action_handling VARCHAR(20) NOT NULL DEFAULT 'AUTOMATIC',
    equity_currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    -- Temporal table columns
    valid_from DATETIME2 GENERATED ALWAYS AS ROW START,
    valid_to DATETIME2 GENERATED ALWAYS AS ROW END,
    PERIOD FOR SYSTEM_TIME (valid_from, valid_to)
) WITH (SYSTEM_VERSIONING = ON (HISTORY_TABLE = contract_references_history));

-- Indexes for contract_references table
CREATE INDEX idx_contract_refs_underlying ON contract_references(underlying);
CREATE INDEX idx_contract_refs_status ON contract_references(status);
CREATE INDEX idx_contract_refs_interest_index ON contract_references(interest_index);

-- =====================================================
-- 2. CASH FLOW TABLES - OUR PRIMARY OUTPUT
-- =====================================================

-- Cash Flows table (Temporal Table) - Our primary output
-- Note: This is what our service actually owns and generates
CREATE TABLE cash_flows (
    cash_flow_id VARCHAR(50) NOT NULL PRIMARY KEY,
    request_id VARCHAR(50) NOT NULL,
    contract_id VARCHAR(50) NOT NULL,
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
    FOREIGN KEY (contract_id) REFERENCES contract_references(contract_id)
) WITH (SYSTEM_VERSIONING = ON (HISTORY_TABLE = cash_flows_history));

-- Indexes for cash_flows table
CREATE INDEX idx_cash_flows_contract ON cash_flows(contract_id);
CREATE INDEX idx_cash_flows_date ON cash_flows(calculation_date);
CREATE INDEX idx_cash_flows_type ON cash_flows(cash_flow_type);
CREATE INDEX idx_cash_flows_state ON cash_flows(state);
CREATE INDEX idx_cash_flows_request ON cash_flows(request_id);

-- =====================================================
-- 3. CALCULATION METADATA - AUDIT TRAIL
-- =====================================================

-- Calculation Requests table (Temporal Table) - Track calculation requests
-- Note: This provides audit trail for regulatory compliance
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

-- =====================================================
-- 4. SETTLEMENT TABLES - OUR SECONDARY OUTPUT
-- =====================================================

-- Settlement Instructions table (Temporal Table) - Settlement instructions
-- Note: This is our secondary output for settlement processing
CREATE TABLE settlement_instructions (
    settlement_id VARCHAR(50) NOT NULL PRIMARY KEY,
    cash_flow_id VARCHAR(50) NOT NULL,
    counterparty VARCHAR(50) NOT NULL,
    settlement_date DATE NOT NULL,
    amount DECIMAL(20,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    settlement_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT DEFAULT 0,
    last_retry_date DATETIME2,
    error_message NVARCHAR(MAX),
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    -- Temporal table columns
    valid_from DATETIME2 GENERATED ALWAYS AS ROW START,
    valid_to DATETIME2 GENERATED ALWAYS AS ROW END,
    PERIOD FOR SYSTEM_TIME (valid_from, valid_to),
    FOREIGN KEY (cash_flow_id) REFERENCES cash_flows(cash_flow_id)
) WITH (SYSTEM_VERSIONING = ON (HISTORY_TABLE = settlement_instructions_history));

-- Indexes for settlement_instructions table
CREATE INDEX idx_settlements_cash_flow ON settlement_instructions(cash_flow_id);
CREATE INDEX idx_settlements_date ON settlement_instructions(settlement_date);
CREATE INDEX idx_settlements_status ON settlement_instructions(status);
CREATE INDEX idx_settlements_counterparty ON settlement_instructions(counterparty);

-- =====================================================
-- 5. MARKET DATA CACHE - PERFORMANCE OPTIMIZATION
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
-- 6. AUDIT AND MONITORING TABLES
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
-- 7. CONFIGURATION TABLES
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
-- 8. MATERIALIZED VIEWS FOR PERFORMANCE
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
    cr.contract_id,
    cr.underlying,
    cr.currency,
    COUNT(cf.cash_flow_id) as total_cash_flows,
    SUM(cf.total_amount) as total_amount,
    SUM(cf.equity_total_pnl) as total_pnl,
    MAX(cf.calculation_date) as last_calculation_date
FROM contract_references cr
LEFT JOIN cash_flows cf ON cr.contract_id = cf.contract_id
WHERE cr.status = 'ACTIVE'
GROUP BY cr.contract_id, cr.underlying, cr.currency;

-- =====================================================
-- 9. STORED PROCEDURES
-- =====================================================

-- Procedure to archive old cash flows (MS SQL Server - with active contract preservation)
CREATE PROCEDURE archive_cash_flows
    @retention_months INT,
    @archival_location NVARCHAR(200)
AS
BEGIN
    DECLARE @archive_date DATE = DATEADD(MONTH, -@retention_months, GETDATE());
    DECLARE @archived_count INT = 0;
    DECLARE @preserved_count INT = 0;
    
    -- Only archive data for closed/terminated contracts, preserves active contract history
    INSERT INTO @archival_location
    SELECT cfh.* 
    FROM cash_flows_history cfh
    JOIN contract_references_history crh ON cfh.contract_id = crh.contract_id
    WHERE cfh.valid_to < @archive_date
    AND crh.status IN ('CLOSED', 'TERMINATED', 'EXPIRED')
    AND crh.valid_to < @archive_date;
    
    SET @archived_count = @@ROWCOUNT;
    
    -- Count preserved records for active contracts
    SELECT @preserved_count = COUNT(*)
    FROM cash_flows_history cfh
    JOIN contract_references cr ON cfh.contract_id = cr.contract_id
    WHERE cfh.valid_to < @archive_date
    AND cr.status = 'ACTIVE';
    
    -- Log the archival operation
    INSERT INTO maintenance_log (operation, status, details)
    VALUES ('ARCHIVE_CASH_FLOWS', 'COMPLETED', 
            JSON_OBJECT(
                'retention_months', @retention_months, 
                'archived_date', @archive_date,
                'archived_count', @archived_count,
                'preserved_count', @preserved_count,
                'note', 'Only archived data for closed/terminated contracts. Active contract history preserved.'
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

-- =====================================================
-- 10. INITIAL DATA
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
('archival.frequency', 'MONTHLY', 'STRING', 'Archival frequency');

-- =====================================================
-- 11. COMMENTS AND DOCUMENTATION
-- =====================================================

-- This schema implements a minimal storage approach for the Cash Flow Management Service
-- Key principles:
-- 1. Only store what the service owns (cash flows, settlement instructions)
-- 2. Store minimal contract references for calculations
-- 3. Use temporal tables for automatic versioning and audit trails
-- 4. Implement performance optimizations (caching, indexing)
-- 5. Provide comprehensive monitoring and maintenance capabilities

-- Performance considerations:
-- 1. All tables have appropriate indexes for common query patterns
-- 2. Temporal tables provide efficient point-in-time queries
-- 3. JSON data type for flexible metadata storage
-- 4. Materialized views for aggregated data
-- 5. Stored procedures for common operations

-- Regulatory compliance:
-- 1. Temporal tables provide automatic audit trails
-- 2. Calculation requests table tracks all calculation activities
-- 3. Input data hashing for data integrity verification
-- 4. Maintenance log for operational transparency
-- 5. Performance metrics for service monitoring
