package com.hazergu.backend.controller;

import com.hazergu.backend.entity.HistoricalPerson;
import com.hazergu.backend.service.GameRoomService;
import com.hazergu.backend.service.HistoricalPersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class WebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private HistoricalPersonService historicalPersonService;

    @Autowired
    private GameRoomService gameRoomService;

    /**
     * 玩家创建房间
     */
    @MessageMapping("/game/createRoom")
    public void createRoom(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) {
        String playerId = (String) payload.get("playerId");
        String playerName = (String) payload.get("playerName");
        String roomId = gameRoomService.createRoom(playerId, playerName);

        Map<String, Object> response = new HashMap<>();
        response.put("type", "ROOM_CREATED");
        response.put("roomId", roomId);
        response.put("creator", playerId);
        response.put("creatorName", playerName);
        response.put("message", "房间创建成功，房间号: " + roomId);
        response.put("isRoomOwner", true); // 创建者自动是房主

        // 发送给创建者
        messagingTemplate.convertAndSendToUser(playerId, "/queue/game", response);

        // 发送房间状态给创建者
        sendRoomState(roomId, playerId);

        // 广播房间列表更新
        broadcastRoomList();
    }

    /**
     * 玩家加入房间
     */
    @MessageMapping("/game/joinRoom")
    public void joinRoom(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) {
        String playerId = (String) payload.get("playerId");
        String playerName = (String) payload.get("playerName");
        String roomId = (String) payload.get("roomId");

        boolean success = gameRoomService.joinRoom(roomId, playerId, playerName);

        Map<String, Object> response = new HashMap<>();
        if (success) {
            // 获取更新后的房间状态 - 使用全限定名
            GameRoomService.RoomState roomState = gameRoomService.getRoomState(roomId);

            // 确定玩家是否是房主
            boolean isRoomOwner = false;
            if (roomState != null) {
                for (GameRoomService.PlayerInfo player : roomState.getPlayers()) { // 使用全限定名
                    if (player.getId().equals(playerId)) {
                        isRoomOwner = player.isRoomOwner();
                        break;
                    }
                }
            }

            // 发送加入成功消息给玩家个人
            Map<String, Object> joinReceipt = new HashMap<>();
            joinReceipt.put("type", "JOIN_SUCCESS");
            joinReceipt.put("roomId", roomId);
            joinReceipt.put("isRoomOwner", isRoomOwner); // 添加房主状态
            messagingTemplate.convertAndSendToUser(playerId, "/queue/game", joinReceipt);

            // 发送房间状态给新玩家
            sendRoomState(roomId, playerId);

            // 广播玩家加入消息给房间
            response.put("type", "PLAYER_JOINED");
            response.put("roomId", roomId);
            response.put("playerId", playerId);
            response.put("playerName", playerName);
            response.put("isRoomOwner", isRoomOwner);
            response.put("message", playerName + " 加入了房间");
            messagingTemplate.convertAndSend("/topic/game/room/" + roomId, response);

            // 广播房间列表更新
            broadcastRoomList();
        } else {
            response.put("type", "JOIN_FAILED");
            response.put("message", "加入房间失败，房间可能已满或不存在");
            messagingTemplate.convertAndSendToUser(playerId, "/queue/game", response);
        }
    }

    /**
     * 玩家离开房间
     */
    @MessageMapping("/game/leaveRoom")
    public void leaveRoom(@Payload Map<String, Object> payload) {
        String playerId = (String) payload.get("playerId");
        String roomId = (String) payload.get("roomId");
        Boolean wasOwner = (Boolean) payload.get("wasOwner"); // 前端发送的是否是房主

        // 获取离开玩家的姓名 - 使用全限定名
        GameRoomService.RoomState roomStateBefore = gameRoomService.getRoomState(roomId);
        String leavingPlayerName = "未知玩家";
        if (roomStateBefore != null) {
            leavingPlayerName = roomStateBefore.getPlayers().stream()
                    .filter(p -> p.getId().equals(playerId))
                    .map(GameRoomService.PlayerInfo::getName) // 使用全限定名
                    .findFirst()
                    .orElse("未知玩家");
        }

        boolean isDestroyed = gameRoomService.leaveRoom(roomId, playerId);

        Map<String, Object> response = new HashMap<>();

        if (isDestroyed) {
            // 房间被解散
            response.put("type", "ROOM_DISSOLVED");
            response.put("message", "房间已解散");
            messagingTemplate.convertAndSend("/topic/game/room/" + roomId, response);
        } else {
            // 玩家离开
            response.put("type", "PLAYER_LEFT");
            response.put("playerId", playerId);
            response.put("playerName", leavingPlayerName);
            response.put("message", leavingPlayerName + " 离开了房间");

            // 如果房主离开且房间还有玩家，获取新房主信息 - 使用全限定名
            GameRoomService.RoomState roomStateAfter = gameRoomService.getRoomState(roomId);
            if (wasOwner != null && wasOwner && roomStateAfter != null && !roomStateAfter.getPlayers().isEmpty()) {
                // 查找新房主
                GameRoomService.PlayerInfo newOwner = roomStateAfter.getPlayers().stream()
                        .filter(GameRoomService.PlayerInfo::isRoomOwner) // 使用全限定名
                        .findFirst()
                        .orElse(null);

                if (newOwner != null) {
                    response.put("type", "OWNER_TRANSFERRED"); // 添加房主转移类型
                    response.put("newOwnerId", newOwner.getId());
                    response.put("newOwnerName", newOwner.getName());
                    response.put("message", leavingPlayerName + " 离开了房间，房主已转移给 " + newOwner.getName());
                }
            }

            messagingTemplate.convertAndSend("/topic/game/room/" + roomId, response);

            // 广播更新后的房间状态
            broadcastRoomState(roomId);
        }

        // 广播房间列表更新
        broadcastRoomList();
    }

    /**
     * 开始游戏
     */
    @MessageMapping("/game/start")
    public void startGame(@Payload Map<String, Object> payload) {
        String roomId = (String) payload.get("roomId");
        String starterId = (String) payload.get("playerId");

        // 检查是否是房主
        if (gameRoomService.isRoomOwner(roomId, starterId)) {
            // 随机获取一个历史人物作为谜底
            HistoricalPerson targetPerson = historicalPersonService.getRandomHistoricalPerson()
                    .orElseThrow(() -> new RuntimeException("无法获取随机历史人物"));

            // 设置游戏开始
            gameRoomService.startGame(roomId, targetPerson);

            Map<String, Object> response = new HashMap<>();
            response.put("type", "GAME_STARTED");
            response.put("message", "游戏开始！猜猜这是哪位历史人物？");
            response.put("starter", starterId);

            // 广播给房间内所有玩家
            messagingTemplate.convertAndSend("/topic/game/room/" + roomId, response);

            // 向房主发送目标人物的提示信息（可以控制信息量）
            Map<String, Object> hintForOwner = new HashMap<>();
            hintForOwner.put("type", "TARGET_PERSON_HINT");
            hintForOwner.put("hint", "目标人物: " + targetPerson.getName() +
                    " (生于" + targetPerson.getBirthYear() + "年)");
            messagingTemplate.convertAndSendToUser(starterId, "/queue/game", hintForOwner);
        }
    }

    /**
     * 玩家猜测
     */
    @MessageMapping("/game/guess")
    public void handleGuess(@Payload Map<String, Object> payload) {
        String roomId = (String) payload.get("roomId");
        String playerId = (String) payload.get("playerId");
        String guessName = (String) payload.get("guess");

        // 获取房间状态
        GameRoomService.RoomState roomState = gameRoomService.getRoomState(roomId);

        Map<String, Object> response = new HashMap<>();

        if (roomState.isGameActive()) {
            // 检查玩家是否还有猜测次数
            GameRoomService.PlayerInfo player = roomState.getPlayers().stream()
                    .filter(p -> p.getId().equals(playerId))
                    .findFirst()
                    .orElse(null);

            if (player != null && player.getGuessCount() >= roomState.getMaxGuessCount()) {
                // 猜测次数已用完
                response.put("type", "GUESS_INVALID");
                response.put("message", "你的猜测次数已用完！");
                messagingTemplate.convertAndSendToUser(playerId, "/queue/game", response);
                return;
            }

            HistoricalPerson targetPerson = roomState.getTargetPerson();

            // 搜索匹配的历史人物
            List<HistoricalPerson> matchedPersons = historicalPersonService.searchHistoricalPersons(guessName);

            if (!matchedPersons.isEmpty()) {
                HistoricalPerson guessedPerson = matchedPersons.get(0);

                // 增加猜测次数
                gameRoomService.incrementGuessCount(roomId, playerId);

                // 添加猜测历史到房间
                Map<String, Object> guessRecord = new HashMap<>();
                guessRecord.put("playerId", playerId);
                guessRecord.put("guess", guessedPerson);
                guessRecord.put("isCorrect", guessedPerson.getId().equals(targetPerson.getId()));
                guessRecord.put("timestamp", System.currentTimeMillis());
                gameRoomService.addGuess(roomId, guessRecord);

                // 获取更新后的猜测列表
                List<Map<String, Object>> allGuesses = gameRoomService.getGuesses(roomId);

                response.put("type", "GUESS_RESULT");
                response.put("playerId", playerId);
                response.put("guess", guessName);
                response.put("isCorrect", guessedPerson.getId().equals(targetPerson.getId()));
                response.put("guessCount", player != null ? player.getGuessCount() + 1 : 0);
                response.put("maxGuessCount", roomState.getMaxGuessCount());
                response.put("guesses", allGuesses); // 发送所有猜测记录
                response.put("guessedPerson", guessedPerson); // 发送猜测的人物信息

                if (guessedPerson.getId().equals(targetPerson.getId())) {
                    // 猜对了！
                    response.put("message", "恭喜！猜对了！目标人物是: " + targetPerson.getName());

                    // 结束游戏
                    gameRoomService.endGame(roomId, playerId);
                    response.put("gameEnded", true);
                    response.put("winner", playerId);
                    response.put("targetPerson", targetPerson);
                } else {
                    // 猜错了，给出线索
                    response.put("message", "猜错了！");
                    response.put("hint", generateHint(targetPerson, guessedPerson));

                    // 检查游戏是否结束（猜测次数用完）
                    boolean gameEnded = gameRoomService.checkGameEnd(roomId);
                    if (gameEnded) {
                        response.put("gameEnded", true);
                        response.put("winner", null);
                        response.put("targetPerson", targetPerson);
                        response.put("message", "所有玩家猜测次数已用完！");
                    }
                }

                // 广播猜结果给房间内所有玩家
                messagingTemplate.convertAndSend("/topic/game/room/" + roomId, response);
            } else {
                response.put("type", "GUESS_INVALID");
                response.put("message", "没有找到名为 '" + guessName + "' 的历史人物");

                // 只发送给猜的玩家
                messagingTemplate.convertAndSendToUser(playerId, "/queue/game", response);
            }
        }
    }

    /**
     * 玩家投降
     */
    @MessageMapping("/game/surrender")
    public void handleSurrender(@Payload Map<String, Object> payload) {
        String roomId = (String) payload.get("roomId");
        String playerId = (String) payload.get("playerId");

        // 处理投降
        gameRoomService.surrender(roomId, playerId);

        // 获取更新后的房间状态
        GameRoomService.RoomState roomState = gameRoomService.getRoomState(roomId);
        HistoricalPerson targetPerson = roomState.getTargetPerson();

        Map<String, Object> response = new HashMap<>();
        response.put("type", "SURRENDER_RESULT");
        response.put("playerId", playerId);
        response.put("message", "玩家选择了投降！");

        // 检查游戏是否结束
        if (!roomState.isGameActive()) {
            response.put("gameEnded", true);
            response.put("winner", roomState.getWinnerId());
            response.put("targetPerson", targetPerson);
            response.put("message", roomState.getWinnerId() != null ? "游戏结束，有人猜对了！" : "所有玩家都已投降！");
        }

        // 广播投降结果给房间内所有玩家
        messagingTemplate.convertAndSend("/topic/game/room/" + roomId, response);
    }

    /**
     * 获取房间列表
     */
    @MessageMapping("/game/getRooms")
    public void getRooms(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) {
        String playerId = (String) payload.get("playerId");
        List<GameRoomService.RoomInfo> rooms = gameRoomService.getAllRooms();

        Map<String, Object> response = new HashMap<>();
        response.put("type", "ROOM_LIST");
        response.put("rooms", rooms);

        messagingTemplate.convertAndSendToUser(playerId, "/queue/game", response);
    }

    /**
     * 发送聊天消息
     */
    @MessageMapping("/game/chat")
    public void handleChat(@Payload Map<String, Object> payload) {
        String roomId = (String) payload.get("roomId");
        String playerId = (String) payload.get("playerId");
        String message = (String) payload.get("message");

        // 获取玩家名称
        GameRoomService.RoomState roomState = gameRoomService.getRoomState(roomId);
        String playerName = "未知玩家";
        if (roomState != null) {
            playerName = roomState.getPlayers().stream()
                    .filter(p -> p.getId().equals(playerId))
                    .map(GameRoomService.PlayerInfo::getName)
                    .findFirst()
                    .orElse("未知玩家");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("type", "CHAT_MESSAGE");
        response.put("playerId", playerId);
        response.put("senderName", playerName); // 添加发送者名称
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());

        // 确保使用正确的路径
        messagingTemplate.convertAndSend("/topic/game/room/" + roomId + "/chat", response);
    }

    // ============ 辅助方法 ============

    /**
     * 生成提示信息
     */
    private String generateHint(HistoricalPerson target, HistoricalPerson guessed) {
        StringBuilder hint = new StringBuilder();

        // 比较出生年份
        if (target.getBirthYear() != null && guessed.getBirthYear() != null) {
            if (target.getBirthYear() > guessed.getBirthYear()) {
                hint.append("目标人物出生得更晚。 ");
            } else if (target.getBirthYear() < guessed.getBirthYear()) {
                hint.append("目标人物出生得更早。 ");
            } else {
                hint.append("出生年份相同！ ");
            }
        }

        // 比较类别
        if (target.getIsLiterary() == 1 && guessed.getIsLiterary() == 0) {
            hint.append("目标人物是文学家。 ");
        }
        if (target.getIsPolitical() == 1 && guessed.getIsPolitical() == 0) {
            hint.append("目标人物是政治家。 ");
        }
        if (target.getIsThinker() == 1 && guessed.getIsThinker() == 0) {
            hint.append("目标人物是思想家。 ");
        }
        if (target.getIsScientist() == 1 && guessed.getIsScientist() == 0) {
            hint.append("目标人物是科学家。 ");
        }

        return hint.toString();
    }

    /**
     * 发送房间状态给指定玩家
     */
    private void sendRoomState(String roomId, String playerId) {
        GameRoomService.RoomState roomState = gameRoomService.getRoomState(roomId); // 使用全限定名

        if (roomState != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "ROOM_STATE");

            // 添加额外的房主信息
            Map<String, Object> enhancedRoomState = new HashMap<>();
            enhancedRoomState.put("roomId", roomState.getRoomId());
            enhancedRoomState.put("creatorId", roomState.getCreatorId());
            enhancedRoomState.put("gameActive", roomState.isGameActive());
            enhancedRoomState.put("targetPerson", roomState.getTargetPerson());
            enhancedRoomState.put("winnerId", roomState.getWinnerId());
            enhancedRoomState.put("players", roomState.getPlayers());

            // 查找并添加房主姓名 - 使用全限定名
            String ownerName = roomState.getPlayers().stream()
                    .filter(GameRoomService.PlayerInfo::isRoomOwner) // 使用全限定名
                    .map(GameRoomService.PlayerInfo::getName) // 使用全限定名
                    .findFirst()
                    .orElse("未知");
            enhancedRoomState.put("ownerName", ownerName);

            response.put("roomState", enhancedRoomState);

            messagingTemplate.convertAndSendToUser(playerId, "/queue/game", response);
        }
    }

    /**
     * 广播房间状态给房间内所有玩家
     */
    private void broadcastRoomState(String roomId) {
        GameRoomService.RoomState roomState = gameRoomService.getRoomState(roomId); // 使用全限定名

        Map<String, Object> response = new HashMap<>();
        response.put("type", "ROOM_STATE_UPDATE");
        response.put("roomState", roomState);

        messagingTemplate.convertAndSend("/topic/game/room/" + roomId, response);
    }

    /**
     * 广播房间列表给所有在线玩家
     */
    private void broadcastRoomList() {
        List<GameRoomService.RoomInfo> rooms = gameRoomService.getAllRooms();

        Map<String, Object> response = new HashMap<>();
        response.put("type", "ROOM_LIST_UPDATE");
        response.put("rooms", rooms);

        messagingTemplate.convertAndSend("/topic/game/rooms", response);
    }
}