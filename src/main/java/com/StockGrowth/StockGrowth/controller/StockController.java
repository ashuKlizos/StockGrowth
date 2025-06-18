package com.StockGrowth.StockGrowth.controller;

import com.StockGrowth.StockGrowth.dto.StockAnalysis;
import com.StockGrowth.StockGrowth.model.HistoricalPrice;
import com.StockGrowth.StockGrowth.model.Stock;
import com.StockGrowth.StockGrowth.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockService stockService;

    @Autowired
    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/fetch-under-100m")
    public ResponseEntity<List<Stock>> fetchStocksUnder100M() {
        List<Stock> stocks = stockService.fetchAndSaveStocksUnder100M();
        return ResponseEntity.ok(stocks);
    }

    @GetMapping("/analyze")
    public ResponseEntity<List<StockAnalysis>> analyzeStocks() {
        List<StockAnalysis> analysis = stockService.analyzeStocks();
        return ResponseEntity.ok(analysis);
    }

    @PostMapping("/refresh-historical-data")
    public ResponseEntity<String> refreshHistoricalData() {
        try {
            stockService.refreshHistoricalData();
            return ResponseEntity.ok("Historical data refresh completed successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error refreshing historical data: " + e.getMessage());
        }
    }

    @GetMapping("/analyze/uptrend")
    public ResponseEntity<List<StockAnalysis>> getUptrendingStocks() {
        List<StockAnalysis> analyses = stockService.analyzeStocks()
            .stream()
            .filter(StockAnalysis::getIsUptrending)
            .sorted(Comparator.comparing(StockAnalysis::getPriceChange30d).reversed())
            .collect(Collectors.toList());
        return ResponseEntity.ok(analyses);
    }

    @GetMapping("/analyze/volume-spike")
    public ResponseEntity<List<StockAnalysis>> getUnusualVolumeStocks() {
        List<StockAnalysis> analyses = stockService.analyzeStocks()
            .stream()
            .filter(StockAnalysis::getHasUnusualVolume)
            .sorted(Comparator.comparing((StockAnalysis analysis) -> 
                analysis.getVolume().doubleValue() / analysis.getAverageVolume()).reversed())
            .collect(Collectors.toList());
        return ResponseEntity.ok(analyses);
    }

    @GetMapping("/analyze/gainers")
    public ResponseEntity<List<StockAnalysis>> getTopGainers(
            @RequestParam(defaultValue = "30") String period) {
        List<StockAnalysis> analyses = stockService.analyzeStocks();
        
        Comparator<StockAnalysis> comparator = switch(period) {
            case "1" -> Comparator.comparing(StockAnalysis::getPriceChange1d);
            case "5" -> Comparator.comparing(StockAnalysis::getPriceChange5d);
            default -> Comparator.comparing(StockAnalysis::getPriceChange30d);
        };

        List<StockAnalysis> sortedAnalyses = analyses.stream()
            .sorted(comparator.reversed())
            .collect(Collectors.toList());

        return ResponseEntity.ok(sortedAnalyses);
    }

    @GetMapping("/analyze/filter")
    public ResponseEntity<List<StockAnalysis>> filterStocks(
            @RequestParam(required = false) Double minMarketCap,
            @RequestParam(required = false) Double maxMarketCap,
            @RequestParam(required = false) Double minPrice30dChange,
            @RequestParam(required = false) Boolean uptrendOnly,
            @RequestParam(required = false) Boolean unusualVolumeOnly) {
        
        List<StockAnalysis> analyses = stockService.analyzeStocks()
            .stream()
            .filter(analysis -> minMarketCap == null || analysis.getMarketCap() >= minMarketCap)
            .filter(analysis -> maxMarketCap == null || analysis.getMarketCap() <= maxMarketCap)
            .filter(analysis -> minPrice30dChange == null || analysis.getPriceChange30d() >= minPrice30dChange)
            .filter(analysis -> !Boolean.TRUE.equals(uptrendOnly) || Boolean.TRUE.equals(analysis.getIsUptrending()))
            .filter(analysis -> !Boolean.TRUE.equals(unusualVolumeOnly) || Boolean.TRUE.equals(analysis.getHasUnusualVolume()))
            .collect(Collectors.toList());

        return ResponseEntity.ok(analyses);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Stock>> getAllStocks() {
        return ResponseEntity.ok(stockService.getAllStocks());
    }





    @GetMapping("/historical/{symbol}")
    public ResponseEntity<?> getHistoricalData(@PathVariable String symbol) {
        try {
            List<HistoricalPrice> historicalData = stockService.getHistoricalData(symbol);
            if (historicalData.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No historical data found for symbol: " + symbol);
            }
            return ResponseEntity.ok(historicalData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching historical data: " + e.getMessage());
        }
    }

    @GetMapping("/historical/count")
    public ResponseEntity<?> getHistoricalDataCount() {
        try {
            long count = stockService.getHistoricalDataCount();
            Map<String, Object> response = new HashMap<>();
            response.put("total_records", count);
            response.put("message", "Total historical price records in database");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error getting count: " + e.getMessage());
        }
    }
} 