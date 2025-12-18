package com.hazergu.backend.controller;

import com.hazergu.backend.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * 游戏控制器
 */
@RestController
@RequestMapping("/api/game")
public class GameController {
    
    @Autowired
    private GameService gameService;
    
    /**
     * 开始游戏
     * @param roomId 房间ID
     * @return 游戏开始结果
     */
    @PostMapping("/rooms/{roomId}/start")
    public ResponseEntity<?> startGame(@PathVariable String roomId) {
        Optional<?> roomOptional = gameService.startGame(roomId);
        if (roomOptional.isPresent()) {
            return ResponseEntity.ok(Map.of(
                    "message", "游戏开始",
                    "room", roomOptional.get()
            ));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "无法开始游戏"));
    }
    
    /**
     * 结束游戏
     * @param roomId 房间ID
     * @param request 结束游戏请求
     * @return 游戏结束结果
     */
    @PostMapping("/rooms/{roomId}/end")
    public ResponseEntity<?> endGame(@PathVariable String roomId, @RequestBody Map<String, Object> request) {
        String winnerId = (String) request.get("winnerId");
        
        if (winnerId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "缺少获胜者ID"));
        }
        
        Optional<?> roomOptional = gameService.endGame(roomId, winnerId);
        if (roomOptional.isPresent()) {
            return ResponseEntity.ok(Map.of(
                    "message", "游戏结束",
                    "room", roomOptional.get()
            ));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "无法结束游戏"));
    }
    
    /**
     * 玩家猜测历史人物
     * @param roomId 房间ID
     * @param request 猜测请求
     * @return 猜测结果
     */
    @PostMapping("/rooms/{roomId}/guess")
    public ResponseEntity<?> guessPerson(@PathVariable String roomId, @RequestBody Map<String, Object> request) {
        String playerId = (String) request.get("playerId");
        Long personId = Long.parseLong(request.get("personId").toString());
        
        if (playerId == null || personId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "缺少必要参数"));
        }
        
        GameService.GuessResult result = gameService.guessPerson(roomId, playerId, personId);
        if (result != null) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.badRequest().body(Map.of("error", "猜测失败"));
    }
    
    /**
     * 获取游戏状态
     * @param roomId 房间ID
     * @return 游戏状态
     */
    @GetMapping("/rooms/{roomId}/status")
    public ResponseEntity<?> getGameStatus(@PathVariable String roomId) {
        GameService.GameStatusInfo statusInfo = gameService.getGameStatus(roomId);
        if (statusInfo != null) {
            return ResponseEntity.ok(statusInfo);
        }
        return ResponseEntity.status(404).body(Map.of("error", "获取游戏状态失败"));
    }
}
