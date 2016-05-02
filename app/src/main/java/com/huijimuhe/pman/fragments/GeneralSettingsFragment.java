package com.huijimuhe.pman.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import com.huijimuhe.pman.R;
import com.huijimuhe.pman.activities.WebViewActivity;

public class GeneralSettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.general_preferences);
        setPrefListeners();
    }

    private void setPrefListeners() {

        Preference excludeWordsPref = findPreference("pref_watch_exclude_words");
        String summary = getResources().getString(R.string.pref_watch_exclude_words_summary);
        String value = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("pref_watch_exclude_words", "");
        if (value.length() > 0) excludeWordsPref.setSummary(summary + ":" + value);

        excludeWordsPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                String summary = getResources().getString(R.string.pref_watch_exclude_words_summary);
                if (o != null && o.toString().length() > 0) {
                    preference.setSummary(summary + ":" + o.toString());
                } else {
                    preference.setSummary(summary);
                }
                return true;
            }
        });
    }
}
