# Messaging and Caching Abstraction Design

## Overview

This document outlines the abstraction layer design for messaging (Kafka) and caching (Redis) to provide flexibility in choosing underlying technologies while maintaining consistent business logic.

## Architecture Principles

### 1. **Technology Agnostic**
- Business logic should not depend on specific messaging/caching technologies
- Easy migration between different solutions
- Consistent API regardless of underlying implementation

### 2. **Performance First**
- Abstraction should not significantly impact performance
- Support for async operations
- Batch processing capabilities

### 3. **Observability**
- Comprehensive monitoring and metrics
- Error tracking and debugging
- Performance monitoring

### 4. **Resilience**
- Retry mechanisms
- Circuit breakers
- Graceful degradation

## Messaging Abstraction

### 1. **Core Messaging Interface**

```java
public interface MessagingService {
    
    /**
     * Publish a message to a topic
     */
    CompletableFuture<Void> publish(String topic, Message message);
    
    /**
     * Publish a message with key for partitioning
     */
    CompletableFuture<Void> publish(String topic, String key, Message message);
    
    /**
     * Publish multiple messages in batch
     */
    CompletableFuture<Void> publishBatch(String topic, List<Message> messages);
    
    /**
     * Subscribe to a topic
     */
    Subscription subscribe(String topic, MessageHandler handler);
    
    /**
     * Subscribe to a topic with consumer group
     */
    Subscription subscribe(String topic, String consumerGroup, MessageHandler handler);
    
    /**
     * Close the messaging service
     */
    CompletableFuture<Void> close();
}

public interface Message {
    String getId();
    String getTopic();
    String getKey();
    byte[] getPayload();
    Map<String, String> getHeaders();
    Instant getTimestamp();
    MessageType getType();
}

public interface MessageHandler {
    CompletableFuture<Void> handle(Message message);
}

public interface Subscription {
    String getTopic();
    String getConsumerGroup();
    CompletableFuture<Void> unsubscribe();
}
```

### 2. **Message Types and Events**

```java
public enum MessageType {
    CASH_FLOW_CALCULATED,
    SETTLEMENT_CREATED,
    POSITION_UPDATED,
    CONTRACT_MODIFIED,
    CORPORATE_ACTION_PROCESSED,
    AUDIT_EVENT
}

public class CashFlowEvent implements Message {
    private final String eventId;
    private final String contractId;
    private final LocalDate calculationDate;
    private final List<CashFlow> cashFlows;
    private final CalculationType calculationType;
    
    // Implementation details...
}

public class SettlementEvent implements Message {
    private final String eventId;
    private final String settlementId;
    private final String contractId;
    private final SettlementStatus status;
    private final LocalDate settlementDate;
    
    // Implementation details...
}
```

### 3. **Configuration Abstraction**

```java
@Configuration
public class MessagingConfiguration {
    
    @Bean
    @ConditionalOnProperty(name = "messaging.provider", havingValue = "kafka")
    public MessagingService kafkaMessagingService(MessagingProperties properties) {
        return new KafkaMessagingService(properties);
    }
    
    @Bean
    @ConditionalOnProperty(name = "messaging.provider", havingValue = "solace")
    public MessagingService solaceMessagingService(MessagingProperties properties) {
        return new SolaceMessagingService(properties);
    }
    
    @Bean
    @ConditionalOnProperty(name = "messaging.provider", havingValue = "rabbitmq")
    public MessagingService rabbitMqMessagingService(MessagingProperties properties) {
        return new RabbitMqMessagingService(properties);
    }
    
    @Bean
    @ConditionalOnProperty(name = "messaging.provider", havingValue = "memory")
    public MessagingService inMemoryMessagingService(MessagingProperties properties) {
        return new InMemoryMessagingService(properties);
    }
}

@ConfigurationProperties(prefix = "messaging")
public class MessagingProperties {
    private String provider = "kafka";
    private String bootstrapServers;
    private String clientId;
    private int maxRetries = 3;
    private Duration retryBackoff = Duration.ofSeconds(1);
    private int batchSize = 100;
    private Duration batchTimeout = Duration.ofMillis(100);
    private boolean enableCompression = true;
    private Map<String, String> topicConfigs = new HashMap<>();
}
```

