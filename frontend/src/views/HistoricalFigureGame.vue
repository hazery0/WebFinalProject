<template>
  <div class="historical-game-container">
    <!-- 头部 -->
    <header class="game-header">
      <h1>单人模式</h1>
      <div class="login-status">
        <span @click="goToLogin" class="login-link">{{ isLoggedIn ? username : 'Not Logged In' }}</span>
      </div>
    </header>

    <!-- 搜索区域 -->
    <section class="search-section">
      <div class="search-container">
        <input
          v-model="searchQuery"
          @input="handleSearch"
          @keyup.enter="handleSearch"
          placeholder="输入历史人物名称"
          class="search-input"
        />
        <button @click="handleSearch" class="search-button">搜索</button>
      </div>

      <!-- 搜索结果下拉列表 -->
      <div v-if="searchResults.length > 0 && searchQuery" class="search-results">
        <div
          v-for="person in searchResults"
          :key="person.id"
          @click="selectPerson(person)"
          class="search-result-item"
        >
          {{ person.name }}
        </div>
      </div>
    </section>

    <!-- 游戏结果显示 -->
    <section class="result-section">
      <div class="game-controls">
        <h2>猜测历史</h2>
        <button @click="surrender" class="surrender-button">投降</button>
      </div>

      <!-- 胜利提示 -->
      <div v-if="gameWon" class="win-alert">
        <h3>恭喜你猜对了！</h3>
        <p>目标人物是：{{ targetPerson.name }}</p>
        <button @click="resetGame" class="reset-button">重新开始</button>
      </div>

      <!-- 投降提示 -->
      <div v-if="gameSurrendered" class="surrender-alert">
        <h3>你选择了投降</h3>
        <p>目标人物是：{{ targetPerson.name }}</p>
        <p>出生年份：{{ targetPerson.birthYear }}</p>
        <div class="tags-container">
          <span v-if="targetPerson.isLiterary" class="tag tag-match">文学家</span>
          <span v-if="targetPerson.isPolitical" class="tag tag-match">政治家</span>
          <span v-if="targetPerson.isThinker" class="tag tag-match">思想家</span>
          <span v-if="targetPerson.isScientist" class="tag tag-match">科学家</span>
        </div>
        <button @click="resetGame" class="reset-button">重新开始</button>
      </div>

      <!-- 猜测历史表格 -->
      <table class="guess-table">
        <thead>
          <tr>
            <th>名称</th>
            <th>出生年份</th>
            <th>标签</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(guess, index) in guessHistory" :key="index">
            <td>{{ guess.name }}</td>
            <td>
              {{ guess.birthYear }}
              <span v-if="guess.birthYearComparison" class="year-comparison">
                {{ guess.birthYearComparison === 'higher' ? '↑' : '↓' }}
              </span>
            </td>
            <td class="tags-container">
              <span
                v-if="guess.isLiterary"
                :class="['tag', { 'tag-match': guess.isLiterary === targetPerson.isLiterary, 'tag-no-match': guess.isLiterary !== targetPerson.isLiterary }]"
              >
                文学家
              </span>
              <span
                v-if="guess.isPolitical"
                :class="['tag', { 'tag-match': guess.isPolitical === targetPerson.isPolitical, 'tag-no-match': guess.isPolitical !== targetPerson.isPolitical }]"
              >
                政治家
              </span>
              <span
                v-if="guess.isThinker"
                :class="['tag', { 'tag-match': guess.isThinker === targetPerson.isThinker, 'tag-no-match': guess.isThinker !== targetPerson.isThinker }]"
              >
                思想家
              </span>
              <span
                v-if="guess.isScientist"
                :class="['tag', { 'tag-match': guess.isScientist === targetPerson.isScientist, 'tag-no-match': guess.isScientist !== targetPerson.isScientist }]"
              >
                科学家
              </span>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="guessHistory.length === 0" class="empty-history">
        还没有猜测记录，开始搜索并选择历史人物吧！
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { historicalPersonApi } from '../services/api';

const router = useRouter();

// 状态管理
const isLoggedIn = ref(!!localStorage.getItem('token')); // 从localStorage读取登录状态
const username = ref(localStorage.getItem('username') || 'User123'); // 从localStorage读取用户名
const searchQuery = ref('');
const searchResults = ref<any[]>([]);
const guessHistory = ref<any[]>([]);
const gameWon = ref(false);
const gameSurrendered = ref(false);

// 目标人物和猜测结果类型
interface HistoricalPerson {
  id: number;
  name: string;
  birthYear: number;
  isLiterary: number;
  isPolitical: number;
  isThinker: number;
  isScientist: number;
}

