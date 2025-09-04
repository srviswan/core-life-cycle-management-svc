-- =====================================================
-- Cash Flow Management Service Database Migrations
-- =====================================================

-- Migration: V1.0.0 - Initial Schema Creation
-- Date: 2024-01-15
-- Description: Create initial database schema for Cash Flow Management Service

-- =====================================================
-- 1. CORE TABLES
-- =====================================================

-- Contracts table (Temporal Table)
CREATE TABLE contracts (
    contract_id VARCHAR(50) NOT NULL PRIMARY KEY,
    basket_contract_id VARCHAR(50),
    underlying VARCHAR(20) NOT NULL,
    trade_date DATE NOT NULL,
    effective_date DATE NOT NULL,
    maturity_date DATE NOT NULL,
    notional_amount DECIMAL(20,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    weight DECIMAL(5,4),
    buyer VARCHAR(50) NOT NULL,
    seller VARCHAR(50) NOT NULL,
    equity_underlying VARCHAR(20) NOT NULL,
    equity_quantity DECIMAL(15,2) NOT NULL,
    equity_dividend_treatment VARCHAR(20) NOT NULL DEFAULT 'REINVEST',
    equity_corporate_action_handling VARCHAR(20) NOT NULL DEFAULT 'AUTOMATIC',
    equity_currency VARCHAR(3) NOT NULL,
    interest_rate_type VARCHAR(20) NOT NULL,
    interest_index VARCHAR(20) NOT NULL,
    interest_spread DECIMAL(10,6) NOT NULL,
    interest_reset_frequency VARCHAR(20) NOT NULL,
    interest_day_count_convention VARCHAR(20) NOT NULL,
    interest_currency VARCHAR(3) NOT NULL,
    interest_notional_amount DECIMAL(20,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    -- Temporal table columns
    valid_from DATETIME2 GENERATED ALWAYS AS ROW START,
    valid_to DATETIME2 GENERATED ALWAYS AS ROW END,
    PERIOD FOR SYSTEM_TIME (valid_from, valid_to)
) WITH (SYSTEM_VERSIONING = ON (HISTORY_TABLE = contracts_history));

-- Indexes for contracts table
CREATE INDEX idx_contracts_basket ON contracts(basket_contract_id);
CREATE INDEX idx_contracts_underlying ON contracts(underlying);
CREATE INDEX idx_contracts_dates ON contracts(trade_date, effective_date, maturity_date);
CREATE INDEX idx_contracts_status ON contracts(status);

-- Positions table (Temporal Table)
CREATE TABLE positions (
    position_id VARCHAR(50) NOT NULL PRIMARY KEY,
    contract_id VARCHAR(50) NOT NULL,
    underlying VARCHAR(20) NOT NULL,
    total_quantity DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    -- Temporal table columns
    valid_from DATETIME2 GENERATED ALWAYS AS ROW START,
    valid_to DATETIME2 GENERATED ALWAYS AS ROW END,
    PERIOD FOR SYSTEM_TIME (valid_from, valid_to),
    FOREIGN KEY (contract_id) REFERENCES contracts(contract_id)
) WITH (SYSTEM_VERSIONING = ON (HISTORY_TABLE = positions_history));

-- Indexes for positions table
CREATE INDEX idx_positions_contract ON positions(contract_id);
CREATE INDEX idx_positions_underlying ON positions(underlying);
CREATE INDEX idx_positions_status ON positions(status);

-- Lots table (Temporal Table with partitioning)
CREATE TABLE lots (
    lot_id VARCHAR(50) NOT NULL,
    contract_id VARCHAR(50) NOT NULL,
    position_id VARCHAR(50) NOT NULL,
    underlying VARCHAR(20) NOT NULL,
    quantity DECIMAL(15,2) NOT NULL,
    cost_price DECIMAL(15,4) NOT NULL,
    cost_date DATE NOT NULL,
    lot_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    unwinding_method VARCHAR(20) NOT NULL DEFAULT 'LIFO',
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    -- Temporal table columns
    valid_from DATETIME2 GENERATED ALWAYS AS ROW START,
    valid_to DATETIME2 GENERATED ALWAYS AS ROW END,
    PERIOD FOR SYSTEM_TIME (valid_from, valid_to),
    PRIMARY KEY (lot_id, cost_date),
    FOREIGN KEY (contract_id) REFERENCES contracts(contract_id),
    FOREIGN KEY (position_id) REFERENCES positions(position_id)
) WITH (SYSTEM_VERSIONING = ON (HISTORY_TABLE = lots_history));

-- Note: MS SQL Server handles partitioning differently than MySQL
-- For MS SQL Server, we'll use table partitioning with filegroups
-- This will be configured separately based on the specific MS SQL Server setup

-- Indexes for lots table
CREATE INDEX idx_lots_contract_cost_date ON lots(contract_id, cost_date);
CREATE INDEX idx_lots_position_cost_date ON lots(position_id, cost_date);
CREATE INDEX idx_lots_underlying_cost_date ON lots(underlying, cost_date);
CREATE INDEX idx_lots_status_cost_date ON lots(status, cost_date);

-- Payment schedules table (Temporal Table)
CREATE TABLE payment_schedules (
    schedule_id VARCHAR(50) NOT NULL PRIMARY KEY,
    contract_id VARCHAR(50) NOT NULL,
    schedule_type VARCHAR(30) NOT NULL,
    scheduled_date DATE NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    leg VARCHAR(20) NOT NULL,
    notional_amount DECIMAL(20,2) NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    effective_from DATE NOT NULL,
    effective_to DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    -- Temporal table columns
    valid_from DATETIME2 GENERATED ALWAYS AS ROW START,
    valid_to DATETIME2 GENERATED ALWAYS AS ROW END,
    PERIOD FOR SYSTEM_TIME (valid_from, valid_to),
    FOREIGN KEY (contract_id) REFERENCES contracts(contract_id)
) WITH (SYSTEM_VERSIONING = ON (HISTORY_TABLE = payment_schedules_history));

-- Indexes for payment schedules table
CREATE INDEX idx_schedules_contract_date ON payment_schedules(contract_id, scheduled_date);
CREATE INDEX idx_schedules_type_date ON payment_schedules(schedule_type, scheduled_date);
CREATE INDEX idx_schedules_effective_dates ON payment_schedules(effective_from, effective_to);
CREATE INDEX idx_schedules_status ON payment_schedules(status);

-- =====================================================
-- 2. CASH FLOW TABLES
-- =====================================================

-- Cash flows table (Temporal Table with partitioning)
CREATE TABLE cash_flows (
    cash_flow_id VARCHAR(50) NOT NULL,
    request_id VARCHAR(50) NOT NULL,
    contract_id VARCHAR(50) NOT NULL,
    position_id VARCHAR(50) NOT NULL,
    lot_id VARCHAR(50) NOT NULL,
    schedule_id VARCHAR(50),
    calculation_date DATE NOT NULL,
    cash_flow_type VARCHAR(30) NOT NULL,
    equity_leg_amount DECIMAL(20,2),
    interest_leg_amount DECIMAL(20,2),
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
    PRIMARY KEY (cash_flow_id, calculation_date),
    FOREIGN KEY (contract_id) REFERENCES contracts(contract_id),
    FOREIGN KEY (position_id) REFERENCES positions(position_id),
    FOREIGN KEY (schedule_id) REFERENCES payment_schedules(schedule_id)
) WITH (SYSTEM_VERSIONING = ON (HISTORY_TABLE = cash_flows_history));

-- Note: MS SQL Server table partitioning will be configured separately
-- using filegroups and partition functions based on the specific setup

-- Indexes for cash_flows table
CREATE INDEX idx_cash_flows_request_date ON cash_flows(request_id, calculation_date);
CREATE INDEX idx_cash_flows_contract_date ON cash_flows(contract_id, calculation_date);
CREATE INDEX idx_cash_flows_lot_date ON cash_flows(lot_id, calculation_date);
CREATE INDEX idx_cash_flows_type_date ON cash_flows(cash_flow_type, calculation_date);
CREATE INDEX idx_cash_flows_state_date ON cash_flows(state, calculation_date);
CREATE INDEX idx_cash_flows_currency_date ON cash_flows(currency, calculation_date);

-- Calculation requests table (Temporal Table)
CREATE TABLE calculation_requests (
    request_id VARCHAR(50) NOT NULL PRIMARY KEY,
    calculation_type VARCHAR(30) NOT NULL,
    date_range_from DATE NOT NULL,
    date_range_to DATE NOT NULL,
    calculation_frequency VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_contracts INTEGER,
    processed_contracts INTEGER DEFAULT 0,
    total_positions INTEGER,
    processed_positions INTEGER DEFAULT 0,
    total_lots INTEGER,
    processed_lots INTEGER DEFAULT 0,
    processing_time_ms BIGINT,
    memory_usage_mb INTEGER,
    cache_hit_rate DECIMAL(5,4),
    data_source VARCHAR(20),
    error_message NVARCHAR(MAX),
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    -- Temporal table columns
    valid_from DATETIME2 GENERATED ALWAYS AS ROW START,
    valid_to DATETIME2 GENERATED ALWAYS AS ROW END,
    PERIOD FOR SYSTEM_TIME (valid_from, valid_to)
) WITH (SYSTEM_VERSIONING = ON (HISTORY_TABLE = calculation_requests_history));

-- Indexes for calculation requests table
CREATE INDEX idx_requests_type_date ON calculation_requests(calculation_type, date_range_from, date_range_to);
CREATE INDEX idx_requests_status ON calculation_requests(status);
CREATE INDEX idx_requests_created ON calculation_requests(created_at);

-- =====================================================
-- 3. SETTLEMENT TABLES
-- =====================================================

-- Settlement instructions table (Temporal Table)
CREATE TABLE settlement_instructions (
    instruction_id VARCHAR(50) NOT NULL PRIMARY KEY,
    cash_flow_id VARCHAR(50) NOT NULL,
    contract_id VARCHAR(50) NOT NULL,
    lot_id VARCHAR(50) NOT NULL,
    cash_flow_type VARCHAR(30) NOT NULL,
    amount DECIMAL(20,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    counterparty VARCHAR(50) NOT NULL,
    settlement_date DATE NOT NULL,
    settlement_method VARCHAR(20) NOT NULL DEFAULT 'CASH',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    failure_reason NVARCHAR(MAX),
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    -- Temporal table columns
    valid_from DATETIME2 GENERATED ALWAYS AS ROW START,
    valid_to DATETIME2 GENERATED ALWAYS AS ROW END,
    PERIOD FOR SYSTEM_TIME (valid_from, valid_to),
    FOREIGN KEY (contract_id) REFERENCES contracts(contract_id)
) WITH (SYSTEM_VERSIONING = ON (HISTORY_TABLE = settlement_instructions_history));

-- Indexes for settlement instructions table
CREATE INDEX idx_settlements_cash_flow ON settlement_instructions(cash_flow_id);
CREATE INDEX idx_settlements_contract ON settlement_instructions(contract_id);
CREATE INDEX idx_settlements_counterparty ON settlement_instructions(counterparty);
CREATE INDEX idx_settlements_date ON settlement_instructions(settlement_date);
CREATE INDEX idx_settlements_status ON settlement_instructions(status);
CREATE INDEX idx_settlements_currency ON settlement_instructions(currency);

-- =====================================================
-- 4. MARKET DATA TABLES
-- =====================================================

-- Market data cache table (Temporal Table with partitioning)
CREATE TABLE market_data_cache (
    cache_key VARCHAR(100) NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    data_type VARCHAR(20) NOT NULL,
    data_date DATE NOT NULL,
    data_value DECIMAL(20,6) NOT NULL,
    currency VARCHAR(3),
    source VARCHAR(50),
    expires_at DATETIME2 NOT NULL,
    created_at DATETIME2 DEFAULT GETDATE(),
    -- Temporal table columns
    valid_from DATETIME2 GENERATED ALWAYS AS ROW START,
    valid_to DATETIME2 GENERATED ALWAYS AS ROW END,
    PERIOD FOR SYSTEM_TIME (valid_from, valid_to),
    PRIMARY KEY (cache_key, data_date)
) WITH (SYSTEM_VERSIONING = ON (HISTORY_TABLE = market_data_cache_history));

-- Indexes for market data cache table
CREATE INDEX idx_market_data_symbol_date ON market_data_cache(symbol, data_date);
CREATE INDEX idx_market_data_type_date ON market_data_cache(data_type, data_date);
CREATE INDEX idx_market_data_expires ON market_data_cache(expires_at);

-- Note: MS SQL Server table partitioning will be configured separately
-- using filegroups and partition functions based on the specific setup

-- =====================================================
-- 5. AUDIT AND MONITORING TABLES
-- =====================================================

-- Note: Using MS SQL Server Temporal Tables for automatic versioning
-- This provides better performance and automatic audit trails
-- No separate audit_trail table needed - temporal tables handle this automatically

-- Performance metrics table
CREATE TABLE performance_metrics (
    metric_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DECIMAL(20,6) NOT NULL,
    metric_unit VARCHAR(20),
    tags NVARCHAR(MAX), -- JSON stored as NVARCHAR(MAX)
    recorded_at DATETIME2 DEFAULT GETDATE()
);

-- Indexes for performance metrics table
CREATE INDEX idx_metrics_name_time ON performance_metrics(metric_name, recorded_at);
CREATE INDEX idx_metrics_recorded ON performance_metrics(recorded_at);

-- Maintenance log table
CREATE TABLE maintenance_log (
    log_id BIGINT IDENTITY(1,1) PRIMARY KEY,
    operation VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    details NVARCHAR(MAX), -- JSON stored as NVARCHAR(MAX)
    started_at DATETIME2 DEFAULT GETDATE(),
    completed_at DATETIME2,
    duration_ms BIGINT
);

-- Indexes for maintenance log table
CREATE INDEX idx_maintenance_operation ON maintenance_log(operation);
CREATE INDEX idx_maintenance_status ON maintenance_log(status);
CREATE INDEX idx_maintenance_started ON maintenance_log(started_at);

-- =====================================================
-- 6. CONFIGURATION TABLES
-- =====================================================

-- Archival configuration table
CREATE TABLE archival_config (
    config_id INT IDENTITY(1,1) PRIMARY KEY,
    table_name VARCHAR(50) NOT NULL,
    retention_months INTEGER NOT NULL,
    archival_frequency VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    archival_date DATE,
    compression_enabled BIT NOT NULL DEFAULT 1,
    archival_location VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT uk_archival_table UNIQUE (table_name)
);

-- Indexes for archival configuration table
CREATE INDEX idx_archival_status ON archival_config(status);

-- Backup configuration table
CREATE TABLE backup_config (
    config_id INT IDENTITY(1,1) PRIMARY KEY,
    backup_type VARCHAR(20) NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    retention_days INTEGER NOT NULL,
    backup_location VARCHAR(200) NOT NULL,
    compression_enabled BIT NOT NULL DEFAULT 1,
    encryption_enabled BIT NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE()
);

-- Indexes for backup configuration table
CREATE INDEX idx_backup_type ON backup_config(backup_type);
CREATE INDEX idx_backup_status ON backup_config(status);

-- =====================================================
-- 7. MATERIALIZED VIEWS
-- =====================================================

-- Daily cash flow summary view
CREATE TABLE daily_cash_flow_summary (
    summary_date DATE NOT NULL,
    contract_id VARCHAR(50) NOT NULL,
    underlying VARCHAR(20) NOT NULL,
    total_cash_flows INTEGER NOT NULL,
    total_amount DECIMAL(20,2) NOT NULL,
    total_interest DECIMAL(20,2) NOT NULL,
    total_dividends DECIMAL(20,2) NOT NULL,
    total_pnl DECIMAL(20,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    last_updated DATETIME2 DEFAULT GETDATE(),
    PRIMARY KEY (summary_date, contract_id)
);

-- Indexes for daily cash flow summary table
CREATE INDEX idx_summary_date ON daily_cash_flow_summary(summary_date);
CREATE INDEX idx_summary_underlying ON daily_cash_flow_summary(underlying);
CREATE INDEX idx_summary_contract ON daily_cash_flow_summary(contract_id);

-- =====================================================
-- 8. STORED PROCEDURES
-- =====================================================

-- Procedure to create new partitions automatically (MS SQL Server)
CREATE PROCEDURE create_partition_if_not_exists
    @table_name NVARCHAR(100),
    @partition_name NVARCHAR(100),
    @partition_range NVARCHAR(200)
AS
BEGIN
    DECLARE @partition_exists INT = 0;
    
    SELECT @partition_exists = COUNT(*)
    FROM sys.partitions p
    JOIN sys.objects o ON p.object_id = o.object_id
    WHERE o.name = @table_name
    AND p.partition_number > 1;
    
    IF @partition_exists = 0
    BEGIN
        -- MS SQL Server partitioning logic would go here
        -- This is a simplified version - actual implementation depends on partition scheme
        PRINT 'Partition creation logic for MS SQL Server';
    END
END;

-- Procedure to archive old cash flows (MS SQL Server)
CREATE PROCEDURE archive_cash_flows
    @retention_months INT,
    @archival_location NVARCHAR(200)
AS
BEGIN
    DECLARE @archive_date DATE = DATEADD(MONTH, -@retention_months, GETDATE());
    
    -- Archive old cash flows using temporal table history
    INSERT INTO @archival_location
    SELECT * FROM cash_flows_history
    WHERE valid_to < @archive_date;
    
    -- Log archival
    INSERT INTO maintenance_log (operation, status, details)
    VALUES ('ARCHIVE_CASH_FLOWS', 'COMPLETED', 
            JSON_VALUE('{"retention_months": "' + CAST(@retention_months AS NVARCHAR(10)) + '", "archived_date": "' + CAST(@archive_date AS NVARCHAR(10)) + '"}', '$.retention_months'));
END;

-- Procedure to refresh materialized views (MS SQL Server)
CREATE PROCEDURE refresh_daily_cash_flow_summary
AS
BEGIN
    -- Clear existing summary data
    DELETE FROM daily_cash_flow_summary;
    
    -- Refresh summary data
    INSERT INTO daily_cash_flow_summary (
        summary_date,
        contract_id,
        underlying,
        total_cash_flows,
        total_amount,
        total_interest,
        total_dividends,
        total_pnl,
        currency
    )
    SELECT 
        calculation_date,
        contract_id,
        underlying,
        COUNT(*) as total_cash_flows,
        SUM(total_amount) as total_amount,
        SUM(interest_leg_amount) as total_interest,
        SUM(equity_dividend_amount) as total_dividends,
        SUM(equity_total_pnl) as total_pnl,
        currency
    FROM cash_flows cf
    JOIN contracts c ON cf.contract_id = c.contract_id
    GROUP BY calculation_date, contract_id, underlying, currency;
    
    -- Log refresh
    INSERT INTO maintenance_log (operation, status, details)
    VALUES ('REFRESH_SUMMARY', 'COMPLETED', '{"view": "daily_cash_flow_summary"}');
END;

-- Procedure to cleanup expired market data (MS SQL Server)
CREATE PROCEDURE cleanup_expired_market_data
AS
BEGIN
    DECLARE @deleted_count INT;
    
    DELETE FROM market_data_cache 
    WHERE expires_at < GETDATE();
    
    SET @deleted_count = @@ROWCOUNT;
    
    -- Log cleanup
    INSERT INTO maintenance_log (operation, status, details)
    VALUES ('CLEANUP_MARKET_DATA', 'COMPLETED', '{"deleted_count": ' + CAST(@deleted_count AS NVARCHAR(10)) + '}');
END;

-- =====================================================
-- 9. TRIGGERS
-- =====================================================

-- Trigger to update audit trail for contracts
DELIMITER //
CREATE TRIGGER contracts_audit_trigger
AFTER UPDATE ON contracts
FOR EACH ROW
BEGIN
    INSERT INTO audit_trail (table_name, record_id, operation, old_values, new_values)
    VALUES ('contracts', NEW.contract_id, 'UPDATE', 
            JSON_OBJECT('notional_amount', OLD.notional_amount, 'status', OLD.status),
            JSON_OBJECT('notional_amount', NEW.notional_amount, 'status', NEW.status));
END //
DELIMITER ;

-- Trigger to update audit trail for cash flows
DELIMITER //
CREATE TRIGGER cash_flows_audit_trigger
AFTER INSERT ON cash_flows
FOR EACH ROW
BEGIN
    INSERT INTO audit_trail (table_name, record_id, operation, new_values)
    VALUES ('cash_flows', NEW.cash_flow_id, 'INSERT',
            JSON_OBJECT('amount', NEW.total_amount, 'type', NEW.cash_flow_type, 'date', NEW.calculation_date));
END //
DELIMITER ;

-- =====================================================
-- 10. INITIAL DATA
-- =====================================================

-- Insert default archival configuration
INSERT INTO archival_config (table_name, retention_months, archival_frequency, archival_location) VALUES
('cash_flows', 84, 'MONTHLY', '/archive/cash_flows'),
('lots', 84, 'MONTHLY', '/archive/lots'),
('market_data_cache', 12, 'MONTHLY', '/archive/market_data'),
('audit_trail', 120, 'MONTHLY', '/archive/audit_trail');

-- Insert default backup configuration
INSERT INTO backup_config (backup_type, frequency, retention_days, backup_location, compression_enabled, encryption_enabled) VALUES
('FULL', 'DAILY', 30, '/backups/full', TRUE, TRUE),
('INCREMENTAL', 'HOURLY', 7, '/backups/incremental', TRUE, TRUE),
('ARCHIVE', 'WEEKLY', 365, '/backups/archive', TRUE, TRUE);

-- =====================================================
-- 11. INDEXES FOR PERFORMANCE
-- =====================================================

-- Composite indexes for common query patterns
CREATE INDEX idx_cash_flows_composite_1 ON cash_flows(contract_id, calculation_date, cash_flow_type);
CREATE INDEX idx_cash_flows_composite_2 ON cash_flows(lot_id, calculation_date, state);
CREATE INDEX idx_cash_flows_composite_3 ON cash_flows(currency, calculation_date, total_amount);

-- Partial indexes for active records
CREATE INDEX idx_contracts_active ON contracts(contract_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_positions_active ON positions(position_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_lots_active ON lots(lot_id) WHERE status = 'ACTIVE';

-- Covering indexes for common queries
CREATE INDEX idx_cash_flows_covering ON cash_flows(contract_id, calculation_date) 
INCLUDE (total_amount, currency, cash_flow_type, state);

-- =====================================================
-- 12. CONSTRAINTS
-- =====================================================

-- Add check constraints
ALTER TABLE cash_flows 
ADD CONSTRAINT chk_cash_flows_amount_positive 
CHECK (total_amount >= 0);

ALTER TABLE lots 
ADD CONSTRAINT chk_lots_quantity_positive 
CHECK (quantity > 0);

ALTER TABLE contracts 
ADD CONSTRAINT chk_contracts_notional_positive 
CHECK (notional_amount > 0);

ALTER TABLE payment_schedules 
ADD CONSTRAINT chk_schedules_effective_dates 
CHECK (effective_from <= effective_to);

-- =====================================================
-- Migration Complete
-- =====================================================

-- Verify schema creation
SELECT 'Migration V1.0.0 completed successfully' as status;
