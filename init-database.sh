#!/bin/bash

# Database initialization script for Cash Flow Management Service
echo "Waiting for SQL Server to be ready..."

# Wait for SQL Server to be ready
until /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "YourStrong@Passw0rd" -C -Q "SELECT 1" > /dev/null 2>&1; do
    echo "SQL Server is not ready yet. Waiting..."
    sleep 5
done

echo "SQL Server is ready. Setting up database..."

# Create database and tables
/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "YourStrong@Passw0rd" -C -i /docker-entrypoint-initdb.d/setup-database.sql

echo "Database setup completed!"
