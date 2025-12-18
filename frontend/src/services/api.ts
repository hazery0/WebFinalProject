import axios from 'axios';

// 创建 axios 实例
const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 10000,
});

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器 - 直接返回 response.data
api.interceptors.response.use(
  (response) => {
    return response.data;
  },
  (error) => {
    if (error.response) {
      const message = error.response.data?.error || error.response.data?.message || '请求失败';
      console.error(`请求失败 (${error.response.status}):`, message);
      return Promise.reject(new Error(message));
    } else if (error.request) {
      console.error('没有收到响应，请检查网络连接');
      return Promise.reject(new Error('网络连接失败'));
    } else {
      console.error('请求配置错误:', error.message);
      return Promise.reject(error);
    }
  }
);

// 类型定义
export interface GamePlayer {
  id?: number;
  playerId: string;
  playerName: string;
  username?: string; // 兼容字段
  isHost: boolean;
  score: number;
  guessCount?: number;
  status?: string;
}

export interface GameRoom {
  roomId: string;
  status: 'waiting' | 'playing' | 'finished' | 'WAITING' | 'PLAYING' | 'FINISHED';
  players: GamePlayer[];
  createdAt?: string;
  targetPerson?: HistoricalPerson;
}

export interface HistoricalPerson {
  id: number;
  name: string;
  birthYear: number;
  isLiterary: number | boolean;
  isPolitical: number | boolean;
  isThinker: number | boolean;
  isScientist: number | boolean;
}

// 创建历史人物时不需要id字段，因为id由后端生成
export type CreateHistoricalPerson = Omit<HistoricalPerson, 'id'>;

// 创建类型安全的请求函数
function typedGet<T>(url: string): Promise<T> {
  return api.get(url).then(response => response as T);
}

function typedPost<T>(url: string, data?: any): Promise<T> {
  return api.post(url, data).then(response => response as T);
}

// 新增 PUT 和 DELETE 方法
function typedPut<T>(url: string, data?: any): Promise<T> {
  return api.put(url, data).then(response => response as T);
}

function typedDelete<T>(url: string): Promise<T> {
  return api.delete(url).then(response => response as T);
}

// 游戏房间相关 API
export const gameRoomApi = {
  // 创建房间
  createRoom: (): Promise<GameRoom> => {
    return typedPost<GameRoom>('/game/rooms');
  },

  // 获取房间列表
  getRoomList: (): Promise<GameRoom[]> => {
    return typedGet<GameRoom[]>('/game/rooms');
  },

  // 获取房间详情
  getRoomInfo: (roomId: string): Promise<{
    room: GameRoom;
    players: GamePlayer[];
  }> => {
    return typedGet<{ room: GameRoom; players: GamePlayer[] }>(`/game/rooms/${roomId}`);
  },

  // 加入房间
  joinRoom: (roomId: string, playerId: string, username: string): Promise<{
    room: GameRoom;
    players: GamePlayer[];
    player: GamePlayer;
  }> => {
    return typedPost<{
      room: GameRoom;
      players: GamePlayer[];
      player: GamePlayer;
    }>(`/game/rooms/${roomId}/join`, {
      playerId,
      username
    });
  },

  // 离开房间
  leaveRoom: (roomId: string, playerId: string): Promise<any> => {
    return typedPost<any>(`/game/rooms/${roomId}/leave`, {
      playerId
    });
  },

  // 更新玩家分数（如果后端支持）
  updatePlayerScore: (roomId: string, playerId: string, score: number): Promise<any> => {
    return typedPost<any>(`/game/rooms/${roomId}/players/${playerId}/score`, {
      score
    });
  }
};

// 游戏操作相关 API
export const gameApi = {
  // 开始游戏
  startGame: (roomId: string): Promise<GameRoom> => {
    return typedPost<GameRoom>(`/game/rooms/${roomId}/start`);
  },

  // 结束游戏
  endGame: (roomId: string, winnerId?: string): Promise<GameRoom> => {
    return typedPost<GameRoom>(`/game/rooms/${roomId}/end`, { winnerId });
  },

  // 猜测人物
  guessPerson: (roomId: string, playerId: string, personId: number): Promise<any> => {
    return typedPost<any>(`/game/rooms/${roomId}/guess`, {
      playerId,
      personId
    });
  },

  // 获取游戏状态
  getGameStatus: (roomId: string): Promise<any> => {
    return typedGet<any>(`/game/rooms/${roomId}/status`);
  }
};

// 历史人物相关 API
export const historicalPersonApi = {
  // 模糊搜索历史人物
  search: (name: string): Promise<HistoricalPerson[]> => {
    return typedGet<HistoricalPerson[]>(`/historical-persons/search?name=${encodeURIComponent(name)}`);
  },

  // 获取随机历史人物
  getRandom: (): Promise<HistoricalPerson> => {
    return typedGet<HistoricalPerson>('/historical-persons/random');
  },

  // 根据 ID 获取历史人物详情
  getById: (id: number): Promise<HistoricalPerson> => {
    return typedGet<HistoricalPerson>(`/historical-persons/${id}`);
  },

  // 获取所有历史人物
  getAll: (): Promise<HistoricalPerson[]> => {
    return typedGet<HistoricalPerson[]>('/historical-persons');
  },

  // 添加新历史人物
  add: (person: CreateHistoricalPerson): Promise<HistoricalPerson> => {
    return typedPost<HistoricalPerson>('/historical-persons', person);
  },

  // 删除历史人物 - 改为 DELETE 方法
  delete: (id: number): Promise<void> => {
    return typedDelete<void>(`/historical-persons/${id}`);
  },

  // 批量添加历史人物
  batchAdd: (persons: CreateHistoricalPerson[]): Promise<HistoricalPerson[]> => {
    return typedPost<HistoricalPerson[]>('/historical-persons/batch', persons);
  },

  // 更新历史人物 - 改为 PUT 方法
  update: (id: number, person: HistoricalPerson): Promise<HistoricalPerson> => {
    return typedPut<HistoricalPerson>(`/historical-persons/${id}`, person);
  },
};

// 认证相关 API
export const authApi = {
  // 用户注册
  register: (username: string, password: string): Promise<any> => {
    return typedPost<any>('/auth/register', { username, password });
  },

  // 用户登录
  login: (username: string, password: string): Promise<{ token: string; username: string; isAdmin: number }> => {
    return typedPost<{ token: string; username: string; isAdmin: number }>('/auth/login', { username, password });
  },
};

// 健康检查 API
export const healthApi = {
  checkHealth: (): Promise<any> => {
    return typedGet<any>('/health');
  }
};

// 导出 api 实例
export default api;
