<template>
  <div class="game-mode-select-container">
    <h1 class="game-title">历史人物猜猜乐</h1>
    <h2 class="mode-title">选择游戏模式</h2>
    <div class="mode-buttons">
      <button class="mode-button" @click="goToSinglePlayer">
        单人模式
      </button>
      <button class="mode-button" @click="goToMultiPlayer">
        多人模式
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';

const router = useRouter();

const goToSinglePlayer = () => {
  router.push('/game');
};

const goToMultiPlayer = () => {
  router.push('/multiplayer');
};
</script>

<style scoped>
.game-mode-select-container {
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
  margin-bottom: 20px;
}

.mode-title {
  font-size: 2rem;
  color: #555;
  margin-bottom: 40px;
}

.mode-buttons {
  display: flex;
  gap: 30px;
  flex-wrap: wrap;
  justify-content: center;
}

.mode-button {
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

.mode-button:hover {
  background-color: #45a049;
  transform: scale(1.05);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.mode-button:active {
  transform: scale(0.98);
}
</style>
