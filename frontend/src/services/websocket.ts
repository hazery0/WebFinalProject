// src/services/websocket.ts
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class WebSocketService {
  private client: Client | null = null;
  private _isConnected = false; // 改为私有属性，加下划线前缀
  private subscriptions: Array<{ destination: string; callback: Function; id?: string }> = [];
  private subscriptionMap: Map<string, any> = new Map();
  private messageHandlers: Map<string, Function[]> = new Map();

  constructor() {
    this.initClient();
  }

  private initClient() {
    this.client = new Client({
      webSocketFactory: () => {
        const playerId = localStorage.getItem('tempPlayerId') || 'unknown';
        const url = `/ws-game?playerId=${playerId}`;

        const sock = new SockJS(url, null, {
          transports: ['websocket', 'xhr-streaming', 'xhr-polling'],
          withCredentials: false
        });
        return sock;
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    this.client.onConnect = (frame) => {
      console.log('WebSocket连接成功');
      this._isConnected = true;

      // 重新连接时重新订阅
      this.subscriptions.forEach(sub => {
        if (sub.id) {
          const subscription = this.doSubscribe(sub.destination, sub.callback);
          if (subscription) {
            this.subscriptionMap.set(sub.id, subscription);
          }
        }
      });

      this.triggerEvent('connected', frame);
    };

    this.client.onStompError = (frame) => {
      console.error('WebSocket错误:', frame);
      this.triggerEvent('error', frame);
    };

    this.client.onDisconnect = (frame) => {
      console.log('WebSocket断开连接:', frame);
      this._isConnected = false;
      this.subscriptionMap.clear();
      this.triggerEvent('disconnected', frame);
    };

    this.client.onWebSocketError = (error) => {
      console.error('WebSocket连接错误:', error);
    };
  }

  connect(headers: any = {}) {
    if (this.client) {
      const token = localStorage.getItem('token');
      const authHeaders = token ? { Authorization: `Bearer ${token}` } : {};

      this.client.connectHeaders = {
        ...authHeaders,
        ...headers
      };

      if (!this._isConnected) {
        this.client.activate();
      } else {
        console.warn('WebSocket 已经是连接状态');
      }
    }
  }

  disconnect() {
    if (this.client && this._isConnected) {
      this.client.deactivate();
      this._isConnected = false;
      this.subscriptionMap.clear();
    }
  }

  subscribe(destination: string, callback: Function): string {
    const subscriptionId = `sub_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    const subscriptionInfo = { destination, callback, id: subscriptionId };
    this.subscriptions.push(subscriptionInfo);

    if (this._isConnected && this.client) {
      const subscription = this.doSubscribe(destination, callback);
      if (subscription) {
        this.subscriptionMap.set(subscriptionId, subscription);
      }
    }

    return subscriptionId;
  }

  private doSubscribe(destination: string, callback: Function) {
    if (!this.client) return null;

    try {
      const subscription = this.client.subscribe(destination, (message) => {
        try {
          const body = JSON.parse(message.body);
          console.log(`收到消息 [${destination}]:`, body);
          callback(body);
          this.triggerEvent('message', body);
        } catch (error) {
          console.warn('非JSON消息:', message.body);
          callback(message.body);
        }
      });

      console.log(`订阅成功: ${destination}`);
      return subscription;
    } catch (error) {
      console.error(`订阅失败 [${destination}]:`, error);
      return null;
    }
  }

  // 取消订阅
  unsubscribe(subscriptionId: string): boolean {
    const subscription = this.subscriptionMap.get(subscriptionId);
    if (subscription) {
      try {
        subscription.unsubscribe();
        this.subscriptionMap.delete(subscriptionId);

        const index = this.subscriptions.findIndex(sub => sub.id === subscriptionId);
        if (index !== -1) {
          this.subscriptions.splice(index, 1);
        }

        console.log(`取消订阅成功: ${subscriptionId}`);
        return true;
      } catch (error) {
        console.error(`取消订阅失败 [${subscriptionId}]:`, error);
        return false;
      }
    }

    console.warn(`未找到订阅: ${subscriptionId}`);
    return false;
  }

  // 取消所有订阅
  unsubscribeAll(): void {
    this.subscriptionMap.forEach((subscription, id) => {
      try {
        subscription.unsubscribe();
        console.log(`取消订阅: ${id}`);
      } catch (error) {
        console.error(`取消订阅失败 [${id}]:`, error);
      }
    });

    this.subscriptionMap.clear();
    this.subscriptions = [];
  }

  send(destination: string, body: any): boolean {
    if (!this.client || !this._isConnected) {
      console.error('WebSocket未连接');
      return false;
    }

    try {
      this.client.publish({
        destination,
        body: JSON.stringify(body)
      });
      console.log(`发送消息到 ${destination}:`, body);
      return true;
    } catch (error) {
      console.error('发送消息失败:', error);
      return false;
    }
  }

  // 事件处理
  on(event: string, handler: Function) {
    if (!this.messageHandlers.has(event)) {
      this.messageHandlers.set(event, []);
    }
    this.messageHandlers.get(event)?.push(handler);
  }

  off(event: string, handler: Function) {
    const handlers = this.messageHandlers.get(event);
    if (handlers) {
      const index = handlers.indexOf(handler);
      if (index > -1) {
        handlers.splice(index, 1);
      }
    }
  }

  private triggerEvent(event: string, data?: any) {
    const handlers = this.messageHandlers.get(event);
    if (handlers) {
      handlers.forEach(handler => {
        try {
          handler(data);
        } catch (error) {
          console.error(`事件处理器错误 [${event}]:`, error);
        }
      });
    }
  }

  // ✅ 修改：改为 getter 方法，避免命名冲突
  get isConnected(): boolean {
    return this._isConnected;
  }

  // ✅ 添加一个方法版本，保持向后兼容（如果需要）
  checkConnection(): boolean {
    return this._isConnected;
  }

  // 获取所有活动订阅ID
  getActiveSubscriptions(): string[] {
    return Array.from(this.subscriptionMap.keys());
  }

  // 重新连接
  reconnect(): void {
    console.log('尝试重新连接...');
    this.disconnect();
    setTimeout(() => {
      this.connect();
    }, 1000);
  }
}

export const webSocketService = new WebSocketService();
