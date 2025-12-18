<template>
  <div class="multiplayer-mode-container">
    <h1 class="game-title">多人模式</h1>
    <!-- 添加错误信息显示 -->
    <div v-if="errorMessage" class="error-message">
      {{ errorMessage }}
    </div>

    <div class="multiplayer-buttons">
      <button class="multiplayer-button" @click="createRoom">
        创建房间
      </button>
      <button class="multiplayer-button" @click="toggleJoinForm">
        加入房间
      </button>
    </div>

    <div v-if="showJoinForm" class="join-room-form">
      <h3>加入房间</h3>
      <input
        type="text"
        placeholder="请输入房间号"
        v-model="roomId"
        class="room-input"
        maxlength="4"
        pattern="[0-9]{4}"
      />
      <button class="join-button" @click="joinRoom">
        确认加入
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { gameRoomApi, type GameRoom } from '../services/api';

const router = useRouter();
const showJoinForm = ref(false);
const roomId = ref('');
const errorMessage = ref('');

const createRoom = async () => {
  try {
    // 生成玩家ID（需要与MultiplayerGame.vue的期望格式一致）
    const timestamp = Date.now();
    const randomSuffix = Math.random().toString(36).substr(2, 9);
    const playerId = `player_${timestamp}_${randomSuffix}`;
    const username = `玩家${Math.floor(1000 + Math.random() * 9000)}`;

    // 1. 创建房间
    const roomData: GameRoom = await gameRoomApi.createRoom();
    console.log('创建房间返回数据:', roomData);

    if (roomData && roomData.roomId) {
      const createdRoomId = roomData.roomId;
      console.log('创建的房间ID:', createdRoomId);

      // 2. 加入刚创建的房间
      console.log('尝试加入房间:', createdRoomId);
      const joinResult = await gameRoomApi.joinRoom(createdRoomId, playerId, username);
      console.log('加入房间成功:', joinResult);

      // 保存玩家信息到localStorage（格式需要与MultiplayerGame.vue一致）
      localStorage.setItem('currentPlayer', JSON.stringify({
        playerId: playerId,              // 必须，用于WebSocket连接
        username: username,              // 必须，显示用
        playerName: username,           // 兼容字段，有些地方用playerName
        roomId: createdRoomId,          // 当前房间ID
        isHost: true                    // 创建者默认是房主
      }));

      // 3. 跳转到房间页面
      router.push(`/multiplayer-room/${createdRoomId}`);
    } else {
      throw new Error('创建房间失败: 没有返回有效的roomId');
    }
  } catch (error: any) {
    console.error('创建房间失败:', error);
    errorMessage.value = '创建房间失败: ' + (error.response?.data?.error || error.message);
  }
};

const toggleJoinForm = () => {
  showJoinForm.value = !showJoinForm.value;
  errorMessage.value = '';
};

const joinRoom = async () => {
  const trimmedRoomId = roomId.value.trim();

  if (!trimmedRoomId) {
    errorMessage.value = '请输入房间号';
    return;
  }

  // 验证房间号格式（4位数字）
  if (!/^\d{4}$/.test(trimmedRoomId)) {
    errorMessage.value = '房间号必须是4位数字';
    return;
  }

  try {
    // 生成玩家ID（需要与MultiplayerGame.vue的期望格式一致）
    const timestamp = Date.now();
    const randomSuffix = Math.random().toString(36).substr(2, 9);
    const playerId = `player_${timestamp}_${randomSuffix}`;
    const username = `玩家${Math.floor(1000 + Math.random() * 9000)}`;

    console.log('尝试加入房间:', trimmedRoomId);

    const joinResult = await gameRoomApi.joinRoom(trimmedRoomId, playerId, username);
    console.log('加入房间成功:', joinResult);

    if (joinResult && joinResult.room) {
      // 保存玩家信息到localStorage（格式需要与MultiplayerGame.vue一致）
      localStorage.setItem('currentPlayer', JSON.stringify({
        playerId: playerId,              // 必须，用于WebSocket连接
        username: username,              // 必须，显示用
        playerName: username,           // 兼容字段，有些地方用playerName
        roomId: trimmedRoomId,          // 当前房间ID
        isHost: false                   // 加入者不是房主
      }));

      router.push(`/multiplayer-room/${trimmedRoomId}`);
    } else {
      throw new Error('加入房间失败: 服务器返回数据异常');
    }
  } catch (error: any) {
    console.error('加入房间失败:', error);

    if (error.response?.status === 404) {
      errorMessage.value = '房间不存在';
    } else if (error.response?.status === 400) {
      errorMessage.value = error.response.data.error || '房间已满或游戏已开始';
    } else {
      errorMessage.value = '加入房间失败: ' + (error.response?.data?.error || error.message);
    }
  }
};
</script>

<style scoped>
.error-message {
  color: #dc3545;
  background-color: #f8d7da;
  border: 1px solid #f5c6cb;
  padding: 10px;
  border-radius: 4px;
  margin-bottom: 20px;
  max-width: 400px;
  text-align: center;
}

.multiplayer-mode-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 20px;
}

.game-title {
  font-size: 3rem;
  color: #333;
  margin-bottom: 40px;
}

.multiplayer-buttons {
  display: flex;
  gap: 30px;
  margin-bottom: 30px;
  flex-wrap: wrap;
  justify-content: center;
}

.multiplayer-button {
  padding: 20px 40px;
  font-size: 1.5rem;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 200px;
}

.multiplayer-button:hover {
  background-color: #45a049;
  transform: scale(1.05);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.multiplayer-button:active {
  transform: scale(0.98);
}

.join-room-form {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
  background-color: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  width: 300px;
}

.join-room-form h3 {
  margin: 0;
  color: #333;
  font-size: 1.5rem;
}

.room-input {
  padding: 10px;
  font-size: 1.2rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  width: 100%;
  box-sizing: border-box;
  text-align: center;
  letter-spacing: 2px;
}

.room-input:focus {
  outline: none;
  border-color: #4CAF50;
  box-shadow: 0 0 0 2px rgba(76, 175, 80, 0.2);
}

.join-button {
  padding: 10px 20px;
  font-size: 1.2rem;
  background-color: #2196F3;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.join-button:hover {
  background-color: #1976D2;
  transform: scale(1.05);
}

.join-button:active {
  transform: scale(0.98);
}
</style>
