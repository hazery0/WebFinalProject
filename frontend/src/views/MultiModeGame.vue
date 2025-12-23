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
              <p v-if="gameHint" class="game-hint">{{ gameHint }}</p>
              <p v-else class="game-hint"></p>
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
                      <th>提示</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(guess, index) in formattedGuesses" :key="index">
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
                      <td class="hint-cell">{{ guess.hint || '' }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>
          </section>

          <!-- 游戏结束 -->
          <section v-if="gameEnded" class="game-over-section">
            <h2>🎉 游戏结束 🎉</h2>

            <!-- 显示获胜者 -->
            <div v-if="winnerInfo" class="winner-info">
              <h3 v-if="winnerInfo.playerId === playerId" class="winner-you">🎊 恭喜你获胜！</h3>
              <h3 v-else class="winner-other">🏆 获胜者：{{ winnerInfo.playerName }}</h3>
              <p v-if="winnerInfo.guessCount > 0">猜测次数：{{ winnerInfo.guessCount }}次</p>
            </div>
            <div v-else class="winner-info">
              <h3 class="no-winner">😢 没有获胜者</h3>
              <p>所有玩家都已投降或猜测次数用完</p>
            </div>

            <!-- 目标人物信息 -->
            <div class="target-person-info">
              <h4>目标人物信息：</h4>
              <p><strong>名称：</strong>{{ targetPerson.name }}</p>
              <p><strong>出生年份：</strong>{{ targetPerson.birthYear }}</p>
              <div class="tags-container">
                <span v-if="targetPerson.isLiterary" class="tag tag-match">文学家</span>
                <span v-if="targetPerson.isPolitical" class="tag tag-match">政治家</span>
                <span v-if="targetPerson.isThinker" class="tag tag-match">思想家</span>
                <span v-if="targetPerson.isScientist" class="tag tag-match">科学家</span>
              </div>
            </div>

            <!-- 重新开始游戏按钮 -->
            <div class="restart-control">
              <button @click="restartGame" class="restart-button">🔄 重新开始游戏</button>
            </div>
            <div class="restart-control">
              <p class="hint-text">等待重新开始游戏...</p>
            </div>

            <!-- 猜测历史 -->
            <div class="final-guesses">
              <h4>本局游戏猜测记录：</h4>
              <div class="final-guesses-list">
                <div v-for="(guess, index) in formattedGuesses" :key="index" class="final-guess-item">
                  <span class="player-name">{{ getPlayerName(guess.playerId) }}：</span>
                  <span :class="guess.isCorrect ? 'correct-guess' : 'incorrect-guess'">{{ guess.name }}</span>
                  <span v-if="guess.hint" class="hint-text">({{ guess.hint }})</span>
                </div>
              </div>
            </div>
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
const gameHint = ref(''); // 改名为 gameHint，避免泄露答案
const winnerInfo = ref<any>(null); // 获胜者信息

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

// 格式化猜测历史，用于显示
const formattedGuesses = computed(() => {
  return guesses.value.map(guess => {
    return {
      ...guess,
      hint: generateHint(guess)
    };
  });
});

// 生成提示信息
const generateHint = (guess: any) => {
  if (guess.isCorrect) return '猜对了！';

  let hint = '';
  const target = targetPerson.value;

  // 比较出生年份
  if (target.birthYear && guess.birthYear) {
    if (target.birthYear > guess.birthYear) {
      hint += '目标人物出生得更晚。 ';
    } else if (target.birthYear < guess.birthYear) {
      hint += '目标人物出生得更早。 ';
    } else {
      hint += '出生年份相同！ ';
    }
  }

  // 比较类别
  if (target.isLiterary === 1 && guess.isLiterary === 0) {
    hint += '目标人物是文学家。 ';
  }
  if (target.isPolitical === 1 && guess.isPolitical === 0) {
    hint += '目标人物是政治家。 ';
  }
  if (target.isThinker === 1 && guess.isThinker === 0) {
    hint += '目标人物是思想家。 ';
  }
  if (target.isScientist === 1 && guess.isScientist === 0) {
    hint += '目标人物是科学家。 ';
  }

  return hint.trim();
};

// --- 核心逻辑：处理 WebSocket 消息 ---
const handleMessage = (data: any) => {
  console.log('MultiMode收到消息:', data);

  if (data.type === 'ROOM_STATE' || data.type === 'ROOM_STATE_UPDATE') {
    const state = data.roomState;
    players.value = state.players || [];
    gameActive.value = state.gameActive || false;
    gameEnded.value = state.winnerId !== undefined && !state.gameActive;

    // 从服务器获取猜测记录
    if (state.guesses) {
      guesses.value = state.guesses.map((guess: any) => {
        // 确保猜测包含完整的人物信息
        const guessedPerson = guess.guessedPerson || guess.guess;
        return {
          playerId: guess.playerId,
          name: guessedPerson.name || guess.guess,
          birthYear: guessedPerson.birthYear,
          isLiterary: guessedPerson.isLiterary || 0,
          isPolitical: guessedPerson.isPolitical || 0,
          isThinker: guessedPerson.isThinker || 0,
          isScientist: guessedPerson.isScientist || 0,
          isCorrect: guess.isCorrect || false
        };
      });
    } else {
      guesses.value = [];
    }

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

    // 更新房主状态
    if (state.players) {
      const me = state.players.find((p: any) => p.id === playerId.value);
      if (me) {
        isRoomOwner.value = me.isRoomOwner || false;
      }
    }

    // 如果是游戏结束状态，获取获胜者信息
    if (state.winnerId) {
      const winner = state.players.find((p: any) => p.id === state.winnerId);
      if (winner) {
        winnerInfo.value = {
          playerId: winner.id,
          playerName: winner.name,
          guessCount: winner.guessCount || 0
        };
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
    gameHint.value = data.message || '猜猜这是哪位历史人物？';
    winnerInfo.value = null;

    // 如果有目标人物信息，更新它
    if (data.targetPerson) {
      targetPerson.value = data.targetPerson;
    }

    console.log('游戏开始!');
  } else if (data.type === 'GUESS_RESULT') {
    // 处理猜测结果 - 使用服务器返回的完整猜测数据
    if (data.guesses) {
      guesses.value = data.guesses.map((guess: any) => {
        const guessedPerson = guess.guessedPerson || guess.guess;
        return {
          playerId: guess.playerId,
          name: guessedPerson.name || guess.guess,
          birthYear: guessedPerson.birthYear,
          isLiterary: guessedPerson.isLiterary || 0,
          isPolitical: guessedPerson.isPolitical || 0,
          isThinker: guessedPerson.isThinker || 0,
          isScientist: guessedPerson.isScientist || 0,
          isCorrect: guess.isCorrect || false
        };
      });
    }

    // 更新提示信息
    if (data.hint) {
      gameHint.value = data.hint;
    }

    // 检查是否猜对
    if (data.isCorrect) {
      console.log(`${getPlayerName(data.playerId)} 猜对了！`);
      // 如果是当前玩家猜对了，更新游戏状态
      if (data.playerId === playerId.value) {
        gameWon.value = true;
      }
    }

    // 检查游戏是否结束
    if (data.gameEnded) {
      gameActive.value = false;
      gameEnded.value = true;

      // 设置获胜者信息
      if (data.winner) {
        const winner = players.value.find(p => p.id === data.winner);
        if (winner) {
          winnerInfo.value = {
            playerId: winner.id,
            playerName: winner.name,
            guessCount: winner.guessCount || 0
          };
        }
      }

      targetPerson.value = data.targetPerson || targetPerson.value;
    }
  } else if (data.type === 'SURRENDER_RESULT') {
    // 处理投降结果
    if (data.gameEnded) {
      gameActive.value = false;
      gameEnded.value = true;

      // 设置获胜者信息
      if (data.winner) {
        const winner = players.value.find(p => p.id === data.winner);
        if (winner) {
          winnerInfo.value = {
            playerId: winner.id,
            playerName: winner.name,
            guessCount: winner.guessCount || 0
          };
        }
      }

      targetPerson.value = data.targetPerson || targetPerson.value;
    }
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
  } else if (data.type === 'PLAYER_JOINED') {
    console.log('MultiMode收到消息:', data);

    if (data.type === 'PLAYER_JOINED') {
      // 处理玩家加入消息
      const newPlayer = {
        id: data.playerId,
        name: data.playerName,
        isRoomOwner: data.isRoomOwner || false
      };

      // 检查是否已存在该玩家
      const existingIndex = players.value.findIndex(p => p.id === data.playerId);
      if (existingIndex === -1) {
        players.value.push(newPlayer);
        console.log(`玩家 ${data.playerName} 已加入房间`);
      }

      return;
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

const getPlayerName = (pid: string, senderName?: string) => {
  const p = players.value.find(x => x.id === pid);
  // 如果找不到玩家，尝试使用 senderName
  if (!p && senderName) {
    return senderName;
  }
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

const startGame = () => {
  console.log('开始游戏按钮被点击');
  gameService.startGame();
};

const surrender = () => {
  if (confirm('确定要投降吗？')) gameService.surrender();
};

const restartGame = () => {
  gameService.startGame();
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
    const senderName = getPlayerName(data.playerId, data.senderName) || '未知玩家';
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
    gameHint.value = data.message || '猜猜这是哪位历史人物？';
    winnerInfo.value = null;

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
  display: flex;
  flex-direction: column;
  height: 100vh;
  padding: 16px;
  color: #333;
}

.loading-overlay {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: rgba(255, 255, 255, 0.9);
  font-size: 1.5em;
}

.loading-spinner {
  border: 8px solid #f3f3f3;
  border-top: 8px solid #3498db;
  border-radius: 50%;
  width: 60px;
  height: 60px;
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

.game-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  margin-bottom: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.room-info h1 {
  margin: 0;
  font-size: 1.8em;
  color: #2c3e50;
}

.room-status-bar {
  display: flex;
  gap: 10px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.badge {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 0.85em;
  font-weight: 600;
}

.room-id {
  background: #4CAF50;
  color: white;
}

.player-count {
  background: #2196F3;
  color: white;
}

.status {
  background: #e0e0e0;
  color: #555;
}

.status-active {
  background: #4CAF50;
  color: white;
}

.status-wait {
  background: #FF9800;
  color: white;
}

.status-disconnected {
  background: #f44336;
  color: white;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% {
    opacity: 1;
  }

  50% {
    opacity: 0.5;
  }

  100% {
    opacity: 1;
  }
}

.header-controls {
  display: flex;
  align-items: center;
  gap: 15px;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar-small {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: #3498db;
  color: white;
  border-radius: 50%;
  font-weight: bold;
}

.username {
  font-weight: 600;
  color: #2c3e50;
}

.win-badge {
  background: #FFD700;
  color: #000;
  font-weight: bold;
  padding: 4px 8px;
}

.surrender-button {
  padding: 8px 16px;
  background: #f44336;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
  transition: background 0.3s;
}

.surrender-button:hover {
  background: #d32f2f;
}

.game-layout {
  display: flex;
  flex: 1;
  gap: 16px;
  overflow: hidden;
}

.main-area {
  flex: 3;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.chat-sidebar {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 280px;
  max-width: 320px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.room-section,
.game-active-header,
.game-over-section {
  margin-bottom: 20px;
}

.waiting-header {
  text-align: center;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 10px;
  margin-bottom: 20px;
}

.hint-text {
  color: #666;
  font-size: 0.9em;
  margin-top: 8px;
}

.start-game-button {
  padding: 12px 24px;
  background: #4CAF50;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 1.1em;
  cursor: pointer;
  transition: background 0.3s;
  margin-top: 15px;
}

.start-game-button:hover {
  background: #388E3C;
}

.players-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 15px;
  margin-top: 15px;
}

.player-card {
  background: white;
  border-radius: 10px;
  padding: 15px;
  text-align: center;
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s;
  border: 2px solid transparent;
}

.player-card:hover {
  transform: translateY(-5px);
}

.player-card.is-me {
  border-color: #3498db;
  background: #e8f4fc;
}

.player-card.is-owner {
  border-color: #FF9800;
}

.player-avatar {
  width: 50px;
  height: 50px;
  background: #3498db;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.5em;
  font-weight: bold;
  margin: 0 auto 10px;
}

.player-name {
  font-weight: 600;
  margin-bottom: 8px;
}

.owner-tag,
.me-tag {
  font-size: 0.8em;
  padding: 2px 6px;
  border-radius: 10px;
  margin-left: 5px;
}

.owner-tag {
  background: #FF9800;
  color: white;
}

.me-tag {
  background: #3498db;
  color: white;
}

.ready-indicator {
  color: #4CAF50;
  font-weight: 600;
  font-size: 0.9em;
}

.no-players {
  text-align: center;
  padding: 30px;
  color: #666;
  font-style: italic;
}

.game-active-header {
  text-align: center;
  padding: 15px;
  background: linear-gradient(90deg, #4CAF50, #45a049);
  color: white;
  border-radius: 10px;
}

.game-hint {
  font-size: 1.1em;
  margin-top: 10px;
}

.search-section {
  position: relative;
  margin: 20px 0;
}

.search-container {
  display: flex;
  gap: 10px;
}

.search-input {
  flex: 1;
  padding: 12px 15px;
  border: 2px solid #ddd;
  border-radius: 8px;
  font-size: 1em;
  transition: border-color 0.3s;
}

.search-input:focus {
  outline: none;
  border-color: #3498db;
}

.search-input:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.search-button {
  padding: 12px 24px;
  background: #3498db;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  transition: background 0.3s;
}

.search-button:hover {
  background: #2980b9;
}

.search-results {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  max-height: 300px;
  overflow-y: auto;
  z-index: 100;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  margin-top: 5px;
}

.search-result-item {
  padding: 12px 15px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid #f0f0f0;
}

.search-result-item:hover {
  background: #f5f5f5;
}

.search-result-item:last-child {
  border-bottom: none;
}

.win-alert {
  background: #4CAF50;
  color: white;
  padding: 20px;
  border-radius: 10px;
  text-align: center;
  margin: 20px 0;
}

.result-section {
  margin-top: 20px;
}

.game-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.table-container {
  overflow-x: auto;
}

.guess-table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.05);
}

.guess-table th {
  background: #f8f9fa;
  padding: 15px;
  text-align: left;
  font-weight: 600;
  color: #495057;
  border-bottom: 2px solid #dee2e6;
}

.guess-table td {
  padding: 15px;
  border-bottom: 1px solid #dee2e6;
}

.guess-table tr:hover {
  background: #f8f9fa;
}

.player-tag {
  background: #e9ecef;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 0.9em;
}

.correct {
  color: #4CAF50;
  font-weight: bold;
}

.incorrect {
  color: #f44336;
}

.year-comparison {
  font-weight: bold;
  margin-left: 5px;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.tag {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 0.85em;
  font-weight: 600;
}

.tag-match {
  background: #4CAF50;
  color: white;
}

.tag-no-match {
  background: #f44336;
  color: white;
}

.hint-cell {
  max-width: 200px;
  font-size: 0.9em;
  color: #666;
}

.game-over-section {
  text-align: center;
  padding: 30px;
  background: #f8f9fa;
  border-radius: 12px;
}

.winner-info {
  margin: 20px 0;
  padding: 20px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.1);
}

.winner-you {
  color: #4CAF50;
}

.winner-other {
  color: #FF9800;
}

.no-winner {
  color: #666;
}

.target-person-info {
  background: white;
  padding: 20px;
  border-radius: 10px;
  margin: 20px 0;
  text-align: left;
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.1);
}

.restart-control {
  margin: 20px 0;
}

.restart-button {
  padding: 12px 30px;
  background: #3498db;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 1.1em;
  cursor: pointer;
  transition: background 0.3s;
}

.restart-button:hover {
  background: #2980b9;
}

.final-guesses {
  background: white;
  padding: 20px;
  border-radius: 10px;
  margin-top: 20px;
  text-align: left;
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.1);
}

.final-guesses-list {
  max-height: 200px;
  overflow-y: auto;
  margin-top: 15px;
}

.final-guess-item {
  padding: 10px;
  border-bottom: 1px solid #f0f0f0;
}

.final-guess-item:last-child {
  border-bottom: none;
}

.player-name {
  font-weight: 600;
  color: #2c3e50;
}

.correct-guess {
  color: #4CAF50;
  font-weight: bold;
}

.incorrect-guess {
  color: #f44336;
}

.chat-header {
  padding: 15px;
  background: #3498db;
  color: white;
  font-weight: bold;
  border-radius: 12px 12px 0 0;
  text-align: center;
}

.chat-messages {
  flex: 1;
  padding: 15px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.chat-item {
  display: flex;
  flex-direction: column;
  background: #f8f9fa;
  padding: 10px 15px;
  border-radius: 10px;
  word-break: break-word;
}

.chat-user {
  font-weight: 600;
  color: #3498db;
  font-size: 0.9em;
}

.chat-content {
  margin-top: 5px;
  color: #333;
}

.chat-input-area {
  display: flex;
  padding: 15px;
  gap: 10px;
  border-top: 1px solid #eee;
}

.chat-input-area input {
  flex: 1;
  padding: 10px 15px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 1em;
}

.chat-input-area input:focus {
  outline: none;
  border-color: #3498db;
}

.chat-input-area input:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.chat-input-area button {
  padding: 10px 20px;
  background: #4CAF50;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  transition: background 0.3s;
}

.chat-input-area button:hover {
  background: #388E3C;
}

.chat-input-area button:disabled {
  background: #ccc;
  cursor: not-allowed;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .game-layout {
    flex-direction: column;
  }

  .chat-sidebar {
    max-width: 100%;
  }
}

@media (max-width: 768px) {
  .game-header {
    flex-direction: column;
    gap: 15px;
    text-align: center;
  }

  .room-status-bar {
    justify-content: center;
  }

  .players-grid {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  }
}
</style>
