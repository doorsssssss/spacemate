# 项目 API 接口总览（SpaceMate v1）

## 公共约定

- 基础路径：`/api/v1`
- 内容类型：`application/json`
- 编码：`UTF-8`
- 时间格式：`ISO-8601`（如 `2026-05-20T14:00:00+08:00`）
- 鉴权：
  - 公开接口无需鉴权
  - 用户接口需 `Authorization: Bearer <access_token>` 或基础版 `X-User-Phone`
  - 管理接口需 `X-Admin-Token: <token>`（基础版）或管理员 JWT
- 请求头约定：
  - `[精进] X-User-Phone`：客户端基础版身份标识，适用于预约、评论、点赞等需要区分用户的接口
  - `[精进] X-Admin-Token`：管理端基础版固定令牌，仅用于后台接口
  - `[精进] Authorization: Bearer <token>`：预留给后续 JWT 升级
- 统一响应结构：

```json
{
  "code": 200,
  "message": "ok",
  "data": {},
  "traceId": "a1b2c3d4e5f6"
}
```

- 统一错误结构（示例）：

```json
{
  "code": 409,
  "message": "该时段座位已被预约",
  "path": "/api/v1/bookings",
  "timestamp": "2026-05-17T18:00:00+08:00",
  "traceId": "a1b2c3d4e5f6"
}
```

## 状态与枚举约定

- 用户状态：`1=ACTIVE`，`0=DISABLED`
- 空间状态：`1=ACTIVE`，`0=DISABLED`
- 座位状态：`1=ACTIVE`，`0=DISABLED`
- 预约状态：`1=PENDING`，`2=USED`，`3=CANCELLED`

## 错误码

| code | 含义 | 场景 |
| --- | --- | --- |
| 200 | 成功 | 请求成功 |
| 400 | 参数错误 | 参数缺失、格式不合法 |
| 401 | 未授权 | token 无效或缺失 |
| 403 | 禁止访问 | 越权、用户禁用 |
| 404 | 资源不存在 | 空间/座位/预约不存在 |
| 409 | 业务冲突 | 座位时间冲突、重复提交 |
| 500 | 服务器异常 | 未捕获系统异常 |

[精进] 说明：

1. 对于评论发布、回复、点赞接口，如果未提供 `X-User-Phone` 且未携带登录态，应返回 `401`。
2. 对于普通查询接口，若未登录，评论状态字段 `liked` 默认返回 `false`。

---

## 认证与账号

> 说明：基础版可先用 `X-User-Phone` 识别用户；为兼容后续升级，这里按 JWT 形式设计标准接口。

### 1. 发送验证码

- 方法：`POST`
- 路径：`/api/v1/auth/send-code`
- 请求体：

```json
{
  "phone": "13800000000",
  "scene": "LOGIN"
}
```

- 响应：`object`
  - `phone` `string`
  - `scene` `string`
  - `expireSeconds` `int`
- 示例：`curl -X POST "https://host/api/v1/auth/send-code" -H "Content-Type: application/json" -d '{"phone":"13800000000","scene":"LOGIN"}'`

### 2. 验证码登录

- 方法：`POST`
- 路径：`/api/v1/auth/login`
- 请求体：

```json
{
  "phone": "13800000000",
  "code": "123456"
}
```

- 响应：`object`
  - `accessToken` `string`
  - `refreshToken` `string`
  - `tokenType` `string`（`Bearer`）
  - `expiresIn` `int`
  - `user` `object`（`id/phone/nickname/status`）
- 示例：`curl -X POST "https://host/api/v1/auth/login" -H "Content-Type: application/json" -d '{"phone":"13800000000","code":"123456"}'`

### 3. 刷新 Token

- 方法：`POST`
- 路径：`/api/v1/auth/token/refresh`
- 请求体：`{"refreshToken":"<token>"}`
- 响应：`object`
  - `accessToken` `string`
  - `refreshToken` `string`
  - `tokenType` `string`
  - `expiresIn` `int`
- 示例：`curl -X POST "https://host/api/v1/auth/token/refresh" -H "Content-Type: application/json" -d '{"refreshToken":"<token>"}'`

### 4. 当前用户信息

- 方法：`GET`
- 路径：`/api/v1/auth/me`
- 鉴权：需要
- 响应：`object`
  - `id` `long`
  - `phone` `string`
  - `nickname` `string`
  - `role` `string`
  - `status` `int`
- 示例：`curl "https://host/api/v1/auth/me" -H "Authorization: Bearer <access_token>"`

### 5. 注销

