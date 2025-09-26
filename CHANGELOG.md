# 变更日志

本项目的所有显著变更将记录在此文件中。

格式参考 Keep a Changelog，版本号遵循语义化版本。

## [Unreleased]
- 敬请期待。

## [0.2] - 2025-09-27
### 新增
- 新增：英文介绍文档 `README-en.md` 与两种尺寸的项目 Logo。
- 新增：在 `Iterable<Pack>#toRequest()` 中对资源包去重，避免重复发送同一包。
- 新增：S3 文件上传服务（`amazon_s3`），支持可配置的 endpoint、region、bucket、目录等。
- 新增：上传服务启动清理逻辑，支持自托管/ S3 中清理未被引用的对象。
- 新增：多个函数的必要注释与文档化内容。
- 新增：`.gitignore` 中对 `run/` 目录的忽略。

### 优化
- 优化：`UploadService#upload()` 支持传入预先计算的哈希以避免重复计算。
- 优化：`paper-plugin.yml` 的版本变量替换与 `build.gradle.kts` 的任务结构。
- 优化：`PackItem` 系列结构，将 `hash` 上移到父类统一管理；`toResourcePackInfo()` 的异常抛出与结构更清晰。
- 优化：资源包清理流程、若干函数命名与注释本地化；`Options` 字段命名与冗余接口移除。
- 优化：`BinaryCache#set()` 支持传入 `null` 即删除；整体代码结构与日志工具函数。
- 优化：与 CommandAPI 的对接与 `PermPacks` 中部分函数顺序。

### 修复
- 修复：`FilePackItem` 在初始化后未触发上传的问题。
- 修复：URI 序列化问题与 S3 `endpoint` 缺失 schema 的问题。
- 修复：部分字符串格式化中误用原始 `toString()` 的问题。
- 修复：`cleanup()` 同步修改遗漏导致的逻辑问题。
- 修复：`BinaryCache#get()` 潜在未处理异常；移除未使用的 `BinaryCache#remove()`。
- 修复：移除未使用的 `Logger#error()` 扩展方法。

### 文档
- 更新：`README.md` 调整表格与内容；补充项目概览与多语言说明。

## [0.1.1] - 2025-09-22
### 新增 / 变更
- 更新：版本号至 `v0.1.1`。
- 允许上传服务校验并使缓存的 URL 失效；改进配置与工具的可读性。

### 修复
- 修复：`SelfHostHandler` 中哈希包含前导斜杠的问题。
- 修复：`FileMetaRepository#clear()` 中潜在的 NPE。
- 修复：`File.sha1()` 的线程安全问题。
- 修复：`Options` 使用未初始化 `UploadService` 的问题。
- 修复：URL 序列化问题与编译错误若干。

## [0.1.0] - 2025-09-21
### 初始发布
- 基于权限的多层资源包栈下发。
- 支持 `url` 与 `file` 两类条目；内置自托管上传分发。
- 支持热重载与基础诊断命令。


[Unreleased]: https://github.com/Pectics/PermPacks/compare/v0.2...HEAD
[0.2]: https://github.com/Pectics/PermPacks/compare/v0.1.1...v0.2
[0.1.1]: https://github.com/Pectics/PermPacks/compare/v0.1.0...v0.1.1
[0.1.0]: https://github.com/Pectics/PermPacks/releases/tag/v0.1.0
