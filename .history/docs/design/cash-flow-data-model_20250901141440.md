# Cash Flow Data Model

## Overview

This document defines the complete data model for the Cash Flow Engine, including input requirements, internal data structures, and output specifications for the Core Life Cycle Management Service.

**Key Design Principle**: The Cash Flow Service is designed as a **self-contained service** that receives all necessary data in a single request and does not depend on external data sources during processing. This ensures:
- **Deterministic calculations** based on the provided reference date range
- **No external dependencies** during cash flow processing
- **Complete audit trail** of all inputs used in calculations
- **Reproducible results** for any given date range
- **Historical recalculation** support for long-running contracts

## Input Data Model

### Reference Date Range Concept

The cash flow service uses a **reference date range** approach to handle both current and historical calculations:

1. **Calculation Date Range**: The period for which cash flows are being calculated
   - **From Date**: Start of calculation period (e.g., contract inception, last reset)
   - **To Date**: End of calculation period (e.g., current date, maturity)
2. **Historical Recalculation**: Support for recalculating cash flows from inception
   - **5-year contracts**: Can recalculate entire contract history
   - **Incremental updates**: Can recalculate specific periods
   - **Audit requirements**: Can reproduce any historical calculation

### 1. Cash Flow Calculation Request

```json
{
  "cashFlowRequest": {
    "requestId": "CF_REQ_001",
    "calculationType": "HISTORICAL_RECALCULATION",
    "dateRange": {
      "fromDate": "2019-01-01",
      "toDate": "2024-01-15",
      "calculationFrequency": "DAILY"
    },
    "contracts": [
      {
        "contractId": "SWAP_001",
        "basketContractId": "BASKET_001",
        "tradeDate": "2019-01-01",
        "effectiveDate": "2019-01-01",
        "maturityDate": "2024-01-01",
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
          "currency": "USD"
        },
        "interestLeg": {
          "rateType": "FLOATING",
          "index": "LIBOR_3M",
          "spread": 0.0025,
          "resetFrequency": "QUARTERLY",
          "dayCountConvention": "ACT_360",
          "currency": "USD",
          "notionalAmount": 1000000
        }
      }
    ],
    "historicalData": {
      "marketData": {
        "dateRange": {
          "fromDate": "2019-01-01",
          "toDate": "2024-01-15"
        },
        "securities": [
          {
            "symbol": "IBM",
            "prices": [
              {"date": "2019-01-01", "price": 120.00},
              {"date": "2019-01-02", "price": 121.00},
              {"date": "2024-01-15", "price": 155.00}
            ],
            "currency": "USD"
          }
        ],
        "rates": [
          {
            "index": "LIBOR_3M",
            "rates": [
              {"date": "2019-01-01", "rate": 0.0250},
              {"date": "2019-04-01", "rate": 0.0275},
              {"date": "2024-01-15", "rate": 0.0525}
            ],
            "currency": "USD"
          }
        ],
        "dividends": [
          {
            "symbol": "IBM",
            "dividends": [
              {
                "exDate": "2019-03-15",
                "paymentDate": "2019-03-20",
                "amount": 1.20,
                "currency": "USD"
              },
              {
                "exDate": "2024-01-13",
                "paymentDate": "2024-01-20",
                "amount": 1.50,
                "currency": "USD"
              }
            ]
          }
        ]
      },
      "positions": [
        {
          "lotId": "LOT_001",
          "contractId": "SWAP_001",
          "underlying": "IBM",
          "quantity": 1000,
          "costPrice": 120.00,
          "costDate": "2019-01-01",
          "positionHistory": [
            {
              "date": "2019-01-01",
              "quantity": 1000,
              "costPrice": 120.00,
              "status": "ACTIVE"
            },
            {
              "date": "2024-01-15",
              "quantity": 1000,
              "costPrice": 120.00,
              "status": "ACTIVE"
            }
          ]
        }
      ],
      "corporateActions": [
        {
          "actionId": "CA_001",
          "symbol": "IBM",
          "actionType": "DIVIDEND",
          "exDate": "2024-01-13",
          "paymentDate": "2024-01-20",
          "amount": 1.50,
          "currency": "USD"
        }
      ],
      "tradeEvents": [
        {
          "eventId": "TE_001",
          "eventType": "LOT_CLOSURE",
          "lotId": "LOT_001",
          "contractId": "SWAP_001",
          "eventDate": "2024-01-15",
          "salePrice": 160.00,
          "saleQuantity": 1000,
          "realizedP&l": 40000.00
        }
      ]
    }
  }
}
```

## Output Data Model

### 1. Cash Flow Calculation Response

