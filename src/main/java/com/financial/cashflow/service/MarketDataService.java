package com.financial.cashflow.service;

import com.financial.cashflow.exception.MarketDataException;
import com.financial.cashflow.model.CashFlowRequest;
import com.financial.cashflow.model.MarketData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Market Data Service with hybrid strategy support
 * Uses virtual threads for I/O operations
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MarketDataService {
    
    private final WebClient webClient;
    private final MarketDataCache marketDataCache;
    
    // Virtual threads for I/O operations
    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    
    /**
     * Load market data based on strategy
     */
    public MarketData loadMarketData(CashFlowRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Loading market data for request: {}", request.getRequestId());
        
        try {
            CashFlowRequest.MarketDataStrategy strategy = request.getMarketDataStrategy();
            
            switch (strategy.getMode()) {
                case HYBRID:
                    return loadHybridMarketData(request);
                case SELF_CONTAINED:
                    return loadSelfContainedMarketData(request);
                case ENDPOINTS:
                    return loadFromEndpoints(request);
                default:
                    throw new IllegalArgumentException("Unsupported market data mode: " + strategy.getMode());
            }
        } catch (Exception e) {
            log.error("Failed to load market data for request: {}", request.getRequestId(), e);
            throw new MarketDataException("Failed to load market data", e);
        } finally {
            log.info("Market data loading completed in {}ms", System.currentTimeMillis() - startTime);
        }
    }
    
    /**
     * Load market data using hybrid strategy (cache + endpoints)
     */
    private MarketData loadHybridMarketData(CashFlowRequest request) {
        try {
            // Try cache first
            MarketData cached = marketDataCache.get(request.getCacheKey());
            if (cached != null && cached.getIsValid()) {
                log.info("Using cached market data for request: {}", request.getRequestId());
                return cached;
            }
            
            // Load from endpoints using virtual threads
            return loadFromEndpointsAsync(request);
        } catch (Exception e) {
            log.warn("Hybrid market data loading failed, falling back to self-contained", e);
            return loadSelfContainedMarketData(request);
        }
    }
    
    /**
     * Load market data from external endpoints asynchronously
     */
    private MarketData loadFromEndpointsAsync(CashFlowRequest request) {
        try {
            // Load price data
            CompletableFuture<MarketData.PriceData> priceFuture = CompletableFuture.supplyAsync(() -> {
                return webClient.get()
                    .uri("/prices/{symbol}", request.getUnderlying())
                    .retrieve()
                    .bodyToMono(MarketData.PriceData.class)
                    .block();
            }, virtualThreadExecutor);
            
            // Load rate data
            CompletableFuture<MarketData.RateData> rateFuture = CompletableFuture.supplyAsync(() -> {
                return webClient.get()
                    .uri("/rates/{index}", request.getIndex())
                    .retrieve()
                    .bodyToMono(MarketData.RateData.class)
                    .block();
            }, virtualThreadExecutor);
            
            // Load dividend data
            CompletableFuture<MarketData.DividendData> dividendFuture = CompletableFuture.supplyAsync(() -> {
                return webClient.get()
                    .uri("/dividends/{symbol}", request.getUnderlying())
                    .retrieve()
                    .bodyToMono(MarketData.DividendData.class)
                    .block();
            }, virtualThreadExecutor);
            
            // Wait for all futures to complete
            CompletableFuture.allOf(priceFuture, rateFuture, dividendFuture).join();
            
            // Build market data
            MarketData marketData = MarketData.builder()
                .price(priceFuture.get())
                .rate(rateFuture.get())
                .dividends(dividendFuture.get())
                .timestamp(LocalDateTime.now())
                .source("ENDPOINTS")
                .isValid(true)
                .validUntil(LocalDateTime.now().plusHours(24))
                .build();
            
            // Cache the result
            marketDataCache.put(request.getCacheKey(), marketData);
            
            return marketData;
        } catch (Exception e) {
            log.error("Failed to load market data from endpoints", e);
            throw new MarketDataException("Failed to load market data from endpoints", e);
        }
    }
    
    /**
     * Load market data from external endpoints synchronously
     */
    private MarketData loadFromEndpoints(CashFlowRequest request) {
        try {
            // Load price data
            MarketData.PriceData priceData = webClient.get()
                .uri("/prices/{symbol}", request.getUnderlying())
                .retrieve()
                .bodyToMono(MarketData.PriceData.class)
                .block();
            
            // Load rate data
            MarketData.RateData rateData = webClient.get()
                .uri("/rates/{index}", request.getIndex())
                .retrieve()
                .bodyToMono(MarketData.RateData.class)
                .block();
            
            // Load dividend data
            MarketData.DividendData dividendData = webClient.get()
                .uri("/dividends/{symbol}", request.getUnderlying())
                .retrieve()
                .bodyToMono(MarketData.DividendData.class)
                .block();
            
            // Build market data
            MarketData marketData = MarketData.builder()
                .price(priceData)
                .rate(rateData)
                .dividends(dividendData)
                .timestamp(LocalDateTime.now())
                .source("ENDPOINTS")
                .isValid(true)
                .validUntil(LocalDateTime.now().plusHours(24))
                .build();
            
            // Cache the result
            marketDataCache.put(request.getCacheKey(), marketData);
            
            return marketData;
        } catch (Exception e) {
            log.error("Failed to load market data from endpoints", e);
            throw new MarketDataException("Failed to load market data from endpoints", e);
        }
    }
    
    /**
     * Load self-contained market data
     */
    private MarketData loadSelfContainedMarketData(CashFlowRequest request) {
        log.info("Loading self-contained market data for request: {}", request.getRequestId());
        
        CashFlowRequest.MarketDataContainer container = request.getMarketData();
        if (container == null || container.getData() == null) {
            throw new MarketDataException("No self-contained market data provided");
        }
        
        return MarketData.builder()
            .price(extractPriceData(container.getData(), request.getUnderlying()))
            .rate(extractRateData(container.getData(), request.getIndex()))
            .dividends(extractDividendData(container.getData(), request.getUnderlying()))
            .timestamp(LocalDateTime.now())
            .source("SELF_CONTAINED")
            .isValid(true)
            .validUntil(LocalDateTime.now().plusHours(24))
            .build();
    }
    
    /**
     * Extract price data from container
     */
    private MarketData.PriceData extractPriceData(CashFlowRequest.MarketDataContent content, String symbol) {
        return content.getSecurities().stream()
            .filter(security -> security.getSymbol().equals(symbol))
            .findFirst()
            .map(this::buildPriceData)
            .orElseThrow(() -> new MarketDataException("Price data not found for symbol: " + symbol));
    }
    
    /**
     * Extract rate data from container
     */
    private MarketData.RateData extractRateData(CashFlowRequest.MarketDataContent content, String index) {
        return content.getRates().stream()
            .filter(rate -> rate.getIndex().equals(index))
            .findFirst()
            .map(this::buildRateData)
            .orElseThrow(() -> new MarketDataException("Rate data not found for index: " + index));
    }
    
    /**
     * Extract dividend data from container
     */
    private MarketData.DividendData extractDividendData(CashFlowRequest.MarketDataContent content, String symbol) {
        return content.getDividends().stream()
            .filter(dividend -> dividend.getSymbol().equals(symbol))
            .findFirst()
            .map(this::buildDividendData)
            .orElseThrow(() -> new MarketDataException("Dividend data not found for symbol: " + symbol));
    }
    
    /**
     * Build price data from security data
     */
    private MarketData.PriceData buildPriceData(CashFlowRequest.SecurityData security) {
        return MarketData.PriceData.builder()
            .symbol(security.getSymbol())
            .basePrice(security.getBasePrice())
            .baseDate(security.getBaseDate())
            .changes(security.getChanges().stream()
                .map(change -> MarketData.PriceData.PriceChange.builder()
                    .date(change.getDate())
                    .price(change.getPrice())
                    .build())
                .toList())
            .build();
    }
    
    /**
     * Build rate data from rate data
     */
    private MarketData.RateData buildRateData(CashFlowRequest.RateData rate) {
        return MarketData.RateData.builder()
            .index(rate.getIndex())
            .baseRate(rate.getBaseRate())
            .baseDate(rate.getBaseDate())
            .changes(rate.getChanges().stream()
                .map(change -> MarketData.RateData.RateChange.builder()
                    .date(change.getDate())
                    .rate(change.getRate())
                    .build())
                .toList())
            .build();
    }
    
    /**
     * Build dividend data from dividend data
     */
    private MarketData.DividendData buildDividendData(CashFlowRequest.DividendData dividend) {
        return MarketData.DividendData.builder()
            .symbol(dividend.getSymbol())
            .dividends(dividend.getDividends().stream()
                .map(div -> MarketData.DividendData.Dividend.builder()
                    .exDate(div.getExDate())
                    .amount(div.getAmount())
                    .currency(div.getCurrency())
                    .build())
                .toList())
            .build();
    }
}
