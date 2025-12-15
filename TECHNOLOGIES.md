# 技術概覽（聲之形眼鏡）

本專案是面向聽障/聽損情境的 Android Demo：以 Speechmatics Realtime API 進行即時語音轉寫（STT）+ 語者分離（Speaker Diarization），並可選擇在背景同步啟用 MediaPipe 手勢辨識與手勢播報（TTS）。以下整理主要技術與元件，方便快速了解系統組成。

````mermaid
flowchart TD
    subgraph App（Android）
        MIC[Mic 錄音\nAudioRecorder] -->|PCM| WS[SpeechmaticsClient\nWebSocket]
        WS -->|結果| TM[TranscriptManager\n排版/存檔/回呼]
        TM --> UI[MainActivity\n字幕 UI（顏色分說話者）]

        CAM[相機\nCameraX ImageAnalysis] -->|RGBA| GR[GestureBackgroundRunner\n背景手勢辨識]
        GR --> UI
        GR --> TTS[TextToSpeech\n手勢播報]
    end

    subgraph Speechmatics
        WS --> API[(Realtime API)]
        API --> WS
    end
````

上圖包含兩條主線：

- **語音**：麥克風 → WebSocket 串流 → Speechmatics 回傳結果 → `TranscriptManager` 排版/存檔 → UI 更新
- **手勢**（可選）：CameraX `ImageAnalysis` → MediaPipe Gesture Recognizer → UI 浮層顯示手勢 →（可選）TTS 播報

````mermaid
flowchart TD
    subgraph UI層
        A0[MainActivity\n錄音流程 / 語言切換 / 字幕 UI / 手勢整合]
        A1[SettingsActivity\n字體大小 / 語言設定 / TTS 引擎 / 資料管理]
        A2[SpeakerEnrollmentActivity\n說話者註冊流程]
    end

    subgraph 音訊與資料層
        B0[AudioRecorder\n麥克風錄音 16kHz PCM]
        B1[TranscriptManager\nToken 緩衝 / 輸出 / 存檔]
        B2[EnrollmentRecorder\nPCM16 收音 + WAV 匯出]
        B3[EnrolledSpeakerStore\nSharedPreferences + Gson]
    end

    subgraph Speechmatics整合
        C0[ConfigManager\n載入 config.json（不提交）→ AppConfig]
        C1[SpeechmaticsClient\n主轉寫 WebSocket 客戶端]
        C2[SpeechmaticsEnrollmentClient\n註冊專用 WebSocket 客戶端]
        C3[Speechmatics Realtime API\nJWT Token 服務 + 即時辨識]
    end

    subgraph 手勢（MediaPipe）
        G0[GestureBackgroundRunner\nCameraX 背景辨識]
        G1[GestureRecognizerHelper\nMediaPipe Tasks]
        G2[TextToSpeech\n手勢播報]
    end

    A0 -->|啟動/停止錄音| B0
    A0 -->|顯示結果| B1
    A0 -->|讀寫設定| C0
    A0 -->|帶入註冊語者| B3
    A0 -->|建立即時連線| C1
    A0 -->|啟動註冊畫面| A2
    A0 -->|啟用/停止手勢| G0

    A1 -->|更新偏好| C0
    A1 -->|管理錄音與語者清單| B3
    A1 -->|選擇/測試 TTS| G2

    A2 -->|長按錄音| B2
    A2 -->|送音訊註冊| C2
    A2 -->|儲存識別碼| B3

    B0 -->|PCM Float| C1
    B1 -->|Flush 結果| A0

    C0 -->|AppConfig 設定| C1
    C0 -->|AppConfig 設定| C2

    C1 -->|JWT 要求 + StartRecognition| C3
    C1 -->|AddTranscript / EndOfUtterance| B1
    C2 -->|StartRecognition + 音訊 / GetSpeakers| C3
    C2 -->|SpeakersResult| A2

    G0 -->|相機影像| G1
    G1 -->|手勢 label| A0
    A0 -->|播報| G2

````

## 平台與語言

- **Android SDK**：`compileSdk 36`、`minSdk 28`、`targetSdk 28`，對應 Android 9 以上裝置。
- **程式語言**：主 App 以 Java 撰寫（Java 11 相容）；手勢模組為 Kotlin（Kotlin Android Plugin）。
- **建置工具**：使用 Gradle（Kotlin DSL）與 Android Gradle Plugin `8.13.0` 管理模組與依賴。

## UI 與互動

- **Android View 系統**：以 `AppCompatActivity` 提供向下相容支援。
- **Material Components**：應用 Material Design 元件與深色介面主題。
- **佈局**：主畫面以 `FrameLayout + LinearLayout + ScrollView` 組成，並加上頂部固定狀態列與右下角手勢浮層。
- **Spinner 與自訂項目佈局**：語言選擇器使用自訂的 `item_language_spinner*.xml` 版面。
- **SharedPreferences**：儲存字體大小與語言偏好設定。
- **SettingsActivity**：提供 UI 設定入口，讓使用者調整字體大小（24–48）、語言、TTS 引擎，以及管理說話者/轉寫檔案。

