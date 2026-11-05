# 更新日志

遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 与
[语义化版本](https://semver.org/lang/zh-CN/)。

## [1.0.0] - 开源整理版

### Added
- MIT 许可证，以及 README、CONTRIBUTING、SECURITY、CHANGELOG
- README 按展示用途重写：项目定位、技术亮点、架构判断与取舍、可按步骤复现的能力验证表
- 作者署名：LICENSE 版权行、根 POM 的 `<url>` / `<scm>` / `<developers>`、前端页面页脚
- `docs/architecture.md` 组件说明与 `docs/api.md` 接口文档
- `db/` 目录：可重复执行的建库建表与演示数据脚本，自带 `CREATE DATABASE IF NOT EXISTS`
- `.editorconfig`、`.gitattributes`
- goods-service 与 user-service 的纯单元测试：商品查询、下单降级分支、fallback 行为、
  消息文本、密码不外泄（共 9 个测试，不需要任何中间件）

### Changed
- groupId 与包路径由个人前缀统一迁移为 `com.campustrading.*`
- 中间件地址与数据库凭据全部改为 `${ENV:默认值}` 注入，不再硬编码
- 根 POM：移除重复声明的 `<module>`，补充 `name` / `description` / `licenses`，
  版本集中到 `<properties>` 与 `<dependencyManagement>`
- 各服务补充 `spring-boot-maven-plugin`，`mvn package` 现在产出可执行 fat jar
- 控制器改为构造器注入，`System.out.println` 替换为 SLF4J 日志
- 各服务日志级别前缀输出 `[服务名,TraceId,SpanId]`，便于与 Zipkin 调用链对照
- `User.password` 增加 `@JsonIgnore`，不再随 REST / Feign 响应返回
- MyBatis 查询由 `SELECT *` 改为显式列名
- 前端页面改为标注个人项目与仓库链接，去掉课程字样，并移除未被使用的 Element Plus CDN 依赖

### Fixed
- goods-service 的 `spring.zipkin.*` 曾错误嵌套在 `spring.cloud` 下，导致 Zipkin 上报配置未生效
- goods-service 的 RabbitMQ 连接参数曾写在 `spring.cloud.stream.rabbit.*`，
  实际应属于 `spring.rabbitmq.*`
- `orderOutput-out-0` 的 Rabbit 生产者参数归位到
  `spring.cloud.stream.rabbit.bindings.<binding>.producer.*`
- `user-service` 的 `bootstrap.yml` 中 Nacos 地址改为可注入，避免换环境就要改文件

### Removed
- Maven archetype 残留：根目录 `src/main/java/org/example/App.java`、`src/test/java/org/example/AppTest.java`
- 各模块根包下的 Hello-World 占位类
- `.idea/` IDE 本地配置与 `target/` 构建产物
- SQL 脚本头部的 Navicat 来源服务器标识与导出时间
