# 手勢 + 字幕（STT）聯動（保留 Kotlin 模組）

本專案將 MediaPipe 手勢辨識整合為 `:gesture` library module，主 App（Java）可選擇在錄音時於背景同步啟用手勢辨識（不顯示相機畫面），並在右下角顯示手勢結果；也可單獨打開手勢原版相機介面做測試。

## 目前的互動方式

- `開始錄音`：啟動/停止 STT 串流錄音流程
- `只測手勢`：只打開手勢原版介面（會顯示相機畫面），用來單獨測試手勢模型
- `啟用手勢（相機）` 勾選框：
  - 勾選 + 開始錄音：在背景啟用手勢辨識（不顯示相機畫面）
  - 取消勾選 / 停止錄音：停止手勢辨識並釋放資源
- 右下角浮層：顯示最新手勢，例如 `手勢：good`；若 5 秒沒有更新會自動淡出隱藏
- 手勢語音播報（TTS）：啟用手勢時會播報辨識到的手勢（已做去重與最小間隔，避免連續播報）

## Android Studio 要打開哪個工程

請開啟 `SpeakerDiarazationDemo/` 目錄（裡面有 `settings.gradle.kts`）。

手勢模組位置：
- `SpeakerDiarazationDemo/gesture/`（Gradle module 名稱：`:gesture`）

## 重要檔案與入口（方便改/查）

- UI：
  - `SpeakerDiarazationDemo/app/src/main/res/layout/activity_main.xml`
    - `cbEnableGesture`（勾選框）
    - `tvGestureStatus`（右下角手勢浮層）
    - `tvStatusChip` / `tvGestureSpeakingChip`（頂部狀態列）
- 主 App（Java）接入：
  - `SpeakerDiarazationDemo/app/src/main/java/hk/edu/hkmu/speakerdiarazationdemo/MainActivity.java`
    - 於開始錄音後啟動：`startGestureIfEnabled()`
    - 於停止錄音時停止：`stopGesture()`
    - 手勢播報：`speakGestureLabel(...)`、釋放：`shutdownGestureTts()`
- 背景手勢辨識（Kotlin，CameraX 僅 ImageAnalysis，無預覽）：
  - `SpeakerDiarazationDemo/gesture/src/main/java/com/google/mediapipe/examples/gesturerecognizer/GestureBackgroundRunner.kt`

## 權限

- 主 App manifest 已包含相機權限：
  - `SpeakerDiarazationDemo/app/src/main/AndroidManifest.xml`
    - `android.permission.CAMERA`
- 執行期：只有勾選 `啟用手勢（相機）` 並開始錄音時才會要求相機權限。

## TTS（裝置找不到系統入口時）

若裝置系統設定裡找不到「文字轉語音 / TTS」入口，可以在 App 設定頁使用：
- `TTS 語音資料 / 設定`：嘗試開啟系統語音資料安裝頁，或導向 Play 商店
- `TTS 測試（Hi / Thank you）`：直接播報一段英文驗證引擎是否可用
- `TTS 引擎`：切換不同 TTS Engine（部分眼鏡裝置可能只有特定引擎可用）

