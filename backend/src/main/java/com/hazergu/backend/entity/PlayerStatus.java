package com.hazergu.backend.entity;

/**
 * 玩家状态枚举
 */
public enum PlayerStatus {
    READY,   // 准备就绪
    PLAYING, // 游戏中
    WINNER,  // 胜利者
    SURRENDERED, LOSER    // 失败者
}
