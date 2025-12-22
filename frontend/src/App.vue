<script setup lang="ts">
import { onMounted } from 'vue';
import { gameService } from './services/gameService';
import { RouterView } from 'vue-router'

onMounted(() => {
  // 在应用挂载时就初始化连接
  // 即使在 RoomSelect 页面，连接也是激活状态
  gameService.connect();
});

</script>

<template>
  <main>
    <RouterView />
  </main>
</template>
