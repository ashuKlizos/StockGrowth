package com.StockGrowth.StockGrowth.repository;

import com.StockGrowth.StockGrowth.model.HistoricalPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HistoricalPriceRepository extends JpaRepository<HistoricalPrice, Long> {
    
    List<HistoricalPrice> findBySymbolAndDateBetweenOrderByDateDesc(
        String symbol, LocalDate startDate, LocalDate endDate);
    
    @Query("SELECT h.symbol FROM HistoricalPrice h WHERE h.lastUpdated < :date GROUP BY h.symbol")
    List<String> findSymbolsToUpdate(LocalDate date);

    void deleteBySymbol(String symbol);

    void deleteBySymbolAndDateBefore(String symbol, LocalDate date);

    List<HistoricalPrice> findBySymbolOrderByDateDesc(String symbol);
    
    List<HistoricalPrice> findBySymbolAndDate(String symbol, LocalDate date);

    @Modifying
    @Query("DELETE FROM HistoricalPrice h WHERE h.symbol IN :symbols")
    void deleteBySymbolIn(List<String> symbols);
} 