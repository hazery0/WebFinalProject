package com.hazergu.backend.controller;

import com.hazergu.backend.entity.GameRoom;
import com.hazergu.backend.entity.GameStatus;
import com.hazergu.backend.service.GamePlayerService;
import com.hazergu.backend.service.GameRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 游戏房间控制器
 */
@RestController
@RequestMapping("/api/game/rooms")
public class GameRoomController {
    
    @Autowired
    private GameRoomService gameRoomService;
    
    @Autowired
    private GamePlayerService gamePlayerService;
    
    /**
     * 创建新房间
     * @return 创建的房间信息
     */
    @PostMapping
    public ResponseEntity<GameRoom> createRoom() {
        GameRoom room = gameRoomService.createRoom();
        return ResponseEntity.ok(room);
    }
    
    /**
     * 获取房间列表
     * @return 房间列表
     */
    @GetMapping
    public ResponseEntity<List<GameRoom>> getRoomList() {
        List<GameRoom> rooms = gameRoomService.getWaitingRooms();
        return ResponseEntity.ok(rooms);
    }
    
    /**
     * 获取房间详情
     * @param roomId 房间ID
     * @return 房间详情
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<?> getRoomInfo(@PathVariable String roomId) {
        Optional<GameRoom> roomOptional = gameRoomService.getRoomInfo(roomId);
        if (roomOptional.isPresent()) {
            return ResponseEntity.ok(Map.of(
                    "room", roomOptional.get(),
                    "players", gamePlayerService.getPlayersInRoom(roomId)
            ));
        }
        return ResponseEntity.status(404).body(Map.of("error", "房间不存在"));
    }
    
    /**
     * 加入房间
     * @param roomId 房间ID
     * @param request 加入请求
     * @return 加入结果
     */
    @PostMapping("/{roomId}/join")
    public ResponseEntity<?> joinRoom(@PathVariable String roomId, @RequestBody Map<String, Object> request) {
        String playerId = (String) request.get("playerId");
        String username = (String) request.get("username");
        
        if (playerId == null || username == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "缺少必要参数"));
        }
        
        return gamePlayerService.addPlayer(roomId, playerId, username)
                .map(player -> ResponseEntity.ok(Map.of(
                        "room", gameRoomService.getRoomInfo(roomId).orElse(null),
                        "players", gamePlayerService.getPlayersInRoom(roomId),
                        "player", player
                )))
                .orElse(ResponseEntity.status(404).body(Map.of("error", "加入房间失败")));
    }
    
    /**
     * 离开房间
     * @param roomId 房间ID
     * @param request 离开请求
     * @return 离开结果
     */
    @PostMapping("/{roomId}/leave")
    public ResponseEntity<?> leaveRoom(@PathVariable String roomId, @RequestBody Map<String, Object> request) {
        String playerId = (String) request.get("playerId");
        
        if (playerId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "缺少必要参数"));
        }
        
        gamePlayerService.removePlayer(roomId, playerId);
        
        return ResponseEntity.ok(Map.of(
                "room", gameRoomService.getRoomInfo(roomId).orElse(null),
                "players", gamePlayerService.getPlayersInRoom(roomId),
                "message", "成功离开房间"
        ));
    }
}
