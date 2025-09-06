package com.financial.cashflow.service.impl;

import com.financial.cashflow.domain.CashFlowResponse;
import com.financial.cashflow.service.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementation of EventPublisher for publishing cash flow events.
 * Uses virtual threads for I/O operations like messaging system calls.
 */
@Slf4j
@Service
public class EventPublisherImpl implements EventPublisher {

    @Override
    public void publishCashFlowCalculated(CashFlowResponse response) {
        log.info("Publishing cash flow calculated event for request: {}", response.getRequestId());
        
        try {
            CashFlowEvent event = createCashFlowEvent(response);
            publishToMessagingSystem(event);
            publishToODS(event);
            
            log.info("Successfully published cash flow event for request: {}", response.getRequestId());
        } catch (Exception e) {
            log.error("Failed to publish cash flow event for request: {}", response.getRequestId(), e);
            // Don't throw exception - event publishing should not fail the main flow
        }
    }

    @Override
    public void publishCashFlowCalculatedEvent(String requestId, String contractId, int cashFlowCount, Double totalAmount, String currency) {
        log.info("Publishing cash flow calculated event - Request: {}, Contract: {}, CashFlows: {}, Amount: {} {}", 
            requestId, contractId, cashFlowCount, totalAmount, currency);
        
        try {
            CashFlowEvent event = CashFlowEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("CASH_FLOW_CALCULATED")
                .timestamp(LocalDateTime.now())
                .requestId(requestId)
                .contractId(contractId)
                .calculationType("AUTO_DETERMINED")
                .cashFlowCount(cashFlowCount)
                .settlementCount(0) // Will be updated when settlements are generated
                .totalAmount(totalAmount != null ? totalAmount : 0.0)
                .currency(currency != null ? currency : "USD")
                .status("SUCCESS")
                .build();
            
            publishToMessagingSystem(event);
            publishToODS(event);
            
            log.info("Successfully published cash flow event for request: {}", requestId);
        } catch (Exception e) {
            log.error("Failed to publish cash flow event for request: {}", requestId, e);
            // Don't throw exception - event publishing should not fail the main flow
        }
    }

    private CashFlowEvent createCashFlowEvent(CashFlowResponse response) {
        return CashFlowEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("CASH_FLOW_CALCULATED")
            .timestamp(LocalDateTime.now())
            .requestId(response.getRequestId())
            .contractId(response.getContractId())
            .calculationType(response.getCalculationType() != null ? response.getCalculationType() : "UNKNOWN")
            .cashFlowCount(response.getCashFlows() != null ? response.getCashFlows().size() : 0)
            .settlementCount(response.getSettlementInstructions() != null ? response.getSettlementInstructions().size() : 0)
            .totalAmount(calculateTotalAmount(response))
            .currency(response.getCashFlows() != null && !response.getCashFlows().isEmpty() ? 
                     response.getCashFlows().get(0).getCurrency() : "USD")
            .status("SUCCESS")
            .build();
    }

    private double calculateTotalAmount(CashFlowResponse response) {
        if (response.getCashFlows() == null) {
            return 0.0;
        }
        
        return response.getCashFlows().stream()
            .mapToDouble(cashFlow -> cashFlow.getAmount())
            .sum();
    }

    private void publishToMessagingSystem(CashFlowEvent event) {
        // In a real implementation, this would use Kafka or Solace
        log.debug("Publishing to messaging system: {}", event);
        
        // TODO: Implement actual messaging system integration
        // Example for Kafka:
        // kafkaTemplate.send("cash-flow-events", event.getEventId(), event);
        
        // Example for Solace:
        // solaceTemplate.send("cash-flow-events", event);
    }

    private void publishToODS(CashFlowEvent event) {
        // In a real implementation, this would publish to the Operational Data Store
        log.debug("Publishing to ODS: {}", event);
        
        // TODO: Implement actual ODS integration
        // Example:
        // odsTemplate.send("real-time-cash-flows", event);
    }

    /**
     * Internal event class for cash flow events.
     */
    public static class CashFlowEvent {
        private String eventId;
        private String eventType;
        private LocalDateTime timestamp;
        private String requestId;
        private String contractId;
        private String calculationType;
        private int cashFlowCount;
        private int settlementCount;
        private double totalAmount;
        private String currency;
        private String status;

        // Builder pattern
        public static CashFlowEventBuilder builder() {
            return new CashFlowEventBuilder();
        }

        public static class CashFlowEventBuilder {
            private CashFlowEvent event = new CashFlowEvent();

            public CashFlowEventBuilder eventId(String eventId) {
                event.eventId = eventId;
                return this;
            }

            public CashFlowEventBuilder eventType(String eventType) {
                event.eventType = eventType;
                return this;
            }

            public CashFlowEventBuilder timestamp(LocalDateTime timestamp) {
                event.timestamp = timestamp;
                return this;
            }

            public CashFlowEventBuilder requestId(String requestId) {
                event.requestId = requestId;
                return this;
            }

            public CashFlowEventBuilder contractId(String contractId) {
                event.contractId = contractId;
                return this;
            }

            public CashFlowEventBuilder calculationType(String calculationType) {
                event.calculationType = calculationType;
                return this;
            }

            public CashFlowEventBuilder cashFlowCount(int cashFlowCount) {
                event.cashFlowCount = cashFlowCount;
                return this;
            }

            public CashFlowEventBuilder settlementCount(int settlementCount) {
                event.settlementCount = settlementCount;
                return this;
            }

            public CashFlowEventBuilder totalAmount(double totalAmount) {
                event.totalAmount = totalAmount;
                return this;
            }

            public CashFlowEventBuilder currency(String currency) {
                event.currency = currency;
                return this;
            }

            public CashFlowEventBuilder status(String status) {
                event.status = status;
                return this;
            }

            public CashFlowEvent build() {
                return event;
            }
        }

        // Getters
        public String getEventId() { return eventId; }
        public String getEventType() { return eventType; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getRequestId() { return requestId; }
        public String getContractId() { return contractId; }
        public String getCalculationType() { return calculationType; }
        public int getCashFlowCount() { return cashFlowCount; }
        public int getSettlementCount() { return settlementCount; }
        public double getTotalAmount() { return totalAmount; }
        public String getCurrency() { return currency; }
        public String getStatus() { return status; }
    }
}
