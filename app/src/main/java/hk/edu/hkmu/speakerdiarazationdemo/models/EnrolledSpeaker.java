package hk.edu.hkmu.speakerdiarazationdemo.models;

import java.util.ArrayList;
import java.util.List;

public class EnrolledSpeaker {
    private String name;
    private String language;
    private List<String> speakerIdentifiers;
    private String audioPath;
    private long createdAt;
    private long updatedAt;
    private boolean muted;

    public EnrolledSpeaker() {
        // Default constructor for Gson
    }

    public EnrolledSpeaker(String name,
                           String language,
                           List<String> speakerIdentifiers,
                           String audioPath,
                           long createdAt,
                           long updatedAt,
                           boolean muted) {
        this.name = name;
        this.language = language;
        this.speakerIdentifiers = speakerIdentifiers != null
                ? new ArrayList<>(speakerIdentifiers)
                : new ArrayList<>();
        this.audioPath = audioPath;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.muted = muted;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<String> getSpeakerIdentifiers() {
        if (speakerIdentifiers == null) {
            speakerIdentifiers = new ArrayList<>();
        }
        return speakerIdentifiers;
    }

    public void setSpeakerIdentifiers(List<String> speakerIdentifiers) {
        this.speakerIdentifiers = speakerIdentifiers;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }
}
