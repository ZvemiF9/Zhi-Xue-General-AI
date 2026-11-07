# ZhiXueGeneral-AI

基于 Spring Boot、Spring Cloud、Spring AI 的在线学习平台后端。项目包含网关、用户、认证、课程、学习、交易、支付、消息、搜索、营销、数据和 AI 等服务，模块目录统一使用 `zxt-` 前缀。

## 构建与测试

需要 JDK 17、Maven 3.9 和可访问的 Maven 依赖仓库。IDEA 中打开根目录的 `pom.xml`，将项目及 Maven 的 JDK 都设为 17。

```powershell
# 编译所有模块、编译测试代码并生成 JAR；不执行集成测试
mvn -B -ntp -DskipTests package

# 执行不需要数据库、支付平台或 AI 接口的四项测试，并打包全部模块
mvn -B -ntp "-Dtest=AuthPropertiesTest,CodeUtilTest,EncryptTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.test.redirectTestOutputToFile=true" package
```

项目中的其他测试包含数据库读写、短信、真实支付/退款和云模型调用。应先配置独立测试环境，再按测试类执行；不要把上述四项测试通过理解为全部业务集成测试通过。

## 运行前准备

这是多服务项目，完整启动需要外部中间件和业务数据。源码未包含完整部署环境，也未包含全部数据库初始化脚本和 Nacos 配置导出文件。

1. 准备 Nacos，将各模块 `application-local.yml` 中的服务器地址、命名空间和注册 IP 改为实际环境。原示例配置指向 `192.168.150.101:8848`，注册 IP 为 `192.168.150.1`。
2. 在 Nacos 中配置各服务的 `${spring.application.name}.yaml`，以及模块引用的 `shared-spring.yaml`、`shared-redis.yaml`、`shared-logs.yaml`、`shared-mybatis.yaml`、`shared-feign.yaml`、`shared-mq.yaml`、`shared-seata.yaml`、`shared-xxljob.yaml`。
3. 按模块配置 MySQL、Redis、RabbitMQ、Elasticsearch、MongoDB，以及使用到的 Seata、XXL-JOB。AI 模块中的 Redis 向量存储还需要支持相关向量检索能力的 Redis 服务。普通 Redis 连接成功并不能证明向量检索可用。
4. 导入实际业务数据库结构和初始数据。仓库中仅有 AI 聊天记录的局部建表脚本，不能代替整套数据库。
5. 配置模型、对象存储、短信、支付等实际使用功能的密钥。
6. 准备认证服务的本地密钥库，再按依赖顺序启动认证、用户及业务服务，最后启动网关和 AI 服务进行联调。

## 本地密钥配置

仓库中的密钥值已改为 `${变量名}`。复制模板，并填写自己的值：

```powershell
Copy-Item application-secrets.example.properties application-local-secrets.properties
```

`application-local-secrets.properties` 被 Git 忽略。迁移时生成的本地文件保留了原配置值；该文件不会推送到 GitHub。Spring 服务可以通过下面的启动参数读取它；操作系统环境变量也可以提供同名配置。

```powershell
# 从项目根目录运行网关
java -jar zxt-gateway/target/zxt-gateway.jar --spring.config.additional-location=optional:file:./application-local-secrets.properties

# 从项目根目录运行 AI 服务
java -jar zxt-aigc/target/zxt-aigc.jar --spring.config.additional-location=optional:file:./application-local-secrets.properties
```

在 IDEA 的运行配置中，将工作目录设为项目根目录，并将 `--spring.config.additional-location=optional:file:./application-local-secrets.properties` 加入程序参数。

测试代码里的 `System.getenv(...)` 直接读取操作系统环境变量，不会自动读取这个 properties 文件。`AppTest` 需要 `ALIYUN_API_KEY` 和 `TEST_USER_TOKEN`；`AliPayTest` 需要 `ALIPAY_TEST_PRIVATE_KEY`、`ALIPAY_TEST_PUBLIC_KEY`，且必须先确认测试商户、订单号和支付环境。

## 认证密钥库

`*.jks` 等私钥文件已被 Git 忽略。本机原有文件仍在；从 GitHub 新克隆项目时，需要自行提供密钥库。可交互生成开发环境密钥库：

```powershell
keytool -genkeypair -alias tjxt -keyalg RSA -keysize 2048 -validity 365 -storetype JKS -keystore zxt-auth/zxt-auth-service/src/main/resources/tjxt.jks
```

按提示设置口令和证书信息，然后配置 `AUTH_KEYSTORE_PASSWORD`、`AUTH_KEY_PASSWORD`。网关及资源服务还需要在 Nacos 中配置与认证服务一致的公钥/JWK 获取地址。

## 服务入口

| 服务 | 模块 | 配置端口 |
| --- | --- | --- |
| 网关 | `zxt-gateway` | 10010 |
| 认证 | `zxt-auth/zxt-auth-service` | 8081 |
| 用户 | `zxt-user` | 8082 |
| AI | `zxt-aigc` | 8094 |

以上是仓库中的默认端口；外部配置可能覆盖它们。具体运行验证结果见 [运行检查记录](docs/runtime-check.md)。
