package com.hazergu.backend.entity;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class MessageModels {

    // 基础消息模型
    @Data
    public static class BaseMessage {
        private String type;
        private String message;
        private Long timestamp;
    }

    // 创建房间请求
    @Data
    public static class CreateRoomRequest {
        private String playerId;
        private String playerName;
    }

    // 创建房间响应
    @Data
    public static class CreateRoomResponse {
        private String type = "ROOM_CREATED";
        private String roomId;
        private String creator;
        private String message;
    }

    // 加入房间请求
    @Data
    public static class JoinRoomRequest {
        private String playerId;
        private String playerName;
        private String roomId;
    }

    // 玩家加入响应
    @Data
    public static class PlayerJoinedResponse {
        private String type = "PLAYER_JOINED";
        private String roomId;
        private String playerId;
        private String playerName;
        private String message;
    }

    // 猜测请求
    @Data
    public static class GuessRequest {
        private String roomId;
        private String playerId;
        private String guess;
    }

    // 猜测结果响应
    @Data
    public static class GuessResultResponse {
        private String type = "GUESS_RESULT";
        private String playerId;
        private String guess;
        private boolean isCorrect;
        private String message;
        private boolean gameEnded;
        private String winner;
        private Map<String, Object> targetPerson;
        private String hint;
        private Map<String, Object> guessedPerson;
    }

    // 聊天消息
    @Data
    public static class ChatMessage {
        private String type = "CHAT_MESSAGE";
        private String playerId;
        private String message;
        private Long timestamp = System.currentTimeMillis();
    }

    // 房间列表响应
    @Data
    public static class RoomListResponse {
        private String type = "ROOM_LIST";
        private List<Map<String, Object>> rooms;
    }

    // 房间状态响应
    @Data
    public static class RoomStateResponse {
        private String type = "ROOM_STATE";
        private Map<String, Object> roomState;
    }

    // 错误响应
    @Data
    public static class ErrorResponse {
        private String type = "ERROR";
        private String message;
        private String errorCode;
    }
}