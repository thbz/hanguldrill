/*
 * Copyright (c) 2015 Thierry Bézecourt
 *
 * This file is part of HangulDrill.
 *
 * HangulDrill is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HangulDrill is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with HangulDrill.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.thbz.hanguldrill;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestion des settings et configurations.
 * Created by Thierry on 16/12/14.
 */

/*
 *
 * Settings are everything that is stored in the default SharedPreferences.
 * Configurations are everything that can be copied in non-default SharedPreferences.
 * A Configuration object is associated to each non-default SharedPreferences object and to
 * each entry in the Config menu.
 *
 * Toutes les fonctions relatives aux configurations sont dans les sous-classes ConfigManager ou
 * Configuration. Settings ne doit donc comprendre donc que des fonctions utiles pour l'ensemble des Settings,
 * qu'ils soient ou pas susceptibles d'être enregistrés dans une configuration.
 */
public class Settings {
    public static final String TAG = Settings.class.getName();

    // Cette classe contient juste des utilitaires
    private Settings() {
        throw new AssertionError("This class (" + TAG + ") must be instantiated");
    }

    static SharedPreferences getSharedPreferences(Context context, String configId)
            throws InternalException {
        return context.getSharedPreferences(configId, Activity.MODE_PRIVATE);
    }

    // Returns the SharedPreferences that contain the current settings
    static SharedPreferences getSettingsSharedPreferences(Context context)
            throws InternalException {
        SharedPreferences settingsSharedPref =
                PreferenceManager.getDefaultSharedPreferences(context);
        if (settingsSharedPref == null)
            throw new InternalException("Missing preferences");
        return settingsSharedPref;
    }

    // Récupère quelque chose dans les default shared preferences (c'est-à-dire les current
    // settings)
    static String getCurrentSetting(Context context,  String propName, String defaultValue)
            throws InternalException {
        SharedPreferences settingsSharedPref = getSettingsSharedPreferences(context);
        return settingsSharedPref.getString(propName, defaultValue);
    }

    public static class Key {
        // Settings that can be modified by the user : doit être cohérent avec preferences.xml
        public static final String pref_name = "pref_name";
        public static final String pref_nbWords = "pref_nbWords";
        public static final String pref_nbDigits = "pref_nbDigits";
        public static final String pref_speed = "pref_speed";

        // User settings that will not be stored in configurations
        public static final String pref_textSize = "pref_textSize";
        public static final String pref_theme = "pref_theme";

        // Internal parameters
        public static final String lastselected_config_id = "lastselected_config_id";
        public static final String all_config_ids = "all_config_ids";
    }

    static private final List<String> configSettingsKeys =
            new ArrayList<>(Arrays.asList(new String[] {
                    Key.pref_name,
                    Key.pref_nbWords,
                    Key.pref_nbDigits,
                    Key.pref_speed
            }));

    // Renvoie la liste des paramètres qui peuvent être stockés dans une configuration
    public static List<String> getConfigSettingsKeys() {
        return configSettingsKeys;
    }

    static private final List<String> userSettingsKeys =
            new ArrayList<>(Arrays.asList(new String[] {
                    Key.pref_name,
                    Key.pref_nbWords,
                    Key.pref_nbDigits,
                    Key.pref_speed,
                    Key.pref_textSize,
                    Key.pref_theme
            }));

    public static List<String> getUserSettingKeys() {
        return userSettingsKeys;
    }

    public static boolean isUserSettingKey(String key) {
        List<String> keys = getUserSettingKeys();
        for(String k: keys) {
            if(k.equals(key))
                return true;
        }
        return false;
    }

    public static class InvalidSettingException extends InternalException {
        InvalidSettingException(String msg) {
            super(msg);
        }
    }

    public static void checkValidSetting(String key, String value)
            throws InvalidSettingException {
        if(key.equals(Key.pref_nbWords)/* || key.equals(Key.todel_pref_delaySyllable)*/
                || key.equals(Key.pref_nbDigits) /*|| key.equals(Key.todel_pref_delayDigit)*/) {
            try {
                Integer intValue = Integer.parseInt(value);
                if(intValue < 0)
                    throw new InvalidSettingException("Invalid value! You cannot enter a negative value.");
            }
            catch(NumberFormatException exc) {
                throw new InvalidSettingException("Invalid value! You should enter an integer value.");
            }
        }
        // Other settings are chosen in a list: a wrong value cannot be entered.
    }

/*
    public static class TextSize {
        public final String name;
        public final float size;

        public TextSize(String _name, float _size) {
            name = _name;
            size = _size;
        }

        // Les noms doivent être cohérents avec strings.xml et arrays.xml
*/
/*
        public static TextSize SMALL = new TextSize("Small", 20);
        public static TextSize NORMAL = new TextSize("Normal", 30);
        public static TextSize LARGE = new TextSize("Large", 40);
        public static TextSize HUGE = new TextSize("Huge", 50);
*//*


*/
/*
        public static float get(String setting) {
            TextSize[] textSizes = new TextSize[] { SMALL, NORMAL, LARGE, HUGE };
            for(TextSize ts: textSizes) {
                if(ts.name.equals(setting))
                    return ts.size;
            }
            return NORMAL.size; // par défaut
        }
*//*

    }
*/

