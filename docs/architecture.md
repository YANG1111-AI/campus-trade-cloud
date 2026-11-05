# 架构说明

本文说明三个服务进程各自负责什么、一次典型请求如何穿过整个链路，以及每个中间件在项目里的落点。
如何使用请看 [README](../README.md)。

## 模块划分

| 模块 | 端口 | 职责 | 外部依赖 |
| --- | --- | --- | --- |
| `api-gateway` | 8000 | 唯一对外入口；按 `Path` 谓词路由到后端服务；托管 Vue 单页 | Nacos、Zipkin |
| `user-service` | 8001 | 用户查询、登录示例、配置中心演示 | Nacos（注册 + 配置）、MySQL `campus_user`、Zipkin |
| `goods-service` | 8002 | 商品查询、下单示例（远程调用 + 消息投递） | Nacos、MySQL `campus_goods`、RabbitMQ、Sentinel、Zipkin |

拆分依据是"数据所有权"：用户数据与商品数据分别落在两个库、两个服务，跨服务只通过 Feign 读接口访问，
不共享数据库连接。这是示例里唯一刻意保持的设计约束，其余都是最小实现。

## 一次下单请求的完整链路

在页面点击"立即购买"（假设 `goodsId=1`、`userId=2`）：

1. **浏览器** `GET /goods/order/1/2` → 网关 8000
2. **api-gateway** 命中 `goods_route`（`Path=/goods/**`），把 `lb://goods-service` 交给 LoadBalancer
   解析成 Nacos 里的一个真实实例地址
3. **goods-service** `GoodsController#order`
   - **OpenFeign** `UserFeignClient#getUserInfo(2)` → 经 Nacos 发现 → **user-service** `GET /user/info/2`
   - **user-service** `UserMapper#findById` 查 `campus_user.user`，返回 JSON（`password` 被 `@JsonIgnore` 过滤）
   - 若这一步失败（服务停止 / 超时），**Sentinel** 让调用落到 `UserFeignFallback`，返回 `null`，
     控制器直接返回降级文案，不再发消息
   - 成功则 **StreamBridge** 向 binding `orderOutput-out-0` 发送字符串，落到 RabbitMQ 的 `order-topic`
4. **Sleuth** 在整条链路上生成并传播 TraceId/SpanId，**Zipkin** 收集后绘制调用图

对应到日志里，每一行都带有 `[服务名,TraceId,SpanId]` 前缀，可以直接和 Zipkin 界面里的 trace 对齐。

## 中间件落点

### Nacos：注册中心 + 配置中心

- 注册：三个服务都带 `@EnableDiscoveryClient`，`spring.application.name` 即服务名，网关用 `lb://服务名` 寻址
- 配置：只有 `user-service` 接了配置中心（`spring-cloud-starter-alibaba-nacos-config` + `bootstrap.yml`）
  - Data ID 约定为 `${spring.application.name}.${file-extension}`，即 `user-service.yaml`
  - `UserController` 上的 `@RefreshScope` 让 `@Value("${campus.welcome:...}")` 在配置变更后重新绑定，无需重启
  - 配置缺失时走 `@Value` 的默认值，服务照样能起——这是有意为之，降低上手门槛

### Sentinel：只用来做 Feign 降级

`feign.sentinel.enabled: true` + `@FeignClient(fallback = UserFeignFallback.class)` 是全部配置。
项目**没有**接 Sentinel Dashboard，也没有流控/熔断规则文件：降级只在"目标服务不可用"时触发，
这正好是演示点想说明的部分。想做限流演示请自行加 dashboard 与规则。

### Spring Cloud Stream + RabbitMQ

- 生产者：`GoodsController` 用 `StreamBridge#send("orderOutput-out-0", msg)`
- 绑定定义在 `spring.cloud.stream.bindings.orderOutput-out-0.destination: order-topic`
- **没有消费者**：`db/02_campus_goods.sql` 中的 `orders` 表是预留结构，当前不会被写入
- 连接参数在 `spring.rabbitmq.*`，Rabbit 专属的绑定参数在 `spring.cloud.stream.rabbit.bindings.<binding>.producer.*`
  ——这两处最容易写错位置，写错了不会报错，只会静默不生效

### Sleuth + Zipkin

采样率固定为 `1.0`（全部采样），因为演示场景下丢采样会让调用链看起来"缺一段"。
Spring Boot 2.6 版本线上仍然使用 Sleuth；升级到 Boot 3 时需替换为 Micrometer Tracing。

## 数据模型

两个库互不 JOIN，`goods.seller_id` 与 `orders.buyer_id` 只是逻辑关联。

```
campus_user.user                campus_goods.goods          campus_goods.orders
+------------+----------+       +-----+----------+------+   +-----+----------+------+
| id (PK)    | username |       | id  | title    | price|   | id  | goods_id | buyer_id
| password   | balance  |       +-----+-----+----+------+   | amount | status | create_time
+------------+----------+             | seller_id           +--------+---------+
        ^                             v                          （预留，无写入）
        +--------- Feign 只读 ------- id
```

## 前端

`api-gateway/src/main/resources/static/index.html` 是一个单文件 Vue 3 应用：

- 没有构建步骤，Vue / Axios / Font Awesome 全部来自公网 CDN，因此**离线环境会白屏**
- 由 WebFlux 的默认静态资源处理器提供，访问 `http://localhost:8000/` 即可
- 因为它和 API 同源，所以项目**不需要**任何 CORS 配置；如果把前端拆成独立 dev server，需要自行在网关加 CORS

## 与"生产系统"的差距

见 [SECURITY.md](../SECURITY.md) 与 [README 已知限制与后续方向](../README.md#已知限制与后续方向)。
最短的一句话：缺少鉴权、校验、事务、幂等、消费端与可观测性落地，任何真实业务都要重做这五块。
