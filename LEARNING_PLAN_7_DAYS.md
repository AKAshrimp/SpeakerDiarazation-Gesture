# 学习路线（7 天）：看得懂并能改「声之形眼镜」代码

目标：你不需要先把“所有 Android/Java”学完，也能逐步看懂这个项目、知道该改哪里、改了不容易炸。

---

## 你需要学什么？（按重要程度）

### 1) Java 语法（必须）
- class / 方法 / 构造器 / `static`
- `if/for/switch`、`try/catch`
- `interface`、`implements`、`extends`、`@Override`
- 泛型：`List<T>`、`Map<K,V>`
- lambda：`v -> { ... }`（Android 里非常常见）

### 2) OOP（必须）
- 对象的“状态”与“职责”：谁保存数据、谁负责 UI、谁负责网络
- 组合 vs 继承（这个项目主要用组合）
- **回调（Callback/Listener）**：A 调用 B，B 用接口把结果回传给 A（项目核心套路）

### 3) Android 生命周期（必须）
- `Activity`：`onCreate()` / `onResume()` / `onPause()` / `onDestroy()`
- 为什么“设置保存/恢复”要放在 `onResume()`，为什么资源要在 `onStop/onDestroy` 释放

### 4) 线程 + UI 更新（最关键）
- 主线程（UI thread） vs 后台线程
- `Handler/Looper`、`runOnUiThread`
- 网络/录音/相机都不能卡主线程

### 5) Android 常用基础（需要）
- 权限：Manifest + 运行时权限（麦克风、相机）
- 资源：`res/layout`、`findViewById`、`R.id.xxx`
- `SharedPreferences`：存“字體大小”“语言”“TTS 引擎”等

### 6) 了解项目相关技术（按需）
- WebSocket（OkHttp）：消息回调怎么驱动字幕更新
- 音频：`AudioRecord`、采样率、PCM
- Kotlin 读懂即可（因为 gesture module 是 Kotlin）：`val/var`、函数、lambda、可空 `?`

---

## 最短上手顺序（你说的“先能改 UI/功能”）

1. Java 语法 + OOP（1–2 天够你读懂结构）
2. Android Activity 生命周期 + 线程/UI 更新（最关键）
3. 再看本项目三条主线：
   - `MainActivity`（UI/流程）
   - `SpeechmaticsClient`（网络/WebSocket）
   - `TranscriptManager`（拼字幕/排版/存档）

---

## 7 天游学路线（每天 30–90 分钟，边学边改项目）

### Day 1：Java 语法速通（能看懂文件）
**学什么**
- class、字段、方法、构造器、`static/final`
- `List/Map`、`try/catch`
- lambda（`v -> ...`）

**在项目里练什么（直接打开看）**
- `app/src/main/java/hk/edu/hkmu/speakerdiarazationdemo/models/TranscriptResult.java`
- `app/src/main/java/hk/edu/hkmu/speakerdiarazationdemo/models/AppConfig.java`

**你要看懂的点**
- 这些文件基本就是“数据结构 + getter/setter”，是最容易上手的。

---

### Day 2：OOP + 回调（项目最常用套路）
**学什么**
- interface 的作用：定义“回调接口”
- A 调 B：B 通过 callback 把结果回给 A

**在项目里练什么**
- `app/src/main/java/hk/edu/hkmu/speakerdiarazationdemo/SpeechmaticsClient.java`
  - 找 `interface SpeechmaticsCallback`
- `app/src/main/java/hk/edu/hkmu/speakerdiarazationdemo/TranscriptManager.java`
  - 找 `TranscriptCallback`

**你要看懂的点**
- “网络收到消息”不会直接改 UI，而是回调给上层（通常是 MainActivity）。

---

### Day 3：Android 生命周期（为什么设置“看起来没保存”）
**学什么**
- `onCreate` 初始化控件
- `onResume` 适合“每次回到页面都刷新”的事情（例如重新套用字体大小）
- `onPause/onDestroy` 适合保存/释放资源

**在项目里练什么**
- `app/src/main/java/hk/edu/hkmu/speakerdiarazationdemo/MainActivity.java`
  - 看 `onCreate()`、`onResume()`
- `app/src/main/java/hk/edu/hkmu/speakerdiarazationdemo/SettingsActivity.java`
  - 看保存字体、`SharedPreferences`