    public static class Theme {
        public final int textColor;
        public final int backgroundColor;

        public Theme (int _textColor, int _backgroundColor) {
            textColor = _textColor;
            backgroundColor = _backgroundColor;
        }

        public static Theme DEFAULT =  new Theme (Color.WHITE, Color.BLACK);

        public static Theme get(String colorDesc) {
            String[] names = colorDesc.split("/");
            Theme res;
            if(names.length == 2) {
                try {
                    int textColor = Color.parseColor(names[0]);
                    int bgColor = Color.parseColor(names[1]);
                    res = new Theme(textColor, bgColor);
                }
                catch(IllegalArgumentException exc) {
                    // Peut arriver lors d'un changement de format (version nouvelle)
                    res = DEFAULT;
                }
            }
            else
                res = DEFAULT;
            return res;
        }
    }

    // Description of a configuration (used for initialization)
    private static class ConfigDesc {
        private Map<String, String> values;

        private ConfigDesc(String _name, String _nbWords, String _nbDigits, String _speed) {
            values = new HashMap<>();

            values.put(Key.pref_name, _name);
            values.put(Key.pref_nbWords, "" + _nbWords);
            values.put(Key.pref_nbDigits, "" + _nbDigits);
            values.put(Key.pref_speed, "" + _speed);

            if(BuildConfig.DEBUG
                    && values.size() != Settings.getConfigSettingsKeys().size())
                throw new AssertionError("Error when building a ConfigDesc: "
                        + values.size() + " != " + Settings.getConfigSettingsKeys().size());
        }

        protected String get(String prefId) throws InternalException {
            if(! values.containsKey(prefId))
                throw new InternalException("ConfigDesc does not contain " + prefId);

            return values.get(prefId);
        }

        protected void copyToPreferences(SharedPreferences sharedPreferences)
                throws InternalException {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            List<String> configSettingsKeys = getConfigSettingsKeys();
            for(String prefId: configSettingsKeys)
                editor.putString(prefId, get(prefId));
            editor.apply();
        }
    }

    protected static class Configuration {
        // Représentation interne
        private String _id;

        protected String getId() {
            return _id;
        }

        protected SharedPreferences getSharedPreferences(Context context) throws InternalException {
            return Settings.getSharedPreferences(context, getId());
        }

        public Configuration(String id) {
            this._id = id;
        }

        public String getName(Context context) throws InternalException {
            try {
                return getSharedPreferences(context).getString(Key.pref_name, null);
            }
            catch(ClassCastException exc) {
                Log.e(TAG, "ClassCastException", exc);
                throw new InternalException(exc);
            }
        }

        public void setName(Context context, String value) throws InternalException {
            getSharedPreferences(context).edit()
                    .putString(Key.pref_name, value)
                    .apply();
        }

        // Sets a give configuration as the current settings
        protected void saveAsSettings(Context context)
                throws InternalException {
            SharedPreferences settingsSharedPref = getSettingsSharedPreferences(context);

            String configId = getId();
            SharedPreferences configSharedPref = getSharedPreferences(context);

            ConfigManager.copyConfigBetweenPreferences(configSharedPref, settingsSharedPref);

            String configName = getName(context);

            settingsSharedPref.edit()
                    .putString(Key.lastselected_config_id, configId)
                    .putString(Key.pref_name, configName)
                    .apply();
        }

        private void delete(Context context) throws InternalException {
            String configId = getId();
            SharedPreferences configSharedPref = Settings.getSharedPreferences(context, configId);
            if (configSharedPref == null)
                throw new InternalException("Error while getting shared preferences for config "
                        + configId);
            configSharedPref.edit()
                    .clear()
                    .apply();
        }
    }

    // Méta-fonctions relatives aux configurations
    static class ConfigManager {
        static private void copyConfigBetweenPreferences(SharedPreferences fromPref,
                                                           SharedPreferences toPref)
                throws InternalException {
            List<String> keys = getConfigSettingsKeys();
            SharedPreferences.Editor editor = toPref.edit();

            for(String key : keys) {
                if(toPref.contains(key))
                    editor.remove(key);

                String value;
                try {
                    value = fromPref.getString(key, null);
                    if(value != null)
                        editor.putString(key, value);
                }
                catch(ClassCastException exc) {
                    throw new InternalException(exc);
                }
            }
            editor.apply();
        }

        static String idxToId(int idx) {
            return "config-" + idx;
        }

