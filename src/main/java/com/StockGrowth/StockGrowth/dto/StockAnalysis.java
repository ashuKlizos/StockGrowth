package com.StockGrowth.StockGrowth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class StockAnalysis {
    private String ticker;
    private String companyName;
    private Double marketCap;
    private Long volume;
    private Double priceChange1d;
    private Double priceChange5d;
    private Double priceChange30d;
    private Boolean isUptrending;
    private Boolean hasUnusualVolume;
    private Double averageVolume;
} 