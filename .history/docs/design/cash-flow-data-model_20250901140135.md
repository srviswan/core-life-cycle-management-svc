# Cash Flow Data Model

## Overview

This document defines the complete data model for the Cash Flow Engine, including input requirements, internal data structures, and output specifications for the Core Life Cycle Management Service.

## Input Data Model

### 1. Swap Contract Input

```json
{
  "swapContract": {
    "contractId": "SWAP_001",
    "basketContractId": "BASKET_001",
    "tradeDate": "2024-01-01",
    "effectiveDate": "2024-01-01",
    "maturityDate": "2025-01-01",
    "notionalAmount": 1000000,
    "currency": "USD",
    "counterparties": {
      "buyer": "CLIENT_ABC",
      "seller": "BANK_XYZ"
    },
    "equityLeg": {
      "underlying": "IBM",
      "quantity": 1000,
      "weight": 0.4,
      "dividendTreatment": "REINVEST",
      "corporateActionHandling": "AUTOMATIC",
      "currency": "USD",
      "fxRate": 1.0
    },
    "interestLeg": {
      "rateType": "FLOATING",
      "index": "LIBOR_3M",
      "spread": 0.0025,
      "resetFrequency": "QUARTERLY",
      "dayCountConvention": "ACT_360",
      "currency": "USD",
      "notionalAmount": 1000000
    },
    "settlementTerms": {
      "settlementCurrency": "USD",
      "settlementMethod": "CASH",
      "paymentFrequency": "QUARTERLY"
    },
    "regulatoryInfo": {
      "miFIDClassification": "NON_ELECTRONIC",
      "doddFrankCategory": "SWAP_DEALER",
      "reportingJurisdictions": ["US", "EU"]
    }
  }
}
```

### 2. Lot-Level Position Input

```json
{
  "lotPosition": {
    "lotId": "LOT_001",
    "contractId": "SWAP_001",
    "basketContractId": "BASKET_001",
    "underlying": "IBM",
    "quantity": 1000,
    "costPrice": 150.00,
    "costDate": "2024-01-01",
    "currentPrice": 155.00,
    "priceDate": "2024-01-15",
    "positionStatus": "ACTIVE",
    "lotType": "NEW_LOT",
    "equityLegTerms": {
      "weight": 0.4,
      "dividendTreatment": "REINVEST",
      "currency": "USD",
      "fxRate": 1.0
    },
    "interestLegTerms": {
      "currentRate": 0.0525,
      "lastResetDate": "2024-01-01",
      "nextResetDate": "2024-04-01",
      "notionalAmount": 400000,
      "currency": "USD"
    },
    "economicTerms": {
      "dividendYield": 0.025,
      "lastDividendDate": "2023-12-15",
      "nextDividendDate": "2024-03-15"
    },
    "taxInfo": {
      "withholdingTaxRate": 0.10,
      "taxJurisdiction": "US",
      "taxTreaty": "US_UK_TREATY"
    }
  }
}
```

### 3. Market Data Input

```json
{
  "marketData": {
    "dataId": "MD_001",
    "timestamp": "2024-01-15T16:00:00Z",
    "dataType": "DAILY_CLOSE",
    "securities": [
      {
        "symbol": "IBM",
        "price": 155.00,
        "currency": "USD",
        "priceType": "CLOSE",
        "volume": 5000000,
        "date": "2024-01-15"
      }
    ],
    "rates": [
      {
        "index": "LIBOR_3M",
        "rate": 0.0525,
        "currency": "USD",
        "date": "2024-01-15"
      }
    ],
    "fxRates": [
      {
        "fromCurrency": "USD",
        "toCurrency": "EUR",
        "rate": 0.85,
        "date": "2024-01-15"
      }
    ],
    "dividends": [
      {
        "symbol": "IBM",
        "dividendAmount": 1.50,
        "exDate": "2024-01-13",
        "recordDate": "2024-01-14",
        "paymentDate": "2024-01-20",
        "currency": "USD"
      }
    ]
  }
}
```

### 4. Corporate Action Input

```json
{
  "corporateAction": {
    "actionId": "CA_001",
    "actionType": "DIVIDEND",
    "security": "IBM",
    "actionDate": "2024-01-15",
    "effectiveDate": "2024-01-15",
    "exDate": "2024-01-13",
    "recordDate": "2024-01-14",
    "paymentDate": "2024-01-20",
    "details": {
      "dividendAmount": 1.50,
      "currency": "USD",
      "dividendType": "ORDINARY",
      "taxRate": 0.10
    },
    "affectedPositions": [
      {
        "lotId": "LOT_001",
        "contractId": "SWAP_001",
        "quantity": 1000,
        "impactAmount": 1500.00
      }
    ]
  }
}
```

