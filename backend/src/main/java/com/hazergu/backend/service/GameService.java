package com.hazergu.backend.service;

import com.hazergu.backend.entity.*;
import com.hazergu.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 游戏服务（重构版 - 移除内存Map，依赖数据库作为唯一事实来源）
 */
@Service
public class GameService {

    @Autowired
    private GameRoomRepository gameRoomRepository;

    @Autowired
    private GamePlayerRepository gamePlayerRepository;

    @Autowired
    private HistoricalPersonService historicalPersonService;

    @Autowired
    private GameHistoryRepository gameHistoryRepository;

    // 每个玩家的最大猜测次数
    private static final int MAX_GUESSES_PER_PLAYER = 5;

    /**
     * 开始游戏
     * @param roomId 房间ID
     * @return 更新后的房间信息
     */
    @Transactional
    public Optional<GameRoom> startGame(String roomId) {
        // 检查房间是否存在
        Optional<GameRoom> roomOptional = gameRoomRepository.findById(roomId);
        if (roomOptional.isPresent()) {
            GameRoom room = roomOptional.get();

            // 检查房间状态是否为等待中
            if (room.getStatus() != GameStatus.WAITING) {
                return Optional.empty();
            }

            // 获取房间内的玩家
            List<GamePlayer> players = gamePlayerRepository.findByRoom_RoomId(roomId);
            if (players.isEmpty()) {
                return Optional.empty();
            }

            // 重置玩家的猜测次数（从数据库直接操作）
            players.forEach(player -> {
                player.setGuessCount(0);
                player.setStatus(PlayerStatus.PLAYING);
                gamePlayerRepository.save(player);
            });

            // 生成随机目标人物
            Optional<HistoricalPerson> targetPersonOptional = historicalPersonService.getRandomHistoricalPerson();
            if (targetPersonOptional.isPresent()) {
                HistoricalPerson targetPerson = targetPersonOptional.get();
                room.setTargetPerson(targetPerson);
                room.setStatus(GameStatus.PLAYING);

                // 保存房间状态
                gameRoomRepository.save(room);

                return Optional.of(room);
            }
        }
        return Optional.empty();
    }

    /**
     * 结束游戏
     * @param roomId 房间ID
     * @param winnerId 获胜玩家ID（可为null，表示无人获胜）
     * @return 更新后的房间信息
     */
    @Transactional
    public Optional<GameRoom> endGame(String roomId, String winnerId) {
        // 检查房间是否存在
        Optional<GameRoom> roomOptional = gameRoomRepository.findById(roomId);
        if (roomOptional.isPresent()) {
            GameRoom room = roomOptional.get();

            // 检查房间状态是否为游戏中
            if (room.getStatus() != GameStatus.PLAYING) {
                return Optional.empty();
            }

            // 更新房间状态为结束
            room.setStatus(GameStatus.ENDED);
            gameRoomRepository.save(room);

            // 更新玩家状态
            List<GamePlayer> players = gamePlayerRepository.findByRoom_RoomId(roomId);
            for (GamePlayer player : players) {
                if (winnerId != null && player.getPlayerId().equals(winnerId)) {
                    player.setStatus(PlayerStatus.WINNER);
                } else {
                    player.setStatus(PlayerStatus.LOSER);
                }
                gamePlayerRepository.save(player);
            }

            // 保存游戏历史
            GameHistory history = new GameHistory();
            history.setRoomId(roomId);
            history.setStartTime(room.getCreateTime());
            history.setEndTime(LocalDateTime.now());
            history.setTargetPerson(room.getTargetPerson());
            history.setWinnerId(winnerId);

            // 计算总猜测次数（直接从数据库聚合）
            int totalGuessCount = players.stream()
                    .mapToInt(GamePlayer::getGuessCount)
                    .sum();
            history.setTotalGuessCount(totalGuessCount);
            gameHistoryRepository.save(history);

            return Optional.of(room);
        }
        return Optional.empty();
    }

