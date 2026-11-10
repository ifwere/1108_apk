# AI 选用助手

一个从零创建的原生 Android 应用，用当前北京时间判断应该使用 **DeepSeek** 还是 **WorkBuddy**。

- 最低支持 Android 10（API 29），目标/编译 SDK 34。
- 面向 ARMv8（arm64-v8a）设备；应用本身是纯 Java 代码，不依赖原生库，因此在 arm64 设备上直接运行。
- 默认离线运行；只有在设置页手动点击“刷新网络数据”时才会联网，启动和推荐过程不发起网络请求。

## 功能

打开应用后：

1. **屏幕上半部分只展示推荐结果**（不做任何交互）：
   - 高峰时段 → **WorkBuddy**
   - 空闲时段 → **DeepSeek**
   - 推荐卡片整块背景随结果变为蓝色或绿色，产品名称独占一行，便于一眼识别。
   - 卡片标题行右侧显示日期、时间和星期/节假日；副标题显示「下一次 DeepSeek 低价时间」或「当前低价剩余」，保留天、小时、分钟。
2. **屏幕下半部分**展示倒计时圆环动画和剩余秒数；**点击下半屏任意位置**（不限于按钮）都会停止计时并跳转到设置。
3. 默认倒计时 **5 秒**，倒计时结束后自动退出应用（从最近任务中移除）。
4. **设置页**支持：
   - 规则的新增、查看、修改、删除；
   - 规则启用/停用、上移/下移调整优先级；
   - 自定义倒计时时长（1–30 秒）；
   - 添加/删除自定义节假日（公司假期、调休或未来年份），无需重新安装；
   - 手动从网络刷新节假日数据，失败时自动沿用本地数据；
   - 查看从本月到最后一个有数据年份年底的日历；
   - 一键恢复默认规则。

### 主界面配色

推荐卡片不再使用左侧圆形徽标，产品名称独占整行，并通过 `autoSizeTextType` 在 20sp-34sp 之间自动缩放，确保任何屏幕宽度和系统字体大小下都能完整显示。

| 推荐 | 卡片背景 | 卡片文字 | 品牌色来源 |
| --- | --- | --- | --- |
| DeepSeek | 蓝色渐变 `#4D6BFE` -> `#3D52D8` | 白色 | DeepSeek 官方 logo 主色 `#4D6BFE` |
| WorkBuddy | 绿色渐变 `#0EC8A9` -> `#01C886` | 深色 `#06251C` | WorkBuddy 官网 `logo.svg` 渐变 |

主界面推荐卡片与设置页顶部预览卡片使用同一套品牌渐变和文字颜色，版式也保持一致（左侧标题、右侧时间、大号产品名、低价倒计时）。倒计时圆环和规则列表中的产品徽标同时使用对应品牌色，避免主界面与设置页配色不一致。

### 默认判定规则

| 优先级 | 规则 | 星期 | 时段（北京时间） | 推荐 |
| --- | --- | --- | --- | --- |
| 1 | 工作日高峰 · 上午 | 周一–周五 | 09:00–12:00（不含 12:00） | WorkBuddy |
| 2 | 工作日高峰 · 下午 | 周一–周五 | 14:00–18:00（不含 18:00） | WorkBuddy |
| 3 | 其余时间 | 每天 | 全天 | DeepSeek |

- 规则按列表顺序匹配，**第一条命中的规则生效**。
- 默认高峰规则开启「跳过中国法定节假日」；法定节假日全天按空闲处理。
- 判定口径：北京时间周一至周五、且不是法定节假日的 09:00-12:00 与 14:00-18:00 为高峰；其余时间全部空闲。
- 周末全天按空闲处理；调休上班的周末仍然按周末处理，只在日历和主界面上做标记，不触发高峰规则。
- 12:00–14:00 午间以及 18:00 之后均属于空闲时段，使用 DeepSeek。
- 空闲时段 DeepSeek 价格为高峰时段的一半，因此策略是：高峰用 WorkBuddy，把 DeepSeek 留给半价闲时。
- 若没有任何规则命中（例如规则全部被停用），兜底使用 DeepSeek。

规则编辑支持跨天时段（如 22:00–06:00），此时按当前时间的日期判断星期；开始时间包含、结束时间不包含。

### 节假日数据

数据来源与优先级（从高到低）：

