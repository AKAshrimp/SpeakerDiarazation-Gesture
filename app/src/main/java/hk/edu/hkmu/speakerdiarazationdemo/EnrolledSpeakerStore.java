package hk.edu.hkmu.speakerdiarazationdemo;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import hk.edu.hkmu.speakerdiarazationdemo.models.EnrolledSpeaker;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class EnrolledSpeakerStore {
    private static final String TAG = "EnrolledSpeakerStore";
    private static final String PREFS_NAME = "enrolled_speakers_store";
    private static final String KEY_SPEAKERS_JSON = "speakers_json";

    private static final Gson gson = new Gson();
    private static final Type listType = new TypeToken<List<EnrolledSpeaker>>() {}.getType();

    private EnrolledSpeakerStore() {
        // Utility class
    }

    public static List<EnrolledSpeaker> getSpeakers(Context context) {
        SharedPreferences prefs = getPrefs(context);
        String json = prefs.getString(KEY_SPEAKERS_JSON, null);
        if (TextUtils.isEmpty(json)) {
            return new ArrayList<>();
        }
        try {
            List<EnrolledSpeaker> speakers = gson.fromJson(json, listType);
            return speakers != null ? speakers : new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse enrolled speakers JSON", e);
            return new ArrayList<>();
        }
    }

    public static void saveSpeakers(Context context, List<EnrolledSpeaker> speakers) {
        SharedPreferences prefs = getPrefs(context);
        String json = gson.toJson(speakers != null ? speakers : Collections.emptyList(), listType);
        prefs.edit().putString(KEY_SPEAKERS_JSON, json).apply();
    }

    public static void addOrReplace(Context context, EnrolledSpeaker speaker) {
        if (speaker == null) {
            return;
        }
        List<EnrolledSpeaker> speakers = getSpeakers(context);
        boolean replaced = false;
        for (int i = 0; i < speakers.size(); i++) {
            if (speakers.get(i).getName().equalsIgnoreCase(speaker.getName())) {
                speakers.set(i, speaker);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            speakers.add(speaker);
        }
        saveSpeakers(context, speakers);
    }

    public static void remove(Context context, String name) {
        if (TextUtils.isEmpty(name)) {
            return;
        }
        List<EnrolledSpeaker> speakers = getSpeakers(context);
        Iterator<EnrolledSpeaker> iterator = speakers.iterator();
        while (iterator.hasNext()) {
            EnrolledSpeaker speaker = iterator.next();
            if (name.equalsIgnoreCase(speaker.getName())) {
                iterator.remove();
            }
        }
        saveSpeakers(context, speakers);
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
