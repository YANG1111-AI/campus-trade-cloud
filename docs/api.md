# 接口文档

所有请求都通过网关访问：`http://localhost:8000`。直连服务（8001 / 8002）也可以，
但那样看不到网关路由与链路追踪的效果。

以下示例假设你已经导入 `db/` 下的演示数据，即用户 `seller_01(id=1)`、`buyer_01(id=2)`，
商品 `id=1..3`。

> 这些接口**没有鉴权**，返回的是裸对象或纯文本，没有统一响应包装，也没有错误码体系。
> 这是为了把代码量压到最小，不代表推荐做法。

---

## user-service

### GET /user/info/{id}

按 ID 查询用户。该接口的主要消费者是 `goods-service` 的 OpenFeign 客户端。

`password` 字段在实体上标注了 `@JsonIgnore`，因此不会出现在响应里。

```bash
curl http://localhost:8000/user/info/1
```

```json
{
  "id": 1,
  "username": "seller_01",
  "balance": 1000.00
}
```

用户不存在时返回 `null`（HTTP 200，空 body），这是示例的简化行为。

### GET /user/login

登录示例。**只按用户名查询，完全不校验密码**，也不产生任何会话或 token。

```bash
curl "http://localhost:8000/user/login?username=seller_01"
```

```
Login Success: seller_01
```

用户名不存在时返回 `Login Failed`。

### GET /user/register

注册桩接口，只回显参数，**不写数据库**：

```bash
curl "http://localhost:8000/user/register?username=newbie&password=whatever"
```

```
注册接口为示例桩实现，未写入数据库：newbie
```

### GET /user/welcome

返回 Nacos 配置中心里 `campus.welcome` 的值，用于演示 `@RefreshScope` 动态刷新。

```bash
curl http://localhost:8000/user/welcome
```

```
欢迎来到校园二手交易平台（来自 Nacos 配置中心）
```

若 Nacos 中没有 `user-service.yaml` 配置，返回代码里的默认值 `欢迎来到校园二手交易平台`。

---

## goods-service

### GET /goods/list

商品列表。

```bash
curl http://localhost:8000/goods/list
```

```json
[
  { "id": 1, "title": "二手Java微服务教材", "price": 35.00, "sellerId": 1, "stock": 5 },
  { "id": 2, "title": "九成新山地自行车",   "price": 200.00, "sellerId": 1, "stock": 2 },
  { "id": 3, "title": "机械键盘",           "price": 80.00, "sellerId": 2, "stock": 10 }
]
```

### GET /goods/detail/{id}

单个商品，字段同上。商品不存在时返回 `null`。

```bash
curl http://localhost:8000/goods/detail/1
```

### GET /goods/order/{goodsId}/{userId}

下单示例，串联 Feign 远程调用与 RabbitMQ 消息投递。

```bash
curl http://localhost:8000/goods/order/1/2
```

```
下单成功！消息已发送至 RabbitMQ
```

行为分支：

| 条件 | 返回 |
| --- | --- |
| `user-service` 正常且用户存在 | `下单成功！消息已发送至 RabbitMQ`，同时向 `order-topic` 投递消息 |
| `user-service` 未启动或调用失败（Sentinel 降级） | `下单失败：用户不存在或服务降级！`，不投递消息 |
| 用户 ID 不存在 | 同降级分支（`/user/info/{id}` 查不到时返回 null） |

**注意：该接口不写 `orders` 表，也不扣减 `goods.stock`。** 消息落到 MQ 之后没有消费者处理。
另外它用 GET 改变状态，属于示例遗留问题，见
[README 已知限制与后续方向](../README.md#已知限制与后续方向)。

去 RabbitMQ 管理台 http://localhost:15672 （guest / guest）可以看到 `order-topic`
这个 exchange 以及消息堆积。

---

## api-gateway

### GET /

返回前端单页 `static/index.html`，无需单独启动前端服务。

### 路由规则

定义在 `api-gateway/src/main/resources/application.yml`：

| 前缀 | 目标 |
| --- | --- |
| `/user/**` | `lb://user-service` |
| `/goods/**` | `lb://goods-service` |

同时 `spring.cloud.gateway.discovery.locator.enabled: true` 打开了基于服务名的自动路由，
所以 `/user-service/user/info/1`、`/goods-service/goods/list` 这类路径也可以访问通
（路径会带上服务名，前端没有使用这种形式）。

后端服务未注册时，网关返回 503，body 里是 `Unable to find instance for xxx`。
