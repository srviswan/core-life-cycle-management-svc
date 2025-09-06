package com.financial.cashflow.repository;

import com.financial.cashflow.model.SettlementInstruction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Row mapper for SettlementInstruction entities
 */
@Component
@Slf4j
public class SettlementInstructionRowMapper implements RowMapper<SettlementInstruction> {
    
    @Override
    public SettlementInstruction mapRow(ResultSet rs, int rowNum) throws SQLException {
        return SettlementInstruction.builder()
            .instructionId(rs.getString("instruction_id"))
            .contractId(rs.getString("contract_id"))
            .counterparty(rs.getString("counterparty"))
            .currency(rs.getString("currency"))
            .amount(rs.getBigDecimal("amount"))
            .settlementDate(rs.getDate("settlement_date").toLocalDate())
            .status(rs.getString("status"))
            .settlementType(rs.getString("settlement_type"))
            .accountNumber(rs.getString("account_number"))
            .bankCode(rs.getString("bank_code"))
            .reference(rs.getString("reference"))
            .createdTimestamp(rs.getTimestamp("created_timestamp").toLocalDateTime())
            .updatedTimestamp(rs.getTimestamp("updated_timestamp").toLocalDateTime())
            .errorMessage(rs.getString("error_message"))
            .build();
    }
}
