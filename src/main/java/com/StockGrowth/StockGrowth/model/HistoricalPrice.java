package com.StockGrowth.StockGrowth.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "historical_prices", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"symbol", "date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class HistoricalPrice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Long volume;

    @Column(nullable = false)
    private LocalDateTime lastUpdated;
} 