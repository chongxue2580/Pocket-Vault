# 建议命令

## 当前仓库现状
- 当前仓库尚未初始化为 Android/Gradle 工程，因此下面与 Gradle 相关的命令是“项目脚手架完成后”的预期命令，不保证现在即可执行。

## Windows 常用基础命令
- 进入目录：`cd D:\桌面\Pocket Vault`
- 列出目录：`Get-ChildItem` 或 `dir`
- 递归查找文件：`Get-ChildItem -Recurse`
- 文本搜索（优先）：`rg 关键词`
- Git 状态：`git status`
- 查看差异：`git diff`

## Android 项目初始化后建议使用的命令
- 构建 Debug：`./gradlew.bat assembleDebug`
- 安装 Debug：`./gradlew.bat installDebug`
- 运行单元测试：`./gradlew.bat testDebugUnitTest`
- 运行 lint：`./gradlew.bat lintDebug`
- 执行完整检查：`./gradlew.bat check`
- 生成 APK：`./gradlew.bat assembleRelease`（正式发布前仍需签名配置）

## 推荐开发顺序
1. 先创建 Android 工程骨架与 Gradle 配置。
2. 再接入 Compose、Navigation、Hilt、Room。
3. 然后实现数据层、安全层、设计系统和核心页面。

## 当前限制
- 由于仓库中尚无 `gradlew.bat`、`settings.gradle.kts`、`build.gradle.kts` 等文件，现阶段无法直接运行 Android 构建、测试、lint 命令。
