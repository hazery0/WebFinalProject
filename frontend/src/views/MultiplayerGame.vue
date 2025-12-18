<template>
  <div class="historical-game-container">
    <!-- 头部 -->
    <header class="game-header">
      <h1>多人模式 - 房间 {{ roomId }}</h1>
      <div class="connection-status">
        <span :class="['status-dot', isConnected ? 'online' : 'offline']"></span>
        {{ isConnected ? '已连接' : '连接中...' }}
      </div>
    </header>

    <!-- 房间状态和玩家信息 -->
    <section class="room-info-section">
      <div class="room-info-card">
        <div class="room-status">
          <h2>房间状态: <span :class="['status-badge', roomStatus]">{{ roomStatusText }}</span></h2>
          <div v-if="isHost && roomStatus === 'waiting'" class="start-game-control">
            <button @click="startGame" class="start-btn">开始游戏</button>
            <p class="hint">需要至少1名玩家才能开始</p>
          </div>
        </div>

        <div class="players-info">
          <h3>玩家列表 ({{ players.length }}/2):</h3>
          <div class="players-list">
            <div v-for="player in players" :key="player.playerId" class="player-card">
              <div class="player-header">
                <span class="player-name">
                  {{ player.playerName }}
                  <span v-if="player.playerId === currentPlayerId">(我)</span>
                </span>
                <span v-if="player.isHost" class="host-badge">房主</span>
              </div>
              <div class="player-stats">
                剩余猜测: {{ player.playerId === currentPlayerId ? remainingGuesses : '?' }}
              </div>
            </div>
            <div v-if="players.length === 0" class="no-players">
              暂无玩家
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 游戏区域 -->
    <div v-if="roomStatus === 'playing'">
      <!-- 使用 GuessModule 组件 -->
      <GuessModule
        ref="guessModuleRef"
        :disabled="!isMyTurn || gameEnded"
        :auto-get-random="false"
        :show-header="false"
        :show-login-status="false"
        :title="'多人模式'"
        @select="handleSelectPerson"
        @guess="handleGuess"
        @surrender="handleSurrender"
        @reset="handleReset"
        @win="handleWin"
      />

      <!-- 等待对手回合 -->
      <div v-if="!isMyTurn && roomStatus === 'playing'" class="waiting-turn">
        <div class="waiting-content">
          <h3>等待对手回合...</h3>
          <p>当前轮到: {{ currentTurnPlayer }}</p>
        </div>
      </div>

      <!-- 实时战况 -->
      <div class="battle-log">
        <h3>实时战况</h3>
        <ul>
          <li v-for="(log, index) in gameLogs" :key="index" :class="{'my-log': log.playerId === currentPlayerId}">
            <span class="log-player">{{ log.playerName }}</span>:
            {{ log.message }}
          </li>
        </ul>
      </div>

      <!-- 游戏结果 -->
      <div v-if="gameEnded" class="game-result-alert">
        <h3>游戏结束!</h3>
        <div class="result-message">
          {{ gameResultMessage }}
        </div>
        <div class="result-actions">
          <button @click="leaveRoom" class="leave-btn">离开房间</button>
        </div>
      </div>
    </div>

    <!-- 等待游戏开始 -->
    <div v-else-if="roomStatus === 'waiting'" class="waiting-section">
      <div class="waiting-content">
        <h2>等待游戏开始...</h2>
        <p>当前玩家: {{ players.length }}/2</p>
        <p v-if="isHost">你是房主，可以点击"开始游戏"按钮开始游戏</p>
        <p v-else>等待房主开始游戏...</p>
        <button @click="leaveRoom" class="leave-btn">离开房间</button>
      </div>
    </div>

    <!-- 投降确认模态框 -->
    <div v-if="showSurrenderModal" class="modal-overlay" @click="closeSurrenderModal">
      <div class="modal" @click.stop>
        <h3>确认投降</h3>
        <p>你确定要投降吗？</p>
        <div class="modal-actions">
          <button @click="confirmSurrender" class="confirm-btn">确认</button>
          <button @click="closeSurrenderModal" class="cancel-btn">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { gameRoomApi, gameApi } from '../services/api';
import type { GamePlayer, HistoricalPerson } from '../services/api';
import GuessModule from './GuessModule.vue';

// WebSocket 相关
import Stomp from 'stompjs';
import SockJS from 'sockjs-client';

const route = useRoute();
const router = useRouter();
const roomId = computed(() => route.params.roomId as string);
const guessModuleRef = ref<InstanceType<typeof GuessModule>>();