## 音訊處理

- **AudioRecord API**：`AudioRecorder` 以 `MediaRecorder.AudioSource.MIC` 擷取 16kHz、單聲道、`pcm_f32le` 格式的音訊資料。
- **多執行緒處理**：錄音過程以背景 `Thread` 將浮點 PCM 轉為位元組流，並透過回呼推送至串流客戶端。

## 轉寫與語者分離

- **Speechmatics 即時 API**：
  - REST API：先以 `https://mp.speechmatics.com/v1/api_keys` 建立時效性 JWT。
  - WebSocket：透過 `wss://wus.rt.speechmatics.com/v2/` 進行即時音訊串流與辨識結果接收。
- **OkHttp 4.10.0**：同時提供 REST 呼叫與 WebSocket 連線管理。
- **Gson 2.10.1 與 org.json**：解析 REST 回應、建立 WebSocket 訊息與處理辨識結果。
- **TranscriptManager**：
  - 根據 `Speechmatics` 回傳的語者標記進行文字緩衝與排版。
  - 透過 `Handler` 定時檢查超時並輸出完整段落。
  - 支援將完整紀錄寫入內部儲存的文字檔案。

## 手勢辨識（MediaPipe + CameraX）

- **CameraX ImageAnalysis**：背景手勢辨識使用 `ImageAnalysis`（不顯示預覽），以 `RGBA_8888` 格式餵給模型。
- **MediaPipe Tasks - Gesture Recognizer**：`gesture/src/main/assets/gesture_recognizer.task`。
- **背景穩定化**：背景版會以分數門檻與連續幀數抑制誤判（例如低信心「認背景」）。

## 語音輸出（TTS）

- **Android TextToSpeech**：用於手勢播報與設定頁測試。
- **避免 TTS 被 STT 錄進去**：播報時暫停/恢復收音，並做去重與最小播報間隔。

## 設定與資料

- **配置檔**：`app/src/main/res/raw/config.json` 以 JSON 定義 API 金鑰、語言、區域與輸出選項（**不提交**，請由 `config_example.json` 複製建立）；`ConfigManager` 以 Gson 載入。
- **資料模型**：`AppConfig`、`TranscriptResult` 等 model 類別封裝設定與紀錄資料。

## 核心類別職責

- `MainActivity.java`：主畫面控制中心，負責權限檢查、語言選擇、啟停錄音流程與 UI 更新，並整合錄音、WebSocket 與逐字稿呈現。
- `SettingsActivity.java`：提供字體大小調整介面、TTS 引擎選擇/測試，透過 `SharedPreferences` 儲存設定並即時預覽。
- `AudioRecorder.java`：封裝 `AudioRecord` 生命週期，優先使用 `pcm_f32le`，必要時退回 16-bit PCM，並在背景執行緒將音訊轉為浮點 byte stream。
- `SpeechmaticsClient.java`：管理 Speechmatics REST+WebSocket 流程，建立暫時 JWT、送出啟動訊息、解析辨識回傳與錯誤，再將資料交給 `TranscriptManager`。
- `TranscriptManager.java`：依語者分別緩衝 token，處理 speaker change/self-correction、字元間空白規則與逾時輸出，最後組裝 `TranscriptResult` 並可保存成檔。
- `ConfigManager.java`：Singleton 管理，讀取 `config.json` 並提供全域 `AppConfig` 供其他元件取用或更新語言設定。
- `models/AppConfig.java`：對應設定檔欄位，提供存取方法與語者代碼對應。
- `models/TranscriptResult.java`：封裝單筆輸出包含語者、時間戳與內容的資料物件，方便 UI 與檔案寫入共用。
- `gesture/GestureBackgroundRunner.kt`：背景手勢辨識（CameraX ImageAnalysis + MediaPipe），將結果回呼給主 App。

## 測試與除錯支援

- **JUnit 4.13.2**：單元測試依賴。
- **AndroidX Test (JUnit 1.3.0 / Espresso 3.7.0)**：提供儲存的儀器測試環境。
- **Logcat**：程式廣泛使用 `android.util.Log` 協助診斷連線、錄音與轉寫流程。

## 其他實用技術

- **Handler / Runnable**：主執行緒更新 UI 與排程觸發事件（例如錄音逾時檢查）。
- **File I/O**：以 `FileWriter` 將轉寫結果存放於應用內部儲存的 `transcripts/` 目錄。
- **色彩與字元處理**：自訂語者色彩、高亮顯示語者標籤，並針對中英文標點做格式化處理。

如需延伸開發，可依據上述技術堆疊擴充額外功能（例如改用 Kotlin、導入 Room 或 Compose 等）。
