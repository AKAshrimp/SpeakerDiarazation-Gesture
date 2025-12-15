# 聲之形眼鏡（PhonoForm Glasses）— 對話/專案續接備忘

這份文件是為了「下次開新對話」能快速讓助理理解：我們做了什麼、目前專案長什麼樣、接下來要改什麼。

## 專案目標（你要做的 Demo）

- 面向聽障/聽損情境：把周遭對話即時轉成「可閱讀」字幕。
- 支援語者分離（不同說話者用顏色/標籤區分）。
- 在眼鏡（INMO Air 3）上運行：同時可以選擇啟用「背景手勢辨識 + 手勢 TTS 播報」。

## 重要決策（當時討論過的方向）

- **不做 Kotlin 全量改寫成 Java**：保留 MediaPipe 手勢模組為 Kotlin library module，讓 Java 主 App 直接依賴（最快、最穩）。
- 專案名稱定案：**「聲之形眼鏡」**（可選英文：PhonoForm Glasses）。

## 目前目錄/模組結構（很重要）

請在 Android Studio 打開：`SpeakerDiarazationDemo/`（裡面有 `settings.gradle.kts`）。

- `:app`（Java）：`SpeakerDiarazationDemo/app`
- `:gesture`（Kotlin Library）：`SpeakerDiarazationDemo/gesture`

> 不需要把 gesture 包著 speaker 之類的巢狀結構；目前已是乾淨的 multi-module。

## 目前 UI 與使用流程（MainActivity）

- `開始錄音`：啟動/停止 STT 串流錄音。
- `啟用手勢（相機）` 勾選框：
  - 勾選 + 開始錄音：背景啟用手勢辨識（**不顯示相機畫面**）
  - 取消勾選 / 停止錄音：停止手勢辨識並釋放資源
- 右下角浮層：顯示最新手勢（例如 `手勢：good`），**5 秒沒更新自動淡出隱藏**。
- 頂部固定狀態列（不會滾走）：顯示 `連線中 / 錄音中 / 暫停` 與 `手勢播報中` 等狀態。
- Partial 顯示：不混在說話者對話中，單獨一行灰色斜體：`（正在說話：...）`（繁體）。

## 手勢模組整合（背景辨識）

背景手勢辨識入口：
- `SpeakerDiarazationDemo/gesture/src/main/java/com/google/mediapipe/examples/gesturerecognizer/GestureBackgroundRunner.kt`

主 App 接入點（Java）：
- `SpeakerDiarazationDemo/app/src/main/java/hk/edu/hkmu/speakerdiarazationdemo/MainActivity.java`
  - `startGestureIfEnabled()` / `stopGesture()`

### 為什麼整合後「感覺沒原版準」？

主要原因通常是：
- 整合後裝置同時跑 STT + UI +（可能還有 TTS），眼鏡上更容易掉幀/降頻，導致誤判增加。
- 背景模式沒有相機預覽，你的手容易出框/距離不對/光線不穩，原版則會因為你看得到畫面自然放對位置。

### 已做的「更接近原版」調整（降低認背景/誤判）

已更新 `GestureBackgroundRunner.kt`：
- 不再拿第一個候選手勢：改為選取 **score 最高**的手勢。
- 加入基本的「抑制誤判」：
  - `minGestureScore=0.65`
  - `stableFramesRequired=2`
  - `resetAfterNoGestureMs=700`

如果你覺得變得太保守（漏手勢），可以把 `minGestureScore` 調低（例如 0.55）或把 `stableFramesRequired` 改成 1。

## TTS（語音播報）狀態與問題處理

你遇到的問題：**TTS 的字會被 STT 錄進去**（眼鏡上特別明顯）。

目前策略：
- 播報手勢時會暫停/恢復收音，降低「TTS 自己被轉寫」的機率。
- 去重與最小播報間隔，避免連續播報造成更嚴重回音。
- 預先初始化 TTS，減少第一句卡在 queued 很久的狀況。

設定頁支援（眼鏡找不到系統 TTS 設定入口時）：
- 可選擇 TTS 引擎、打開語音資料安裝頁、與一鍵 TTS 測試。
  - `SpeakerDiarazationDemo/app/src/main/java/hk/edu/hkmu/speakerdiarazationdemo/SettingsActivity.java`

## 繁體化（UI 字串）

已把 App 內「顯示給使用者看的簡體/英文」大多改為繁體，並可成功編譯。

還可能剩下的（如果你想更徹底）：
- 手勢原版相機介面（`Test Gesture Only`）的英文 UI 字串：`SpeakerDiarazationDemo/gesture/src/main/res/values/strings.xml`
  - 建議做法：新增 `values-zh-rHK/strings.xml` 只覆蓋需要顯示的字串。

## 文件（已更新）

- 根目錄：`README.md`（已改成「聲之形眼鏡」總覽）
- `SpeakerDiarazationDemo/README.md`（主工程說明）
- `SpeakerDiarazationDemo/GESTURE_INTEGRATION.md`（手勢+STT 聯動說明，繁體）
- `VOICE_OUTPUT_ANALYSIS.md`（TTS 現況與設計備忘，已更新為「目前有 TTS」）

## 建置/驗證

- 編譯命令（Windows / PowerShell）：在 `SpeakerDiarazationDemo/` 內執行
  - `.\gradlew.bat :app:assembleDebug`

## 下次想繼續優化的方向（TODO）

1. 讓背景手勢更穩：曝光/解析度/節流、或把信心門檻/穩定幀數做成設定項（可在 Settings 加 slider）。
2. 手勢「認背景」再降低：要求必須有 hand landmarks/presence 高於門檻才 emit（目前已用 score 門檻 + 穩定幀數）。
3. 把 `:gesture` 原版 UI 的英文做繁體化（新增 `values-zh-rHK`）。
4. 如果眼鏡偶發 TTS queued 很久：加「TTS warm-up 讀一個空白/極短詞」或改 AudioFocus 行為（需依裝置實測）。

