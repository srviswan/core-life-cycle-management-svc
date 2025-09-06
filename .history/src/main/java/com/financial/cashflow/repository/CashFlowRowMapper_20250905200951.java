package com.financial.cashflow.repository;

import com.financial.cashflow.model.CashFlowResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Row mapper for CashFlow entities
 */
@Component
@Slf4j
public class CashFlowRowMapper implements RowMapper<CashFlowResponse.CashFlow> {
    
    @Override
    public CashFlowResponse.CashFlow mapRow(ResultSet rs, int rowNum) throws SQLException {
        return CashFlowResponse.CashFlow.builder()
            .cashFlowId(rs.getString("cash_flow_id"))
            .requestId(rs.getString("request_id"))
            .contractId(rs.getString("contract_id"))
            .positionId(rs.getString("position_id"))
            .lotId(rs.getString("lot_id"))
            .scheduleId(rs.getString("schedule_id"))
            .calculationDate(rs.getDate("calculation_date").toLocalDate())
            .cashFlowType(rs.getString("cash_flow_type"))
            .equityLegAmount(rs.getBigDecimal("equity_leg_amount"))
            .interestLegAmount(rs.getBigDecimal("interest_leg_amount"))
            .totalAmount(rs.getBigDecimal("total_amount"))
            .currency(rs.getString("currency"))
            .state(rs.getString("state"))
            .equityUnrealizedPnl(rs.getBigDecimal("equity_unrealized_pnl"))
            .equityRealizedPnl(rs.getBigDecimal("equity_realized_pnl"))
            .equityTotalPnl(rs.getBigDecimal("equity_total_pnl"))
            .equityDividendAmount(rs.getBigDecimal("equity_dividend_amount"))
            .equityWithholdingTax(rs.getBigDecimal("equity_withholding_tax"))
            .equityNetDividend(rs.getBigDecimal("equity_net_dividend"))
            .interestAccruedAmount(rs.getBigDecimal("interest_accrued_amount"))
            .interestRate(rs.getBigDecimal("interest_rate"))
            .interestNotionalAmount(rs.getBigDecimal("interest_notional_amount"))
            .createdTimestamp(rs.getTimestamp("created_timestamp").toLocalDateTime())
            .build();
    }
}
