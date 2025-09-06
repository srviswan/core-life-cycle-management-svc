package com.financial.cashflow.service;

import com.financial.cashflow.domain.CashFlowRequestContent;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for handling market data operations.
 * Provides unified access to market data including:
 * - Price data for equity instruments
 * - Rate data for interest rate instruments
 * - FX rates for currency conversions
 * - Caching and fallback strategies
 * - Real-time and historical data
 */
public interface MarketDataService {

    /**
     * Get current market price for an instrument.
     * 
     * @param instrumentId The instrument ID
     * @param date The date for price
     * @param request The full request context
     * @return Optional containing the price if available
     */
    Optional<Double> getPrice(String instrumentId, LocalDate date, CashFlowRequestContent request);

    /**
     * Get current market price for an instrument with fallback strategy.
     * 
     * @param instrumentId The instrument ID
     * @param date The date for price
     * @param fallbackPrice Fallback price if market data unavailable
     * @param request The full request context
     * @return The market price or fallback price
     */
    double getPriceWithFallback(String instrumentId, LocalDate date, double fallbackPrice, 
                               CashFlowRequestContent request);

    /**
     * Get interest rate for a rate index.
     * 
     * @param rateIndex The rate index (e.g., LIBOR, SOFR)
     * @param date The date for rate
     * @param tenor The tenor (e.g., 1M, 3M, 6M)
     * @param request The full request context
     * @return Optional containing the rate if available
     */
    Optional<Double> getRate(String rateIndex, LocalDate date, String tenor, CashFlowRequestContent request);

    /**
     * Get interest rate with fallback strategy.
     * 
     * @param rateIndex The rate index
     * @param date The date for rate
     * @param tenor The tenor
     * @param fallbackRate Fallback rate if market data unavailable
     * @param request The full request context
     * @return The market rate or fallback rate
     */
    double getRateWithFallback(String rateIndex, LocalDate date, String tenor, double fallbackRate, 
                              CashFlowRequestContent request);

    /**
     * Get FX rate for currency pair.
     * 
     * @param fromCurrency The from currency
     * @param toCurrency The to currency
     * @param date The date for FX rate
     * @param request The full request context
     * @return Optional containing the FX rate if available
     */
    Optional<Double> getFxRate(String fromCurrency, String toCurrency, LocalDate date, 
                               CashFlowRequestContent request);

    /**
     * Get FX rate with fallback strategy.
     * 
     * @param fromCurrency The from currency
     * @param toCurrency The to currency
     * @param date The date for FX rate
     * @param fallbackRate Fallback rate if market data unavailable
     * @param request The full request context
     * @return The FX rate or fallback rate
     */
    double getFxRateWithFallback(String fromCurrency, String toCurrency, LocalDate date, double fallbackRate, 
                                 CashFlowRequestContent request);

    /**
     * Get dividend rate for an underlier.
     * 
     * @param underlier The underlier
     * @param date The date
     * @param request The full request context
     * @return Optional containing the dividend rate if available
     */
    Optional<Double> getDividendRate(String underlier, LocalDate date, CashFlowRequestContent request);

    /**
     * Get dividend rate with fallback strategy.
     * 
     * @param underlier The underlier
     * @param date The date
     * @param fallbackRate Fallback dividend rate
     * @param request The full request context
     * @return The dividend rate or fallback rate
     */
    double getDividendRateWithFallback(String underlier, LocalDate date, double fallbackRate, 
                                      CashFlowRequestContent request);

    /**
     * Get all prices for a list of instruments.
     * 
     * @param instrumentIds List of instrument IDs
     * @param date The date for prices
     * @param request The full request context
     * @return List of price data
     */
    List<CashFlowRequestContent.PriceData> getPrices(List<String> instrumentIds, LocalDate date, 
                                                    CashFlowRequestContent request);

    /**
     * Get all rates for a list of rate indices.
     * 
     * @param rateIndices List of rate indices
     * @param date The date for rates
     * @param tenor The tenor
     * @param request The full request context
     * @return List of rate data
     */
    List<CashFlowRequestContent.RateData> getRates(List<String> rateIndices, LocalDate date, String tenor, 
                                                  CashFlowRequestContent request);

    /**
     * Get all FX rates for a list of currency pairs.
     * 
     * @param currencyPairs List of currency pairs (e.g., ["USD/EUR", "EUR/GBP"])
     * @param date The date for FX rates
     * @param request The full request context
     * @return List of FX rate data
     */
    List<CashFlowRequestContent.FxRateData> getFxRates(List<String> currencyPairs, LocalDate date, 
                                                       CashFlowRequestContent request);

    /**
     * Check if market data is available for an instrument.
     * 
     * @param instrumentId The instrument ID
     * @param date The date
     * @param request The full request context
     * @return True if data is available, false otherwise
     */
    boolean isDataAvailable(String instrumentId, LocalDate date, CashFlowRequestContent request);

    /**
     * Get the data source for a specific instrument and date.
     * 
     * @param instrumentId The instrument ID
     * @param date The date
     * @param request The full request context
     * @return Data source description
     */
    String getDataSource(String instrumentId, LocalDate date, CashFlowRequestContent request);

    /**
     * Refresh market data cache.
     * 
     * @param request The full request context
     */
    void refreshCache(CashFlowRequestContent request);

    /**
     * Clear market data cache.
     */
    void clearCache();

    /**
     * Get cache statistics.
     * 
     * @return Cache statistics
     */
    CacheStatistics getCacheStatistics();

    /**
     * Represents cache statistics.
     */
    class CacheStatistics {
        private final long hitCount;
        private final long missCount;
        private final long totalRequests;
        private final double hitRate;

        public CacheStatistics(long hitCount, long missCount) {
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.totalRequests = hitCount + missCount;
            this.hitRate = totalRequests > 0 ? (double) hitCount / totalRequests : 0.0;
        }

        public long getHitCount() { return hitCount; }
        public long getMissCount() { return missCount; }
        public long getTotalRequests() { return totalRequests; }
        public double getHitRate() { return hitRate; }
    }
}
