# 字幕眼鏡

這是一個 Android Demo，用來測試「字幕眼鏡」的基本流程：麥克風收音後送到 Speechmatics 做即時語音轉文字和語者分離，再把不同說話者的內容用字幕顯示出來。專案也整合了手勢辨識模組，可以在錄音時背景偵測手勢並用 TTS 播報。

![INMO Air 3 smart glasses](inmoair3.png)

## 主要功能

- 即時錄音，將 16 kHz 單聲道音訊串流到 Speechmatics WebSocket。
- 顯示語音轉寫結果，並用不同顏色區分說話者。
- 可在設定頁調整語言、字體大小、TTS 引擎和輸出選項。
- 可把完整辨識內容儲存成文字檔。
- 可選擇在錄音時同步啟用手勢辨識，右下角會顯示最新手勢，也可以播報結果。

## 建置需求

- Android Studio Jellyfish 或以上
- Android Gradle Plugin 8.1+
- Android SDK API Level 28 和 36
- JDK 11

目前專案設定：

- `compileSdk = 36`
- `targetSdk = 28`
- `minSdk = 28`

## Speechmatics API key 設定

真實 API key 不要放進 GitHub。這個專案會讀取本機的 `config.json`，而這個檔案已經被 `.gitignore` 排除。

1. 複製範例設定檔：

   ```powershell
   Copy-Item app/src/main/res/raw/config_example.json app/src/main/res/raw/config.json
   ```

   macOS 或 Linux 可用：

   ```bash
   cp app/src/main/res/raw/config_example.json app/src/main/res/raw/config.json
   ```

2. 打開 `app/src/main/res/raw/config.json`，把 `api_key` 改成你的 Speechmatics API key。
3. 如有需要，再調整 `language`、`region`、`max_speakers` 等設定。
4. 不要用 `git add -f app/src/main/res/raw/config.json`，否則真實 key 會被強制提交。

## 快速開始

1. 用 Android Studio 開啟專案根目錄，也就是含有 `settings.gradle.kts` 的資料夾。
2. 依照上方步驟建立 `config.json`。
3. 等 Gradle Sync 完成。
4. 用 Android Studio 執行 `app`，或在終端機執行：

   ```bash
   ./gradlew assembleDebug
   ```

App 首次啟動時會請求麥克風權限。授權後即可開始錄音和即時辨識。

## 更換手勢模型

如果重新訓練 MediaPipe 手勢模型，把新的模型覆蓋到這個位置即可：

```text
gesture/src/main/assets/gesture_recognizer.task
```
