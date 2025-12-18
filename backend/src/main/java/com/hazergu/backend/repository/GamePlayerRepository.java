package com.hazergu.backend.repository;

import com.hazergu.backend.entity.GamePlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 游戏玩家数据访问接口
 */
@Repository
public interface GamePlayerRepository extends JpaRepository<GamePlayer, Long> {
    
    /**
     * 根据房间ID查询玩家列表
     * @param roomId 房间ID
     * @return 房间内的玩家列表
     */
    List<GamePlayer> findByRoom_RoomId(String roomId);
    
    /**
     * 根据房间ID和玩家ID查询玩家
     * @param roomId 房间ID
     * @param playerId 玩家ID
     * @return 匹配的玩家
     */
    Optional<GamePlayer> findByRoom_RoomIdAndPlayerId(String roomId, String playerId);
    
    /**
     * 根据玩家ID查询玩家
     * @param playerId 玩家ID
     * @return 匹配的玩家
     */
    Optional<GamePlayer> findByPlayerId(String playerId);
}
