<template>
  <div class="historical-game-container">
    <!-- 加载状态 -->
    <div v-if="isLoading" class="loading-overlay">
      <div class="loading-spinner"></div>
      <p v-if="!isConnected">正在连接服务器...</p>
      <p v-else>正在加入房间...</p>
    </div>

    <!-- 主内容 -->
    <div v-else>
      <header class="game-header">
        <div class="room-info">
          <h1>多人历史人物竞猜</h1>
          <div class="room-status-bar">
            <span class="badge room-id">房间号: {{ roomId }}</span>
            <span class="badge player-count">玩家: {{ players.length }}/8</span>
            <span class="badge status" :class="gameActive ? 'status-active' : 'status-wait'">
              {{ gameActive ? '游戏中' : '等待中' }}
            </span>
            <span v-if="!isConnected" class="badge status-disconnected">连接中...</span>
          </div>
        </div>

        <div class="header-controls">
          <div class="user-profile">
            <span class="avatar-small">{{ username.charAt(0).toUpperCase() }}</span>
            <span class="username">{{ username }}</span>
            <span v-if="gameWon" class="badge win-badge">获胜!</span>
          </div>
          <button @click="leaveRoom" class="surrender-button">离开房间</button>
        </div>
      </header>

      <div class="game-layout">
        <div class="main-area">
          <!-- 等待区域 -->
          <section class="room-section" v-if="!gameActive && !gameEnded">
            <div class="waiting-header">
              <h2>等待玩家加入</h2>
              <div>
                <p class="hint-text">所有玩家都可以开始游戏（测试模式）</p>
                <p>当前房主: {{ getRoomOwnerName() }}</p>
                <div class="start-game-control">
                  <button @click="startGame" class="start-game-button">
                    🎮 开始游戏
                  </button>
                </div>
              </div>
            </div>

            <!-- 玩家列表 -->
            <h3>房间玩家 ({{ players.length }}/8)</h3>
            <div v-if="players.length > 0" class="players-grid">
              <div v-for="player in players" :key="player.id" class="player-card"
                :class="{ 'is-me': player.id === playerId, 'is-owner': player.isRoomOwner }">
                <div class="player-avatar">{{ player.name.charAt(0).toUpperCase() }}</div>
                <div class="player-name">
                  {{ player.name }}
                  <span v-if="player.isRoomOwner" class="owner-tag">房主</span>
                  <span v-if="player.id === playerId" class="me-tag">我</span>
                </div>
                <div v-if="player.isReady" class="ready-indicator">✓ 准备</div>
              </div>
            </div>
            <div v-else class="no-players">
              <p>暂无玩家，等待玩家加入...</p>
            </div>
          </section>

          <!-- 游戏进行中 -->
          <section v-if="gameActive && !gameEnded">
            <div class="game-active-header">
              <h2>🎮 游戏进行中</h2>
              <p v-if="targetHint" class="game-hint">{{ targetHint }}</p>
              <p v-else class="game-hint">猜猜这是哪位历史人物？</p>
            </div>

            <!-- 搜索区域 -->
            <div class="search-section">
              <div class="search-container">
                <input v-model="searchQuery" @input="handleSearch" @keyup.enter="handleSearch" placeholder="输入历史人物名称..."
                  class="search-input" :disabled="!canGuess" />
                <button @click="handleSearch" class="search-button">搜索</button>
              </div>

              <!-- 搜索结果下拉列表 -->
              <div v-if="searchResults.length > 0 && searchQuery" class="search-results">
                <div v-for="person in searchResults" :key="person.id" @click="selectPerson(person)"
                  class="search-result-item">
                  {{ person.name }}
                </div>
              </div>
            </div>

            <!-- 获胜提示 -->
            <div v-if="gameWon" class="win-alert">
              <h3>恭喜你猜对了！</h3>
              <p>目标人物是：{{ targetPerson.name }}</p>
            </div>

            <!-- 猜测历史表格 -->
            <section class="result-section">
              <div class="game-controls">
                <h2>猜测历史</h2>
                <button v-if="gameActive" @click="surrender" class="surrender-button">投降</button>
              </div>

              <div class="table-container">
                <table class="guess-table">
                  <thead>
                    <tr>
                      <th>玩家</th>
                      <th>名称</th>
                      <th>出生年份</th>
                      <th>标签</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(guess, index) in guesses" :key="index">
                      <td><span class="player-tag">{{ getPlayerName(guess.playerId) }}</span></td>
                      <td :class="guess.isCorrect ? 'correct' : 'incorrect'">{{ guess.name }}</td>
                      <td>
                        {{ guess.birthYear }}
                        <span v-if="guess.birthYear > targetPerson.birthYear" class="year-comparison">↓</span>
                        <span v-else-if="guess.birthYear < targetPerson.birthYear" class="year-comparison">↑</span>
                      </td>
                      <td class="tags-container">
                        <span v-if="guess.isLiterary"
                          :class="['tag', { 'tag-match': guess.isLiterary === targetPerson.isLiterary, 'tag-no-match': guess.isLiterary !== targetPerson.isLiterary }]">
                          文学家
                        </span>
                        <span v-if="guess.isPolitical"
                          :class="['tag', { 'tag-match': guess.isPolitical === targetPerson.isPolitical, 'tag-no-match': guess.isPolitical !== targetPerson.isPolitical }]">
                          政治家
                        </span>
                        <span v-if="guess.isThinker"
                          :class="['tag', { 'tag-match': guess.isThinker === targetPerson.isThinker, 'tag-no-match': guess.isThinker !== targetPerson.isThinker }]">
                          思想家
                        </span>
                        <span v-if="guess.isScientist"
                          :class="['tag', { 'tag-match': guess.isScientist === targetPerson.isScientist, 'tag-no-match': guess.isScientist !== targetPerson.isScientist }]">
                          科学家
                        </span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>
          </section>

          <!-- 游戏结束 -->
          <section v-if="gameEnded" class="game-over-section">
            <h2>游戏结束</h2>
            <p>目标人物是：{{ targetPerson.name }}</p>
            <p>出生年份：{{ targetPerson.birthYear }}</p>
            <div class="tags-container">
              <span v-if="targetPerson.isLiterary" class="tag tag-match">文学家</span>
              <span v-if="targetPerson.isPolitical" class="tag tag-match">政治家</span>
              <span v-if="targetPerson.isThinker" class="tag tag-match">思想家</span>
              <span v-if="targetPerson.isScientist" class="tag tag-match">科学家</span>
            </div>
            <button @click="resetGame" class="reset-button">返回房间</button>
          </section>
        </div>

        <aside class="chat-sidebar">
          <div class="chat-header">房间聊天</div>
          <div class="chat-messages" ref="chatScroll">
            <div v-for="(msg, i) in chatMessages" :key="i" class="chat-item">
              <span class="chat-user">{{ msg.sender }}:</span>
              <span class="chat-content">{{ msg.content }}</span>
            </div>
          </div>
          <div class="chat-input-area">
            <input v-model="chatInput" @keyup.enter="sendChatMessage" placeholder="按回车发送..." :disabled="!hasJoined" />
            <button @click="sendChatMessage" :disabled="!hasJoined">发送</button>
          </div>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { gameService } from '../services/gameService';
