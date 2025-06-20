## Server Lab Bot v2

Server Lab Bot 是由 [TASA-Ed 工作室](https://www.tasaed.top/)制作的一款机器人，可以用于查询SCP：SL服务器人数。

本机器人基于 [云湖Java SDK](https://github.com/daenmax/yhchat-sdk-core) 。

## 贡献指南

1. 安装 JDK 17
2. 安装 Maven 3.9
3. 克隆此仓库
4. 运行 `mvn clean install -U` 和 `mvn dependency:resolve` 安装依赖
5. 随后可通过 Pull Request 提交代码。

需要注意的是，打包后除 `README.md` 以外的 `.md` 文件均需要放在 `jar` 运行目录下，否则机器人将无法正常运行。