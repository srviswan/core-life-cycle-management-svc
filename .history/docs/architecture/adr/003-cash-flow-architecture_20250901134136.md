# ADR-003: Cash Flow Architecture

## Status

**Accepted** - This decision has been approved and will be implemented.

## Context

The Core Life Cycle Management Service needs to handle complex cash flow generation for synthetic equity swaps. The requirements include:

- **Triple Cash Flow Types**: Interest cash flows, equity cash flows, and P&L calculations
- **P&L Requirements**: Daily P&L (mark-to-market) and trade-level P&L (realized) at lot level
- **Complex Dividend Management**: Dividend reinvestment, withholding tax, foreign dividend handling, timing complexity
- **Multi-stage Processing**: Cash flows progress through multiple stages (Accrual → Realized & Deferred → Realized & Unsettled → Realized & Settled)
- **High Performance**: Support for 1M+ daily P&L calculations, 100K+ daily interest calculations, and 50K+ daily dividend processing
- **Regulatory Compliance**: Tax reporting, settlement reporting, audit trail requirements
- **Integration Complexity**: Multiple payment systems, tax systems, regulatory systems

The key question is how to design the cash flow architecture to handle this complexity while maintaining performance and regulatory compliance.

## Decision

**Multi-stage Cash Flow Processing Engine with Specialized Components**

We will implement a sophisticated cash flow architecture with:

1. **Triple Cash Flow Types**: Separate processing for interest, equity, and P&L cash flows
2. **P&L Processing**: Daily mark-to-market and trade-level realized P&L calculations
3. **Multi-stage Workflow**: Four-stage processing with state management
4. **Complex Dividend Management**: Specialized handling for dividend complexity
5. **Event-driven Processing**: Asynchronous processing with state transitions
6. **Lot-level Calculations**: Maintain economic accuracy at the lowest granularity

## Rationale

### Why Multi-stage Processing?

1. **Business Complexity**: Cash flows naturally progress through multiple stages
   - Accrual: Initial calculation and recognition
   - Realized & Deferred: Cash flow realization with potential deferrals
   - Realized & Unsettled: Settlement preparation and processing
   - Realized & Settled: Final settlement and completion

2. **Regulatory Requirements**: Different stages require different regulatory reporting
   - Accrual stage: P&L and risk reporting
   - Settlement stage: Payment and settlement reporting
   - Completion stage: Final regulatory reporting

3. **Error Handling**: Multi-stage allows for better error recovery
   - Stage-specific error handling
   - Partial processing capabilities
   - Rollback and compensation actions

### Why Dual Cash Flow Types?

1. **Different Processing Logic**: Interest and equity cash flows have fundamentally different characteristics
   - Interest: Regular, predictable, rate-based calculations
   - Equity: Event-driven, complex, corporate action dependent

2. **Performance Optimization**: Specialized processing for each type
   - Interest: Batch processing for regular calculations
   - Equity: Event-driven processing for corporate actions

3. **Business Rules**: Different business rules and validation requirements
   - Interest: Rate validation, day count conventions
   - Equity: Corporate action validation, tax implications

### Why P&L Processing?

1. **Business Requirements**: Financing business requires comprehensive P&L tracking
   - Daily mark-to-market P&L for risk management
   - Trade-level realized P&L for performance measurement
   - Lot-level P&L for granular tracking
   - Cost basis tracking for tax purposes

2. **Risk Management**: Real-time P&L for risk monitoring
   - Daily P&L for position monitoring
   - Unrealized P&L for risk assessment
   - Realized P&L for performance analysis
   - P&L attribution for analysis

3. **Regulatory Requirements**: P&L reporting for compliance
   - Daily P&L reporting
   - Realized P&L tracking
   - P&L attribution reporting
   - Performance measurement

### Why Complex Dividend Management?

1. **Business Requirements**: Synthetic swaps require sophisticated dividend handling
   - Dividend reinvestment options
   - Withholding tax calculations
   - Foreign dividend processing
   - Timing complexity management

2. **Regulatory Compliance**: Tax and regulatory requirements
   - Tax treaty applications
   - Cross-border dividend reporting
   - Withholding tax compliance

3. **Client Requirements**: Different client preferences and requirements
   - Automatic vs. manual reinvestment
   - Tax optimization strategies
   - Currency preferences

## Consequences

### Positive Consequences

