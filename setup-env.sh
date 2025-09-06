#!/bin/bash

# Environment setup script for Cash Flow Management Service
echo "Setting up environment variables for Cash Flow Management Service..."

# Database configuration
export DB_USERNAME=sa
export DB_PASSWORD="YourStrong@Passw0rd"
export DB_URL="jdbc:sqlserver://localhost:1433;databaseName=cashflow_db;encrypt=true;trustServerCertificate=true"

# Application settings
export SERVER_PORT=8080
export SPRING_PROFILES_ACTIVE=default

# Logging
export LOGGING_LEVEL_COM_FINANCIAL_CASHFLOW=INFO

echo "Environment variables configured:"
echo "  DB_USERNAME: $DB_USERNAME"
echo "  DB_PASSWORD: [HIDDEN]"
echo "  DB_URL: $DB_URL"
echo "  SERVER_PORT: $SERVER_PORT"
echo "  SPRING_PROFILES_ACTIVE: $SPRING_PROFILES_ACTIVE"
echo ""
echo "You can now run: mvn spring-boot:run"