    /**
     * 玩家猜测历史人物（核心方法 - 事务性操作）
     * @param roomId 房间ID
     * @param playerId 玩家ID
     * @param personId 猜测的历史人物ID
     * @return 猜测结果
     */
    @Transactional
    public GuessResult guessPerson(String roomId, String playerId, Long personId) {
        // 获取房间和玩家信息（使用数据库作为唯一事实来源）
        Optional<GameRoom> roomOptional = gameRoomRepository.findById(roomId);
        Optional<GamePlayer> playerOptional = gamePlayerRepository.findByRoom_RoomIdAndPlayerId(roomId, playerId);

        if (!roomOptional.isPresent() || !playerOptional.isPresent()) {
            return null;
        }

        GameRoom room = roomOptional.get();
        GamePlayer player = playerOptional.get();

        // 检查游戏是否进行中
        if (room.getStatus() != GameStatus.PLAYING) {
            return null;
        }

        // 检查玩家是否还有猜测次数（直接查询数据库）
        if (!canGuess(player)) {
            return null;
        }

        // 增加猜测次数并保存到数据库
        int newGuessCount = player.getGuessCount() + 1;
        player.setGuessCount(newGuessCount);
        gamePlayerRepository.save(player);

        int remainingGuesses = MAX_GUESSES_PER_PLAYER - newGuessCount;

        // 获取目标人物
        HistoricalPerson targetPerson = room.getTargetPerson();

        // 获取猜测的人物
        Optional<HistoricalPerson> guessedPersonOptional = historicalPersonService.getHistoricalPersonById(personId);
        if (!guessedPersonOptional.isPresent() || targetPerson == null) {
            return null;
        }

        HistoricalPerson guessedPerson = guessedPersonOptional.get();

        // 判断是否猜对
        boolean isCorrect = targetPerson.getId().equals(guessedPerson.getId());

        // 生成比较结果
        ComparisonResult comparison = comparePersons(targetPerson, guessedPerson);

        return new GuessResult(isCorrect, comparison, guessedPerson, remainingGuesses);
    }

    /**
     * 比较两个历史人物的属性
     * @param target 目标人物
     * @param guessed 猜测的人物
     * @return 比较结果
     */
    private ComparisonResult comparePersons(HistoricalPerson target, HistoricalPerson guessed) {
        ComparisonResult result = new ComparisonResult();

        // 比较出生年份
        if (target.getBirthYear() != null && guessed.getBirthYear() != null) {
            if (target.getBirthYear().equals(guessed.getBirthYear())) {
                result.setBirthYearMatch(true);
            } else {
                result.setBirthYearEarlier(target.getBirthYear() < guessed.getBirthYear());
            }
        }

        // 比较文学属性
        result.setLiteraryMatch(target.getIsLiterary().equals(guessed.getIsLiterary()));

        // 比较政治属性
        result.setPoliticalMatch(target.getIsPolitical().equals(guessed.getIsPolitical()));

        // 比较思想家属性
        result.setThinkerMatch(target.getIsThinker().equals(guessed.getIsThinker()));

        // 比较科学家属性
        result.setScientistMatch(target.getIsScientist().equals(guessed.getIsScientist()));

        return result;
    }

    /**
     * 获取游戏状态
     * @param roomId 房间ID
     * @return 游戏状态信息
     */
    public GameStatusInfo getGameStatus(String roomId) {
        Optional<GameRoom> roomOptional = gameRoomRepository.findById(roomId);
        if (roomOptional.isPresent()) {
            GameRoom room = roomOptional.get();
            List<GamePlayer> players = gamePlayerRepository.findByRoom_RoomId(roomId);

            return new GameStatusInfo(
                    room.getStatus(),
                    players,
                    room.getTargetPerson() != null
            );
        }
        return null;
    }

    /**
     * 获取玩家剩余猜测次数（直接从数据库查询）
     */
    public int getRemainingGuesses(String roomId, String playerId) {
        return gamePlayerRepository.findByRoom_RoomIdAndPlayerId(roomId, playerId)
                .map(player -> {
                    int remaining = MAX_GUESSES_PER_PLAYER - player.getGuessCount();
                    return Math.max(0, remaining);
                })
                .orElse(0);
    }

    /**
     * 获取游戏统计信息（基于数据库数据）
     */
    public Map<String, Object> getGameStats(String roomId) {
        Map<String, Object> stats = new HashMap<>();
        Map<String, Integer> remainingGuesses = new HashMap<>();

        List<GamePlayer> players = gamePlayerRepository.findByRoom_RoomId(roomId);

        if (players != null && !players.isEmpty()) {
            boolean allExhausted = true;

            for (GamePlayer player : players) {
                String playerId = player.getPlayerId();
                int usedCount = player.getGuessCount();
                int remaining = MAX_GUESSES_PER_PLAYER - usedCount;
                remainingGuesses.put(playerId, remaining);

                // 检查是否有玩家还有剩余次数
                if (remaining > 0) {
                    allExhausted = false;
                }
            }

            stats.put("remainingGuesses", remainingGuesses);
            stats.put("maxGuesses", MAX_GUESSES_PER_PLAYER);
            stats.put("activePlayers", players.size());
            stats.put("allGuessesExhausted", allExhausted);
        }

        return stats;
    }

    /**
     * 玩家投降
     */
    @Transactional
    public boolean surrender(String roomId, String playerId) {
        Optional<GamePlayer> playerOptional = gamePlayerRepository.findByRoom_RoomIdAndPlayerId(roomId, playerId);
        if (playerOptional.isPresent()) {
            GamePlayer player = playerOptional.get();
            player.setStatus(PlayerStatus.SURRENDERED);
            gamePlayerRepository.save(player);

            // 可以触发游戏结束检查
            checkGameEndCondition(roomId);
            return true;
        }
        return false;
    }