// WebSocket 状态
const isConnected = ref(false);
let stompClient: any = null;
const wsUrl = 'http://localhost:8080/ws';

// 游戏状态
const roomStatus = ref('waiting');
const players = ref<GamePlayer[]>([]);
const gameEnded = ref(false);
const gameResultMessage = ref('');
const showSurrenderModal = ref(false);
const currentTurnPlayer = ref('');
const isMyTurn = ref(true);
const currentPlayerId = ref('');
const currentPlayerName = ref('');
const remainingGuesses = ref(5);
const gameLogs = ref<Array<{playerId: string, playerName: string, message: string}>>([]);

// 计算属性
const roomStatusText = computed(() => {
  const statusMap: Record<string, string> = {
    waiting: '等待中',
    playing: '游戏中',
    finished: '已结束'
  };
  return statusMap[roomStatus.value] || roomStatus.value;
});

const isHost = computed(() => {
  return players.value.some(player => player.playerId === currentPlayerId.value && player.isHost);
});

// WebSocket 连接
const connectWebSocket = () => {
  console.log('正在连接WebSocket...');

  // 创建SockJS实例时配置transports和其他选项
  const socket = new SockJS(wsUrl, null, {
    // 禁用withCredentials，避免CORS问题
    withCredentials: false,
    // 只使用WebSocket作为传输方式，避免使用HTTP轮询
    transports: ['websocket']
  });

  stompClient = Stomp.over(socket);

  // 禁用withCredentials，避免CORS问题
  stompClient.withCredentials = false;

  // 配置连接头
  const headers = {
    // 可以添加认证信息等
  };

  stompClient.connect(headers, () => {
    console.log('WebSocket 连接成功');
    isConnected.value = true;

    // 订阅房间广播
    stompClient.subscribe(`/topic/game/${roomId.value}`, (message: any) => {
      const data = JSON.parse(message.body);
      handleWebSocketMessage(data);
    });

    // 订阅私有消息
    stompClient.subscribe(`/user/queue/room/${roomId.value}/private`, (message: any) => {
      const data = JSON.parse(message.body);
      handlePrivateMessage(data);
    });

    // 发送加入房间消息
    sendWebSocketMessage('join', {
      type: 'JOIN',
      playerId: currentPlayerId.value,
      username: currentPlayerName.value
    });
  }, (error: any) => {
    console.error('WebSocket 连接失败:', error);
    isConnected.value = false;
    // 连接失败时，使用HTTP轮询作为备选方案
    console.log('WebSocket连接失败，使用HTTP轮询替代');
    fetchRoomInfo();
    setInterval(() => {
      fetchRoomInfo();
    }, 200); // 每2秒获取一次房间信息
  });
};

// 发送 WebSocket 消息
const sendWebSocketMessage = (endpoint: string, message: any) => {
  if (stompClient && stompClient.connected) {
    stompClient.send(`/app/game/${roomId.value}/${endpoint}`, {}, JSON.stringify(message));
  }
};

// 处理 WebSocket 广播消息
const handleWebSocketMessage = (data: any) => {
  console.log('收到广播消息:', data);

  switch (data.type) {
    case 'PLAYER_JOINED':
      handlePlayerJoined(data);
      break;
    case 'PLAYER_LEFT':
      handlePlayerLeft(data);
      break;
    case 'GAME_START':
      handleGameStart(data);
      break;
    case 'GUESS_MADE':
      handleGuessMade(data);
      break;
    case 'GAME_END':
      handleGameEnd(data);
      break;
    case 'GAME_FORCE_ENDED':
      handleGameForceEnded(data);
      break;
    case 'SURRENDER':
      handleSurrenderMessage(data);
      break;
    case 'GUESSES_EXHAUSTED':
      handleGuessesExhausted(data);
      break;
    case 'SYSTEM_MESSAGE':
      handleSystemMessage(data);
      break;
    default:
      console.log('未知消息类型:', data.type);
  }
};

// 处理私有消息
const handlePrivateMessage = (data: any) => {
  console.log('收到私有消息:', data);

  switch (data.type) {
    case 'GAME_STATUS':
      updateGameStatus(data);
      break;
    case 'REMAINING_GUESSES':
      updateRemainingGuesses(data);
      break;
    case 'GAME_STATS':
      updateGameStats(data);
      break;
    case 'ROOM_INFO':
      updateRoomInfo(data);
      break;
    case 'ERROR':
      handleErrorMessage(data);
      break;
    case 'PONG':
      // 心跳响应，可以更新连接状态
      break;
    default:
      console.log('未知私有消息类型:', data.type);
  }
};

