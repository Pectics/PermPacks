<p style="text-align: center"><img src="assets/logo_180x.png" width="180" alt="PermPacks Logo"></p>
<p style="text-align: center"><b>PermPacks</b></p>

<p style="text-align: center">
  <a href="README-en.md">English</a> | 
  <span>中文</span>
</p>

基于权限为玩家自动下发「多层资源包栈」的 Paper 插件。支持远程 URL 与本地文件两类资源来源，内置自托管与 Amazon S3 上传分发，实现即开即用的稳定下发；可选拦截外部资源包，保障栈一致性。

### 功能亮点

- 权限驱动的资源包栈：依据玩家权限筛选 `packs.yml` 中配置的包，按优先级升序整体下发；后加载覆盖先加载。
- 多来源与统一下发：支持 `url` 与 `file` 两种条目，统一构造请求并一次性发送给客户端。
- 可插拔上传服务：内置 `self_host` 轻量自托管与 `amazon_s3` 云端存储，自动计算与校验 SHA-1，避免重复上传。
- 外部包拦截：配合 ProtocolLib，拦截非本插件标记的资源包推送，避免被其他插件/服务打断。
- 热重载与排查：命令热重载配置、即时重新分发；查询指定包详情与玩家实际下发状态。

### 运行环境

- Paper 1.21+，Java 21
- 可选依赖：ProtocolLib（启用外部包拦截时建议安装）

### 安装

1) 将 `PermPacks-<version>-all.jar` 放入 `plugins/`
2) 首次启动生成 `config.yml` 与 `packs.yml`
3) 按需修改配置后，执行 `/permpacks reload packs` 即时生效

注：插件数据目录会生成缓存与仓库文件夹（如 `.filerepo/`），安全删除后会自动重建。

### 配置详解（`config.yml`）

```yaml
block_other_packs: false

file_upload:
  enabled: false
  cleanup: false
  service: self_host # amazon_s3, self_host
  self_host:
    host: 127.0.0.1
    port: 7077
  amazon_s3:
    endpoint: s3.amazonaws.com
    region: cn-north-1
    bucket: your-bucket-name
    directory: uploads
    access_key_id: ""
    secret_access_key: ""
    path_style_access: false
    chunked_encoding: true
```

- `block_other_packs`：开启后拦截非本插件标记的资源包（需 ProtocolLib）。
- `file_upload.enabled`：启用后允许在 `packs.yml` 使用 `file` 条目；关闭时所有 `file` 将被忽略。
- `file_upload.service`：选择上传后端，内置 `self_host` 与 `amazon_s3`。
- `file_upload.cleanup`：启动时清理后端中不再被引用的对象，仅保留当前配置涉及的文件。
- `self_host.*`：自托管 HTTP 服务监听地址与端口。
- `amazon_s3.*`：S3 客户端与 Bucket 设置；支持 Path-Style 与 Chunked Encoding。

### 资源包定义（`packs.yml`）

优先级规则：优先级默认 0，允许负数；按优先级升序加载，同级按出现顺序加载，后出现覆盖先出现。

```yaml
example:
  permission: permpacks.pack.example
  priority: 10
  items:
    - url: https://example.com/high.zip
      hash: d41d8cd98f00b204e9800998ecf8427e
    - file: plugins/OtherPlugin/assets.zip
      hash: 900150983cd24fb0d6963f7d28e17f72
```

- `permission`：拥有该权限的玩家将获得此包（以及其 `items`）。
- `priority`：较小的数值更先加载；后加载覆盖先加载。
- `items.url`：资源包直链；建议提供官方 SHA-1。
- `items.file`：服务器本地文件路径；需启用上传服务。插件会校验文件、计算 SHA-1，并上传到自托管或 S3，生成可访问 URL。

修改完成后使用：`/permpacks reload packs` 重新分发在线玩家。

### 命令与权限

- `/permpacks reload configs`（`permpacks.admin`）：重载 `config.yml` 与 `packs.yml`。
- `/permpacks reload packs`（`permpacks.admin`）：按当前配置重新缓存并分发。
- `/permpacks info <PACK_ID>`（`permpacks.admin`）：查看指定包的权限与条目明细（URL/文件与哈希）。
- `/permpacks check <PLAYER>`（`permpacks.admin`）：对比玩家拥有权限与实际下发，显示 ✔/✘ 状态。

别名：`/ppacks`、`/pp`

### 工作机制（实现细节概览）

- 启动：`PermPacks` 初始化 `Options`、`BinaryCache`、上传服务（`SelfHostService`/`S3Service`），读取并缓存 `packs`。
- 分发：`Packer.distribute(player)` 依据权限筛选 `Pack` 并构造请求，一次性发送所有条目；同时将每个条目标记到 `PackPacketTracker`。
- 拦截：如启用 `block_other_packs`，`PackBlocker` 在 ProtocolLib 层拦截未被标记的资源包数据包。
- 清理：若开启 `file_upload.cleanup`，启动时对自托管目录或 S3 Bucket 进行不在引用集合内的对象清理。

### 从源码构建

```bash
./gradlew build
```

- 产物：`build/libs/PermPacks-<version>-all.jar`
- 运行开发服：`./gradlew runServer`（Paper 1.21）

### 许可

基于 GPL-3.0 授权发布，欢迎提交 Issue / PR 参与改进。