- 方法：`POST`
- 路径：`/api/v1/auth/logout`
- 鉴权：需要
- 请求体：`{"refreshToken":"<token>"}`
- 响应：`204 No Content`
- 示例：`curl -X POST "https://host/api/v1/auth/logout" -H "Authorization: Bearer <access_token>" -H "Content-Type: application/json" -d '{"refreshToken":"<token>"}'`

---

## 空间（公开）

### 1. 空间列表

- 方法：`GET`
- 路径：`/api/v1/spaces`
- 查询参数：
  - `status` `int`（可选）
  - `keyword` `string`（可选，按名称模糊）
  - `page` `int`（默认 1）
  - `size` `int`（默认 20，最大 100）
- 响应：`object`
  - `items` `SpaceItem[]`
  - `page` `int`
  - `size` `int`
  - `total` `long`
- 数组元素结构：`SpaceItem`
  - `id` `long`
  - `code` `string`
  - `name` `string`
  - `openStartTime` `string`
  - `openEndTime` `string`
  - `priceHourly` `number`
  - `status` `int`
- 示例：`curl "https://host/api/v1/spaces?page=1&size=20"`

### 2. 空间详情

- 方法：`GET`
- 路径：`/api/v1/spaces/{id}`
- 响应：`object`
  - `id` `long`
  - `code` `string`
  - `name` `string`
  - `openStartTime` `string`
  - `openEndTime` `string`
  - `wifiSsid` `string`
  - `rules` `string`
  - `priceHourly` `number`
  - `status` `int`
- 示例：`curl "https://host/api/v1/spaces/1"`

### 3. 空间评论列表

- 方法：`GET`
- 路径：`/api/v1/spaces/{spaceId}/comments`
- 查询参数：
  - `page` `int`（默认 1）
  - `size` `int`（默认 20，最大 100）
- 请求头：
  - `X-User-Phone`：可选，用于返回 `liked` 状态
- 响应：`object`
  - `items` `CommentItem[]`
  - `page` `int`
  - `size` `int`
  - `total` `long`
- 数组元素结构：`CommentItem`
  - `id` `long`
  - `spaceId` `long`
  - `userId` `long`
  - `nickname` `string`
  - `maskedPhone` `string`
  - `parentId` `long`
  - `rootId` `long`
  - `content` `string`
  - `likeCount` `long`
  - `replyCount` `long`
  - `liked` `boolean`
  - `createdAt` `string`
  - `replies` `CommentItem[]`
- 示例：

```http
GET /api/v1/spaces/1/comments?page=1&size=20 HTTP/1.1
Host: localhost:8080
X-User-Phone: 13800000000
```

```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "items": [
      {
        "id": 1001,
        "spaceId": 1,
        "userId": 11,
        "nickname": "用户-0000",
        "maskedPhone": "138****0000",
        "parentId": 0,
        "rootId": 0,
        "content": "这个空间很安静，插座也够用。",
        "likeCount": 3,
        "replyCount": 1,
        "liked": false,
        "createdAt": "2026-06-04T18:00:00",
        "replies": [
          {
            "id": 1002,
            "spaceId": 1,
            "userId": 12,
            "nickname": "用户-8888",
            "maskedPhone": "139****8888",
            "parentId": 1001,
            "rootId": 1001,
            "content": "我也觉得环境不错。",
            "likeCount": 1,
            "replyCount": 0,
            "liked": true,
            "createdAt": "2026-06-04T18:10:00",
            "replies": []
          }
        ]
      }
    ],
    "page": 1,
    "size": 20,
    "total": 1
  },
  "traceId": "c9f8d2a1b3"
}
```

---

## 座位（公开）

### 1. 查询可用座位

- 方法：`GET`
- 路径：`/api/v1/seats/available`
- 查询参数：
  - `spaceId` `long`（必填）
  - `startAt` `string`（必填，ISO）
  - `endAt` `string`（必填，ISO）
  - `hasSocket` `int`（可选，0/1）
  - `isQuiet` `int`（可选，0/1）
  - `page` `int`（默认 1）
  - `size` `int`（默认 20，最大 100）
- 响应：`object`
  - `items` `SeatItem[]`
  - `page` `int`
  - `size` `int`
  - `total` `long`
- 数组元素结构：`SeatItem`
  - `id` `long`
  - `spaceId` `long`
  - `seatNumber` `string`
  - `hasSocket` `boolean`
  - `isQuiet` `boolean`
  - `status` `int`
- 示例：`curl "https://host/api/v1/seats/available?spaceId=1&startAt=2026-05-20T14:00:00%2B08:00&endAt=2026-05-20T16:00:00%2B08:00&hasSocket=1&isQuiet=1"`

### 2. 座位详情

