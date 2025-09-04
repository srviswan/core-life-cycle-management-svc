package com.financial.cashflow.service.impl;

import com.financial.cashflow.domain.CashFlowRequestContent;
import com.financial.cashflow.exception.ValidationException;
import com.financial.cashflow.service.CashFlowValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of CashFlowValidator with comprehensive validation logic.
 */
@Slf4j
@Service
public class CashFlowValidatorImpl implements CashFlowValidator {

    @Override
    public boolean validateRequest(CashFlowRequestContent request) {
        List<String> errors = new ArrayList<>();

        // Basic validation
        validateBasicFields(request, errors);
        
        // Date range validation
        validateDateRange(request, errors);
        
        // Contract validation
        validateContracts(request, errors);
        
        // Position validation
        validatePositions(request, errors);
        
        // Lot validation
        validateLots(request, errors);
        
        // Market data validation
        validateMarketData(request, errors);

        if (!errors.isEmpty()) {
            String errorMessage = "Validation failed: " + String.join("; ", errors);
            log.error("Cash flow request validation failed: {}", errorMessage);
            throw new ValidationException(errorMessage);
        }

        log.info("Cash flow request validation passed for request: {}", request.getRequestId());
        return true;
    }

    private void validateBasicFields(CashFlowRequestContent request, List<String> errors) {
        if (request == null) {
            errors.add("Request cannot be null");
            return;
        }

        if (!StringUtils.hasText(request.getRequestId())) {
            errors.add("Request ID is required");
        }

        if (!StringUtils.hasText(request.getContractId())) {
            errors.add("Contract ID is required");
        }

        if (request.getDateRange() == null) {
            errors.add("Date range is required");
        }
    }

    private void validateDateRange(CashFlowRequestContent request, List<String> errors) {
        if (request.getDateRange() == null) {
            return;
        }

        CashFlowRequestContent.DateRange dateRange = request.getDateRange();

        if (dateRange.getFromDate() == null) {
            errors.add("From date is required");
        }

        if (dateRange.getToDate() == null) {
            errors.add("To date is required");
        }

        if (dateRange.getFromDate() != null && dateRange.getToDate() != null) {
            if (dateRange.getFromDate().isAfter(dateRange.getToDate())) {
                errors.add("From date cannot be after to date");
            }

            if (dateRange.getFromDate().isAfter(LocalDate.now().plusYears(10))) {
                errors.add("From date cannot be more than 10 years in the future");
            }

            if (dateRange.getToDate().isAfter(LocalDate.now().plusYears(10))) {
                errors.add("To date cannot be more than 10 years in the future");
            }
        }

        if (!StringUtils.hasText(dateRange.getCalculationFrequency())) {
            errors.add("Calculation frequency is required");
        } else {
            String frequency = dateRange.getCalculationFrequency().toUpperCase();
            if (!frequency.matches("^(DAILY|WEEKLY|MONTHLY|QUARTERLY|YEARLY)$")) {
                errors.add("Invalid calculation frequency: " + dateRange.getCalculationFrequency());
            }
        }
    }

    private void validateContracts(CashFlowRequestContent request, List<String> errors) {
        if (request.getContracts() == null || request.getContracts().isEmpty()) {
            errors.add("At least one contract is required");
            return;
        }

        for (int i = 0; i < request.getContracts().size(); i++) {
            CashFlowRequestContent.Contract contract = request.getContracts().get(i);
            validateContract(contract, i, errors);
        }
    }

    private void validateContract(CashFlowRequestContent.Contract contract, int index, List<String> errors) {
        String prefix = "Contract[" + index + "]";

        if (!StringUtils.hasText(contract.getContractId())) {
            errors.add(prefix + ": Contract ID is required");
        }

        if (!StringUtils.hasText(contract.getContractType())) {
            errors.add(prefix + ": Contract type is required");
        } else {
            String contractType = contract.getContractType().toUpperCase();
            if (!contractType.matches("^(SWAP|CFD|FORWARD|OPTION)$")) {
                errors.add(prefix + ": Invalid contract type: " + contract.getContractType());
            }
        }

        if (contract.getNotionalAmount() == null || contract.getNotionalAmount() <= 0) {
            errors.add(prefix + ": Notional amount must be positive");
        }

        if (!StringUtils.hasText(contract.getCurrency())) {
            errors.add(prefix + ": Currency is required");
        } else {
            String currency = contract.getCurrency().toUpperCase();
            if (!currency.matches("^[A-Z]{3}$")) {
                errors.add(prefix + ": Invalid currency format: " + contract.getCurrency());
            }
        }

        if (contract.getStartDate() == null) {
            errors.add(prefix + ": Start date is required");
        }

        if (contract.getEndDate() == null) {
            errors.add(prefix + ": End date is required");
        }

        if (contract.getStartDate() != null && contract.getEndDate() != null) {
            if (contract.getStartDate().isAfter(contract.getEndDate())) {
                errors.add(prefix + ": Start date cannot be after end date");
            }
        }

        if (contract.getInterestRate() != null && (contract.getInterestRate() < -1 || contract.getInterestRate() > 1)) {
            errors.add(prefix + ": Interest rate must be between -100% and 100%");
        }
    }

    private void validatePositions(CashFlowRequestContent request, List<String> errors) {
        if (request.getPositions() == null || request.getPositions().isEmpty()) {
            errors.add("At least one position is required");
            return;
        }

        for (int i = 0; i < request.getPositions().size(); i++) {
            CashFlowRequestContent.Position position = request.getPositions().get(i);
            validatePosition(position, i, errors);
        }
    }

