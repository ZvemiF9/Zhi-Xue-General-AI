# 运行检查记录

检查日期：2026-11-07（北京时间）。检查对象为迁移后的 `ZhiXueGeneral-AI` 后端项目。

## 结论

项目可以编译和打包，选定的四项本地测试通过；当前环境尚不能完成正常启动和业务联调。已修复网关在缺少鉴权排除路径配置时的空指针异常。修复后，实际启动仍被 Nacos 连接失败阻断。

检查使用与目标项目源码一致的临时副本，修复及公开仓库所需的配置调整已同步到项目目录。原来的 `tj-` 项目保留。

## 已执行检查

| 检查 | 结果 |
| --- | --- |
| JDK | 17.0.12 |
| Maven | 3.9.12 |
| 全部工程编译、测试代码编译、JAR 打包 | 28/28 成功 |
| 网关缺少排除路径配置时初始化 | 修复前失败；修复后通过 |
| 网关保留自定义排除路径并添加默认路径 | 通过 |
| 优惠码生成与解析样例 | 通过 |
| 加密样例 | 通过 |
| 全量业务集成测试 | 未执行；依赖外部服务，部分涉及短信、支付/退款及云模型调用 |

最终构建命令：

```powershell
mvn -o -B -ntp "-Dtest=AuthPropertiesTest,CodeUtilTest,EncryptTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.test.redirectTestOutputToFile=true" package
```

`-o` 使用本机已缓存的依赖。首次构建应去掉 `-o`。

## 启动结果和修复

首次启动 `zxt-gateway.jar`，日志出现：

```text
NullPointerException: Cannot invoke "java.util.Set.add(Object)" because "this.excludePath" is null
com.tianji.gateway.config.AuthProperties.afterPropertiesSet
```

`AuthProperties.excludePath` 未初始化，在 Nacos 未提供该配置时为 `null`。已初始化为空 `HashSet`，并添加两项回归测试验证默认路径和用户配置路径。修复保留原有的五项默认放行路径，不扩大放行范围。

修复后重新打包并实际启动，空指针异常已消失，随后出现：

```text
NacosException: Client not connected, current status:STARTING
```

检查程序在启动失败后结束了本次 Java 进程。没有保留后台运行的网关，也没有执行真实支付、退款、短信或模型调用。

## 外部环境

- `192.168.150.101` 的 8848、9848、3306、6379、16379、27017、5672、9200 端口在本次 TCP 探测中均超时。
- 本机 `127.0.0.1:3306`、`127.0.0.1:6379` 可建立 TCP 连接；这不代表对应账号、业务数据库或 Redis 向量功能已配置。
- 本机 `127.0.0.1:8848` 未连接成功。
- 启动日志显示 `gateway-service.yaml`、`shared-spring.yaml`、`shared-redis.yaml`、`shared-logs.yaml` 未加载到有效内容。
- 仓库缺少整套数据库初始化数据以及 Nacos 配置导出，需使用实际部署资料补齐。

## GitHub 发布处理

短信密钥、支付私钥、数据库/缓存口令及测试令牌已从准备提交的源码中移除，改用外部配置或操作系统环境变量。原有本地配置值保存在被 Git 忽略的 `application-local-secrets.properties` 中。证书密钥库仍留在本机，`*.jks`、本地密钥配置、IDEA 文件、构建产物和日志不进入 Git 提交。

启动步骤、配置模板和密钥库生成方式见项目根目录的 README。
