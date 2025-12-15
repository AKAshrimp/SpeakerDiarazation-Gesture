package hk.edu.hkmu.speakerdiarazationdemo.models;

public class TranscriptResult {
    private String speaker;
    private String text;
    private String timestamp;
    private boolean isPartial;

    public TranscriptResult(String speaker, String text, String timestamp, boolean isPartial) {
        this.speaker = speaker;
        this.text = text;
        this.timestamp = timestamp;
        this.isPartial = isPartial;
    }

    public String getSpeaker() {
        return speaker;
    }

    public String getText() {
        return text;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public boolean isPartial() {
        return isPartial;
    }

    @Override
    public String toString() {
        return "[" + timestamp + "] [" + speaker + "] " + text;
    }
}
