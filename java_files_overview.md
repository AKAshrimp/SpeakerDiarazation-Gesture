# 檔案角色速覽（更詳細版）

這份文件按「資料夾/模組」整理：每個檔案在做什麼、主要入口方法在哪、以及你要改某個功能時該從哪個檔開始看。

## 專案結構（你要看的資料夾）

- `app/src/main/java/hk/edu/hkmu/speakerdiarazationdemo/`：主 App（Java）
  - `audio/`：註冊流程用錄音器
  - `adapters/`：設定頁的列表 adapter
  - `models/`：設定與資料模型
- `gesture/src/main/java/com/google/mediapipe/examples/gesturerecognizer/`：手勢模組（Kotlin）
  - `fragment/`：手勢原版 UI（相機預覽頁）

## 主要資料流（快速理解）

- **STT**：`AudioRecorder`（麥克風）→ `SpeechmaticsClient`（WebSocket 串流）→ `TranscriptManager`（排版/存檔/回呼）→ `MainActivity`（顯示字幕）
- **Enrollment（聲紋註冊）**：`SpeakerEnrollmentActivity` → `EnrollmentRecorder`（錄音+WAV）→ `SpeechmaticsEnrollmentClient`（取得 speaker identifiers）→ `EnrolledSpeakerStore`
- **Gesture（可選）**：`MainActivity` → `GestureBackgroundRunner`（CameraX 背景辨識）→ UI 浮層 + `TextToSpeech` 播報

## 主 App（Java）：`hk.edu.hkmu.speakerdiarazationdemo/`

### `MainActivity.java`

- **角色**：整個 Demo 的中樞（錄音、連線、字幕渲染、手勢整合、手勢 TTS）。
- **你常改的地方**
  - 錄音開始/停止：`startRecording()`、`stopRecording()`
  - UI 錄音狀態：`updateRecordingUi(...)`、`updateStatusChip()`
  - 字體大小套用：`applyFontSize()`（讀取 `PREF_FONT_SIZE_SP` 並套到字幕與錄音頁顯示）
  - 手勢：`startGestureIfEnabled()`、`stopGesture()`、`onGestureRecognized(...)`
  - 手勢 TTS：`ensureGestureTts()`、`speakGestureLabel(...)`、`shutdownGestureTts()`
  - 「TTS 會被 STT 錄進去」處理：播報時暫停/恢復收音（在 TTS 相關方法內）
- **輸入/輸出**
  - 輸入：麥克風 PCM、Speechmatics 回傳訊息、（可選）相機影像
  - 輸出：字幕 UI、transcripts 存檔、手勢浮層、TTS 播報

### `SettingsActivity.java`

- **角色**：設定頁（字體大小、TTS 引擎、說話者管理、轉寫檔管理）。
- **你常改的地方**
  - 字體大小：`seekFontSize` + `persistFontSize(...)`（會保存到 `PREF_FONT_SIZE_SP`）
  - TTS：引擎列舉/切換、`TTS 測試`、`TTS 語音資料/設定`
  - transcripts 管理：瀏覽、重新命名、刪除、刪除全部
  - 說話者清單：播放樣本、重錄、刪除、靜音（mute）
- **輸入/輸出**
  - 輸入：`SharedPreferences` 設定、`files/transcripts/`、`EnrolledSpeakerStore`
  - 輸出：保存偏好設定、啟動註冊頁、播放錄音樣本

### `SpeakerEnrollmentActivity.java`

- **角色**：聲紋註冊 UI（長按錄音、預覽、上傳註冊、輸入名稱）。
- **流程重點**
  - 收音：`EnrollmentRecorder`（PCM16 + 產出 WAV 供預覽）
  - 上傳註冊：`SpeechmaticsEnrollmentClient.enroll(...)`
  - 儲存：`EnrolledSpeakerStore.addOrReplace(...)`

### `SpeechmaticsClient.java`

- **角色**：即時轉寫 WebSocket 客戶端。
- **重點**
  - REST 取 JWT：`generateTempToken(...)`
  - WebSocket：連線/送 `StartRecognition`/串流音訊/解析回傳
  - 將 partial/final 結果丟給 `TranscriptManager`
- **你要改的地方通常是**：Speechmatics payload（language、diarization、delay、speaker identifiers 等）

### `SpeechmaticsEnrollmentClient.java`

