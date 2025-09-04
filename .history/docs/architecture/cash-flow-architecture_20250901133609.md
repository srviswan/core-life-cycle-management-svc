# Cash Flow Architecture

## Overview

The Cash Flow Generation Engine is a critical component of the Lifecycle Management Service, handling the complex cash flow requirements for synthetic equity swaps. This includes both interest and equity cash flows, with sophisticated multi-stage processing and complex dividend management.

## Cash Flow Types

### 1. Interest Cash Flows

**Interest Accruals**
- Daily interest accrual calculations
- Rate reset processing
- Interest period management
- Compounding and day count conventions

**Interest Payments**
- Scheduled interest payments
- Early termination interest calculations
- Rate adjustment impacts
- Interest settlement processing

**Reset Calculations**
- Floating rate resets
- Rate fixing and application
- Reset date management
- Historical rate tracking

### 2. Equity Cash Flows

**Dividend Cash Flows**
- Dividend declaration processing
- Ex-dividend date management
- Dividend amount calculations
- Dividend timing and frequency

**Corporate Action Cash Flows**
- Stock splits and reverse splits
- Mergers and acquisitions
- Rights issues and offerings
- Spin-offs and special dividends

**Stock Dividend Handling**
- Stock dividend calculations
- Fractional share handling
- Dividend reinvestment options
- Tax implications

## Complex Dividend Management

### Dividend Reinvestment
- Automatic dividend reinvestment
- Manual dividend reinvestment options
- Reinvestment timing and pricing
- Reinvestment fee calculations

### Dividend Withholding Tax
- Foreign dividend withholding tax
- Tax treaty applications
- Tax reclaim processing
- Tax reporting and compliance

### Foreign Dividend Handling
- Currency conversion for foreign dividends
- Exchange rate management
- Cross-border dividend processing
- Regulatory compliance for foreign dividends

### Dividend Timing Complexity
- Declaration date vs. ex-dividend date
- Record date vs. payment date
- Settlement timing variations
- Market-specific dividend calendars

## Cash Flow Stages

### Stage 1: Accrual Stage
```
┌─────────────────────────────────────────────────────────────┐
│                    Accrual Stage                           │
├─────────────────────────────────────────────────────────────┤
│  Interest Accruals          │  Dividend Accruals           │
│  • Daily interest accrual   │  • Dividend declaration      │
│  • Rate reset accruals      │  • Ex-dividend date          │
│  • Period-end accruals      │  • Dividend amount calc      │
│  • Compounding effects      │  • Tax withholding calc      │
├─────────────────────────────────────────────────────────────┤
│  Accrual Processing         │  State Management            │
│  • Lot-level accruals       │  • Accrual state tracking    │
│  • Aggregated accruals      │  • Accrual history           │
│  • Real-time updates        │  • Audit trail               │
└─────────────────────────────────────────────────────────────┘
```

### Stage 2: Realized & Deferred
```
┌─────────────────────────────────────────────────────────────┐
│                Realized & Deferred Stage                   │
├─────────────────────────────────────────────────────────────┤
│  Realization Processing     │  Deferral Management         │
│  • Cash flow realization    │  • Deferred cash flows       │
│  • Settlement preparation   │  • Deferral reasons          │
│  • Payment scheduling       │  • Deferral tracking         │
│  • Confirmation generation  │  • Deferral resolution       │
├─────────────────────────────────────────────────────────────┤
│  State Transitions          │  Event Processing            │
│  • Accrual → Realized       │  • Realization events        │
│  • Realized → Deferred      │  • Deferral events           │
│  • Deferral → Realized      │  • State transition events   │
└─────────────────────────────────────────────────────────────┘
```

### Stage 3: Realized & Unsettled
```
┌─────────────────────────────────────────────────────────────┐
│                Realized & Unsettled Stage                  │
├─────────────────────────────────────────────────────────────┤
│  Settlement Preparation     │  Payment Processing          │
│  • Payment instruction gen  │  • Payment system routing    │
│  • Settlement scheduling    │  • Payment confirmation      │
│  • Payment validation       │  • Payment status tracking   │
│  • Risk checks              │  • Payment retry logic       │
├─────────────────────────────────────────────────────────────┤
│  Integration Management     │  Error Handling              │
│  • Settlement system int    │  • Payment failures          │
│  • Payment gateway int      │  • Retry mechanisms          │
│  • Confirmation system int  │  • Error escalation          │
└─────────────────────────────────────────────────────────────┘
```