### 4. **Kafka Implementation**

```java
@Service
public class KafkaMessagingService implements MessagingService {
    
    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final KafkaConsumer<String, byte[]> consumer;
    private final MessagingProperties properties;
    private final MeterRegistry meterRegistry;
    
    public KafkaMessagingService(MessagingProperties properties, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.kafkaTemplate = createKafkaTemplate();
        this.consumer = createKafkaConsumer();
    }
    
    @Override
    public CompletableFuture<Void> publish(String topic, Message message) {
        return CompletableFuture.runAsync(() -> {
            try {
                kafkaTemplate.send(topic, message.getKey(), message.getPayload());
                meterRegistry.counter("messaging.published", "topic", topic, "type", message.getType().name()).increment();
            } catch (Exception e) {
                meterRegistry.counter("messaging.published.error", "topic", topic, "type", message.getType().name()).increment();
                throw new MessagingException("Failed to publish message", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Void> publishBatch(String topic, List<Message> messages) {
        return CompletableFuture.runAsync(() -> {
            try {
                List<ProducerRecord<String, byte[]>> records = messages.stream()
                    .map(msg -> new ProducerRecord<>(topic, msg.getKey(), msg.getPayload()))
                    .collect(Collectors.toList());
                
                kafkaTemplate.send(records);
                meterRegistry.counter("messaging.published.batch", "topic", topic, "count", String.valueOf(messages.size())).increment();
            } catch (Exception e) {
                meterRegistry.counter("messaging.published.batch.error", "topic", topic).increment();
                throw new MessagingException("Failed to publish batch", e);
            }
        });
    }
    
    @Override
    public Subscription subscribe(String topic, String consumerGroup, MessageHandler handler) {
        return new KafkaSubscription(topic, consumerGroup, consumer, handler, meterRegistry);
    }
    
    private KafkaTemplate<String, byte[]> createKafkaTemplate() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        configs.put(ProducerConfig.CLIENT_ID_CONFIG, properties.getClientId());
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        configs.put(ProducerConfig.RETRIES_CONFIG, properties.getMaxRetries());
        configs.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, properties.isEnableCompression() ? "snappy" : "none");
        configs.put(ProducerConfig.BATCH_SIZE_CONFIG, properties.getBatchSize());
        configs.put(ProducerConfig.LINGER_MS_CONFIG, properties.getBatchTimeout().toMillis());
        
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(configs));
    }
}

public class KafkaSubscription implements Subscription {
    private final String topic;
    private final String consumerGroup;
    private final KafkaConsumer<String, byte[]> consumer;
    private final MessageHandler handler;
    private final MeterRegistry meterRegistry;
    private volatile boolean running = true;
    
    public KafkaSubscription(String topic, String consumerGroup, KafkaConsumer<String, byte[]> consumer, 
                           MessageHandler handler, MeterRegistry meterRegistry) {
        this.topic = topic;
        this.consumerGroup = consumerGroup;
        this.consumer = consumer;
        this.handler = handler;
        this.meterRegistry = meterRegistry;
        
        consumer.subscribe(Collections.singletonList(topic));
        startConsuming();
    }
    
    private void startConsuming() {
        CompletableFuture.runAsync(() -> {
            while (running) {
                try {
                    ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(100));
                    for (ConsumerRecord<String, byte[]> record : records) {
                        Message message = createMessage(record);
                        handler.handle(message)
                            .thenRun(() -> meterRegistry.counter("messaging.consumed", "topic", topic).increment())
                            .exceptionally(throwable -> {
                                meterRegistry.counter("messaging.consumed.error", "topic", topic).increment();
                                return null;
                            });
                    }
                } catch (Exception e) {
                    meterRegistry.counter("messaging.consumed.error", "topic", topic).increment();
                    // Log error and continue
                }
            }
        });
    }
}
```

### 5. **Solace Implementation**

