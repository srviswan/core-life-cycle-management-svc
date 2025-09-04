-- =====================================================
-- Cash Flow Management Service Database Migrations
-- =====================================================

-- Migration: V1.0.0 - Initial Schema Creation
-- Date: 2024-01-15
-- Description: Create initial database schema for Cash Flow Management Service

-- =====================================================
-- 1. CORE TABLES
-- =====================================================

-- Contracts table
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_contracts_basket (basket_contract_id),
    INDEX idx_contracts_underlying (underlying),
    INDEX idx_contracts_dates (trade_date, effective_date, maturity_date),
    INDEX idx_contracts_status (status)
);

-- Positions table
CREATE TABLE positions (
    position_id VARCHAR(50) NOT NULL PRIMARY KEY,
    contract_id VARCHAR(50) NOT NULL,
    underlying VARCHAR(20) NOT NULL,
    total_quantity DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (contract_id) REFERENCES contracts(contract_id),
    INDEX idx_positions_contract (contract_id),
    INDEX idx_positions_underlying (underlying),
    INDEX idx_positions_status (status)
);

-- Lots table (partitioned by cost_date for performance)
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (lot_id, cost_date),
    FOREIGN KEY (contract_id) REFERENCES contracts(contract_id),
    FOREIGN KEY (position_id) REFERENCES positions(position_id)
) PARTITION BY RANGE (YEAR(cost_date));

-- Create partitions for lots table (2020-2030)
CREATE TABLE lots_2020 PARTITION OF lots FOR VALUES FROM ('2020-01-01') TO ('2021-01-01');
CREATE TABLE lots_2021 PARTITION OF lots FOR VALUES FROM ('2021-01-01') TO ('2022-01-01');
CREATE TABLE lots_2022 PARTITION OF lots FOR VALUES FROM ('2022-01-01') TO ('2023-01-01');
CREATE TABLE lots_2023 PARTITION OF lots FOR VALUES FROM ('2023-01-01') TO ('2024-01-01');
CREATE TABLE lots_2024 PARTITION OF lots FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');
CREATE TABLE lots_2025 PARTITION OF lots FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
CREATE TABLE lots_2026 PARTITION OF lots FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');
CREATE TABLE lots_2027 PARTITION OF lots FOR VALUES FROM ('2027-01-01') TO ('2028-01-01');
CREATE TABLE lots_2028 PARTITION OF lots FOR VALUES FROM ('2028-01-01') TO ('2029-01-01');
CREATE TABLE lots_2029 PARTITION OF lots FOR VALUES FROM ('2029-01-01') TO ('2030-01-01');
CREATE TABLE lots_2030 PARTITION OF lots FOR VALUES FROM ('2030-01-01') TO ('2031-01-01');

-- Indexes for lots table
CREATE INDEX idx_lots_contract_cost_date ON lots(contract_id, cost_date);
CREATE INDEX idx_lots_position_cost_date ON lots(position_id, cost_date);
CREATE INDEX idx_lots_underlying_cost_date ON lots(underlying, cost_date);
CREATE INDEX idx_lots_status_cost_date ON lots(status, cost_date);

-- Payment schedules table
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (contract_id) REFERENCES contracts(contract_id),
    INDEX idx_schedules_contract_date (contract_id, scheduled_date),
    INDEX idx_schedules_type_date (schedule_type, scheduled_date),
    INDEX idx_schedules_effective_dates (effective_from, effective_to),
    INDEX idx_schedules_status (status)
);

-- =====================================================
-- 2. CASH FLOW TABLES
-- =====================================================

-- Cash flows table (partitioned by calculation_date)
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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (cash_flow_id, calculation_date),
    FOREIGN KEY (contract_id) REFERENCES contracts(contract_id),
    FOREIGN KEY (position_id) REFERENCES positions(position_id),
    FOREIGN KEY (schedule_id) REFERENCES payment_schedules(schedule_id)
) PARTITION BY RANGE (calculation_date);

-- Create partitions for cash_flows table (2020-2030)
CREATE TABLE cash_flows_2020 PARTITION OF cash_flows FOR VALUES FROM ('2020-01-01') TO ('2021-01-01');
CREATE TABLE cash_flows_2021 PARTITION OF cash_flows FOR VALUES FROM ('2021-01-01') TO ('2022-01-01');
CREATE TABLE cash_flows_2022 PARTITION OF cash_flows FOR VALUES FROM ('2022-01-01') TO ('2023-01-01');
CREATE TABLE cash_flows_2023 PARTITION OF cash_flows FOR VALUES FROM ('2023-01-01') TO ('2024-01-01');
CREATE TABLE cash_flows_2024 PARTITION OF cash_flows FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');
CREATE TABLE cash_flows_2025 PARTITION OF cash_flows FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
CREATE TABLE cash_flows_2026 PARTITION OF cash_flows FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');
CREATE TABLE cash_flows_2027 PARTITION OF cash_flows FOR VALUES FROM ('2027-01-01') TO ('2028-01-01');
CREATE TABLE cash_flows_2028 PARTITION OF cash_flows FOR VALUES FROM ('2028-01-01') TO ('2029-01-01');
CREATE TABLE cash_flows_2029 PARTITION OF cash_flows FOR VALUES FROM ('2029-01-01') TO ('2030-01-01');
CREATE TABLE cash_flows_2030 PARTITION OF cash_flows FOR VALUES FROM ('2030-01-01') TO ('2031-01-01');