import { webSocketService } from '../services/websocket';
import { historicalPersonApi } from '../services/api';

const route = useRoute();
const router = useRouter();

// --- 基础状态 ---
const roomId = ref(route.query.roomId as string || '');
const playerId = ref(localStorage.getItem('tempPlayerId') || '');
const username = ref(localStorage.getItem('username') || '匿名玩家');

// --- 游戏数据 ---
const players = ref<any[]>([]);
const gameActive = ref(false);
const gameEnded = ref(false);
const guesses = ref<any[]>([]);
const canGuess = ref(true);

// --- 房主相关状态 ---
const isRoomOwner = ref(false);
const roomOwnerName = ref('');

// --- 目标人物（多人共用）---
const targetPerson = ref<any>({
  id: 0,
  name: '',
  birthYear: 0,
  isLiterary: 0,
  isPolitical: 0,
  isThinker: 0,
  isScientist: 0
});

// --- 提示信息 ---
const targetHint = ref('');

// --- 搜索与交互 ---
const searchQuery = ref('');
const searchResults = ref<any[]>([]);
const chatInput = ref('');
const chatMessages = ref<any[]>([]);
const chatScroll = ref<HTMLElement | null>(null);

// --- 连接状态 ---
const isConnected = ref(false);
const isLoading = ref(true);
const hasJoined = ref(false);