```java
@Service
public class SolaceMessagingService implements MessagingService {
    
    private final JCSMPSession session;
    private final MessagingProperties properties;
    private final MeterRegistry meterRegistry;
    
    public SolaceMessagingService(MessagingProperties properties, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.session = createSolaceSession();
    }
    
    @Override
    public CompletableFuture<Void> publish(String topic, Message message) {
        return CompletableFuture.runAsync(() -> {
            try {
                TextMessage solaceMessage = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
                solaceMessage.setText(new String(message.getPayload()));
                solaceMessage.setCorrelationId(message.getId());
                
                // Add headers
                for (Map.Entry<String, String> header : message.getHeaders().entrySet()) {
                    solaceMessage.setApplicationMessageId(header.getKey() + ":" + header.getValue());
                }
                
                session.getMessageProducer(new JCSMPStreamingPublishCorrelatingEventHandler() {
                    @Override
                    public void responseReceivedEx(Object key) {
                        // Handle response
                    }
                    
                    @Override
                    public void handleErrorEx(Object key, JCSMPException cause, long timestamp) {
                        meterRegistry.counter("messaging.published.error", "topic", topic).increment();
                    }
                }).send(solaceMessage, JCSMPFactory.onlyInstance().createTopic(topic));
                
                meterRegistry.counter("messaging.published", "topic", topic, "type", message.getType().name()).increment();
            } catch (Exception e) {
                meterRegistry.counter("messaging.published.error", "topic", topic).increment();
                throw new MessagingException("Failed to publish message", e);
            }
        });
    }
}
```

## Caching Abstraction

### 1. **Core Caching Interface**

```java
public interface CacheService {
    
    /**
     * Get value from cache
     */
    <T> CompletableFuture<Optional<T>> get(String key, Class<T> type);
    
    /**
     * Put value in cache
     */
    <T> CompletableFuture<Void> put(String key, T value);
    
    /**
     * Put value in cache with TTL
     */
    <T> CompletableFuture<Void> put(String key, T value, Duration ttl);
    
    /**
     * Delete value from cache
     */
    CompletableFuture<Void> delete(String key);
    
    /**
     * Check if key exists
     */
    CompletableFuture<Boolean> exists(String key);
    
    /**
     * Get multiple values
     */
    <T> CompletableFuture<Map<String, T>> getMultiple(List<String> keys, Class<T> type);
    
    /**
     * Put multiple values
     */
    <T> CompletableFuture<Void> putMultiple(Map<String, T> keyValuePairs);
    
    /**
     * Put multiple values with TTL
     */
    <T> CompletableFuture<Void> putMultiple(Map<String, T> keyValuePairs, Duration ttl);
    
    /**
     * Get cache statistics
     */
    CacheStats getStats();
}

public class CacheStats {
    private final long hits;
    private final long misses;
    private final long evictions;
    private final long size;
    private final Duration averageLoadTime;
    
    // Constructor and getters...
}
```

### 2. **Cache Configuration**

```java
@Configuration
public class CacheConfiguration {
    
    @Bean
    @ConditionalOnProperty(name = "cache.provider", havingValue = "redis")
    public CacheService redisCacheService(CacheProperties properties) {
        return new RedisCacheService(properties);
    }
    
    @Bean
    @ConditionalOnProperty(name = "cache.provider", havingValue = "hazelcast")
    public CacheService hazelcastCacheService(CacheProperties properties) {
        return new HazelcastCacheService(properties);
    }
    
    @Bean
    @ConditionalOnProperty(name = "cache.provider", havingValue = "caffeine")
    public CacheService caffeineCacheService(CacheProperties properties) {
        return new CaffeineCacheService(properties);
    }
    
    @Bean
    @ConditionalOnProperty(name = "cache.provider", havingValue = "memory")
    public CacheService inMemoryCacheService(CacheProperties properties) {
        return new InMemoryCacheService(properties);
    }
}

@ConfigurationProperties(prefix = "cache")
public class CacheProperties {
    private String provider = "redis";
    private String host = "localhost";
    private int port = 6379;
    private String password;
    private int database = 0;
    private Duration defaultTtl = Duration.ofHours(1);
    private int maxConnections = 10;
    private Duration connectionTimeout = Duration.ofSeconds(5);
    private boolean enableCompression = true;
    private Map<String, Duration> ttlOverrides = new HashMap<>();
}
```

