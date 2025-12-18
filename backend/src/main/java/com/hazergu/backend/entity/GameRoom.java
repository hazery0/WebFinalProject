package com.hazergu.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.hazergu.backend.entity.GameStatus; // 假设枚举在这个路径
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 游戏房间实体
 */
@Entity
@Table(name = "game_rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameRoom {

    @Id
    @Column(length = 4, nullable = false, unique = true)
    private String roomId; // 4位房间号

    @Column(nullable = false)
    private LocalDateTime createTime = LocalDateTime.now(); // 创建时间，默认当前时间

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus status = GameStatus.WAITING; // 游戏状态: WAITING, PLAYING, ENDED

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<GamePlayer> players; // 房间内的玩家列表

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "target_person_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private HistoricalPerson targetPerson; // 当前游戏目标人物

    @Column(nullable = false)
    private Integer maxPlayers = 2; // 最大玩家数

    // 如果需要自定义构造函数，可以添加
    public GameRoom(String roomId) {
        this.roomId = roomId;
        this.createTime = LocalDateTime.now();
        this.status = GameStatus.WAITING;
        this.maxPlayers = 2;
    }

    // 获取当前玩家人数
    @Transient
    public Integer getCurrentPlayerCount() {
        return players != null ? players.size() : 0;
    }

    // 检查房间是否已满
    @Transient
    public Boolean isFull() {
        return getCurrentPlayerCount() >= maxPlayers;
    }

    // 检查房间是否可以开始游戏
    @Transient
    public Boolean canStart() {
        return GameStatus.WAITING.equals(status) && isFull();
    }
}