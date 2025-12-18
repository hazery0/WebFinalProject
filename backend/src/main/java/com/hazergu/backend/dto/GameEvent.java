package com.hazergu.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GameEvent {
    private String type;      // 事件类型
    private String playerId;  // 相关玩家ID
    private Object data;      // 事件数据
    private LocalDateTime timestamp;

    public GameEvent() {
        this.timestamp = LocalDateTime.now();
    }

    public GameEvent(String type, String playerId, Object data) {
        this.type = type;
        this.playerId = playerId;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }
}