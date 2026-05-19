# CampusHub Local Implementation

“校集 CampusHub”是校园二手交易与学生微服务平台的本地实现原型，用于数据库系统课程设计后续开发演示。

## 模块范围

- 二手商品交易
- 悬赏任务与接单
- 学生技能店铺与预约
- 项目广告、收藏、评论、评价
- 文件资源与业务绑定
- 内容审核、举报、违规与信用分
- 钱包流水与服务费记录
- 本地支付抽象，预留 API-Transfer-Station 支付中心接入点

## 本地运行

### 依赖

- JDK 17+
- Maven 3.9+
- Node.js 20+
- Docker Desktop

### 启动数据库

```bash
docker compose up -d mysql
```

### 启动后端

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

后端默认地址：`http://localhost:8080`。

### 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`。

## 本地账号

演示数据由 Flyway `V2__seed_demo_data.sql` 初始化。默认密码以开发环境哈希形式存储，仅用于本地演示。

## 本地支付说明

当前实现提供 `PaymentProvider` 抽象与 `MockPaymentProvider`：

- `POST /api/payment/service-fees/{feeId}/mock-pay` 创建本地模拟支付；
- `POST /api/payment/service-fees/{feeId}/mock-success` 查询 mock provider 并把待支付服务费幂等标记为 `PAID`；
- 支付成功后会写入 `wallet_flows`，用于演示服务费支付闭环；
- `AlipayPaymentProvider` 只保留生产骨架，不读取、不复制、不保存支付宝密钥。

## 生产部署准备

生产推荐目录为 `/opt/campushub`，部署文件包括：

- `docker-compose.prod.yml`
- `.env.prod.example`
- `backend/Dockerfile`
- `frontend/Dockerfile`
- `frontend/nginx.conf`

生产 compose 包含：

- `campushub-mysql`
- `campushub-backend`
- `campushub-web`

MySQL 不暴露公网端口；前端容器默认只绑定宿主机 `127.0.0.1:18080`，由服务器上的 Caddy/Nginx 反代到公网域名。

### 生产环境变量

复制 `.env.prod.example` 为服务器上的 `.env` 后，必须替换所有 `change-me` 值。不要把真实 `.env`、SMTP 密码、JWT secret、内部支付 token 或任何支付宝密钥提交到 GitHub。

关键变量：

```env
MYSQL_DATABASE=campushub
MYSQL_USER=campushub
MYSQL_PASSWORD=强随机密码
MYSQL_ROOT_PASSWORD=强随机密码
CAMPUSHUB_JWT_SECRET=至少32字节强随机字符串
CAMPUSHUB_MAIL_SMTP_HOST=smtp-relay.brevo.com
CAMPUSHUB_MAIL_SMTP_USERNAME=Brevo SMTP 用户名
CAMPUSHUB_MAIL_SMTP_PASSWORD=Brevo SMTP 密码
CAMPUSHUB_MAIL_FROM=suntomb@suntomb.qzz.io
CAMPUSHUB_MAIL_FROM_NAME=校集 CampusHub
```

### 反代建议

`campushub-web` 已经在容器内把 `/api/` 反代到 `campushub-backend:8080`。服务器公网反代可指向宿主机本地端口：

```caddyfile
ustc.suntomb.qzz.io {
    reverse_proxy 127.0.0.1:18080
}
```

也可以让 Caddy 接入 `campushub` Docker network 后直接反代容器服务。

### GitHub 到服务器部署流程

建议生产部署按以下顺序执行：

1. 本地确认代码不包含真实 `.env`、SMTP 密码、JWT secret、内部支付 token 或支付宝密钥。
2. 提交代码并推送到 GitHub 仓库。
3. 在服务器 `/opt/campushub` 拉取仓库代码。
4. 在服务器创建真实 `.env`，内容参考 `.env.prod.example`，并替换所有 `change-me` 占位符。
5. 使用 `docker-compose.prod.yml` 启动 `campushub-mysql`、`campushub-backend`、`campushub-web`。
6. 配置现有 Caddy/Nginx，把 `https://ustc.suntomb.qzz.io` 反代到 `127.0.0.1:18080` 或容器网络内的 `campushub-web:80`。
7. 用浏览器访问域名，验证首页、注册/登录、API 反代、钱包页和本地 mock 支付演示。

## API-Transfer-Station 支付中心边界

生产支付宝收款应由 API-Transfer-Station 作为支付中心负责，CampusHub 不直接接触支付宝私钥/公钥正文。

CampusHub 负责：

- 创建本地钱包充值或服务费支付订单；
- 调用支付中心内部接口创建收款单；
- 接收支付中心内部回调并校验内部 token；
- 校验订单号、金额和状态，幂等更新本地订单、钱包余额和流水。

API-Transfer-Station 负责：

- 保存支付宝 APPID、应用私钥、支付宝公钥等敏感配置；
- 调用支付宝官方电脑网站支付；
- 接收支付宝异步通知、验签、校验金额和幂等；
- 把支付结果转发给 CampusHub 内部回调。

安全要求：

- 不要读取、打印、复制、提交或写入 `/opt/ai-relay/secrets/alipay/` 下的密钥正文；
- 排查时最多检查文件是否存在、权限和挂载路径，不输出文件内容；
- 内部 token 只保存在服务器 `.env` 或 secret 管理中；
- 日志不要打印 token、完整支付宝回调参数或用户敏感信息。