// --- 处理器引用 ---
const messageHandlerRef = ref<Function | null>(null);
const chatHandlerRef = ref<Function | null>(null);
const errorHandlerRef = ref<Function | null>(null);
const connectionCheckInterval = ref<any>(null);

// 获取房主名称
const getRoomOwnerName = () => {
  if (roomOwnerName.value) return roomOwnerName.value;

  const owner = players.value.find(p => p.isRoomOwner);
  if (owner) {
    roomOwnerName.value = owner.name;
    return owner.name;
  }
  return '未知';
};

// 检查是否获胜
const gameWon = computed(() => {
  if (!targetPerson.value.name) return false;
  const myLastGuess = guesses.value
    .filter(g => g.playerId === playerId.value)
    .slice(-1)[0];
  return myLastGuess?.isCorrect || false;
});

// --- 核心逻辑：处理 WebSocket 消息 ---
const handleMessage = (data: any) => {
  console.log('MultiMode收到消息:', data);

  if (data.type === 'ROOM_STATE' || data.type === 'ROOM_STATE_UPDATE') {
    const state = data.roomState;
    players.value = state.players || [];
    gameActive.value = state.gameActive || false;
    guesses.value = state.guesses || [];

    // 更新目标人物（如果服务器传了）
    if (state.targetPerson) {
      targetPerson.value = state.targetPerson;
    }

    // 更新 playerId（确保使用正确的ID）
    if (state.players) {
      const me = state.players.find((p: any) => p.name === username.value);
      if (me && me.id) {
        playerId.value = me.id;
        localStorage.setItem('tempPlayerId', me.id);
      }
    }

    isLoading.value = false;
    hasJoined.value = true;
  } else if (data.type === 'GAME_STARTED') {
    // 处理游戏开始消息
    gameActive.value = true;
    gameEnded.value = false;
    canGuess.value = true;
    guesses.value = [];
    searchQuery.value = '';
    searchResults.value = [];

    // 如果有目标人物信息，更新它
    if (data.targetPerson) {
      targetPerson.value = data.targetPerson;
    }

    console.log('游戏开始!');
  } else if (data.type === 'TARGET_PERSON_HINT') {
    // 处理目标人物提示
    console.log('收到目标人物提示:', data.hint);
    targetHint.value = data.hint || '';
  } else if (data.type === 'GUESS_RESULT') {
    // 处理猜测结果
    guesses.value = data.guesses || [];

    // 检查是否猜对
    if (data.isCorrect) {
      console.log(`${getPlayerName(data.playerId)} 猜对了！`);
      // 如果是当前玩家猜对了，更新游戏状态
      if (data.playerId === playerId.value) {
        gameWon.value = true;
      }
    }
  } else if (data.type === 'GAME_OVER') {
    gameActive.value = false;
    gameEnded.value = true;
    alert(`游戏结束！获胜者: ${data.winnerName}`);
  } else if (data.type === 'PLAYER_LEFT' || data.type === 'ROOM_DISSOLVED') {
    if (data.message === '房间已解散') {
      alert('房间已被解散，将返回房间选择页面');
      router.push('/room-select');
    }
  } else if (data.type === 'JOIN_FAILED') {
    alert(`加入房间失败: ${data.message}`);
    router.push('/room-select');
  } else if (data.type === 'OWNER_TRANSFERRED') {
    console.log(`房主已转移给: ${data.newOwnerName}`);
    roomOwnerName.value = data.newOwnerName || '';

    // 更新本地房主状态
    if (data.newOwnerId === playerId.value) {
      isRoomOwner.value = true;
      alert('你已成为新房主！');
    } else {
      isRoomOwner.value = false;
    }
  }
};

// --- 辅助方法 ---
const scrollToBottom = () => {
  nextTick(() => {
    if (chatScroll.value) {
      chatScroll.value.scrollTop = chatScroll.value.scrollHeight;
    }
  });
};

