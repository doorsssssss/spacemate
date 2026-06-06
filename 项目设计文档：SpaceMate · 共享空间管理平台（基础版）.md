# 项目设计文档：SpaceMate · 共享空间管理平台（基础版）

## 文档信息

| 项 | 内容 |
| --- | --- |
| 文档版本 | v1.2.0 |
| 文档状态 | Draft（可直接指导开发） |
| 最后更新 | 2026-06-04 |
| 作者 | 项目组 |

### 修订历史

| 版本 | 日期 | 修订人 | 说明 |
| --- | --- | --- | --- |
| v1.0.0 | YYYY-MM-DD | 初版作者 | 初始设计文档 |
| v1.1.0 | 2026-05-17 | 架构整理 | 规范化结构、补全接口示例、重构表结构与约束、补充鉴权/日志/部署方案 |
| v1.2.0 | 2026-06-04 | 架构整理 | [精进] 补充空间评论、回复、点赞能力，同步更新数据库、接口与页面设计 |

---

## 一、项目背景与目标

### 1.1 背景

社区自习室、共享图书馆、活动室等小微空间日益增多，但普遍缺乏数字化管理工具。管理者依赖手工登记或微信群，效率低、易出错。  
本项目旨在提供一个轻量级、易用的共享空间管理平台，先实现基础信息化（座位管理、预约管理、用户管理），为后续 AI 客服与智能调度能力打下数据和服务基础。

### 1.2 目标（MVP 基础版）

1. 提供管理端（Web 后台），供管理员管理空间、座位、预约、用户。
2. 提供客户端（H5/小程序风格页面），供用户浏览座位、提交预约、查看自己的预约。
3. [精进] 完成核心增删改查功能，并支持空间评论、回复、点赞等基础互动能力。
4. 不包含 AI 能力，但预留可扩展接口。

### 1.3 后续扩展计划（AI 版）

在基础版 API 上新增 `/api/chat`，对接大模型 + 知识库 + 函数调用，实现智能问答、自动推荐座位、辅助预约创建。

### 1.4 范围说明

1. In Scope：空间、座位、预约、用户、空间评论、管理端和客户端页面、基础鉴权、健康检查。
2. Out of Scope：支付、营销券、复杂会员体系、多门店多租户深度隔离、AI 运营分析。
3. [假设] 当前为单机构部署，后续可升级为多租户。

---

## 二、技术栈

| 层次 | 技术选型 |
| --- | --- |
| 后端框架 | Spring Boot 2.7+ |
| ORM | MyBatis-Plus |
| 数据库 | MySQL 8.0 |
| 前端（管理端） | Thymeleaf + Bootstrap（或 Vue + Element UI） |
| 前端（客户端） | HTML + CSS + JavaScript（响应式） |
| 接口协议 | RESTful API |
| 构建与协作 | Maven、Git、Postman |

### 2.1 分层规范（维护性要求）

| 分层 | 职责 |
| --- | --- |
| Controller | 路由入口、参数校验、统一响应 |
| Service | 业务规则、事务、权限判断 |
| Mapper | SQL 映射、复杂查询 |
| Entity | 表映射模型 |
| DTO/VO | 入参与出参对象，避免直接暴露 Entity |
| Config | 安全、日志、跨域、异常处理 |

约束：

1. Controller 不直接写 SQL。
2. Service 不返回数据库实体给前端。
3. 所有接口返回统一结构。

---

## 三、功能模块

### 3.1 管理端（管理员）

| 功能 | 描述 | CRUD |
| --- | --- | --- |
| 空间管理 | 添加/编辑/删除空间（名称、营业时间、WiFi 信息、规则） | 是 |
| 座位管理 | 查看/添加/编辑/删除座位（座位号、所属空间、插座、安静区） | 是 |
| 预约管理 | 查看所有预约，按状态筛选，取消或标记已使用 | 是 |
| 用户管理 | 查看用户、禁用/启用用户 | 是 |
| 空间评论 | [精进][假设] 评论由客户端直接发布、回复、点赞；基础版暂不提供后台审核页 | 否 |
| 统计看板 | 今日预约数、座位占用率（可选） | 否 |

### 3.2 客户端（普通用户）

