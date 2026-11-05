# 参与贡献

## 关于这个项目

本项目是 [YANG1111-AI](https://github.com/YANG1111-AI) 的独立学习作品，
代码结构与文档都按我自己的学习目标定型。

- 想讨论实现思路、指出写得不对的地方 → 开 Issue 就好，我会尽量回应
- 想用在自己项目里 → 直接 fork，下面的约定可以帮你少踩坑
- 想提 PR → 请先开 Issue 把问题聊清楚。改动方向如果和"最少代码讲清每个组件"的定位不一致，
  我更建议放到 fork 里做

下面记录的是这个代码库的既有约定，照着写能保持整体风格一致。

## 项目定位（决定了什么改动适合放进这个示例）

本仓库的目标是"用最少的代码把每个微服务组件串起来"，而不是可上线的交易平台：

- 合适：能更清楚地展示某个组件的配置、更贴近真实调用链的最小实现
- 不合适：新增业务功能（订单、支付、图片上传）、把示例改造成"生产级"而失去可读性

## 构建与测试

```bash
mvn clean verify        # 编译 + 单元测试，不需要 MySQL / Nacos
```

要求 JDK 8 与 Maven 3.6+。中间件（MySQL 5.7、Nacos 2.1.x、RabbitMQ 3.11.x、Zipkin 2.24.x）
本机安装或用 Docker 起容器都可以，具体版本与端口见
[README 快速开始](README.md#快速开始)。

## 代码约定

**Java**

- 缩进 4 空格，遵循根目录 [`.editorconfig`](.editorconfig)
- 包名统一 `com.campustrading.<module>`，fork 后请整体替换成你自己的前缀
- 依赖用构造器注入，不用字段 `@Autowired`
- 日志用 SLF4J，不用 `System.out.println`
- 注释说明"为什么"，不复述代码在做什么；面向中文读者，注释用中文

**单元测试**

- 只写纯单元测试（Mockito mock 接口 + AssertJ 断言），不要引入需要连接中间件的测试
- `StreamBridge` 等 final 类在当前依赖版本线上无法被打桩（Byte Buddy 版本较旧），
  需要断言消息发送时请把逻辑提取成可单测的纯函数，参考 `GoodsController#buildOrderMessage`

**POM / 依赖**

- 版本一律集中在根 `pom.xml` 的 `<properties>` 与 `<dependencyManagement>`，子模块不重复声明
- 本项目锁定在 JDK 8 + Spring Boot 2.6 教学版本线，升级依赖请单独开分支验证

**配置**

- 任何中间件地址、账号、密码都走 `${ENV:默认值}`，禁止把真实凭据提交进仓库
- 新增配置项要同步更新 README 的「配置项」表格
- 注意配置层级：RabbitMQ 连接在 `spring.rabbitmq.*`，Stream 绑定在
  `spring.cloud.stream.bindings.<binding>.*`，Zipkin 在 `spring.zipkin.*`。
  写在错误层级下不会报错，只会静默失效

**SQL**

- 表结构变更改 `db/` 下的脚本，保持 `01_` / `02_` 的编号顺序以便按序执行
- 演示数据必须是明显虚构的值，不要使用看起来像真实凭据的字符串

## 提交信息

使用 [Conventional Commits](https://www.conventionalcommits.org/)：

```
fix(goods-service): 修正 RabbitMQ 连接配置的错误层级
docs: 补充 Nacos 配置中心 dataId 说明
```

作用域用模块名（`user-service` / `goods-service` / `api-gateway` / `db` / `docs`）。

## 报告安全问题

见 [SECURITY.md](SECURITY.md)。示例里缺少的鉴权、校验、事务等防护是有意留下的取舍，
请把它当作组件演示而不是可部署的后端。