**小练习**
- 把某个 Toast 文案改一下，确认你知道在哪里改、为什么会显示。

---

### Day 4：线程 + UI 更新（为什么要 runOnUiThread）
**学什么**
- UI 必须在主线程更新
- 网络/录音/相机在后台线程跑
- `Handler` 用来延迟执行/切回主线程

**在项目里练什么**
- `MainActivity.java`：搜 `runOnUiThread` / `Handler`
- `AudioRecorder.java`：看录音线程怎么把数据丢出去

**你要看懂的点**
- “录音启动”→“后台不断吐 PCM”→“WebSocket 发送”是一条异步流水线。

---

### Day 5：Speechmatics WebSocket（字幕从哪来）
**学什么**
- WebSocket 的事件：`onOpen/onMessage/onFailure/onClosed`
- JSON 消息解析（`org.json` / `Gson`）

**在项目里练什么**
- `SpeechmaticsClient.java`
  - 看如何生成 JWT（token）
  - 看 `onMessage` 如何把 partial/final 交给 `TranscriptManager`

**小练习**
- 把某个错误提示（例如网络失败）文案改成你想要的繁体/更友好提示。

---

### Day 6：字幕排版（你最常改的业务逻辑）
**学什么**
- 字符串拼接（`StringBuilder/SpannableStringBuilder`）
- 说话者切换、partial 显示策略

**在项目里练什么**
- `TranscriptManager.java`：看 token 缓冲与 flush 逻辑
- `MainActivity.java`：看最终怎么显示到 `tvTranscript`

**小练习**
- 你想要“正在说话”单独一行/灰色/斜体，这类需求大多在这两处改。

---

### Day 7：手势模块（只需要会读 Kotlin）
**学什么（Kotlin 只要能读）**
- `val/var`、函数、lambda、可空类型 `?`
- 看懂回调接口和状态字段

**在项目里练什么**
- 背景手势：`gesture/src/main/java/com/google/mediapipe/examples/gesturerecognizer/GestureBackgroundRunner.kt`
  - 看 `minGestureScore` / `stableFramesRequired`（抑制误判）
- 手势模型封装：`gesture/src/main/java/com/google/mediapipe/examples/gesturerecognizer/GestureRecognizerHelper.kt`
  - 看怎么把 `ImageProxy` 转成 MPImage、怎么回调结果

**小练习**
- 想更灵敏：降低 `minGestureScore` 或把 `stableFramesRequired` 改小。
- 想更稳：提高 `minGestureScore` 或把 `stableFramesRequired` 改大。

---

## 读代码的“抓手”：你应该从哪三条线看

### 1) UI/流程线：`MainActivity.java`
你做任何“功能改动”，通常从这里进。
- 录音：`startRecording()` / `stopRecording()`
- 字体：`applyFontSize()`（读 `PREF_FONT_SIZE_SP`）
- 手势：`startGestureIfEnabled()` / `stopGesture()` / `onGestureRecognized(...)`

### 2) 网络线：`SpeechmaticsClient.java`
你要改“识别参数、语言、延迟、说话者配置”，看这里。
- token：`generateTempToken(...)`
- websocket：`onOpen/onMessage/onFailure`

### 3) 字幕线：`TranscriptManager.java`
你要改“字幕怎么拼、怎么换行、partial 怎么显示”，看这里。
- token 缓冲、speaker 切换、flush 输出

---

## 你现在最常遇到的“看不懂”其实是哪几类问题？

- “为什么这个函数没被直接调用？”：因为用了 **回调**（Callback/Listener），调用链在 WebSocket/录音线程里。
- “为什么 UI 改了没更新？”：可能没在主线程更新，或应该放在 `onResume()` 重新套用。
- “为什么设置保存了但回到主页没生效？”：通常是主页没在 `onResume()`/某个刷新点重新读 `SharedPreferences`。

---

## 下一步（你选一个目标，我可以用你的代码带你读）

- 目标 A：**快速能改功能**（比如 UI/字幕/手势阈值/提示文案）
- 目标 B：**系统学扎实**（Android + Java + 网络 + 音频）

你选 A 还是 B？我就按你的目标给你“下一次要打开哪些文件、每个文件看哪几个函数”的逐步导读。 
