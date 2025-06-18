package com.StockGrowth.StockGrowth.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "stocks")
public class Stock {
    @Id
    private String symbol;
    private String companyName;
    private Double marketCap;
    private String sector;
    private String industry;
    private Double beta;
    private Double price;
    private Double lastAnnualDividend;
    private Long volume;
    private String exchange;
    private String country;
    private Boolean isEtf;
    private Boolean isFund;
    private Boolean isActivelyTrading;
} 