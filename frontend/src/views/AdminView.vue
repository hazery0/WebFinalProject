<template>
  <div class="admin-container">
    <header class="admin-header">
      <h1>管理员后台 - 历史人物管理</h1>
      <div class="admin-info">
        <span>欢迎，{{ username }}</span>
        <button @click="logout" class="logout-button">退出登录</button>
      </div>
    </header>

    <main class="admin-main">
      <!-- 添加新人物表单 -->
      <section class="admin-section">
        <h2>添加新历史人物</h2>
        <form @submit.prevent="addHistoricalPerson" class="person-form">
          <div class="form-row">
            <div class="form-group">
              <label for="name">姓名</label>
              <input
                type="text"
                id="name"
                v-model="newPerson.name"
                placeholder="请输入历史人物姓名"
                required
                class="form-input"
              />
            </div>
            <div class="form-group">
              <label for="birthYear">出生年份</label>
              <input
                type="number"
                id="birthYear"
                v-model="newPerson.birthYear"
                placeholder="请输入出生年份"
                class="form-input"
              />
            </div>
          </div>

          <div class="form-row">
            <div class="form-group tags-group">
              <label>标签</label>
              <div class="tags-container">
                <label class="tag-checkbox">
                  <input
                    type="checkbox"
                    v-model="newPerson.isLiterary"
                    true-value="1"
                    false-value="0"
                  />
                  <span>文学家</span>
                </label>
                <label class="tag-checkbox">
                  <input
                    type="checkbox"
                    v-model="newPerson.isPolitical"
                    true-value="1"
                    false-value="0"
                  />
                  <span>政治家</span>
                </label>
                <label class="tag-checkbox">
                  <input
                    type="checkbox"
                    v-model="newPerson.isThinker"
                    true-value="1"
                    false-value="0"
                  />
                  <span>思想家</span>
                </label>
                <label class="tag-checkbox">
                  <input
                    type="checkbox"
                    v-model="newPerson.isScientist"
                    true-value="1"
                    false-value="0"
                  />
                  <span>科学家</span>
                </label>
              </div>
            </div>
          </div>

          <button type="submit" class="submit-button">添加人物</button>
        </form>
      </section>

      <!-- 编辑悬浮窗 -->
      <div v-if="isEditing" class="modal-overlay" @click="cancelEdit">
        <div class="modal-content" @click.stop>
          <div class="modal-header">
            <h2>编辑历史人物</h2>
            <button @click="cancelEdit" class="close-button">×</button>
          </div>
          <form @submit.prevent="saveEdit" class="person-form">
            <div class="form-row">
              <div class="form-group">
                <label for="edit-name">姓名</label>
                <input
                  type="text"
                  id="edit-name"
                  v-model="editingPerson.name"
                  placeholder="请输入历史人物姓名"
                  required
                  class="form-input"
                />
              </div>
              <div class="form-group">
                <label for="edit-birthYear">出生年份</label>
                <input
                  type="number"
                  id="edit-birthYear"
                  v-model="editingPerson.birthYear"
                  placeholder="请输入出生年份"
                  class="form-input"
                />
              </div>
            </div>

            <div class="form-row">
              <div class="form-group tags-group">
                <label>标签</label>
                <div class="tags-container">
                  <label class="tag-checkbox">
                    <input
                      type="checkbox"
                      v-model="editingPerson.isLiterary"
                      true-value="1"
                      false-value="0"
                    />
                    <span>文学家</span>
                  </label>
                  <label class="tag-checkbox">
                    <input
                      type="checkbox"
                      v-model="editingPerson.isPolitical"
                      true-value="1"
                      false-value="0"
                    />
                    <span>政治家</span>
                  </label>
                  <label class="tag-checkbox">
                    <input
                      type="checkbox"
                      v-model="editingPerson.isThinker"
                      true-value="1"
                      false-value="0"
                    />
                    <span>思想家</span>
                  </label>
                  <label class="tag-checkbox">
                    <input
                      type="checkbox"
                      v-model="editingPerson.isScientist"
                      true-value="1"
                      false-value="0"
                    />
                    <span>科学家</span>
                  </label>
                </div>
              </div>
            </div>

            <div class="form-actions">
              <button type="submit" class="submit-button">保存修改</button>
              <button type="button" @click="cancelEdit" class="cancel-button">取消</button>
            </div>
          </form>
        </div>
      </div>

      <!-- 批量导入 -->
      <section class="admin-section">
        <h2>批量导入历史人物</h2>
        <div class="batch-import">
          <div class="import-info">
            <p>支持JSON格式文件导入，示例格式：</p>
            <pre class="json-example">[
  {
    "name": "李白",
    "birthYear": 701,
    "isLiterary": 1,
    "isPolitical": 0,
    "isThinker": 1,
    "isScientist": 0
  }
]</pre>
          </div>

          <div class="import-controls">
            <input
              type="file"
              ref="fileInput"
              @change="handleFileChange"
              accept=".json"
              class="file-input"
            />
            <button
              @click="importFromFile"
              :disabled="!batchFile || importLoading"
              class="import-button"
            >
              {{ importLoading ? '导入中...' : '批量导入' }}
            </button>
            <button
              @click="clearFile"
              :disabled="!batchFile"
              class="clear-button"
            >
              清除文件
            </button>
          </div>

          <!-- 导入结果提示 -->
          <div v-if="importSuccess" class="success-message">
            批量导入成功！
          </div>
          <div v-if="importError" class="error-message">
            {{ importError }}
          </div>
        </div>
      </section>

      <!-- 人物列表 -->
      <section class="admin-section">
        <h2>历史人物列表</h2>
        <div v-if="loading" class="loading">加载中...</div>
        <div v-else-if="historicalPersons.length === 0" class="empty-list">暂无数据</div>
        <div v-else>
          <table class="persons-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>姓名</th>
                <th>出生年份</th>
                <th>文学家</th>
                <th>政治家</th>
                <th>思想家</th>
                <th>科学家</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="person in paginatedPersons" :key="person.id">
                <td>{{ person.id }}</td>
                <td>{{ person.name }}</td>
                <td>{{ person.birthYear || '-' }}</td>
                <td>{{ person.isLiterary ? '是' : '否' }}</td>
                <td>{{ person.isPolitical ? '是' : '否' }}</td>
                <td>{{ person.isThinker ? '是' : '否' }}</td>
                <td>{{ person.isScientist ? '是' : '否' }}</td>
                <td>
                  <button @click="startEdit(person)" class="edit-button">编辑</button>
                  <button @click="deleteHistoricalPerson(person.id)" class="delete-button">删除</button>
                </td>
              </tr>
            </tbody>
          </table>

          <!-- 分页控件 -->
          <div class="pagination">
            <div class="page-info">
              显示 {{ startIndex + 1 }} - {{ endIndex }} / {{ totalItems }} 条记录
            </div>
            <div class="page-controls">
              <button @click="prevPage" :disabled="currentPage === 1" class="page-btn">
                &lt; 上一页
              </button>

              <div class="page-size-control">
                <span>每页显示：</span>
                <input
                  type="number"
                  v-model="inputPageSize"
                  @change="changePageSize"
                  min="1"
                  class="page-size-input"
                />
                <span>条</span>
              </div>

              <div class="page-jump">
                <span>第</span>
                <input
                  type="number"
                  v-model="inputPage"
                  @change="goToPage"
                  min="1"
                  :max="totalPages"
                  class="page-jump-input"
                />
                <span>页 / 共 {{ totalPages }} 页</span>
              </div>

              <button @click="nextPage" :disabled="currentPage === totalPages" class="page-btn">
                下一页 &gt;
              </button>
            </div>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { historicalPersonApi } from '../services/api';

