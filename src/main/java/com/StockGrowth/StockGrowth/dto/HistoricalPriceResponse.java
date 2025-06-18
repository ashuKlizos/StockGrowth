package com.StockGrowth.StockGrowth.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class HistoricalPriceResponse {
    private String symbol;
    private List<HistoricalPriceDTO> historical = new ArrayList<>(); // Initialize to empty list to avoid NPE
} 