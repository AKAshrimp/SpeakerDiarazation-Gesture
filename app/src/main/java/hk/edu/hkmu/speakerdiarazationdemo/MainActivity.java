package hk.edu.hkmu.speakerdiarazationdemo;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Handler;
import android.os.Looper;
import android.content.res.ColorStateList;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import hk.edu.hkmu.speakerdiarazationdemo.EnrolledSpeakerStore;
import hk.edu.hkmu.speakerdiarazationdemo.models.AppConfig;
import hk.edu.hkmu.speakerdiarazationdemo.models.EnrolledSpeaker;
import hk.edu.hkmu.speakerdiarazationdemo.models.TranscriptResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.Locale;
import com.google.mediapipe.examples.gesturerecognizer.GestureBackgroundRunner;
import hk.edu.hkmu.speakerdiarazationdemo.SettingsActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_AUDIO = 100;
    private static final int PERMISSION_REQUEST_CAMERA = 101;
    private static final String PREFS_NAME = "app_settings";
    private static final String PREF_LANGUAGE_CODE = "language_code";
    private static final String PREF_FONT_SIZE_SP = "font_size_sp";
    private static final float DEFAULT_FONT_SIZE_SP = 24f;
    private static final float MIN_FONT_SIZE_SP = 24f;
    private static final float MAX_FONT_SIZE_SP = 48f;
    
    private View rootContainer;
    private LinearLayout layoutControls;
    private LinearLayout layoutRecordingControls;
    private Button btnRecord;
    private ImageButton btnSettings;
    private TextView tvTranscript;
    private ScrollView scrollView;
    private Spinner spinnerLanguage;
    private TextView tvTranscriptLabel;
    private Button btnPauseRecording;
    private Button btnStopRecording;
    private TextView tvRecordingFontSize;
    private CheckBox cbEnableGesture;
    private TextView tvGestureStatus;
    private TextView tvStatusChip;
    private TextView tvGestureSpeakingChip;
    private Button btnTestGesture;
    
    private AudioRecorder audioRecorder;
    private SpeechmaticsClient speechmaticsClient;
    private TranscriptManager transcriptManager;
    private GestureBackgroundRunner gestureRunner;
    private boolean sttConnected = false;
    private TextToSpeech gestureTts;
    private boolean gestureTtsReady = false;
    private boolean gestureTtsSpeaking = false;
    private boolean pausedMicForTts = false;
    private String pendingGestureToSpeak = null;
    private String lastSpokenGestureLabel = null;
    private long lastSpokenGestureAtMs = 0L;
    private static final long GESTURE_TTS_MIN_INTERVAL_MS = 1200L;
    private static final long GESTURE_OVERLAY_AUTO_HIDE_MS = 5000L;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private Runnable gestureHideRunnable;
    private AudioManager audioManager;
    private AudioManager.OnAudioFocusChangeListener gestureTtsFocusListener;

    private boolean isRecording = false;
    private boolean isPaused = false;
    private SharedPreferences preferences;
    private AppConfig config;

    private Map<String, Integer> speakerColors = new HashMap<>();
    private final List<EnrolledSpeaker> activeEnrolledSpeakers = new ArrayList<>();
    private final Set<String> mutedSpeakerTokens = new HashSet<>();
    private final SpannableStringBuilder transcriptBuilder = new SpannableStringBuilder();
    private String lastDisplayedSpeaker = null;
    private String partialSpeaker = null;
    private int partialStartIndex = -1;
    private int partialTextStart = -1;
    
    private static final LanguageOption[] LANGUAGE_OPTIONS = new LanguageOption[] {
        new LanguageOption("cmn_en", "中英雙語 (cmn_en)"),
        new LanguageOption("cmn", "普通話 (cmn)"),
        new LanguageOption("en", "英語 (en)"),
        new LanguageOption("yue", "粵語 (yue)")
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        initViews();
        initSpeakerColors();
        loadConfig();
        setupLanguageSelector();
        applyFontSize();
        checkPermissions();
        
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyFontSize();
        refreshEnrolledSpeakers();
    }

    private void initViews() {
        rootContainer = findViewById(R.id.rootContainer);
        layoutControls = findViewById(R.id.layoutControls);
        layoutRecordingControls = findViewById(R.id.layoutRecordingControls);
        btnRecord = findViewById(R.id.btnRecord);
        btnSettings = findViewById(R.id.btnSettings);
        tvTranscript = findViewById(R.id.tvTranscript);
        tvTranscriptLabel = findViewById(R.id.tvTranscriptLabel);
        scrollView = findViewById(R.id.scrollView);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        btnPauseRecording = findViewById(R.id.btnPauseRecording);
        btnStopRecording = findViewById(R.id.btnStopRecording);
        tvRecordingFontSize = findViewById(R.id.tvRecordingFontSize);
        cbEnableGesture = findViewById(R.id.cbEnableGesture);
        tvGestureStatus = findViewById(R.id.tvGestureStatus);
        tvStatusChip = findViewById(R.id.tvStatusChip);
        tvGestureSpeakingChip = findViewById(R.id.tvGestureSpeakingChip);
        btnTestGesture = findViewById(R.id.btnTestGesture);

        btnRecord.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            } else {
                startRecording();
            }
        });

        if (btnTestGesture != null) {
            btnTestGesture.setOnClickListener(v -> {
                if (isRecording) {
                    Toast.makeText(this, "請先停止錄音再測試手勢", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(this, com.google.mediapipe.examples.gesturerecognizer.MainActivity.class);
                startActivity(intent);
            });
        }

        if (cbEnableGesture != null) {
            cbEnableGesture.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isRecording) {
                    if (isChecked) {
                        // Warm up TTS early to reduce first-time delay on slower devices (e.g. glasses).
                        ensureGestureTts();
                    }
                    setGestureOverlayVisible(false);
                    return;
                }
                if (isChecked) {
                    startGestureIfEnabled();
                } else {
                    stopGesture();
                }
            });
        }

        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> openSettings());
        }

        if (btnStopRecording != null) {
            btnStopRecording.setOnClickListener(v -> {
                if (isRecording) {
                    stopRecording();
                }
            });
        }

        if (btnPauseRecording != null) {
            btnPauseRecording.setOnClickListener(v -> togglePauseRecording());
        }

        updateRecordingUi(false);
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void initSpeakerColors() {
        resetSpeakerColors();
    }

    private void resetSpeakerColors() {
        speakerColors.clear();
        speakerColors.put("S1", Color.rgb(255, 99, 71));      // Red
        speakerColors.put("S2", Color.rgb(255, 165, 0));      // Orange
        speakerColors.put("S3", Color.rgb(255, 215, 0));      // Gold
        speakerColors.put("S4", Color.rgb(50, 205, 50));      // Green
        speakerColors.put("S5", Color.rgb(30, 144, 255));     // Blue
        speakerColors.put("s1", Color.rgb(255, 99, 71));
        speakerColors.put("s2", Color.rgb(255, 165, 0));
        speakerColors.put("s3", Color.rgb(255, 215, 0));
        speakerColors.put("s4", Color.rgb(50, 205, 50));
        speakerColors.put("s5", Color.rgb(30, 144, 255));
        applyEnrolledSpeakerColors();
    }

    private void refreshEnrolledSpeakers() {
        activeEnrolledSpeakers.clear();
        mutedSpeakerTokens.clear();
        List<EnrolledSpeaker> storedSpeakers = EnrolledSpeakerStore.getSpeakers(this);
        for (EnrolledSpeaker speaker : storedSpeakers) {
            if (speaker == null) {
                continue;
            }
            activeEnrolledSpeakers.add(speaker);
            if (speaker.isMuted()) {
                addMutedTokensForSpeaker(speaker);
            }
        }
        resetSpeakerColors();
    }

    private void applyEnrolledSpeakerColors() {
        int namedColor = Color.parseColor("#2196F3");
        for (EnrolledSpeaker speaker : activeEnrolledSpeakers) {
            if (speaker != null && speaker.getName() != null) {
                speakerColors.put(speaker.getName(), namedColor);
            }
        }
    }

    private void addMutedTokensForSpeaker(EnrolledSpeaker speaker) {
        if (speaker == null) {
            return;
        }
        String name = speaker.getName();
        if (!TextUtils.isEmpty(name) && !isGenericSpeakerLabel(name)) {
            mutedSpeakerTokens.add(name);
        }
        List<String> identifiers = speaker.getSpeakerIdentifiers();
        if (identifiers == null) {
            return;
        }
        for (String identifier : identifiers) {
            if (!TextUtils.isEmpty(identifier)) {
                mutedSpeakerTokens.add(identifier);
            }
        }
    }

    private boolean isGenericSpeakerLabel(String name) {
        if (TextUtils.isEmpty(name) || name.length() < 2) {
            return false;
        }
        char first = Character.toUpperCase(name.charAt(0));
        if (first != 'S') {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isDigit(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private void updateRecordingUi(boolean recording) {
        if (recording) {
            if (layoutControls != null) {
                layoutControls.setVisibility(View.GONE);
            }
            if (tvTranscriptLabel != null) {
                tvTranscriptLabel.setVisibility(View.GONE);
            }
            if (layoutRecordingControls != null) {
                layoutRecordingControls.setVisibility(View.VISIBLE);
            }
            if (rootContainer != null) {
                rootContainer.setBackgroundColor(Color.BLACK);
            }
            if (scrollView != null) {
                scrollView.setBackgroundColor(Color.BLACK);
            }
        } else {
            if (layoutControls != null) {
                layoutControls.setVisibility(View.VISIBLE);
            }
            if (tvTranscriptLabel != null) {
                tvTranscriptLabel.setVisibility(View.VISIBLE);
            }
            if (layoutRecordingControls != null) {
                layoutRecordingControls.setVisibility(View.GONE);
            }
            if (rootContainer != null) {
                rootContainer.setBackgroundColor(Color.parseColor("#1E1E1E"));
            }
            if (scrollView != null) {
                scrollView.setBackgroundColor(Color.parseColor("#2E2E2E"));
            }
        }

        refreshPauseButtonState();
    }

    private void applyFontSize() {
        float fontSize = getSavedFontSize();
        if (tvTranscript != null) {
            tvTranscript.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
        }
        if (tvRecordingFontSize != null) {
            tvRecordingFontSize.setText(String.format(Locale.getDefault(), "字體：%.0f", fontSize));
        }
    }

    private float getSavedFontSize() {
        if (preferences != null) {
            float value = preferences.getFloat(PREF_FONT_SIZE_SP, DEFAULT_FONT_SIZE_SP);
            if (value < MIN_FONT_SIZE_SP) value = MIN_FONT_SIZE_SP;
            if (value > MAX_FONT_SIZE_SP) value = MAX_FONT_SIZE_SP;
            return value;
        }
        return DEFAULT_FONT_SIZE_SP;
    }

    private void updateStatusChip() {
        if (tvStatusChip == null) {
            return;
        }
        if (!isRecording) {
            tvStatusChip.setVisibility(View.GONE);
            if (tvGestureSpeakingChip != null) {
                tvGestureSpeakingChip.setVisibility(View.GONE);
            }
            return;
        }

        String text;
        int tint;

        if (!sttConnected) {
            text = "● 連線中";
            tint = Color.parseColor("#2196F3");
        } else if (isPaused) {
            text = "● 暫停";
            tint = Color.parseColor("#FFC107");
        } else {
            text = "● 錄音中";
            tint = Color.parseColor("#4CAF50");
        }

        tvStatusChip.setText(text);
        tvStatusChip.setVisibility(View.VISIBLE);
        tvStatusChip.setBackgroundTintList(ColorStateList.valueOf(tint));

        if (tvGestureSpeakingChip != null) {
            if (!isGestureEnabled()) {
                tvGestureSpeakingChip.setVisibility(View.GONE);
            } else if (gestureTtsSpeaking) {
                tvGestureSpeakingChip.setVisibility(View.VISIBLE);
                tvGestureSpeakingChip.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#9C27B0")));
                tvGestureSpeakingChip.setText("手勢播報中");
            } else if (pendingGestureToSpeak != null) {
                tvGestureSpeakingChip.setVisibility(View.VISIBLE);
                tvGestureSpeakingChip.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#78909C")));
                tvGestureSpeakingChip.setText("手勢等待中");
            } else {
                tvGestureSpeakingChip.setVisibility(View.GONE);
            }
        }
    }

    private void loadConfig() {
        try {
            ConfigManager.getInstance().loadConfig(this);
            config = ConfigManager.getInstance().getConfig();
            Log.d(TAG, "Config loaded successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load config", e);
            Toast.makeText(this, "無法載入配置文件", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupLanguageSelector() {
        if (spinnerLanguage == null) {
            return;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.item_language_spinner,
                getLanguageLabels()
        );
        adapter.setDropDownViewResource(R.layout.item_language_spinner_dropdown);
        spinnerLanguage.setAdapter(adapter);

        String savedLanguage = preferences != null
                ? preferences.getString(PREF_LANGUAGE_CODE, null)
                : null;
        String initialLanguage = savedLanguage;
        if (initialLanguage == null && config != null) {
            initialLanguage = config.getLanguage();
        }

        int selectedIndex = getLanguageIndex(initialLanguage);
        if (selectedIndex < 0) {
            selectedIndex = 0;
        }
        String resolvedLanguage = LANGUAGE_OPTIONS[selectedIndex].code;
        applyLanguage(resolvedLanguage, false);
        spinnerLanguage.setSelection(selectedIndex, false);

        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean isFirstSelection = true;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isFirstSelection) {
                    isFirstSelection = false;
                    return;
                }
                String languageCode = LANGUAGE_OPTIONS[position].code;
                applyLanguage(languageCode, true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action required
            }
        });
    }

    private void applyLanguage(String languageCode, boolean showToast) {
        AppConfig activeConfig = config != null ? config : ConfigManager.getInstance().getConfig();
        if (activeConfig != null) {
            activeConfig.setLanguage(languageCode);
            config = activeConfig;
        }

        if (preferences != null) {
            preferences.edit().putString(PREF_LANGUAGE_CODE, languageCode).apply();
        }

        Log.d(TAG, "Language set to: " + languageCode);

        if (showToast) {
            Toast.makeText(this, "語言已切換為: " + getLanguageLabel(languageCode), Toast.LENGTH_SHORT).show();
        }
    }

    private String[] getLanguageLabels() {
        String[] labels = new String[LANGUAGE_OPTIONS.length];
        for (int i = 0; i < LANGUAGE_OPTIONS.length; i++) {
            labels[i] = LANGUAGE_OPTIONS[i].label;
        }
        return labels;
    }

    private int getLanguageIndex(String languageCode) {
        if (languageCode == null) {
            return -1;
        }
        for (int i = 0; i < LANGUAGE_OPTIONS.length; i++) {
            if (LANGUAGE_OPTIONS[i].code.equals(languageCode)) {
                return i;
            }
        }
        return -1;
    }

    private String getLanguageLabel(String languageCode) {
        for (LanguageOption option : LANGUAGE_OPTIONS) {
            if (option.code.equals(languageCode)) {
                return option.label;
            }
        }
        return languageCode;
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_AUDIO);
        }
    }

    private boolean isGestureEnabled() {
        return cbEnableGesture != null && cbEnableGesture.isChecked();
    }

    private void setGestureOverlayVisible(boolean visible) {
        if (tvGestureStatus != null) {
            tvGestureStatus.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void updateGestureLabel(String label) {
        if (tvGestureStatus != null) {
            tvGestureStatus.setText("手勢：" + (label != null && !label.isEmpty() ? label : "--"));
        }
    }

    private void onGestureRecognized(String label) {
        if (tvGestureStatus == null || label == null) {
            return;
        }
        String trimmed = label.trim();
        if (trimmed.isEmpty() || "--".equals(trimmed)) {
            return;
        }

        updateGestureLabel(trimmed);
        showGestureOverlay();
        scheduleGestureOverlayHide();
        speakGestureLabel(trimmed);
    }

    private void showGestureOverlay() {
        if (tvGestureStatus == null) {
            return;
        }
        if (gestureHideRunnable != null) {
            uiHandler.removeCallbacks(gestureHideRunnable);
        }
        tvGestureStatus.animate().cancel();
        if (tvGestureStatus.getVisibility() != View.VISIBLE) {
            tvGestureStatus.setAlpha(0f);
            tvGestureStatus.setVisibility(View.VISIBLE);
        }
        tvGestureStatus.animate().alpha(1f).setDuration(200).start();
    }

    private void scheduleGestureOverlayHide() {
        if (tvGestureStatus == null) {
            return;
        }
        if (gestureHideRunnable != null) {
            uiHandler.removeCallbacks(gestureHideRunnable);
        }
        gestureHideRunnable = () -> {
            if (tvGestureStatus == null) {
                return;
            }
            tvGestureStatus.animate().cancel();
            tvGestureStatus.animate()
                    .alpha(0f)
                    .setDuration(350)
                    .withEndAction(() -> {
                        if (tvGestureStatus != null) {
                            tvGestureStatus.setVisibility(View.GONE);
                            tvGestureStatus.setAlpha(1f);
                            updateGestureLabel("--");
                        }
                    })
                    .start();
        };
        uiHandler.postDelayed(gestureHideRunnable, GESTURE_OVERLAY_AUTO_HIDE_MS);
    }

    private void ensureGestureTts() {
        if (gestureTts != null) {
            return;
        }
        gestureTtsReady = false;
        String preferredEngine = preferences != null ? preferences.getString(SettingsActivity.PREF_TTS_ENGINE_PACKAGE, null) : null;
        try {
            gestureTts = preferredEngine == null
                ? new TextToSpeech(this, status -> onGestureTtsInit(status))
                : new TextToSpeech(this, status -> onGestureTtsInit(status), preferredEngine);
        } catch (Throwable ignored) {
            gestureTts = new TextToSpeech(this, status -> onGestureTtsInit(status));
        }
    }

    private void onGestureTtsInit(int status) {
            gestureTtsReady = status == TextToSpeech.SUCCESS;
            if (gestureTtsReady && gestureTts != null) {
                gestureTts.setAudioAttributes(
                    new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                );
                int result = gestureTts.setLanguage(Locale.getDefault());
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    gestureTts.setLanguage(Locale.US);
                }
                gestureTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        uiHandler.post(() -> {
                            gestureTtsSpeaking = true;
                            updateStatusChip();
                            pauseAudioCaptureForTts();
                        });
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        uiHandler.post(() -> {
                            gestureTtsSpeaking = false;
                            updateStatusChip();
                            resumeAudioCaptureAfterTts();
                            abandonGestureTtsFocus();
                        });
                    }

                    @Override
                    public void onError(String utteranceId) {
                        uiHandler.post(() -> {
                            gestureTtsSpeaking = false;
                            updateStatusChip();
                            resumeAudioCaptureAfterTts();
                            abandonGestureTtsFocus();
                        });
                    }
                });
            }
            if (gestureTtsReady) {
                speakPendingGestureIfAny();
            }
    }

    private void speakPendingGestureIfAny() {
        if (pendingGestureToSpeak == null) {
            return;
        }
        String label = pendingGestureToSpeak;
        pendingGestureToSpeak = null;
        // Re-run the normal gating (isRecording/isGestureEnabled/debounce).
        speakGestureLabel(label);
    }

    private boolean requestGestureTtsFocus() {
        if (audioManager == null) {
            return true;
        }
        if (gestureTtsFocusListener == null) {
            gestureTtsFocusListener = focusChange -> {};
        }
        int result = audioManager.requestAudioFocus(
            gestureTtsFocusListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        );
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void abandonGestureTtsFocus() {
        if (audioManager == null || gestureTtsFocusListener == null) {
            return;
        }
        try {
            audioManager.abandonAudioFocus(gestureTtsFocusListener);
        } catch (Throwable ignored) {}
    }

    private void pauseAudioCaptureForTts() {
        if (!isRecording || isPaused) {
            return;
        }
        if (pausedMicForTts) {
            return;
        }
        if (audioRecorder != null) {
            try {
                audioRecorder.stopRecording();
                audioRecorder.release();
            } catch (Throwable ignored) {
            } finally {
                audioRecorder = null;
            }
            pausedMicForTts = true;
        }
    }

    private void resumeAudioCaptureAfterTts() {
        if (!pausedMicForTts) {
            return;
        }
        if (!isRecording || isPaused) {
            return;
        }
        if (speechmaticsClient == null || !speechmaticsClient.isConnected()) {
            return;
        }
        pausedMicForTts = false;
        startAudioRecording();
    }

    private void speakGestureLabel(String label) {
        if (label == null) {
            return;
        }
        String trimmed = label.trim();
        if (trimmed.isEmpty() || "--".equals(trimmed)) {
            return;
        }
        if (!isGestureEnabled() || !isRecording) {
            return;
        }

        long now = SystemClock.uptimeMillis();
        if (trimmed.equalsIgnoreCase(lastSpokenGestureLabel)
                && (now - lastSpokenGestureAtMs) < GESTURE_TTS_MIN_INTERVAL_MS) {
            return;
        }

        ensureGestureTts();
        if (!gestureTtsReady || gestureTts == null) {
            // TTS init is async. Cache the most recent gesture so it can be spoken once ready.
            pendingGestureToSpeak = trimmed;
            updateStatusChip();
            return;
        }
        // Some devices won't play TTS while AudioRecord is active; stop mic capture first.
        pauseAudioCaptureForTts();
        // Try to request audio focus, but don't hard-block speaking if it's not granted on this device.
        requestGestureTtsFocus();

        lastSpokenGestureLabel = trimmed;
        lastSpokenGestureAtMs = now;

        gestureTts.speak(trimmed, TextToSpeech.QUEUE_FLUSH, null, "gesture_" + now);
    }

    private void shutdownGestureTts() {
        gestureTtsSpeaking = false;
        pendingGestureToSpeak = null;
        if (gestureTts != null) {
            try {
                gestureTts.stop();
                gestureTts.shutdown();
            } catch (Throwable ignored) {
            }
        }
        gestureTts = null;
        gestureTtsReady = false;
        lastSpokenGestureLabel = null;
        lastSpokenGestureAtMs = 0L;
        updateStatusChip();
        resumeAudioCaptureAfterTts();
        pausedMicForTts = false;
        abandonGestureTtsFocus();
    }

    private void startGestureIfEnabled() {
        if (!isGestureEnabled() || !isRecording) {
            setGestureOverlayVisible(false);
            return;
        }

        // Warm up TTS before the first gesture arrives to avoid long "queued" time.
        ensureGestureTts();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CAMERA
            );
            Toast.makeText(this, "需要相機權限才能啟用手勢辨識", Toast.LENGTH_SHORT).show();
            setGestureOverlayVisible(false);
            return;
        }

        if (gestureRunner != null && gestureRunner.isRunning()) {
            setGestureOverlayVisible(true);
            return;
        }

        setGestureOverlayVisible(false);
        gestureRunner = new GestureBackgroundRunner(
                this,
                this,
                new GestureBackgroundRunner.Listener() {
                    @Override
                    public void onGesture(String label) {
                        onGestureRecognized(label);
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(MainActivity.this, "手勢錯誤：" + message, Toast.LENGTH_SHORT).show();
                    }
                }
        );
        gestureRunner.start();
    }

    private void stopGesture() {
        if (gestureHideRunnable != null) {
            uiHandler.removeCallbacks(gestureHideRunnable);
            gestureHideRunnable = null;
        }
        if (gestureRunner != null) {
            gestureRunner.stop();
            gestureRunner = null;
        }
        setGestureOverlayVisible(false);
        updateGestureLabel("--");
        shutdownGestureTts();
    }

    private void startRecording() {
        if (config == null) {
            Toast.makeText(this, "配置未載入", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "請授予麥克風權限", Toast.LENGTH_SHORT).show();
            checkPermissions();
            return;
        }

        if (!isNetworkAvailable()) {
            Toast.makeText(this, "請檢查網路連線後再試", Toast.LENGTH_SHORT).show();
            return;
        }
        
        refreshEnrolledSpeakers();
        if (config != null) {
            String preferredLanguage = preferences != null
                    ? preferences.getString(PREF_LANGUAGE_CODE, config.getLanguage())
                    : config.getLanguage();
            if (preferredLanguage != null && !preferredLanguage.equalsIgnoreCase(config.getLanguage())) {
                config.setLanguage(preferredLanguage);
                Log.d(TAG, "Applied preferred language before start: " + preferredLanguage);
            }
        }
        
        isRecording = true;
        isPaused = false;
        sttConnected = false;
        btnRecord.setEnabled(false);
        transcriptBuilder.clear();
        lastDisplayedSpeaker = null;
        tvTranscript.setText("");
        partialSpeaker = null;
        partialStartIndex = -1;
        partialTextStart = -1;
        updateRecordingUi(true);
        refreshPauseButtonState();
        updateStatusChip();

        startGestureIfEnabled();

        transcriptManager = new TranscriptManager(this, config, new TranscriptManager.TranscriptCallback() {
            @Override
            public void onTranscriptUpdate(TranscriptResult result) {
                runOnUiThread(() -> appendTranscript(result));
            }

            @Override
            public void onPartialTranscript(String speakerName, String text) {
                runOnUiThread(() -> displayPartial(speakerName, text));
            }

            @Override
            public void onPartialTranscriptCleared(String speakerName) {
                runOnUiThread(() -> clearPartialDisplay(speakerName));
            }
        });
        transcriptManager.setMutedSpeakers(mutedSpeakerTokens);

        speechmaticsClient = new SpeechmaticsClient(
                config,
                transcriptManager,
                new SpeechmaticsClient.SpeechmaticsCallback() {
                    @Override
                    public void onConnected() {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "已連接", Toast.LENGTH_SHORT).show();
                            sttConnected = true;
                            btnRecord.setEnabled(true);
                            partialSpeaker = null;
                            partialStartIndex = -1;
                            partialTextStart = -1;
                            if (!isPaused) {
                                startAudioRecording();
                            }
                            refreshPauseButtonState();
                            updateStatusChip();
                        });
                    }

                    @Override
                    public void onDisconnected() {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "已斷開連接", Toast.LENGTH_SHORT).show();
                            isPaused = false;
                            sttConnected = false;
                            refreshPauseButtonState();
                            updateStatusChip();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "錯誤: " + error, Toast.LENGTH_LONG).show();
                            stopRecording(false);
                        });
                    }
                }
        );

        speechmaticsClient.setEnrolledSpeakers(activeEnrolledSpeakers);
        speechmaticsClient.connect();
    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork == null) {
                return false;
            }
            NetworkCapabilities capabilities =
                    connectivityManager.getNetworkCapabilities(activeNetwork);
            return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            );
        } else {
            @SuppressWarnings("deprecation")
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
    }

    private void startAudioRecording() {
        try {
            audioRecorder = new AudioRecorder(data -> {
                if (speechmaticsClient != null && speechmaticsClient.isConnected()) {
                    speechmaticsClient.sendAudio(data);
                }
            });
            audioRecorder.startRecording();
            Log.d(TAG, "Audio recording started");
            isPaused = false;
            refreshPauseButtonState();
            updateStatusChip();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Failed to start audio recording", e);
            Toast.makeText(this, "此裝置不支援目前的錄音設定", Toast.LENGTH_LONG).show();
            stopRecording(false);
        }
    }

    private void togglePauseRecording() {
        if (!isRecording) {
            return;
        }
        if (!isPaused) {
            pauseAudioCapture();
        } else {
            if (speechmaticsClient == null || !speechmaticsClient.isConnected()) {
                Toast.makeText(this, "尚未連線，無法恢復錄音", Toast.LENGTH_SHORT).show();
                return;
            }
            resumeAudioCapture();
        }
    }

    private void pauseAudioCapture() {
        if (audioRecorder != null) {
            audioRecorder.stopRecording();
            audioRecorder.release();
            audioRecorder = null;
        }
        isPaused = true;
        if (transcriptManager != null) {
            transcriptManager.handleEndOfUtterance();
        }
        Toast.makeText(this, "暫停錄音", Toast.LENGTH_SHORT).show();
        refreshPauseButtonState();
        updateStatusChip();
    }

    private void resumeAudioCapture() {
        if (speechmaticsClient == null || !speechmaticsClient.isConnected()) {
            Toast.makeText(this, "尚未連線至服務器", Toast.LENGTH_SHORT).show();
            return;
        }
        startAudioRecording();
        Toast.makeText(this, "恢復錄音", Toast.LENGTH_SHORT).show();
        updateStatusChip();
    }

    private void refreshPauseButtonState() {
        if (btnPauseRecording == null) {
            return;
        }
        if (!isRecording) {
            btnPauseRecording.setText("暫停錄音");
            btnPauseRecording.setEnabled(false);
        } else {
            btnPauseRecording.setText(isPaused ? "繼續錄音" : "暫停錄音");
            boolean canResume = speechmaticsClient != null && speechmaticsClient.isConnected();
            btnPauseRecording.setEnabled(!isPaused || (isPaused && canResume));
        }
    }

    private void stopRecording() {
        stopRecording(true);
    }

    private void stopRecording(boolean showSuccessToast) {
        boolean wasRecording = isRecording;
        isRecording = false;
        isPaused = false;
        sttConnected = false;
        btnRecord.setText("開始錄音");
        btnRecord.setEnabled(true);
        partialSpeaker = null;
        partialStartIndex = -1;
        partialTextStart = -1;
        updateRecordingUi(false);
        refreshPauseButtonState();
        updateStatusChip();
        stopGesture();

        // Stop audio recording
        if (audioRecorder != null) {
            audioRecorder.stopRecording();
            audioRecorder.release();
            audioRecorder = null;
        }
        
        // Flush remaining transcripts
        if (transcriptManager != null) {
            transcriptManager.flush();
            transcriptManager.saveToFile();
        }
        
        // Disconnect WebSocket
        if (speechmaticsClient != null) {
            speechmaticsClient.disconnect();
            speechmaticsClient = null;
        }
        
        if (showSuccessToast && wasRecording) {
            Toast.makeText(this, "錄音已停止並保存", Toast.LENGTH_SHORT).show();
        }
        Log.d(TAG, "Recording stopped");
    }

    private void appendTranscript(TranscriptResult result) {
        String speakerName = result.getSpeaker();
        String content = result.getText();
        if (content == null || content.trim().isEmpty()) {
            return;
        }

        clearPartialDisplay(speakerName);

        int color = speakerColors.getOrDefault(speakerName, Color.WHITE);

        if (transcriptBuilder.length() > 0) {
            transcriptBuilder.append("\n");
        }
        int start = transcriptBuilder.length();
        transcriptBuilder.append(speakerName).append("：").append(content.trim());
        transcriptBuilder.setSpan(
            new ForegroundColorSpan(color),
            start,
            transcriptBuilder.length(),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        lastDisplayedSpeaker = speakerName;
        tvTranscript.setText(transcriptBuilder);

        // Auto scroll to bottom
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void displayPartial(String speakerName, String text) {
        String trimmed = text != null ? text.trim() : "";
        if (trimmed.isEmpty()) {
            clearPartialDisplay(speakerName);
            return;
        }

        String partialText = "（正在說話：" + trimmed + "）";
        int partialColor = Color.parseColor("#B0B0B0");

        if (partialStartIndex >= 0) {
            if (partialSpeaker != null && partialSpeaker.equals(speakerName)) {
                transcriptBuilder.replace(partialTextStart, transcriptBuilder.length(), partialText);
                for (Object span : transcriptBuilder.getSpans(partialTextStart, transcriptBuilder.length(), Object.class)) {
                    transcriptBuilder.removeSpan(span);
                }
                transcriptBuilder.setSpan(new ForegroundColorSpan(partialColor), partialTextStart, transcriptBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                transcriptBuilder.setSpan(new StyleSpan(Typeface.ITALIC), partialTextStart, transcriptBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                partialSpeaker = speakerName;
                tvTranscript.setText(transcriptBuilder);
                scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                return;
            } else {
                removePartial();
            }
        }

        if (partialStartIndex < 0) {
            partialStartIndex = transcriptBuilder.length();
            if (transcriptBuilder.length() > 0) {
                transcriptBuilder.append("\n");
            }

            partialTextStart = transcriptBuilder.length();
            transcriptBuilder.append(partialText);
            partialSpeaker = speakerName;
            transcriptBuilder.setSpan(new ForegroundColorSpan(partialColor), partialTextStart, transcriptBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            transcriptBuilder.setSpan(new StyleSpan(Typeface.ITALIC), partialTextStart, transcriptBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        tvTranscript.setText(transcriptBuilder);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void clearPartialDisplay(String speakerName) {
        if (partialStartIndex >= 0 && (partialSpeaker == null || partialSpeaker.equals(speakerName))) {
            removePartial();
        }
    }

    private void removePartial() {
        if (partialStartIndex >= 0) {
            transcriptBuilder.delete(partialStartIndex, transcriptBuilder.length());
            partialStartIndex = -1;
            partialTextStart = -1;
            partialSpeaker = null;
            tvTranscript.setText(transcriptBuilder);
        }
    }

    @Override
    public void onBackPressed() {
        if (isRecording) {
            stopRecording();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isRecording) {
            stopRecording();
        }
        stopGesture();
        shutdownGestureTts();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "麥克風權限已授予", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "需要麥克風權限才能使用此應用", Toast.LENGTH_LONG).show();
            }
            return;
        }
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted) {
                Toast.makeText(this, "相機權限已授予", Toast.LENGTH_SHORT).show();
                startGestureIfEnabled();
            } else {
                Toast.makeText(this, "未授予相機權限，手勢辨識不會啟用", Toast.LENGTH_LONG).show();
                setGestureOverlayVisible(false);
            }
        }
    }

    private static class LanguageOption {
        final String code;
        final String label;

        LanguageOption(String code, String label) {
            this.code = code;
            this.label = label;
        }
    }
}
