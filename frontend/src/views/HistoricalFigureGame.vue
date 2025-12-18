<template>
  <div class="historical-game-container">
    <!-- 头部 -->
    <header class="game-header">
      <h1>历史人物猜谜游戏</h1>
      <div class="login-status">
        <span @click="goToLogin" class="login-link">{{ isLoggedIn ? username : 'Not Logged In' }}</span>
      </div>
    </header>

    <!-- 使用 GuessModule 组件 -->
    <GuessModule
      ref="guessModuleRef"
      @guess="handleGuess"
      @surrender="handleSurrender"
      @reset="handleReset"
      @win="handleWin"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import GuessModule from './GuessModule.vue'; // 导入 GuessModule 组件

const router = useRouter();
const guessModuleRef = ref<InstanceType<typeof GuessModule>>();

// 状态管理
const isLoggedIn = ref(!!localStorage.getItem('token')); // 从localStorage读取登录状态
const username = ref(localStorage.getItem('username') || 'User123'); // 从localStorage读取用户名

// 跳转到登录页面
const goToLogin = () => {
  router.push('/login');
};

// 处理猜测事件
const handleGuess = (guessResult: any) => {
  console.log('猜测结果:', guessResult);
  // 单人模式中，猜谜逻辑完全由 GuessModule 处理
  // 这里可以添加额外的逻辑（如保存游戏记录到后端等）
};

// 处理投降事件
const handleSurrender = () => {
  console.log('玩家投降了');
  // 可以在这里添加投降后的额外逻辑
};

// 处理重置事件
const handleReset = () => {
  console.log('游戏重置');
  // 可以在这里添加重置后的额外逻辑
};

// 处理胜利事件
const handleWin = (targetPerson: any) => {
  console.log('游戏胜利，目标人物:', targetPerson);
  // 可以在这里添加胜利后的额外逻辑，如保存得分等
};

// 初始化
onMounted(() => {
  console.log('单人模式游戏已加载');
});
</script>

<style scoped>
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
</style>