    /**
     * 检查游戏结束条件
     */
    private void checkGameEndCondition(String roomId) {
        // 检查是否所有玩家都用完了猜测次数或投降
        List<GamePlayer> players = gamePlayerRepository.findByRoom_RoomId(roomId);

        boolean allExhaustedOrSurrendered = players.stream()
                .allMatch(player ->
                        player.getStatus() == PlayerStatus.SURRENDERED ||
                                player.getGuessCount() >= MAX_GUESSES_PER_PLAYER
                );

        if (allExhaustedOrSurrendered) {
            // 自动结束游戏（无获胜者）
            endGame(roomId, null);
        }
    }

    /**
     * 检查玩家是否还有猜测次数（基于数据库数据）
     */
    public boolean canGuess(String roomId, String playerId) {
        return getRemainingGuesses(roomId, playerId) > 0;
    }

    /**
     * 检查玩家是否还有猜测次数（基于GamePlayer对象）
     */
    private boolean canGuess(GamePlayer player) {
        return player.getGuessCount() < MAX_GUESSES_PER_PLAYER;
    }

    /**
     * 检查是否所有玩家都用完了猜测次数
     */
    public boolean allGuessesExhausted(String roomId) {
        Map<String, Object> stats = getGameStats(roomId);
        return stats.containsKey("allGuessesExhausted") &&
                (boolean) stats.get("allGuessesExhausted");
    }

    /**
     * 获取最大猜测次数
     */
    public static int getMaxGuessesPerPlayer() {
        return MAX_GUESSES_PER_PLAYER;
    }

    /**
     * 内部类：猜测结果
     */
    public static class GuessResult {
        private boolean correct;
        private ComparisonResult comparison;
        private HistoricalPerson guessedPerson;
        private int remainingGuesses;

        public GuessResult(boolean correct, ComparisonResult comparison,
                           HistoricalPerson guessedPerson, int remainingGuesses) {
            this.correct = correct;
            this.comparison = comparison;
            this.guessedPerson = guessedPerson;
            this.remainingGuesses = remainingGuesses;
        }

        // getter和setter方法
        public boolean isCorrect() { return correct; }
        public void setCorrect(boolean correct) { this.correct = correct; }
        public ComparisonResult getComparison() { return comparison; }
        public void setComparison(ComparisonResult comparison) { this.comparison = comparison; }
        public HistoricalPerson getGuessedPerson() { return guessedPerson; }
        public void setGuessedPerson(HistoricalPerson guessedPerson) { this.guessedPerson = guessedPerson; }
        public int getRemainingGuesses() { return remainingGuesses; }
        public void setRemainingGuesses(int remainingGuesses) { this.remainingGuesses = remainingGuesses; }
    }

    /**
     * 内部类：比较结果
     */
    public static class ComparisonResult {
        private boolean birthYearMatch;
        private Boolean birthYearEarlier; // null表示无法比较
        private boolean literaryMatch;
        private boolean politicalMatch;
        private boolean thinkerMatch;
        private boolean scientistMatch;

        // getter和setter方法
        public boolean isBirthYearMatch() { return birthYearMatch; }
        public void setBirthYearMatch(boolean birthYearMatch) { this.birthYearMatch = birthYearMatch; }
        public Boolean getBirthYearEarlier() { return birthYearEarlier; }
        public void setBirthYearEarlier(Boolean birthYearEarlier) { this.birthYearEarlier = birthYearEarlier; }
        public boolean isLiteraryMatch() { return literaryMatch; }
        public void setLiteraryMatch(boolean literaryMatch) { this.literaryMatch = literaryMatch; }
        public boolean isPoliticalMatch() { return politicalMatch; }
        public void setPoliticalMatch(boolean politicalMatch) { this.politicalMatch = politicalMatch; }
        public boolean isThinkerMatch() { return thinkerMatch; }
        public void setThinkerMatch(boolean thinkerMatch) { this.thinkerMatch = thinkerMatch; }
        public boolean isScientistMatch() { return scientistMatch; }
        public void setScientistMatch(boolean scientistMatch) { this.scientistMatch = scientistMatch; }
    }

    /**
     * 内部类：游戏状态信息
     */
    public static class GameStatusInfo {
        private GameStatus status;
        private java.util.List<GamePlayer> players;
        private boolean hasTargetPerson;

        public GameStatusInfo(GameStatus status, java.util.List<GamePlayer> players, boolean hasTargetPerson) {
            this.status = status;
            this.players = players;
            this.hasTargetPerson = hasTargetPerson;
        }

        // getter和setter方法
        public GameStatus getStatus() { return status; }
        public void setStatus(GameStatus status) { this.status = status; }
        public java.util.List<GamePlayer> getPlayers() { return players; }
        public void setPlayers(java.util.List<GamePlayer> players) { this.players = players; }
        public boolean isHasTargetPerson() { return hasTargetPerson; }
        public void setHasTargetPerson(boolean hasTargetPerson) { this.hasTargetPerson = hasTargetPerson; }
    }
}