### Stage 4: Realized & Settled
```
┌─────────────────────────────────────────────────────────────┐
│                  Realized & Settled Stage                  │
├─────────────────────────────────────────────────────────────┤
│  Settlement Confirmation    │  Post-Settlement Processing  │
│  • Payment confirmation     │  • Settlement reconciliation │
│  • Settlement completion    │  • Post-settlement events    │
│  • Final state update       │  • Audit trail completion    │
│  • Regulatory reporting     │  • Client notification       │
├─────────────────────────────────────────────────────────────┤
│  State Finalization         │  Event Publishing            │
│  • Final state transition   │  • Settlement events         │
│  • State history update     │  • ODS publishing            │
│  • Audit trail finalization │  • Downstream notifications  │
└─────────────────────────────────────────────────────────────┘
```

## Cash Flow Processing Engine

### Core Components

**Cash Flow Calculator**
- Interest calculation engine
- Dividend calculation engine
- Corporate action calculator
- Tax calculation engine

**Stage Manager**
- Multi-stage workflow management
- State transition processing
- Stage-specific business logic
- Error handling and recovery

**Settlement Manager**
- Payment system integration
- Settlement scheduling
- Payment confirmation
- Retry and error handling

**Event Publisher**
- Cash flow event publishing
- ODS integration
- Downstream system notifications
- Regulatory event publishing

### Cash Flow Triggers

**Lot-Level Events**
- **New Lot Creation**: Initial cash flow calculations for new positions
- **Lot Updates**: Recalculation of cash flows for modified lots
- **Lot Modifications**: Adjustment of cash flows for lot changes
- **Lot Closures**: Final cash flow calculations and P&L

**Contract Changes**
- **Interest Rate Index Change**: Recalculation of interest accruals
- **Reset/Payment Date Change**: Adjustment of interest schedules
- **Schedule Modifications**: Update of payment and reset schedules
- **Contract Term Changes**: Impact on cash flow calculations

**Corporate Actions**
- **Dividend Declarations**: New dividend cash flow generation
- **Stock Splits**: Adjustment of position and cash flow calculations
- **Mergers/Acquisitions**: Complex cash flow impact calculations
- **Rights Issues**: New cash flow event generation

**Market Data Changes**
- **Daily Valuation Updates**: Mark-to-market cash flow adjustments
- **Market Price Changes**: P&L impact on cash flows
- **Index Rate Changes**: Interest accrual recalculations
- **Volatility Changes**: Option-based cash flow adjustments

**Trade Events**
- **Position Closures**: Final cash flow calculations
- **Partial Terminations**: Pro-rata cash flow adjustments
- **P&L Calculations**: Cost vs. sale price differences
- **Trade Modifications**: Cash flow recalculation

**Scheduled Events**
- **Interest Reset Dates**: Regular interest rate resets
- **Payment Due Dates**: Scheduled cash flow payments
- **Dividend Payment Dates**: Dividend cash flow processing
- **Corporate Action Dates**: Scheduled corporate action processing

### Processing Flow

```
┌─────────────────────────────────────────────────────────────┐
│                Cash Flow Processing Flow                   │
├─────────────────────────────────────────────────────────────┤
│  Lot-Level Triggers         │  Calculation Engine          │
│  • New lot creation          │  • Interest calculations     │
│  • Lot updates/modifications │  • Dividend calculations     │
│  • Lot closures              │  • P&L calculations         │
│  • Contract changes          │  • Tax calculations          │
│  • Corporate actions         │  • Corporate action calc     │
│  • Market data changes       │  • Valuation adjustments    │
├─────────────────────────────────────────────────────────────┤
│  Multi-stage Processing      │  State Management            │
│  • Accrual processing        │  • State transitions         │
│  • Realization processing    │  • State history             │
│  • Settlement processing     │  • Audit trail               │
│  • Completion processing     │  • Event correlation         │
└─────────────────────────────────────────────────────────────┘
```

## Data Model

### Cash Flow Event Structure

