package com.financial.cashflow.service.impl;

import com.financial.cashflow.domain.CashFlowRequestContent;
import com.financial.cashflow.service.MarketDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Implementation of MarketDataService with comprehensive market data handling.
 * Uses virtual threads for I/O operations like external API calls.
 * Provides caching, fallback strategies, and unified access to market data.
 */
@Slf4j
@Service
public class MarketDataServiceImpl implements MarketDataService {

    @Autowired
    @Qualifier("virtualThreadExecutor")
    private ExecutorService virtualThreadExecutor;

    // Cache for market data
    private final Map<String, Double> priceCache = new ConcurrentHashMap<>();
    private final Map<String, Double> rateCache = new ConcurrentHashMap<>();
    private final Map<String, Double> fxRateCache = new ConcurrentHashMap<>();
    private final Map<String, Double> dividendRateCache = new ConcurrentHashMap<>();
    
    // Cache statistics
    private long hitCount = 0;
    private long missCount = 0;

    @Override
    public Optional<Double> getPrice(String instrumentId, LocalDate date, CashFlowRequestContent request) {
        String cacheKey = "PRICE_" + instrumentId + "_" + date.toString();
        
        if (priceCache.containsKey(cacheKey)) {
            hitCount++;
            return Optional.of(priceCache.get(cacheKey));
        }
        
        missCount++;
        
        // Try to get from embedded market data first
        if (request.getMarketData() != null && request.getMarketData().getPrices() != null) {
            for (CashFlowRequestContent.PriceData priceData : request.getMarketData().getPrices()) {
                if (priceData.getInstrumentId().equals(instrumentId) && 
                    priceData.getPriceDate().equals(date)) {
                    double price = priceData.getPrice();
                    priceCache.put(cacheKey, price);
                    return Optional.of(price);
                }
            }
        }
        
        // Try external data sources using virtual threads for I/O
        try {
            CompletableFuture<Optional<Double>> externalPriceFuture = 
                CompletableFuture.supplyAsync(() -> getExternalPrice(instrumentId, date), virtualThreadExecutor);
            
            Optional<Double> externalPrice = externalPriceFuture.get();
            if (externalPrice.isPresent()) {
                priceCache.put(cacheKey, externalPrice.get());
                return externalPrice;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch external price for {} on {}", instrumentId, date, e);
        }
        
        return Optional.empty();
    }

    @Override
    public double getPriceWithFallback(String instrumentId, LocalDate date, double fallbackPrice, 
                                     CashFlowRequestContent request) {
        return getPrice(instrumentId, date, request).orElse(fallbackPrice);
    }

    @Override
    public Optional<Double> getRate(String rateIndex, LocalDate date, String tenor, CashFlowRequestContent request) {
        String cacheKey = "RATE_" + rateIndex + "_" + tenor + "_" + date.toString();
        
        if (rateCache.containsKey(cacheKey)) {
            hitCount++;
            return Optional.of(rateCache.get(cacheKey));
        }
        
        missCount++;
        
        // Try to get from embedded market data first
        if (request.getMarketData() != null && request.getMarketData().getRates() != null) {
            for (CashFlowRequestContent.RateData rateData : request.getMarketData().getRates()) {
                if (rateData.getRateIndex().equals(rateIndex) && 
                    rateData.getRateDate().equals(date) &&
                    (rateData.getTenor() == null || rateData.getTenor().equals(tenor))) {
                    double rate = rateData.getRate();
                    rateCache.put(cacheKey, rate);
                    return Optional.of(rate);
                }
            }
        }
        
        // Try external data sources using virtual threads for I/O
        try {
            CompletableFuture<Optional<Double>> externalRateFuture = 
                CompletableFuture.supplyAsync(() -> getExternalRate(rateIndex, date, tenor), virtualThreadExecutor);
            
            Optional<Double> externalRate = externalRateFuture.get();
            if (externalRate.isPresent()) {
                rateCache.put(cacheKey, externalRate.get());
                return externalRate;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch external rate for {} {} on {}", rateIndex, tenor, date, e);
        }
        
        return Optional.empty();
    }

    @Override
    public double getRateWithFallback(String rateIndex, LocalDate date, String tenor, double fallbackRate, 
                                    CashFlowRequestContent request) {
        return getRate(rateIndex, date, tenor, request).orElse(fallbackRate);
    }

    @Override
    public Optional<Double> getFxRate(String fromCurrency, String toCurrency, LocalDate date, 
                                    CashFlowRequestContent request) {
        String cacheKey = "FX_" + fromCurrency + "_" + toCurrency + "_" + date.toString();
        
        if (fxRateCache.containsKey(cacheKey)) {
            hitCount++;
            return Optional.of(fxRateCache.get(cacheKey));
        }
        
        missCount++;
        
        // Try to get from embedded market data first
        if (request.getMarketData() != null && request.getMarketData().getFxRates() != null) {
            for (CashFlowRequestContent.FxRateData fxRateData : request.getMarketData().getFxRates()) {
                if (fxRateData.getFromCurrency().equals(fromCurrency) && 
                    fxRateData.getToCurrency().equals(toCurrency) &&
                    fxRateData.getRateDate().equals(date)) {
                    double fxRate = fxRateData.getFxRate();
                    fxRateCache.put(cacheKey, fxRate);
                    return Optional.of(fxRate);
                }
            }
        }
        
        // Try external data sources using virtual threads for I/O
        try {
            CompletableFuture<Optional<Double>> externalFxRateFuture = 
                CompletableFuture.supplyAsync(() -> getExternalFxRate(fromCurrency, toCurrency, date), virtualThreadExecutor);
            
            Optional<Double> externalFxRate = externalFxRateFuture.get();
            if (externalFxRate.isPresent()) {
                fxRateCache.put(cacheKey, externalFxRate.get());
                return externalFxRate;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch external FX rate for {}/{} on {}", fromCurrency, toCurrency, date, e);
        }
        
        return Optional.empty();
    }

    @Override
    public double getFxRateWithFallback(String fromCurrency, String toCurrency, LocalDate date, double fallbackRate, 
                                       CashFlowRequestContent request) {
        return getFxRate(fromCurrency, toCurrency, date, request).orElse(fallbackRate);
    }

    @Override
    public Optional<Double> getDividendRate(String underlier, LocalDate date, CashFlowRequestContent request) {
        String cacheKey = "DIVIDEND_" + underlier + "_" + date.toString();
        
        if (dividendRateCache.containsKey(cacheKey)) {
            hitCount++;
            return Optional.of(dividendRateCache.get(cacheKey));
        }
        
        missCount++;
        
        // Try external data sources using virtual threads for I/O
        try {
            CompletableFuture<Optional<Double>> externalDividendRateFuture = 
                CompletableFuture.supplyAsync(() -> getExternalDividendRate(underlier, date), virtualThreadExecutor);
            
            Optional<Double> externalDividendRate = externalDividendRateFuture.get();
            if (externalDividendRate.isPresent()) {
                dividendRateCache.put(cacheKey, externalDividendRate.get());
                return externalDividendRate;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch external dividend rate for {} on {}", underlier, date, e);
        }
        
        // Use default dividend rate if no data available
        double defaultRate = 0.02; // 2% annual dividend rate
        dividendRateCache.put(cacheKey, defaultRate);
        return Optional.of(defaultRate);
    }

    @Override
    public double getDividendRateWithFallback(String underlier, LocalDate date, double fallbackRate, 
                                            CashFlowRequestContent request) {
        return getDividendRate(underlier, date, request).orElse(fallbackRate);
    }

    @Override
    public List<CashFlowRequestContent.PriceData> getPrices(List<String> instrumentIds, LocalDate date, 
                                                           CashFlowRequestContent request) {
        List<CashFlowRequestContent.PriceData> prices = new ArrayList<>();
        
        // Use virtual threads for parallel price fetching
        List<CompletableFuture<Optional<Double>>> priceFutures = instrumentIds.stream()
            .map(instrumentId -> CompletableFuture.supplyAsync(
                () -> getPrice(instrumentId, date, request), virtualThreadExecutor))
            .toList();
        
        // Collect results
        for (int i = 0; i < instrumentIds.size(); i++) {
            try {
                Optional<Double> price = priceFutures.get(i).get();
                if (price.isPresent()) {
                    prices.add(CashFlowRequestContent.PriceData.builder()
                        .instrumentId(instrumentIds.get(i))
                        .price(price.get())
                        .currency("USD") // Default currency
                        .priceDate(date)
                        .priceType("CLOSE")
                        .build());
                }
            } catch (Exception e) {
                log.warn("Failed to get price for instrument: {}", instrumentIds.get(i), e);
            }
        }
        
        return prices;
    }

    @Override
    public List<CashFlowRequestContent.RateData> getRates(List<String> rateIndices, LocalDate date, String tenor, 
                                                          CashFlowRequestContent request) {
        List<CashFlowRequestContent.RateData> rates = new ArrayList<>();
        
        // Use virtual threads for parallel rate fetching
        List<CompletableFuture<Optional<Double>>> rateFutures = rateIndices.stream()
            .map(rateIndex -> CompletableFuture.supplyAsync(
                () -> getRate(rateIndex, date, tenor, request), virtualThreadExecutor))
            .toList();
        
        // Collect results
        for (int i = 0; i < rateIndices.size(); i++) {
            try {
                Optional<Double> rate = rateFutures.get(i).get();
                if (rate.isPresent()) {
                    rates.add(CashFlowRequestContent.RateData.builder()
                        .rateIndex(rateIndices.get(i))
                        .rate(rate.get())
                        .rateDate(date)
                        .tenor(tenor)
                        .build());
                }
            } catch (Exception e) {
                log.warn("Failed to get rate for index: {}", rateIndices.get(i), e);
            }
        }
        
        return rates;
    }

    @Override
    public List<CashFlowRequestContent.FxRateData> getFxRates(List<String> currencyPairs, LocalDate date, 
                                                            CashFlowRequestContent request) {
        List<CashFlowRequestContent.FxRateData> fxRates = new ArrayList<>();
        
        // Use virtual threads for parallel FX rate fetching
        List<CompletableFuture<Optional<Double>>> fxRateFutures = currencyPairs.stream()
            .map(currencyPair -> {
                String[] currencies = currencyPair.split("/");
                if (currencies.length == 2) {
                    return CompletableFuture.supplyAsync(
                        () -> getFxRate(currencies[0], currencies[1], date, request), virtualThreadExecutor);
                }
                return CompletableFuture.completedFuture(Optional.<Double>empty());
            })
            .toList();
        
        // Collect results
        for (int i = 0; i < currencyPairs.size(); i++) {
            try {
                Optional<Double> fxRate = fxRateFutures.get(i).get();
                if (fxRate.isPresent()) {
                    String[] currencies = currencyPairs.get(i).split("/");
                    if (currencies.length == 2) {
                        fxRates.add(CashFlowRequestContent.FxRateData.builder()
                            .fromCurrency(currencies[0])
                            .toCurrency(currencies[1])
                            .fxRate(fxRate.get())
                            .rateDate(date)
                            .build());
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to get FX rate for pair: {}", currencyPairs.get(i), e);
            }
        }
        
        return fxRates;
    }

    @Override
    public boolean isDataAvailable(String instrumentId, LocalDate date, CashFlowRequestContent request) {
        return getPrice(instrumentId, date, request).isPresent();
    }

    @Override
    public String getDataSource(String instrumentId, LocalDate date, CashFlowRequestContent request) {
        // Check embedded data first
        if (request.getMarketData() != null && request.getMarketData().getPrices() != null) {
            for (CashFlowRequestContent.PriceData priceData : request.getMarketData().getPrices()) {
                if (priceData.getInstrumentId().equals(instrumentId) && 
                    priceData.getPriceDate().equals(date)) {
                    return "EMBEDDED";
                }
            }
        }
        
        // Check external sources
        if (getExternalPrice(instrumentId, date).isPresent()) {
            return "EXTERNAL";
        }
        
        return "UNAVAILABLE";
    }

    @Override
    public void refreshCache(CashFlowRequestContent request) {
        log.info("Refreshing market data cache");
        
        // Clear existing cache
        clearCache();
        
        // Pre-populate cache with embedded data
        if (request.getMarketData() != null) {
            if (request.getMarketData().getPrices() != null) {
                for (CashFlowRequestContent.PriceData priceData : request.getMarketData().getPrices()) {
                    String cacheKey = "PRICE_" + priceData.getInstrumentId() + "_" + priceData.getPriceDate().toString();
                    priceCache.put(cacheKey, priceData.getPrice());
                }
            }
            
            if (request.getMarketData().getRates() != null) {
                for (CashFlowRequestContent.RateData rateData : request.getMarketData().getRates()) {
                    String cacheKey = "RATE_" + rateData.getRateIndex() + "_" + 
                                   (rateData.getTenor() != null ? rateData.getTenor() : "3M") + "_" + 
                                   rateData.getRateDate().toString();
                    rateCache.put(cacheKey, rateData.getRate());
                }
            }
            
            if (request.getMarketData().getFxRates() != null) {
                for (CashFlowRequestContent.FxRateData fxRateData : request.getMarketData().getFxRates()) {
                    String cacheKey = "FX_" + fxRateData.getFromCurrency() + "_" + 
                                    fxRateData.getToCurrency() + "_" + fxRateData.getRateDate().toString();
                    fxRateCache.put(cacheKey, fxRateData.getFxRate());
                }
            }
        }
        
        log.info("Market data cache refreshed. Prices: {}, Rates: {}, FX Rates: {}", 
            priceCache.size(), rateCache.size(), fxRateCache.size());
    }

    @Override
    public void clearCache() {
        priceCache.clear();
        rateCache.clear();
        fxRateCache.clear();
        dividendRateCache.clear();
        hitCount = 0;
        missCount = 0;
        log.info("Market data cache cleared");
    }

    @Override
    public CacheStatistics getCacheStatistics() {
        return new CacheStatistics(hitCount, missCount);
    }

    // Helper methods for external data sources (placeholder implementations)

    private Optional<Double> getExternalPrice(String instrumentId, LocalDate date) {
        // Placeholder for external price data source
        // In a real implementation, this would call external APIs or databases
        log.debug("Fetching external price for {} on {}", instrumentId, date);
        
        // Simulate external data with some default values
        if ("IBM".equals(instrumentId)) {
            return Optional.of(150.0 + Math.random() * 10.0); // Simulate price variation
        } else if ("AAPL".equals(instrumentId)) {
            return Optional.of(180.0 + Math.random() * 15.0);
        } else if ("MSFT".equals(instrumentId)) {
            return Optional.of(300.0 + Math.random() * 20.0);
        }
        
        return Optional.empty();
    }

    private Optional<Double> getExternalRate(String rateIndex, LocalDate date, String tenor) {
        // Placeholder for external rate data source
        log.debug("Fetching external rate for {} {} on {}", rateIndex, tenor, date);
        
        // Simulate external data with some default values
        if ("LIBOR".equals(rateIndex)) {
            return Optional.of(0.05 + Math.random() * 0.02); // 5-7% range
        } else if ("SOFR".equals(rateIndex)) {
            return Optional.of(0.04 + Math.random() * 0.015); // 4-5.5% range
        }
        
        return Optional.empty();
    }

    private Optional<Double> getExternalFxRate(String fromCurrency, String toCurrency, LocalDate date) {
        // Placeholder for external FX rate data source
        log.debug("Fetching external FX rate for {}/{} on {}", fromCurrency, toCurrency, date);
        
        // Simulate external data with some default values
        if ("USD".equals(fromCurrency) && "EUR".equals(toCurrency)) {
            return Optional.of(0.85 + Math.random() * 0.05); // 0.85-0.90 range
        } else if ("EUR".equals(fromCurrency) && "USD".equals(toCurrency)) {
            return Optional.of(1.15 + Math.random() * 0.05); // 1.15-1.20 range
        } else if ("USD".equals(fromCurrency) && "GBP".equals(toCurrency)) {
            return Optional.of(0.75 + Math.random() * 0.03); // 0.75-0.78 range
        }
        
        return Optional.empty();
    }

    private Optional<Double> getExternalDividendRate(String underlier, LocalDate date) {
        // Placeholder for external dividend rate data source
        log.debug("Fetching external dividend rate for {} on {}", underlier, date);
        
        // Simulate external data with some default values
        if ("IBM".equals(underlier)) {
            return Optional.of(0.025); // 2.5% dividend yield
        } else if ("AAPL".equals(underlier)) {
            return Optional.of(0.015); // 1.5% dividend yield
        } else if ("MSFT".equals(underlier)) {
            return Optional.of(0.01); // 1.0% dividend yield
        }
        
        return Optional.empty();
    }
}
