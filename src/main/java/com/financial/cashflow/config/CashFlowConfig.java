package com.financial.cashflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Configuration for Cash Flow Management Service
 */
@Configuration
@EnableAsync
@Slf4j
public class CashFlowConfig {
    
    /**
     * Virtual threads executor for I/O operations
     */
    @Bean
    public ExecutorService virtualThreadExecutor() {
        log.info("Creating virtual thread executor for I/O operations");
        return Executors.newVirtualThreadPerTaskExecutor();
    }
    
    /**
     * Platform threads executor for CPU-intensive work
     */
    @Bean
    public ExecutorService cpuThreadExecutor() {
        int processors = Runtime.getRuntime().availableProcessors();
        log.info("Creating platform thread executor with {} threads for CPU work", processors);
        return Executors.newFixedThreadPool(processors);
    }
    
    /**
     * WebClient for external API calls
     */
    @Bean
    public WebClient webClient() {
        log.info("Creating WebClient for external API calls");
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(
                HttpClient.create()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                    .responseTimeout(Duration.ofSeconds(30))
            ))
            .build();
    }
    
    /**
     * ObjectMapper for JSON processing
     */
    @Bean
    public ObjectMapper objectMapper() {
        log.info("Creating ObjectMapper for JSON processing");
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