-- Indexes for cash_flows table
CREATE INDEX idx_cash_flows_request_date ON cash_flows(request_id, calculation_date);
CREATE INDEX idx_cash_flows_contract_date ON cash_flows(contract_id, calculation_date);
CREATE INDEX idx_cash_flows_lot_date ON cash_flows(lot_id, calculation_date);
CREATE INDEX idx_cash_flows_type_date ON cash_flows(cash_flow_type, calculation_date);
CREATE INDEX idx_cash_flows_state_date ON cash_flows(state, calculation_date);
CREATE INDEX idx_cash_flows_currency_date ON cash_flows(currency, calculation_date);

-- Calculation requests table
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
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_requests_type_date (calculation_type, date_range_from, date_range_to),
    INDEX idx_requests_status (status),
    INDEX idx_requests_created (created_at)
);

-- =====================================================
-- 3. SETTLEMENT TABLES
-- =====================================================

-- Settlement instructions table
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
    failure_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (contract_id) REFERENCES contracts(contract_id),
    INDEX idx_settlements_cash_flow (cash_flow_id),
    INDEX idx_settlements_contract (contract_id),
    INDEX idx_settlements_counterparty (counterparty),
    INDEX idx_settlements_date (settlement_date),
    INDEX idx_settlements_status (status),
    INDEX idx_settlements_currency (currency)
);

-- =====================================================
-- 4. MARKET DATA TABLES
-- =====================================================

-- Market data cache table (partitioned by year)
CREATE TABLE market_data_cache (
    cache_key VARCHAR(100) NOT NULL,
    symbol VARCHAR(20) NOT NULL,
    data_type VARCHAR(20) NOT NULL,
    data_date DATE NOT NULL,
    data_value DECIMAL(20,6) NOT NULL,
    currency VARCHAR(3),
    source VARCHAR(50),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (cache_key, data_date),
    INDEX idx_market_data_symbol_date (symbol, data_date),
    INDEX idx_market_data_type_date (data_type, data_date),
    INDEX idx_market_data_expires (expires_at)
) PARTITION BY RANGE (YEAR(data_date));

-- Create partitions for market_data_cache table (2020-2030)
CREATE TABLE market_data_cache_2020 PARTITION OF market_data_cache FOR VALUES FROM ('2020-01-01') TO ('2021-01-01');
CREATE TABLE market_data_cache_2021 PARTITION OF market_data_cache FOR VALUES FROM ('2021-01-01') TO ('2022-01-01');
CREATE TABLE market_data_cache_2022 PARTITION OF market_data_cache FOR VALUES FROM ('2022-01-01') TO ('2023-01-01');
CREATE TABLE market_data_cache_2023 PARTITION OF market_data_cache FOR VALUES FROM ('2023-01-01') TO ('2024-01-01');
CREATE TABLE market_data_cache_2024 PARTITION OF market_data_cache FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');
CREATE TABLE market_data_cache_2025 PARTITION OF market_data_cache FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
CREATE TABLE market_data_cache_2026 PARTITION OF market_data_cache FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');
CREATE TABLE market_data_cache_2027 PARTITION OF market_data_cache FOR VALUES FROM ('2027-01-01') TO ('2028-01-01');
CREATE TABLE market_data_cache_2028 PARTITION OF market_data_cache FOR VALUES FROM ('2028-01-01') TO ('2029-01-01');
CREATE TABLE market_data_cache_2029 PARTITION OF market_data_cache FOR VALUES FROM ('2029-01-01') TO ('2030-01-01');
CREATE TABLE market_data_cache_2030 PARTITION OF market_data_cache FOR VALUES FROM ('2030-01-01') TO ('2031-01-01');

-- =====================================================
-- 5. AUDIT AND MONITORING TABLES
-- =====================================================

-- Audit trail table
CREATE TABLE audit_trail (
    audit_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_name VARCHAR(50) NOT NULL,
    record_id VARCHAR(50) NOT NULL,
    operation VARCHAR(20) NOT NULL,
    old_values JSON,
    new_values JSON,
    changed_by VARCHAR(50),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_table_record (table_name, record_id),
    INDEX idx_audit_operation (operation),
    INDEX idx_audit_changed_at (changed_at)
);

