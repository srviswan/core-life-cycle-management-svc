# ADR-002: CDM-Inspired Event Model

## Status

**Accepted** - This decision has been approved and will be implemented.

## Context

The Core Life Cycle Management Service needs a robust event model to handle complex synthetic equity swaps lifecycle events. The requirements include:

- **Regulatory Compliance**: MiFID, Dodd-Frank, and other frameworks require complete audit trails
- **Event Ordering**: Guaranteed event ordering for regulatory reporting
- **State Reconstruction**: Ability to reconstruct any point in time for audit purposes
- **Complex Lifecycle Events**: Corporate actions, resets, terminations, modifications
- **Performance**: High-throughput event processing for 1M+ daily updates
- **Industry Standards**: Alignment with financial industry best practices

The key question is whether to implement:
1. **Custom Event Model**: Completely custom event structure and processing
2. **Full CDM Compliance**: Complete implementation of FINOS CDM Event Model
3. **CDM-Inspired with Custom Extensions**: CDM principles with synthetic swap-specific extensions

## Decision

**CDM-Inspired Event Model with Custom Extensions for Synthetic Swaps**

We will implement an event model based on FINOS CDM principles while adding custom extensions specifically designed for synthetic equity swaps. This approach provides industry-standard foundations with business-specific flexibility.

## Rationale

### Why CDM-Inspired?

1. **Industry Standards**: CDM is a well-established financial industry standard
   - Developed by FINOS (Financial Open Source Foundation)
   - Widely adopted by major financial institutions
   - Comprehensive coverage of financial instrument lifecycle events

2. **Regulatory Alignment**: CDM principles align with regulatory requirements
   - Complete audit trail and state reconstruction
   - Event ordering and correlation
   - Comprehensive event qualification and classification

3. **Proven Architecture**: CDM's event model is battle-tested
   - Used in production by major banks
   - Handles complex financial instrument lifecycles
   - Scalable and performant architecture

4. **Future Compatibility**: Easier to adopt more CDM standards over time
   - Gradual migration path to full CDM compliance
   - Industry tooling and ecosystem support
   - Reduced vendor lock-in

### Why Custom Extensions?

1. **Synthetic Swap Specificity**: CDM doesn't fully cover synthetic equity swaps
   - Basket contract management with multiple constituents
   - Complex corporate action handling
   - Synthetic swap-specific lifecycle events

2. **Performance Requirements**: Custom optimizations for high-throughput processing
   - Optimized event serialization for synthetic swaps
   - Custom aggregation and calculation logic
   - Performance tuning for 1M+ daily updates

3. **Business Logic**: Domain-specific business rules and workflows
   - Synthetic swap approval workflows
   - Corporate action processing logic
   - Cash flow generation algorithms

4. **Integration Requirements**: Custom integration with existing systems
   - Legacy system compatibility
   - Custom ODS publishing formats
   - Specialized regulatory reporting

## Consequences

### Positive Consequences

1. **Industry Alignment**: Follows established financial industry standards
2. **Regulatory Compliance**: Built-in support for audit trails and compliance
3. **Tooling Support**: Access to CDM ecosystem and tools
4. **Future Flexibility**: Easy to adopt more CDM standards
5. **Reduced Risk**: Proven architecture reduces implementation risk

### Negative Consequences

1. **Complexity**: CDM concepts add initial complexity
2. **Learning Curve**: Team needs to understand CDM principles
3. **Custom Extensions**: Need to maintain custom code alongside standards
4. **Version Management**: CDM version updates may require changes

### Mitigation Strategies

1. **Phased Implementation**: Start with core CDM concepts, add extensions gradually
2. **Team Training**: Invest in CDM training and documentation
3. **Clear Boundaries**: Well-defined separation between CDM and custom code
4. **Version Strategy**: Plan for CDM version updates and migrations

## Alternatives Considered

### Alternative 1: Custom Event Model
- **Pros**: Complete control, optimized for synthetic swaps, no external dependencies
- **Cons**: No industry standards, higher regulatory risk, reinventing the wheel
- **Rejection Reason**: Would not provide the regulatory compliance and industry alignment benefits

### Alternative 2: Full CDM Compliance
- **Pros**: Complete industry standard compliance, maximum tooling support
- **Cons**: Over-engineering for current needs, complex implementation, longer time to market
- **Rejection Reason**: Full CDM compliance is not required for current business needs and would delay implementation

### Alternative 3: FpML-Based Event Model
- **Pros**: XML-based standard, good for trade confirmations
- **Cons**: Limited lifecycle event support, performance overhead, less flexible
- **Rejection Reason**: FpML is more suited for trade confirmations than lifecycle event management

## Implementation Notes

### CDM Principles to Implement

1. **TradeState Management**
   - Complete state lineage and history
   - State transitions through primitive operators
   - Reset and transfer history tracking

2. **Event Composition**
   - Primitive operators for fundamental state changes
   - Business event composition from primitive operators
   - Event qualification and classification

3. **Workflow Support**
   - Multi-step approval workflows
   - Workflow state management
   - Audit trail and lineage tracking

4. **Event Sourcing**
   - Complete event history and replay capability
   - State reconstruction at any point in time
   - Event correlation and relationships

### Custom Extensions

1. **Synthetic Swap Events**
   - Basket contract management events
   - Corporate action processing events
   - Cash flow generation events

2. **Performance Optimizations**
   - Custom event serialization
   - Optimized event processing
   - High-throughput event handling

3. **Business Logic**
   - Synthetic swap approval workflows
   - Corporate action processing logic
   - Cash flow calculation algorithms

### Event Schema Structure

```json
{
  "businessEvent": {
    "eventIdentifier": "EVENT_001",
    "intent": "CORPORATE_ACTION",
    "corporateActionIntent": "DIVIDEND",
    "eventDate": "2024-01-15",
    "effectiveDate": "2024-01-15",
    "before": ["TradeState_001"],
    "after": ["TradeState_002"],
    "instruction": [
      {
        "primitiveInstruction": {
          "primitiveOperator": "ADJUST_POSITION",
          "before": "TradeState_001",
          "after": "TradeState_002",
          "customExtensions": {
            "syntheticSwapAdjustment": {
              "adjustmentType": "DIVIDEND",
              "constituent": "IBM",
              "adjustmentAmount": 1000,
              "adjustmentMethod": "LOT_LEVEL"
            }
          }
        }
      }
    ],
    "customExtensions": {
      "basketImpact": {
        "totalAdjustment": 1000,
        "constituentAdjustments": [
          {
            "security": "IBM",
            "adjustment": 400
          },
          {
            "security": "AAPL",
            "adjustment": 600
          }
        ]
      }
    }
  }
}
```

### Implementation Phases

1. **Phase 1: CDM Foundation**
   - Basic TradeState management
   - Simple event composition
   - Basic workflow support

2. **Phase 2: Custom Extensions**
   - Synthetic swap events
   - Corporate action processing
   - Performance optimizations

3. **Phase 3: Advanced Features**
   - Complex workflows
   - Advanced event correlation
   - Full CDM compliance (optional)

## References

- [FINOS CDM Event Model](https://cdm.finos.org/docs/event-model)
- [CDM GitHub Repository](https://github.com/finos/common-domain-model)
- [CDM Documentation](https://cdm.finos.org/)
- [Event Sourcing Patterns](https://martinfowler.com/eaaDev/EventSourcing.html)
- [Domain Events](https://martinfowler.com/articles/201701-event-driven.html)
