package org.thbz.hanguldrill;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends ActionBarActivity
    implements ConfigManageDialogFragment.NoticeDialogListener,
        InputConfigNameDialogFragment.NoticeDialogListener {
    private final static String TAG = MainActivity.class.getName();

    private TextView textView = null;
    private ProgressBar progressBar = null;
    private Button mainButton = null;

    // Gestion des états : stopped, running...
    private enum State { STOPPED, RUNNING, ASKSTOP }
    private State _state;

    // Implémentation d'un singleton pour obtenir une référence à l'activité
    // Idée : si l'activité est détruite et reconstruite, je suppose que onCreate() est
    // à nouveau appelé et que s_instance est donc recréée.
    // Donc il faut sans doute mieux appeler getInstance() chaque fois qu'on en a besoin
    // plutôt que de stocker une référence. Mais il est sans doute préférable de passer le
    // contexte en argument aux fonctions chaque fois que possible.
    static private MainActivity s_instance;
    synchronized static private MainActivity getInstance() {
        return s_instance; // La valeur devra être testée avant d'être utilisée
    }

    static Context doGetApplicationContext() {
        MainActivity main = getInstance();
        if(main == null)
            return null;
        else
            return main.getApplicationContext();
    }

    synchronized static void setInstance(MainActivity mainActivity) {
        s_instance = mainActivity;
    }

    // Accès  à l'état
    protected synchronized State getState() {
        // debug("getState = " + _state);
        return _state;
    }

    protected synchronized void setState(State state) {
        _state = state;
        if (_state == State.STOPPED) {
            updateMainButtonText(getString(R.string.main_button_start));
        }
        else if (_state == State.RUNNING)
            updateMainButtonText(getString(R.string.main_button_stop));
    }

/*
    private void getOverflowMenu() {
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if(menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
*/

    // Initialisations
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Empêcher des appels à this pendant qu'il n'est pas entièrement construit
        setInstance(null);

        setState(State.STOPPED);

        // Récupération ou création des préférences
        try {
            Settings.Configuration lastSelectedConfig =
                    Settings.ConfigManager.getLastSelectedConfig(this);
            if (lastSelectedConfig == null)
                resetConfigurations();
        } catch (Throwable thro) {
            if(BuildConfig.DEBUG)
                if(BuildConfig.DEBUG)
                    Log.e(TAG, "Throwable while loading the preferences", thro);
        }

        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.text);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(ProgressBar.INVISIBLE);
        mainButton = (Button) findViewById(R.id.mainButton);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setIcon(R.drawable.ic_launcher);

        updateTextSizeFromSettings();
        updateThemeFromSettings();

        setInstance(this);

        // Lance l'initialisation de la base
        // WordDbContract.ensureDatabaseInitialized(this);
        try {
            WordDbContract.DbHelper.setupDbIfNeeded(getApplicationContext());
        }
        catch(InternalException exc) {
            toastError(exc.getMessage());
        }

