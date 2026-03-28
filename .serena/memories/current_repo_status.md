# 当前仓库现状（2026-03-27）

- Pocket Vault 仓库现已是 Android 应用工程，不再只是文档仓库。
- 根目录存在 `settings.gradle.kts`、`build.gradle.kts`、`app/build.gradle.kts`、`gradle/libs.versions.toml`。
- 主要技术栈：Kotlin、Jetpack Compose、Navigation Compose、Room、Hilt、Coil、Biometric。
- 当前环境限制：仓库内没有 `gradlew` / `gradlew.bat` / `gradle-wrapper.jar`；执行环境也没有 `java`、`gradle`、Android SDK，因此无法在此环境实际编译或运行。
- 2026-03-27 已完成的关键修复：
  - `ui/screen/security/UnlockViewModel.kt`：进入解锁界面时重置状态，避免旧的 `isUnlocked=true` 状态复用导致锁屏绕过。
  - `ui/screen/security/UnlockScreen.kt`：`UnlockGate` 初次进入时触发 challenge reset。
  - `ui/screen/detail/ItemDetailScreen.kt`：为密码详情增加 PIN 二次验证弹窗；当 `requireAuthForSecrets` 且已设置 PIN 时，每次查看密码前都要先验证；隐藏密码时会清空已解密内容。
- 由于缺少构建工具链，所有修改目前只做了静态核对，未完成编译验证。
