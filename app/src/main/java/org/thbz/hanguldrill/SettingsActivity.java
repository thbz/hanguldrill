package org.thbz.hanguldrill;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.util.Log;

import java.util.List;

public class SettingsActivity extends PreferenceActivity {
    private static final String TAG = SettingsActivity.class.getName();

    SettingsFragment settingsFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // cf. http://stackoverflow.com/questions/6822319/what-to-use-instead-of-addpreferencesfromresource-in-a-preferenceactivity
        // addPreferencesFromResource(R.xml.preferences);
        settingsFragment = new SettingsFragment();
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                settingsFragment).commit();
    }

    private static class PreferenceChangeListener implements Preference.OnPreferenceChangeListener {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String key = preference.getKey();

            try {
                Settings.checkValidSetting(key, (String) newValue);

                Context context = MainActivity.doGetApplicationContext();

                if((key.equals(Settings.Key.pref_nbWords)
                        && (newValue + Settings.getCurrentSetting(context, Settings.Key.pref_nbDigits, "0"))
                            .equals("00"))
                    || (key.equals(Settings.Key.pref_nbDigits)
                        && (newValue + Settings.getCurrentSetting(context, Settings.Key.pref_nbWords, "0"))
                            .equals("00")))
                    throw new InternalException("You cannot set the number of words and the number of digits both equal to zero");

                return true; // validation
            }
            catch(ClassCastException exc) {
                if(BuildConfig.DEBUG)
                    Log.e(TAG, "ClassCastException", exc);
            }
            catch(InternalException exc) {
                MainActivity.doToastError(exc.getMessage());
            }

            return false;
        }
    }

    public static class SettingsFragment extends PreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            SharedPreferences sharedPref = getPreferenceScreen().getSharedPreferences();
            List<String> keys = Settings.getUserSettingKeys();
            for(String key: keys) {
                Preference connectionPref = findPreference(key);
                if(connectionPref != null) {
                    // Set summary to be the user-description for the selected value
                    String value = sharedPref.getString(key, "");
                    connectionPref.setSummary(prefValueToSummary(key, value));
                }
            }

            try {
                // Seulement pour les préférences pour lesquelles l'utilisateur risque d'entrer
                // une valeur interdite.
                String[] prefIds = new String[] {
                        Settings.Key.pref_nbWords,
                        Settings.Key.pref_nbDigits};
                for(String id: prefIds) {
                    Preference pref = findPreference(id);
                    if(pref != null)
                        pref.setOnPreferenceChangeListener(new PreferenceChangeListener());
                }
            }
            catch(ClassCastException exc) {
                Log.e(TAG, "Class error when retrieving the context", exc);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            SharedPreferences sharedPref = getPreferenceScreen().getSharedPreferences();
            sharedPref.registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        // Computes the summary from a preference value
        static String prefValueToSummary(String key, String value) {
            Context context = MainActivity.doGetApplicationContext();
            if(context == null)
                return value; // The context is not initialized yet?
            else {
                String res;
                switch(key) {
                    case Settings.Key.pref_speed:
                        res = getLabelForValue(context, R.array.pref_speed_entries,
                                R.array.pref_speed_values, value);
                        break;
                    case Settings.Key.pref_textSize:
                        res = getLabelForValue(context, R.array.pref_textSize_entries,
                                R.array.pref_textSize_values, value);
                        break;
                    case Settings.Key.pref_theme:
                        res = getLabelForValue(context, R.array.pref_theme_entries,
                                R.array.pref_theme_values, value);
                        break;
                    default:
                        res = value;
                }
                return res;
            }
        }

        static String getLabelForValue(Context context, int labelArrayResourceId,
                                            int valuesArrayResourceId, String value) {
            String label;
            try {
                Resources resources = context.getResources();
                if(resources == null)
                    return value; // Maybe the context needs to be initialized???
                String[] sVals = resources.getStringArray(valuesArrayResourceId);
                int idx;
                for(idx=0; idx < sVals.length; idx++) {
                    if(sVals[idx].equals(value))
                        break;
                }
                if(idx >= sVals.length)
                    label = value; // Cela peut arriver avec un changement de version
                else {
                    String[] labels = resources.getStringArray(labelArrayResourceId);
                    if(idx > labels.length - 1)
                        throw new AssertionError("pref_speed_entries is too short");
                    label = labels[idx];
                }
            }
            catch(Resources.NotFoundException exc) {
                label = value;
            }
            catch(NumberFormatException exc) {
                label = value;
            }
            return label;
        }

        // Appelé lorsqu'un setting est modifié
        public void onSharedPreferenceChanged(SharedPreferences sharedPref,
                                              String key) {
            if (Settings.isUserSettingKey(key)) {
                Preference connectionPref = findPreference(key);
                // Set summary to be the user-description for the selected value
                String value = sharedPref.getString(key, "");
                connectionPref.setSummary(prefValueToSummary(key, value));

                if(key.equals(Settings.Key.pref_textSize)) {
                    MainActivity.doUpdateTextSizeFromSettings();
                }
                else if(key.equals(Settings.Key.pref_theme)) {
                    MainActivity.doUpdateThemeFromSettings();
                }
            }
        }
    }
}
