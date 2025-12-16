package com.hazergu.backend.repository;

import com.hazergu.backend.entity.HistoricalPerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface HistoricalPersonRepository extends JpaRepository<HistoricalPerson, Long> {
    
    // 模糊搜索历史人物
    List<HistoricalPerson> findByNameContaining(String name);
    
    // 随机获取一个历史人物
    @Query(value = "SELECT * FROM historical_persons ORDER BY RAND() LIMIT 1", nativeQuery = true)
    Optional<HistoricalPerson> findRandomPerson();
}