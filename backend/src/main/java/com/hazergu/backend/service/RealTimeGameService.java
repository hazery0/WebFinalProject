package com.hazergu.backend.service;

import com.hazergu.backend.dto.GameEvent;
import com.hazergu.backend.entity.*;
import com.hazergu.backend.repository.GamePlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实时游戏服务 - 负责WebSocket连接和实时事件广播（重构版）
 */
@Service
public class RealTimeGameService {

    @Autowired
    private GameRoomService gameRoomService;

    @Autowired
    private GamePlayerService gamePlayerService;

    @Autowired
    private GameService gameService;

    @Autowired
    private GamePlayerRepository gamePlayerRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // 每个玩家的最大猜测次数（从GameService获取，保持一致性）
    private static final int MAX_GUESSES_PER_PLAYER = GameService.getMaxGuessesPerPlayer();

    // 内存中只存储在线玩家状态，不存储游戏逻辑状态
    private static class OnlineRoomState {
        Set<String> onlinePlayers;  // 当前在线玩家
        boolean gameActive;         // 游戏是否正在进行中

        OnlineRoomState() {
            this.onlinePlayers = new HashSet<>();
            this.gameActive = false;
        }
    }

    private final Map<String, OnlineRoomState> onlineRoomStates = new ConcurrentHashMap<>();

    /**
     * 玩家加入房间（WebSocket连接时调用）
     */
    public void playerJoin(String roomId, String playerId, String playerName) {
        // 获取或创建房间在线状态
        OnlineRoomState state = onlineRoomStates.computeIfAbsent(roomId, k -> new OnlineRoomState());
        state.onlinePlayers.add(playerId);

        // 先调用原有的服务添加玩家
        gamePlayerService.addPlayer(roomId, playerId, playerName);

        // 广播玩家加入事件
        GameEvent event = new GameEvent("PLAYER_JOINED", playerId, playerName);
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/events", event);

        // 更新玩家列表
        broadcastPlayerList(roomId);
    }

    /**
     * 玩家离开房间
     */
    public void playerLeave(String roomId, String playerId) {
        // 从在线状态中移除
        OnlineRoomState state = onlineRoomStates.get(roomId);
        if (state != null) {
            state.onlinePlayers.remove(playerId);

            // 如果房间空了，清理状态
            if (state.onlinePlayers.isEmpty()) {
                onlineRoomStates.remove(roomId);
            }
        }

        // 调用原有服务移除玩家
        gamePlayerService.removePlayer(roomId, playerId);

        // 广播玩家离开事件
        GameEvent event = new GameEvent("PLAYER_LEFT", playerId, null);
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/events", event);

        // 更新玩家列表
        broadcastPlayerList(roomId);
    }

    /**
     * 开始游戏
     */
    public boolean startGame(String roomId, String hostId) {
        // 检查是否是房主
        Optional<GameRoom> roomOpt = gameRoomService.getRoomInfo(roomId);
        if (roomOpt.isEmpty()) return false;

        GameRoom room = roomOpt.get();
        List<GamePlayer> players = gamePlayerService.getPlayersInRoom(roomId);

        // 需要至少1名玩家（可以是单人模式）
        if (players.isEmpty()) return false;

        // 更新在线状态
        OnlineRoomState state = onlineRoomStates.computeIfAbsent(roomId, k -> new OnlineRoomState());
        state.gameActive = true;

        // 调用GameService的开始游戏逻辑
        Optional<GameRoom> updatedRoom = gameService.startGame(roomId);
        if (updatedRoom.isPresent()) {
            // 广播游戏开始事件
            Map<String, Object> stats = getGameStats(roomId);
            GameEvent startEvent = new GameEvent("GAME_START", null, stats);
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/events", startEvent);

            // 广播游戏统计信息
            broadcastGameStats(roomId);

            // 获取并广播目标人物ID（不显示具体信息）
            HistoricalPerson targetPerson = updatedRoom.get().getTargetPerson();
            if (targetPerson != null) {
                Map<String, Object> targetInfo = new HashMap<>();
                targetInfo.put("targetPersonId", targetPerson.getId());
                messagingTemplate.convertAndSend("/topic/room/" + roomId + "/target", targetInfo);
            }

            return true;
        }
        return false;
    }

    /**
     * 处理玩家猜测
     */
    @Transactional
    public GameService.GuessResult handleGuess(String roomId, String playerId, Long guessPersonId) {
        // 检查游戏是否在进行中
        OnlineRoomState state = onlineRoomStates.get(roomId);
        if (state == null || !state.gameActive) {
            return null;
        }

        // 调用GameService的猜测逻辑（这会更新数据库）
        GameService.GuessResult result = gameService.guessPerson(roomId, playerId, guessPersonId);

        if (result != null) {
            // 广播猜测事件
            Map<String, Object> guessData = new HashMap<>();
            guessData.put("playerId", playerId);
            guessData.put("correct", result.isCorrect());
            guessData.put("guessedPerson", result.getGuessedPerson());
            guessData.put("remainingGuesses", result.getRemainingGuesses());
            guessData.put("comparison", result.getComparison());
            guessData.put("isLastGuess", result.getRemainingGuesses() == 0);

            GameEvent guessEvent = new GameEvent("GUESS_MADE", playerId, guessData);
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/events", guessEvent);

            // 广播更新的游戏统计
            broadcastGameStats(roomId);

            if (result.isCorrect()) {
                // 游戏结束，玩家获胜
                endGame(roomId, playerId);
            } else if (result.getRemainingGuesses() == 0) {
                // 该玩家用完了所有猜测次数
                GameEvent exhaustedEvent = new GameEvent("GUESSES_EXHAUSTED", playerId,
                        Map.of("playerId", playerId, "remainingPlayers", state.onlinePlayers.size()));
                messagingTemplate.convertAndSend("/topic/room/" + roomId + "/events", exhaustedEvent);

                // 检查是否所有玩家都用完了猜测次数
                if (checkAllPlayersExhausted(roomId)) {
                    forceEndGame(roomId);
                }
            }
        }

        return result;
    }