-- Performance metrics table
CREATE TABLE performance_metrics (
    metric_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DECIMAL(20,6) NOT NULL,
    metric_unit VARCHAR(20),
    tags JSON,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_metrics_name_time (metric_name, recorded_at),
    INDEX idx_metrics_recorded (recorded_at)
);

-- Maintenance log table
CREATE TABLE maintenance_log (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operation VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    details JSON,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    duration_ms BIGINT,
    INDEX idx_maintenance_operation (operation),
    INDEX idx_maintenance_status (status),
    INDEX idx_maintenance_started (started_at)
);

-- =====================================================
-- 6. CONFIGURATION TABLES
-- =====================================================

-- Archival configuration table
CREATE TABLE archival_config (
    config_id INT AUTO_INCREMENT PRIMARY KEY,
    table_name VARCHAR(50) NOT NULL,
    retention_months INTEGER NOT NULL,
    archival_frequency VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    archival_date DATE,
    compression_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    archival_location VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_archival_table (table_name),
    INDEX idx_archival_status (status)
);

-- Backup configuration table
CREATE TABLE backup_config (
    config_id INT AUTO_INCREMENT PRIMARY KEY,
    backup_type VARCHAR(20) NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    retention_days INTEGER NOT NULL,
    backup_location VARCHAR(200) NOT NULL,
    compression_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    encryption_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_backup_type (backup_type),
    INDEX idx_backup_status (status)
);

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
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (summary_date, contract_id),
    INDEX idx_summary_date (summary_date),
    INDEX idx_summary_underlying (underlying),
    INDEX idx_summary_contract (contract_id)
);

-- =====================================================
-- 8. STORED PROCEDURES
-- =====================================================

-- Procedure to create new partitions automatically
DELIMITER //
CREATE PROCEDURE create_partition_if_not_exists(
    IN table_name VARCHAR(100),
    IN partition_name VARCHAR(100),
    IN partition_range VARCHAR(200)
)
BEGIN
    DECLARE partition_exists INTEGER DEFAULT 0;
    
    SELECT COUNT(*) INTO partition_exists
    FROM INFORMATION_SCHEMA.PARTITIONS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = table_name
    AND PARTITION_NAME = partition_name;
    
    IF partition_exists = 0 THEN
        SET @sql = CONCAT('ALTER TABLE ', table_name, ' ADD PARTITION (PARTITION ', partition_name, ' VALUES LESS THAN (', partition_range, '))');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

-- Procedure to archive old cash flows
DELIMITER //
CREATE PROCEDURE archive_cash_flows(
    IN retention_months INTEGER,
    IN archival_location VARCHAR(200)
)
BEGIN
    DECLARE done INTEGER DEFAULT FALSE;
    DECLARE partition_name VARCHAR(100);
    DECLARE partition_date DATE;
    DECLARE archive_date DATE;
    
    DECLARE partition_cursor CURSOR FOR
        SELECT PARTITION_NAME, PARTITION_DESCRIPTION
        FROM INFORMATION_SCHEMA.PARTITIONS
        WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'cash_flows'
        AND PARTITION_NAME != 'default';
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    SET archive_date = DATE_SUB(CURDATE(), INTERVAL retention_months MONTH);
    
    OPEN partition_cursor;
    
    read_loop: LOOP
        FETCH partition_cursor INTO partition_name, partition_date;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        IF partition_date < archive_date THEN
            -- Archive partition data
            SET @sql = CONCAT('INSERT INTO ', archival_location, ' SELECT * FROM cash_flows PARTITION(', partition_name, ')');
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
            
            -- Drop partition
            SET @sql = CONCAT('ALTER TABLE cash_flows DROP PARTITION ', partition_name);
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
            
            -- Log archival
            INSERT INTO maintenance_log (operation, status, details)
            VALUES ('ARCHIVE_PARTITION', 'COMPLETED', JSON_OBJECT('partition', partition_name, 'archived_date', partition_date));
        END IF;
    END LOOP;
    
    CLOSE partition_cursor;
END //
DELIMITER ;

-- Procedure to refresh materialized views
DELIMITER //
CREATE PROCEDURE refresh_daily_cash_flow_summary()
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
    VALUES ('REFRESH_SUMMARY', 'COMPLETED', JSON_OBJECT('view', 'daily_cash_flow_summary'));
END //
DELIMITER ;

-- Procedure to cleanup expired market data
DELIMITER //
CREATE PROCEDURE cleanup_expired_market_data()
BEGIN
    DELETE FROM market_data_cache 
    WHERE expires_at < NOW();
    
    -- Log cleanup
    INSERT INTO maintenance_log (operation, status, details)
    VALUES ('CLEANUP_MARKET_DATA', 'COMPLETED', JSON_OBJECT('deleted_count', ROW_COUNT()));
END //
DELIMITER ;

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
