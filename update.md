# 2025-11-09 Updates

- **Speaker Mute Control**
  - 每位已註冊說話者卡片加入「隱藏此說話者」勾選框（預設未勾選）。切換時即時存回 `EnrolledSpeakerStore`，並保留於重新錄製後。
  - `MainActivity` 會同步讀取被隱藏的說話者，蒐集其自訂名稱與 `speaker_identifiers`，以供轉寫流程辨識。
- **Transcript 過濾**
  - `TranscriptManager` 新增靜音名單，`AddTranscript` 與 partial 訊息若來自靜音名單會被直接忽略，不再輸出或寫入檔案。
  - `SettingsActivity` 的提示讓使用者知道目前勾選狀態，避免誤解為刪除說話者。
- **Updated Server**
  - 專案預設 region 改為 `usa`，WebSocket 與 Enrollment 皆指向 `wss://wus.rt.speechmatics.com/v2/`，確保 STT 及註冊流程都對應美西叢集。

# 2025-10-29 Updates

- **Configuration**
  - Added Speechmatics parameters (`timeout_seconds`, `operating_point`, `max_delay_mode`, `enable_partials`, `diarization`, `max_speakers`, `speaker_sensitivity`) to `app/src/main/res/raw/config.json`; default `max_speakers` is now 10.
  - Extended `AppConfig` getters with default values and range clamping.
- **Realtime StartRecognition**
  - `SpeechmaticsClient` reads low-latency and diarization settings from config and attaches enrolled speaker identifiers when available.
  - Added `conversation_config.end_of_utterance_silence_trigger` wiring so Speechmatics can推送 `EndOfUtterance` 提早結束一段話；同時把預設 `timeout_seconds` 調低為 1 秒以縮短回傳延遲。
  - 啟用 `audio_filtering_config`，可以依 `audio_filter_volume_threshold` 過濾低音量背景聲，減少粵語環境下的干擾語音。
- **TranscriptManager**
  - Removed local timeout flushing; finals rely on API `is_final` / `is_eos` flags.
  - Partial callbacks now carry `(speakerName, text)`; final output clears the partial before updating the UI.
  - 新增 `handleEndOfUtterance()`，收到 Speechmatics 的 EndOfUtterance 訊息時會立即產出最後一位說話者的文字，避免段落卡住。
- **MainActivity**
  - Removed `tvPartial`; partial text is appended inline (wrapped with parentheses) without flicker via cached indices.
  - Added enrolled-speaker colour mapping (registered names use blue; generic S1-S5 retain legacy colours) and pipe the roster to `SpeechmaticsClient` before connecting.
  - 在開始錄音前會重新套用 `SharedPreferences` 中的語言設定，確保轉錄語系即時跟隨 UI spinner 切換。
  - 新增「暫停/恢復錄音」按鈕：會停止本地麥克風輸入但保持 WebSocket 連線，恢復時立即重新推音訊；暫停時也會強制 flush 當前 partial 以利顯示清楚。
- **Settings & Enrollment UI**
  - Rebuilt `SettingsActivity` layout with a speaker-enrolment section and transcript manager. The transcript list now surfaces saved `.txt` files and allows viewing/deleting them.
  - Added `SpeakerEnrollmentActivity`, `EnrollmentRecorder`, and `SpeechmaticsEnrollmentClient` to handle hold-to-record, preview, `GetSpeakers(final=true)` enrolment, and persistence to `EnrolledSpeakerStore`. Fixed the hold-to-record UX so the button releases correctly and added an `isSubmitting` guard during upload。
  - Enrollment flow 現在會等待 `RecognitionStarted` 後串流錄音、追蹤 `last_seq_no`，並發 `EndOfStream`（含 `last_seq_no`）再送出 `GetSpeakers(final=true)`，符合 Speechmatics RT schema，避免 invalid message。
  - Settings page allows playback/re-record/delete of enrolled speakers, plus transcript viewing and management.
  - Transcript manager list 改為 `RecyclerView` 分頁載入（每批 20 筆），並新增「載入更多」與「刪除全部」操作以及路徑提示，清單不會因大量檔案而卡頓（改用 `NestedScrollView` 包裝讓整頁可同時滾動）。
- **Storage**
  - Introduced `EnrolledSpeaker` model and `EnrolledSpeakerStore` (SharedPreferences + Gson) and store WAVs under `files/enrollments/`; transcripts remain under `files/transcripts/` and are now exposed in the UI.
- **Layouts & Resources**
  - Added `activity_speaker_enrollment.xml`, `item_enrolled_speaker.xml`, and `item_audio_file.xml`; removed the obsolete partial TextView from `activity_main.xml`.
- **Misc**
  - Registered `SpeakerEnrollmentActivity` in the manifest.
  - Cross-checked Speechmatics real-time enrollment/identification samples; confirmed our flow mirrors the official `EndOfStream` + `GetSpeakers(final=true)` pattern and the main client now injects stored `speaker_identifiers` just like the SDK example.
