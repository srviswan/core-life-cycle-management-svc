package com.financial.cashflow.repository;

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
}
