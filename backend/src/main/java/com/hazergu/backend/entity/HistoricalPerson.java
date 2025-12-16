package com.hazergu.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "historical_persons")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalPerson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    private Integer birthYear;
    
    @Column(columnDefinition = "int default 0")
    private Integer isLiterary = 0;
    
    @Column(columnDefinition = "int default 0")
    private Integer isPolitical = 0;
    
    @Column(columnDefinition = "int default 0")
    private Integer isThinker = 0;
    
    @Column(columnDefinition = "int default 0")
    private Integer isScientist = 0;
}