### 4. Basket Contract Breakdown

```json
{
  "basketBreakdown": {
    "basketContractId": "BASKET_001",
    "breakdownDate": "2024-01-15",
    "constituents": [
      {
        "underlying": "IBM",
        "weight": 0.4,
        "quantity": 1000,
        "notionalAmount": 400000,
        "currency": "USD",
        "fxRate": 1.0,
        "individualContractId": "SWAP_IBM_001"
      },
      {
        "underlying": "AAPL",
        "weight": 0.6,
        "quantity": 1500,
        "notionalAmount": 600000,
        "currency": "USD",
        "fxRate": 1.0,
        "individualContractId": "SWAP_AAPL_001"
      }
    ],
    "totalNotional": 1000000,
    "basketCurrency": "USD",
    "rebalancingInfo": {
      "lastRebalanceDate": "2024-01-01",
      "nextRebalanceDate": "2024-04-01",
      "rebalancingFrequency": "QUARTERLY"
    }
  }
}
```

### 5. Trade Event Input

```json
{
  "tradeEvent": {
    "eventId": "TE_001",
    "eventType": "LOT_CLOSURE",
    "lotId": "LOT_001",
    "contractId": "SWAP_001",
    "basketContractId": "BASKET_001",
    "underlying": "IBM",
    "eventDate": "2024-01-15",
    "effectiveDate": "2024-01-15",
    "details": {
      "closureType": "FULL_CLOSURE",
      "salePrice": 160.00,
      "saleQuantity": 1000,
      "costPrice": 150.00,
      "costQuantity": 1000,
      "realizedP&l": 10000.00
    },
    "settlementInfo": {
      "settlementDate": "2024-01-17",
      "settlementCurrency": "USD",
      "settlementMethod": "CASH"
    }
  }
}
```

## Internal Data Model

### 1. Cash Flow State Model

```json
{
  "cashFlowState": {
    "stateId": "CF_STATE_001",
    "cashFlowId": "CF_001",
    "lotId": "LOT_001",
    "contractId": "SWAP_001",
    "basketContractId": "BASKET_001",
    "underlying": "IBM",
    "currentStage": "REALIZED_UNSETTLED",
    "cashFlowType": "P&L",
    "cashFlowSubType": "DAILY_P&L",
    "stageHistory": [
      {
        "stage": "ACCRUAL",
        "startTime": "2024-01-15T10:00:00Z",
        "endTime": "2024-01-15T10:05:00Z",
        "status": "COMPLETED",
        "amount": 1000.00,
        "currency": "USD"
      },
      {
        "stage": "REALIZED_DEFERRED",
        "startTime": "2024-01-15T10:05:00Z",
        "endTime": "2024-01-15T10:10:00Z",
        "status": "COMPLETED",
        "amount": 1000.00,
        "currency": "USD"
      },
      {
        "stage": "REALIZED_UNSETTLED",
        "startTime": "2024-01-15T10:10:00Z",
        "endTime": null,
        "status": "IN_PROGRESS",
        "amount": 1000.00,
        "currency": "USD"
      }
    ],
    "amounts": {
      "grossAmount": 1000.00,
      "taxAmount": 100.00,
      "netAmount": 900.00,
      "currency": "USD"
    },
    "equityLegDetails": {
      "unrealizedP&l": 5000.00,
      "realizedP&l": 0.00,
      "totalP&l": 5000.00,
      "costPrice": 150.00,
      "currentPrice": 155.00,
      "weight": 0.4,
      "fxRate": 1.0
    },
    "interestLegDetails": {
      "accruedInterest": 50.00,
      "interestRate": 0.0525,
      "lastResetDate": "2024-01-01",
      "nextResetDate": "2024-04-01",
      "notionalAmount": 400000,
      "currency": "USD"
    },
    "dividendDetails": {
      "dividendAmount": 1500.00,
      "withholdingTax": 150.00,
      "netDividend": 1350.00,
      "dividendDate": "2024-01-20"
    },
    "settlementDetails": {
      "paymentSystem": "SWIFT",
      "paymentReference": "PAY_REF_001",
      "settlementDate": "2024-01-20",
      "status": "PENDING"
    }
  }
}
```

### 2. Cash Flow Calculation Model

