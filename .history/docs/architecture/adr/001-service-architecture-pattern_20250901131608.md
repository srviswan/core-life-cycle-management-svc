# ADR-001: Service Architecture Pattern

## Status

**Accepted** - This decision has been approved and will be implemented.

## Context

The Core Life Cycle Management Service needs to handle complex synthetic equity swaps with the following requirements:

- **Complexity**: Highly customized contracts with frequent changes
- **Performance**: 1M daily position updates with <100ms response time
- **Scalability**: Support for 4x growth in contracts (65K → 260K) and positions (160K → 640K)
- **Regulatory Compliance**: MiFID, Dodd-Frank, and other regulatory frameworks
- **Team Structure**: Single development team managing both contract and position management

The key question is whether to implement this as:
1. **Separate Services**: Contract Management and Position Management as distinct microservices
2. **Single Service**: Unified Lifecycle Management Service with internal component separation
3. **Monolithic Service**: Single large service handling all functionality

## Decision

**Single Lifecycle Management Service with internal component separation**

We will implement a single, cohesive service that handles both contract and position management through well-defined internal components. This service will follow CDM principles with custom extensions for synthetic swaps.

## Rationale

### Why Single Service?

1. **High Coupling**: Contract and position data are tightly coupled in synthetic swaps
   - Most operations affect both contract and position simultaneously
   - Basket contracts require coordinated management of multiple constituents
   - Corporate actions impact both contract terms and position calculations

2. **Transaction Boundaries**: Operations span contract and position domains
   - New trades create both contract and position records
   - Corporate actions modify both contract terms and position data
   - Terminations affect both contract lifecycle and position calculations

3. **Data Consistency**: Easier to maintain ACID compliance
   - Single transaction boundary for related operations
   - Consistent state across contract and position data
   - Simplified rollback and recovery mechanisms

4. **Team Efficiency**: Single team with clear component boundaries
   - Easier coordination and communication
   - Shared understanding of business logic
   - Reduced integration complexity

### Why Internal Component Separation?

1. **Separation of Concerns**: Clear boundaries between different functional areas
   - Contract Management: Legal terms, counterparty info, regulatory compliance
   - Position Management: Lot-level tracking, aggregations, cash flows
   - Lifecycle Events: Corporate actions, workflows, approvals

2. **Maintainability**: Easier to understand and modify specific functionality
   - Clear interfaces between components
   - Isolated testing and debugging
   - Incremental development and deployment

3. **Performance Optimization**: Component-specific optimizations
   - Different caching strategies for different data types
   - Optimized database queries for specific use cases
   - Targeted performance improvements

## Consequences

### Positive Consequences

1. **Simplified Architecture**: Single service boundary reduces complexity
2. **Better Performance**: Optimized data access patterns within single service
3. **Easier Testing**: Single service can be tested as a unit
4. **Reduced Network Overhead**: No inter-service communication for internal operations
5. **Simplified Deployment**: Single service deployment and scaling

### Negative Consequences

1. **Service Size**: Larger service with more responsibilities
2. **Coupling**: Internal components are more tightly coupled
3. **Team Coordination**: Requires good internal component design
4. **Technology Lock-in**: All components must use same technology stack

### Mitigation Strategies

1. **Clear Component Boundaries**: Well-defined interfaces and responsibilities
2. **Event-Driven Internal Communication**: Decoupled component interaction
3. **Comprehensive Testing**: Unit, integration, and performance testing
4. **Monitoring and Observability**: Component-level metrics and tracing

## Alternatives Considered

### Alternative 1: Separate Microservices
- **Pros**: Clear service boundaries, independent scaling, technology flexibility
- **Cons**: High coupling between services, complex integration, network overhead
- **Rejection Reason**: The tight coupling between contract and position data makes separate services inefficient

### Alternative 2: Monolithic Service
- **Pros**: Simple deployment, no integration complexity
- **Cons**: Difficult to maintain, poor scalability, hard to test
- **Rejection Reason**: Would not provide the component separation needed for maintainability

### Alternative 3: Event Sourcing with Separate Aggregates
- **Pros**: Clear aggregate boundaries, event-driven architecture
- **Cons**: Complex event correlation, potential consistency issues
- **Rejection Reason**: The business domain doesn't naturally separate into distinct aggregates

## Implementation Notes

### Component Structure

```
Lifecycle Management Service
├── Contract Management Component
│   ├── Contract lifecycle management
│   ├── Legal terms and counterparty info
│   ├── Regulatory compliance
│   └── Basket constituent management
├── Position Management Component
│   ├── Lot-level position tracking
│   ├── Real-time aggregations
│   ├── Economic calculations
│   └── Cash flow generation
├── Lifecycle Events Component
│   ├── Corporate action processing
│   ├── Event workflow management
│   ├── Approval and validation
│   └── Event correlation
└── Core Engine Components
    ├── Trade State Engine (CDM-aligned)
    ├── Event Processing Engine
    └── Workflow Engine
```

### Communication Patterns

1. **Internal Communication**: Direct method calls for high-performance operations
2. **Event-Driven**: Internal events for decoupled component interaction
3. **Shared State**: Common data structures and interfaces
4. **External Communication**: Event streaming and REST APIs

### Data Management

1. **Shared Database**: MS SQL Server for ACID compliance
2. **Event Store**: Kafka for event sourcing and audit trails
3. **Cache Layer**: Redis for real-time aggregations and performance
4. **Data Consistency**: Single transaction boundary for related operations

## References

- [FINOS CDM Event Model](https://cdm.finos.org/docs/event-model)
- [Microservices Patterns](https://microservices.io/patterns/)
- [Event Sourcing](https://martinfowler.com/eaaDev/EventSourcing.html)
- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html)