const router = useRouter();

// 从localStorage读取管理员信息
const username = ref(localStorage.getItem('username') || '管理员');
const isAdmin = ref(localStorage.getItem('isAdmin') === '1');

// 检查是否为管理员，不是则跳转到首页
if (!isAdmin.value) {
  router.push('/start');
}

// 状态管理
const loading = ref(false);
const historicalPersons = ref<any[]>([]);
const newPerson = ref({
  name: '',
  birthYear: null,
  isLiterary: 0,
  isPolitical: 0,
  isThinker: 0,
  isScientist: 0
});

// 编辑相关
const isEditing = ref(false);
const editingPerson = ref({
  id: 0,
  name: '',
  birthYear: null,
  isLiterary: 0,
  isPolitical: 0,
  isThinker: 0,
  isScientist: 0
});

// 分页相关
const currentPage = ref(1);
const pageSize = ref(10);
const totalPages = ref(1);
const totalItems = ref(0);
const startIndex = ref(0);
const endIndex = ref(0);
const paginatedPersons = ref<any[]>([]);
const inputPageSize = ref(pageSize.value.toString());

// 批量导入相关
const batchFile = ref<File | null>(null);
const fileInput = ref<HTMLInputElement | null>(null);
const importSuccess = ref(false);
const importError = ref('');
const importLoading = ref(false);