### 3. **Redis Implementation**

```java
@Service
public class RedisCacheService implements CacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheProperties properties;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;
    
    public RedisCacheService(CacheProperties properties, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.objectMapper = new ObjectMapper();
        this.redisTemplate = createRedisTemplate();
    }
    
    @Override
    public <T> CompletableFuture<Optional<T>> get(String key, Class<T> type) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Object value = redisTemplate.opsForValue().get(key);
                if (value == null) {
                    meterRegistry.counter("cache.miss", "key", key).increment();
                    return Optional.empty();
                }
                
                T result = objectMapper.convertValue(value, type);
                meterRegistry.counter("cache.hit", "key", key).increment();
                return Optional.of(result);
            } catch (Exception e) {
                meterRegistry.counter("cache.error", "key", key).increment();
                throw new CacheException("Failed to get value from cache", e);
            }
        });
    }
    
    @Override
    public <T> CompletableFuture<Void> put(String key, T value) {
        return put(key, value, properties.getDefaultTtl());
    }
    
    @Override
    public <T> CompletableFuture<Void> put(String key, T value, Duration ttl) {
        return CompletableFuture.runAsync(() -> {
            try {
                redisTemplate.opsForValue().set(key, value, ttl);
                meterRegistry.counter("cache.put", "key", key).increment();
            } catch (Exception e) {
                meterRegistry.counter("cache.put.error", "key", key).increment();
                throw new CacheException("Failed to put value in cache", e);
            }
        });
    }
    
    @Override
    public <T> CompletableFuture<Map<String, T>> getMultiple(List<String> keys, Class<T> type) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Object> values = redisTemplate.opsForValue().multiGet(keys);
                Map<String, T> result = new HashMap<>();
                
                for (int i = 0; i < keys.size(); i++) {
                    if (values.get(i) != null) {
                        T value = objectMapper.convertValue(values.get(i), type);
                        result.put(keys.get(i), value);
                    }
                }
                
                meterRegistry.counter("cache.get.multiple", "count", String.valueOf(keys.size())).increment();
                return result;
            } catch (Exception e) {
                meterRegistry.counter("cache.get.multiple.error").increment();
                throw new CacheException("Failed to get multiple values from cache", e);
            }
        });
    }
    
    private RedisTemplate<String, Object> createRedisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        
        RedisConnectionFactory factory = new LettuceConnectionFactory(
            properties.getHost(), 
            properties.getPort()
        );
        
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        return template;
    }
}
```

### 4. **Caffeine Implementation (Local Cache)**

```java
@Service
public class CaffeineCacheService implements CacheService {
    
    private final Cache<String, Object> cache;
    private final CacheProperties properties;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;
    
    public CaffeineCacheService(CacheProperties properties, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.objectMapper = new ObjectMapper();
        this.cache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(properties.getDefaultTtl())
            .recordStats()
            .build();
    }
    
    @Override
    public <T> CompletableFuture<Optional<T>> get(String key, Class<T> type) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Object value = cache.getIfPresent(key);
                if (value == null) {
                    meterRegistry.counter("cache.miss", "key", key).increment();
                    return Optional.empty();
                }
                
                T result = objectMapper.convertValue(value, type);
                meterRegistry.counter("cache.hit", "key", key).increment();
                return Optional.of(result);
            } catch (Exception e) {
                meterRegistry.counter("cache.error", "key", key).increment();
                throw new CacheException("Failed to get value from cache", e);
            }
        });
    }
    
    @Override
    public <T> CompletableFuture<Void> put(String key, T value) {
        return put(key, value, properties.getDefaultTtl());
    }
    
    @Override
    public <T> CompletableFuture<Void> put(String key, T value, Duration ttl) {
        return CompletableFuture.runAsync(() -> {
            try {
                cache.put(key, value);
                meterRegistry.counter("cache.put", "key", key).increment();
            } catch (Exception e) {
                meterRegistry.counter("cache.put.error", "key", key).increment();
                throw new CacheException("Failed to put value in cache", e);
            }
        });
    }
}
```

