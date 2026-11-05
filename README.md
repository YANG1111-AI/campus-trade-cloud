# 校园二手交易微服务系统

**独立设计与实现的个人项目** · 作者：[YANG1111-AI](https://github.com/YANG1111-AI)

一个把 Spring Cloud Alibaba 微服务核心技术串起来跑通的校园二手交易平台示例：
服务注册与发现、配置中心热刷新、声明式远程调用、熔断降级、消息驱动、全链路追踪、API 网关，
外加一个无需构建、由网关直接托管的 Vue 3 单页前端。

> **一个人的独立学习作品**：从需求拆解、服务拆分、编码联调到文档与开源整理全部自己完成，
> 代码按 [MIT](LICENSE) 开源，欢迎 fork、二次开发，也欢迎开 Issue 聊实现思路。
>
> ⚠️ **定位与边界**：它的设计目标是"把每个组件的配置和调用链讲清楚"，**不是**一个可上线的交易系统。
> 没有鉴权、没有参数校验、没有事务与幂等，下单接口不写库。
> 这些取舍在 [已知限制与后续方向](#已知限制与后续方向) 与 [SECURITY.md](SECURITY.md) 里逐条写明了，
> 我认为把它们说清楚比假装完备更有价值。

---

## 目录

- [30 秒看懂这个项目](#30-秒看懂这个项目)
- [技术亮点](#技术亮点)
- [我做的几个判断](#我做的几个判断)
- [如何亲手验证这些能力](#如何亲手验证这些能力)
- [架构总览](#架构总览)
- [技术栈](#技术栈)
- [目录结构](#目录结构)
- [快速开始](#快速开始)
- [配置项](#配置项)
- [接口一览](#接口一览)
- [代码在哪看某个特性](#代码在哪看某个特性)
- [测试与构建](#测试与构建)
- [已知限制与后续方向](#已知限制与后续方向)
- [常见问题](#常见问题)
- [关于贡献与交流](#关于贡献与交流)
- [许可证与作者](#许可证与作者)

---

## 30 秒看懂这个项目

| | |
| --- | --- |
| **做了什么** | 三模块 Spring Cloud Alibaba 微服务 + Vue 3 前端，覆盖注册发现 / 配置中心 / Feign / Sentinel / Stream+RabbitMQ / Sleuth+Zipkin / Gateway 七类能力 |
| **规模** | 3 个服务 + 1 个前端页面 + 2 个库；约 30 个源文件与配置，`mvn clean verify` 一键通过 |
| **独立程度** | 需求分析、架构拆分、编码、联调、文档、开源整理全部独立完成 |
| **测试** | 9 个纯单元测试，不依赖任何中间件即可跑完 |
| **技术栈** | JDK 8 · Spring Boot 2.6 · Spring Cloud 2021 · Spring Cloud Alibaba 2021 · MyBatis · MySQL · RabbitMQ · Nacos · Zipkin · Vue 3 |

## 技术亮点

**1. 网关统一入口，路由靠服务发现而不是硬编码地址**
`spring.cloud.gateway.routes` 用 `lb://user-service` 配合 Nacos 寻址，后端实例增减不需要改网关配置；
同时打开了 `discovery.locator`，网关还能托管前端页面，让整个系统只暴露一个 8000 端口。

**2. 可复现的熔断降级，而不是"配置了但没验证"**
`feign.sentinel.enabled: true` + `@FeignClient(fallback = UserFeignFallback.class)`，
停掉 `user-service` 后下单接口返回降级文案而不是 500，且不往 MQ 投递消息。
这条分支有单元测试 `orderFailsWithoutTouchingMqWhenFeignFallsBack` 兜住。

**3. 配置中心热刷新，并对"配置缺失"做了兜底**
`@RefreshScope` + `@Value("${campus.welcome:欢迎来到校园二手交易平台}")`：
Nacos 里改 `user-service.yaml` 不用重启就生效；没建配置时服务照样能起，
避免把"必须先在 Nacos 建 dataId"变成别人的上手门槛。

**4. 消息驱动解耦下单动作**
用 `StreamBridge` 面向 binding（`orderOutput-out-0` → `order-topic`）编程，
业务代码不出现任何 RabbitMQ API，换成 Kafka 只需要改依赖与配置。

**5. 全链路可观测，日志能和 Zipkin 图对上**
三个服务统一采样率 1.0，并在 `logging.pattern.level` 里输出 `[服务名,TraceId,SpanId]`，
所以排查时可以直接拿日志里的 TraceId 去 Zipkin 搜索，而不是靠时间戳猜。

**6. 配置全部外部化，凭据不进仓库**
中间件地址、数据库与 MQ 账号密码统一写成 `${ENV:默认值}`，
默认值指向本机以便开箱运行，真实环境只改环境变量、不动代码也不产生提交差异。

**7. 单元测试刻意不依赖中间件**
`mvn clean verify` 在只有 JDK + Maven 的机器上就能全绿，
任何人或任何 CI 想验证这个仓库都不需要搭 MySQL/Nacos——这是我给它设计的最低接手成本。

**8. 开源工程化的整理**
补齐 MIT 许可证、README / CHANGELOG / CONTRIBUTING / SECURITY、`docs/` 架构与接口文档、
`.editorconfig` 与 `.gitattributes`；清掉 IDE 本地配置、构建产物和脚手架残留；
统一包前缀与 POM 版本管理（子模块不再重复声明版本），并补上 `spring-boot-maven-plugin`
让 `mvn package` 真正产出可执行 jar。

## 我做的几个判断

这几处是我认为最能体现工程取舍的地方，也是最想被追问的部分。

**① RabbitMQ 配置写错层级不会报错，只会静默失效**
`spring.cloud.stream.rabbit.host` 不是有效的属性路径，连接参数应该属于 `spring.rabbitmq.*`。
Spring Boot 对无法识别的配置不报错，所以服务照常启动、`StreamBridge#send` 却在运行时抛错。
我把连接参数归位到 `spring.rabbitmq.*`，Rabbit 专属的生产者参数放在
`spring.cloud.stream.rabbit.bindings.<binding>.producer.*`，并在 [CONTRIBUTING.md](CONTRIBUTING.md)
里写下"层级写错不报错"这条踩坑记录。
→ 教训：配置类问题要靠"起得来但跑不通"这类现象反推，光看启动日志不够。

**② `StreamBridge` 是 final 类，当前依赖线上打不了桩**
Spring Boot 2.6 版本线里的 Byte Buddy 不支持在新 JDK 上为 final 类插桩，
直接 mock 会让测试整体报错。我没有为了跑测试去升依赖（会破坏锁定的教学版本线），
而是把消息文本的构造提取成纯函数 `GoodsController#buildOrderMessage`，
让"消息内容正确"这一半变成可测的，另一半（确实发送）在文档里明确写清"需要到 MQ 管理台肉眼确认"。
→ 我选择"缩小断言范围 + 把未覆盖处写明"，而不是假装覆盖率很高。

**③ 前端放在网关的 `static/` 下，牺牲一点"正统"**
独立前端工程要引入 Node 构建链和跨域配置。这里由 WebFlux 默认静态资源处理器直接托管单页，
同源所以完全不需要 CORS，接手成本几乎为零。代价是网关职责略重、前端没法独立部署——
对示例项目我认为这个交换是划算的，但真实项目不会这么拆。

**④ 下单接口保留了 GET，尽管它违反 HTTP 语义**
`GET /goods/order/{goodsId}/{userId}` 确实会改变状态，不符合语义，会带来 CSRF 与浏览器预取风险。
改成 POST 需要同时改动 `index.html` 的 `buyGoods` 与接口文档；我把它作为已知问题明确写进
[已知限制](#已知限制与后续方向)第 3 条并给出改法，而不是悄悄改掉一个对外示例契约。
→ 这是本项目里我最想被指出来的一处，也是我下一步最想修的。

**⑤ 用户库和商品库不共享连接**
拆成 `campus_user` / `campus_goods` 两个库、两个服务，跨服务只能通过 Feign 只读接口访问，
`goods.seller_id` 是逻辑关联、不做跨库 JOIN。
这条约束是本仓库唯一刻意守住的架构原则，其余部分都服从"最短路径演示"。

## 如何亲手验证这些能力

不需要读代码，照下面五步走一遍就能全部看到效果（详细安装见[快速开始](#快速开始)）：

| 想验证 | 操作 | 预期看到 |
| --- | --- | --- |
| 网关路由 + 服务发现 | `curl :8000/goods/list` | 返回 3 条商品 JSON；停掉 goods-service 后变 503 |
| Feign 跨服务调用 | `curl :8000/goods/order/1/2` | `下单成功！消息已发送至 RabbitMQ` |
| Sentinel 降级 | 先杀掉 `user-service`，再点页面"立即购买" | `下单失败：用户不存在或服务降级！`，日志出现降级 warn |
| MQ 消息落达 | RabbitMQ 管理台 → Exchanges 看 `order-topic` | 下单后消息计数增加 |
| 配置热刷新 | Nacos 改 `user-service.yaml` 的 `campus.welcome`，刷新页面 | 顶部公告文案变化，**不重启服务** |
| 链路追踪 | 下一单后打开 Zipkin，选 goods-service 的 trace | 一条包含 gateway → goods → user 的调用链 |
| 测试可复现 | 只装 JDK 8 + Maven，执行 `mvn clean verify` | 9 个测试全绿，无需任何中间件 |

## 架构总览

```
                         ┌──────────────────────────┐
   浏览器 ──────────────► │  api-gateway  :8000      │  Spring Cloud Gateway
   （Vue 3 单页）          │  托管 static/index.html   │  按 Path 动态路由
                         └────────┬─────────┬────────┘
                        /user/**  │         │  /goods/**
                                  ▼         ▼
                    ┌───────────────┐   ┌───────────────┐
                    │ user-service  │◄──│ goods-service │
                    │     :8001     │   │     :8002     │
                    │ MyBatis+MySQL │   │ MyBatis+MySQL │
                    └───────┬───────┘   │  OpenFeign    │
                            │           │  Sentinel     │
                     Nacos Config       │  StreamBridge │
                            │           └───────┬───────┘
                            ▼                   ▼
                    ┌───────────────┐   ┌───────────────┐
                    │  Nacos 注册   │   │  RabbitMQ     │
                    │  与配置中心   │   │ order-topic   │
                    └───────────────┘   └───────────────┘
                            │                   │
                            └───── Zipkin ◄─────┘  链路追踪
```

三个服务启动时都把自己注册到 Nacos；网关通过 `lb://服务名` 走负载均衡转发；
`goods-service` 下单时用 OpenFeign 反向调用 `user-service`，若后者不可用则由 Sentinel 触发 fallback 降级，
成功下单则通过 Spring Cloud Stream 把消息投递到 RabbitMQ 的 `order-topic`。

更细的链路拆解见 [docs/architecture.md](docs/architecture.md)。

## 技术栈

| 组件 | 版本 | 在本项目中的角色 |
| --- | --- | --- |
| JDK | 8 | 语言运行时 |
| Spring Boot | 2.6.13 | 应用骨架 |
| Spring Cloud | 2021.0.5 | Gateway / OpenFeign / Sleuth / Stream |
| Spring Cloud Alibaba | 2021.0.5.0 | Nacos Discovery、Nacos Config、Sentinel |
| MyBatis Spring Boot Starter | 2.2.2 | 数据访问（注解式 SQL） |
| MySQL Connector/J | 5.1.47 | JDBC 驱动（对应 MySQL 5.7） |
| Vue | 3（CDN） | 前端，无需 npm 构建 |
| Axios / Font Awesome | CDN | 请求与图标 |

> 版本被有意锁定在 JDK 8 + Spring Boot 2.6 这一教学版本线上，社区早已进入 Spring Boot 3 / JDK 17。
> 保持这一组合是为了让示例的每一步都能按文档复现；迁移到 3.x 的改动点列在
> [已知限制与后续方向](#已知限制与后续方向) 里。

## 目录结构

```
campus-trade-cloud/
├── api-gateway/          # 网关 :8000，含前端 static/index.html
├── user-service/         # 用户服务 :8001，库 campus_user
├── goods-service/        # 商品服务 :8002，库 campus_goods
├── db/                   # 建库建表与演示数据脚本
├── docs/                 # 架构说明与接口文档
├── pom.xml               # 父 POM，统一版本与依赖管理
├── LICENSE               # MIT
├── CHANGELOG.md
├── CONTRIBUTING.md
└── SECURITY.md
```

## 快速开始

### 前置要求

- JDK 8（本项目按 `source/target 1.8` 编译）
- Maven 3.6+
- 以下中间件，本机安装或用 Docker 起容器都可以：

| 中间件 | 建议版本 | 端口 | 说明 |
| --- | --- | --- | --- |
| MySQL | 5.7 | 3306 | 驱动锁 5.1.47，请对上 5.x；MySQL 8 需换 `mysql-connector-j` |
| Nacos | 2.1.x | 8848（+9848 gRPC） | 单机模式 `-m standalone` 即可，[官网](https://nacos.io) |
| RabbitMQ | 3.11.x | 5672（管理台 15672） | 需启用 `rabbitmq_management` 插件 |
| Zipkin | 2.24.x | 9411 | 可选，不起服务也能跑 |

### 1. 初始化数据库

两个库分别对应两个服务，互不共享连接：

```bash
mysql -u root -p < db/01_campus_user.sql
mysql -u root -p < db/02_campus_goods.sql
```

脚本自带 `CREATE DATABASE IF NOT EXISTS`，不需要先手工建库。

### 2. 启动 Nacos 与 RabbitMQ

Nacos 以单机模式启动（解压后 `startup.sh -m standalone`，Windows 用 `startup.cmd -m standalone`），
RabbitMQ 启动后可访问 http://localhost:15672 确认 broker 正常。

Zipkin 可选：不启动只是看不到调用链图，服务照常运行（上报失败会在日志里产生告警级记录）。

### 3. 在 Nacos 中创建配置（可选，但推荐）

`user-service` 演示了配置中心动态刷新。不创建配置也能启动（会回落到本地默认值），
创建后才能看到"改配置不用重启"的效果：

1. 打开 http://localhost:8848/nacos 登录控制台
2. 配置管理 → 新建配置
   - **Data ID**：`user-service.yaml`
   - **Group**：`DEFAULT_GROUP`
   - **格式**：YAML
   - **内容**：

     ```yaml
     campus:
       welcome: "欢迎来到校园二手交易平台（来自 Nacos 配置中心）"
     ```

### 4. 构建并启动服务

在项目根目录：

```bash
mvn clean package -DskipTests
```

分别在三个终端启动（顺序无要求，建议先起两个业务服务再起网关）：

```bash
java -jar user-service/target/user-service-1.0.0.jar
java -jar goods-service/target/goods-service-1.0.0.jar
java -jar api-gateway/target/api-gateway-1.0.0.jar
```

若在 IDE 里跑，主类分别是
`com.campustrading.userservice.UserApplication`、
`com.campustrading.goodsservice.GoodsApplication`、
`com.campustrading.apigateway.ApiGatewayApplication`。

数据库凭据通过环境变量注入，**默认值是 `root` 且空密码**，与多数本地安装不一致，
启动前请先设置：

```bash
# Linux / macOS
export MYSQL_HOST=127.0.0.1 MYSQL_PORT=3306 MYSQL_USERNAME=root MYSQL_PASSWORD=你的密码

# Windows PowerShell
$env:MYSQL_PASSWORD="你的密码"
```

### 5. 访问

- 前端页面：http://localhost:8000/
- Nacos 控制台：http://localhost:8848/nacos
- RabbitMQ 管理台：http://localhost:15672
- Zipkin：http://localhost:9411

页面上可以依次验证：商品列表（网关→商品服务）、按 ID 查用户、点击"立即购买"
（Feign 远程调用 + 投递 MQ）、在 Nacos 里改 `campus.welcome` 后刷新页面看公告语变化。

### 6. 观察降级

把 `user-service` 进程停掉，再在页面点一次"立即购买"：
预期返回 **"下单失败：用户不存在或服务降级！"**，
`goods-service` 日志出现 `user-service 调用失败，已触发降级`。这就是 Sentinel fallback 生效的表现。

## 配置项

所有中间件地址与数据库凭据都通过环境变量注入，未设置时使用本机默认值。

| 变量 | 默认值 | 使用方 |
| --- | --- | --- |
| `MYSQL_HOST` / `MYSQL_PORT` | `localhost` / `3306` | user-service、goods-service |
| `MYSQL_USERNAME` / `MYSQL_PASSWORD` | `root` / 空 | user-service、goods-service |
| `NACOS_SERVER_ADDR` | `127.0.0.1:8848` | 三个服务 |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` | `127.0.0.1` / `5672` | goods-service |
| `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | `guest` / `guest` | goods-service |
| `ZIPKIN_BASE_URL` | `http://127.0.0.1:9411` | 三个服务 |
| `USER_SERVICE_PORT` / `GOODS_SERVICE_PORT` / `GATEWAY_PORT` | `8001` / `8002` / `8000` | 各服务 |

`RABBITMQ_*` 要和你的 broker 一致，`NACOS_SERVER_ADDR` 三个服务必须指向同一个地址，
否则会出现"网关 503 但服务明明起来了"这类误导性现象。

## 接口一览

全部通过网关 `http://localhost:8000` 访问，均为返回纯文本或 JSON 的演示接口。

| 方法 | 路径 | 归属服务 | 说明 |
| --- | --- | --- | --- |
| GET | `/user/info/{id}` | user-service | 按 ID 查用户，供 Feign 调用；不返回密码 |
| GET | `/user/login?username=` | user-service | 登录示例，**不校验密码** |
| GET | `/user/register?username=&password=` | user-service | 桩接口，不写库 |
| GET | `/user/welcome` | user-service | 返回 Nacos 配置中的公告语 |
| GET | `/goods/list` | goods-service | 商品列表 |
| GET | `/goods/detail/{id}` | goods-service | 商品详情 |
| GET | `/goods/order/{goodsId}/{userId}` | goods-service | 下单示例：Feign + MQ |

请求/响应示例见 [docs/api.md](docs/api.md)。

## 代码在哪看某个特性

| 想看什么 | 打开这里 |
| --- | --- |
| 服务如何注册 | `user-service/.../UserApplication.java` 的 `@EnableDiscoveryClient` |
| 配置中心与热刷新 | `user-service/.../controller/UserController.java` 的 `@RefreshScope` + `@Value`，以及 `src/main/resources/bootstrap.yml` |
| 声明式调用与降级 | `goods-service/.../feign/UserFeignClient.java`、`goods-service/.../fallback/UserFeignFallback.java` |
| 消息投递 | `goods-service/.../controller/GoodsController.java` 的 `StreamBridge#send`，绑定名 `orderOutput-out-0` |
| 路由规则 | `api-gateway/src/main/resources/application.yml` 的 `spring.cloud.gateway.routes` |
| 前端如何调后端 | `api-gateway/src/main/resources/static/index.html` |
| 不依赖中间件的测试写法 | `goods-service/src/test/.../GoodsControllerTest.java` |

## 测试与构建

```bash
mvn clean verify          # 编译 + 单元测试，不需要任何中间件
```

现有 9 个测试覆盖商品查询与下单降级分支、下单消息文本与 binding 名（`GoodsControllerTest`）、
fallback 行为（`UserFeignFallbackTest`）、登录示例与"密码不出现在 JSON 中"的回归断言（`UserControllerTest`）。

`StreamBridge` 是 final 类，在当前版本线的 Byte Buddy 上无法被打桩，
所以"消息确实被发出"这一段没有单元测试覆盖，需要在 RabbitMQ 管理台按
[快速开始第 6 步](#6-观察降级)自行确认——这一点我在[技术亮点第 2 条](#技术亮点)和
[我做的几个判断](#我做的几个判断)②里都写明了，不想用虚假的覆盖率说话。

构建与测试只需要 JDK 8 + Maven，任何机器上都能复现同样的结果。

## 已知限制与后续方向

按"上手时最容易踩到"的顺序列出。前 8 条是我明确接受的取舍，最后一组是我下一步想做的改进。

**已知限制**

1. **没有鉴权**：任何接口都匿名可访，网关也没有过滤器；Nacos、RabbitMQ 用默认凭据。
2. **下单不写库**：`db/02_campus_goods.sql` 里的 `orders` 表是预留结构，没有消费者写入；库存也不扣减。
3. **`/goods/order/**` 用 GET 改变状态**：不符合 HTTP 语义，会带来 CSRF 与浏览器预取问题。
4. **注册接口是桩**：`/user/register` 只回显参数，不落库、不加密密码。
5. **`User.password` 为明文列**：实体上已用 `@JsonIgnore` 阻止对外返回，但库内仍是占位明文。
6. **技术栈版本线偏旧**：JDK 8 + Spring Boot 2.6 + MySQL 驱动 5.1.47，与社区主流的 Boot 3 / JDK 17 不同。
7. **前端走公网 CDN**：`index.html` 从 unpkg / cdnjs 加载依赖，离线环境会白屏。
8. **SQL 日志打到标准输出**：`log-impl: StdOutImpl` 只适合演示。

**如果继续做，优先级依次是**

1. `order` 改 POST + 加消费端把消息落成 `orders` 记录并扣减库存（带幂等键）
2. Spring Security + JWT 接管网关鉴权，密码改 BCrypt 存储
3. 依赖升级到 Spring Boot 3 / JDK 17：Sleuth → Micrometer Tracing、`javax.*` → `jakarta.*`、驱动 → `mysql-connector-j`
4. 前端拆成独立 Vite 工程，依赖本地化以摆脱公网 CDN
5. 引入 `spring-cloud-stream-test-binder` 补齐"消息确实发出"的断言

## 常见问题

**启动报 `Communications link failure` / `Access denied for user 'root'`**
MySQL 没起来、地址端口不对，或 `MYSQL_PASSWORD` 没设置成你本机 root 的密码。

**页面打开是 503 / `Unable to find instance for user-service`**
服务还没注册上。先在 Nacos 控制台查看服务列表，确认三个服务的 `NACOS_SERVER_ADDR` 指向同一个地址。

**`/user/welcome` 一直显示默认文案**
Nacos 里没有 `user-service.yaml`，或 Data ID / Group / 格式写错。见[快速开始第 3 步](#3-在-nacos-中创建配置可选但推荐)。

**下单永远提示降级**
`user-service` 没启动，或 `feign.sentinel.enabled` 被改成 `false` 导致 fallback 未生效。

**RabbitMQ 连不上但服务照常启动**
`goods-service` 启动时不会强依赖 MQ，但点击购买会在 `StreamBridge#send` 抛错。检查 `RABBITMQ_HOST/PORT/USERNAME/PASSWORD`。

**Zipkin 里看不到链路**
Zipkin 没启动，或 `ZIPKIN_BASE_URL` 不对。缺 Zipkin 不影响功能，只是没有调用链图。

## 关于贡献与交流

这是个人学习项目，代码结构与文档都按我自己的学习目标定型，日常以复现和扩展其中的技术点为主。

- 有问题、想法，或者发现我写得不准确的地方 → 欢迎
  [开 Issue](https://github.com/YANG1111-AI/campus-trade-cloud/issues) 讨论，我看到就会回
- 想改造成适合自己场景的版本 → 直接 fork。[CONTRIBUTING.md](CONTRIBUTING.md)
  记录了这个代码库的既有约定（包前缀、配置外部化规则、测试为什么不依赖中间件、
  以及几个写错层级就静默失效的配置位置），照着改可以少踩坑
- 安全问题 → 请先读 [SECURITY.md](SECURITY.md)。里面缺的那些防护是有意留下的取舍，
  不是疏忽；**不要把它部署到公网**

## 许可证与作者

- 许可证：[MIT](LICENSE)
- 作者：[YANG1111-AI](https://github.com/YANG1111-AI)
- 仓库：https://github.com/YANG1111-AI/campus-trade-cloud

## 免责声明

本项目为个人学习作品。文中的库名、账号、商品与用户数据均为虚构演示内容，与任何真实系统无关。
示例依赖的第三方商标、组件与文档版权归各自所有者所有。