```json
{
  "cashFlowResponse": {
    "requestId": "CF_REQ_001",
    "calculationDate": "2024-01-15",
    "dateRange": {
      "fromDate": "2019-01-01",
      "toDate": "2024-01-15"
    },
    "calculationType": "HISTORICAL_RECALCULATION",
    "cashFlows": [
      {
        "date": "2019-01-01",
        "contractId": "SWAP_001",
        "lotId": "LOT_001",
        "cashFlowType": "INTEREST_ACCRUAL",
        "equityLeg": {
          "unrealizedP&l": 0.00,
          "realizedP&l": 0.00,
          "totalP&l": 0.00
        },
        "interestLeg": {
          "accruedInterest": 0.00,
          "interestRate": 0.0250,
          "notionalAmount": 1000000
        },
        "totalCashFlow": 0.00,
        "currency": "USD"
      },
      {
        "date": "2019-03-20",
        "contractId": "SWAP_001",
        "lotId": "LOT_001",
        "cashFlowType": "DIVIDEND",
        "equityLeg": {
          "dividendAmount": 1200.00,
          "withholdingTax": 120.00,
          "netDividend": 1080.00
        },
        "interestLeg": {
          "accruedInterest": 0.00,
          "interestRate": 0.0250,
          "notionalAmount": 1000000
        },
        "totalCashFlow": 1080.00,
        "currency": "USD"
      },
      {
        "date": "2024-01-15",
        "contractId": "SWAP_001",
        "lotId": "LOT_001",
        "cashFlowType": "LOT_CLOSURE",
        "equityLeg": {
          "unrealizedP&l": 35000.00,
          "realizedP&l": 40000.00,
          "totalP&l": 75000.00
        },
        "interestLeg": {
          "accruedInterest": 50.00,
          "interestRate": 0.0525,
          "notionalAmount": 1000000
        },
        "totalCashFlow": 75050.00,
        "currency": "USD"
      }
    ],
    "summary": {
      "totalCashFlows": 76130.00,
      "totalInterest": 1250.00,
      "totalDividends": 1080.00,
      "totalP&l": 75000.00,
      "currency": "USD"
    }
  }
}
```

### 2. Settlement Instructions

```json
{
  "settlementInstructions": {
    "requestId": "CF_REQ_001",
    "settlementDate": "2024-01-17",
    "instructions": [
      {
        "instructionId": "SI_001",
        "contractId": "SWAP_001",
        "lotId": "LOT_001",
        "cashFlowType": "LOT_CLOSURE",
        "amount": 75050.00,
        "currency": "USD",
        "counterparty": "CLIENT_ABC",
        "settlementMethod": "CASH",
        "status": "PENDING"
      }
    ]
  }
}
```

## Self-Contained Service Architecture

### Design Principles

1. **Complete Data Input**: All necessary data is provided in a single request
   - Contract terms and conditions
   - Historical market data for the entire date range
   - Position history and changes
   - Corporate actions and trade events

2. **No External Dependencies**: The service does not fetch data during processing
   - No database queries during calculation
   - No external API calls
   - No real-time market data feeds
   - All calculations based on provided inputs

3. **Deterministic Processing**: Same inputs always produce same outputs
   - Date range ensures consistent calculations
   - No time-dependent external factors
   - Reproducible results for audit purposes
   - Predictable performance characteristics

4. **Historical Recalculation**: Support for long-running contracts
   - 5-year contract recalculation from inception
   - Incremental updates for specific periods
   - Complete audit trail for regulatory compliance
   - Performance optimization for large date ranges

### Service Interface

```json
{
  "cashFlowCalculationRequest": {
    "requestId": "CF_REQ_001",
    "calculationType": "HISTORICAL_RECALCULATION",
    "dateRange": {
      "fromDate": "2019-01-01",
      "toDate": "2024-01-15",
      "calculationFrequency": "DAILY"
    },
    "data": {
      "contracts": [...],
      "historicalData": {
        "marketData": {...},
        "positions": [...],
        "corporateActions": [...],
        "tradeEvents": [...]
      }
    }
  }
}
```

### Benefits of Self-Contained Design

1. **Performance**: No network latency during calculations
2. **Reliability**: No dependency on external service availability
3. **Scalability**: Can process multiple requests in parallel
4. **Testing**: Easy to unit test with mock data
5. **Debugging**: Complete visibility into all inputs and outputs
6. **Compliance**: Full audit trail for regulatory requirements
7. **Historical Support**: Can recalculate entire contract history

## Performance Requirements

### Throughput
- **Input Processing**: 1M+ daily lot-level inputs
- **Cash Flow Calculations**: 1M+ daily calculations
- **Historical Recalculation**: 5-year contracts in <5 minutes
- **Event Generation**: 1M+ daily events
- **Output Publishing**: 1M+ daily outputs

### Latency
- **Input Processing**: <50ms for lot-level inputs
- **Cash Flow Calculation**: <100ms for complex calculations
- **Historical Recalculation**: <5 minutes for 5-year contracts
- **Event Publishing**: <10ms for ODS publishing
- **Output Generation**: <50ms for downstream outputs

### Data Quality
- **Accuracy**: 99.99% calculation accuracy
- **Completeness**: 100% lot-level coverage
- **Consistency**: ACID compliance for state updates
- **Auditability**: Complete audit trail for all calculations
- **Historical Integrity**: Reproducible results for any date range
