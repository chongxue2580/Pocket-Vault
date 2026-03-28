# Pocket Vault

Pocket Vault（拾光盒）是一个 Android 本地离线收藏应用，支持链接、文字、图片、账号密码四类内容的统一收纳、搜索、分类与详情查看。

## 当前工程

- UI：Jetpack Compose + Material 3
- 架构：UI / Domain / Data / Core 分层
- 数据：Room
- 依赖注入：Hilt
- 图片：Coil
- 安全：Android Keystore + Biometric

## 本地运行前提

- JDK 17
- Android SDK
- Android Studio 或可用的 Gradle 环境

## 当前限制

这个仓库目前缺少 `gradlew`、`gradlew.bat` 和 `gradle-wrapper.jar`。如果要在命令行构建，需要先在具备 Gradle 的机器上生成 Wrapper：

```bash
gradle wrapper
```

之后可使用常规命令：

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

## 最近修复

- 修复了解锁 ViewModel 状态复用导致的锁屏绕过问题
- 为密码详情增加了 PIN 二次验证流程
- 隐藏密码时会清空已解密内容，避免敏感信息长期停留在内存态 UI 中
