# Linux虚拟机Nginx代理部署配置

## 一、前端配置

1. **确保生产构建配置正确**：

   * 检查`vite.config.prod.ts`中的`base`配置，确保为`/`或适合部署的子路径

   * 确保构建输出目录为`dist`，便于Nginx访问

2. **构建前端静态文件**：

   * 在前端目录下运行`npm run build`生成生产版本

   * 确认`dist`目录下生成了正确的静态文件

## 二、后端配置

1. **确保后端服务配置正确**：

   * 检查`application.properties`中的端口配置，确保为`8080`

   * 确认没有配置上下文路径，保持默认`/`

   * 确保WebSocket配置正确，允许外部访问

2. **打包后端服务**：

   * 在后端目录下运行`mvn package`生成可执行jar包

   * 确认`target`目录下生成了正确的jar文件

## 三、Nginx配置

1. **安装Nginx**：

   * 在Linux虚拟机上运行`apt-get install nginx`（Ubuntu/Debian）或`yum install nginx`（CentOS/RHEL）

   * 启动Nginx服务：`systemctl start nginx`

   * 设置Nginx开机自启：`systemctl enable nginx`

2. **配置Nginx代理**：

   * 创建或修改Nginx配置文件，例如`/etc/nginx/sites-available/web-final-project`

   * 配置以下内容：

     * 监听80端口

     * 设置根目录为前端`dist`目录

     * 配置`/api`路径代理到后端`http://localhost:8080`

     * 配置WebSocket代理，处理`/ws-game`路径

     * 添加CORS和其他必要的HTTP头

3. **启用Nginx配置**：

   * 创建符号链接：`ln -s /etc/nginx/sites-available/web-final-project /etc/nginx/sites-enabled/`

   * 测试配置：`nginx -t`

   * 重新加载配置：`systemctl reload nginx`

## 四、部署步骤

1. **上传文件到Linux虚拟机**：

   * 将前端`dist`目录上传到指定位置，例如`/var/www/web-final-project`

   * 将后端jar包上传到指定位置，例如`/opt/web-final-project`

2. **启动后端服务**：

   * 使用`java -jar backend.jar`启动后端服务

   * 建议使用systemd或pm2等工具管理服务，确保服务稳定运行

3. **验证部署**：

   * 访问虚拟机IP地址，确认前端页面正常显示

   * 测试API调用和WebSocket连接，确保功能正常

## 五、注意事项

1. **防火墙配置**：确保Linux虚拟机的80端口（Nginx）和8080端口（后端）允许外部访问
2. **数据库配置**：如果使用外部数据库，需要确保数据库服务正常运行，并且后端配置正确
3. **SSL配置**：如果需要HTTPS访问，可以在Nginx中配置SSL证书
4. **日志管理**：定期查看Nginx和后端服务日志，以便排查问题

## 六、配置文件示例

### Nginx配置示例

```nginx
server {
    listen 80;
    server_name your-domain.com;
    root /var/www/web-final-project/dist;
    index index.html;

    # 处理前端路由
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API代理
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket代理
    location /ws-game {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 后端systemd服务配置示例

```
[Unit]
Description=Web Final Project Backend
After=network.target

[Service]
User=www-data
WorkingDirectory=/opt/web-final-project
ExecStart=/usr/bin/java -jar backend.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

