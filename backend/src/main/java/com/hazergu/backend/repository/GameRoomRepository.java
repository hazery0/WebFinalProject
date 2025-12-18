package com.hazergu.backend.repository;

import com.hazergu.backend.entity.GameRoom;
import com.hazergu.backend.entity.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 游戏房间数据访问接口
 */
@Repository
public interface GameRoomRepository extends JpaRepository<GameRoom, String> {
    
    /**
     * 根据状态查询房间列表
     * @param status 游戏状态
     * @return 符合条件的房间列表
     */
    List<GameRoom> findByStatus(GameStatus status);
    
    /**
     * 检查房间是否存在
     * @param roomId 房间ID
     * @return 是否存在
     */
    boolean existsByRoomId(String roomId);
}