```json
{
  "cashFlowCalculation": {
    "calculationId": "CALC_001",
    "lotId": "LOT_001",
    "contractId": "SWAP_001",
    "basketContractId": "BASKET_001",
    "underlying": "IBM",
    "calculationDate": "2024-01-15",
    "calculationType": "DAILY_P&L",
    "inputs": {
      "equityLeg": {
        "quantity": 1000,
        "costPrice": 150.00,
        "currentPrice": 155.00,
        "weight": 0.4,
        "fxRate": 1.0
      },
      "interestLeg": {
        "rate": 0.0525,
        "daysAccrued": 14,
        "dayCountConvention": "ACT_360",
        "notionalAmount": 400000,
        "currency": "USD"
      },
      "dividend": {
        "dividendAmount": 1.50,
        "exDate": "2024-01-13",
        "paymentDate": "2024-01-20"
      },
      "tax": {
        "withholdingTaxRate": 0.10,
        "taxJurisdiction": "US"
      }
    },
    "calculations": {
      "equityLeg": {
        "unrealizedP&l": 5000.00,
        "realizedP&l": 0.00,
        "totalP&l": 5000.00,
        "fxImpact": 0.00
      },
      "interestLeg": {
        "accruedInterest": 50.00,
        "interestAmount": 50.00,
        "notionalAmount": 400000
      },
      "dividend": {
        "grossDividend": 1500.00,
        "withholdingTax": 150.00,
        "netDividend": 1350.00
      },
      "total": {
        "grossAmount": 1550.00,
        "taxAmount": 150.00,
        "netAmount": 1400.00,
        "equityLegAmount": 5000.00,
        "interestLegAmount": 50.00,
        "dividendAmount": 1500.00
      }
    },
    "outputs": {
      "cashFlowEvents": ["CF_EVENT_001", "CF_EVENT_002"],
      "stateUpdates": ["STATE_UPDATE_001"],
      "settlementInstructions": ["SETTLEMENT_001"]
    }
  }
}
```

### 3. Cash Flow Event Model

```json
{
  "cashFlowEvent": {
    "eventId": "CF_EVENT_001",
    "eventType": "CASH_FLOW_GENERATED",
    "lotId": "LOT_001",
    "contractId": "SWAP_001",
    "eventDate": "2024-01-15T10:00:00Z",
    "effectiveDate": "2024-01-15",
    "cashFlowType": "P&L",
    "cashFlowSubType": "DAILY_P&L",
    "stage": "REALIZED_UNSETTLED",
    "amounts": {
      "grossAmount": 1000.00,
      "taxAmount": 100.00,
      "netAmount": 900.00,
      "currency": "USD"
    },
    "p&lDetails": {
      "unrealizedP&l": 5000.00,
      "realizedP&l": 0.00,
      "totalP&l": 5000.00,
      "costPrice": 150.00,
      "currentPrice": 155.00,
      "quantity": 1000
    },
    "interestDetails": {
      "accruedInterest": 50.00,
      "interestRate": 0.0525,
      "resetDate": "2024-04-01"
    },
    "dividendDetails": {
      "dividendAmount": 1500.00,
      "withholdingTax": 150.00,
      "netDividend": 1350.00,
      "dividendDate": "2024-01-20"
    },
    "triggerEvent": {
      "eventType": "DAILY_CLOSE",
      "eventId": "MD_001",
      "timestamp": "2024-01-15T16:00:00Z"
    },
    "metadata": {
      "calculationId": "CALC_001",
      "version": "1.0",
      "createdBy": "SYSTEM",
      "createdAt": "2024-01-15T10:00:00Z"
    }
  }
}
```

## Output Data Model

### 1. Cash Flow Data Product

```json
{
  "cashFlowDataProduct": {
    "productId": "CF_DP_001",
    "productType": "CASH_FLOW_DATA",
    "timestamp": "2024-01-15T10:00:00Z",
    "version": "1.0",
    "data": {
      "cashFlows": [
        {
          "cashFlowId": "CF_001",
          "lotId": "LOT_001",
          "contractId": "SWAP_001",
          "cashFlowType": "P&L",
          "cashFlowSubType": "DAILY_P&L",
          "stage": "REALIZED_UNSETTLED",
          "eventDate": "2024-01-15",
          "effectiveDate": "2024-01-15",
          "amounts": {
            "grossAmount": 1000.00,
            "taxAmount": 100.00,
            "netAmount": 900.00,
            "currency": "USD"
          },
          "p&lDetails": {
            "unrealizedP&l": 5000.00,
            "realizedP&l": 0.00,
            "totalP&l": 5000.00
          },
          "interestDetails": {
            "accruedInterest": 50.00,
            "interestRate": 0.0525
          },
          "dividendDetails": {
            "dividendAmount": 1500.00,
            "netDividend": 1350.00
          },
          "settlementDetails": {
            "settlementDate": "2024-01-20",
            "status": "PENDING"
          }
        }
      ],
      "aggregations": {
        "totalP&l": 5000.00,
        "totalInterest": 50.00,
        "totalDividend": 1500.00,
        "totalTax": 100.00,
        "totalNetAmount": 900.00
      }
    }
  }
}
```

