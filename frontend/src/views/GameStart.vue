<template>
  <div class="game-start-container">
    <div class="game-start-content">
      <h1>历史人物猜猜呗</h1>
      <p class="game-description">
        猜测系统随机选择的历史人物，通过比较他们的tags来获得线索
      </p>
      <button
        @click="startGame"
        class="start-button"
      >
        开始游戏
      </button>
      <div class="login-status">
        <span @click="goToLogin" class="login-link">{{ isLoggedIn ? username : 'Not Logged In' }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();

// 从localStorage读取登录状态
const isLoggedIn = ref(!!localStorage.getItem('token'));
const username = ref(localStorage.getItem('username') || 'User123');

// 跳转到登录页面
const goToLogin = () => {
  router.push('/login');
};

// 开始游戏，跳转到游戏模式选择页面
const startGame = () => {
  router.push('/mode-select');
};
</script>

<style scoped>
.game-start-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f0f2f5;
  font-family: Arial, sans-serif;
}

.game-start-content {
  text-align: center;
  background-color: white;
  padding: 60px 40px;
  border-radius: 15px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  max-width: 500px;
  width: 100%;
}

.game-start-content h1 {
  font-size: 2.5rem;
  color: #333;
  margin-bottom: 20px;
}

.game-description {
  font-size: 1.1rem;
  color: #666;
  margin-bottom: 40px;
  line-height: 1.6;
}

.start-button {
  padding: 15px 40px;
  font-size: 1.2rem;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-weight: bold;
  box-shadow: 0 4px 15px rgba(76, 175, 80, 0.3);
}

.start-button:hover {
  background-color: #45a049;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(76, 175, 80, 0.4);
}

.start-button:active {
  transform: translateY(0);
  box-shadow: 0 2px 10px rgba(76, 175, 80, 0.3);
}

.login-status {
  margin-top: 30px;
  font-size: 1rem;
}

.login-link {
  color: #4CAF50;
  cursor: pointer;
  text-decoration: underline;
  font-weight: bold;
}

.login-link:hover {
  color: #45a049;
}
</style>