/*
        // Hack : see http://stackoverflow.com/questions/9739498/android-action-bar-not-showing-overflow
        getOverflowMenu();
*/
    }

    // Arrêt de l'animation en cas de pause de l'activité
    protected void onPause () {
        super.onPause();
        if(getState() == State.RUNNING)
           setState(State.ASKSTOP);
    }

    protected void onDestroy () {
        super.onDestroy();
        setInstance(null);
    }

    // Gestion des configurations
    private final int START_MENUITEM_ID = 1;

    // Initialisation du menu des configurations dans l'Option Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_activity_actions, menu);

        try {
            MenuItem configListItem = menu.findItem(R.id.action_configlist);
            String[] configIds = Settings.ConfigManager.getAllConfigIds(this);


            if (configListItem.hasSubMenu() && configIds.length > 0) {
                SubMenu configMenu = configListItem.getSubMenu();
                if (configMenu == null) {
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "configMenu = null");
                }
                else {
                    // On ajoute une entrée pour chacune des configurations existantes
                    for(int i=0; i < configIds.length; i++) {
                        String configId = configIds[i];
                        Settings.Configuration config =
                                Settings.ConfigManager.getConfigFromId(this, configId);
                        String name = config.getName(this);
                        configMenu.add(Menu.NONE, START_MENUITEM_ID + i, i, name);
                    }
                }

                // Set configMenu title
                try {
                    Settings.Configuration lastSelectedConfig =
                            Settings.ConfigManager.getLastSelectedConfig(this);
                    if(lastSelectedConfig != null) {
                        String configName = lastSelectedConfig.getName(this);
                        if(configName != null) {
                            if (configName.length() > 15)
                                configName = configName.substring(0, 13) + "\u2026";
                            configListItem.setTitle(configName);
                        }
                    }
                }
                catch(ClassCastException exc) {
                    toastError("Exception : " + exc.getMessage());
                }
            }
        }
        catch(InternalException exc) {
            if(BuildConfig.DEBUG)
                if(BuildConfig.DEBUG)
                    Log.e(TAG, "Erreur interne", exc);
        }

        return super.onCreateOptionsMenu(menu);
    }

    // Implémentation de InputConfigNameDialogFragment.NoticeDialogListener
    // Un nom de configuration a été entré dans un objet InputConfigNameDialogFragment
    // pour enregistrer les paramètres actuels.
    @Override
    public void onInputConfigName(String newConfigName) {
        try {
            String[] configIds = Settings.ConfigManager.getAllConfigIds(this);
            for(String configId: configIds) {
                Settings.Configuration config =
                        Settings.ConfigManager.getConfigFromId(this, configId);
                if(config.getName(this).equals(newConfigName)) {
                    alert("A configuration already exists with this name. Please choose another name.");
                    return;
                }
            }

            Settings.Configuration newConfig =
                    Settings.ConfigManager.saveSettingsAsConfig(this, newConfigName);
            if(newConfig != null) {
                // Redraw the menu if necessary
                if (Build.VERSION.SDK_INT >= 11)
                    invalidateOptionsMenu();

                // Message de confirmation
                toastMessage("Config saved as " + newConfigName);

                if(BuildConfig.DEBUG)
                    Log.d(TAG, "Config copiée vers " + newConfig.getId());
            }
        } catch (InternalException exc) {
            if(BuildConfig.DEBUG)
                Log.e(TAG, "Internal error", exc);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            int countConfigs = Settings.ConfigManager.getAllConfigIds(this).length;

            int itemId = item.getItemId();

            if(itemId == R.id.action_configure_savecurrentsettings) {
                InputConfigNameDialogFragment dialog = new InputConfigNameDialogFragment();
                dialog.setListener(this);
                dialog.show(getSupportFragmentManager(), "InputTextDialogFragment");
            }
            else if(itemId == R.id.action_configure_manageconfigs) {
                ConfigManageDialogFragment configManageDialog = new ConfigManageDialogFragment();
                configManageDialog.show(getSupportFragmentManager(), "configManage");
            }
            else if(itemId == R.id.action_configure_reinitconfigs) {
                ConfirmDialogFragment.Listener listener =
                        new ConfirmDialogFragment.Listener() {
                            @Override
                            public void onConfirmDialogYes() {
                                resetConfigurations();
                                synchronized(this) {
                                    if(getState() == State.RUNNING)
                                        setState(State.ASKSTOP);
                                }
                                toastMessage("Configurations have been reinitialized");
                            }
                            @Override
                            public void onConfirmDialogNo() {
                                toastMessage("Configurations have not been modified");
                            }
                        };

                ConfirmDialogFragment dialog = new ConfirmDialogFragment();
                dialog.setMessage("Do you really want to reinitialize the configurations?")
                        .setListener(listener)
                        .show(getSupportFragmentManager(), "ConfirmDialogFragment");
            }
            else if(itemId == R.id.action_configure_help) {
                DialogFragment helpDialog = new HelpDialogFragment();
                helpDialog.show(getSupportFragmentManager(), "helpDialog");
            }
/*
            else if(itemId == R.id.action_configure_resetdb) {
                ConfirmDialogFragment.Listener listener =
                        new ConfirmDialogFragment.Listener() {
                            @Override
                            public void onConfirmDialogYes() {
                                try {
                                    if (getState() == State.RUNNING) {
                                        MainActivity.doToastError("You must first press the Stop button");
                                        return;
                                    }
                                    WordDbContract.DbHelper.resetDb(MainActivity.this);
                                    toastMessage("Database has been reset.");
                                }
                                catch(InternalException exc) {
                                    toastError("Error: " + exc.getMessage());
                                }
                            }
                            @Override
                            public void onConfirmDialogNo() {
                                toastMessage("Cancelled.");
                            }
                        };

                ConfirmDialogFragment dialog = new ConfirmDialogFragment();
                dialog.setMessage("Do you really want to reset the database?")
                        .setListener(listener)
                        .show(getSupportFragmentManager(), "ConfirmDialogFragment");
            }
*/
            else if (itemId >= START_MENUITEM_ID && itemId < START_MENUITEM_ID + countConfigs) {
                // Select one of the existing configurations
                String[] configIds = Settings.ConfigManager.getAllConfigIds(this);
                String configId = configIds[itemId - START_MENUITEM_ID];

                Settings.Configuration config = Settings.ConfigManager.getConfigFromId(this, configId);
                config.saveAsSettings(this);

                if (Build.VERSION.SDK_INT >= 11)
                    invalidateOptionsMenu();

                if(BuildConfig.DEBUG)
                    Log.d(TAG, "selected " + configId);
                return true;
            }
            else if(itemId == R.id.action_settings) {
                // Bouton ouvrant le menu des paramètres
                synchronized(this) {
                    if (getState() == State.RUNNING)
                        setState(State.ASKSTOP);
                }
                openSettings();
                return true;
            }
        }
        catch(InternalException exc) {
            if(BuildConfig.DEBUG)
                Log.e(TAG, "Internal error", exc);
        }

        return super.onOptionsItemSelected(item);
    }

    public void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    protected void resetConfigurations() {
        try {
            Settings.ConfigManager.resetConfigurations(this);
            if (Build.VERSION.SDK_INT >= 11)
                invalidateOptionsMenu();
        }
        catch(InternalException exc) {
            toastError("Reset has failed: " + exc.getMessage());
            if(BuildConfig.DEBUG)
                Log.e(TAG, "Internal error", exc);
        }
    }

    // Utilitaires

    // A appeler si les settings ont été modifiés
    protected void updateTextSizeFromSettings() {
        String sizeSetting = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(Settings.Key.pref_textSize, "30");
        try {
            final float textSize = Float.parseFloat(sizeSetting);
            runOnUiThread(new Runnable() {
                public void run() {
                    textView.setTextSize(textSize);
                }
            });
        }
        catch(NumberFormatException exc) {
            MainActivity.doToastError(exc.getMessage());
        }
    }

    static synchronized void doUpdateTextSizeFromSettings() {
        MainActivity main = getInstance();
        if(main != null)
            main.updateTextSizeFromSettings();
    }

    protected void updateThemeFromSettings() {
        String themeName = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(Settings.Key.pref_theme, "black/white");
        final Settings.Theme theme = Settings.Theme.get(themeName);

        runOnUiThread(new Runnable() {
            public void run() {
                textView.setTextColor(theme.textColor);
                View mainView = findViewById(R.id.main_layout);
                mainView.setBackgroundColor(theme.backgroundColor);
            }
        });
    }

    static synchronized void doUpdateThemeFromSettings() {
        MainActivity main = getInstance();
        if(main != null)
            main.updateThemeFromSettings();
    }

    // Nécessaire pour modifier des éléments de l'UI depuis un autre thread
    public void updateText(final String text) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (textView != null)
                    textView.setText(text);
            }
        });
    }

    private void updateMainButtonText(final String text) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (mainButton != null)
                    mainButton .setText(text);
            }
        });
    }

    // Affiche une alerte (qui requiert l'attention de l'utilisateur, par opposition
    // au toastMessage, qui peut être ignoré
    protected void alert(final String text) {
        new AlertDialog.Builder(this)
                .setTitle("Warning")
                .setMessage(text)
                .setPositiveButton(android.R.string.yes, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    // Affiche un message qui disparaît au bout de quelques secondes
    // (par exemple pour confirmer qu'une action a été effectuée)
    private void toastMessage(final String text) {
        runOnUiThread(new Runnable() {
            public void run() {
                Context context = getApplicationContext();
                Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    static synchronized public void doToastMessage(final String text) {
        MainActivity main = getInstance();
        if(main != null)
            main.toastMessage(text);
    }

    // Affiche un message qui disparaît au bout de quelques secondes
    // (par exemple pour confirmer qu'une action a été effectuée)
    private void toastError(final String text) {
        runOnUiThread(new Runnable() {
            public void run() {
                Context context = getApplicationContext();
                Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        });
    }

    static synchronized public void doToastError(final String text) {
        MainActivity main = getInstance();
        if(main != null)
            main.toastError(text);
    }

    static synchronized public AssetManager doGetAssets() throws InternalException{
        MainActivity main = getInstance();
        if(main != null)
            return main.getAssets();
        else
            throw new InternalException("Main acitvity not initialized");
    }

    // Gestion du flux temporel de l'interface utilisateur : clics, progress bar, affichage des
    // nombres et mots...
    private void updateProgressBar(final int value) {
        runOnUiThread(new Runnable() {
            public void run() {
                if(progressBar != null) {
                    progressBar.setProgress(value);

                    // Apparition de la progress bar seulement vers la fin
                    if(value >= 75) {
                        if(progressBar.getVisibility() == ProgressBar.INVISIBLE) {
                            // Set visibility
                            progressBar.setVisibility(ProgressBar.VISIBLE);

                            // Animation
                            Animation anim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.progressbar_appear);
                            progressBar.startAnimation(anim);
                        }
                    }
                    else if(progressBar.getVisibility() == ProgressBar.VISIBLE)
                        progressBar.setVisibility(ProgressBar.INVISIBLE);
                }
            }
        });
    }

    private int totalDelay = 0;

    private class GetRandResult {
        final List<String> words;
        final int nbSyllables;
        final List<Integer> digits;

        GetRandResult(List<String> _words, int _nbSyllables, List<Integer> _digits) {
            words = _words;
            nbSyllables = _nbSyllables;
            digits = _digits;
        }
    }

    private GetRandResult getRand(int nbItems, int nbDigits)
        throws InternalException {
        try {
            List<String> words = new ArrayList<>();
            int nbSyllables = 0;

            for (int i = 0; i < nbItems; i++) {
                String word = WordDbContract.getWord(this);
                words.add(word);
                nbSyllables += word.length();
            }
            List<Integer> digits = new ArrayList<>();
            for (int i = 0; i < nbDigits; i++)
                digits.add((int)(Math.random() * 10));

            return new GetRandResult(words, nbSyllables, digits);
        }
        catch(RuntimeException exc) {
            if(BuildConfig.DEBUG)
                Log.e(TAG, "RuntimeException error", exc);
            throw exc;
        }
    }

    private void doPrintOutput() throws InternalException {
        int nbWords = Integer.valueOf(Settings.getCurrentSetting(this, Settings.Key.pref_nbWords,
                getString(R.string.pref_default_nbWords)));
        int nbDigits = Integer.valueOf(Settings.getCurrentSetting(this, Settings.Key.pref_nbDigits,
                getString(R.string.pref_default_nbDigits)));
        int speed = Integer.valueOf(Settings.getCurrentSetting(this, Settings.Key.pref_speed,
                getString(R.string.pref_default_speed)));

        int delaySyllable = (int)Math.floor(2000 / speed);
        int delayDigit = (int)Math.floor(2500 / speed);

        GetRandResult res = getRand(nbWords, nbDigits);

        String chaine = "";
        for (int i = 0; i < res.words.size(); i++) {
            if (i > 0)
                chaine += " ";
            chaine += res.words.get(i);
        }

        if (res.words.size() > 0 && res.digits.size() > 0)
            chaine += System.getProperty("line.separator");

        int nbD = res.digits.size();
        for (int i = 0; i < nbD; i++) {
            if (i % 8 == 0 && i > 0)
                chaine += System.getProperty("line.separator");
            else if(i < (nbD - (nbD % 8))) {
                if (i % 4 == 0 && i > 0)
                    chaine += " ";
            }
            else if((nbD - i) % 4 == 0 && i > 0)
                chaine += " ";
            chaine += res.digits.get(i);
        }

        totalDelay = res.nbSyllables * delaySyllable + res.digits.size() * delayDigit;

        // Test
        // chaine = WordDbContract.getTestData(this);
        updateText(chaine);
    }

    private void waitForDelay() {
        updateProgressBar(0);
        try {
            float delayDone = 0;
            int unit = 25;
            while (getState() == State.RUNNING && delayDone < totalDelay) {
                Thread.sleep(unit);
                delayDone += unit;
                updateProgressBar((int) Math.floor(100 * delayDone / totalDelay));
            }
        } catch (InterruptedException e) {
            if(BuildConfig.DEBUG)
                Log.e(TAG, "ProgressBar thread interrupted", e);
        }
    }

    Thread runningThread = null;

    public void onClickMainButton(View v) {
        final State state = getState();
        if(state == State.STOPPED) {
            runningThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        // WordDbContract.DbHelper.setupDbIfNeeded(getApplicationContext());

                        while (getState() == State.RUNNING) {
                            doPrintOutput();
                            waitForDelay();
                        }
                        // On a été stoppé
                        if(getState() == State.ASKSTOP)
                            setState(State.STOPPED);
                    }
                    catch(Exception exc) {
                        if(BuildConfig.DEBUG)
                            Log.e(TAG, "Erreur dans boucle du main button", exc);
                        toastError("Error: " + exc.getMessage());
                        setState(State.STOPPED);
                    }
                }
            });
            setState(State.RUNNING);
            runningThread.start();
        }
        else if(state == State.RUNNING) {
            setState(State.ASKSTOP);
            try {
                if(runningThread == null)
                    throw new Exception("Error : no current action to stop!");
                runningThread.join();
            } catch (InterruptedException ie) {
                if(BuildConfig.DEBUG)
                    Log.e(TAG, "Main thread interrupted", ie);
            } catch(Exception e) {
                if(BuildConfig.DEBUG)
                    Log.e(TAG, "Exception", e);
            }
        }
        // else state = ASKSTOP (double clic ?) : on ne fait rien
    }

    // Implementation de ConfigManageDialogFragment.NoticeDialogListener : destruction de
    // configurations
    @Override
    public void onConfigManageSaveClick(Set<Integer> configToDeleteIds) {
        try {
            boolean lastSelectedConfigIsDestroyed = false;

            Settings.Configuration lastSelectedConfig = Settings.ConfigManager.getLastSelectedConfig(this);
            String lastSelectedConfigId = (lastSelectedConfig == null ? null
                    : lastSelectedConfig.getId());

            List<String> allConfigIds = Settings.ConfigManager.getAllConfigIdsAsList(this);
            List<String> newAllConfigIds = new ArrayList<>();
            int nbDone = 0;
            for(int i = 0; i < allConfigIds.size(); i++) {
                String configId = allConfigIds.get(i);
                if(configToDeleteIds.contains(i)) {
                    Settings.ConfigManager.deleteConfigFromId(this, configId);
                    nbDone++;

                    if(lastSelectedConfigId != null && configId.equals(lastSelectedConfigId))
                        lastSelectedConfigIsDestroyed = true;
                }
                else
                    newAllConfigIds.add(configId);
            }

            // Enregistrer la liste des ids de configuration
            Settings.ConfigManager.setAllConfigIds(this, newAllConfigIds);

            // Si la configuration courante a été détruite, on sélectionne
            // la première configuration de la liste
            if(lastSelectedConfigIsDestroyed) {
                if(newAllConfigIds.size() >= 1) {
                    String firstConfigId = newAllConfigIds.get(0);
                    Settings.Configuration config =
                            Settings.ConfigManager.getConfigFromId(this, firstConfigId);
                    config.saveAsSettings(this);
                }
            }

            if (Build.VERSION.SDK_INT >= 11)
                invalidateOptionsMenu();

            toastMessage("Deleted " + nbDone + " configuration" + (nbDone > 1 ? "s" : ""));
        }
        catch(InternalException exc) {
            toastError("Erreur : " + exc.getMessage());
        }
    }

    public void onConfigManageException(InternalException exc) {
        toastError(exc.getMessage());
    }
}
