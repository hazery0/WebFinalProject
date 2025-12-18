package com.hazergu.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 游戏历史实体
 */
@Entity
@Table(name = "game_histories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String roomId;
    
    @Column(nullable = false)
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_person_id")
    private HistoricalPerson targetPerson;
    
    @Column(nullable = false)
    private String winnerId;
    
    private Integer totalGuessCount;
}
