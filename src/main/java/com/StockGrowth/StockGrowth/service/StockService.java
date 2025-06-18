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
        analysis.setTicker(stock.getSymbol());
        analysis.setCompanyName(stock.getCompanyName());
        analysis.setMarketCap(stock.getMarketCap());
        
        if (historicalData.isEmpty()) {
            System.out.println("No historical data for " + stock.getSymbol());
            return analysis;
        }

        HistoricalPrice latest = historicalData.get(0);
        log.info("ticker  {}" , latest);
        analysis.setVolume(latest.getVolume());

        // For 1-day change, we need at least 2 days of data
        if (historicalData.size() > 1) {
            analysis.setPriceChange1d(calculatePercentageChange(
                historicalData.get(1).getPrice(), latest.getPrice()));
        }

        // For 5-day change, we need at least 5 trading days
        if (historicalData.size() >= 5) {
            analysis.setPriceChange5d(calculatePercentageChange(
                historicalData.get(4).getPrice(), latest.getPrice()));
        }

        // For 30-day change, we need at least 15 trading days (approximately 3 weeks)
        // This is because 30 calendar days typically have 21-22 trading days
        if (historicalData.size() >= 15) {
            analysis.setPriceChange30d(calculatePercentageChange(
                historicalData.get(historicalData.size() - 1).getPrice(), latest.getPrice()));
        } else {
            System.out.println("Not enough trading days for 30d change for " + stock.getSymbol() + 
                ". Need at least 15 trading days, got " + historicalData.size());
        }

        double avgVolume = calculateAverageVolume(historicalData);
        analysis.setAverageVolume(avgVolume);
        analysis.setHasUnusualVolume(isUnusualVolume(latest.getVolume(), avgVolume));
        analysis.setIsUptrending(isUptrending(historicalData));

        return analysis;
    }

    private double calculatePercentageChange(double oldValue, double newValue) {
        if (oldValue == 0) return 0;
        return ((newValue - oldValue) / oldValue) * 100;
    }

    private boolean isUptrending(List<HistoricalPrice> data) {
        if (data.size() < 5) return false;

        List<Double> movingAverages = new ArrayList<>();
        for (int i = 0; i < data.size() - 4; i++) {
            double sum = 0;
            for (int j = i; j < i + 5; j++) {
                sum += data.get(j).getPrice();
            }
            movingAverages.add(sum / 5);
        }

        int increasingCount = 0;
        for (int i = 0; i < movingAverages.size() - 1; i++) {
            if (movingAverages.get(i) > movingAverages.get(i + 1)) {
                increasingCount++;
            }
        }

        return (double) increasingCount / (movingAverages.size() - 1) > 0.6;
    }

    private double calculateAverageVolume(List<HistoricalPrice> data) {
        if (data.isEmpty()) return 0;
        return data.stream()
            .mapToLong(HistoricalPrice::getVolume)
            .average()
            .orElse(0.0);
    }

    private boolean isUnusualVolume(long currentVolume, double averageVolume) {
        return averageVolume > 0 && currentVolume > (averageVolume * 1.5);
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