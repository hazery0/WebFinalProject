import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';
import LoginView from '../views/LoginView.vue';
import RegisterView from '../views/RegisterView.vue';
import HistoricalFigureGame from '../views/HistoricalFigureGame.vue';
import GameStart from '../views/GameStart.vue';
import AdminView from '../views/AdminView.vue';
import GameModeSelect from '../views/GameModeSelect.vue';
import MultiplayerMode from '../views/MultiplayerMode.vue';
import MultiplayerGame from '../views/MultiplayerGame.vue';

const routes = [
  {
    path: '/',
    redirect: '/start', // 重定向到游戏开始页面
  },
  {
    path: '/start',
    name: 'game-start',
    component: GameStart,
  },
  {
    path: '/mode-select',
    name: 'mode-select',
    component: GameModeSelect,
  },
  {
    path: '/multiplayer',
    name: 'multiplayer',
    component: MultiplayerMode,
  },
  {
    path: '/multiplayer-room/:roomId',
    name: 'multiplayer-game',
    component: MultiplayerGame,
  },
  {
    path: '/home',
    name: 'home',
    redirect: '/start', // 登录后重定向到游戏开始页面
  },
  {
    path: '/game',
    name: 'historical-game',
    component: HistoricalFigureGame,
  },
  {
    path: '/admin',
    name: 'admin',
    component: AdminView,
    meta: { requiresAdmin: true }, // 标记需要管理员权限
  },
  {
    path: '/login',
    name: 'login',
    component: LoginView,
  },
  {
    path: '/register',
    name: 'register',
    component: RegisterView,
  },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
});

// 路由守卫，防止非管理员访问管理员页面
router.beforeEach((to, from, next) => {
  // 检查路由是否需要管理员权限
  if (to.meta.requiresAdmin) {
    // 从localStorage获取isAdmin状态
    const isAdmin = localStorage.getItem('isAdmin') === '1';
    if (isAdmin) {
      // 是管理员，允许访问
      next();
    } else {
      // 不是管理员，重定向到首页
      next('/start');
    }
  } else {
    // 不需要管理员权限的路由，直接放行
    next();
  }
});

export default router;
