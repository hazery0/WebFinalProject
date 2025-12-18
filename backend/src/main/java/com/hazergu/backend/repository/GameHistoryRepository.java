package com.hazergu.backend.repository;

import com.hazergu.backend.entity.GameHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 游戏历史数据访问接口
 */
@Repository
public interface GameHistoryRepository extends JpaRepository<GameHistory, Long> {
    
    // 可以根据需要添加更多查询方法
}
