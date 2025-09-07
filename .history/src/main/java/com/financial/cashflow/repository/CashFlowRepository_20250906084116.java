package com.financial.cashflow.repository;

import com.financial.cashflow.model.CashFlowAggregation;
import com.financial.cashflow.model.CashFlowHierarchy;
import com.financial.cashflow.model.CashFlowResponse;
import com.financial.cashflow.model.SettlementInstruction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for Cash Flow data access using JDBC Template
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class CashFlowRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private final CashFlowRowMapper cashFlowRowMapper;
    
    /**
     * Save all cash flows in batch
     */
    public void saveAll(List<CashFlowResponse.CashFlow> cashFlows) {
        if (cashFlows.isEmpty()) {
            return;
        }
        
        String sql = """
            INSERT INTO cash_flows (
                cash_flow_id, request_id, contract_id, position_id, lot_id, 
                schedule_id, calculation_date, cash_flow_type, equity_leg_amount,
                interest_leg_amount, total_amount, currency, state,
                equity_unrealized_pnl, equity_realized_pnl, equity_total_pnl,
                equity_dividend_amount, equity_withholding_tax, equity_net_dividend,
                interest_accrued_amount, interest_rate, interest_notional_amount,
                created_timestamp
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                CashFlowResponse.CashFlow cashFlow = cashFlows.get(i);
                ps.setString(1, cashFlow.getCashFlowId());
                ps.setString(2, cashFlow.getRequestId());
                ps.setString(3, cashFlow.getContractId());
                ps.setString(4, cashFlow.getPositionId());
                ps.setString(5, cashFlow.getLotId());
                ps.setString(6, cashFlow.getScheduleId());
                ps.setDate(7, Date.valueOf(cashFlow.getCalculationDate()));
                ps.setString(8, cashFlow.getCashFlowType());
                ps.setBigDecimal(9, cashFlow.getEquityLegAmount());
                ps.setBigDecimal(10, cashFlow.getInterestLegAmount());
                ps.setBigDecimal(11, cashFlow.getTotalAmount());
                ps.setString(12, cashFlow.getCurrency());
                ps.setString(13, cashFlow.getState());
                ps.setBigDecimal(14, cashFlow.getEquityUnrealizedPnl());
                ps.setBigDecimal(15, cashFlow.getEquityRealizedPnl());
                ps.setBigDecimal(16, cashFlow.getEquityTotalPnl());
                ps.setBigDecimal(17, cashFlow.getEquityDividendAmount());
                ps.setBigDecimal(18, cashFlow.getEquityWithholdingTax());
                ps.setBigDecimal(19, cashFlow.getEquityNetDividend());
                ps.setBigDecimal(20, cashFlow.getInterestAccruedAmount());
                ps.setBigDecimal(21, cashFlow.getInterestRate());
                ps.setBigDecimal(22, cashFlow.getInterestNotionalAmount());
                ps.setTimestamp(23, java.sql.Timestamp.valueOf(cashFlow.getCreatedTimestamp()));
            }
            
            @Override
            public int getBatchSize() {
                return cashFlows.size();
            }
        });
        
        log.info("Saved {} cash flows to database", cashFlows.size());
    }
    
    /**
     * Find cash flows by contract and date range
     */
    public List<CashFlowResponse.CashFlow> findByContractIdAndDateRange(String contractId, 
                                                                       LocalDate fromDate, 
                                                                       LocalDate toDate, 
                                                                       String cashFlowType, 
                                                                       String state) {
        StringBuilder sql = new StringBuilder("""
            SELECT * FROM cash_flows 
            WHERE contract_id = ? 
            AND calculation_date BETWEEN ? AND ?
            """);
        
        List<Object> params = new ArrayList<>();
        params.add(contractId);
        params.add(fromDate);
        params.add(toDate);
        
        if (cashFlowType != null) {
            sql.append(" AND cash_flow_type = ?");
            params.add(cashFlowType);
        }
        
        if (state != null) {
            sql.append(" AND state = ?");
            params.add(state);
        }
        
        sql.append(" ORDER BY calculation_date");
        
        return jdbcTemplate.query(sql.toString(), cashFlowRowMapper, params.toArray());
    }
    
    /**
     * Find pending settlements
     */
    public List<SettlementInstruction> findPendingSettlements(String counterparty, String currency) {
        StringBuilder sql = new StringBuilder("""
            SELECT * FROM settlement_instructions 
            WHERE status = 'PENDING'
            """);
        
        List<Object> params = new ArrayList<>();
        
        if (counterparty != null) {
            sql.append(" AND counterparty = ?");
            params.add(counterparty);
        }
        
        if (currency != null) {
            sql.append(" AND currency = ?");
            params.add(currency);
        }
        
        sql.append(" ORDER BY settlement_date");
        
        return jdbcTemplate.query(sql.toString(), new SettlementInstructionRowMapper(), params.toArray());
    }
    
    /**
     * Get cash flow count by contract
     */
    public int getCashFlowCountByContract(String contractId) {
        String sql = "SELECT COUNT(*) FROM cash_flows WHERE contract_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, contractId);
    }
    
    /**
     * Delete cash flows by request ID
     */
    public int deleteByRequestId(String requestId) {
        String sql = "DELETE FROM cash_flows WHERE request_id = ?";
        return jdbcTemplate.update(sql, requestId);
    }
    
    // =====================================================
    // CONSOLIDATION QUERIES - LOT TO POSITION TO CONTRACT
    // =====================================================
    
    /**
     * Get cash flows consolidated by lot (most granular level)
     */
    public List<CashFlowResponse.CashFlow> getCashFlowsByLot(String lotId, LocalDate fromDate, LocalDate toDate) {
        String sql = """
            SELECT * FROM cash_flows 
            WHERE lot_id = ? AND calculation_date BETWEEN ? AND ?
            ORDER BY calculation_date
            """;
        return jdbcTemplate.query(sql, cashFlowRowMapper, lotId, fromDate, toDate);
    }
    
    /**
     * Get cash flows consolidated by position
     */
    public List<CashFlowResponse.CashFlow> getCashFlowsByPosition(String positionId, LocalDate fromDate, LocalDate toDate) {
        String sql = """
            SELECT * FROM cash_flows 
            WHERE position_id = ? AND calculation_date BETWEEN ? AND ?
            ORDER BY calculation_date
            """;
        return jdbcTemplate.query(sql, cashFlowRowMapper, positionId, fromDate, toDate);
    }
    
    /**
     * Get cash flows consolidated by contract (highest level)
     */
    public List<CashFlowResponse.CashFlow> getCashFlowsByContract(String contractId, LocalDate fromDate, LocalDate toDate) {
        String sql = """
            SELECT * FROM cash_flows 
            WHERE contract_id = ? AND calculation_date BETWEEN ? AND ?
            ORDER BY calculation_date
            """;
        return jdbcTemplate.query(sql, cashFlowRowMapper, contractId, fromDate, toDate);
    }
    
    /**
     * Get aggregated cash flows by position (sum of all lots in position)
     */
    public CashFlowAggregation getAggregatedCashFlowsByPosition(String positionId, LocalDate fromDate, LocalDate toDate) {
        String sql = """
            SELECT 
                position_id,
                contract_id,
                currency,
                SUM(total_amount) as total_amount,
                SUM(equity_leg_amount) as equity_leg_amount,
                SUM(interest_leg_amount) as interest_leg_amount,
                SUM(equity_unrealized_pnl) as equity_unrealized_pnl,
                SUM(equity_realized_pnl) as equity_realized_pnl,
                SUM(equity_total_pnl) as equity_total_pnl,
                SUM(equity_dividend_amount) as equity_dividend_amount,
                SUM(equity_withholding_tax) as equity_withholding_tax,
                SUM(equity_net_dividend) as equity_net_dividend,
                SUM(interest_accrued_amount) as interest_accrued_amount,
                SUM(interest_notional_amount) as interest_notional_amount,
                COUNT(DISTINCT lot_id) as lot_count,
                COUNT(*) as cash_flow_count
            FROM cash_flows 
            WHERE position_id = ? AND calculation_date BETWEEN ? AND ?
            GROUP BY position_id, contract_id, currency
            """;
        
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            CashFlowAggregation aggregation = new CashFlowAggregation();
            aggregation.setPositionId(rs.getString("position_id"));
            aggregation.setContractId(rs.getString("contract_id"));
            aggregation.setCurrency(rs.getString("currency"));
            aggregation.setTotalAmount(rs.getBigDecimal("total_amount"));
            aggregation.setEquityLegAmount(rs.getBigDecimal("equity_leg_amount"));
            aggregation.setInterestLegAmount(rs.getBigDecimal("interest_leg_amount"));
            aggregation.setEquityUnrealizedPnl(rs.getBigDecimal("equity_unrealized_pnl"));
            aggregation.setEquityRealizedPnl(rs.getBigDecimal("equity_realized_pnl"));
            aggregation.setEquityTotalPnl(rs.getBigDecimal("equity_total_pnl"));
            aggregation.setEquityDividendAmount(rs.getBigDecimal("equity_dividend_amount"));
            aggregation.setEquityWithholdingTax(rs.getBigDecimal("equity_withholding_tax"));
            aggregation.setEquityNetDividend(rs.getBigDecimal("equity_net_dividend"));
            aggregation.setInterestAccruedAmount(rs.getBigDecimal("interest_accrued_amount"));
            aggregation.setInterestNotionalAmount(rs.getBigDecimal("interest_notional_amount"));
            aggregation.setLotCount(rs.getInt("lot_count"));
            aggregation.setCashFlowCount(rs.getInt("cash_flow_count"));
            return aggregation;
        }, positionId, fromDate, toDate);
    }
    
    /**
     * Get aggregated cash flows by contract (sum of all positions in contract)
     */
    public CashFlowAggregation getAggregatedCashFlowsByContract(String contractId, LocalDate fromDate, LocalDate toDate) {
        String sql = """
            SELECT 
                contract_id,
                currency,
                SUM(total_amount) as total_amount,
                SUM(equity_leg_amount) as equity_leg_amount,
                SUM(interest_leg_amount) as interest_leg_amount,
                SUM(equity_unrealized_pnl) as equity_unrealized_pnl,
                SUM(equity_realized_pnl) as equity_realized_pnl,
                SUM(equity_total_pnl) as equity_total_pnl,
                SUM(equity_dividend_amount) as equity_dividend_amount,
                SUM(equity_withholding_tax) as equity_withholding_tax,
                SUM(equity_net_dividend) as equity_net_dividend,
                SUM(interest_accrued_amount) as interest_accrued_amount,
                SUM(interest_notional_amount) as interest_notional_amount,
                COUNT(DISTINCT position_id) as position_count,
                COUNT(DISTINCT lot_id) as lot_count,
                COUNT(*) as cash_flow_count
            FROM cash_flows 
            WHERE contract_id = ? AND calculation_date BETWEEN ? AND ?
            GROUP BY contract_id, currency
            """;
        
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            CashFlowAggregation aggregation = new CashFlowAggregation();
            aggregation.setContractId(rs.getString("contract_id"));
            aggregation.setCurrency(rs.getString("currency"));
            aggregation.setTotalAmount(rs.getBigDecimal("total_amount"));
            aggregation.setEquityLegAmount(rs.getBigDecimal("equity_leg_amount"));
            aggregation.setInterestLegAmount(rs.getBigDecimal("interest_leg_amount"));
            aggregation.setEquityUnrealizedPnl(rs.getBigDecimal("equity_unrealized_pnl"));
            aggregation.setEquityRealizedPnl(rs.getBigDecimal("equity_realized_pnl"));
            aggregation.setEquityTotalPnl(rs.getBigDecimal("equity_total_pnl"));
            aggregation.setEquityDividendAmount(rs.getBigDecimal("equity_dividend_amount"));
            aggregation.setEquityWithholdingTax(rs.getBigDecimal("equity_withholding_tax"));
            aggregation.setEquityNetDividend(rs.getBigDecimal("equity_net_dividend"));
            aggregation.setInterestAccruedAmount(rs.getBigDecimal("interest_accrued_amount"));
            aggregation.setInterestNotionalAmount(rs.getBigDecimal("interest_notional_amount"));
            aggregation.setPositionCount(rs.getInt("position_count"));
            aggregation.setLotCount(rs.getInt("lot_count"));
            aggregation.setCashFlowCount(rs.getInt("cash_flow_count"));
            return aggregation;
        }, contractId, fromDate, toDate);
    }
    
    /**
     * Get hierarchical cash flow breakdown (contract -> position -> lot)
     */
    public List<CashFlowHierarchy> getCashFlowHierarchy(String contractId, LocalDate fromDate, LocalDate toDate) {
        String sql = """
            SELECT 
                contract_id,
                position_id,
                lot_id,
                currency,
                SUM(total_amount) as total_amount,
                SUM(equity_leg_amount) as equity_leg_amount,
                SUM(interest_leg_amount) as interest_leg_amount,
                SUM(equity_unrealized_pnl) as equity_unrealized_pnl,
                SUM(equity_realized_pnl) as equity_realized_pnl,
                SUM(equity_total_pnl) as equity_total_pnl,
                SUM(equity_dividend_amount) as equity_dividend_amount,
                SUM(equity_withholding_tax) as equity_withholding_tax,
                SUM(equity_net_dividend) as equity_net_dividend,
                SUM(interest_accrued_amount) as interest_accrued_amount,
                SUM(interest_notional_amount) as interest_notional_amount,
                COUNT(*) as cash_flow_count
            FROM cash_flows 
            WHERE contract_id = ? AND calculation_date BETWEEN ? AND ?
            GROUP BY contract_id, position_id, lot_id, currency
            ORDER BY position_id, lot_id
            """;
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            CashFlowHierarchy hierarchy = new CashFlowHierarchy();
            hierarchy.setContractId(rs.getString("contract_id"));
            hierarchy.setPositionId(rs.getString("position_id"));
            hierarchy.setLotId(rs.getString("lot_id"));
            hierarchy.setCurrency(rs.getString("currency"));
            hierarchy.setTotalAmount(rs.getBigDecimal("total_amount"));
            hierarchy.setEquityLegAmount(rs.getBigDecimal("equity_leg_amount"));
            hierarchy.setInterestLegAmount(rs.getBigDecimal("interest_leg_amount"));
            hierarchy.setEquityUnrealizedPnl(rs.getBigDecimal("equity_unrealized_pnl"));
            hierarchy.setEquityRealizedPnl(rs.getBigDecimal("equity_realized_pnl"));
            hierarchy.setEquityTotalPnl(rs.getBigDecimal("equity_total_pnl"));
            hierarchy.setEquityDividendAmount(rs.getBigDecimal("equity_dividend_amount"));
            hierarchy.setEquityWithholdingTax(rs.getBigDecimal("equity_withholding_tax"));
            hierarchy.setEquityNetDividend(rs.getBigDecimal("equity_net_dividend"));
            hierarchy.setInterestAccruedAmount(rs.getBigDecimal("interest_accrued_amount"));
            hierarchy.setInterestNotionalAmount(rs.getBigDecimal("interest_notional_amount"));
            hierarchy.setCashFlowCount(rs.getInt("cash_flow_count"));
            return hierarchy;
        }, contractId, fromDate, toDate);
    }
}
