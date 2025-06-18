package com.StockGrowth.StockGrowth.repository;

import com.StockGrowth.StockGrowth.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockRepository extends JpaRepository<Stock, String> {
    // Additional query methods can be added here if needed
} 