| 功能 | 描述 | CRUD |
| --- | --- | --- |
| 查看空间 | 查看空间基本信息（营业时间、价格、规则） | 读 |
| 查看座位 | 按日期/时间段查询可用座位（可筛选插座/安静区） | 读 |
| 创建预约 | 选择座位与时间段，提交预约 | 创建 |
| 我的预约 | 查看/取消自己的预约（仅未开始） | 读、取消 |
| 空间评论 | [精进] 查看空间评论，发布一级评论、回复评论、点赞/取消点赞 | 读、创建、更新 |
| 用户信息 | 手机号登录（简化）与历史记录展示 | 读 |

注：基础版支持“手机号验证码”或“手机号+会话”模式，后续可升级 Spring Security + JWT。

---

## 四、数据库设计（重点已规范）

### 4.1 设计原则

1. 统一审计字段：`created_at`、`updated_at`、`deleted`、`deleted_at`。
2. 统一主键：`BIGINT UNSIGNED AUTO_INCREMENT`。
3. 软删除为默认删除策略，物理删除仅用于运维清理。
4. 避免关键字冲突：用户表命名为 `app_user`。
5. 时间段统一用 `start_at`、`end_at`（DATETIME）避免跨天查询复杂化。

### 4.2 关系模型

```text
space (1) ---- (N) seat
seat  (1) ---- (N) booking
app_user (1) -- (N) booking
space (1) ---- (N) space_comment
space_comment (1) ---- (N) space_comment（回复树）
space_comment (1) ---- (N) space_comment_like
app_user (1) -- (N) space_comment
app_user (1) -- (N) space_comment_like
```

### 4.3 表结构总览

#### 4.3.1 `space` 空间表

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint unsigned | PK, auto | 空间 ID |
| code | varchar(32) | unique, not null | 空间编码（业务唯一） |
| name | varchar(100) | not null | 空间名称 |
| open_start_time | time | not null | 营业开始时间 |
| open_end_time | time | not null | 营业结束时间 |
| wifi_ssid | varchar(64) | null | WiFi 名称 |
| wifi_password | varchar(128) | null | WiFi 密码（建议加密存储） |
| rules | text | null | 规则说明（展示前需 XSS 过滤） |
| price_hourly | decimal(10,2) | not null default 0 | 每小时价格 |
| status | tinyint | not null default 1 | 1=启用,0=停用 |
| deleted | tinyint | not null default 0 | 软删除标记 |
| deleted_at | datetime(3) | null | 删除时间 |
| created_at | datetime(3) | not null default current_timestamp(3) | 创建时间 |
| updated_at | datetime(3) | not null default current_timestamp(3) on update current_timestamp(3) | 更新时间 |

索引建议：

1. `uk_space_code (code)`
2. `idx_space_status_deleted (status, deleted)`
3. `idx_space_name_deleted (name, deleted)`

#### 4.3.2 `seat` 座位表

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint unsigned | PK, auto | 座位 ID |
| space_id | bigint unsigned | FK | 所属空间 |
| seat_number | varchar(20) | not null | 座位号，如 A01 |
| has_socket | tinyint | not null default 0 | 1=有插座 |
| is_quiet | tinyint | not null default 0 | 1=安静区 |
| status | tinyint | not null default 1 | 1=可用,0=停用 |
| deleted | tinyint | not null default 0 | 软删除标记 |
| deleted_at | datetime(3) | null | 删除时间 |
| created_at | datetime(3) | not null default current_timestamp(3) | 创建时间 |
| updated_at | datetime(3) | not null default current_timestamp(3) on update current_timestamp(3) | 更新时间 |

索引建议：

1. `uk_seat_space_no_deleted (space_id, seat_number, deleted)`
2. `idx_seat_space_status_deleted (space_id, status, deleted)`
3. `idx_seat_feature_deleted (has_socket, is_quiet, deleted)`

外键：

1. `fk_seat_space`：`space_id -> space.id`，`ON UPDATE CASCADE ON DELETE RESTRICT`

#### 4.3.3 `app_user` 用户表

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint unsigned | PK, auto | 用户 ID |
| phone | varchar(20) | not null | 手机号（唯一） |
| nickname | varchar(50) | null | 昵称 |
| role | tinyint | not null default 1 | 1=普通用户,9=管理员 |
| status | tinyint | not null default 1 | 1=启用,0=禁用 |
| last_login_at | datetime(3) | null | 最近登录时间 |
| deleted | tinyint | not null default 0 | 软删除标记 |
| deleted_at | datetime(3) | null | 删除时间 |
| created_at | datetime(3) | not null default current_timestamp(3) | 创建时间 |
| updated_at | datetime(3) | not null default current_timestamp(3) on update current_timestamp(3) | 更新时间 |