## Service Integration

### 1. **Cash Flow Service Integration**

```java
@Service
public class CashFlowCalculationService {
    
    private final MessagingService messagingService;
    private final CacheService cacheService;
    private final MeterRegistry meterRegistry;
    
    public CashFlowCalculationService(MessagingService messagingService, 
                                    CacheService cacheService, 
                                    MeterRegistry meterRegistry) {
        this.messagingService = messagingService;
        this.cacheService = cacheService;
        this.meterRegistry = meterRegistry;
    }
    
    public CompletableFuture<CashFlowResponse> calculateCashFlows(CashFlowRequestContent request) {
        return CompletableFuture.supplyAsync(() -> {
            // Check cache first
            String cacheKey = generateCacheKey(request);
            Optional<CashFlowResponse> cached = cacheService.get(cacheKey, CashFlowResponse.class).join();
            
            if (cached.isPresent()) {
                return cached.get();
            }
            
            // Perform calculation
            CashFlowResponse response = performCalculation(request);
            
            // Cache result
            cacheService.put(cacheKey, response, Duration.ofMinutes(30)).join();
            
            // Publish event
            CashFlowEvent event = new CashFlowEvent(
                UUID.randomUUID().toString(),
                request.getContractId(),
                LocalDate.now(),
                response.getCashFlows(),
                response.getCalculationType().getDeterminedType()
            );
            
            messagingService.publish("cash-flow-events", event).join();
            
            return response;
        });
    }
    
    private String generateCacheKey(CashFlowRequestContent request) {
        return String.format("cashflow:%s:%s:%s:%s",
            request.getContractId(),
            request.getDateRange().getFromDate(),
            request.getDateRange().getToDate(),
            request.getCalculationType() != null ? request.getCalculationType() : "auto"
        );
    }
}
```

### 2. **Configuration Examples**

#### **Kafka + Redis Configuration**
```yaml
# application.yml
messaging:
  provider: kafka
  bootstrap-servers: localhost:9092
  client-id: cash-flow-service
  max-retries: 3
  batch-size: 100
  enable-compression: true

cache:
  provider: redis
  host: localhost
  port: 6379
  default-ttl: 1h
  max-connections: 10
  enable-compression: true
```

#### **Solace + Hazelcast Configuration**
```yaml
# application.yml
messaging:
  provider: solace
  bootstrap-servers: tcp://localhost:55555
  client-id: cash-flow-service
  max-retries: 3
  batch-size: 100

cache:
  provider: hazelcast
  host: localhost
  port: 5701
  default-ttl: 1h
  max-connections: 10
```

#### **In-Memory Configuration (Testing)**
```yaml
# application-test.yml
messaging:
  provider: memory
  batch-size: 10

cache:
  provider: memory
  default-ttl: 5m
```

## Migration Strategy

### 1. **Phase 1: Abstraction Layer**
- Implement abstraction interfaces
- Create current technology implementations (Kafka/Redis)
- Add comprehensive monitoring

### 2. **Phase 2: Alternative Implementations**
- Implement Solace messaging
- Implement Hazelcast/Caffeine caching
- Performance testing and comparison

### 3. **Phase 3: Migration**
- Gradual migration to new technologies
- A/B testing capabilities
- Rollback mechanisms

## Benefits

### 1. **Technology Flexibility**
- Easy migration between messaging/caching solutions
- No business logic changes required
- Technology selection based on performance/cost

### 2. **Testing Capabilities**
- In-memory implementations for testing
- Easy mocking and stubbing
- Performance testing with different technologies

### 3. **Operational Benefits**
- Consistent monitoring across technologies
- Standardized error handling
- Unified configuration management

### 4. **Future-Proofing**
- Easy adoption of new technologies
- Vendor lock-in avoidance
- Technology selection based on requirements

This abstraction design provides the flexibility you need while maintaining performance and observability. You can start with Kafka/Redis and easily migrate to other solutions as your needs evolve.
