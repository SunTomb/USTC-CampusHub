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
- 本地支付抽象，预留支付宝接入点

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

## 支付说明

当前实现只提供本地 mock 支付适配器，用于展示服务费扣取、钱包流水和订单闭环。生产支付宝网页支付需要 HTTPS 回调域名、支付宝应用配置、私钥/公钥、异步通知验签和幂等处理，不包含在本地里程碑内。