const getPlayerName = (pid: string) => {
  const p = players.value.find(x => x.id === pid);
  return p ? p.name : '未知玩家';
};

// --- 用户操作 ---
const handleSearch = async () => {
  if (!searchQuery.value.trim()) {
    searchResults.value = [];
    return;
  }
  try {
    const res = await historicalPersonApi.search(searchQuery.value);
    searchResults.value = res;
  } catch (e) {
    console.error('搜索失败:', e);
  }
};

const selectPerson = async (person: any) => {
  if (!canGuess.value || !gameActive.value) {
    alert('游戏尚未开始或不能猜测！');
    return;
  }

  try {
    // 发送猜测到服务器
    gameService.sendGuess(person);
    searchQuery.value = '';
    searchResults.value = [];
  } catch (error) {
    console.error('发送猜测失败:', error);
    alert('发送猜测失败，请重试！');
  }
};

const sendChatMessage = () => {
  if (!chatInput.value.trim()) return;
  gameService.sendChatMessage(chatInput.value);
  chatInput.value = '';
};

const startGame = () => gameService.startGame();

const surrender = () => {
  if (confirm('确定要投降吗？')) gameService.surrender();
};

const resetGame = () => {
  gameActive.value = false;
  gameEnded.value = false;
  guesses.value = [];
  searchQuery.value = '';
  searchResults.value = [];
};

// --- 连接管理 ---
const setupConnection = async (): Promise<boolean> => {
  return new Promise((resolve) => {
    let attempts = 0;
    const maxAttempts = 30;

    const checkConnection = () => {
      attempts++;
      if (webSocketService.isConnected) {
        isConnected.value = true;
        resolve(true);
      } else if (attempts >= maxAttempts) {
        console.error('连接超时');
        resolve(false);
      } else {
        setTimeout(checkConnection, 100);
      }
    };

    checkConnection();
  });
};

// --- 重新加入房间 ---
const rejoinRoom = async (): Promise<boolean> => {
  try {
    console.log('尝试重新加入房间:', roomId.value);
    isLoading.value = true;

    // 等待连接建立
    const connected = await setupConnection();
    if (!connected) {
      throw new Error('WebSocket连接失败');
    }

    // 重新加入房间
    await gameService.joinRoom(roomId.value, true);

    console.log('重新加入房间成功');
    return true;
  } catch (error: any) {
    console.error('重新加入房间失败:', error);

    if (error.message?.includes('不存在') || error.message?.includes('已满')) {
      alert(`房间不可用: ${error.message}`);
    } else {
      alert('连接房间失败，请检查网络后重试');
    }

    router.push('/room-select');
    return false;
  }
};

// --- 统一的退出方法 ---
const performLeave = () => {
  gameService.leaveRoom();
  router.push('/room-select');
};

// 处理页面内的"离开"按钮
const leaveRoom = () => {
  if (isRoomOwner.value) {
    if (confirm('你是房主，离开将解散房间，确定吗？')) {
      performLeave();
    }
  } else {
    performLeave();
  }
};