    private void validatePosition(CashFlowRequestContent.Position position, int index, List<String> errors) {
        String prefix = "Position[" + index + "]";

        if (!StringUtils.hasText(position.getPositionId())) {
            errors.add(prefix + ": Position ID is required");
        }

        if (!StringUtils.hasText(position.getContractId())) {
            errors.add(prefix + ": Contract ID is required");
        }

        if (position.getQuantity() == null || position.getQuantity() == 0) {
            errors.add(prefix + ": Quantity must be non-zero");
        }

        if (position.getAveragePrice() == null || position.getAveragePrice() <= 0) {
            errors.add(prefix + ": Average price must be positive");
        }

        if (!StringUtils.hasText(position.getCurrency())) {
            errors.add(prefix + ": Currency is required");
        }
    }

    private void validateLots(CashFlowRequestContent request, List<String> errors) {
        if (request.getLots() == null || request.getLots().isEmpty()) {
            errors.add("At least one lot is required");
            return;
        }

        for (int i = 0; i < request.getLots().size(); i++) {
            CashFlowRequestContent.Lot lot = request.getLots().get(i);
            validateLot(lot, i, errors);
        }
    }

    private void validateLot(CashFlowRequestContent.Lot lot, int index, List<String> errors) {
        String prefix = "Lot[" + index + "]";

        if (!StringUtils.hasText(lot.getLotId())) {
            errors.add(prefix + ": Lot ID is required");
        }

        if (!StringUtils.hasText(lot.getPositionId())) {
            errors.add(prefix + ": Position ID is required");
        }

        if (lot.getQuantity() == null || lot.getQuantity() <= 0) {
            errors.add(prefix + ": Quantity must be positive");
        }

        if (lot.getPrice() == null || lot.getPrice() <= 0) {
            errors.add(prefix + ": Price must be positive");
        }

        if (!StringUtils.hasText(lot.getCurrency())) {
            errors.add(prefix + ": Currency is required");
        }

        if (lot.getTradeDate() == null) {
            errors.add(prefix + ": Trade date is required");
        }
    }

    private void validateMarketData(CashFlowRequestContent request, List<String> errors) {
        if (request.getMarketDataStrategy() != null) {
            validateMarketDataStrategy(request.getMarketDataStrategy(), errors);
        }

        if (request.getMarketData() != null) {
            validateMarketDataContainer(request.getMarketData(), errors);
        }
    }

    private void validateMarketDataStrategy(CashFlowRequestContent.MarketDataStrategy strategy, List<String> errors) {
        if (!StringUtils.hasText(strategy.getStrategyType())) {
            errors.add("Market data strategy type is required");
        } else {
            String strategyType = strategy.getStrategyType().toUpperCase();
            if (!strategyType.matches("^(HYBRID|EXTERNAL_ONLY|EMBEDDED_ONLY)$")) {
                errors.add("Invalid market data strategy type: " + strategy.getStrategyType());
            }
        }

        if (strategy.getCacheTimeout() != null && strategy.getCacheTimeout() < 0) {
            errors.add("Cache timeout cannot be negative");
        }

        if (strategy.getRetryAttempts() != null && strategy.getRetryAttempts() < 0) {
            errors.add("Retry attempts cannot be negative");
        }
    }

    private void validateMarketDataContainer(CashFlowRequestContent.MarketDataContainer container, List<String> errors) {
        if (container.getPrices() != null) {
            for (int i = 0; i < container.getPrices().size(); i++) {
                validatePriceData(container.getPrices().get(i), i, errors);
            }
        }

        if (container.getRates() != null) {
            for (int i = 0; i < container.getRates().size(); i++) {
                validateRateData(container.getRates().get(i), i, errors);
            }
        }

        if (container.getFxRates() != null) {
            for (int i = 0; i < container.getFxRates().size(); i++) {
                validateFxRateData(container.getFxRates().get(i), i, errors);
            }
        }
    }

    private void validatePriceData(CashFlowRequestContent.PriceData priceData, int index, List<String> errors) {
        String prefix = "PriceData[" + index + "]";

        if (!StringUtils.hasText(priceData.getInstrumentId())) {
            errors.add(prefix + ": Instrument ID is required");
        }

        if (priceData.getPrice() == null || priceData.getPrice() <= 0) {
            errors.add(prefix + ": Price must be positive");
        }

        if (!StringUtils.hasText(priceData.getCurrency())) {
            errors.add(prefix + ": Currency is required");
        }

        if (priceData.getPriceDate() == null) {
            errors.add(prefix + ": Price date is required");
        }
    }

    private void validateRateData(CashFlowRequestContent.RateData rateData, int index, List<String> errors) {
        String prefix = "RateData[" + index + "]";

        if (!StringUtils.hasText(rateData.getRateIndex())) {
            errors.add(prefix + ": Rate index is required");
        }

        if (rateData.getRate() == null) {
            errors.add(prefix + ": Rate is required");
        }

        if (rateData.getRateDate() == null) {
            errors.add(prefix + ": Rate date is required");
        }
    }

    private void validateFxRateData(CashFlowRequestContent.FxRateData fxRateData, int index, List<String> errors) {
        String prefix = "FxRateData[" + index + "]";

        if (!StringUtils.hasText(fxRateData.getFromCurrency())) {
            errors.add(prefix + ": From currency is required");
        }

        if (!StringUtils.hasText(fxRateData.getToCurrency())) {
            errors.add(prefix + ": To currency is required");
        }

        if (fxRateData.getFxRate() == null || fxRateData.getFxRate() <= 0) {
            errors.add(prefix + ": FX rate must be positive");
        }

        if (fxRateData.getRateDate() == null) {
            errors.add(prefix + ": Rate date is required");
        }
    }
}
