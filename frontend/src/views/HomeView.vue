<template>
  <div class="home">
    <h1>欢迎使用 Vue 初始项目</h1>
    <p>这是一个简化的 Vue 项目，已配置好后端连接。</p>
    
    <div class="test-section">
      <h2>测试后端连接</h2>
      <button @click="testBackendConnection" class="test-button">
        {{ loading ? '测试中...' : '测试连接' }}
      </button>
      <div v-if="result" class="result" :class="{ success: result.success, error: !result.success }">
        {{ result.message }}
      </div>
    </div>
    
    <div class="info">
      <h2>项目信息</h2>
      <p>后端 API 地址: <code>http://localhost:8080/api</code></p>
      <p>前端地址: <code>http://localhost:5174</code></p>
      <p>项目已配置好与后端的基础连接。</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import axios from 'axios';

const loading = ref(false);
const result = ref<{ success: boolean; message: string } | null>(null);

// 测试后端连接
const testBackendConnection = async () => {
  loading.value = true;
  result.value = null;
  
  try {
    // 使用 axios 直接测试连接（不依赖其他服务）
    const response = await axios.get('http://localhost:8080/api/health', {
      timeout: 5000
    });
    
    result.value = {
      success: true,
      message: `连接成功！后端状态: ${response.data.status || '正常'}`
    };
  } catch (error: any) {
    let errorMessage = '连接失败！';
    if (error.response) {
      errorMessage += `状态码: ${error.response.status}, 消息: ${error.response.data.message || '未知错误'}`;
    } else if (error.request) {
      errorMessage += '未收到响应，请检查后端服务是否运行。';
    } else {
      errorMessage += `请求错误: ${error.message}`;
    }
    
    result.value = {
      success: false,
      message: errorMessage
    };
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.home {
  max-width: 800px;
  margin: 0 auto;
  text-align: center;
  padding: 40px 20px;
}

h1 {
  font-size: 2.5rem;
  color: #333;
  margin-bottom: 20px;
}

p {
  font-size: 1.1rem;
  color: #666;
  margin-bottom: 30px;
}

.test-section {
  background: white;
  padding: 30px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  margin-bottom: 30px;
}

h2 {
  font-size: 1.8rem;
  color: #333;
  margin-bottom: 20px;
}

.test-button {
  padding: 12px 24px;
  font-size: 1rem;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.test-button:hover:not(:disabled) {
  background-color: #45a049;
}

.test-button:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

.result {
  margin-top: 20px;
  padding: 15px;
  border-radius: 6px;
  font-weight: bold;
}

.result.success {
  background-color: #d4edda;
  color: #155724;
}

.result.error {
  background-color: #f8d7da;
  color: #721c24;
}

.info {
  background: white;
  padding: 30px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

code {
  background-color: #f0f0f0;
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Courier New', Courier, monospace;
  color: #c7254e;
}
</style>
