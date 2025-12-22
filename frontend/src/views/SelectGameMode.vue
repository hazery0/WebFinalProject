<template>
  <div class="background-container">
    <div class="select-game-mode-container">
      <h1>选择游戏模式</h1>
      <div class="mode-buttons">
        <button @click="goToSingleMode" class="mode-button">单人模式</button>
        <button @click="goToMultiMode" class="mode-button">多人模式</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
//<script setup>：使用 Vue 3 的 setup 语法糖 lang="ts"：在 <script> 块中使用 TypeScript
import { useRouter } from "vue-router";

//下面实现路由跳转功能
const router = useRouter(); // 获取路由实例，用于跳转页面
const goToSingleMode = () => {
  // 跳转到单人模式页面
  router.push("/game");
};
const goToMultiMode = () => {
  // 跳转到多人模式页面
  router.push("/room-select");
};

</script>

<style scoped>
.h1 {
  font-size: 2.5rem;
  margin-bottom: 30px;
  color: #333;
  font-weight: bold;
}

.background-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f0f2f5;
  font-family: Arial, sans-serif;
}

.select-game-mode-container {
  text-align: center;
  background-color: white;
  padding: 60px 40px;
  border-radius: 15px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  max-width: 500px;
  width: 100%;
}

.mode-buttons {
  display: flex;
  justify-content: space-around;
  margin-top: 30px;
}

.mode-button {
  padding: 15px 40px;
  font-size: 1.2rem;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-weight: bold;
  box-shadow: 0 10px 20px rgba(76, 175, 80, 0.3);
}
</style>