// 处理各种消息类型
const handlePlayerJoined = (data: any) => {
  console.log('玩家加入:', data);
  fetchRoomInfo(); // 重新获取房间信息
};

const handlePlayerLeft = (data: any) => {
  console.log('玩家离开:', data);
  fetchRoomInfo();
};

const handleGameStart = (data: any) => {
  console.log('游戏开始:', data);
  roomStatus.value = 'playing';
  gameEnded.value = false;
  fetchRoomInfo();
  gameLogs.value.push({
    playerId: 'system',
    playerName: '系统',
    message: '游戏开始！每个玩家有5次猜测机会。'
  });
};

const handleGuessMade = (data: any) => {
  console.log('玩家猜测:', data);
  const guessData = data.payload || data;
  const playerName = players.value.find(p => p.playerId === guessData.playerId)?.playerName || '未知玩家';

  if (guessData.correct) {
    gameLogs.value.push({
      playerId: guessData.playerId,
      playerName: playerName,
      message: `猜对了！正确答案是 ${guessData.guessedPerson?.name}`
    });
  } else {
    gameLogs.value.push({
      playerId: guessData.playerId,
      playerName: playerName,
      message: `猜错了，猜测的是 ${guessData.guessedPerson?.name}，剩余${guessData.remainingGuesses}次机会`
    });
  }

  fetchRoomInfo();
};

const handleGameEnd = (data: any) => {
  console.log('游戏结束:', data);
  gameEnded.value = true;
  const winnerName = players.value.find(p => p.playerId === data.payload?.winnerId)?.playerName || '未知玩家';
  gameResultMessage.value = `恭喜 ${winnerName} 获得胜利！`;
  roomStatus.value = 'finished';
  gameLogs.value.push({
    playerId: 'system',
    playerName: '系统',
    message: `游戏结束！获胜者：${winnerName}`
  });
};

const handleGameForceEnded = (data: any) => {
  console.log('游戏强制结束:', data);
  gameEnded.value = true;
  gameResultMessage.value = data.payload?.message || '游戏结束';
  roomStatus.value = 'finished';
};

const handleSurrenderMessage = (data: any) => {
  console.log('玩家投降:', data);
  const playerName = players.value.find(p => p.playerId === data.payload?.playerId)?.playerName || '未知玩家';
  gameLogs.value.push({
    playerId: data.payload?.playerId,
    playerName: playerName,
    message: '选择投降'
  });
};

const handleGuessesExhausted = (data: any) => {
  console.log('猜测次数用尽:', data);
  const playerName = players.value.find(p => p.playerId === data.payload?.playerId)?.playerName || '未知玩家';
  gameLogs.value.push({
    playerId: data.payload?.playerId,
    playerName: playerName,
    message: '猜测次数已用尽'
  });
};

const handleSystemMessage = (data: any) => {
  console.log('系统消息:', data);
  gameLogs.value.push({
    playerId: 'system',
    playerName: '系统',
    message: data.payload?.message || '系统消息'
  });
};

// 更新状态
const updateGameStatus = (data: any) => {
  if (data.status) {
    const rawStatus = data.status.status;
    const statusMap: Record<string, string> = {
      WAITING: 'waiting',
      PLAYING: 'playing',
      FINISHED: 'finished'
    };
    roomStatus.value = statusMap[rawStatus] || String(rawStatus).toLowerCase();
  }
};

const updateRemainingGuesses = (data: any) => {
  if (data.playerId === currentPlayerId.value) {
    remainingGuesses.value = data.remainingGuesses;
  }
};

const updateGameStats = (data: any) => {
  console.log('游戏统计更新:', data);
};

const updateRoomInfo = (data: any) => {
  console.log('房间信息更新:', data);
};

const handleErrorMessage = (data: any) => {
  console.error('收到错误消息:', data);
  alert(data.message || '发生错误');
};

// 获取房间信息
const fetchRoomInfo = async () => {
  try {
    const response = await gameRoomApi.getRoomInfo(roomId.value);

    if (response) {
      const room = response.room || response;
      const playersData = response.players || [];

      // 更新房间状态
      if (room && room.status) {
        const rawStatus = room.status;
        const statusMap: Record<string, string> = {
          WAITING: 'waiting',
          PLAYING: 'playing',
          FINISHED: 'finished',
          READY: 'waiting'
        };
        roomStatus.value = statusMap[rawStatus as string] || String(rawStatus).toLowerCase();
      }

      // 更新玩家列表
      if (Array.isArray(playersData)) {
        players.value = playersData.map((p: any, idx: number) => ({
          id: p.id ?? idx,
          playerId: p.playerId || `player_${idx}`,
          playerName: p.username ?? p.playerName ?? p.playerId ?? `玩家${idx + 1}`,
          isHost: p.isHost ?? (idx === 0),
          score: p.score ?? p.guessCount ?? 0
        }));
      }
    }
  } catch (error: any) {
    console.error('获取房间信息失败:', error);
  }
};

