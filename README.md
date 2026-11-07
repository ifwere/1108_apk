# AI 选用助手

一个从零创建的原生 Android 应用，用当前北京时间判断应该使用 **DeepSeek** 还是 **WorkBuddy**。

- 最低支持 Android 10（API 29），目标/编译 SDK 34。
- 面向 ARMv8（arm64-v8a）设备；应用本身是纯 Java 代码，不依赖原生库，因此在 arm64 设备上直接运行。
- 无网络权限，所有规则和节假日数据都在本地，启动速度快、隐私友好。

## 功能

打开应用后：

1. **屏幕上半部分只展示推荐结果**（不做任何交互）：
   - 高峰时段 → **WorkBuddy**
   - 空闲时段 → **DeepSeek**
   - 同时显示当前北京时间、星期/节假日和命中的规则。
2. **屏幕下半部分**展示倒计时圆环动画和剩余秒数；**点击下半屏任意位置**（不限于按钮）都会停止计时并跳转到设置。
3. 默认倒计时 **5 秒**，倒计时结束后自动退出应用（从最近任务中移除）。
4. **设置页**支持：
   - 规则的新增、查看、修改、删除；
   - 规则启用/停用、上移/下移调整优先级；
   - 自定义倒计时时长（1–30 秒）；
   - 添加/删除自定义节假日（公司假期、调休或未来年份），无需重新安装；
   - 一键恢复默认规则。

### 默认判定规则

| 优先级 | 规则 | 星期 | 时段（北京时间） | 推荐 |
| --- | --- | --- | --- | --- |
| 1 | 工作日高峰 · 上午 | 周一–周五 | 09:00–12:00（不含 12:00） | WorkBuddy |
| 2 | 工作日高峰 · 下午 | 周一–周五 | 14:00–18:00（不含 18:00） | WorkBuddy |
| 3 | 其余时间 | 每天 | 全天 | DeepSeek |

- 规则按列表顺序匹配，**第一条命中的规则生效**。
- 默认高峰规则开启「跳过中国法定节假日」；法定节假日全天按空闲处理。
- 周末全天按空闲处理（调休上班的周末也按周末处理，与需求一致）。
- 12:00–14:00 午间以及 18:00 之后均属于空闲时段，使用 DeepSeek。
- 空闲时段 DeepSeek 价格为高峰时段的一半，因此策略是：高峰用 WorkBuddy，把 DeepSeek 留给半价闲时。
- 若没有任何规则命中（例如规则全部被停用），兜底使用 DeepSeek。

规则编辑支持跨天时段（如 22:00–06:00），此时按当前时间的日期判断星期；开始时间包含、结束时间不包含。

### 节假日数据

- 内置数据位于 `app/src/main/assets/holidays.json`，当前包含 **2024–2026 年**国务院公布的放假日期，以及 **2027 年**已知的法定节日日期。
- 只列出「休息日」；调休上班的周末无需列出，因为应用本身把周末视为空闲。
- 数据缺失的年份不会报错，只是无法识别该年的节假日。

**自定义节假日（方案 B）**：设置页的「自定义节假日」区域可以按日期添加任意休息日，并填写名称：

- 自定义日期与内置数据合并，**优先于内置节假日和星期判断**；同一天以自定义名称为准。
- 适合公司假期、临时调休，以及官方安排尚未内置的未来年份，**添加后立即生效，无需重新编译或安装 APK**。
- 数据保存在本机 SharedPreferences（`ai_choice_custom_holidays` / `days_v1`），不上传网络；删除某条后，该日期恢复为按内置节假日和星期判断。
- 若后续拿到了新的官方数据，仍可更新 `holidays.json` 并重新打包；两种方式互不冲突。

## 构建与运行（GitHub Actions）

仓库已包含 `.github/workflows/android.yml`：

1. 将项目推送到 GitHub（`main` 或 `master` 分支），或在 Actions 页面手动触发 **Android CI**。
2. 工作流会：
   - 使用 JDK 17、Gradle 8.7、Android SDK 34 构建；
   - 运行全部单元测试；
   - 生成 debug 与 release APK；
   - 上传两个 APK 到 Actions Artifacts：
     - `ai-choice-assistant-debug`
     - `ai-choice-assistant-release`
3. 下载 APK 后直接安装到 ARMv8 的 Android 10+ 设备即可。release 包暂时使用 debug 签名，便于直接安装。

本地构建（可选）：

```bash
# 需要 JDK 17、Gradle 8.7、Android SDK Platform 34
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```

APK 输出在 `app/build/outputs/apk/debug/`。

## 测试

`app/src/test/` 下的 JVM 单元测试覆盖：

- 高峰/空闲时段边界（09:00、12:00、14:00、18:00）；
- 周末与法定节假日；
- “跳过节假日”开关；
- 跨天时段；
- 星期过滤、禁用规则、优先级顺序、全天规则；
- 无规则兜底；
- 输入为 UTC 时仍按北京时间判断；
- 规则 JSON 序列化/反序列化；
- 节假日 JSON 解析与日期分类；
- 自定义节假日的合并/覆盖、空名称兜底；
- 自定义节假日 JSON 编解码（排序、无效数据容错）；
- 时间、星期格式化。

## 项目结构

```
app/src/main/java/com/aichoice/assistant/
├── MainActivity.java          # 上/下 1/2 主界面与倒计时
├── SettingsActivity.java      # 规则列表、倒计时、节假日信息
├── RuleEditActivity.java      # 规则新增/编辑
├── data/
│   ├── CustomHolidayCodec.java      # 自定义节假日 JSON 编解码
│   ├── CustomHolidayRepository.java # 自定义节假日本地存储
│   ├── Rule.java              # 规则模型 + JSON
│   ├── RuleRepository.java    # SharedPreferences 持久化与默认规则
│   └── Target.java            # DeepSeek / WorkBuddy
├── logic/
│   ├── HolidayCalendar.java   # 节假日数据与日期分类
│   ├── RecommendationEngine.java
│   ├── Recommendation.java
│   └── DayKind.java
├── ui/
│   ├── CountdownRingView.java # 倒计时圆环动画
│   └── RuleAdapter.java
└── util/TimeUtils.java
```

## 技术说明

- 包名：`com.aichoice.assistant`；应用名：AI 选用助手。
- `minSdk 29` / `targetSdk 34` / `compileSdk 34`。
- `abiFilters = ["arm64-v8a"]`，明确 ARMv8 目标；当前没有 native 代码，因此 APK 本身 ABI 无关，可直接在 arm64 设备上安装运行。
- 使用 Java 17、AndroidX、Material 3、ViewBinding。
- 时间判断统一使用 `Asia/Shanghai`（北京时间），不受设备时区影响。