    /**
     * 检查是否所有玩家都用完了猜测次数
     */
    private boolean checkAllPlayersExhausted(String roomId) {
        // 直接从数据库检查所有玩家的猜测次数
        List<GamePlayer> players = gamePlayerRepository.findByRoom_RoomId(roomId);
        if (players.isEmpty()) {
            return false;
        }

        return players.stream()
                .allMatch(player ->
                        player.getStatus() == PlayerStatus.SURRENDERED ||
                                player.getGuessCount() >= MAX_GUESSES_PER_PLAYER
                );
    }

    /**
     * 强制结束游戏（无人猜中）
     */
    private void forceEndGame(String roomId) {
        OnlineRoomState state = onlineRoomStates.get(roomId);
        if (state == null) return;

        // 更新在线状态
        state.gameActive = false;

        // 调用GameService的结束游戏逻辑（无获胜者）
        gameService.endGame(roomId, null);

        // 广播游戏强制结束事件
        Map<String, Object> endData = new HashMap<>();
        endData.put("reason", "ALL_GUESSES_EXHAUSTED");
        endData.put("winner", null);
        endData.put("message", "所有玩家都用完了猜测次数，游戏结束！");

        GameEvent endEvent = new GameEvent("GAME_FORCE_ENDED", null, endData);
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/events", endEvent);
    }

    /**
     * 处理玩家投降
     */
    public boolean surrender(String roomId, String playerId) {
        OnlineRoomState state = onlineRoomStates.get(roomId);
        if (state == null || !state.gameActive) return false;

        // 调用GameService的投降逻辑
        boolean success = gameService.surrender(roomId, playerId);
        if (success) {
            // 广播投降事件
            GameEvent surrenderEvent = new GameEvent("SURRENDER", playerId,
                    Map.of("playerId", playerId, "message", "玩家选择投降"));
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/events", surrenderEvent);

            // 检查是否所有玩家都投降或用完了次数
            if (checkAllPlayersExhausted(roomId)) {
                forceEndGame(roomId);
            }

            return true;
        }

        return false;
    }

    /**
     * 结束游戏（有获胜者）
     */
    private void endGame(String roomId, String winnerId) {
        OnlineRoomState state = onlineRoomStates.get(roomId);
        if (state != null) {
            state.gameActive = false;
        }

        // 调用GameService的结束游戏逻辑
        gameService.endGame(roomId, winnerId);

        // 广播游戏结束事件
        GameEvent endEvent = new GameEvent("GAME_END", winnerId,
                Map.of("winnerId", winnerId, "reason", "CORRECT_GUESS"));
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/events", endEvent);
    }

    /**
     * 获取玩家剩余猜测次数（从GameService获取，依赖数据库）
     */
    public int getRemainingGuesses(String roomId, String playerId) {
        return gameService.getRemainingGuesses(roomId, playerId);
    }

    /**
     * 获取游戏统计信息（从GameService获取，依赖数据库）
     */
    public Map<String, Object> getGameStats(String roomId) {
        return gameService.getGameStats(roomId);
    }

    /**
     * 获取最大猜测次数
     */
    public static int getMaxGuessesPerPlayer() {
        return MAX_GUESSES_PER_PLAYER;
    }

    /**
     * 获取在线玩家数量
     */
    public int getOnlinePlayerCount(String roomId) {
        OnlineRoomState state = onlineRoomStates.get(roomId);
        return state != null ? state.onlinePlayers.size() : 0;
    }

    /**
     * 检查玩家是否在线
     */
    public boolean isPlayerOnline(String roomId, String playerId) {
        OnlineRoomState state = onlineRoomStates.get(roomId);
        return state != null && state.onlinePlayers.contains(playerId);
    }

    /**
     * 广播玩家列表
     */
    private void broadcastPlayerList(String roomId) {
        List<GamePlayer> players = gamePlayerService.getPlayersInRoom(roomId);
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/players", players);
    }

    /**
     * 广播游戏统计信息
     */
    private void broadcastGameStats(String roomId) {
        Map<String, Object> stats = getGameStats(roomId);
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/stats", stats);
    }

    /**
     * 向特定玩家发送私人消息
     */
    public void sendPrivateMessage(String roomId, String playerId, Object message) {
        messagingTemplate.convertAndSendToUser(playerId, "/queue/room/" + roomId + "/private", message);
    }

    /**
     * 广播系统消息
     */
    public void broadcastSystemMessage(String roomId, String message) {
        GameEvent systemEvent = new GameEvent("SYSTEM_MESSAGE", null,
                Map.of("message", message, "timestamp", new Date()));
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/events", systemEvent);
    }

    /**
     * 检查游戏是否可以开始
     */
    public boolean canStartGame(String roomId) {
        // 检查是否至少有1名玩家在线
        OnlineRoomState state = onlineRoomStates.get(roomId);
        if (state == null || state.onlinePlayers.isEmpty()) {
            return false;
        }

        // 检查游戏是否已经在进行中
        if (state.gameActive) {
            return false;
        }

        return true;
    }

    /**
     * 重置房间状态（用于测试或异常恢复）
     */
    public void resetRoomState(String roomId) {
        onlineRoomStates.remove(roomId);
        broadcastSystemMessage(roomId, "房间状态已重置");
    }
}