// --- 生命周期 ---
onMounted(async () => {
  window.addEventListener('beforeunload', performLeave);

  // 检查房间ID
  if (!roomId.value) {
    alert('房间ID无效');
    router.push('/room-select');
    return;
  }

  // 显示加载状态
  isLoading.value = true;

  // 定义处理器函数
  const messageHandler = (data: any) => {
    handleMessage(data);
  };

  const chatHandler = (data: any) => {
    console.log('收到聊天消息事件:', data);
    const senderName = getPlayerName(data.playerId) || data.senderName || '未知玩家';
    chatMessages.value.push({
      sender: senderName,
      content: data.message,
      timestamp: data.timestamp
    });
    scrollToBottom();
  };

  const errorHandler = (error: any) => {
    console.error('GameService 错误:', error);
    if (error.message?.includes('房间') || error.message?.includes('加入')) {
      alert(error.message);
      router.push('/room-select');
    }
  };

  // 保存引用
  messageHandlerRef.value = messageHandler;
  chatHandlerRef.value = chatHandler;
  errorHandlerRef.value = errorHandler;

  // 监听事件
  gameService.on('message', messageHandler);
  gameService.on('chatMessage', chatHandler);
  gameService.on('roomMessage', messageHandler);
  gameService.on('error', errorHandler);
  gameService.on('gameStart', (data: any) => {
    console.log('收到 gameStart 事件:', data);
    gameActive.value = true;
    gameEnded.value = false;
    canGuess.value = true;
    guesses.value = [];

    if (data.targetPerson) {
      targetPerson.value = data.targetPerson;
    }
  });
  gameService.on('roomRejoined', (roomId) => {
    console.log('成功重新加入房间:', roomId);
  });
  gameService.on('ownerTransferred', (data: any) => {
    console.log('房主变更事件:', data);
    if (data.newOwnerId === playerId.value) {
      isRoomOwner.value = true;
      alert('你已成为新房主！');
    } else {
      isRoomOwner.value = false;
    }
    roomOwnerName.value = data.newOwnerName || '';
  });

  // 初始化连接
  gameService.connect();
  gameService.setPlayerInfo(username.value);

  // 重新加入房间
  const success = await rejoinRoom();
  if (!success) {
    return;
  }

  // 设置连接状态检查
  connectionCheckInterval.value = setInterval(() => {
    if (!webSocketService.isConnected && !isLoading.value) {
      console.log('检测到连接断开，尝试重新连接...');
      isConnected.value = false;
      isLoading.value = true;

      setTimeout(async () => {
        gameService.connect();
        await rejoinRoom();
      }, 1000);
    }
  }, 3000);
});

onUnmounted(() => {
  // 清理定时器
  if (connectionCheckInterval.value) {
    clearInterval(connectionCheckInterval.value);
  }

  // 移除事件监听器
  if (messageHandlerRef.value) {
    gameService.off('message', messageHandlerRef.value);
  }
  if (chatHandlerRef.value) {
    gameService.off('chatMessage', chatHandlerRef.value);
  }
  if (errorHandlerRef.value) {
    gameService.off('error', errorHandlerRef.value);
  }

  // 移除页面卸载监听
  window.removeEventListener('beforeunload', performLeave);

  // 离开房间
  if (hasJoined.value) {
    gameService.leaveRoom();
  }
});
</script>

<style scoped>
.historical-game-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  font-family: sans-serif;
  background-color: #f8f9fa;
  min-height: 100vh;
  position: relative;
}

.game-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 2px solid #eee;
  padding-bottom: 20px;
  margin-bottom: 20px;
}

.room-status-bar {
  display: flex;
  gap: 10px;
  margin-top: 5px;
  flex-wrap: wrap;
}

.badge {
  padding: 4px 10px;
  border-radius: 15px;
  font-size: 0.8rem;
  background: #ddd;
}

.status-active {
  background: #d4edda;
  color: #155724;
}

.status-wait {
  background: #fff3cd;
  color: #856404;
}

.status-disconnected {
  background: #f8d7da;
  color: #721c24;
}

.win-badge {
  background: #4CAF50;
  color: white;
  margin-left: 8px;
}

.header-controls {
  display: flex;
  align-items: center;
  gap: 15px;
}

.avatar-small {
  width: 30px;
  height: 30px;
  background: #4CAF50;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
}

.game-layout {
  display: grid;
  grid-template-columns: 1fr 300px;
  gap: 20px;
}

.players-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: 10px;
  margin-top: 20px;
}

.player-card {
  background: white;
  padding: 10px;
  border-radius: 8px;
  text-align: center;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s;
}

.player-card:hover {
  transform: translateY(-2px);
}

.is-me {
  border: 2px solid #4CAF50;
}

.owner-tag {
  font-size: 0.6rem;
  background: #ff9800;
  color: white;
  padding: 1px 4px;
  border-radius: 3px;
  margin-left: 4px;
}

.no-players {
  text-align: center;
  padding: 40px;
  color: #666;
  font-style: italic;
}

/* 游戏进行中标题样式 */
.game-active-header {
  text-align: center;
  margin-bottom: 20px;
  padding: 15px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 10px;
}

.game-active-header h2 {
  margin: 0 0 10px 0;
  font-size: 1.5rem;
}

.game-hint {
  margin: 0;
  font-size: 1rem;
  opacity: 0.9;
}

/* 搜索区域 */
.search-section {
  position: relative;
  margin-bottom: 20px;
}

