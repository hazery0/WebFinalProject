<template>
  <div class="room-select-container">
    <header class="game-header">
      <h1>历史人物猜猜乐</h1>
      <div class="login-status">
        <span @click="goToLogin" class="login-link">{{ isLoggedIn ? username : '未登录' }}</span>
      </div>
    </header>

    <div class="main-content">
      <div class="create-room-section">
        <h2>创建新房间</h2>
        <div class="input-group">
          <input v-model="playerName" placeholder="输入你的昵称" class="name-input" :disabled="isLoggedIn" />
          <button @click="handleCreateRoom" class="create-button">
            创建房间
          </button>
        </div>
      </div>

      <div class="divider">
        <span>或</span>
      </div>

      <div class="join-room-section">
        <h2>加入已有房间</h2>

        <div class="input-group">
          <input v-model="joinRoomId" placeholder="输入房间号" class="room-input" />
          <button @click="handleJoinRoom" class="join-button">
            加入房间
          </button>
        </div>

        <div class="room-list-section">
          <div class="room-list-header">
            <h3>可用房间列表</h3>
            <button @click="refreshRoomList" class="refresh-button">
              ↻ 刷新
            </button>
          </div>

          <div v-if="loadingRooms" class="loading-rooms">
            加载中...
          </div>

          <div v-else-if="availableRooms.length === 0" class="no-rooms">
            暂无可用房间
          </div>

          <div v-else class="room-list">
            <div v-for="room in availableRooms" :key="room.roomId" class="room-item" @click="selectRoom(room)">
              <div class="room-info">
                <div class="room-id">房间号: {{ room.roomId }}</div>
                <div class="room-creator">房主: {{ room.creatorName }}</div>
                <div class="room-players">玩家: {{ room.playerCount }}/8</div>
                <div class="room-status" :class="{ 'active': room.gameActive }">
                  {{ room.gameActive ? '游戏中' : '等待中' }}
                </div>
              </div>
              <button @click.stop="joinSelectedRoom(room.roomId)" class="join-item-button"
                :disabled="room.gameActive || room.playerCount >= 8">
                加入
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载弹窗 -->
    <div v-if="showLoading" class="loading-modal">
      <div class="loading-content">
        <div class="spinner"></div>
        <p>{{ loadingMessage }}</p>
      </div>
    </div>

    <!-- 错误提示 -->
    <div v-if="errorMessage" class="error-message">
      {{ errorMessage }}
      <button @click="errorMessage = ''" class="close-error">×</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { gameService } from '../services/gameService';

const router = useRouter();

// 状态管理
const isLoggedIn = ref(!!localStorage.getItem('token'));
const username = ref(localStorage.getItem('username') || '');
const playerName = ref(username.value || '');
const joinRoomId = ref('');
const availableRooms = ref<any[]>([]);
const loadingRooms = ref(false);
const showLoading = ref(false);
const loadingMessage = ref('');
const errorMessage = ref('');

// 获取用户名
if (isLoggedIn.value && !playerName.value) {
  playerName.value = username.value;
}

// 跳转到登录页面
const goToLogin = () => {
  router.push('/login');
};

// 创建房间
const handleCreateRoom = async () => {
  if (!playerName.value.trim() && !isLoggedIn.value) {
    alert('请输入昵称');
    return;
  }

  try {
    // 1. 设置玩家信息
    gameService.setPlayerInfo(playerName.value);

    // 2. 调用创建房间请求
    const newRoomId = await gameService.createRoom();

    // 3. 成功后直接跳转到游戏页面，带上 roomId
    router.push({
      path: '/multi-game',
      query: { roomId: newRoomId }
    });
  } catch (error) {
    console.error('创建房间失败:', error);
    alert('创建房间失败，请重试');
  }
};

// 加入房间
const handleJoinRoom = async () => {
  if (!joinRoomId.value.trim()) {
    errorMessage.value = '请输入房间号';
    return;
  }

  if (!playerName.value.trim()) {
    errorMessage.value = '请输入昵称';
    return;
  }

  showLoading.value = true;
  loadingMessage.value = '加入房间中...';

  try {
    // 设置玩家信息
    gameService.setPlayerInfo(playerName.value);

    // 加入房间
    await gameService.joinRoom(joinRoomId.value);

    // 跳转到游戏页面
    router.push({
      path: '/multi-game',
      query: { roomId: joinRoomId.value }
    });
  } catch (error) {
    errorMessage.value = '加入房间失败: ' + (error as Error).message;
  } finally {
    showLoading.value = false;
  }
};

// 选择房间（点击房间项）
const selectRoom = (room: any) => {
  joinRoomId.value = room.roomId;
};