- 方法：`GET`
- 路径：`/api/v1/seats/{id}`
- 响应：`object`
  - `id` `long`
  - `spaceId` `long`
  - `seatNumber` `string`
  - `hasSocket` `boolean`
  - `isQuiet` `boolean`
  - `status` `int`
- 示例：`curl "https://host/api/v1/seats/101"`

---

## 空间评论（客户端）

[精进] 说明：

1. 评论体系采用“一级评论 + 回复”的树形结构。
2. 点赞采用独立点赞表，保证重复点击幂等。
3. 评论默认不做后台审核页，若后续要加审核，建议在 `space_comment.status` 上扩展状态流转。

### 1. 发布评论或回复

- 方法：`POST`
- 路径：`/api/v1/spaces/{spaceId}/comments`
- 请求头：
  - `X-User-Phone`：必填，基础版身份标识
- 请求体：

```json
{
  "parentId": 0,
  "content": "这个空间很适合自习。"
}
```

- 说明：
  - `parentId=0` 表示一级评论
  - `parentId>0` 表示回复某条评论
- 响应：`object`
  - `id` `long`
  - `spaceId` `long`
  - `userId` `long`
  - `nickname` `string`
  - `maskedPhone` `string`
  - `parentId` `long`
  - `rootId` `long`
  - `content` `string`
  - `likeCount` `long`
  - `replyCount` `long`
  - `liked` `boolean`
  - `createdAt` `string`
  - `replies` `array`
- 示例：

```http
POST /api/v1/spaces/1/comments HTTP/1.1
Host: localhost:8080
Content-Type: application/json
X-User-Phone: 13800000000

{
  "parentId": 0,
  "content": "这个空间很适合自习。"
}
```

### 2. 评论点赞

- 方法：`POST`
- 路径：`/api/v1/comments/{commentId}/like`
- 请求头：
  - `X-User-Phone`：必填
- 响应：`object`
  - `commentId` `long`
  - `liked` `boolean`
  - `changed` `boolean`
  - `likeCount` `long`
- 示例：

```http
POST /api/v1/comments/1001/like HTTP/1.1
Host: localhost:8080
X-User-Phone: 13800000000
```

```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "commentId": 1001,
    "liked": true,
    "changed": true,
    "likeCount": 4
  },
  "traceId": "9f11c7e6ab"
}
```

### 3. 取消评论点赞

- 方法：`DELETE`
- 路径：`/api/v1/comments/{commentId}/like`
- 请求头：
  - `X-User-Phone`：必填
- 响应：`object`
  - `commentId` `long`
  - `liked` `boolean`
  - `changed` `boolean`
  - `likeCount` `long`
- 示例：`curl -X DELETE "https://host/api/v1/comments/1001/like" -H "X-User-Phone: 13800000000"`

---

## 预约（客户端）

### 1. 创建预约

- 方法：`POST`
- 路径：`/api/v1/bookings`
- 鉴权：需要（JWT 或 `X-User-Phone`）
- 请求体：

```json
{
  "seatId": 101,
  "startAt": "2026-05-20T14:00:00+08:00",
  "endAt": "2026-05-20T16:00:00+08:00",
  "remark": "靠窗位优先"
}
```

- 响应：`object`
  - `bookingId` `long`
  - `bookingNo` `string`
  - `userId` `long`
  - `spaceId` `long`
  - `seatId` `long`
  - `startAt` `string`
  - `endAt` `string`
  - `status` `int`
  - `confirmCode` `string`
- 示例：`curl -X POST "https://host/api/v1/bookings" -H "Authorization: Bearer <access_token>" -H "Content-Type: application/json" -d '{"seatId":101,"startAt":"2026-05-20T14:00:00+08:00","endAt":"2026-05-20T16:00:00+08:00"}'`

### 2. 我的预约列表

- 方法：`GET`
- 路径：`/api/v1/bookings/mine`
- 鉴权：需要
- 查询参数：
  - `status` `int`（可选）
  - `startDate` `string`（可选，`yyyy-MM-dd`）
  - `endDate` `string`（可选，`yyyy-MM-dd`）
  - `page` `int`（默认 1）
  - `size` `int`（默认 20，最大 100）
- 响应：`object`
  - `items` `MyBookingItem[]`
  - `page` `int`
  - `size` `int`
  - `total` `long`
- 数组元素结构：`MyBookingItem`
  - `id` `long`
  - `bookingNo` `string`
  - `spaceName` `string`
  - `seatNumber` `string`
  - `startAt` `string`
  - `endAt` `string`
  - `status` `int`
  - `confirmCode` `string`
