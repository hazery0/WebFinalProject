import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { GameRoom, GamePlayer, HistoricalPerson } from '../services/api';

export const useGameStore = defineStore('game', () => {
  // 状态
  const stompClient = ref<Client | null>(null);
  const isConnected = ref(false);

  const currentRoom = ref<GameRoom | null>(null);
  const players = ref<GamePlayer[]>([]);
  const gameStatus = ref<'waiting' | 'playing' | 'ended'>('waiting');

  // 当前玩家信息
  const myPlayerId = ref<string>('');
  const myUsername = ref<string>('');

  // 游戏数据
  const targetPersonId = ref<number | null>(null); // 多人模式通常不直接给完整对象，防作弊
  const winnerId = ref<string | null>(null);
  const guessResults = ref<any[]>([]); // 存储所有人的猜测记录
  const remainingGuesses = ref<number>(5);

  const isHost = computed(() => {
    const me = players.value.find(p => p.playerId === myPlayerId.value);
    return me ? me.isHost : false;
  });

  // --- WebSocket Actions ---

  // 1. 初始化连接
  function connect(roomId: string, playerId: string, username: string) {
    if (stompClient.value && stompClient.value.active) return;

    myPlayerId.value = playerId;
    myUsername.value = username;

    const socket = new SockJS('http://localhost:8080/ws');
    const client = new Client({
      webSocketFactory: () => socket,
      debug: (str) => console.log('[WS Debug]: ' + str),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    client.onConnect = () => {
      isConnected.value = true;
      console.log('WebSocket 连接成功');

      // 订阅房间频道
      subscribeToRoom(client, roomId);

      // 发送加入消息
      client.publish({
        destination: `/app/game/${roomId}/join`,
        body: JSON.stringify({ playerId, username })
      });
    };

    client.onStompError = (frame) => {
      console.error('Broker reported error: ' + frame.headers['message']);
      console.error('Additional details: ' + frame.body);
    };

    client.activate();
    stompClient.value = client;
  }

  // 2. 订阅房间消息
  function subscribeToRoom(client: Client, roomId: string) {
    // 监听通用游戏事件 (根据你后端 RealTimeGameService 的逻辑)
    // 后端广播地址是 /topic/room/{roomId}/events 或 /topic/game/{roomId}
    // 假设我们按照优化后的建议统一使用 /topic/game/{roomId}

    client.subscribe(`/topic/game/${roomId}`, (message) => {
      const body = JSON.parse(message.body);
      handleSocketMessage(body);
    });
  }

  // 3. 处理后端推过来的消息
  function handleSocketMessage(msg: any) {
    console.log('收到消息:', msg);
    // msg 结构参考后端 WebSocketMessage: { type: string, data: object }
    // 或者直接是数据包。这里假设后端返回 { type: '...', ...data } 混合结构或由 type 字段区分

    // 如果后端返回的是 WebSocketMessage 包装类
    const type = msg.type;
    const data = msg.data || msg; // 兼容处理

    switch (type) {
      case 'player-joined':
      case 'player-left':
        if (data.room) currentRoom.value = data.room;
        if (data.players) players.value = data.players;
        break;

      case 'game-started':
        gameStatus.value = 'playing';
        if (data.room) currentRoom.value = data.room;
        guessResults.value = []; // 清空旧记录
        remainingGuesses.value = data.maxGuesses || 5;
        // 游戏开始时后端通常不直接返回目标详情，只返回ID或加密串，
        // 或者由前端 GuessModule 获取到 ID 后去查询
        break;

      case 'guess-result':
        // 某人猜了
        const result = data.result || data;
        const guesserId = data.playerId;

        // 如果是我猜的
        if (guesserId === myPlayerId.value) {
          remainingGuesses.value = data.remainingGuesses;
        }

        // 记录猜测结果（可以在UI上显示谁猜了什么）
        guessResults.value.unshift({
          playerId: guesserId,
          playerName: players.value.find(p => p.playerId === guesserId)?.username || '未知',
          correct: data.isCorrect,
          message: data.message
        });

        if (data.gameEnded) {
          gameStatus.value = 'ended';
          winnerId.value = data.winnerId;
        }
        break;

      case 'game-status':
        // 同步完整状态
        if (data.status) {
            gameStatus.value = data.status.status === 'PLAYING' ? 'playing' : 'waiting';
            players.value = data.players;
        }
        break;
    }
  }

  // --- 发送指令 Actions ---

  function startGame(roomId: string) {
    if (!stompClient.value || !isConnected.value) return;
    stompClient.value.publish({
      destination: `/app/game/${roomId}/start`,
      body: JSON.stringify({ playerId: myPlayerId.value })
    });
  }

  function makeGuess(roomId: string, personId: number) {
    if (!stompClient.value || !isConnected.value) return;
    stompClient.value.publish({
      destination: `/app/game/${roomId}/guess`,
      body: JSON.stringify({ playerId: myPlayerId.value, personId })
    });
  }

  function leaveRoom(roomId: string) {
    if (stompClient.value && isConnected.value) {
      stompClient.value.publish({
        destination: `/app/game/${roomId}/leave`,
        body: JSON.stringify({ playerId: myPlayerId.value })
      });
      stompClient.value.deactivate();
    }
    stompClient.value = null;
    isConnected.value = false;
    currentRoom.value = null;
    players.value = [];
  }

  return {
    connect,
    startGame,
    makeGuess,
    leaveRoom,
    isConnected,
    currentRoom,
    players,
    gameStatus,
    myPlayerId,
    isHost,
    remainingGuesses,
    guessResults,
    winnerId
  };
});
