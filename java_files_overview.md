# 檔案角色速覽（Java + Gesture 模組）

| 檔案 | 主要職責 |
| --- | --- |
| `app/src/main/java/hk/edu/hkmu/speakerdiarazationdemo/MainActivity.java` | App 的錄音與即時轉寫主流程。處理麥克風/相機權限、語言下拉選單、錄音/暫停/停止按鈕，以及把 `TranscriptManager` 發出的最終與 Partial 轉寫內容渲染到畫面上（含已註冊說話者配色）。同時整合背景手勢辨識（`GestureBackgroundRunner`）與手勢 TTS 播報。 |
| `app/src/main/java/hk/edu/hkmu/speakerdiarazationdemo/SettingsActivity.java` | 設定頁。提供字體大小（24–48）調整與即時預覽、TTS 引擎選擇/測試/安裝入口、已註冊說話者清單（播放／重錄／刪除／靜音）、以及歷史轉寫檔案（`files/transcripts/*.txt`）的瀏覽/重新命名/刪除。 |
| `app/src/main/java/hk/edu/hkmu/speakerdiarazationdemo/SpeakerEnrollmentActivity.java` | 說話者註冊流程 UI。負責長按錄音、錄音預覽、送音訊給 Speechmatics 取得 speaker identifier，並要求使用者輸入名稱後存檔。 |
| `app/src/main/java/hk/edu/hkmu/speakerdiarazationdemo/SpeechmaticsClient.java` | 即時轉寫 WebSocket 客戶端。連線 Speechmatics、送出 `StartRecognition` 設定、串流麥克風音訊、解析回傳訊息並委派給 `TranscriptManager`。也會附加已註冊說話者的 identifier 清單。 |
| `app/src/main/java/hk/edu/hkmu/speakerdiarazationdemo/SpeechmaticsEnrollmentClient.java` | 註冊流程專用 WebSocket 客戶端。上傳單段錄音後送 `EndOfStream`，再發 `GetSpeakers(final=true)` 取得識別碼並回傳給 `SpeakerEnrollmentActivity`。 |
| `app/src/main/java/hk/edu/hkmu/speakerdiarazationdemo/AudioRecorder.java` | 主流程使用的麥克風錄音器。優先使用 `pcm_f32le`，不支援時退回 16-bit PCM，並將資料轉成浮點格式回呼。 |
| `app/src/main/java/hk/edu/hkmu/speakerdiarazationdemo/audio/EnrollmentRecorder.java` | 說話者註冊專用錄音器，擷取 PCM16，提供 PCM16 與 Float32LE 陣列，並能輸出 WAV 檔給預覽或存檔使用。 |
| `app/src/main/java/hk/edu/hkmu/speakerdiarazationdemo/TranscriptManager.java` | 整理 Speechmatics 回傳的 token、處理 partial/final、同說話者合併句子、處理自我修正，最後透過 callback 更新 UI；也負責把完整紀錄寫入文字檔。 |
| `app/src/main/java/hk/edu/hkmu/speakerdiarazationdemo/EnrolledSpeakerStore.java` | 使用 `SharedPreferences` 保存/讀取/刪除 `EnrolledSpeaker` 清單，確保註冊資料在各頁面間共享。 |
| `app/src/main/java/hk/edu/hkmu/speakerdiarazationdemo/ConfigManager.java` | 單例，從 `app/src/main/res/raw/config.json` 載入 `AppConfig`（此檔不提交，請由 `config_example.json` 複製建立）。 |
| `app/src/main/java/hk/edu/hkmu/speakerdiarazationdemo/models/AppConfig.java` | 對應設定檔的資料模型，提供語言、延遲、說話者靈敏度等設定的 getters（含預設與範圍檢查）。 |
| `app/src/main/java/hk/edu/hkmu/speakerdiarazationdemo/models/EnrolledSpeaker.java` | 封裝註冊說話者資訊：名稱、語言、識別碼陣列、錄音檔路徑、建立/更新時間。 |
| `app/src/main/java/hk/edu/hkmu/speakerdiarazationdemo/models/TranscriptResult.java` | UI 與存檔使用的單筆轉寫資料結構（說話者／內容／時間戳／是否 partial）。 |

## Gesture 模組（Kotlin）

| 檔案 | 主要職責 |
| --- | --- |
| `gesture/src/main/java/com/google/mediapipe/examples/gesturerecognizer/GestureBackgroundRunner.kt` | 背景手勢辨識（CameraX `ImageAnalysis`，無預覽）。回呼最終手勢 label 給主 App；內含分數門檻與連續幀數抑制誤判。 |
| `gesture/src/main/java/com/google/mediapipe/examples/gesturerecognizer/GestureRecognizerHelper.kt` | MediaPipe Tasks Gesture Recognizer 的封裝：載入 `gesture_recognizer.task`、處理 LiveStream 影像與回呼。 |
| `gesture/src/main/java/com/google/mediapipe/examples/gesturerecognizer/MainActivity.kt` | 手勢原版 UI（用於「僅測試手勢」），會顯示相機畫面與底部控制面板。 |

### 目前檢查結果
- 每個 Java 檔案皆有對應呼叫點，未發現無人使用的類別或明顯死碼。
- `SpeechmaticsEnrollmentClient` 目前保留 `sessionId` 只做記錄用途，若後續需求單純可再視情況移除。其餘欄位與方法均在現行流程中使用。

如需針對特定檔案做更詳細的流程圖或時序圖，可再告知。現階段程式分層正常，無額外冗餘檔案。