索引建议：

1. `uk_user_phone_deleted (phone, deleted)`
2. `idx_user_role_status_deleted (role, status, deleted)`

#### 4.3.4 `booking` 预约表

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint unsigned | PK, auto | 预约 ID |
| booking_no | varchar(32) | unique, not null | 预约单号 |
| user_id | bigint unsigned | FK | 用户 ID |
| space_id | bigint unsigned | FK | 空间 ID（冗余，便于统计） |
| seat_id | bigint unsigned | FK | 座位 ID |
| start_at | datetime(3) | not null | 开始时间 |
| end_at | datetime(3) | not null | 结束时间 |
| status | tinyint | not null default 1 | 1=待使用,2=已使用,3=已取消 |
| confirm_code | varchar(20) | unique | 核验码 |
| cancel_reason | varchar(255) | null | 取消原因 |
| deleted | tinyint | not null default 0 | 软删除标记 |
| deleted_at | datetime(3) | null | 删除时间 |
| created_at | datetime(3) | not null default current_timestamp(3) | 创建时间 |
| updated_at | datetime(3) | not null default current_timestamp(3) on update current_timestamp(3) | 更新时间 |

索引建议：

1. `uk_booking_no (booking_no)`
2. `idx_booking_user_start_status_del (user_id, start_at, status, deleted)`
3. `idx_booking_seat_time_status_del (seat_id, start_at, end_at, status, deleted)`
4. `idx_booking_space_date_del (space_id, start_at, deleted)`

外键：

1. `fk_booking_user`：`user_id -> app_user.id`，`ON UPDATE CASCADE ON DELETE RESTRICT`
2. `fk_booking_space`：`space_id -> space.id`，`ON UPDATE CASCADE ON DELETE RESTRICT`
3. `fk_booking_seat`：`seat_id -> seat.id`，`ON UPDATE CASCADE ON DELETE RESTRICT`

#### 4.3.5 `space_comment` 空间评论表

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint unsigned | PK, auto | 评论 ID |
| space_id | bigint unsigned | FK | 所属空间 |
| user_id | bigint unsigned | FK | 评论用户 |
| parent_id | bigint unsigned | not null default 0 | 父评论 ID，0 表示一级评论 |
| root_id | bigint unsigned | not null default 0 | 根评论 ID，用于归类回复树 |
| content | varchar(500) | not null | 评论内容 |
| like_count | bigint unsigned | not null default 0 | 点赞数 |
| reply_count | bigint unsigned | not null default 0 | 回复数 |
| status | tinyint | not null default 1 | 1=正常,0=隐藏/停用 |
| deleted | tinyint | not null default 0 | 软删除标记 |
| deleted_at | datetime(3) | null | 删除时间 |
| created_at | datetime(3) | not null default current_timestamp(3) | 创建时间 |
| updated_at | datetime(3) | not null default current_timestamp(3) on update current_timestamp(3) | 更新时间 |

[精进] 索引建议：

1. `idx_space_comment_root_page (space_id, parent_id, status, deleted, created_at, id)`
2. `idx_space_comment_reply_tree (space_id, root_id, status, deleted, created_at, id)`
3. `idx_space_comment_user (user_id, deleted)`

[精进] 外键：

1. `fk_space_comment_space`：`space_id -> space.id`，`ON UPDATE CASCADE ON DELETE RESTRICT`
2. `fk_space_comment_user`：`user_id -> app_user.id`，`ON UPDATE CASCADE ON DELETE RESTRICT`

#### 4.3.6 `space_comment_like` 空间评论点赞表

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | bigint unsigned | PK, auto | 点赞记录 ID |
| comment_id | bigint unsigned | FK | 评论 ID |
| user_id | bigint unsigned | FK | 点赞用户 |
| liked | tinyint | not null default 1 | 1=已点赞,0=已取消 |
| deleted | tinyint | not null default 0 | 软删除标记 |
| deleted_at | datetime(3) | null | 删除时间 |
| created_at | datetime(3) | not null default current_timestamp(3) | 创建时间 |
| updated_at | datetime(3) | not null default current_timestamp(3) on update current_timestamp(3) | 更新时间 |