// 处理选择人物
const handleSelectPerson = (person: HistoricalPerson) => {
  console.log('选择的人物:', person);
};

// 处理猜测
const handleGuess = async (guessResult: any) => {
  try {
    const personId = guessResult.id || 0;
    if (!personId) {
      throw new Error('无效的人物ID');
    }

    // 通过 WebSocket 发送猜测消息
    sendWebSocketMessage('guess', {
      type: 'GUESS',
      playerId: currentPlayerId.value,
      personId: personId
    });

  } catch (error: any) {
    console.error('猜测失败:', error);
    alert('猜测失败: ' + error.message);
  }
};

// 处理投降
const handleSurrender = () => {
  showSurrenderModal.value = true;
};

// 确认投降
const confirmSurrender = async () => {
  try {
    // 通过 WebSocket 发送投降消息
    sendWebSocketMessage('surrender', {
      type: 'SURRENDER',
      playerId: currentPlayerId.value
    });

    showSurrenderModal.value = false;
  } catch (error: any) {
    console.error('投降失败:', error);
    alert('投降失败: ' + error.message);
  }
};

// 关闭投降模态框
const closeSurrenderModal = () => {
  showSurrenderModal.value = false;
};

// 处理胜利
const handleWin = (targetPerson: HistoricalPerson) => {
  // WebSocket 会自动处理，这里只需更新本地状态
  gameEnded.value = true;
  gameResultMessage.value = `恭喜 ${currentPlayerName.value} 猜对了！获得胜利！`;
};

// 处理重置
const handleReset = () => {
  // 多人模式下，重置需要重新开始游戏
  if (isHost.value && gameEnded.value) {
    sendWebSocketMessage('start', {
      type: 'START',
      playerId: currentPlayerId.value
    });
  }
};

// 开始游戏
const startGame = async () => {
  try {
    // 通过 WebSocket 发送开始游戏消息
    sendWebSocketMessage('start', {
      type: 'START',
      playerId: currentPlayerId.value
    });
  } catch (error: any) {
    console.error('开始游戏失败:', error);
    alert('开始游戏失败: ' + error.message);
  }
};

// 离开房间
const leaveRoom = async () => {
  try {
    // 通过 WebSocket 发送离开房间消息
    sendWebSocketMessage('leave', {
      type: 'LEAVE',
      playerId: currentPlayerId.value
    });

    // 断开 WebSocket 连接
    if (stompClient) {
      stompClient.disconnect();
    }

    localStorage.removeItem('currentPlayer');
    router.push('/multiplayer');
  } catch (error) {
    console.error('离开房间失败:', error);
    router.push('/multiplayer');
  }
};

// 初始化
onMounted(() => {
  // 从 localStorage 获取玩家信息
  const currentPlayer = JSON.parse(localStorage.getItem('currentPlayer') || '{}');
  currentPlayerId.value = currentPlayer.playerId || '';
  currentPlayerName.value = currentPlayer.username || currentPlayer.playerName || '';

  if (!currentPlayerId.value || !roomId.value) {
    alert('无效的房间信息');
    router.push('/multiplayer');
    return;
  }

  console.log('初始化多人游戏，房间ID:', roomId.value, '玩家ID:', currentPlayerId.value);

  // 获取初始房间信息
  fetchRoomInfo();

  // 连接 WebSocket
  connectWebSocket();
});

onUnmounted(() => {
  // 清理 WebSocket 连接
  if (stompClient) {
    stompClient.disconnect();
  }
});
</script>

<style scoped>
/* 整体容器样式 */
.historical-game-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  font-family: Arial, sans-serif;
}

/* 头部样式 */
.game-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e0e0e0;
}

.game-header h1 {
  margin: 0;
  color: #333;
  font-size: 2rem;
}

