package com.hazergu.backend.dto;

import lombok.Data;

@Data
public class PlayerAction {
    private String playerId;
    private String playerName;
    private Integer guessPersonId;
    private String actionType; // JOIN, LEAVE, GUESS, SURRENDER, etc.
    private Integer remainingGuesses; // 新增：剩余猜测次数
    private Boolean isHost; // 新增：是否是房主
}