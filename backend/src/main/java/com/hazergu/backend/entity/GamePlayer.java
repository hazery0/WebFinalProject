package com.hazergu.backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "game_players")
@Data
@NoArgsConstructor
public class GamePlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 对应前端传来的唯一ID（非数据库主键）
    @Column(nullable = false)
    private String playerId;

    @Column(nullable = false)
    private String username;

    // 关键：避免 JSON 循环引用。Room 是主，Player 是从。
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    @JsonBackReference
    private GameRoom room;

    @Enumerated(EnumType.STRING)
    private PlayerStatus status = PlayerStatus.READY;

    // 关键：这是猜测次数的“唯一事实来源”，不要在 Service 内存里再存一份
    @Column(columnDefinition = "int default 0")
    private Integer guessCount = 0;
}