.connection-status {
  font-size: 1rem;
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot {
  height: 10px;
  width: 10px;
  border-radius: 50%;
  display: inline-block;
}

.status-dot.online {
  background-color: #4CAF50;
}

.status-dot.offline {
  background-color: #ccc;
}

/* 房间信息卡片 */
.room-info-section {
  margin-bottom: 30px;
}

.room-info-card {
  background-color: white;
  padding: 25px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 40px;
}

.room-status {
  flex: 1;
}

.room-status h2 {
  margin: 0 0 15px 0;
  color: #333;
  font-size: 1.5rem;
}

.status-badge {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 0.9rem;
  font-weight: bold;
  margin-left: 10px;
}

.status-badge.waiting {
  background-color: #ffc107;
  color: #000;
}

.status-badge.playing {
  background-color: #28a745;
  color: #fff;
}

.status-badge.finished {
  background-color: #dc3545;
  color: #fff;
}

.start-game-control {
  margin-top: 15px;
}

.start-btn {
  padding: 12px 24px;
  font-size: 1rem;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.start-btn:hover {
  background-color: #45a049;
}

.hint {
  font-size: 0.8rem;
  color: #666;
  margin-top: 5px;
}

/* 玩家信息 */
.players-info {
  flex: 1;
}

.players-info h3 {
  margin: 0 0 15px 0;
  color: #333;
  font-size: 1.3rem;
}

.players-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.player-card {
  background-color: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
  border-left: 4px solid #4CAF50;
}

.player-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.player-name {
  font-weight: bold;
  font-size: 1.1rem;
  color: #333;
}

.host-badge {
  background-color: #007bff;
  color: #fff;
  padding: 3px 8px;
  border-radius: 12px;
  font-size: 0.8rem;
  font-weight: bold;
}

.player-stats {
  color: #666;
  font-size: 0.95rem;
}

.no-players {
  text-align: center;
  color: #999;
  padding: 20px;
  font-style: italic;
  background-color: #f8f9fa;
  border-radius: 8px;
}

/* 等待对手回合 */
.waiting-turn {
  margin-top: 20px;
  background-color: #fff3cd;
  color: #856404;
  padding: 25px;
  border-radius: 8px;
  text-align: center;
  border: 1px solid #ffeeba;
}

.waiting-content h3 {
  margin-top: 0;
  color: #856404;
  font-size: 1.5rem;
}

.waiting-content p {
  color: #666;
  font-size: 1.1rem;
}

/* 实时战况 */
.battle-log {
  margin-top: 20px;
  background-color: #f9f9f9;
  padding: 20px;
  border-radius: 8px;
  max-height: 300px;
  overflow-y: auto;
}

.battle-log h3 {
  margin-top: 0;
  margin-bottom: 15px;
  color: #333;
}

.battle-log ul {
  list-style-type: none;
  padding: 0;
  margin: 0;
}

.battle-log li {
  margin-bottom: 10px;
  font-size: 0.9rem;
  border-bottom: 1px solid #eee;
  padding-bottom: 8px;
}

.my-log {
  color: #2196F3;
  font-weight: bold;
}

.log-player {
  font-weight: bold;
  color: #333;
}

/* 游戏结果提示 */
.game-result-alert {
  background-color: #d4edda;
  color: #155724;
  padding: 25px;
  border-radius: 8px;
  margin-top: 20px;
  text-align: center;
  border: 1px solid #c3e6cb;
}

.game-result-alert h3 {
  margin-top: 0;
  font-size: 1.5rem;
}

.result-message {
  margin: 15px 0;
  font-size: 1.1rem;
}

.result-actions {
  display: flex;
  justify-content: center;
  gap: 15px;
  margin-top: 20px;
}

.leave-btn {
  padding: 12px 24px;
  background-color: #6c757d;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1rem;
}

.leave-btn:hover {
  background-color: #5a6268;
}

/* 等待区域 */
.waiting-section {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.waiting-content {
  text-align: center;
  background-color: white;
  padding: 40px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  max-width: 500px;
  width: 100%;
}

.waiting-content h2 {
  margin-top: 0;
  color: #333;
  margin-bottom: 15px;
}

.waiting-content p {
  color: #666;
  margin-bottom: 10px;
}

/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal {
  background-color: #fff;
  padding: 25px;
  border-radius: 8px;
  width: 400px;
  max-width: 90%;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
}

.modal h3 {
  margin-top: 0;
  color: #333;
  margin-bottom: 15px;
}

.modal p {
  color: #666;
  margin-bottom: 20px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.confirm-btn {
  padding: 10px 20px;
  background-color: #dc3545;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1rem;
  font-weight: bold;
}

.confirm-btn:hover {
  background-color: #c82333;
}

.cancel-btn {
  padding: 10px 20px;
  background-color: #6c757d;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1rem;
}

.cancel-btn:hover {
  background-color: #5a6268;
}
</style>
