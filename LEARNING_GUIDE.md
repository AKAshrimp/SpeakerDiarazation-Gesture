# 聲之形眼鏡 Reading Guide

這份筆記幫你循序熟悉專案，從設定檔一路跟到 Speechmatics WebSocket，了解每一段程式在做什麼。建議配合 Android Studio，同步點開對應檔案邊讀邊跑。  

## 0. 必備背景
- **工具**：Android Studio Jellyfish 以上、Gradle、ADB；熟悉 `./gradlew assembleDebug` 即可建置。
- **語言與框架**：Java 11、Android View 系統、OkHttp、Gson。
- **Speechmatics**：知道 API Key 位置、`https://mp.speechmatics.com/v1/api_keys` 取得 JWT、Realtime WebSocket `wss://wus.rt.speechmatics.com/v2/` 的 Start/Stop 流程。

## 1. 從設定開始
1. 看 `app/src/main/res/raw/config_example.json`：掌握 API key、語言、region、延遲、說話者設定等參數。
   - 實際運行時需要建立 `app/src/main/res/raw/config.json`（此檔 **不提交**，避免 API key 外洩）。
2. 閱讀 `app/src/main/java/.../models/AppConfig.java`：了解每個欄位如何套預設值、上下限，後續所有類別都透過它取得設定。
3. 查 `ConfigManager`（`app/src/main/java/.../ConfigManager.java`）：Singleton，啟動時讀取 `config.json` 並交給活動或客戶端使用。

> 先抓住「AppConfig 提供設定 → 其他類別只呼叫 getter」，後續看到 `config.getXxx()` 就能快速對應。

## 2. 主流程（MainActivity → 音訊 → 轉寫）
1. **`MainActivity.java`**  
   - 檢查錄音權限、語言選單、啟動/停止錄音按鈕。  
   - 勾選「啟用手勢」時，於錄音期間啟動背景手勢辨識，並在右下角顯示手勢（可選擇 TTS 播報）。
   - 建立 `AudioRecorder`、`TranscriptManager`、`SpeechmaticsClient`。  
   - 監聽 `SpeechmaticsClient.SpeechmaticsCallback` 更新 UI。
2. **`AudioRecorder.java`**  
   - 以 `AudioRecord` 擷取 16 kHz PCM Float32，透過 callback 把 byte[] 推給 `SpeechmaticsClient.sendAudio()`。
3. **`TranscriptManager.java`**  
   - 處理 Speechmatics 回傳的 partial/final token、合併同說話者句子、呼叫 `MainActivity` 更新字幕與儲存檔案。

閱讀順序建議：MainActivity → AudioRecorder → TranscriptManager，因為主流程會顯示這三者如何串在一起。

## 3. Speechmatics 串接（核心重點）
1. **`SpeechmaticsClient.java`**  
   - `generateTempToken()`：POST 到 `https://mp.speechmatics.com/v1/api_keys?type=rt`，body 帶 `region`（以 `config.json` 為準；未設定時程式會用預設值）。  
   - `connect()`：將 JWT 加在 `wss://wus.rt.speechmatics.com/v2/?jwt=...`。  
   - `onOpen()`：送 `StartRecognition` 設定，包含語言、延遲、speaker_diarization_config。  
   - `onMessage()`：拆 `AddPartialTranscript`、`AddTranscript`、`EndOfUtterance` 等訊息交給 `TranscriptManager`。  
   - `sendAudio()` / `disconnect()`：串流 PCM 以及送 `StopRecognition`。
2. **`SpeechmaticsEnrollmentClient.java`**  
   - 讀取 Enrolment 錄音、送 `StartRecognition` → `EndOfStream` → `GetSpeakers(final=true)`，得到識別碼。  
   - 與主客戶端共用 `AppConfig` 取得 region 與其他參數。

理解這兩個類別的狀態機後，就能掌握 app 與 Speechmatics 的完整互動。

## 4. UI 與資料管理
- **`SettingsActivity` + `activity_settings.xml`**：調整語言、字體大小（24–48）、TTS 引擎，查看轉寫檔案與已註冊說話者。
- **`SpeakerEnrollmentActivity`**：按住錄音 → 預覽 → 呼叫 `SpeechmaticsEnrollmentClient` → 透過 `EnrolledSpeakerStore` 存檔。
- **`TranscriptListAdapter`、`TranscriptResult`**：顯示歷史轉錄，對應 `files/transcripts/` 目錄。

這一層比較偏 UI/UX，可在理解主流程後再閱讀。

## 5. 推薦閱讀順序摘要
1. `README.md`、`TECHNOLOGIES.md`、`update.md`：掌握專案定位與最近改動。  
2. 設定：`config.json` → `AppConfig` → `ConfigManager`。  
3. 主流程：`MainActivity` → `AudioRecorder` → `TranscriptManager`。  
4. Speechmatics 串流：`SpeechmaticsClient`、`SpeechmaticsEnrollmentClient`。  
5. UI 延伸：`SettingsActivity`、`SpeakerEnrollmentActivity`、adapter 與 model。  

每完成一步，可嘗試在 `MainActivity` 加 `Log.d` 或 breakpoints，實際跟一下 `connect()` 到 `addToken()` 的呼叫路徑，印象會更深。

## 6. 練習題
1. **增加新語言選項**：在 `config.json` 換 `language`，觀察 `StartRecognition` payload 如何變化。  
2. **模擬連線錯誤**：拔掉網路或改成無效 API key，看看 `SpeechmaticsClient.onFailure()` 如何通知 UI。  
3. **Enrollment 流程**：錄一段音 → 註冊說話者 → 檢查 `EnrolledSpeakerStore` 是否新增資料並被主 client 送到 `speaker_diarization_config`。  
4. **手勢背景辨識**：勾選「啟用手勢」後開始錄音，觀察右下角手勢浮層是否更新；若誤判多，可調整 `GestureBackgroundRunner.kt` 的門檻/穩定幀數。

透過這些練習就能把整個專案跑過一次，之後需要新增功能時也比較不會迷路。
