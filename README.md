# 聲之形眼鏡（PhonoForm Glasses）

面向聽障/聽損情境的 Android Demo：即時語者分離（Speaker Diarization）與語音轉寫（STT），將周遭對話以字幕方式輸出並以色彩區分不同說話者；可選擇在背景同步啟用手勢辨識與手勢播報（TTS）。

## Android API 等級

專案的 Android SDK 設定如下，供確認相容性與上架要求：

- `compileSdk = 36`（對應 Android 15 / API Level 36，仍為預覽版）
- `targetSdk = 28`（對應 Android 9 Pie / API Level 28）
- `minSdk = 28`（最低支援 Android 9 Pie）
- Java 11 相容編譯選項

> 如需上架 Google Play，建議依實際需求更新 `targetSdk` 以符合最新規範。

## 建置需求

- Android Studio Jellyfish (或以上) 搭配 Android Gradle Plugin 8.1+
- 已安裝 Android API Level 28 及 36 的 SDK 元件
- JDK 11

## 快速開始

1. 複製專案後，於 Android Studio 開啟專案根目錄（本資料夾，內含 `settings.gradle.kts`）。
2. 建立 Speechmatics 設定檔（請勿提交 API key）：
   - 複製 `app/src/main/res/raw/config.example.json` → `app/src/main/res/raw/config.json`
   - 填入 Speechmatics 帳戶對應的 `api_key` 與其他參數
3. 透過工具列同步 Gradle，確認所有依賴成功下載。
4. 以 `Run > Run 'app'` 或在終端執行 `./gradlew assembleDebug` 建構 APK。

應用程式首次啟動會請求麥克風權限；授權後即可開始錄音與即時辨識。

## 主要功能

- 以 `AudioRecord` 擷取 16 kHz 單聲道音訊並串流至 Speechmatics WebSocket。
- 透過 `TranscriptManager` 處理語者標記、超時合併與字幕排版。
- 可於設定頁面調整語言、字體大小（24–48）、TTS 引擎與輸出選項。
- 支援將完整辨識內容寫入內部儲存的文字檔案。
- 可選擇在錄音時同步啟用手勢辨識（背景運行、不顯示相機畫面），右下角顯示最新手勢並可播報。

## 更換/自訂手勢模型（進階）

若你重新訓練了 MediaPipe 手勢模型，只需要在本專案替換模型資產檔即可：

- 將新模型覆蓋 `gesture/src/main/assets/gesture_recognizer.task`

> 訓練資料與訓練流程通常不會放在 Android 專案內；Android 端只負責載入 `.task`。

整合細節請參考：
- `GESTURE_INTEGRATION.md`
- `VOICE_OUTPUT_ANALYSIS.md`
- `CHAT_CONTEXT.md`（下次續接對話用）

## 後續延伸建議

- 將 `targetSdk` 更新至最新穩定版本以符合 Play Console 要求。
- 新增儀器測試覆蓋更多 UI 情境。
- 視情況導入 Kotlin 或 Jetpack Compose 以改善可維護性。
