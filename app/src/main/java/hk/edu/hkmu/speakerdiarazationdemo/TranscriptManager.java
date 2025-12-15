package hk.edu.hkmu.speakerdiarazationdemo;

import android.content.Context;
import android.util.Log;
import com.google.gson.JsonObject;
import hk.edu.hkmu.speakerdiarazationdemo.models.AppConfig;
import hk.edu.hkmu.speakerdiarazationdemo.models.TranscriptResult;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class TranscriptManager {
    private static final String TAG = "TranscriptManager";
    private static final String DEFAULT_SPEAKER = "Unknown";

    private final Map<String, SpeakerBuffer> bufferBySpeaker = new HashMap<>();
    private final List<TranscriptResult> transcriptRecords = new ArrayList<>();
    private final AppConfig config;
    private final TranscriptCallback callback;
    private final Context context;
    private final Set<String> mutedSpeakers = new HashSet<>();
    private final Set<String> mutedSpeakersLowerCase = new HashSet<>();

    private String lastSpeaker = null;
    private String partialSpeakerId = null;

    public interface TranscriptCallback {
        void onTranscriptUpdate(TranscriptResult result);
        void onPartialTranscript(String speakerName, String text);
        void onPartialTranscriptCleared(String speakerName);
    }

    public TranscriptManager(Context context, AppConfig config, TranscriptCallback callback) {
        this.context = context;
        this.config = config;
        this.callback = callback;
    }

    public void setMutedSpeakers(Collection<String> speakerIds) {
        mutedSpeakers.clear();
        mutedSpeakersLowerCase.clear();
        if (speakerIds == null) {
            return;
        }
        for (String id : speakerIds) {
            if (id == null) {
                continue;
            }
            String trimmed = id.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            mutedSpeakers.add(trimmed);
            mutedSpeakersLowerCase.add(trimmed.toLowerCase(Locale.ROOT));
        }
    }

    private boolean isSpeakerMuted(String speakerId) {
        if (speakerId == null) {
            return false;
        }
        String trimmed = speakerId.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        return mutedSpeakers.contains(trimmed) || mutedSpeakersLowerCase.contains(lower);
    }

    public void addPartialTranscript(String text) {
        if (callback == null) {
            return;
        }

        String speakerId;
        if (partialSpeakerId != null) {
            speakerId = partialSpeakerId;
        } else if (lastSpeaker != null) {
            speakerId = lastSpeaker;
        } else {
            speakerId = DEFAULT_SPEAKER;
        }
        String speakerName = config != null ? config.getSpeakerName(speakerId) : speakerId;
        if (isSpeakerMuted(speakerId)) {
            return;
        }
        String trimmed = text != null ? text.trim() : "";

        if (trimmed.isEmpty()) {
            callback.onPartialTranscriptCleared(speakerName);
        } else {
            callback.onPartialTranscript(speakerName, trimmed);
        }
    }

    public void addToken(String speaker,
                         String rawContent,
                         String displayContent,
                         String resultType,
                         boolean isEos,
                         JsonObject rawResult) {
        String normalizedSpeaker = (speaker == null || speaker.trim().isEmpty()) ? DEFAULT_SPEAKER : speaker;

        if (lastSpeaker != null && !normalizedSpeaker.equals(lastSpeaker)) {
            SpeakerBuffer previousBuffer = bufferBySpeaker.get(lastSpeaker);
            if (previousBuffer != null && !previousBuffer.isBlank()) {
                outputSpeakerText(lastSpeaker);
            }
        }

        if (isSpeakerMuted(normalizedSpeaker)) {
            bufferBySpeaker.remove(normalizedSpeaker);
            lastSpeaker = normalizedSpeaker;
            partialSpeakerId = normalizedSpeaker;
            return;
        }

        SpeakerBuffer buffer = bufferBySpeaker.computeIfAbsent(normalizedSpeaker, key -> new SpeakerBuffer());

        if ("speaker_change".equals(resultType)) {
            if (!buffer.isBlank()) {
                outputSpeakerText(normalizedSpeaker);
            }
            lastSpeaker = normalizedSpeaker;
            return;
        }

        if ("self_correction".equals(resultType)) {
            handleSelfCorrection(buffer, rawResult);
            lastSpeaker = normalizedSpeaker;
            return;
        }

        TokenType tokenType = mapTokenType(resultType);
        String tokenText = chooseTokenText(displayContent, rawContent, tokenType);
        boolean isFinalResult = rawResult != null && rawResult.has("is_final") && rawResult.get("is_final").getAsBoolean();
        boolean shouldOutput = false;
        if (!tokenText.isEmpty()) {
            buffer.addToken(new Token(tokenType, tokenText));
            shouldOutput = isEos || isFinalResult || shouldFlushOnPunctuation(tokenType, tokenText);
        }

        if (shouldOutput) {
            outputSpeakerText(normalizedSpeaker);
        }

        lastSpeaker = normalizedSpeaker;
        if (!"speaker_change".equals(resultType)) {
            partialSpeakerId = normalizedSpeaker;
        }
    }

    private void outputSpeakerText(String speaker) {
        SpeakerBuffer buffer = bufferBySpeaker.get(speaker);
        if (buffer == null) {
            return;
        }

        String fullText = buffer.getText().trim();
        if (fullText.isEmpty()) {
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timestamp = sdf.format(new Date());
        String speakerName = config != null ? config.getSpeakerName(speaker) : speaker;

        TranscriptResult result = new TranscriptResult(speakerName, fullText, timestamp, false);
        transcriptRecords.add(result);

        if (callback != null) {
            callback.onPartialTranscriptCleared(speakerName);
            callback.onTranscriptUpdate(result);
        }

        buffer.clear();
        Log.d(TAG, "Output: " + result.toString());

        if (partialSpeakerId != null && partialSpeakerId.equals(speaker)) {
            partialSpeakerId = null;
        }
    }

    public void flush() {
        for (String speaker : new ArrayList<>(bufferBySpeaker.keySet())) {
            outputSpeakerText(speaker);
        }
        partialSpeakerId = null;
    }

    public void handleEndOfUtterance() {
        if (lastSpeaker != null) {
            outputSpeakerText(lastSpeaker);
        } else {
            flush();
        }
        partialSpeakerId = null;
    }

    private boolean shouldFlushOnPunctuation(TokenType tokenType, String tokenText) {
        if (tokenType != TokenType.PUNCTUATION || tokenText == null || tokenText.isEmpty()) {
            return false;
        }
        char lastChar = tokenText.charAt(tokenText.length() - 1);
        return isSentenceEndingPunctuation(lastChar);
    }

    private void handleSelfCorrection(SpeakerBuffer buffer, JsonObject rawResult) {
        if (buffer == null || buffer.isBlank()) {
            return;
        }

        int tokensToRemove = extractCorrectionLength(rawResult);
        buffer.removeLastTokens(tokensToRemove);
    }

    private int extractCorrectionLength(JsonObject rawResult) {
        if (rawResult == null) {
            return 1;
        }

        try {
            if (rawResult.has("length")) {
                return Math.max(1, rawResult.get("length").getAsInt());
            }
            if (rawResult.has("num_words")) {
                return Math.max(1, rawResult.get("num_words").getAsInt());
            }
            if (rawResult.has("num_tokens")) {
                return Math.max(1, rawResult.get("num_tokens").getAsInt());
            }
            if (rawResult.has("correction_length")) {
                return Math.max(1, rawResult.get("correction_length").getAsInt());
            }
        } catch (Exception ignored) {
            // Default to removing the last token on parsing errors.
        }

        return 1;
    }

    private TokenType mapTokenType(String resultType) {
        if ("punctuation".equals(resultType)) {
            return TokenType.PUNCTUATION;
        }
        if ("entity".equals(resultType)) {
            return TokenType.ENTITY;
        }
        if ("word".equals(resultType)) {
            return TokenType.WORD;
        }
        return TokenType.OTHER;
    }

    private String chooseTokenText(String displayContent, String rawContent, TokenType tokenType) {
        String candidate = displayContent != null && !displayContent.trim().isEmpty()
                ? displayContent
                : rawContent;

        if (candidate == null) {
            return "";
        }

        if (tokenType == TokenType.PUNCTUATION) {
            return candidate.trim();
        }

        return candidate.trim();
    }

    private static boolean shouldInsertSpace(StringBuilder builder, Token previous, Token current) {
        if (current == null || current.text == null || current.text.isEmpty()) {
            return false;
        }

        if (current.type == TokenType.PUNCTUATION) {
            return false;
        }

        char prevChar = getLastVisibleChar(builder);
        char nextChar = getFirstVisibleChar(current.text);

        if (prevChar == 0 || nextChar == 0) {
            return false;
        }

        if (isCjk(prevChar) || isCjk(nextChar)) {
            return false;
        }

        if (Character.isWhitespace(prevChar)) {
            return false;
        }

        if (prevChar == '-' || nextChar == '-') {
            return false;
        }

        if (current.text.startsWith("'") || current.text.startsWith("\u2019")) {
            return false;
        }

        if (prevChar == '"' || prevChar == '\u201C' || prevChar == '\u201D' || prevChar == '(' || prevChar == '[' || prevChar == '{') {
            return false;
        }

        if (isSentenceEndingPunctuation(prevChar) || (previous != null && previous.type == TokenType.PUNCTUATION)) {
            return true;
        }

        if (isPunctuationChar(prevChar)) {
            return true;
        }

        return true;
    }

    private static char getLastVisibleChar(StringBuilder builder) {
        for (int i = builder.length() - 1; i >= 0; i--) {
            char c = builder.charAt(i);
            if (!Character.isWhitespace(c)) {
                return c;
            }
        }
        return 0;
    }

    private static char getFirstVisibleChar(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!Character.isWhitespace(c)) {
                return c;
            }
        }
        return 0;
    }

    private static void trimTrailingSpaces(StringBuilder builder) {
        while (builder.length() > 0 && Character.isWhitespace(builder.charAt(builder.length() - 1))) {
            builder.deleteCharAt(builder.length() - 1);
        }
    }

    private static boolean isSentenceEndingPunctuation(char c) {
        return c == '.' || c == '!' || c == '?' || c == '。' || c == '！' || c == '？';
    }

    private static boolean isPunctuationChar(char c) {
        return ",.;:!?".indexOf(c) >= 0;
    }

    private static boolean isCjk(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT
                || block == Character.UnicodeBlock.HIRAGANA
                || block == Character.UnicodeBlock.KATAKANA
                || block == Character.UnicodeBlock.HANGUL_SYLLABLES
                || block == Character.UnicodeBlock.BOPOMOFO
                || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION;
    }

    private static class SpeakerBuffer {
        private final List<Token> tokens = new ArrayList<>();
        private String cachedText = "";
        private boolean dirty = true;

        void addToken(Token token) {
            tokens.add(token);
            dirty = true;
        }

        void removeLastTokens(int count) {
            if (tokens.isEmpty()) {
                return;
            }
            int remove = Math.min(count, tokens.size());
            for (int i = 0; i < remove; i++) {
                tokens.remove(tokens.size() - 1);
            }
            dirty = true;
        }

        String getText() {
            if (dirty) {
                cachedText = buildText();
                dirty = false;
            }
            return cachedText;
        }

        int length() {
            return getText().length();
        }

        boolean isBlank() {
            return getText().trim().isEmpty();
        }

        void clear() {
            tokens.clear();
            cachedText = "";
            dirty = false;
        }

        private String buildText() {
            StringBuilder builder = new StringBuilder();
            Token previous = null;

            for (Token token : tokens) {
                if (token == null || token.text == null || token.text.isEmpty()) {
                    previous = token;
                    continue;
                }

                if (token.type == TokenType.PUNCTUATION) {
                    trimTrailingSpaces(builder);
                    builder.append(token.text);
                } else {
                    if (builder.length() > 0 && shouldInsertSpace(builder, previous, token)) {
                        builder.append(' ');
                    }
                    builder.append(token.text);
                }

                previous = token;
            }

            trimTrailingSpaces(builder);
            return builder.toString();
        }
    }

    private static class Token {
        final TokenType type;
        final String text;

        Token(TokenType type, String text) {
            this.type = type;
            this.text = text;
        }
    }

    private enum TokenType {
        WORD,
        ENTITY,
        PUNCTUATION,
        OTHER
    }

    public void saveToFile() {
        if (transcriptRecords.isEmpty()) {
            Log.d(TAG, "No records to save");
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String timestamp = sdf.format(new Date());

            File dir = new File(context.getFilesDir(), "transcripts");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File file = new File(dir, "transcript_" + timestamp + ".txt");
            FileWriter writer = new FileWriter(file);

            writer.write("============================================================\n");
            writer.write("Speechmatics 粵語轉寫記錄\n");
            SimpleDateFormat fullSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            writer.write("生成時間: " + fullSdf.format(new Date()) + "\n");
            writer.write("============================================================\n\n");

            for (TranscriptResult record : transcriptRecords) {
                writer.write(record.toString() + "\n");
            }

            writer.close();
            Log.d(TAG, "Transcript saved to: " + file.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Error saving transcript", e);
        }
    }
}
