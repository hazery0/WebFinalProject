## 问题分析

错误信息表明，在WebSocket握手请求中，服务器返回的`Access-Control-Allow-Origin`头是通配符`*`，但请求的`credentials mode`是`include`，这是不允许的。这导致了WebSocket连接失败。

## 原因定位

1. **前端问题**：MultiplayerGame.vue组件尝试建立WebSocket连接到`http://localhost:8080/ws`，但请求失败
2. **后端问题**：WebSocket配置中的CORS设置不正确，导致返回了通配符`*`作为`Access-Control-Allow-Origin`头
3. **配置不一致**：SecurityConfig.java中的CORS配置已经正确设置了允许的来源，但WebSocketConfig.java中的配置存在问题

## 修复方案

### 1. 修改WebSocketConfig.java

将WebSocket配置中的`setAllowedOriginPatterns`改为`setAllowedOrigins`，并传入与SecurityConfig.java一致的允许来源列表：

```java
@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    // 注册WebSocket端点，允许前端连接
    registry.addEndpoint("/ws")
            .setAllowedOrigins("http://localhost:5173", "http://localhost:5174")
            .withSockJS(); // 启用SockJS，提供降级支持
}
```

### 2. 验证CORS配置一致性

确保SecurityConfig.java和WebSocketConfig.java中的CORS配置保持一致：

* 允许的来源列表相同

* 都设置了`allowCredentials(true)`

* 允许的HTTP方法包含WebSocket握手所需的方法

### 3. 重启后端服务

修改配置后，需要重启后端服务使配置生效。

## 验证步骤

1. 重启后端服务
2. 打开前端应用
3. 进入多人模式
4. 尝试创建或加入房间
5. 检查浏览器控制台是否还有CORS错误
6. 验证WebSocket连接是否成功建立

## 预期结果

* WebSocket连接成功建立

* 没有CORS相关错误

* 多人模式可以正常创建和加入房间