[精进] 索引建议：

1. `uk_space_comment_like_user (comment_id, user_id, deleted)`
2. `idx_space_comment_like_user_liked (user_id, liked, deleted)`
3. `idx_space_comment_like_comment_liked (comment_id, liked, deleted)`

[精进] 外键：

1. `fk_space_comment_like_comment`：`comment_id -> space_comment.id`，`ON UPDATE CASCADE ON DELETE RESTRICT`
2. `fk_space_comment_like_user`：`user_id -> app_user.id`，`ON UPDATE CASCADE ON DELETE RESTRICT`

### 4.4 级联行为与删除策略（避免后续大改）

1. 删除空间：不物理删除，执行软删除事务。同步将该空间座位软删除，并取消未来未开始预约（状态改为已取消）。
2. 删除座位：不物理删除，执行软删除并取消未来未开始预约。
3. [精进] 评论建议默认保留历史记录，仅通过状态与软删除控制前端展示，避免用户讨论内容随空间/座位调整被误删。
4. 删除用户：原则上不删，仅禁用；若软删除，保留历史预约与评论记录用于审计。

### 4.5 推荐 DDL（可直接执行）

```sql
CREATE TABLE space (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(100) NOT NULL,
  open_start_time TIME NOT NULL,
  open_end_time TIME NOT NULL,
  wifi_ssid VARCHAR(64) NULL,
  wifi_password VARCHAR(128) NULL,
  rules TEXT NULL,
  price_hourly DECIMAL(10,2) NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  deleted_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  CONSTRAINT uk_space_code UNIQUE (code),
  INDEX idx_space_status_deleted (status, deleted),
  INDEX idx_space_name_deleted (name, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE seat (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  space_id BIGINT UNSIGNED NOT NULL,
  seat_number VARCHAR(20) NOT NULL,
  has_socket TINYINT NOT NULL DEFAULT 0,
  is_quiet TINYINT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  deleted_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  CONSTRAINT uk_seat_space_no_deleted UNIQUE (space_id, seat_number, deleted),
  CONSTRAINT fk_seat_space FOREIGN KEY (space_id) REFERENCES space(id) ON UPDATE CASCADE ON DELETE RESTRICT,
  INDEX idx_seat_space_status_deleted (space_id, status, deleted),
  INDEX idx_seat_feature_deleted (has_socket, is_quiet, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE app_user (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  phone VARCHAR(20) NOT NULL,
  nickname VARCHAR(50) NULL,
  role TINYINT NOT NULL DEFAULT 1,
  status TINYINT NOT NULL DEFAULT 1,
  last_login_at DATETIME(3) NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  deleted_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  CONSTRAINT uk_user_phone_deleted UNIQUE (phone, deleted),
  INDEX idx_user_role_status_deleted (role, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE booking (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  booking_no VARCHAR(32) NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  space_id BIGINT UNSIGNED NOT NULL,
  seat_id BIGINT UNSIGNED NOT NULL,
  start_at DATETIME(3) NOT NULL,
  end_at DATETIME(3) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  confirm_code VARCHAR(20) NULL,
  cancel_reason VARCHAR(255) NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  deleted_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  CONSTRAINT uk_booking_no UNIQUE (booking_no),
  CONSTRAINT uk_booking_confirm_code UNIQUE (confirm_code),
  CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_booking_space FOREIGN KEY (space_id) REFERENCES space(id) ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_booking_seat FOREIGN KEY (seat_id) REFERENCES seat(id) ON UPDATE CASCADE ON DELETE RESTRICT,
  INDEX idx_booking_user_start_status_del (user_id, start_at, status, deleted),
  INDEX idx_booking_seat_time_status_del (seat_id, start_at, end_at, status, deleted),
  INDEX idx_booking_space_date_del (space_id, start_at, deleted),
  CHECK (start_at < end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 五、API 接口设计（RESTful）

统一响应格式：

```json
{
  "code": 200,
  "message": "ok",
  "data": {},
  "traceId": "a1b2c3d4"
}
```

### 5.1 请求头规范

| Header | 必填 | 适用范围 | 说明 |
| --- | --- | --- | --- |
| Content-Type: application/json | POST/PUT 必填 | 全部写接口 | JSON 请求体 |
| X-Request-Id | 否 | 全部接口 | 链路追踪 ID |
| X-Admin-Token | 管理端必填 | `/admin/**` | 管理端固定令牌 |
| X-User-Phone | 客户端预约接口建议 | `/api/bookings/**` | 用户身份标识（基础版） |
| Authorization: Bearer xxx | 预留 | 二期 JWT | 升级方案 |

### 5.2 错误码

| code | 含义 | 说明 |
| --- | --- | --- |
| 200 | 成功 | 请求成功 |
| 400 | 参数错误 | 参数缺失或格式非法 |
| 401 | 未授权 | 鉴权头缺失或无效 |
| 403 | 禁止访问 | 越权或用户禁用 |
| 404 | 资源不存在 | 记录不存在 |
| 409 | 业务冲突 | 时间冲突、重复提交 |
| 500 | 服务异常 | 未处理异常 |

### 5.3 公共接口（客户端/管理端可读）

| 方法 | 路径 | 描述 |
| --- | --- | --- |
| GET | `/api/spaces` | 获取空间列表 |
| GET | `/api/spaces/{id}` | 获取空间详情 |
| GET | `/api/seats/available` | 查询可用座位 |
| GET | `/api/seats/{id}` | 座位详情 |

#### 示例 1：查询空间列表

请求：

```http
GET /api/spaces HTTP/1.1
Host: localhost:8080
X-Request-Id: req-1001
```

响应：

```json
{
  "code": 200,
  "message": "ok",
  "data": [
    {
      "id": 1,
      "code": "SPACE_001",
      "name": "静心自习室",
      "openStartTime": "08:00:00",
      "openEndTime": "23:00:00",
      "priceHourly": 8.50
    }
  ],
  "traceId": "a8ce9f2b0c1e4d5a"
}
```

#### 示例 2：查询可用座位

请求：

```http
GET /api/seats/available?spaceId=1&startAt=2026-05-20T14:00:00&endAt=2026-05-20T16:00:00&hasSocket=1&isQuiet=1 HTTP/1.1
Host: localhost:8080
```

响应：

```json
{
  "code": 200,
  "message": "ok",
  "data": [
    {
      "seatId": 101,
      "seatNumber": "A01",
      "hasSocket": true,
      "isQuiet": true
    }
  ],
  "traceId": "9c04a1418b2b4cef"
}
```

### 5.4 客户端预约接口

| 方法 | 路径 | 描述 |
| --- | --- | --- |
| POST | `/api/bookings` | 创建预约 |
| GET | `/api/bookings/my` | 查询我的预约 |
| DELETE | `/api/bookings/{id}` | 取消预约 |

#### 示例 3：创建预约

请求：

```http
POST /api/bookings HTTP/1.1
Host: localhost:8080
Content-Type: application/json
X-User-Phone: 13800000000

{
  "seatId": 101,
  "startAt": "2026-05-20T14:00:00",
  "endAt": "2026-05-20T16:00:00"
}
```

响应：

```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "bookingId": 9001,
    "bookingNo": "BK202605170001",
    "status": 1,
    "confirmCode": "583921"
  },
  "traceId": "5757e5595b0d4d51"
}
```

### 5.5 管理端接口（需鉴权）

| 方法 | 路径 | 描述 |
| --- | --- | --- |
| POST | `/admin/spaces` | 新增空间 |
| PUT | `/admin/spaces/{id}` | 修改空间 |
| DELETE | `/admin/spaces/{id}` | 删除空间（软删除） |
| POST | `/admin/seats` | 新增座位 |
| PUT | `/admin/seats/{id}` | 修改座位 |
| DELETE | `/admin/seats/{id}` | 删除座位（软删除） |
| GET | `/admin/bookings` | 预约列表（分页筛选） |
| PUT | `/admin/bookings/{id}/status` | 修改预约状态 |
| GET | `/admin/users` | 用户列表 |
| PUT | `/admin/users/{id}/status` | 禁用/启用用户 |

管理端请求示例：

```http
PUT /admin/bookings/9001/status HTTP/1.1
Host: localhost:8080
Content-Type: application/json
X-Admin-Token: ${ADMIN_TOKEN}

{
  "status": 2
}
```

---

## 六、管理端界面设计（页面草图）

### 6.1 登录页

1. 用户名/密码登录。
2. [假设] 基础版可先使用固定管理员账户，生产建议改为数据库管理员账户并加密存储密码。

### 6.2 主页面

1. 顶部导航：空间管理、座位管理、预约管理、用户管理。
2. 左侧筛选：按空间、状态、时间范围过滤。
3. 表格统一分页，默认 `pageSize=20`。

### 6.3 分页功能页

1. 空间管理页：新增、编辑、删除（软删除）。
2. 座位管理页：先选空间，再管理座位。
3. 预约管理页：筛选状态，支持手动取消/标记已使用。
4. 用户管理页：查看用户并启用/禁用。

---

## 七、客户端界面设计（移动优先）

### 7.1 首页

展示空间简介、营业时间、价格、规则，提供“查看座位并预约”入口。

### 7.2 座位查询页

1. 选择时间段（开始/结束）。
2. 可筛选：有插座、安静区。
3. 返回可用座位列表并支持立即预约。

### 7.3 空间评论区

1. [精进] 空间详情页底部展示评论区，支持查看评论和回复。
2. [精进] 登录用户或已绑定手机号的用户可发布一级评论、回复评论、点赞/取消点赞。
3. [假设] 评论仅支持纯文本，不支持图片、富文本和附件，以降低复杂度与 XSS 风险。

### 7.4 预约提交页

1. 填写手机号。
2. 确认座位与时间段。
3. 提交后返回预约单号与确认码。

### 7.5 我的预约页

1. 展示当前用户预约列表。
2. 未开始预约支持取消。

---

## 八、后续 AI 集成预留设计

### 8.1 接口预留

`POST /api/chat`

请求示例：

```json
{
  "message": "今天下午有安静区座位吗？",
  "userId": 1,
  "phone": "13800000000"
}
```

### 8.2 数据预留

1. `faq`：问题、答案、分类、启用状态、审计字段。
2. `conversation_log`：用户问题、AI 回复、函数调用记录、满意度、时间戳。

### 8.3 服务层预留

将以下逻辑抽象为独立 Service，供 AI 复用：

1. `query_available_seats`
2. `create_booking`
3. `get_space_info`

---

## 九、开发计划（分阶段）

| 阶段 | 时间 | 任务 | 产出 |
| --- | --- | --- | --- |
| 第 1 周 | 3 天 | 环境搭建、数据库建表、MyBatis-Plus 配置 | 服务可连库 |
| 第 2 周 | 4 天 | 管理端空间/座位/用户 CRUD | 管理端 API 可用 |
| 第 3 周 | 3 天 | 客户端预约创建/查询/取消、空间评论/回复/点赞 | 客户端 API 可用 |
| 第 4 周 | 4 天 | 管理端页面开发 | 管理后台可操作 |
| 第 5 周 | 3 天 | 客户端 H5 页面开发 | 移动端可使用 |
| 第 6 周 | 2 天 | 联调、测试、修复、文档完善 | 可演示版本 |

AI 功能可在基础版完成后追加 2-3 周。

### 9.1 测试与质量门禁

1. 单元测试覆盖核心预约冲突判断。
2. 集成测试覆盖创建预约、取消预约、越权访问、禁用用户限制。
3. 上线前执行接口回归测试（Postman Collection）。

### 9.2 性能目标

1. 查询类接口：P95 <= 300ms。
2. 写入类接口：P95 <= 500ms。
3. 预约冲突查询 SQL 单次执行建议 <= 50ms（慢查询阈值 200ms）。

---

## 十、总结

本版文档已满足“可直接开发 + 尽量避免后续大改”的要求，重点完成了：

1. 规范化的数据库设计（命名、类型、索引、外键、软删除、时间戳）。
2. 明确的 API 规范（请求头、错误码、请求/响应示例）。
3. 清晰的权限边界（管理端与客户端隔离）。
4. 落地级非功能要求（安全、性能、可维护、可扩展）。

下一步建议：

1. 先执行第 4 章 DDL 建库。
2. 先实现 `GET /api/spaces` 和 `POST /api/bookings` 两个主链路接口。
3. 同步落地全局异常处理和统一日志，避免后期返工。
