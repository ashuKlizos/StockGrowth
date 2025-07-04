package com.StockGrowth.StockGrowth.service;

import com.StockGrowth.StockGrowth.dto.HistoricalPriceDTO;
import com.StockGrowth.StockGrowth.dto.HistoricalPriceResponse;
import com.StockGrowth.StockGrowth.dto.StockAnalysis;
import com.StockGrowth.StockGrowth.model.HistoricalPrice;
import com.StockGrowth.StockGrowth.model.Stock;
import com.StockGrowth.StockGrowth.repository.HistoricalPriceRepository;
import com.StockGrowth.StockGrowth.repository.StockRepository;
import jakarta.annotation.PreDestroy;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Transactional
@Log4j2
public class StockService {

    private static final String BASE_URL = "https://financialmodelingprep.com/api/v3";
    private static final String API_KEY = "25HLtnCgiRCFX9fcryDyWJzOxPEeXfKx";
    private final RestTemplate restTemplate;
    private final StockRepository stockRepository;
    private final HistoricalPriceRepository historicalPriceRepository;
    private final ExecutorService executorService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    public StockService(RestTemplate restTemplate, 
                       StockRepository stockRepository,
                       HistoricalPriceRepository historicalPriceRepository) {
        this.restTemplate = restTemplate;
        this.stockRepository = stockRepository;
        this.historicalPriceRepository = historicalPriceRepository;
        this.executorService = Executors.newFixedThreadPool(10); // Use 10 threads
    }

    @Transactional
    public List<Stock> fetchAndSaveStocksUnder100M() {
        String url = BASE_URL + "/stock-screener?marketCapLowerThan=100000000&apikey=" + API_KEY;
        
        ResponseEntity<List<Stock>> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Stock>>() {}
        );

        List<Stock> stocks = response.getBody();
        if (stocks != null) {
            List<Stock> usStocks = stocks.stream()
                .filter(stock -> "US".equals(stock.getCountry()))
                .toList();
            
            return stockRepository.saveAll(usStocks);
        }
        
