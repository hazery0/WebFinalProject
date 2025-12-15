<template>
  <div class="auth-container">
    <div class="auth-card">
      <h2>用户注册</h2>
      
      <form @submit.prevent="handleRegister" class="auth-form">
        <div class="form-group">
          <label for="username">用户名</label>
          <input 
            type="text" 
            id="username" 
            v-model="form.username" 
            placeholder="请输入用户名"
            required
            class="form-input"
          />
        </div>
        
        <div class="form-group">
          <label for="password">密码</label>
          <input 
            type="password" 
            id="password" 
            v-model="form.password" 
            placeholder="请输入密码（至少6位）"
            required
            minlength="6"
            class="form-input"
          />
        </div>
        
        <div v-if="error" class="error-message">
          {{ error }}
        </div>
        
        <button 
          type="submit" 
          :disabled="loading"
          class="auth-button"
        >
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </form>
      
      <div class="auth-footer">
        已有账号？
        <router-link to="/login" class="link">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

const router = useRouter();
const form = ref({
  username: '',
  password: ''
});
const loading = ref(false);
const error = ref('');

const handleRegister = async () => {
  loading.value = true;
  error.value = '';
  
  try {
    await axios.post('http://localhost:8080/api/auth/register', form.value);
    
    // 注册成功，跳转到登录页面
    router.push('/login');
  } catch (err: any) {
    error.value = err.response?.data?.message || '注册失败，请稍后重试';
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 80vh;
}

.auth-card {
  background: white;
  padding: 40px;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
}

.auth-card h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-weight: 500;
  color: #555;
}

.form-input {
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 16px;
  transition: border-color 0.3s;
}

.form-input:focus {
  outline: none;
  border-color: #4CAF50;
}

.error-message {
  color: #f44336;
  font-size: 14px;
  text-align: center;
}

.auth-button {
  padding: 12px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.3s;
}

.auth-button:hover:not(:disabled) {
  background-color: #45a049;
}

.auth-button:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

.auth-footer {
  margin-top: 20px;
  text-align: center;
  color: #666;
  font-size: 14px;
}

.link {
  color: #4CAF50;
  text-decoration: none;
  margin-left: 5px;
  font-weight: 500;
}

.link:hover {
  text-decoration: underline;
}
</style>