1. **Business Accuracy**: Handles complex cash flow requirements accurately
2. **Regulatory Compliance**: Built-in support for regulatory reporting
3. **Performance**: Optimized processing for different cash flow types
4. **Error Handling**: Robust error handling and recovery mechanisms
5. **Scalability**: Support for high-volume cash flow processing

### Negative Consequences

1. **Complexity**: More complex architecture and implementation
2. **Development Time**: Longer development time for sophisticated features
3. **Testing Complexity**: More complex testing requirements
4. **Maintenance**: More complex maintenance and support requirements

### Mitigation Strategies

1. **Phased Implementation**: Implement stages incrementally
2. **Comprehensive Testing**: Thorough testing at each stage
3. **Documentation**: Detailed documentation and training
4. **Monitoring**: Comprehensive monitoring and alerting

## Alternatives Considered

### Alternative 1: Simple Cash Flow Processing
- **Pros**: Simple implementation, faster development
- **Cons**: Would not handle complex requirements, regulatory non-compliance
- **Rejection Reason**: Does not meet business requirements for complex cash flows

### Alternative 2: Separate Services for Each Cash Flow Type
- **Pros**: Clear separation of concerns, independent scaling
- **Cons**: Complex integration, data consistency challenges
- **Rejection Reason**: High coupling between cash flow types makes separate services inefficient

### Alternative 3: Batch Processing Only
- **Pros**: Simple processing model, predictable performance
- **Cons**: Not suitable for real-time requirements, poor user experience
- **Rejection Reason**: Does not meet real-time processing requirements

## Implementation Notes

### Cash Flow Types

**Interest Cash Flows**
- Daily interest accrual calculations
- Rate reset processing
- Interest period management
- Compounding and day count conventions
- Scheduled interest payments
- Early termination interest calculations

**Equity Cash Flows**
- Dividend declaration processing
- Corporate action cash flows
- Stock dividend handling
- Dividend reinvestment options
- Withholding tax calculations
- Foreign dividend processing

### Multi-stage Processing

**Stage 1: Accrual**
- Initial cash flow calculation
- Lot-level accrual processing
- Real-time updates
- Audit trail creation

**Stage 2: Realized & Deferred**
- Cash flow realization
- Deferral management
- Settlement preparation
- State transition processing

**Stage 3: Realized & Unsettled**
- Settlement processing
- Payment system integration
- Payment validation
- Error handling and retry

**Stage 4: Realized & Settled**
- Settlement confirmation
- Final state update
- Regulatory reporting
- Client notification

### Complex Dividend Management

**Dividend Reinvestment**
- Automatic dividend reinvestment
- Manual dividend reinvestment options
- Reinvestment timing and pricing
- Reinvestment fee calculations

**Withholding Tax**
- Foreign dividend withholding tax
- Tax treaty applications
- Tax reclaim processing
- Tax reporting and compliance

**Foreign Dividend Handling**
- Currency conversion for foreign dividends
- Exchange rate management
- Cross-border dividend processing
- Regulatory compliance for foreign dividends

### Performance Requirements

- **Interest Cash Flows**: 100K+ daily interest calculations
- **Dividend Cash Flows**: 50K+ daily dividend processing
- **Corporate Actions**: 10K+ daily corporate action events
- **Settlement Processing**: 25K+ daily settlements
- **Accrual Processing**: <50ms for real-time accruals
- **Realization Processing**: <100ms for cash flow realization
- **Settlement Processing**: <200ms for settlement preparation

### Integration Points

**Internal Integrations**
- Position Management: Lot-level position updates
- Contract Management: Contract term validation
- Event Processing: Lifecycle event correlation
- State Management: State transition processing

**External Integrations**
- Payment Systems: SWIFT, ACH, wire transfers
- Tax Systems: Tax calculation and reporting
- Regulatory Systems: Compliance and reporting
- Client Systems: Client notifications and confirmations

## References

- [Cash Flow Architecture](cash-flow-architecture.md)
- [Multi-stage Workflow Patterns](https://martinfowler.com/articles/201701-event-driven.html)
- [Event Sourcing](https://martinfowler.com/eaaDev/EventSourcing.html)
- [Saga Pattern](https://microservices.io/patterns/data/saga.html)
- [State Machine Patterns](https://en.wikipedia.org/wiki/State_pattern)
