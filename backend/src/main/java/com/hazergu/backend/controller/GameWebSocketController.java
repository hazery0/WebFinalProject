package com.hazergu.backend.controller;

import com.hazergu.backend.entity.GamePlayer;
import com.hazergu.backend.entity.GameRoom;
import com.hazergu.backend.entity.WebSocketMessage;
import com.hazergu.backend.service.GamePlayerService;
import com.hazergu.backend.service.GameRoomService;
import com.hazergu.backend.service.GameService;
import com.hazergu.backend.service.RealTimeGameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.Optional;

/**
 * 游戏WebSocket控制器
 */
@Controller
public class GameWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private GameRoomService gameRoomService;

    @Autowired
    private GamePlayerService gamePlayerService;

    @Autowired
    private GameService gameService;

    @Autowired
    private RealTimeGameService realTimeGameService;

    /**
     * 玩家加入房间
     * @param roomId 房间ID
     * @param message 加入房间消息
     * @return 房间信息
     */
    @MessageMapping("/game/{roomId}/join")
    @SendTo("/topic/game/{roomId}")
    public WebSocketMessage joinRoom(@DestinationVariable String roomId, Map<String, Object> message) {
        String playerId = (String) message.get("playerId");
        String username = (String) message.get("username");

        // 使用RealTimeGameService处理玩家加入
        realTimeGameService.playerJoin(roomId, playerId, username);

        // 获取房间信息
        Optional<GameRoom> roomOptional = gameRoomService.getRoomInfo(roomId);
        if (roomOptional.isPresent()) {
            GameRoom room = roomOptional.get();

            // 获取玩家信息
            Optional<GamePlayer> playerOptional = gamePlayerService.getPlayer(roomId, playerId);

            // 构建响应消息
            Map<String, Object> response = Map.of(
                    "room", room,
                    "players", gamePlayerService.getPlayersInRoom(roomId),
                    "playerId", playerId,
                    "username", username,
                    "maxGuesses", GameService.getMaxGuessesPerPlayer()
            );

            if (playerOptional.isPresent()) {
                response.put("player", playerOptional.get());
            }

            return new WebSocketMessage("player-joined", response);
        }

        return new WebSocketMessage("error", "加入房间失败");
    }

    /**
     * 玩家离开房间
     * @param roomId 房间ID
     * @param message 离开房间消息
     * @return 房间信息
     */
    @MessageMapping("/game/{roomId}/leave")
    @SendTo("/topic/game/{roomId}")
    public WebSocketMessage leaveRoom(@DestinationVariable String roomId, Map<String, Object> message) {
        String playerId = (String) message.get("playerId");

        // 使用RealTimeGameService处理玩家离开
        realTimeGameService.playerLeave(roomId, playerId);

        // 发送房间更新消息
        Optional<GameRoom> roomOptional = gameRoomService.getRoomInfo(roomId);
        if (roomOptional.isPresent()) {
            return new WebSocketMessage("player-left", Map.of(
                    "room", roomOptional.get(),
                    "players", gamePlayerService.getPlayersInRoom(roomId),
                    "playerId", playerId
            ));
        }

        return new WebSocketMessage("error", "离开房间失败");
    }

    /**
     * 开始游戏
     * @param roomId 房间ID
     * @param message 开始游戏消息（包含房主ID）
     * @return 游戏状态
     */
    @MessageMapping("/game/{roomId}/start")
    @SendTo("/topic/game/{roomId}")
    public WebSocketMessage startGame(@DestinationVariable String roomId, Map<String, Object> message) {
        String hostId = (String) message.get("playerId");

        // 使用RealTimeGameService开始游戏
        boolean started = realTimeGameService.startGame(roomId, hostId);
        if (started) {
            // 获取游戏统计信息
            Map<String, Object> gameStats = gameService.getGameStats(roomId);

            // 获取房间信息
            Optional<GameRoom> roomOptional = gameRoomService.getRoomInfo(roomId);
            if (roomOptional.isPresent()) {
                GameRoom room = roomOptional.get();

                return new WebSocketMessage("game-started", Map.of(
                        "room", room,
                        "players", gamePlayerService.getPlayersInRoom(roomId),
                        "gameStats", gameStats,
                        "maxGuesses", GameService.getMaxGuessesPerPlayer(),
                        "message", "游戏开始！每个玩家有" + GameService.getMaxGuessesPerPlayer() + "次猜测机会。"
                ));
            }
        }

        return new WebSocketMessage("error", Map.of(
                "message", "开始游戏失败",
                "reason", "需要至少2名玩家才能开始游戏"
        ));
    }

    /**
     * 玩家猜测历史人物
     * @param roomId 房间ID
     * @param message 猜测消息
     * @return 猜测结果
     */
    @MessageMapping("/game/{roomId}/guess")
    @SendTo("/topic/game/{roomId}")
    public WebSocketMessage guessPerson(@DestinationVariable String roomId, Map<String, Object> message) {
        String playerId = (String) message.get("playerId");
        Long personId = Long.parseLong(message.get("personId").toString());

        // 先检查玩家是否还有猜测次数
        if (!gameService.canGuess(roomId, playerId)) {
            return new WebSocketMessage("error", Map.of(
                    "message", "猜测失败",
                    "reason", "您已用完所有猜测次数"
            ));
        }

        // 使用RealTimeGameService处理猜测
        GameService.GuessResult result = realTimeGameService.handleGuess(roomId, playerId, personId);

        if (result != null) {
            int remainingGuesses = result.getRemainingGuesses();

            Map<String, Object> response = Map.of(
                    "result", result,
                    "playerId", playerId,
                    "guessedPersonId", personId,
                    "remainingGuesses", remainingGuesses,
                    "isCorrect", result.isCorrect()
            );

            if (result.isCorrect()) {
                // 猜对了，游戏结束
                response = Map.of(
                        "result", result,
                        "playerId", playerId,
                        "gameEnded", true,
                        "winnerId", playerId,
                        "remainingGuesses", remainingGuesses,
                        "message", "恭喜猜对了！游戏结束。"
                );
            } else {
                // 猜错了
                String messageText;
                if (remainingGuesses <= 0) {
                    messageText = "猜错了！您已用完所有猜测次数。";
                } else {
                    messageText = "猜错了！您还有" + remainingGuesses + "次猜测机会。";
                }

                response.put("message", messageText);

                // 检查是否所有玩家都用完了猜测次数
                if (gameService.allGuessesExhausted(roomId)) {
                    response.put("allGuessesExhausted", true);
                    response.put("gameEnded", true);
                    response.put("message", "所有玩家都用完了猜测次数，游戏结束！");
                }
            }

            return new WebSocketMessage("guess-result", response);
        }

        return new WebSocketMessage("error", Map.of(
                "message", "猜测失败",
                "reason", "游戏未开始或房间不存在"
        ));
    }

    /**
     * 获取游戏状态
     * @param roomId 房间ID
     * @return 游戏状态
     */
    @MessageMapping("/game/{roomId}/status")
    @SendTo("/topic/game/{roomId}")
    public WebSocketMessage getGameStatus(@DestinationVariable String roomId) {
        GameService.GameStatusInfo statusInfo = gameService.getGameStatus(roomId);
        if (statusInfo != null) {
            // 获取游戏统计信息
            Map<String, Object> gameStats = gameService.getGameStats(roomId);

            return new WebSocketMessage("game-status", Map.of(
                    "status", statusInfo,
                    "players", gamePlayerService.getPlayersInRoom(roomId),
                    "gameStats", gameStats,
                    "maxGuesses", GameService.getMaxGuessesPerPlayer()
            ));
        }

        return new WebSocketMessage("error", "获取游戏状态失败");
    }

    /**
     * 获取玩家剩余猜测次数
     * @param roomId 房间ID
     * @param message 请求消息
     * @return 剩余猜测次数
     */
    @MessageMapping("/game/{roomId}/remaining-guesses")
    @SendTo("/topic/game/{roomId}")
    public WebSocketMessage getRemainingGuesses(@DestinationVariable String roomId, Map<String, Object> message) {
        String playerId = (String) message.get("playerId");

        int remainingGuesses = gameService.getRemainingGuesses(roomId, playerId);

        return new WebSocketMessage("remaining-guesses", Map.of(
                "playerId", playerId,
                "remainingGuesses", remainingGuesses,
                "maxGuesses", GameService.getMaxGuessesPerPlayer()
        ));
    }

    /**
     * 获取游戏统计信息
     * @param roomId 房间ID
     * @return 游戏统计
     */
    @MessageMapping("/game/{roomId}/stats")
    @SendTo("/topic/game/{roomId}")
    public WebSocketMessage getGameStats(@DestinationVariable String roomId) {
        Map<String, Object> gameStats = gameService.getGameStats(roomId);

        return new WebSocketMessage("game-stats", Map.of(
                "stats", gameStats,
                "maxGuesses", GameService.getMaxGuessesPerPlayer()
        ));
    }

    /**
     * 玩家投降
     * @param roomId 房间ID
     * @param message 投降消息
     * @return 投降结果
     */
    @MessageMapping("/game/{roomId}/surrender")
    @SendTo("/topic/game/{roomId}")
    public WebSocketMessage surrender(@DestinationVariable String roomId, Map<String, Object> message) {
        String playerId = (String) message.get("playerId");

        // 使用RealTimeGameService处理投降
        boolean surrendered = realTimeGameService.surrender(roomId, playerId);
        if (surrendered) {
            return new WebSocketMessage("surrender", Map.of(
                    "playerId", playerId,
                    "gameEnded", true,
                    "message", "玩家已投降，游戏结束。"
            ));
        }

        return new WebSocketMessage("error", Map.of(
                "message", "投降失败",
                "reason", "游戏未开始或房间不存在"
        ));
    }
}