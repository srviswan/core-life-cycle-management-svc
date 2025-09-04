package com.financial.cashflow.service.impl;

import com.financial.cashflow.domain.CashFlowRequestContent;
import com.financial.cashflow.domain.CashFlowResponse;
import com.financial.cashflow.service.SettlementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of SettlementService for generating settlement instructions.
 */
@Slf4j
@Service
public class SettlementServiceImpl implements SettlementService {

    @Override
    public List<CashFlowResponse.SettlementInstruction> generateSettlementInstructions(
            List<CashFlowResponse.CashFlow> cashFlows,
            CashFlowRequestContent request) {
        
        log.info("Generating settlement instructions for {} cash flows", cashFlows.size());
        
        List<CashFlowResponse.SettlementInstruction> instructions = new ArrayList<>();
        
        for (CashFlowResponse.CashFlow cashFlow : cashFlows) {
            // Only generate settlement instructions for realized cash flows
            if (isRealizedCashFlow(cashFlow)) {
                CashFlowResponse.SettlementInstruction instruction = createSettlementInstruction(cashFlow, request);
                instructions.add(instruction);
            }
        }
        
        log.info("Generated {} settlement instructions", instructions.size());
        return instructions;
    }

    private boolean isRealizedCashFlow(CashFlowResponse.CashFlow cashFlow) {
        return "REALIZED_UNSETTLED".equals(cashFlow.getStatus()) || 
               "REALIZED_SETTLED".equals(cashFlow.getStatus());
    }

    private CashFlowResponse.SettlementInstruction createSettlementInstruction(
            CashFlowResponse.CashFlow cashFlow,
            CashFlowRequestContent request) {
        
        // Determine settlement date based on cash flow type
        LocalDate settlementDate = determineSettlementDate(cashFlow, request);
        
        // Get bank details from request or use defaults
        CashFlowResponse.BankDetails bankDetails = getBankDetails(cashFlow, request);
        
        return CashFlowResponse.SettlementInstruction.builder()
            .settlementId("SETTLE_" + UUID.randomUUID().toString())
            .contractId(cashFlow.getContractId())
            .cashFlowId(cashFlow.getCashFlowId())
            .settlementDate(settlementDate)
            .settlementType(determineSettlementType(cashFlow))
            .amount(cashFlow.getAmount())
            .currency(cashFlow.getCurrency())
            .status("PENDING")
            .instructionDetails(CashFlowResponse.SettlementDetails.builder()
                .priority(determinePriority(cashFlow))
                .bankDetails(bankDetails)
                .reference(generateReferenceNumber(cashFlow))
                .build())
            .build();
    }

    private LocalDate determineSettlementDate(CashFlowResponse.CashFlow cashFlow, CashFlowRequestContent request) {
        // Use the cash flow's settlement date if available
        if (cashFlow.getSettlementDate() != null) {
            return cashFlow.getSettlementDate();
        }
        
        // Default to T+2 for most cash flows
        LocalDate defaultSettlementDate = cashFlow.getCashFlowDate().plusDays(2);
        
        // Adjust for different cash flow types
        switch (cashFlow.getCashFlowType()) {
            case "INTEREST":
                // Interest payments typically settle on the next business day
                return adjustForBusinessDay(cashFlow.getCashFlowDate().plusDays(1));
            case "DIVIDEND":
                // Dividend payments typically settle on the ex-dividend date + 2 business days
                return adjustForBusinessDay(cashFlow.getCashFlowDate().plusDays(2));
            case "PNL":
                // P&L settlements typically settle on T+2
                return adjustForBusinessDay(cashFlow.getCashFlowDate().plusDays(2));
            case "PRINCIPAL":
                // Principal payments typically settle on the contract end date
                return cashFlow.getCashFlowDate();
            default:
                return adjustForBusinessDay(defaultSettlementDate);
        }
    }

    private LocalDate adjustForBusinessDay(LocalDate date) {
        // Simple business day adjustment - skip weekends
        int dayOfWeek = date.getDayOfWeek().getValue();
        if (dayOfWeek == 6) { // Saturday
            return date.plusDays(2);
        } else if (dayOfWeek == 7) { // Sunday
            return date.plusDays(1);
        }
        return date;
    }

    private String determineSettlementType(CashFlowResponse.CashFlow cashFlow) {
        switch (cashFlow.getCashFlowType()) {
            case "INTEREST":
                return "INTEREST_PAYMENT";
            case "DIVIDEND":
                return "DIVIDEND_PAYMENT";
            case "PNL":
                return "PNL_SETTLEMENT";
            case "PRINCIPAL":
                return "PRINCIPAL_PAYMENT";
            default:
                return "GENERAL_SETTLEMENT";
        }
    }

    private String determinePriority(CashFlowResponse.CashFlow cashFlow) {
        // Higher priority for regulatory and time-sensitive payments
        switch (cashFlow.getCashFlowType()) {
            case "DIVIDEND":
                return "HIGH"; // Dividend payments are time-sensitive
            case "PRINCIPAL":
                return "HIGH"; // Principal payments are critical
            case "INTEREST":
                return "MEDIUM"; // Interest payments are regular
            case "PNL":
                return "MEDIUM"; // P&L settlements are regular
            default:
                return "LOW";
        }
    }

    private CashFlowResponse.BankDetails getBankDetails(CashFlowResponse.CashFlow cashFlow, CashFlowRequestContent request) {
        // Try to get bank details from the request
        if (request.getBankDetails() != null) {
            return CashFlowResponse.BankDetails.builder()
                .bankName(request.getBankDetails().getBankName())
                .accountNumber(request.getBankDetails().getAccountNumber())
                .routingNumber(request.getBankDetails().getRoutingNumber())
                .swiftCode(request.getBankDetails().getSwiftCode())
                .iban(request.getBankDetails().getIban())
                .build();
        }
        
        // Return default bank details
        return CashFlowResponse.BankDetails.builder()
            .bankName("Default Bank")
            .accountNumber("1234567890")
            .routingNumber("987654321")
            .swiftCode("DEFBANK")
            .iban("GB29NWBK60161331926819")
            .build();
    }

    private String determineSettlementMethod(CashFlowResponse.CashFlow cashFlow) {
        // Determine settlement method based on cash flow type and amount
        if (cashFlow.getAmount() > 1000000) { // Large amounts
            return "WIRE_TRANSFER";
        } else if ("DIVIDEND".equals(cashFlow.getCashFlowType())) {
            return "ACH"; // Automated Clearing House for dividends
        } else {
            return "WIRE_TRANSFER"; // Default to wire transfer
        }
    }

    private String generateReferenceNumber(CashFlowResponse.CashFlow cashFlow) {
        // Generate a unique reference number for the settlement
        return "REF_" + cashFlow.getContractId() + "_" + 
               cashFlow.getCashFlowType() + "_" + 
               cashFlow.getCashFlowDate().toString().replace("-", "");
    }
}