- 示例：`curl "https://host/api/v1/bookings/mine?page=1&size=20" -H "Authorization: Bearer <access_token>"`

### 3. 预约详情

- 方法：`GET`
- 路径：`/api/v1/bookings/{id}`
- 鉴权：需要
- 响应：`object`
  - `id` `long`
  - `bookingNo` `string`
  - `spaceId` `long`
  - `spaceName` `string`
  - `seatId` `long`
  - `seatNumber` `string`
  - `startAt` `string`
  - `endAt` `string`
  - `status` `int`
  - `confirmCode` `string`
  - `createdAt` `string`
- 示例：`curl "https://host/api/v1/bookings/9001" -H "Authorization: Bearer <access_token>"`

### 4. 取消预约

- 方法：`DELETE`
- 路径：`/api/v1/bookings/{id}`
- 鉴权：需要
- 请求体（可选）：

```json
{
  "cancelReason": "临时有事"
}
```

- 响应：`object`
  - `id` `long`
  - `status` `int`（`3`）
  - `cancelReason` `string`
  - `updatedAt` `string`
- 示例：`curl -X DELETE "https://host/api/v1/bookings/9001" -H "Authorization: Bearer <access_token>" -H "Content-Type: application/json" -d '{"cancelReason":"临时有事"}'`

---

## 管理端（管理员）

> 鉴权：`X-Admin-Token: <token>`

### 空间管理

#### 1. 新增空间

- 方法：`POST`
- 路径：`/api/v1/admin/spaces`
- 请求体：

```json
{
  "code": "SPACE_001",
  "name": "静心自习室",
  "openStartTime": "08:00:00",
  "openEndTime": "23:00:00",
  "wifiSsid": "StudyRoom",
  "wifiPassword": "12345678",
  "rules": "禁止喧哗",
  "priceHourly": 8.50,
  "status": 1
}
```

- 响应：`object`
  - `id` `long`
- 示例：`curl -X POST "https://host/api/v1/admin/spaces" -H "X-Admin-Token: <token>" -H "Content-Type: application/json" -d '{"code":"SPACE_001","name":"静心自习室","openStartTime":"08:00:00","openEndTime":"23:00:00","priceHourly":8.5,"status":1}'`

#### 2. 修改空间

- 方法：`PUT`
- 路径：`/api/v1/admin/spaces/{id}`
- 请求体：同新增（支持部分字段）
- 响应：`object`
  - `id` `long`
  - `updated` `boolean`
- 示例：`curl -X PUT "https://host/api/v1/admin/spaces/1" -H "X-Admin-Token: <token>" -H "Content-Type: application/json" -d '{"name":"静心自习室A区","priceHourly":9.0}'`

#### 3. 删除空间（软删）

- 方法：`DELETE`
- 路径：`/api/v1/admin/spaces/{id}`
- 响应：`object`
  - `id` `long`
  - `deleted` `boolean`
- 示例：`curl -X DELETE "https://host/api/v1/admin/spaces/1" -H "X-Admin-Token: <token>"`

### 座位管理

#### 1. 新增座位

- 方法：`POST`
- 路径：`/api/v1/admin/seats`
- 请求体：

```json
{
  "spaceId": 1,
  "seatNumber": "A01",
  "hasSocket": true,
  "isQuiet": true,
  "status": 1
}
```

- 响应：`object`
  - `id` `long`
- 示例：`curl -X POST "https://host/api/v1/admin/seats" -H "X-Admin-Token: <token>" -H "Content-Type: application/json" -d '{"spaceId":1,"seatNumber":"A01","hasSocket":true,"isQuiet":true,"status":1}'`

#### 2. 修改座位

- 方法：`PUT`
- 路径：`/api/v1/admin/seats/{id}`
- 请求体：支持部分字段更新
- 响应：`object`
  - `id` `long`
  - `updated` `boolean`
- 示例：`curl -X PUT "https://host/api/v1/admin/seats/101" -H "X-Admin-Token: <token>" -H "Content-Type: application/json" -d '{"isQuiet":false}'`

#### 3. 删除座位（软删）

- 方法：`DELETE`
- 路径：`/api/v1/admin/seats/{id}`
- 响应：`object`
  - `id` `long`
  - `deleted` `boolean`
- 示例：`curl -X DELETE "https://host/api/v1/admin/seats/101" -H "X-Admin-Token: <token>"`

### 预约管理

#### 1. 预约列表（分页筛选）

- 方法：`GET`
- 路径：`/api/v1/admin/bookings`
- 查询参数：
  - `spaceId` `long`（可选）
  - `seatId` `long`（可选）
  - `phone` `string`（可选）
  - `status` `int`（可选）
  - `startAt` `string`（可选）
  - `endAt` `string`（可选）
  - `page` `int`（默认 1）
  - `size` `int`（默认 20，最大 100）