// 计算分页数据
const calculatePagination = () => {
  totalItems.value = historicalPersons.value.length;
  totalPages.value = Math.ceil(totalItems.value / pageSize.value);
  startIndex.value = (currentPage.value - 1) * pageSize.value;
  endIndex.value = Math.min(startIndex.value + pageSize.value, totalItems.value);
  paginatedPersons.value = historicalPersons.value.slice(startIndex.value, endIndex.value);

  // 确保当前页不超过总页数
  if (currentPage.value > totalPages.value) {
    currentPage.value = Math.max(1, totalPages.value);
    calculatePagination();
  }
};

// 获取所有历史人物
const fetchHistoricalPersons = async () => {
  loading.value = true;
  try {
    const response = await historicalPersonApi.getAll();
    historicalPersons.value = response;
    // 计算分页
    calculatePagination();
  } catch (error) {
    console.error('获取历史人物失败:', error);
  } finally {
    loading.value = false;
  }
};

// 切换到上一页
const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--;
    calculatePagination();
  }
};

// 切换到下一页
const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++;
    calculatePagination();
  }
};

// 跳转到指定页面
const goToPage = () => {
  const page = parseInt(inputPage.value, 10);
  if (!isNaN(page) && page >= 1 && page <= totalPages.value) {
    currentPage.value = page;
    calculatePagination();
  }
};

// 调整每页显示数量
const changePageSize = () => {
  const size = parseInt(inputPageSize.value, 10);
  if (!isNaN(size) && size > 0) {
    pageSize.value = size;
    currentPage.value = 1; // 重置到第一页
    calculatePagination();
  }
};

// 页码输入
const inputPage = ref(currentPage.value.toString());

// 添加新历史人物
const addHistoricalPerson = async () => {
  try {
    // 转换birthYear为数字类型
    const personData = {
      ...newPerson.value,
      birthYear: newPerson.value.birthYear === null ? null : Number(newPerson.value.birthYear)
    };

    await historicalPersonApi.add(personData);

    // 清空表单
    newPerson.value = {
      name: '',
      birthYear: null,
      isLiterary: 0,
      isPolitical: 0,
      isThinker: 0,
      isScientist: 0
    };

    // 刷新列表
    await fetchHistoricalPersons();
  } catch (error) {
    console.error('添加人物失败:', error);
  }
};

// 删除历史人物
const deleteHistoricalPerson = async (id: number) => {
  if (confirm('确定要删除这个历史人物吗？')) {
    try {
      await historicalPersonApi.delete(id);
      // 刷新列表
      await fetchHistoricalPersons();
    } catch (error) {
      console.error('删除人物失败:', error);
    }
  }
};

