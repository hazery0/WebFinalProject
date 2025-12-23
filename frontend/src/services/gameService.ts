import { webSocketService } from './websocket';

class GameService {
  private playerId: string = '';
  private playerName: string = '';
  private roomId: string = '';
  private currentSubscription: any = null;
  private chatSubscription: any = null;
  private isRejoining: boolean = false;
  private isRoomOwner: boolean = false;

  constructor() {
    this.generatePlayerId();
  }

  // 初始化连接
  connect() {
    webSocketService.connect();
    this.initGlobalSubscriptions();
  }

  private generatePlayerId() {
    // 优先使用 storedId，避免刷新页面ID变化
    const storedId = localStorage.getItem('tempPlayerId');
    if (storedId) {
      this.playerId = storedId;
    } else {
      this.playerId = 'player_' + Math.random().toString(36).substr(2, 9);
      localStorage.setItem('tempPlayerId', this.playerId);
    }
  }

  private initGlobalSubscriptions() {
    // 订阅个人消息 (点对点)
    webSocketService.subscribe('/user/queue/game', (message: any) => {
      this.handleGameMessage(message);
    });
  }

  setPlayerInfo(name: string) {
    this.playerName = name;
    const userId = localStorage.getItem('userId');
    const token = localStorage.getItem('token');

    if (userId && token) {
      this.playerId = userId;
    }
  }

  // 创建房间
  createRoom() {
    return new Promise((resolve, reject) => {
      // 设置超时
      const timeout = setTimeout(() => {
        this.off('message', handler);
        webSocketService.off('message', handler);
        reject(new Error('创建房间超时'));
      }, 10000);

      const handler = (message: any) => {
        if (message.type === 'ROOM_CREATED' || message.type === 'ROOM_STATE') {
          this.off('message', handler);
          webSocketService.off('message', handler);
          clearTimeout(timeout);

          this.roomId = message.roomId || message.roomState?.roomId;
          this.isRejoining = false;

          // 创建成功后订阅房间
          this.subscribeToRoom();

          resolve(this.roomId);
        } else if (message.type === 'ERROR') {
          clearTimeout(timeout);
          this.off('message', handler);
          webSocketService.off('message', handler);
          reject(new Error(message.message || '创建房间失败'));
        }
      };

      this.on('message', handler);
      webSocketService.on('message', handler);

      const sent = webSocketService.send('/app/game/createRoom', {
        playerId: this.playerId,
        playerName: this.playerName || '匿名玩家'
      });

      if (!sent) {
        clearTimeout(timeout);
        this.off('message', handler);
        webSocketService.off('message', handler);
        reject(new Error('WebSocket 未连接'));
      }
    });
  }