- 响应：`object`
  - `items` `AdminBookingItem[]`
  - `page` `int`
  - `size` `int`
  - `total` `long`
- 数组元素结构：`AdminBookingItem`
  - `id` `long`
  - `bookingNo` `string`
  - `phone` `string`（脱敏）
  - `spaceName` `string`
  - `seatNumber` `string`
  - `startAt` `string`
  - `endAt` `string`
  - `status` `int`
- 示例：`curl "https://host/api/v1/admin/bookings?page=1&size=20&status=1" -H "X-Admin-Token: <token>"`

#### 2. 更新预约状态

- 方法：`PUT`
- 路径：`/api/v1/admin/bookings/{id}/status`
- 请求体：

```json
{
  "status": 2,
  "reason": "管理员核销"
}
```

- 响应：`object`
  - `id` `long`
  - `status` `int`
  - `updated` `boolean`
- 示例：`curl -X PUT "https://host/api/v1/admin/bookings/9001/status" -H "X-Admin-Token: <token>" -H "Content-Type: application/json" -d '{"status":2,"reason":"管理员核销"}'`

### 用户管理

#### 1. 用户列表

- 方法：`GET`
- 路径：`/api/v1/admin/users`
- 查询参数：
  - `phone` `string`（可选）
  - `status` `int`（可选）
  - `page` `int`（默认 1）
  - `size` `int`（默认 20，最大 100）
- 响应：`object`
  - `items` `UserItem[]`
  - `page` `int`
  - `size` `int`
  - `total` `long`
- 数组元素结构：`UserItem`
  - `id` `long`
  - `phone` `string`（脱敏）
  - `nickname` `string`
  - `status` `int`
  - `createdAt` `string`
- 示例：`curl "https://host/api/v1/admin/users?page=1&size=20" -H "X-Admin-Token: <token>"`

#### 2. 禁用/启用用户

- 方法：`PUT`
- 路径：`/api/v1/admin/users/{id}/status`
- 请求体：

```json
{
  "status": 0
}
```

- 响应：`object`
  - `id` `long`
  - `status` `int`
  - `updated` `boolean`
- 示例：`curl -X PUT "https://host/api/v1/admin/users/1/status" -H "X-Admin-Token: <token>" -H "Content-Type: application/json" -d '{"status":0}'`

### 统计看板

#### 1. 今日概览

- 方法：`GET`
- 路径：`/api/v1/admin/dashboard/overview`
- 查询参数：
  - `date` `string`（可选，`yyyy-MM-dd`，默认今天）
- 响应：`object`
  - `date` `string`
  - `totalBookings` `long`
  - `pendingBookings` `long`
  - `usedBookings` `long`
  - `cancelledBookings` `long`
  - `occupancyRate` `number`（0~1）
- 示例：`curl "https://host/api/v1/admin/dashboard/overview?date=2026-05-17" -H "X-Admin-Token: <token>"`

---

## AI 预留接口

### 1. 智能问答与意图预约

- 方法：`POST`
- 路径：`/api/v1/chat`
- 鉴权：可选（建议登录）
- 请求体：

```json
{
  "message": "今天晚上 7 点到 9 点有安静区带插座座位吗？",
  "userId": 1,
  "phone": "13800000000",
  "context": {
    "spaceId": 1
  }
}
```

- 响应：`object`
  - `reply` `string`
  - `intent` `string`（`QNA|BOOKING_QUERY|BOOKING_CREATE`）
  - `suggestions` `array`
  - `toolCalls` `array`
- 示例：`curl -X POST "https://host/api/v1/chat" -H "Content-Type: application/json" -d '{"message":"今天有空位吗？","userId":1}'`

---

## 运维与健康检查

### 1. 存活检查

- 方法：`GET`
- 路径：`/actuator/health`
- 响应：`object`
  - `status` `string`（`UP|DOWN`）
  - `components` `object`
- 示例：`curl "https://host/actuator/health"`

### 2. 就绪检查（可选）

- 方法：`GET`
- 路径：`/actuator/health/readiness`
- 响应：`object`
  - `status` `string`
- 示例：`curl "https://host/actuator/health/readiness"`

---

## 建议落地顺序

1. 先实现公开接口：`/spaces`、`/seats/available`
2. 再实现核心链路：`POST /bookings`、`GET /bookings/mine`、`DELETE /bookings/{id}`
3. 最后补管理端：`/admin/spaces`、`/admin/seats`、`/admin/bookings`、`/admin/users`