// 开始编辑
const startEdit = (person: any) => {
  editingPerson.value = { ...person };
  isEditing.value = true;
};

// 保存编辑
const saveEdit = async () => {
  try {
    // 转换birthYear为数字类型
    const personData = {
      ...editingPerson.value,
      birthYear: editingPerson.value.birthYear === null ? null : Number(editingPerson.value.birthYear)
    };

    // 调用API更新人物信息
    await historicalPersonApi.update(personData.id, personData);

    // 刷新列表
    await fetchHistoricalPersons();

    // 退出编辑模式
    cancelEdit();
  } catch (error) {
    console.error('更新人物失败:', error);
  }
};

// 取消编辑
const cancelEdit = () => {
  isEditing.value = false;
  editingPerson.value = {
    id: 0,
    name: '',
    birthYear: null,
    isLiterary: 0,
    isPolitical: 0,
    isThinker: 0,
    isScientist: 0
  };
};

// 处理文件选择
const handleFileChange = (event: any) => {
  const file = event.target.files[0];
  if (file) {
    if (file.type === 'application/json' || file.name.endsWith('.json')) {
      batchFile.value = file;
      importSuccess.value = false;
      importError.value = '';
    } else {
      importError.value = '请选择JSON格式的文件';
      event.target.value = '';
    }
  }
};

// 执行批量导入
const importFromFile = async () => {
  if (!batchFile) return;

  importLoading.value = true;
  importSuccess.value = false;
  importError.value = '';

  try {
    // 读取文件内容
    const fileContent = await readFileAsText(batchFile.value);

    // 解析JSON
    const persons = JSON.parse(fileContent);

    // 验证数据格式
    if (!Array.isArray(persons)) {
      throw new Error('JSON格式错误，应该是数组格式');
    }

    // 批量导入
    await historicalPersonApi.batchAdd(persons);

    // 刷新列表
    await fetchHistoricalPersons();

    // 显示成功消息
    importSuccess.value = true;

    // 清空文件选择
    clearFile();
  } catch (error: any) {
    importError.value = `导入失败：${error.message}`;
  } finally {
    importLoading.value = false;
  }
};

// 清除选中的文件
const clearFile = () => {
  batchFile.value = null;
  if (fileInput.value) {
    fileInput.value.value = '';
  }
  importSuccess.value = false;
  importError.value = '';
};

// 读取文件内容
const readFileAsText = (file: File): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => resolve(e.target?.result as string);
    reader.onerror = () => reject(new Error('文件读取失败'));
    reader.readAsText(file);
  });
};

// 退出登录
const logout = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('username');
  localStorage.removeItem('isAdmin');
  router.push('/login');
};

// 初始化
onMounted(() => {
  fetchHistoricalPersons();
});
</script>

<style scoped>
.admin-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  font-family: Arial, sans-serif;
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e0e0e0;
}

.admin-header h1 {
  margin: 0;
  color: #333;
}

.admin-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.logout-button {
  padding: 8px 16px;
  background-color: #dc3545;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
}

.logout-button:hover {
  background-color: #c82333;
}

