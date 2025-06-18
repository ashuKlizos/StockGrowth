package com.StockGrowth.StockGrowth.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class HistoricalPrice {
    private String symbol;
    private LocalDate date;
    private Double price;
    private Long volume;
} 