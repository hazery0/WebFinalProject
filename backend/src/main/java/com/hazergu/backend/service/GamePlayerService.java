package com.hazergu.backend.service;

import com.hazergu.backend.entity.GamePlayer;
import com.hazergu.backend.entity.GameRoom;
import com.hazergu.backend.entity.PlayerStatus;
import com.hazergu.backend.repository.GamePlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 游戏玩家服务
 */
@Service
public class GamePlayerService {
    
    @Autowired
    private GamePlayerRepository gamePlayerRepository;
    
    @Autowired
    private GameRoomService gameRoomService;
    
    /**
     * 添加玩家到房间
     * @param roomId 房间ID
     * @param playerId 玩家ID
     * @param username 玩家用户名
     * @return 添加的玩家信息
     */
    public Optional<GamePlayer> addPlayer(String roomId, String playerId, String username) {
        // 检查房间是否存在
        Optional<GameRoom> roomOptional = gameRoomService.getRoomInfo(roomId);
        if (roomOptional.isPresent()) {
            GameRoom room = roomOptional.get();
            
            // 检查玩家是否已经在房间中
            Optional<GamePlayer> existingPlayer = gamePlayerRepository.findByRoom_RoomIdAndPlayerId(roomId, playerId);
            if (existingPlayer.isPresent()) {
                return existingPlayer;
            }
            
            // 创建新玩家
            GamePlayer player = new GamePlayer();
            player.setPlayerId(playerId);
            player.setUsername(username);
            player.setRoom(room);
            player.setStatus(PlayerStatus.READY);
            
            return Optional.of(gamePlayerRepository.save(player));
        }
        return Optional.empty();
    }
    
    /**
     * 更新玩家状态
     * @param roomId 房间ID
     * @param playerId 玩家ID
     * @param status 新状态
     * @return 更新后的玩家信息
     */
    public Optional<GamePlayer> updatePlayerStatus(String roomId, String playerId, PlayerStatus status) {
        Optional<GamePlayer> playerOptional = gamePlayerRepository.findByRoom_RoomIdAndPlayerId(roomId, playerId);
        if (playerOptional.isPresent()) {
            GamePlayer player = playerOptional.get();
            player.setStatus(status);
            return Optional.of(gamePlayerRepository.save(player));
        }
        return Optional.empty();
    }
    
    /**
     * 获取房间内所有玩家
     * @param roomId 房间ID
     * @return 玩家列表
     */
    public List<GamePlayer> getPlayersInRoom(String roomId) {
        return gamePlayerRepository.findByRoom_RoomId(roomId);
    }
    
    /**
     * 获取玩家信息
     * @param roomId 房间ID
     * @param playerId 玩家ID
     * @return 玩家信息
     */
    public Optional<GamePlayer> getPlayer(String roomId, String playerId) {
        return gamePlayerRepository.findByRoom_RoomIdAndPlayerId(roomId, playerId);
    }
    
    /**
     * 从房间中移除玩家
     * @param roomId 房间ID
     * @param playerId 玩家ID
     */
    public void removePlayer(String roomId, String playerId) {
        Optional<GamePlayer> playerOptional = gamePlayerRepository.findByRoom_RoomIdAndPlayerId(roomId, playerId);
        playerOptional.ifPresent(gamePlayerRepository::delete);
    }
    
    /**
     * 增加玩家猜测次数
     * @param roomId 房间ID
     * @param playerId 玩家ID
     * @return 更新后的玩家信息
     */
    public Optional<GamePlayer> incrementGuessCount(String roomId, String playerId) {
        Optional<GamePlayer> playerOptional = gamePlayerRepository.findByRoom_RoomIdAndPlayerId(roomId, playerId);
        if (playerOptional.isPresent()) {
            GamePlayer player = playerOptional.get();
            player.setGuessCount(player.getGuessCount() + 1);
            return Optional.of(gamePlayerRepository.save(player));
        }
        return Optional.empty();
    }
}