### 2. Settlement Instruction Output

```json
{
  "settlementInstruction": {
    "instructionId": "SI_001",
    "cashFlowId": "CF_001",
    "lotId": "LOT_001",
    "contractId": "SWAP_001",
    "instructionType": "PAYMENT",
    "instructionDate": "2024-01-15",
    "settlementDate": "2024-01-20",
    "amounts": {
      "grossAmount": 1000.00,
      "taxAmount": 100.00,
      "netAmount": 900.00,
      "currency": "USD"
    },
    "parties": {
      "payer": "BANK_XYZ",
      "payee": "CLIENT_ABC",
      "payerAccount": "ACC_001",
      "payeeAccount": "ACC_002"
    },
    "paymentDetails": {
      "paymentMethod": "SWIFT",
      "paymentReference": "PAY_REF_001",
      "paymentInstructions": "Cash flow payment for IBM swap"
    },
    "status": "PENDING",
    "metadata": {
      "createdBy": "CASH_FLOW_ENGINE",
      "createdAt": "2024-01-15T10:00:00Z",
      "version": "1.0"
    }
  }
}
```

### 3. P&L Report Output

```json
{
  "p&lReport": {
    "reportId": "P&L_RPT_001",
    "reportDate": "2024-01-15",
    "reportType": "DAILY_P&L",
    "contractId": "SWAP_001",
    "data": {
      "lotLevelP&l": [
        {
          "lotId": "LOT_001",
          "quantity": 1000,
          "costPrice": 150.00,
          "currentPrice": 155.00,
          "unrealizedP&l": 5000.00,
          "realizedP&l": 0.00,
          "totalP&l": 5000.00,
          "p&lDate": "2024-01-15"
        }
      ],
      "aggregatedP&l": {
        "totalUnrealizedP&l": 5000.00,
        "totalRealizedP&l": 0.00,
        "totalP&l": 5000.00,
        "totalCostValue": 150000.00,
        "totalMarketValue": 155000.00
      },
      "p&lAttribution": {
        "priceChange": 5000.00,
        "interestAccrual": 50.00,
        "dividendIncome": 1500.00,
        "taxImpact": -100.00
      }
    }
  }
}
```

## Data Flow Specifications

### Input Processing

1. **Swap Blotter Processing**
   - Validate swap terms and conditions
   - Extract interest rate and equity terms
   - Identify cash flow calculation requirements
   - Create initial cash flow state

2. **Lot-Level Position Processing**
   - Calculate lot-level cash flows
   - Update position-based calculations
   - Trigger cash flow recalculation
   - Maintain lot-level state

3. **Market Data Processing**
   - Update prices for P&L calculations
   - Process rate changes for interest calculations
   - Handle dividend declarations
   - Trigger daily P&L calculations

4. **Corporate Action Processing**
   - Identify affected positions
   - Calculate cash flow impact
   - Generate corporate action cash flows
   - Update position calculations

5. **Trade Event Processing**
   - Calculate realized P&L
   - Generate settlement cash flows
   - Update position status
   - Create final cash flow events

### Output Generation

1. **Cash Flow Events**
   - Generate lot-level cash flow events
   - Create aggregated cash flow events
   - Publish to ODS for downstream consumption
   - Maintain event ordering and correlation

2. **Settlement Instructions**
   - Create payment instructions
   - Generate settlement confirmations
   - Send to settlement systems
   - Track settlement status

3. **P&L Reports**
   - Generate daily P&L reports
   - Create trade-level P&L reports
   - Calculate P&L attribution
   - Distribute to risk and finance systems

## Performance Requirements

### Throughput
- **Input Processing**: 1M+ daily lot-level inputs
- **Cash Flow Calculations**: 1M+ daily calculations
- **Event Generation**: 1M+ daily events
- **Output Publishing**: 1M+ daily outputs

### Latency
- **Input Processing**: <50ms for lot-level inputs
- **Cash Flow Calculation**: <100ms for complex calculations
- **Event Publishing**: <10ms for ODS publishing
- **Output Generation**: <50ms for downstream outputs

### Data Quality
- **Accuracy**: 99.99% calculation accuracy
- **Completeness**: 100% lot-level coverage
- **Consistency**: ACID compliance for state updates
- **Auditability**: Complete audit trail for all calculations
