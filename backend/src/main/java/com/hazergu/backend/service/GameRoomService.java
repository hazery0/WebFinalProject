package com.hazergu.backend.service;

import com.hazergu.backend.entity.GameRoom;
import com.hazergu.backend.entity.GameStatus;
import com.hazergu.backend.repository.GameRoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * 游戏房间服务
 */
@Service
public class GameRoomService {
    
    @Autowired
    private GameRoomRepository gameRoomRepository;
    
    /**
     * 创建新房间
     * @return 创建的房间信息
     */
    public GameRoom createRoom() {
        // 生成4位随机房间号
        String roomId = generateUniqueRoomId();
        
        GameRoom room = new GameRoom();
        room.setRoomId(roomId);
        room.setCreateTime(LocalDateTime.now());
        room.setStatus(GameStatus.WAITING);
        room.setMaxPlayers(2); // 默认2人房间
        
        return gameRoomRepository.save(room);
    }
    
    /**
     * 生成唯一的4位房间号
     * @return 唯一房间号
     */
    private String generateUniqueRoomId() {
        Random random = new Random();
        String roomId;
        do {
            // 生成1000-9999之间的随机数
            roomId = String.format("%04d", random.nextInt(9000) + 1000);
        } while (gameRoomRepository.existsByRoomId(roomId));
        
        return roomId;
    }
    
    /**
     * 加入房间
     * @param roomId 房间ID
     * @return 房间信息
     */
    public Optional<GameRoom> joinRoom(String roomId) {
        return gameRoomRepository.findById(roomId);
    }
    
    /**
     * 离开房间
     * @param roomId 房间ID
     */
    public void leaveRoom(String roomId) {
        Optional<GameRoom> roomOptional = gameRoomRepository.findById(roomId);
        if (roomOptional.isPresent()) {
            GameRoom room = roomOptional.get();
            // 这里可以添加离开房间的逻辑，比如检查房间是否为空，为空则删除房间
            gameRoomRepository.save(room);
        }
    }
    
    /**
     * 获取房间信息
     * @param roomId 房间ID
     * @return 房间信息
     */
    public Optional<GameRoom> getRoomInfo(String roomId) {
        return gameRoomRepository.findById(roomId);
    }
    
    /**
     * 获取所有等待中的房间
     * @return 等待中的房间列表
     */
    public List<GameRoom> getWaitingRooms() {
        return gameRoomRepository.findByStatus(GameStatus.WAITING);
    }
    
    /**
     * 更新房间状态
     * @param roomId 房间ID
     * @param status 新状态
     * @return 更新后的房间信息
     */
    public Optional<GameRoom> updateRoomStatus(String roomId, GameStatus status) {
        Optional<GameRoom> roomOptional = gameRoomRepository.findById(roomId);
        if (roomOptional.isPresent()) {
            GameRoom room = roomOptional.get();
            room.setStatus(status);
            return Optional.of(gameRoomRepository.save(room));
        }
        return Optional.empty();
    }
}