.admin-main {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.admin-section {
  margin-bottom: 30px;
}

.admin-section h2 {
  margin-top: 0;
  margin-bottom: 20px;
  color: #333;
  font-size: 1.5rem;
}

.person-form {
  background-color: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
}

.form-row {
  display: flex;
  gap: 20px;
  margin-bottom: 15px;
}

.form-group {
  flex: 1;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
  color: #555;
}

.form-input {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
}

.tags-group {
  flex: 2;
}

.tags-container {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.tag-checkbox {
  display: flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
  font-weight: normal;
}

.submit-button {
  padding: 12px 24px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
  font-weight: bold;
}

.submit-button:hover {
  background-color: #45a049;
}

.loading {
  text-align: center;
  padding: 20px;
  color: #666;
}

.empty-list {
  text-align: center;
  padding: 40px;
  color: #666;
  font-style: italic;
}

.persons-table {
  width: 100%;
  border-collapse: collapse;
}

.persons-table th,
.persons-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #ddd;
}

.persons-table th {
  background-color: #f5f5f5;
  font-weight: bold;
  color: #333;
}

.persons-table tr:hover {
  background-color: #f9f9f9;
}

.delete-button {
  padding: 6px 12px;
  background-color: #dc3545;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
}

.delete-button:hover {
  background-color: #c82333;
}

/* 编辑按钮样式 */
.edit-button {
  padding: 6px 12px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  margin-right: 8px;
}

.edit-button:hover {
  background-color: #0056b3;
}

/* 表单操作区域 */
.form-actions {
  display: flex;
  gap: 15px;
  margin-top: 20px;
  justify-content: flex-start;
}

.cancel-button {
  padding: 12px 24px;
  background-color: #6c757d;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
}

.cancel-button:hover {
  background-color: #5a6268;
}

/* 模态框样式 */
.modal-overlay {
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
  animation: fadeIn 0.3s ease;
}

.modal-content {
  background-color: white;
  padding: 0;
  border-radius: 8px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
  max-width: 600px;
  width: 90%;
  max-height: 90vh;
  overflow-y: auto;
  animation: slideIn 0.3s ease;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #e0e0e0;
}

.modal-header h2 {
  margin: 0;
  font-size: 1.5rem;
  color: #333;
}

.close-button {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #666;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.close-button:hover {
  background-color: #f0f0f0;
  color: #333;
}

.modal-content .person-form {
  padding: 20px;
  background-color: white;
  border-radius: 0 0 8px 8px;
}

/* 动画效果 */
@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 分页样式 */
.pagination {
  margin-top: 20px;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 15px;
}

.page-info {
  color: #555;
  font-size: 0.9rem;
}

.page-controls {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.page-btn {
  padding: 8px 16px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: background-color 0.3s;
}

.page-btn:hover:not(:disabled) {
  background-color: #0056b3;
}

.page-btn:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
}

.page-size-control,
.page-jump {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.9rem;
}

.page-size-input,
.page-jump-input {
  width: 60px;
  padding: 6px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.9rem;
  text-align: center;
}

.page-size-input:focus,
.page-jump-input:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.25);
}

/* 批量导入样式 */
.batch-import {
  background-color: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
}

.import-info {
  margin-bottom: 20px;
}

.import-info p {
  margin: 0 0 10px 0;
  font-weight: bold;
  color: #555;
}

.json-example {
  background-color: #f0f0f0;
  padding: 15px;
  border-radius: 4px;
  font-family: 'Courier New', Courier, monospace;
  font-size: 0.9rem;
  line-height: 1.5;
  overflow-x: auto;
  white-space: pre-wrap;
  word-wrap: break-word;
  border: 1px solid #ddd;
}

.import-controls {
  display: flex;
  gap: 15px;
  align-items: center;
  margin-bottom: 15px;
  flex-wrap: wrap;
}

.file-input {
  flex: 1;
  min-width: 200px;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.9rem;
  background-color: white;
}

.import-button {
  padding: 12px 24px;
  background-color: #28a745;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
  font-weight: bold;
}

.import-button:hover:not(:disabled) {
  background-color: #218838;
}

.import-button:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
}

.clear-button {
  padding: 12px 24px;
  background-color: #6c757d;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
}

.clear-button:hover:not(:disabled) {
  background-color: #5a6268;
}

.clear-button:disabled {
  background-color: #adb5bd;
  cursor: not-allowed;
}

.success-message {
  background-color: #d4edda;
  color: #155724;
  padding: 15px;
  border-radius: 4px;
  margin-top: 15px;
  font-weight: bold;
  border: 1px solid #c3e6cb;
}

.error-message {
  background-color: #f8d7da;
  color: #721c24;
  padding: 15px;
  border-radius: 4px;
  margin-top: 15px;
  font-weight: bold;
  border: 1px solid #f5c6cb;
}
</style>
