#!/bin/bash

# Cash Flow Management Service Deployment Script
# This script deploys the service as a traditional JAR file

set -e

# Configuration
SERVICE_NAME="cash-flow-management-service"
JAR_FILE="target/cash-flow-management-service-1.0.0-SNAPSHOT.jar"
SERVICE_USER="cashflow"
SERVICE_GROUP="cashflow"
INSTALL_DIR="/opt/cashflow"
LOG_DIR="/var/log/cashflow"
PID_FILE="/var/run/cashflow/cash-flow-service.pid"
CONFIG_DIR="/etc/cashflow"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if running as root
check_root() {
    if [[ $EUID -ne 0 ]]; then
        print_error "This script must be run as root"
        exit 1
    fi
}

# Function to create service user
create_service_user() {
    print_status "Creating service user and group..."
    
    if ! getent group $SERVICE_GROUP > /dev/null 2>&1; then
        groupadd $SERVICE_GROUP
    fi
    
    if ! getent passwd $SERVICE_USER > /dev/null 2>&1; then
        useradd -r -g $SERVICE_GROUP -s /bin/false $SERVICE_USER
    fi
}

# Function to create directories
create_directories() {
    print_status "Creating directories..."
    
    mkdir -p $INSTALL_DIR
    mkdir -p $LOG_DIR
    mkdir -p $CONFIG_DIR
    mkdir -p $(dirname $PID_FILE)
    
    chown -R $SERVICE_USER:$SERVICE_GROUP $INSTALL_DIR
    chown -R $SERVICE_USER:$SERVICE_GROUP $LOG_DIR
    chown -R $SERVICE_USER:$SERVICE_GROUP $CONFIG_DIR
    chown -R $SERVICE_USER:$SERVICE_GROUP $(dirname $PID_FILE)
}

# Function to build the application
build_application() {
    print_status "Building application..."
    
    if ! command -v mvn &> /dev/null; then
        print_error "Maven is not installed. Please install Maven first."
        exit 1
    fi
    
    mvn clean package -DskipTests
    
    if [ ! -f "$JAR_FILE" ]; then
        print_error "Build failed. JAR file not found: $JAR_FILE"
        exit 1
    fi
}

# Function to install the application
install_application() {
    print_status "Installing application..."
    
    cp $JAR_FILE $INSTALL_DIR/cash-flow-service.jar
    chown $SERVICE_USER:$SERVICE_GROUP $INSTALL_DIR/cash-flow-service.jar
    
    # Create application.yml if it doesn't exist
    if [ ! -f "$CONFIG_DIR/application.yml" ]; then
        cat > $CONFIG_DIR/application.yml << EOF
spring:
  application:
    name: cash-flow-management-service
  
  datasource:
    url: jdbc:sqlserver://localhost:1433;databaseName=cashflow_db;encrypt=true;trustServerCertificate=true
    username: \${DB_USERNAME:sa}
    password: \${DB_PASSWORD:YourStrong@Passw0rd}
    driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.SQLServerDialect
        format_sql: true
        jdbc:
          batch_size: 50
          fetch_size: 50
        order_inserts: true
        order_updates: true
        batch_versioned_data: true
  
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml
    contexts: \${LIQUIBASE_CONTEXTS:}
    default-schema: cashflow
  
  cache:
    type: redis
    redis:
      host: \${REDIS_HOST:localhost}
      port: \${REDIS_PORT:6379}
      password: \${REDIS_PASSWORD:}
      database: 0
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
          max-wait: -1ms
  
  kafka:
    bootstrap-servers: \${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: cash-flow-service-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      properties:
        spring.json.trusted.packages: "*"
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      properties:
        spring.json.add.type.headers: false
        spring.json.type.mapping: cashflow:com.financial.cashflow.domain.event.CashFlowEvent,settlement:com.financial.cashflow.domain.event.SettlementEvent

server:
  port: \${SERVER_PORT:8080}
  servlet:
    context-path: /api/v1

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
      base-path: /actuator
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    export:
      prometheus:
        enabled: true

logging:
  level:
    com.financial.cashflow: INFO
    org.springframework.kafka: WARN
    org.hibernate.SQL: WARN
  file:
    name: $LOG_DIR/cash-flow-service.log
    max-size: 100MB
    max-history: 30
EOF
    fi
    
    chown $SERVICE_USER:$SERVICE_GROUP $CONFIG_DIR/application.yml
}

# Function to create systemd service
create_systemd_service() {
    print_status "Creating systemd service..."
    
    cat > /etc/systemd/system/cash-flow-service.service << EOF
[Unit]
Description=Cash Flow Management Service
After=network.target

[Service]
Type=simple
User=$SERVICE_USER
Group=$SERVICE_GROUP
WorkingDirectory=$INSTALL_DIR
ExecStart=/usr/bin/java -Xms512m -Xmx2g -XX:+UseG1GC -jar cash-flow-service.jar --spring.config.location=file:$CONFIG_DIR/application.yml
ExecStop=/bin/kill -TERM \$MAINPID
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=cash-flow-service

# Security settings
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=$LOG_DIR $INSTALL_DIR

[Install]
WantedBy=multi-user.target
EOF

    systemctl daemon-reload
}

# Function to start the service
start_service() {
    print_status "Starting service..."
    
    systemctl enable cash-flow-service
    systemctl start cash-flow-service
    
    # Wait for service to start
    sleep 5
    
    if systemctl is-active --quiet cash-flow-service; then
        print_status "Service started successfully"
    else
        print_error "Service failed to start"
        systemctl status cash-flow-service
        exit 1
    fi
}

# Function to check service status
check_service() {
    print_status "Checking service status..."
    
    if systemctl is-active --quiet cash-flow-service; then
        print_status "Service is running"
        systemctl status cash-flow-service --no-pager -l
    else
        print_error "Service is not running"
        systemctl status cash-flow-service --no-pager -l
        exit 1
    fi
}

# Function to stop the service
stop_service() {
    print_status "Stopping service..."
    
    if systemctl is-active --quiet cash-flow-service; then
        systemctl stop cash-flow-service
        print_status "Service stopped"
    else
        print_warning "Service is not running"
    fi
}

# Function to uninstall the service
uninstall_service() {
    print_status "Uninstalling service..."
    
    stop_service
    
    systemctl disable cash-flow-service
    rm -f /etc/systemd/system/cash-flow-service.service
    systemctl daemon-reload
    
    rm -rf $INSTALL_DIR
    rm -rf $CONFIG_DIR
    rm -rf $LOG_DIR
    rm -rf $(dirname $PID_FILE)
    
    print_status "Service uninstalled"
}

# Main script logic
case "${1:-install}" in
    install)
        check_root
        create_service_user
        create_directories
        build_application
        install_application
        create_systemd_service
        start_service
        check_service
        print_status "Installation completed successfully"
        ;;
    start)
        check_root
        start_service
        ;;
    stop)
        check_root
        stop_service
        ;;
    restart)
        check_root
        stop_service
        start_service
        ;;
    status)
        check_service
        ;;
    uninstall)
        check_root
        uninstall_service
        ;;
    *)
        echo "Usage: $0 {install|start|stop|restart|status|uninstall}"
        echo "  install   - Install and start the service"
        echo "  start     - Start the service"
        echo "  stop      - Stop the service"
        echo "  restart   - Restart the service"
        echo "  status    - Check service status"
        echo "  uninstall - Uninstall the service"
        exit 1
        ;;
esac