        static private List<ConfigDesc> getDefaultConfigurationDescs(Context context) throws InternalException {
            try {
                String[] builtinConfigDescs =
                        context.getResources().getStringArray(R.array.builtin_configurations);
                List<ConfigDesc> res = new ArrayList<>();
                for(String desc: builtinConfigDescs) {
                    String[] elems = desc.split("/");
                    if(elems.length != 4)
                        throw new InternalException("The XML configuration description is incorrect: "
                                + desc);
                    res.add(new ConfigDesc(elems[0], elems[1], elems[2], elems[3]));
                }
                return res;
            }
            catch(NullPointerException  exc) {
                throw new InternalException(exc);
            }
        }

        static protected void resetConfigurations(Context context) throws InternalException {
            String[] oldConfigIds = getAllConfigIds(context);
            for (String configId : oldConfigIds) {
                deleteConfigFromId(context, configId);
            }

            // Initialize the config list (normally when the app is installed for the
            // first time)
            List<ConfigDesc> configDescs = getDefaultConfigurationDescs(context);

            // Copy each pre-defined configuration name to a Shared Preference
            List<String> configIds = new ArrayList<>();
            for (int i = 0; i < configDescs.size(); i++) {
                // Création de l'identifiant pour cette configuration
                String configId = ConfigManager.idxToId(i);

                // Création d'un SharedPreferences pour stocker cette configuration
                SharedPreferences sharedPref = getSharedPreferences(context, configId);
                configDescs.get(i).copyToPreferences(sharedPref);

                configIds.add(configId);
            }

            // Store the new config id list in the default shared preferences
            setAllConfigIds(context, configIds);

            // Set the first configuration as the current configuration (i.e default shared
            // preferences)
            String configId = idxToId(0);
            Configuration firstConfig = getConfigFromId(context, configId);
            firstConfig.saveAsSettings(context);

        }

        static void setAllConfigIds(Context context, List<String> configIds) throws InternalException {
            SharedPreferences settingsSharedPref = getSettingsSharedPreferences(context);

            StringBuilder sb = new StringBuilder("");
            boolean first = true;
            for (String item : configIds) {
                if(first)
                    first = false;
                else
                    sb.append(",");
                sb.append(item);
            }

            settingsSharedPref.edit()
                    .putString(Key.all_config_ids, sb.toString())
                    .apply();
        }

        static String[] getAllConfigIds(Context context) throws InternalException {
            String configIds = getCurrentSetting(context, Key.all_config_ids, null);

            String[] res;
            if(configIds == null || configIds.length() == 0)
                res = new String[]{};
            else
                res = configIds.split(",");

            return res;
        }

        // Renvoit un ArrayList pour être sûr que la liste est modifiable
        static ArrayList<String> getAllConfigIdsAsList(Context context) throws InternalException {
            String[] allConfigIds = getAllConfigIds(context);
            return new ArrayList<>(Arrays.asList(allConfigIds));
        }

        // Return a configuration from its id
        static Configuration getConfigFromId(Context context, String configId) throws InternalException {
            SharedPreferences sharedPref = getSharedPreferences(context, configId);

            if(sharedPref == null)
                return null;

            return new Configuration(configId);
        }

        // Return the last selected configuration
        static Configuration getLastSelectedConfig(Context context)
                throws InternalException {
            String lastSelectedConfigId = getCurrentSetting(context, Key.lastselected_config_id, null);
            if(lastSelectedConfigId == null)
                return null;

            return getConfigFromId(context, lastSelectedConfigId);
        }

        // Save current settings in a new configuration
        static Configuration saveSettingsAsConfig(Context context, String newConfigName)
                throws InternalException {
            ArrayList<String> configIds = getAllConfigIdsAsList(context);

            // Compute the new config id
            int countConfigs = configIds.size();
            String newConfigId = idxToId(countConfigs);

            // Create the shared pref for the new configuration
            SharedPreferences currentSettingsSharedPref = getSettingsSharedPreferences(context);
            SharedPreferences newConfigSharedPref = getSharedPreferences(context, newConfigId);

            copyConfigBetweenPreferences(currentSettingsSharedPref, newConfigSharedPref);

            // Set the name in the new configuration (and the underlying shared pref)
            Configuration newConfig = new Configuration(newConfigId);
            newConfig.setName(context, newConfigName);

            // Save the list of config ids
            configIds.add(newConfigId);

            setAllConfigIds(context, configIds);

            // Save the id and name of the last selected configuration
            currentSettingsSharedPref.edit()
                    .putString(Key.lastselected_config_id, newConfigId)
                    .putString(Key.pref_name, newConfigName)
                    .apply();

            return newConfig;
        }

        static protected void deleteConfigFromId(Context context, String configId)
                throws InternalException {
            Configuration config = getConfigFromId(context, configId);
            config.delete(context);
        }
    }
}