.search-container {
  display: flex;
  gap: 10px;
  max-width: 600px;
  margin: 0 auto 10px;
}

.search-input {
  flex: 1;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 16px;
}

.search-button {
  padding: 12px 24px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
}

.search-button:hover {
  background-color: #45a049;
}

.search-results {
  position: absolute;
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  max-width: 600px;
  width: 100%;
  background: white;
  border: 1px solid #ddd;
  border-radius: 0 0 6px 6px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
  z-index: 10;
  max-height: 300px;
  overflow-y: auto;
}

.search-result-item {
  padding: 12px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
}

.search-result-item:hover {
  background: #f5f5f5;
}

/* 胜利提示 */
.win-alert {
  background-color: #d4edda;
  color: #155724;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  text-align: center;
  border: 1px solid #c3e6cb;
}

.win-alert h3 {
  margin-top: 0;
  font-size: 1.5rem;
}

.table-container {
  background: white;
  padding: 15px;
  border-radius: 12px;
  overflow-x: auto;
}

.guess-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 800px;
}

.guess-table th {
  text-align: left;
  border-bottom: 2px solid #eee;
  padding: 10px;
  background: #f8f9fa;
  font-weight: 600;
}

.guess-table td {
  padding: 10px;
  border-bottom: 1px solid #eee;
}

.correct {
  color: #2ecc71;
  font-weight: bold;
}

.incorrect {
  color: #e74c3c;
}

.player-tag {
  background: #e3f2fd;
  color: #1976d2;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 0.85rem;
}

/* 出生年份比较 */
.year-comparison {
  margin-left: 8px;
  font-weight: bold;
  font-size: 1.2rem;
}

/* 标签样式 */
.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 0.85rem;
  font-weight: bold;
}

.tag-match {
  background-color: #d4edda;
  color: #155724;
}

.tag-no-match {
  background-color: #e2e3e5;
  color: #6c757d;
}

/* 游戏结束区域 */
.game-over-section {
  background: white;
  padding: 30px;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.reset-button {
  margin-top: 20px;
  padding: 10px 20px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1rem;
}

.reset-button:hover {
  background-color: #45a049;
}

.chat-sidebar {
  background: white;
  border-radius: 12px;
  height: 600px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.chat-header {
  padding: 15px;
  border-bottom: 1px solid #eee;
  font-weight: bold;
  background: #f8f9fa;
  border-radius: 12px 12px 0 0;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 15px;
}

.chat-item {
  margin-bottom: 10px;
  line-height: 1.4;
}

.chat-user {
  font-weight: bold;
  color: #3498db;
  margin-right: 5px;
}

.chat-content {
  color: #333;
}

.chat-input-area {
  padding: 15px;
  display: flex;
  gap: 10px;
  border-top: 1px solid #eee;
  background: #f8f9fa;
  border-radius: 0 0 12px 12px;
}

.chat-input-area input {
  flex: 1;
  padding: 10px 15px;
  border: 1px solid #ddd;
  border-radius: 20px;
  outline: none;
  font-size: 14px;
}

.chat-input-area input:disabled {
  background: #f0f0f0;
  cursor: not-allowed;
}

.chat-input-area button {
  background: #4CAF50;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 20px;
  cursor: pointer;
  font-weight: 600;
}

.chat-input-area button:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.chat-input-area button:hover:not(:disabled) {
  background: #45a049;
}

.surrender-button {
  background: #ff4757;
  color: white;
  border: none;
  padding: 8px 15px;
  border-radius: 5px;
  cursor: pointer;
  font-weight: 600;
}

.surrender-button:hover {
  background: #ff3742;
}

.hint-text {
  color: #666;
  font-style: italic;
}

/* 开始游戏按钮样式 */
.start-game-control {
  margin: 20px 0;
}

.start-game-button {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 12px 30px;
  border-radius: 8px;
  font-size: 1.1rem;
  font-weight: bold;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.start-game-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(102, 126, 234, 0.3);
}

/* 加载状态 */
.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.9);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 5px solid #f3f3f3;
  border-top: 5px solid #3498db;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 20px;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }

  100% {
    transform: rotate(360deg);
  }
}
</style>
