# <p align="center"><img src="assets/logo.png" width="180"></p>
# <p align="center"><b>PermPacks</b></p>

通过权限节点为玩家自动分发多层资源包栈的 Paper 插件，并为本地资源包提供开箱即用的自托管分发服务。

## ✨ 功能概览

- **权限驱动的资源包栈**：根据玩家拥有的权限节点，从 [`packs.yml`](src/main/resources/packs.yml) 中挑选对应的资源包并按优先级排序后整体下发，后加载的资源包会覆盖先前资源包的内容。
- **多来源资源包**：同时支持远程 URL 和服务器本地文件两种资源包来源，统一转换为 Adventure API 的请求结构并发送给客户端。
- **自托管上传服务**：内置基于 `HttpServer` 的轻量级文件服务器，自动计算 SHA-1 并缓存上传结果，极大降低本地资源包分发的部署成本。
- **资源包防护**：如安装 ProtocolLib，可阻止服务器向玩家推送未在栈内登记的资源包，保障栈的稳定性与一致性。
- **热更新体验**：通过命令热重载配置或重新分发当前在线玩家的资源包，同时提供玩家栈状态与具体包信息的查询能力。

## 🛠️ 环境需求

- **服务器核心**：Paper 1.21 及以上版本。
- **JRE**：Java 21。
- **可选依赖**：
  - [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/) —— 启用“阻止外部资源包”功能时需要。

## 📦 安装步骤

1. 从发行页或自行构建获取 `PermPacks-x.y.z.jar`。
2. 将插件放入 `plugins/` 目录并启动服务器以生成默认配置。
3. 按需修改 `config.yml` 与 `packs.yml` 后重启服务器或使用命令热重载。

> 本插件会在数据目录下维护 `.cache` 与 `.filerepo/` 用于缓存上传结果及本地文件元数据，删除后将自动重建。

## ⚙️ 配置说明

### `config.yml`

| 选项                           | 默认值         | 说明                                               |
|------------------------------|-------------|--------------------------------------------------|
| `block_other_packs`          | `false`     | 是否拒绝所有未在栈内登记的资源包推送（需 ProtocolLib）。               |
| `file_upload.enabled`        | `false`     | 是否启用本地文件上传能力。关闭时，`packs.yml` 中的 `file` 类型条目会被忽略。 |
| `file_upload.service`        | `self_host` | 指定上传后端。当前内置 `self_host`，未来可扩展 S3 等服务。            |
| `file_upload.self_host.host` | 无           | 自托管服务绑定的地址，必须在启用上传时配置。                           |
| `file_upload.self_host.port` | 无           | 自托管服务监听端口，需对外可访问。                                |

### `packs.yml`

配置结构示例：

```yaml
example_pack:
  permission: permpacks.pack.example
  priority: 10
  items:
    - url: https://example.com/high-priority.zip
      hash: d41d8cd98f00b204e9800998ecf8427e
    - file: plugins/OtherPlugin/assets.zip
      hash: 900150983cd24fb0d6963f7d28e17f72
```

- `permission`：拥有该权限的玩家将获得本条资源包。
- `priority`：数值越小越先加载，后加载的资源包会覆盖先前资源包内容。允许负数，同优先级下按配置顺序加载。
- `items`：资源包条目列表。
  - `url`：可直接访问的资源包下载地址。`hash` 推荐填写官方提供的 SHA-1，缺失时客户端会提示验证失败。
  - `file`：服务器本地资源包相对路径，需启用上传服务。插件会自动计算 SHA-1 并自托管到 `http://<host>:<port>/` 下的下载链接。

修改完毕后，可执行 `/permpacks reload packs` 即时重新分发，无需重启。

## 🔧 自托管上传服务

启用 `file_upload.enabled` 并选择 `self_host` 后，插件将在后台启动一个轻量级 HTTP 服务：

- 服务会在配置的 `host:port` 上监听，并将玩家访问重定向到 `.filerepo/` 中缓存的资源包文件。
- 当 `packs.yml` 中包含 `file` 条目时，插件会先验证本地文件是否存在、可读，然后计算 SHA-1 并复制到仓库目录；后续同哈希的资源包将直接命中缓存，避免重复上传。
- URL 形如 `http://<host>:<port>/<sha1>`，客户端访问时会携带合适的 `Content-Disposition` 头保留原始文件名。

## 🧭 命令与权限

| 命令                          | 权限                | 描述                                        |
|-----------------------------|-------------------|-------------------------------------------|
| `/permpacks reload configs` | `permpacks.admin` | 重新加载 `config.yml` 与 `packs.yml`，并重建资源包缓存。 |
| `/permpacks reload packs`   | `permpacks.admin` | 使用当前配置重新缓存并向在线玩家分发资源包。                    |
| `/permpacks info <包ID>`     | `permpacks.admin` | 查看某个资源包的权限、条目明细及哈希信息。                     |
| `/permpacks check <玩家>`     | `permpacks.admin` | 检查玩家当前拥有的资源包权限与已下发状态，显示 ✔/✘ 标记。           |

默认主命令同时提供别名 `/ppacks` 与 `/pp`。

## 🧱 运行机制

1. 插件启用时初始化缓存、配置与上传服务，并自动加载所有配置好的资源包栈。
2. 玩家加入或执行重载命令时，`Packer` 会根据权限筛选资源包条目，构造 Adventure `ResourcePackRequest` 并一次性发送。
3. 若启用阻止外部资源包，`PackPacketTracker` 会记录合法资源包的 URL/Hash，`PackBlocker` 则在 ProtocolLib 层拦截其他来源的资源包推送。

## 🏗️ 从源码构建

```bash
./gradlew build
```

- 构建流程默认使用 Java 21 toolchain，并在 `build/libs/` 下生成包含依赖的阴影包（Shadow Jar）。
- 可使用 `./gradlew runServer` 启动一份开发用 Paper 服务器进行本地调试。

## 🤝 贡献指南

欢迎通过 Issue 或 Pull Request 反馈需求与问题。提交前请确保：

- 代码遵循 Kotlin 语言规范并保持模块化设计。
- 对配置、命令或行为的更改在 README 中进行了同步说明。
- 运行 `./gradlew build` 以确保项目能够成功编译。

## 📄 许可证

本项目基于 [GPL-3.0 License](LICENSE) 发布，欢迎二次开发。
