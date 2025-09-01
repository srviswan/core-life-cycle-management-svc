# Core Life Cycle Management Service

## Overview

The Core Life Cycle Management Service is a modern, event-driven microservice designed to manage the complete lifecycle of synthetic equity swaps. Built with a CDM-inspired architecture and custom extensions for synthetic swaps, this service handles position management, contract lifecycle events, corporate actions, and cash flow generation.

## Architecture Principles

- **CDM-Inspired**: Based on FINOS CDM Event Model with custom extensions
- **Event-Driven**: Asynchronous processing with event sourcing
- **High Performance**: Designed for 1M+ daily position updates
- **Regulatory Compliant**: MiFID, Dodd-Frank, and other regulatory frameworks
- **Scalable**: Container-based deployment with auto-scaling capabilities

## Key Features

- **Position Management**: Lot-level tracking with real-time aggregations
- **Contract Management**: Complex basket contracts with multiple constituents
- **Lifecycle Events**: Corporate actions, resets, terminations, modifications
- **Cash Flow Generation**: Event-driven cash flow processing
- **Real-time ODS**: Operational data store for downstream systems
- **Regulatory Reporting**: Comprehensive audit trails and compliance

## Service Boundaries

- **Input**: Enriched swap blotters from Trade Capture service
- **Core Functions**: Position management, lifecycle events, valuations, cash flows
- **Output**: Real-time data to ODS for downstream consumption
- **Integration**: Settlement systems, regulatory gateways, client reporting

## Technology Stack

- **Runtime**: Java 21, Spring Boot
- **Database**: MS SQL Server (ACID compliance), Redis (caching)
- **Messaging**: Apache Kafka (event streaming), Solace (reliable messaging)
- **Containerization**: Docker, Kubernetes
- **Monitoring**: Prometheus, Grafana, Jaeger

## Repository Structure

```
├── docs/                    # Documentation
│   ├── architecture/        # High-level architecture
│   ├── design/             # Detailed design specifications
│   ├── implementation/     # Implementation guides
│   ├── testing/            # Testing strategy and plans
│   └── operations/         # Operational procedures
├── src/                    # Source code
│   ├── main/               # Main application code
│   └── test/               # Test code
├── scripts/                # Build and deployment scripts
└── tools/                  # Development and testing tools
```

## Getting Started

### Prerequisites

- Java 21+
- Docker
- Kubernetes cluster
- MS SQL Server
- Apache Kafka
- Redis

### Quick Start

1. Clone the repository
2. Review the architecture documentation
3. Set up the development environment
4. Run the test suite
5. Deploy to your environment

## Documentation

- [Architecture Overview](docs/architecture/README.md)
- [System Design](docs/design/README.md)
- [Implementation Guide](docs/implementation/README.md)
- [Testing Strategy](docs/testing/README.md)
- [Operations Guide](docs/operations/README.md)

## Contributing

Please read our [Contributing Guidelines](CONTRIBUTING.md) before submitting changes.

## License

[License information to be added]

## Contact

For questions and support, please contact the development team.