  // 加入房间
  joinRoom(roomId: string, isRejoin: boolean = false) {
    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        this.off('message', handler);
        webSocketService.off('message', handler);
        reject(new Error('加入房间超时'));
      }, 10000);

      const handler = (message: any) => {
        if (message.type === 'ROOM_STATE' || message.type === 'JOIN_SUCCESS') {
          this.off('message', handler);
          webSocketService.off('message', handler);
          clearTimeout(timeout);

          this.roomId = roomId;
          this.isRejoining = isRejoin;

          // 加入成功后订阅房间
          this.subscribeToRoom();

          resolve(this.roomId);
        } else if (message.type === 'JOIN_FAILED' || message.type === 'ERROR') {
          clearTimeout(timeout);
          this.off('message', handler);
          webSocketService.off('message', handler);
          reject(new Error(message.message || '加入房间失败'));
        }
      };

      this.on('message', handler);
      webSocketService.on('message', handler);

      const payload: any = {
        playerId: this.playerId,
        playerName: this.playerName || '匿名玩家',
        roomId: roomId
      };

      if (isRejoin) {
        payload.isRejoin = true;
      }

      const sent = webSocketService.send('/app/game/joinRoom', payload);

      if (!sent) {
        clearTimeout(timeout);
        this.off('message', handler);
        webSocketService.off('message', handler);
        reject(new Error('WebSocket 未连接'));
      }
    });
  }

  private subscribeToRoom() {
    if (!this.roomId) return;

    console.log(`正在订阅房间 ${this.roomId}`);

    // 取消之前的订阅（如果存在）
    if (this.currentSubscription) {
      webSocketService.unsubscribe(this.currentSubscription);
    }
    if (this.chatSubscription) {
      webSocketService.unsubscribe(this.chatSubscription);
    }

    // 订阅房间广播消息
    this.currentSubscription = webSocketService.subscribe(
      `/topic/game/room/${this.roomId}`,
      (message: any) => {
        console.log('收到房间消息:', message);
        this.handleRoomMessage(message);
      }
    );

    // 订阅房间聊天频道
    this.chatSubscription = webSocketService.subscribe(
      `/topic/game/room/${this.roomId}/chat`,
      (message: any) => {
        console.log('收到聊天消息:', message);
        this.triggerEvent('chatMessage', message);
      }
    );

    console.log(`已成功订阅房间 ${this.roomId} 和聊天频道`);

    // 如果是重新加入，触发需要房间状态的事件
    if (this.isRejoining) {
      this.triggerEvent('roomRejoined', this.roomId);
    }
  }

  leaveRoom() {
    if (this.roomId) {
      console.log(`离开房间 ${this.roomId}`);

      webSocketService.send('/app/game/leaveRoom', {
        playerId: this.playerId,
        roomId: this.roomId,
        wasOwner: this.isRoomOwner
      });

      // 取消订阅
      if (this.currentSubscription) {
        webSocketService.unsubscribe(this.currentSubscription);
        this.currentSubscription = null;
      }
      if (this.chatSubscription) {
        webSocketService.unsubscribe(this.chatSubscription);
        this.chatSubscription = null;
      }

      this.roomId = '';
      this.isRejoining = false;
      this.isRoomOwner = false;
    }
  }

  startGame() {
    webSocketService.send('/app/game/start', {
      playerId: this.playerId,
      roomId: this.roomId
    });
  }

  sendGuess(guess: any) {
  webSocketService.send('/app/game/guess', {
    playerId: this.playerId,
    roomId: this.roomId,
    guess: guess.name || guess,  // 发送名称
    guessData: guess  // 也发送完整对象用于调试
  });
}

  sendChatMessage(message: string) {
    if (!this.roomId) {
      console.error('未加入房间，无法发送聊天消息');
      return;
    }

    webSocketService.send('/app/game/chat', {
      playerId: this.playerId,
      roomId: this.roomId,
      message: message
    });
  }

  // 玩家投降
  surrender() {
    webSocketService.send('/app/game/surrender', {
      playerId: this.playerId,
      roomId: this.roomId
    });
  }

  // 消息分发
  private handleGameMessage(message: any) {
    console.log('--- 收到后端原始消息 ---', message);

    // 处理房主转移
    if (message.type === 'OWNER_TRANSFERRED') {
      if (message.newOwnerId === this.playerId) {
        this.isRoomOwner = true;
        console.log('你已成为新房主');
      } else {
        this.isRoomOwner = false;
      }
      this.triggerEvent('ownerTransferred', message);
    }

    // 处理游戏开始消息
    if (message.type === 'GAME_STARTED') {
      this.triggerEvent('gameStart', message);
    }

    // 触发事件
    this.triggerEvent('message', message);
  }

  private handleRoomMessage(message: any) {
    // 房间广播消息
    this.triggerEvent('roomMessage', message);

    // 如果是游戏开始消息，也触发 gameStart 事件
    if (message.type === 'GAME_STARTED') {
      this.triggerEvent('gameStart', message);
    }
  }

  // 简易事件系统
  private eventHandlers: Map<string, Function[]> = new Map();

  on(event: string, handler: Function) {
    if (!this.eventHandlers.has(event)) {
      this.eventHandlers.set(event, []);
    }
    this.eventHandlers.get(event)?.push(handler);
  }

  off(event: string, handler: Function) {
    const handlers = this.eventHandlers.get(event);
    if (handlers) {
      const index = handlers.indexOf(handler);
      if (index > -1) {
        handlers.splice(index, 1);
      }
    }
  }

  private triggerEvent(event: string, data?: any) {
    const handlers = this.eventHandlers.get(event);
    if (handlers) {
      handlers.forEach(handler => {
        try {
          handler(data);
        } catch (error) {
          console.error(`事件 ${event} 处理错误:`, error);
        }
      });
    }
  }

  // 获取房间列表
  getRooms() {
    webSocketService.send('/app/game/getRooms', {
      playerId: this.playerId
    });
  }

  getPlayerId() { return this.playerId; }
  getRoomId() { return this.roomId; }
  isInRoom() { return !!this.roomId; }
  getIsRoomOwner() { return this.isRoomOwner; }
}

export const gameService = new GameService();