// 加入选中的房间
const joinSelectedRoom = (roomId: string) => {
  joinRoomId.value = roomId;
  handleJoinRoom();
};

// 刷新房间列表
const refreshRoomList = () => {
  loadingRooms.value = true;
  gameService.getRooms();
};

// 处理房间列表更新
const handleRoomListUpdate = (message: any) => {
  if (message.type === 'ROOM_LIST' || message.type === 'ROOM_LIST_UPDATE') {
    availableRooms.value = message.rooms || [];
    loadingRooms.value = false;
  }
};

// 处理游戏消息
const handleGameMessage = (message: any) => {
  if (message.type === 'ROOM_LIST') {
    availableRooms.value = message.rooms || [];
    loadingRooms.value = false;
  }
};

// 生命周期
onMounted(() => {
  // 订阅事件
  gameService.on('roomListUpdate', handleRoomListUpdate);
  gameService.on('message', handleGameMessage);

  // 初始获取房间列表
  refreshRoomList();
});

onUnmounted(() => {
  // 取消订阅
  gameService.off('roomListUpdate', handleRoomListUpdate);
  gameService.off('message', handleGameMessage);
});
</script>

<style scoped>
.room-select-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  font-family: Arial, sans-serif;
}

.game-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 40px;
  padding-bottom: 10px;
  border-bottom: 2px solid #4CAF50;
}

.game-header h1 {
  margin: 0;
  color: #333;
  font-size: 2rem;
}

.login-status {
  font-size: 1rem;
}

.login-link {
  color: #4CAF50;
  cursor: pointer;
  text-decoration: underline;
}

.login-link:hover {
  color: #45a049;
}

.main-content {
  display: flex;
  flex-direction: column;
  gap: 30px;
}

.create-room-section,
.join-room-section {
  background-color: white;
  padding: 30px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.create-room-section h2,
.join-room-section h2 {
  margin-top: 0;
  margin-bottom: 20px;
  color: #333;
  text-align: center;
}

.input-group {
  display: flex;
  gap: 10px;
  max-width: 500px;
  margin: 0 auto;
}

.name-input,
.room-input {
  flex: 1;
  padding: 12px;
  font-size: 1rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.name-input:disabled {
  background-color: #f5f5f5;
  cursor: not-allowed;
}

.create-button,
.join-button {
  padding: 12px 30px;
  font-size: 1rem;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.create-button:hover {
  background-color: #45a049;
}

.join-button {
  background-color: #2196F3;
}

.join-button:hover {
  background-color: #1976D2;
}

.divider {
  display: flex;
  align-items: center;
  text-align: center;
  margin: 10px 0;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  border-bottom: 1px solid #ddd;
}

.divider span {
  padding: 0 20px;
  color: #666;
  font-weight: bold;
}

.room-list-section {
  margin-top: 30px;
}

.room-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.room-list-header h3 {
  margin: 0;
  color: #333;
}

.refresh-button {
  padding: 8px 16px;
  background-color: #6c757d;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.refresh-button:hover {
  background-color: #5a6268;
}

.loading-rooms,
.no-rooms {
  text-align: center;
  padding: 40px;
  color: #666;
  font-style: italic;
}

.room-list {
  display: grid;
  gap: 15px;
}

.room-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  background-color: #f8f9fa;
  border: 1px solid #dee2e6;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.room-item:hover {
  background-color: #e9ecef;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.room-info {
  flex: 1;
}

.room-id {
  font-weight: bold;
  color: #333;
  margin-bottom: 5px;
}

.room-creator,
.room-players {
  color: #666;
  font-size: 0.9rem;
  margin-bottom: 3px;
}

.room-status {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 0.8rem;
  font-weight: bold;
}

.room-status.active {
  background-color: #dc3545;
  color: white;
}

.room-status:not(.active) {
  background-color: #28a745;
  color: white;
}

.join-item-button {
  padding: 8px 20px;
  background-color: #2196F3;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.join-item-button:hover:not(:disabled) {
  background-color: #1976D2;
}

.join-item-button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.loading-modal {
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

.loading-content {
  background-color: white;
  padding: 30px;
  border-radius: 10px;
  text-align: center;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #4CAF50;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 15px;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }

  100% {
    transform: rotate(360deg);
  }
}

.error-message {
  position: fixed;
  bottom: 20px;
  right: 20px;
  background-color: #dc3545;
  color: white;
  padding: 15px 20px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
  animation: slideIn 0.3s ease-out;
}

.close-error {
  background: none;
  border: none;
  color: white;
  font-size: 1.5rem;
  cursor: pointer;
  padding: 0;
  line-height: 1;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }

  to {
    transform: translateX(0);
    opacity: 1;
  }
}
</style>