        return List.of();
    }

    @Transactional
    public void refreshHistoricalData() {
        try {
            // First delete all existing historical data
            System.out.println("Deleting all existing historical data...");
            historicalPriceRepository.deleteAll();
            historicalPriceRepository.flush();
            System.out.println("Deleted all existing historical data");

            // Get all stocks
            List<Stock> stocks = stockRepository.findAll();
            LocalDate today = LocalDate.now();
            LocalDate thirtyDaysAgo = today.minusDays(30);

            // Create smaller batches (2 stocks per batch for less contention)
            List<List<Stock>> batches = new ArrayList<>();
            for (int i = 0; i < stocks.size(); i += 2) {
                batches.add(stocks.subList(i, Math.min(i + 2, stocks.size())));
            }

            // Process batches sequentially to avoid lock contention
            for (List<Stock> batch : batches) {
                try {
                    processBatch(batch, today, thirtyDaysAgo);
                    // Add a small delay between batches to reduce database contention
                    Thread.sleep(100);
                } catch (Exception e) {
                    System.err.println("Error processing batch with symbols: " + 
                        batch.stream().map(Stock::getSymbol).collect(Collectors.joining(", ")) +
                        " - " + e.getMessage());
                }
            }

            System.out.println("Completed refreshing historical data");

        } catch (Exception e) {
            System.err.println("Error refreshing historical data: " + e.getMessage());
        }
    }

    @Transactional
    protected void processBatch(List<Stock> batch, LocalDate today, LocalDate thirtyDaysAgo) {
        List<CompletableFuture<List<HistoricalPrice>>> futures = batch.stream()
            .map(stock -> CompletableFuture.supplyAsync(() -> 
                fetchHistoricalData(stock.getSymbol(), today, thirtyDaysAgo), 
                executorService))
            .collect(Collectors.toList());

        List<HistoricalPrice> batchData = futures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .collect(Collectors.toList());

        if (!batchData.isEmpty()) {
            historicalPriceRepository.saveAll(batchData);
            historicalPriceRepository.flush();
            System.out.println("Saved batch data for symbols: " + 
                batch.stream().map(Stock::getSymbol).collect(Collectors.joining(", ")));
        }
    }

    private List<HistoricalPrice> fetchHistoricalData(String symbol, LocalDate today, LocalDate thirtyDaysAgo) {
        String url = String.format("%s/historical-price-full/%s?from=%s&to=%s&apikey=%s",
            BASE_URL, symbol, thirtyDaysAgo.format(dateFormatter), today.format(dateFormatter), API_KEY);
        System.out.println("Fetching data for symbol: " + symbol);

        try {
            ResponseEntity<HistoricalPriceResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<HistoricalPriceResponse>() {}
            );

            if (response.getBody() != null && response.getBody().getHistorical() != null) {
                List<HistoricalPriceDTO> data = response.getBody().getHistorical();
                System.out.println("Received " + data.size() + " days of data for " + symbol);
                
                // Convert DTOs to entities
                return data.stream()
                    .map(dto -> {
                        HistoricalPrice hp = new HistoricalPrice();
                        hp.setSymbol(symbol);
                        hp.setDate(dto.getDate());
                        hp.setPrice(dto.getClose()); // Use close price
                        hp.setVolume(dto.getVolume());
                        hp.setLastUpdated(LocalDateTime.now());
                        return hp;
                    })
                    .collect(Collectors.toList());
            } else {
                System.out.println("No data received for " + symbol);
                return new ArrayList<>();
            }
        } catch (Exception e) {
            System.out.println("Error fetching data for " + symbol + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Transactional(readOnly = true)
    public List<StockAnalysis> analyzeStocks() {
        List<Stock> stocks = stockRepository.findAll();
        List<StockAnalysis> analyses = new ArrayList<>();

        for (Stock stock : stocks) {
            try {
                List<HistoricalPrice> historicalData = historicalPriceRepository
                    .findBySymbolOrderByDateDesc(stock.getSymbol());

                if (historicalData.isEmpty()) {
                    continue;
                }

                StockAnalysis analysis = analyzeStockData(stock, historicalData);
                analyses.add(analysis);
            } catch (Exception e) {
                System.err.println("Error analyzing stock: " + stock.getSymbol() + " - " + e.getMessage());
            }
        }

        return analyses;
    }

    private StockAnalysis analyzeStockData(Stock stock, List<HistoricalPrice> historicalData) {
        StockAnalysis analysis = new StockAnalysis();
        
        // Basic stock info
        analysis.setTicker(stock.getSymbol());
        analysis.setCompanyName(stock.getCompanyName());
        analysis.setMarketCap(stock.getMarketCap() != null ? stock.getMarketCap() : 0.0);
        
        if (historicalData == null || historicalData.isEmpty()) {
            log.info("No historical data for {}", stock.getSymbol());
            analysis.setVolume(0L);
            analysis.setAverageVolume(0.0);
            analysis.setHasUnusualVolume(false);
            analysis.setIsUptrending(false);
            analysis.setPriceChange1d(0.0);
            analysis.setPriceChange5d(0.0);
            analysis.setPriceChange30d(0.0);
            return analysis;
        }

        HistoricalPrice latest = historicalData.get(0);
        log.info("Latest data for {}: {}", stock.getSymbol(), latest);
        
        // Set volume data
        analysis.setVolume(latest.getVolume() != null ? latest.getVolume() : 0L);
        double avgVolume = calculateAverageVolume(historicalData);
        analysis.setAverageVolume(avgVolume);
        
        // Calculate price changes
        try {
            // 1-day change
            if (historicalData.size() > 1 && 
                latest.getPrice() != null && 
                historicalData.get(1).getPrice() != null) {
                analysis.setPriceChange1d(calculatePercentageChange(
                    historicalData.get(1).getPrice(), 
                    latest.getPrice()
                ));
            } else {
                analysis.setPriceChange1d(0.0);
            }

            // 5-day change
            if (historicalData.size() >= 5 && 
                latest.getPrice() != null && 
                historicalData.get(4).getPrice() != null) {
                analysis.setPriceChange5d(calculatePercentageChange(
                    historicalData.get(4).getPrice(), 
                    latest.getPrice()
                ));
            } else {
                analysis.setPriceChange5d(0.0);
            }

            // 30-day change (using last available price)
            if (historicalData.size() >= 15 && 
                latest.getPrice() != null && 
                historicalData.get(historicalData.size() - 1).getPrice() != null) {
                analysis.setPriceChange30d(calculatePercentageChange(
                    historicalData.get(historicalData.size() - 1).getPrice(), 
                    latest.getPrice()
                ));
            } else {
                analysis.setPriceChange30d(0.0);
            }
        } catch (Exception e) {
            log.error("Error calculating price changes for {}: {}", stock.getSymbol(), e.getMessage());
            analysis.setPriceChange1d(0.0);
            analysis.setPriceChange5d(0.0);
            analysis.setPriceChange30d(0.0);
        }

        // Set trend and volume indicators
        try {
            analysis.setHasUnusualVolume(isUnusualVolume(
                analysis.getVolume(), 
                analysis.getAverageVolume()
            ));
            analysis.setIsUptrending(isUptrending(historicalData));
        } catch (Exception e) {
            log.error("Error calculating indicators for {}: {}", stock.getSymbol(), e.getMessage());
            analysis.setHasUnusualVolume(false);
            analysis.setIsUptrending(false);
        }

        return analysis;
    }

    private double calculatePercentageChange(double oldValue, double newValue) {
        if (oldValue == 0 || Double.isNaN(oldValue) || Double.isNaN(newValue)) {
            return 0.0;
        }
        return ((newValue - oldValue) / oldValue) * 100;
    }

    private double calculateAverageVolume(List<HistoricalPrice> data) {
        if (data == null || data.isEmpty()) {
            return 0.0;
        }
        
        return data.stream()
            .filter(price -> price.getVolume() != null)
            .mapToLong(HistoricalPrice::getVolume)
            .average()
            .orElse(0.0);
    }

    private boolean isUnusualVolume(long currentVolume, double averageVolume) {
        if (averageVolume <= 0) {
            return false;
        }
        return currentVolume > (averageVolume * 2.0); // Volume is more than 2x average
    }

    private boolean isUptrending(List<HistoricalPrice> data) {
        if (data == null || data.size() < 10) { // Need at least 10 days of data
            return false;
        }

        try {
            // Get valid prices in chronological order (oldest to newest)
            List<Double> prices = data.stream()
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .map(HistoricalPrice::getPrice)
                .filter(price -> price != null && !Double.isNaN(price))
                .collect(Collectors.toList());

            if (prices.size() < 10) {
                return false;
            }

            // Calculate 5-day and 10-day moving averages
            List<Double> ma5 = calculateMovingAverage(prices, 5);
            List<Double> ma10 = calculateMovingAverage(prices, 10);

            if (ma5.isEmpty() || ma10.isEmpty()) {
                return false;
            }

            // Criteria for uptrend:
            // 1. Current price > 5-day MA
            // 2. 5-day MA > 10-day MA (shorter MA above longer MA indicates uptrend)
            // 3. Current price > price from 5 days ago (higher highs)
            // 4. Both MAs are sloping upward
            double currentPrice = prices.get(prices.size() - 1);
            double fiveDaysAgoPrice = prices.get(prices.size() - 6);
            
            boolean priceAboveMa5 = currentPrice > ma5.get(ma5.size() - 1);
            boolean ma5AboveMa10 = ma5.get(ma5.size() - 1) > ma10.get(ma10.size() - 1);
            boolean higherHigh = currentPrice > fiveDaysAgoPrice;
            
            // Check if MAs are sloping upward
            boolean ma5Sloping = isSlopingUpward(ma5.subList(Math.max(0, ma5.size() - 5), ma5.size()));
            boolean ma10Sloping = isSlopingUpward(ma10.subList(Math.max(0, ma10.size() - 5), ma10.size()));

            // Stock is considered uptrending if it meets the majority of criteria (at least 3 out of 5)
            int criteriaCount = 0;
            if (priceAboveMa5) criteriaCount++;
            if (ma5AboveMa10) criteriaCount++;
            if (higherHigh) criteriaCount++;
            if (ma5Sloping) criteriaCount++;
            if (ma10Sloping) criteriaCount++;

            return criteriaCount >= 3;

        } catch (Exception e) {
            log.error("Error checking uptrend for data size {}: {}", 
                data.size(), e.getMessage());
            return false;
        }
    }

    private List<Double> calculateMovingAverage(List<Double> prices, int period) {
        if (prices == null || prices.size() < period) {
            return List.of();
        }

        List<Double> movingAverages = new ArrayList<>();
        for (int i = 0; i <= prices.size() - period; i++) {
            double sum = 0;
            for (int j = i; j < i + period; j++) {
                sum += prices.get(j);
            }
            movingAverages.add(sum / period);
        }
        return movingAverages;
    }

    private boolean isSlopingUpward(List<Double> values) {
        if (values == null || values.size() < 2) {
            return false;
        }

        // Calculate linear regression slope
        int n = values.size();
        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumXX = 0;

        for (int i = 0; i < n; i++) {
            double x = i;
            double y = values.get(i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }

        // Calculate slope using least squares method
        double slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
        
        // Consider it upward sloping if slope is positive
        return slope > 0;
    }

    public List<Stock> getAllStocks() {
        return stockRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<HistoricalPrice> getHistoricalData(String symbol) {
        return historicalPriceRepository.findBySymbolOrderByDateDesc(symbol);
    }

    @Transactional(readOnly = true)
    public long getHistoricalDataCount() {
        return historicalPriceRepository.count();
    }

    @PreDestroy
    public void cleanup() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
} 