```json
{
  "cashFlowEvent": {
    "eventId": "CF_001",
    "cashFlowType": "DIVIDEND",
    "cashFlowSubType": "ORDINARY_DIVIDEND",
    "contractId": "SWAP_001",
    "constituent": "IBM",
    "eventDate": "2024-01-15",
    "effectiveDate": "2024-01-15",
    "exDate": "2024-01-13",
    "recordDate": "2024-01-14",
    "paymentDate": "2024-01-20",
    "stages": [
      {
        "stage": "ACCRUAL",
        "status": "COMPLETED",
        "amount": 1000.00,
        "currency": "USD",
        "timestamp": "2024-01-15T10:00:00Z"
      },
      {
        "stage": "REALIZED_DEFERRED",
        "status": "IN_PROGRESS",
        "amount": 1000.00,
        "currency": "USD",
        "timestamp": "2024-01-15T10:05:00Z"
      }
    ],
    "lotLevelDetails": [
      {
        "lotId": "LOT_001",
        "quantity": 1000,
        "costPrice": 150.00,
        "currentPrice": 155.00,
        "interestAccrual": 50.00,
        "dividendAmount": 400.00,
        "taxAmount": 40.00,
        "netAmount": 360.00,
        "p&l": 5000.00,
        "triggerEvent": "LOT_CREATION",
        "triggerDate": "2024-01-15T10:00:00Z"
      }
    ],
    "taxDetails": {
      "withholdingTaxRate": 0.10,
      "withholdingTaxAmount": 100.00,
      "taxJurisdiction": "US",
      "taxTreaty": "US_UK_TREATY"
    }
  }
}
```

### Cash Flow State Model

```json
{
  "cashFlowState": {
    "stateId": "CF_STATE_001",
    "cashFlowId": "CF_001",
    "currentStage": "REALIZED_UNSETTLED",
    "stageHistory": [
      {
        "stage": "ACCRUAL",
        "startTime": "2024-01-15T10:00:00Z",
        "endTime": "2024-01-15T10:05:00Z",
        "status": "COMPLETED"
      },
      {
        "stage": "REALIZED_DEFERRED",
        "startTime": "2024-01-15T10:05:00Z",
        "endTime": "2024-01-15T10:10:00Z",
        "status": "COMPLETED"
      },
      {
        "stage": "REALIZED_UNSETTLED",
        "startTime": "2024-01-15T10:10:00Z",
        "endTime": null,
        "status": "IN_PROGRESS"
      }
    ],
    "amounts": {
      "grossAmount": 1000.00,
      "taxAmount": 100.00,
      "netAmount": 900.00,
      "currency": "USD"
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

## Performance Requirements

### Throughput
- **Interest Cash Flows**: 100K+ daily interest calculations
- **Dividend Cash Flows**: 50K+ daily dividend processing
- **Corporate Actions**: 10K+ daily corporate action events
- **Settlement Processing**: 25K+ daily settlements

### Latency
- **Accrual Processing**: <50ms for real-time accruals
- **Realization Processing**: <100ms for cash flow realization
- **Settlement Processing**: <200ms for settlement preparation
- **Event Publishing**: <10ms for ODS publishing

### Scalability
- **Peak Load**: 4x normal volume during corporate action periods
- **Concurrent Processing**: Support for 1000+ concurrent cash flows
- **Data Growth**: Support for 4x growth in cash flow volume

## Error Handling & Resilience

### Retry Mechanisms
- **Exponential Backoff**: Progressive retry delays
- **Circuit Breakers**: Protection against cascading failures
- **Dead Letter Queues**: Failed event handling
- **Manual Intervention**: Human oversight for complex cases

### Error Recovery
- **State Recovery**: Automatic state recovery from failures
- **Partial Processing**: Handle partial cash flow failures
- **Compensation Actions**: Rollback mechanisms for failed operations
- **Audit Trail**: Complete audit trail for error investigation

## Regulatory Compliance

### Reporting Requirements
- **MiFID II**: Cash flow transaction reporting
- **Dodd-Frank**: Swap cash flow reporting
- **Tax Reporting**: Dividend and interest tax reporting
- **Settlement Reporting**: Settlement status reporting

### Audit Requirements
- **Complete Audit Trail**: All cash flow state changes
- **Event Correlation**: Link cash flows to underlying events
- **Data Lineage**: Track data flow and transformations
- **Compliance Monitoring**: Real-time compliance checking

## Integration Points

### Internal Integrations
- **Position Management**: Lot-level position updates
- **Contract Management**: Contract term validation
- **Event Processing**: Lifecycle event correlation
- **State Management**: State transition processing

### External Integrations
- **Payment Systems**: SWIFT, ACH, wire transfers
- **Tax Systems**: Tax calculation and reporting
- **Regulatory Systems**: Compliance and reporting
- **Client Systems**: Client notifications and confirmations