1. 用户自定义节假日：覆盖同一天的休息状态与名称。
2. 网络缓存：设置页手动刷新 `https://cdn.jsdelivr.net/npm/chinese-days/dist/chinese-days.json` 后保存；对网络覆盖到的年份整体替换内置数据。
3. 内置数据：`app/src/main/assets/holidays.json`，作为离线兜底，当前包含 2024-2027 年放假日和调休补班日。
4. 星期判断：周六、周日为空闲。

网络数据包含三类字段：

| 字段 | 含义 | 应用行为 |
| --- | --- | --- |
| `holidays` | 放假日 | 按休息日处理 |
| `workdays` | 调休补班日 | 仅日历标注“班”；即使落在周末仍按空闲处理 |
| `inLieuDays` | 调休来源日 | 忽略（已包含在放假日中） |

- 刷新是手动操作，失败时保留上次缓存或内置数据，不覆盖旧数据。
- 数据不做自动删除；日历只展示本月及以后的月份，不展示历史月份。
- 不写文件日志，失败信息仅通过界面 Toast 提示。
- 数据缺失的年份不会报错，只是无法识别该年的节假日。

**自定义节假日（方案 B）**：设置页的「自定义节假日」区域可以按日期添加任意休息日，并填写名称：

- 自定义日期与内置/网络数据合并，**优先级最高**；同一天以自定义名称为准。
- 适合公司假期、临时调休，以及官方安排尚未内置的未来年份，**添加后立即生效，无需重新编译或安装 APK**。
- 数据保存在本机 SharedPreferences（`ai_choice_custom_holidays` / `days_v1`），不上传网络；删除某条后，该日期恢复为按内置/网络节假日和星期判断。
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
   - 推送到 `main` 分支（或在 `main` 上手动触发）且构建成功后，自动创建 GitHub Release：
     - tag 为 `v<versionName>-build.<run_number>`，例如 `v1.0.0-build.12`；
     - 附带 `ai-choice-assistant-<versionName>-release.apk` 与 `-debug.apk`；
     - 自动设为 Latest release。
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
- chinese-days 网络格式解析、中文名提取、补班日识别与年份替换合并；
- 日历月份范围（本月到最后有数据年份的 12 月）；
- 自定义节假日的合并/覆盖、空名称兜底；
- 自定义节假日 JSON 编解码（排序、无效数据容错）；
- 时间、星期格式化。

## 项目结构

```
app/src/main/java/com/aichoice/assistant/
├── MainActivity.java          # 上/下 1/2 主界面、品牌色卡片与倒计时
├── SettingsActivity.java      # 规则列表、倒计时、节假日信息与手动刷新
├── CalendarActivity.java      # 本月起的节假日日历
├── RuleEditActivity.java      # 规则新增/编辑
├── data/
│   ├── CustomHolidayCodec.java       # 自定义节假日 JSON 编解码
│   ├── CustomHolidayRepository.java  # 自定义节假日本地存储
│   ├── HolidayCacheRepository.java   # 网络节假日快照缓存
│   ├── HolidayDataProvider.java      # 内置/网络/自定义数据合并入口
│   ├── HolidayRemoteClient.java      # 手动刷新时的 HTTP 请求
│   ├── Rule.java              # 规则模型 + JSON
│   ├── RuleRepository.java    # SharedPreferences 持久化与默认规则
│   └── Target.java            # DeepSeek / WorkBuddy
├── logic/
│   ├── HolidayCalendar.java   # 节假日数据、补班日与日期分类
│   ├── CalendarMonths.java    # 日历月份范围计算
│   ├── RecommendationEngine.java
│   ├── Recommendation.java
│   └── DayKind.java
├── ui/
│   ├── CountdownRingView.java # 倒计时圆环动画
│   ├── MonthCalendarAdapter.java
│   ├── MonthCalendarView.java
│   └── RuleAdapter.java
└── util/TimeUtils.java
```

## 技术说明

- 包名：`com.aichoice.assistant`；应用名：AI 选用助手。
- `minSdk 29` / `targetSdk 34` / `compileSdk 34`。
- `abiFilters = ["arm64-v8a"]`，明确 ARMv8 目标；当前没有 native 代码，因此 APK 本身 ABI 无关，可直接在 arm64 设备上安装运行。
- 使用 Java 17、AndroidX、Material 3、ViewBinding。
- 时间判断统一使用 `Asia/Shanghai`（北京时间），不受设备时区影响。
- 品牌色集中在 `app/src/main/res/values/colors.xml`；推荐卡片背景为 `bg_reco_card.xml`（DeepSeek）与 `bg_reco_card_workbuddy.xml`（WorkBuddy），由 `MainActivity.bindRecommendation` 按推荐结果切换。