- **角色**：註冊流程專用 WebSocket 客戶端。
- **重點**
  - 上傳一段音訊後：`EndOfStream` → `GetSpeakers(final=true)` → 回傳 identifiers 給註冊頁

### `AudioRecorder.java`

- **角色**：主流程麥克風錄音器（AudioRecord）。
- **重點**
  - 16kHz、單聲道；優先 `pcm_f32le`，必要時 fallback
  - 用 callback 把音訊 chunk 丟給 `SpeechmaticsClient`

### `TranscriptManager.java`

- **角色**：把 Speechmatics 的 partial/final token 整理成可讀字幕；處理 speaker change、合併段落、存檔。
- **你常改的地方**
  - 字幕排版規則（換行/說話者標籤/顏色）
  - partial 顯示策略（目前是獨立一行 `（正在說話：...）`）

### `ConfigManager.java`

- **角色**：讀取 `app/src/main/res/raw/config.json` → `AppConfig`。
- **注意**
  - `config.json` **不提交**；由 `config_example.json` 複製建立。

### `EnrolledSpeakerStore.java`

- **角色**：用 `SharedPreferences` + Gson 儲存/讀取 `EnrolledSpeaker` 列表。
- **你常改的地方**
  - speaker 靜音（mute）欄位與顯示邏輯

## `audio/`（Java）

### `audio/EnrollmentRecorder.java`

- **角色**：註冊流程專用錄音器（PCM16 收音）+ WAV 匯出（供預覽/存檔）。

## `adapters/`（Java）

### `adapters/TranscriptListAdapter.java`

- **角色**：設定頁 transcripts 列表（顯示檔名、大小、儲存時間；提供檢視/重新命名/刪除 callback）。

## `models/`（Java）

### `models/AppConfig.java`

- **角色**：對應 `config.json` 的設定模型（language、region、delay、speaker_sensitivity…）。
- **注意**
  - region 預設值與程式內 fallback 可能不同（如需統一，優先以此 class 為準）

### `models/EnrolledSpeaker.java`

- **角色**：說話者資訊（名稱/語言/identifiers/音檔路徑/建立與更新時間/是否靜音）。

### `models/TranscriptResult.java`

- **角色**：`TranscriptManager` 回呼給 UI 的資料結構（說話者、文字、時間戳、partial/final 等）。

## Gesture 模組（Kotlin）：`gesture/`

### `GestureBackgroundRunner.kt`（最重要：背景手勢）

- **角色**：CameraX `ImageAnalysis`（不顯示預覽）+ MediaPipe 手勢辨識。
- **輸出策略（抑制認背景/誤判）**
  - 選擇 score 最高的手勢
  - `minGestureScore`、`stableFramesRequired`、`resetAfterNoGestureMs`（背景穩定化參數）
- **主 App 使用方式**：由 `MainActivity.startGestureIfEnabled()` 建立並 `start()`，停止錄音/取消勾選時 `stop()`。

### `GestureRecognizerHelper.kt`

- **角色**：MediaPipe Tasks Gesture Recognizer 的封裝（載入 `gesture_recognizer.task`、把 `ImageProxy` 轉成 MPImage、回呼結果）。
- **注意**
  - LiveStream 路徑目前包含「旋轉 + 水平翻轉」矩陣；若你改相機方向/鏡頭，需一起檢查這段是否符合預期。

### `MainActivity.kt`（手勢原版 UI）

- **角色**：只測手勢用的原版相機 UI（會顯示相機預覽與 bottom sheet 參數調整）。

### `fragment/CameraFragment.kt`

- **角色**：手勢原版 UI 的核心（CameraX Preview + ImageAnalysis + 底部參數面板）。
- **用途**
  - 用來「校準」門檻（minHandDetection/tracking/presence）與 delegate（CPU/GPU）
  - 也是你比對「為什麼背景版比較不準」的基準行為

## 我想改 X，要看哪裡？（快速索引）

- 改錄音 UI / 狀態條 / 字體：`MainActivity.java`、`activity_main.xml`
- 讓字體大小在設定後立刻生效：`SettingsActivity.java`（`persistFontSize`）+ `MainActivity.onResume()`（`applyFontSize`）
- 改 partial 行為（不想混進 s1/s2）：`MainActivity.java`（partial 顯示處）
- 改手勢更穩/更敏感：`GestureBackgroundRunner.kt`（門檻/穩定幀數）
- 改 Speechmatics 參數（language/region/delay）：`config.json` + `AppConfig.java` + `SpeechmaticsClient.java`
