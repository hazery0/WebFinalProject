package com.hazergu.backend.service;

import com.hazergu.backend.entity.HistoricalPerson;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameRoomService {

    // 存储所有房间
    private final Map<String, RoomState> rooms = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private Map<String, List<Map<String, Object>>> roomGuesses = new ConcurrentHashMap<>();

    public void addGuess(String roomId, Map<String, Object> guessRecord) {
        roomGuesses.computeIfAbsent(roomId, k -> new ArrayList<>()).add(guessRecord);
    }

    public List<Map<String, Object>> getGuesses(String roomId) {
        return roomGuesses.getOrDefault(roomId, new ArrayList<>());
    }

    public void clearGuesses(String roomId) {
        roomGuesses.remove(roomId);
    }

    /**
     * 创建房间
     */
    public String createRoom(String creatorId, String creatorName) {
        String roomId = generateRoomId();

        RoomState roomState = new RoomState();
        roomState.setRoomId(roomId);
        roomState.setCreatorId(creatorId);
        roomState.setGameActive(false);

        PlayerInfo creator = new PlayerInfo(creatorId, creatorName, true); // 设置为房主
        roomState.getPlayers().add(creator);

        rooms.put(roomId, roomState);
        return roomId;
    }

    /**
     * 加入房间
     */
    public boolean joinRoom(String roomId, String playerId, String playerName) {
        RoomState roomState = rooms.get(roomId);
        if (roomState == null || roomState.isGameActive() || roomState.getPlayers().size() >= 8) {
            return false; // 房间不存在、游戏已开始或人数已满
        }

        // 检查玩家是否已经在房间中（重新加入的情况）
        for (PlayerInfo existingPlayer : roomState.getPlayers()) {
            if (existingPlayer.getId().equals(playerId)) {
                return true; // 玩家已在房间中
            }
        }

        PlayerInfo player = new PlayerInfo(playerId, playerName, false); // 新加入的不是房主
        roomState.getPlayers().add(player);
        return true;
    }

    /**
     * 玩家离开房间
     * @return 如果房间被销毁返回 true，否则返回 false
     */
    public boolean leaveRoom(String roomId, String playerId) {
        RoomState roomState = rooms.get(roomId);
        if (roomState == null) return true;

        // 找到离开的玩家
        PlayerInfo leavingPlayer = null;
        for (PlayerInfo player : roomState.getPlayers()) {
            if (player.getId().equals(playerId)) {
                leavingPlayer = player;
                break;
            }
        }

        if (leavingPlayer == null) {
            return false; // 玩家不在房间中
        }

        boolean isOwnerLeaving = leavingPlayer.isRoomOwner();

        // 从玩家列表中移除
        roomState.getPlayers().remove(leavingPlayer);

        // 如果房间空了，删除房间
        if (roomState.getPlayers().isEmpty()) {
            rooms.remove(roomId);
            return true;
        }

        // 如果房主离开，需要转移房主
        if (isOwnerLeaving) {
            // 从剩余玩家中随机选择新房主
            List<PlayerInfo> remainingPlayers = roomState.getPlayers();
            if (!remainingPlayers.isEmpty()) {
                PlayerInfo newOwner = remainingPlayers.get(random.nextInt(remainingPlayers.size()));
                newOwner.setRoomOwner(true);
                roomState.setCreatorId(newOwner.getId()); // 更新房主ID
            }
        }

        return false; // 房间未被销毁
    }

    /**
     * 开始游戏
     */
    public void startGame(String roomId, HistoricalPerson targetPerson) {
        RoomState roomState = rooms.get(roomId);
        if (roomState != null) {
            roomState.setGameActive(true);
            roomState.setTargetPerson(targetPerson);
            roomState.setStartTime(new Date());
            roomState.setWinnerId(null);
            roomState.setEndTime(null);
            // 清空猜测历史
            clearGuesses(roomId);

            // 初始化玩家状态
            for (PlayerInfo player : roomState.getPlayers()) {
                player.setGuessCount(0);
                player.setHasSurrendered(false);
            }
        }
    }

    /**
     * 结束游戏
     */
    public void endGame(String roomId, String winnerId) {
        RoomState roomState = rooms.get(roomId);
        if (roomState != null) {
            roomState.setGameActive(false);
            roomState.setWinnerId(winnerId);
            roomState.setEndTime(new Date());
        }
    }

    /**
     * 玩家投降
     */
    public void surrender(String roomId, String playerId) {
        RoomState roomState = rooms.get(roomId);
        if (roomState != null && roomState.isGameActive()) {
            // 找到玩家并标记为已投降
            for (PlayerInfo player : roomState.getPlayers()) {
                if (player.getId().equals(playerId)) {
                    player.setHasSurrendered(true);
                    break;
                }
            }
            // 检查游戏是否结束
            checkGameEnd(roomId);
        }
    }

    /**
     * 增加玩家猜测次数
     */
    public void incrementGuessCount(String roomId, String playerId) {
        RoomState roomState = rooms.get(roomId);
        if (roomState != null) {
            for (PlayerInfo player : roomState.getPlayers()) {
                if (player.getId().equals(playerId)) {
                    player.setGuessCount(player.getGuessCount() + 1);
                    break;
                }
            }
        }
    }

    /**
     * 检查游戏是否结束
     * @return 是否结束
     */
    public boolean checkGameEnd(String roomId) {
        RoomState roomState = rooms.get(roomId);
        if (roomState == null || !roomState.isGameActive()) {
            return false;
        }

        int activePlayers = 0;
        int surrenderedPlayers = 0;
        int maxGuessesReached = 0;

        for (PlayerInfo player : roomState.getPlayers()) {
            if (player.isHasSurrendered()) {
                surrenderedPlayers++;
            } else {
                activePlayers++;
                if (player.getGuessCount() >= roomState.getMaxGuessCount()) {
                    maxGuessesReached++;
                }
            }
        }

        // 所有玩家都已投降
        if (surrenderedPlayers == roomState.getPlayers().size()) {
            endGame(roomId, null); // 没有胜利者
            return true;
        }

        // 所有活跃玩家都已达到最大猜测次数
        if (maxGuessesReached == activePlayers && activePlayers > 0) {
            endGame(roomId, null); // 没有胜利者
            return true;
        }

        return false;
    }

    /**
     * 获取房间状态
     */
    public RoomState getRoomState(String roomId) {
        return rooms.get(roomId);
    }

    /**
     * 获取所有房间信息
     */
    public List<RoomInfo> getAllRooms() {
        List<RoomInfo> roomInfos = new ArrayList<>();
        for (RoomState roomState : rooms.values()) {
            RoomInfo info = new RoomInfo();
            info.setRoomId(roomState.getRoomId());
            info.setCreatorId(roomState.getCreatorId());
            info.setPlayerCount(roomState.getPlayers().size());
            info.setGameActive(roomState.isGameActive());

            // 获取房主名称
            roomState.getPlayers().stream()
                    .filter(p -> p.getId().equals(roomState.getCreatorId()))
                    .findFirst()
                    .ifPresent(p -> info.setCreatorName(p.getName()));

            roomInfos.add(info);
        }
        return roomInfos;
    }

    /**
     * 检查是否是房主
     */
    public boolean isRoomOwner(String roomId, String playerId) {
        RoomState roomState = rooms.get(roomId);
        return roomState != null && roomState.getCreatorId().equals(playerId);
    }

    /**
     * 生成随机房间ID
     */
    private String generateRoomId() {
        return String.format("%04d", random.nextInt(10000));
    }

    // ============ 内部类 ============

    /**
     * 房间状态
     */
    public static class RoomState {
        private String roomId;
        private String creatorId;
        private boolean gameActive;
        private HistoricalPerson targetPerson;
        private String winnerId;
        private Date startTime;
        private Date endTime;
        private List<PlayerInfo> players = new ArrayList<>();
        private int maxGuessCount = 10; // 最大猜测次数

        // getters and setters
        public String getRoomId() { return roomId; }
        public void setRoomId(String roomId) { this.roomId = roomId; }

        public String getCreatorId() { return creatorId; }
        public void setCreatorId(String creatorId) { this.creatorId = creatorId; }

        public boolean isGameActive() { return gameActive; }
        public void setGameActive(boolean gameActive) { this.gameActive = gameActive; }

        public HistoricalPerson getTargetPerson() { return targetPerson; }
        public void setTargetPerson(HistoricalPerson targetPerson) { this.targetPerson = targetPerson; }

        public String getWinnerId() { return winnerId; }
        public void setWinnerId(String winnerId) { this.winnerId = winnerId; }

        public Date getStartTime() { return startTime; }
        public void setStartTime(Date startTime) { this.startTime = startTime; }

        public Date getEndTime() { return endTime; }
        public void setEndTime(Date endTime) { this.endTime = endTime; }

        public List<PlayerInfo> getPlayers() { return players; }
        public void setPlayers(List<PlayerInfo> players) { this.players = players; }

        public int getMaxGuessCount() { return maxGuessCount; }
        public void setMaxGuessCount(int maxGuessCount) { this.maxGuessCount = maxGuessCount; }
    }

    /**
     * 玩家信息
     */
    public static class PlayerInfo {
        private String id;
        private String name;
        private boolean isRoomOwner;
        private int score;
        private int guessCount; // 猜测次数
        private boolean hasSurrendered; // 是否已投降

        public PlayerInfo(String id, String name, boolean isRoomOwner) {
            this.id = id;
            this.name = name;
            this.isRoomOwner = isRoomOwner;
            this.score = 0;
            this.guessCount = 0;
            this.hasSurrendered = false;
        }

        // getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public boolean isRoomOwner() { return isRoomOwner; }
        public void setRoomOwner(boolean roomOwner) { isRoomOwner = roomOwner; }

        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }

        public int getGuessCount() { return guessCount; }
        public void setGuessCount(int guessCount) { this.guessCount = guessCount; }
        public void incrementGuessCount() { this.guessCount++; }

        public boolean isHasSurrendered() { return hasSurrendered; }
        public void setHasSurrendered(boolean hasSurrendered) { this.hasSurrendered = hasSurrendered; }
    }

    /**
     * 房间信息（用于列表展示）
     */
    public static class RoomInfo {
        private String roomId;
        private String creatorId;
        private String creatorName;
        private int playerCount;
        private boolean gameActive;

        // getters and setters
        public String getRoomId() { return roomId; }
        public void setRoomId(String roomId) { this.roomId = roomId; }

        public String getCreatorId() { return creatorId; }
        public void setCreatorId(String creatorId) { this.creatorId = creatorId; }

        public String getCreatorName() { return creatorName; }
        public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

        public int getPlayerCount() { return playerCount; }
        public void setPlayerCount(int playerCount) { this.playerCount = playerCount; }

        public boolean isGameActive() { return gameActive; }
        public void setGameActive(boolean gameActive) { this.gameActive = gameActive; }
    }
}