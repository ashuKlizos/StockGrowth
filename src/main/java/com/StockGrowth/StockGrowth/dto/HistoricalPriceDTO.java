package com.StockGrowth.StockGrowth.dto;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

@Data
public class HistoricalPriceDTO {
    private String date; // API returns date as string
    private Double open;
    private Double high;
    private Double low;
    private Double close;
    @JsonProperty("adjClose")
    private Double adjClose;
    @JsonProperty("volume")
    private Long volume;
    private Double change;
    @JsonProperty("changePercent")
    private Double changePercent;
    private String label;
    @JsonProperty("changeOverTime")
    private Double changeOverTime;
    
    // Custom getter to convert string date to LocalDate
    public LocalDate getDate() {
        if (date == null) return null;
        return LocalDate.parse(date);
    }
} 