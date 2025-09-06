package com.financial.cashflow.service;

import com.financial.cashflow.entity.WithholdingTaxEntity;
import com.financial.cashflow.model.WithholdingTaxInfo;
import com.financial.cashflow.repository.WithholdingTaxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing withholding tax information
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WithholdingTaxService {
    
    private final WithholdingTaxRepository withholdingTaxRepository;
    
    /**
     * Save withholding tax information
     */
    public void saveWithholdingTaxDetails(List<WithholdingTaxInfo> withholdingTaxDetails) {
        if (withholdingTaxDetails == null || withholdingTaxDetails.isEmpty()) {
            log.debug("No withholding tax details to save");
            return;
        }
        
        try {
            List<WithholdingTaxEntity> entities = withholdingTaxDetails.stream()
                    .map(this::mapToEntity)
                    .collect(Collectors.toList());
            
            withholdingTaxRepository.saveAll(entities);
            log.info("Saved {} withholding tax details", entities.size());
            
        } catch (Exception e) {
            log.error("Failed to save withholding tax details", e);
            throw new RuntimeException("Failed to save withholding tax details", e);
        }
    }
    
    /**
     * Get withholding tax details by contract ID
     */
    public List<WithholdingTaxInfo> getWithholdingTaxByContract(String contractId) {
        try {
            List<WithholdingTaxEntity> entities = withholdingTaxRepository.findByContractId(contractId);
            return entities.stream()
                    .map(this::mapToInfo)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get withholding tax details for contract: {}", contractId, e);
            throw new RuntimeException("Failed to get withholding tax details", e);
        }
    }
    
    /**
     * Get withholding tax details by contract ID and date range
     */
    public List<WithholdingTaxInfo> getWithholdingTaxByContractAndDateRange(String contractId, 
                                                                             LocalDate fromDate, 
                                                                             LocalDate toDate) {
        try {
            List<WithholdingTaxEntity> entities = withholdingTaxRepository.findByContractIdAndDateRange(contractId, fromDate, toDate);
            return entities.stream()
                    .map(this::mapToInfo)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get withholding tax details for contract: {} and date range: {} to {}", 
                     contractId, fromDate, toDate, e);
            throw new RuntimeException("Failed to get withholding tax details", e);
        }
    }
    
    /**
     * Get withholding tax details for tax utility reporting
     */
    public List<WithholdingTaxInfo> getWithholdingTaxForTaxUtilityReporting(String jurisdiction, 
                                                                             LocalDate fromDate, 
                                                                             LocalDate toDate) {
        try {
            List<WithholdingTaxEntity> entities = withholdingTaxRepository.findForTaxUtilityReporting(jurisdiction, fromDate, toDate);
            return entities.stream()
                    .map(this::mapToInfo)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get withholding tax details for tax utility reporting: jurisdiction={}, from={}, to={}", 
                     jurisdiction, fromDate, toDate, e);
            throw new RuntimeException("Failed to get withholding tax details for tax utility reporting", e);
        }
    }
    
    /**
     * Get total withholding tax amount by contract
     */
    public double getTotalWithholdingTaxByContract(String contractId) {
        try {
            Double total = withholdingTaxRepository.getTotalWithholdingTaxByContract(contractId);
            return total != null ? total : 0.0;
        } catch (Exception e) {
            log.error("Failed to get total withholding tax for contract: {}", contractId, e);
            throw new RuntimeException("Failed to get total withholding tax", e);
        }
    }
    
    /**
     * Get total withholding tax amount by jurisdiction
     */
    public double getTotalWithholdingTaxByJurisdiction(String jurisdiction, LocalDate fromDate, LocalDate toDate) {
        try {
            Double total = withholdingTaxRepository.getTotalWithholdingTaxByJurisdiction(jurisdiction, fromDate, toDate);
            return total != null ? total : 0.0;
        } catch (Exception e) {
            log.error("Failed to get total withholding tax for jurisdiction: {}", jurisdiction, e);
            throw new RuntimeException("Failed to get total withholding tax by jurisdiction", e);
        }
    }
    
    /**
     * Map WithholdingTaxInfo to WithholdingTaxEntity
     */
    private WithholdingTaxEntity mapToEntity(WithholdingTaxInfo info) {
        return WithholdingTaxEntity.builder()
                .contractId(info.getContractId())
                .lotId(info.getLotId())
                .underlying(info.getUnderlying())
                .currency(info.getCurrency())
                .exDate(info.getExDate())
                .paymentDate(info.getPaymentDate())
                .grossDividendAmount(info.getGrossDividendAmount())
                .withholdingTaxRate(info.getWithholdingTaxRate())
                .withholdingTaxAmount(info.getWithholdingTaxAmount())
                .netDividendAmount(info.getNetDividendAmount())
                .withholdingTreatment(mapWithholdingTreatment(info.getWithholdingTreatment()))
                .taxJurisdiction(info.getTaxJurisdiction())
                .taxUtilityReference(info.getTaxUtilityReference())
                .calculationDate(info.getCalculationDate())
                .calculationType(info.getCalculationType())
                .build();
    }
    
    /**
     * Map WithholdingTaxEntity to WithholdingTaxInfo
     */
    private WithholdingTaxInfo mapToInfo(WithholdingTaxEntity entity) {
        return WithholdingTaxInfo.builder()
                .contractId(entity.getContractId())
                .lotId(entity.getLotId())
                .underlying(entity.getUnderlying())
                .currency(entity.getCurrency())
                .exDate(entity.getExDate())
                .paymentDate(entity.getPaymentDate())
                .grossDividendAmount(entity.getGrossDividendAmount())
                .withholdingTaxRate(entity.getWithholdingTaxRate())
                .withholdingTaxAmount(entity.getWithholdingTaxAmount())
                .netDividendAmount(entity.getNetDividendAmount())
                .withholdingTreatment(mapWithholdingTreatment(entity.getWithholdingTreatment()))
                .taxJurisdiction(entity.getTaxJurisdiction())
                .taxUtilityReference(entity.getTaxUtilityReference())
                .calculationDate(entity.getCalculationDate())
                .calculationType(entity.getCalculationType())
                .build();
    }
    
    /**
     * Map WithholdingTaxInfo.WithholdingTreatment to WithholdingTaxEntity.WithholdingTreatment
     */
    private WithholdingTaxEntity.WithholdingTreatment mapWithholdingTreatment(WithholdingTaxInfo.WithholdingTreatment treatment) {
        if (treatment == null) {
            return null;
        }
        return WithholdingTaxEntity.WithholdingTreatment.valueOf(treatment.name());
    }
    
    /**
     * Map WithholdingTaxEntity.WithholdingTreatment to WithholdingTaxInfo.WithholdingTreatment
     */
    private WithholdingTaxInfo.WithholdingTreatment mapWithholdingTreatment(WithholdingTaxEntity.WithholdingTreatment treatment) {
        if (treatment == null) {
            return null;
        }
        return WithholdingTaxInfo.WithholdingTreatment.valueOf(treatment.name());
    }
}
