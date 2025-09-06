package com.financial.cashflow.repository;

import com.financial.cashflow.model.SettlementInstruction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Row mapper for SettlementInstruction entities
 */
@Component
@Slf4j
public class SettlementInstructionRowMapper implements RowMapper<SettlementInstruction> {
    
    @Override
    public SettlementInstruction mapRow(ResultSet rs, int rowNum) throws SQLException {
        return SettlementInstruction.builder()
            .settlementId(rs.getString("settlement_id"))
            .contractId(rs.getString("contract_id"))
            .cashFlowId(rs.getString("cash_flow_id"))
            .settlementDate(rs.getDate("settlement_date").toLocalDate())
            .settlementType(rs.getString("settlement_type"))
            .counterparty(rs.getString("counterparty"))
            .amount(rs.getBigDecimal("amount"))
            .currency(rs.getString("currency"))
            .status(rs.getString("status"))
            .retryCount(rs.getInt("retry_count"))
            .lastRetryDate(rs.getTimestamp("last_retry_date") != null ? 
                rs.getTimestamp("last_retry_date").toLocalDateTime() : null)
            .nextRetryDate(rs.getTimestamp("next_retry_date") != null ? 
                rs.getTimestamp("next_retry_date").toLocalDateTime() : null)
            .errorMessage(rs.getString("error_message"))
            .actualSettlementDate(rs.getDate("actual_settlement_date") != null ? 
                rs.getDate("actual_settlement_date").toLocalDate() : null)
            .settlementReference(rs.getString("settlement_reference"))
            .notes(rs.getString("notes"))
            .cancelledBy(rs.getString("cancelled_by"))
            .cancellationReason(rs.getString("cancellation_reason"))
            .createdAt(rs.getTimestamp("created_at").toLocalDateTime())
            .updatedAt(rs.getTimestamp("updated_at").toLocalDateTime())
            .build();
    }
}