interface GuessResult extends HistoricalPerson {
  birthYearComparison?: 'higher' | 'lower';
}

const targetPerson = ref<HistoricalPerson>({
  id: 0,
  name: '',
  birthYear: 0,
  isLiterary: 0,
  isPolitical: 0,
  isThinker: 0,
  isScientist: 0
});

// 跳转到登录页面
const goToLogin = () => {
  router.push('/login');
};

// 处理搜索
const handleSearch = async () => {
  if (!searchQuery.value.trim()) {
    searchResults.value = [];
    return;
  }

  try {
    const results = await historicalPersonApi.search(searchQuery.value);
    searchResults.value = results;
  } catch (error) {
    console.error('搜索失败:', error);
    searchResults.value = [];
  }
};

// 选择人物作为猜测
const selectPerson = (person: HistoricalPerson) => {
  // 比较猜测与目标人物
  const guessResult: GuessResult = { ...person };

  // 比较出生年份
  if (person.birthYear > targetPerson.value.birthYear) {
    guessResult.birthYearComparison = 'higher'; // 猜测年份更大，显示↓
  } else if (person.birthYear < targetPerson.value.birthYear) {
    guessResult.birthYearComparison = 'lower'; // 猜测年份更小，显示↑
  }

  // 添加到猜测历史
  guessHistory.value.unshift(guessResult);

  // 清空搜索
  searchQuery.value = '';
  searchResults.value = [];

  // 检查是否猜对
  if (person.name === targetPerson.value.name) {
    gameWon.value = true;
  }
};

// 投降方法
const surrender = () => {
  gameSurrendered.value = true;
};

// 重置游戏
const resetGame = () => {
  gameWon.value = false;
  gameSurrendered.value = false;
  guessHistory.value = [];
  getRandomTargetPerson();
};

// 获取随机目标人物
const getRandomTargetPerson = async () => {
  try {
    const person = await historicalPersonApi.getRandom();
    targetPerson.value = person;
    console.log('目标人物:', targetPerson.value); // 用于调试
  } catch (error) {
    console.error('获取随机人物失败:', error);
  }
};

// 初始化
onMounted(() => {
  getRandomTargetPerson();
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

/* 搜索区域样式 */
.search-section {
  margin-bottom: 30px;
  position: relative;
}

.search-container {
  display: flex;
  gap: 10px;
  max-width: 600px;
  margin: 0 auto;
}

.search-input {
  flex: 1;
  padding: 12px;
  font-size: 1rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.search-button {
  padding: 12px 24px;
  font-size: 1rem;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.3s;
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
  background-color: white;
  border: 1px solid #ddd;
  border-radius: 0 0 6px 6px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  z-index: 100;
  max-height: 300px;
  overflow-y: auto;
}

.search-result-item {
  padding: 12px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
}

.search-result-item:hover {
  background-color: #f5f5f5;
}

.search-result-item:last-child {
  border-bottom: none;
}

/* 结果区域样式 */
.result-section {
  background-color: white;
  padding: 20px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.result-section h2 {
  margin-top: 0;
  margin-bottom: 20px;
  color: #333;
  text-align: center;
}

/* 胜利提示 */
.game-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.surrender-button {
  padding: 10px 20px;
  background-color: #dc3545;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1rem;
  font-weight: bold;
  transition: background-color 0.3s;
}

.surrender-button:hover {
  background-color: #c82333;
}

.win-alert {
  background-color: #d4edda;
  color: #155724;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  text-align: center;
  border: 1px solid #c3e6cb;
}

.surrender-alert {
  background-color: #fff3cd;
  color: #856404;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  text-align: center;
  border: 1px solid #ffeeba;
}

.win-alert h3 {
  margin-top: 0;
  font-size: 1.5rem;
}

.reset-button {
  margin-top: 10px;
  padding: 10px 20px;
  background-color: #28a745;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1rem;
}

.reset-button:hover {
  background-color: #218838;
}

/* 表格样式 */
.guess-table {
  width: 100%;
  border-collapse: collapse;
  margin: 0 auto;
}

.guess-table th,
.guess-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #ddd;
}

.guess-table th {
  background-color: #f5f5f5;
  font-weight: bold;
  color: #333;
}

.guess-table tr:hover {
  background-color: #f9f9f9;
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

/* 空历史提示 */
.empty-history {
  text-align: center;
  color: #666;
  padding: 40px;
  font-style: italic;
}
</style>
