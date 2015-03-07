package org.thbz.hanguldrill;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.provider.BaseColumns;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Créé by Thierry on 20/02/15.
 */
public class WordDbContract {
   private WordDbContract() {
       throw new AssertionError();
   }

    /* Inner class that defines the table contents */
    public static abstract class WordTableDesc implements BaseColumns {
        public static final String TABLE_NAME = "words";
        public static final String COLUMN_NAME_WORD = "w";
        public static final String COLUMN_NAME_CUMUL = "c";

        public static final String SQL_CREATE = "CREATE TABLE " + TABLE_NAME
                + " (" + WordTableDesc.COLUMN_NAME_CUMUL +  " INTEGER PRIMARY KEY, "
                + WordTableDesc.COLUMN_NAME_WORD + " TEXT); ";
    }

    public static abstract class InfoTableDesc implements BaseColumns {
        public static final String TABLE_NAME = "info";
        public static final String COLUMN_NAME_INITSTATE= "initstate";

        public static final String SQL_CREATE = "CREATE TABLE " + TABLE_NAME
                + " (" + InfoTableDesc.COLUMN_NAME_INITSTATE +  " BOOLEAN);";
    }

    final public static class DbHelper extends SQLiteOpenHelper {
        final static private String TAG = DbHelper.class.getName();

        // If you change the database schema, you must increment the database version.
        public static final int DATABASE_VERSION = 31;
        public static final String DATABASE_NAME = "words.db";

        // Database as an instance field to make life management easier
        private SQLiteDatabase _db = null;
        synchronized protected SQLiteDatabase getDb() {
            if(_db == null)
                _db = getWritableDatabase();
            return _db;
        }

        public static void setupDbIfNeeded(Context context) throws InternalException {
            DbHelper mDbHelper = DbHelper.getInstance(context);

            // Déclenche la création de la base si nécessaire (via onCreate)
            SQLiteDatabase db = mDbHelper.getDb();

            // Déclenche le remplissage de la base si ce n'est pas déjà fait
            mDbHelper.launchFillIfNeeded(db);
        }

        // Singleton
        static private DbHelper _instance = null;
        static synchronized DbHelper getInstance(Context context) {
            if(_instance == null)
                _instance = new DbHelper(context.getApplicationContext());
            return _instance;
        }

        private DbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        static private String[] dbTableNames = new String[] {
                InfoTableDesc.TABLE_NAME,
                WordTableDesc.TABLE_NAME
        };

        private void recreateDatabase(SQLiteDatabase db) throws InternalException {
            dropTables(db);
            onCreate(db);
            checkInitState = INITSTATE_TOBECHECKED;
        }

        static private void createTables(SQLiteDatabase db) throws InternalException {
            try {
                db.execSQL(InfoTableDesc.SQL_CREATE);
                db.execSQL(WordTableDesc.SQL_CREATE);
            }
            catch(Exception exc) {
                throw new InternalException(exc);
            }
        }

        static private void dropTables(SQLiteDatabase db) throws InternalException {
            try {
                for(String tableName : dbTableNames) {
                    String sql = "DROP TABLE IF EXISTS " + tableName;
                    db.execSQL(sql);
                }
            }
            catch(Exception exc) {
                throw new InternalException(exc);
            }
        }

        static private void emptyTables(SQLiteDatabase db) throws InternalException {
            try {
                for (String tableName : dbTableNames) {
                    int nb = db.delete(tableName, "1", null);
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "deleted " + nb + " + entries in " + tableName);
                }
            }
            catch(Exception exc) {
                throw new InternalException(exc);
            }
        }

        public void onCreate(SQLiteDatabase db) {

            boolean created = false;
            try {
                createTables(db);
                created = true;
                launchFillIfNeeded(db);
            }
            catch(InternalException exc) {
                if(created) {
                    // On tente de revenir en arrière
                    try {
                        dropTables(db);
                    } catch (Exception exc2) {
                        // Rien
                    }
                }
                MainActivity.doToastError("Error while initializing the database: " + exc.getMessage());
            }
        }

        private final static String DATASOURCE = "kowiki-50occurrences.csv";
//        private final static String DATASOURCE = "kowiki-test.csv";

        public final static int INIT_NONE = 0;
        public final static int INIT_COMPLETED = 1;

        public final static int INITSTATE_TOBECHECKED = 0; // Non encore vérifié
        public final static int INITSTATE_INITIALIZING = 1; // En cours
        public final static int INITSTATE_COMPLETED_CHECKED = 2; // Terminé dans la base
        private int checkInitState = INITSTATE_TOBECHECKED;

        protected void setDbInitState(int state)
                throws InternalException {
            SQLiteDatabase db = getDb();

            try {
                String sql = "INSERT INTO " + InfoTableDesc.TABLE_NAME + " "
                        + "(" + InfoTableDesc.COLUMN_NAME_INITSTATE + ")"
                        + " VALUES (?)";

                SQLiteStatement stmt = db.compileStatement(sql);
                stmt.bindLong(1, state);
                stmt.executeInsert();
                stmt.close();
/*
                db.setTransactionSuccessful();
*/
            }
            catch(SQLException exc) {
                exc.printStackTrace();
                throw new InternalException("SQLException (" + exc.getMessage() + ")");
            }
        }

        static protected void resetDb(Context context) throws InternalException {
            DbHelper dbHelper = DbHelper.getInstance(context);

            if (dbHelper.checkInitState == INITSTATE_INITIALIZING) {
                MainActivity.doToastMessage("Database is initializing, please try later...");
                return;
            }

            SQLiteDatabase db = dbHelper.getDb();

            // Création des tables
            dbHelper.recreateDatabase(db);

            // Lancement en asynchrone du remplissage des tables
            dbHelper.launchFillIfNeeded(db);
        }

        protected int getDbInitState(SQLiteDatabase db) throws InternalException {
            Cursor cursor = db.query(InfoTableDesc.TABLE_NAME,
                    new String[] { InfoTableDesc.COLUMN_NAME_INITSTATE },
                    null, null, null, null, null);

            int res;
            try {
                if(cursor.moveToFirst()) {
                    res = cursor.getInt(0);
                }
                else
                    res = INIT_NONE;
            }
            catch(Exception exc) {
                throw new InternalException(exc);
            }

            return res;
        }

        protected void launchFillIfNeeded(final SQLiteDatabase db) throws InternalException {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "Starting launchFillIfNeeded.");

            if (checkInitState != INITSTATE_TOBECHECKED)
                return;

            // synchronized block to ensure that at most one filling is running
            synchronized (DbHelper.class) {
                // Seconde vérification, au cas où...
                if (checkInitState != INITSTATE_TOBECHECKED)
                    return;

                checkInitState = INITSTATE_INITIALIZING;
            }

            boolean noexception = false;

            try {
                // Peut-être la base a-t-elle déjà intialisée
                int dbInitState = getDbInitState(db);

                if (dbInitState == INIT_COMPLETED) {
                    noexception = true;
                    // Pour gagner du temps lors du prochain appel à la méthode courante
                    checkInitState = INITSTATE_COMPLETED_CHECKED;
                    return;
                }

                // Vider les tables
                emptyTables(db);

                // On initialize la base
                // avec quelques données pour que l'utilisateur puisse l'utiliser tout de suite,
                // puis on insère les autres données en arrière-plan.
                fillPartData(db, 0, 1000, 0);

                Thread subthread = new Thread(new Runnable() {
                    public void run() {
                        try {
                            // Remarque : je sais qu'il y a environ 116000 entrées à insérer.
                            int unit = 10000;
                            fillPartData(db, 1000, unit, 200);
                            int i;
                            for(i=1; i <= 9; i++)
                                fillPartData(db, i*unit, (i+1)*unit, 200);
                            fillPartData(db, i * unit, -1, 200);

                            setDbInitState(INIT_COMPLETED);
                            checkInitState = INITSTATE_COMPLETED_CHECKED;
                        }
                        catch (InternalException exc) {
                            MainActivity.doToastError(exc.getMessage());
                        }
                        finally {
                            // Qu'il y ait eu une exception ou que le processus se soit bien terminé,
                            // on sort de l'état "initialisation en cours" à la fin du thread.
                            synchronized (DbHelper.class) {
                                if(checkInitState == INITSTATE_INITIALIZING)
                                    checkInitState = INITSTATE_TOBECHECKED;
                            }
                        }
                    }
                });
                subthread.start();
                noexception = true;
            }
            finally {
                if(! noexception) {
                    synchronized(DbHelper.class) {
                        if(checkInitState == INITSTATE_INITIALIZING) {
                            // Une exception est survenue et la base n'est probablement pas initialisée.
                            // Faire en sorte que l'initialisation reprenne la prochaine fois.
                            try {
                                setDbInitState(INIT_NONE);
                            }
                            catch (Exception exc) {
                                // Tant pis
                            }
                            checkInitState = INITSTATE_TOBECHECKED;
                        }
                    }
                }
            }
        }

        private int fillPartData(SQLiteDatabase db, int start, int end, long wait)
                throws InternalException {
            if(wait > 0) {
                try {
                    Thread.sleep(wait, 0);
                }
                catch (InterruptedException exc) {
                    return start;
                }
            }

            final BufferedReader in;
            try {
                in = new BufferedReader(new InputStreamReader
                        (MainActivity.doGetAssets().open(DATASOURCE)));
            }
            catch(IOException exc) {
                exc.printStackTrace();
                throw new InternalException("Error while opening the data file: " + exc.getMessage());
            }

            int counter = start;
            try {
                // cf. https://www.sqlite.org/lang_transaction.html
                db.beginTransactionNonExclusive();

                String line;
                String sql = "INSERT INTO " + WordTableDesc.TABLE_NAME + " "
                        + "(" + WordTableDesc.COLUMN_NAME_WORD + ", " + WordTableDesc.COLUMN_NAME_CUMUL
                        + ") VALUES (?, ?)";

                // Si start > 0 : on passe un certain nombre de lignes
                for(int i=0; i < start; i++)
                    in.readLine();

                final SQLiteStatement stmt = db.compileStatement(sql);

                while ((end < 0 || counter < end) && ((line = in.readLine()) != null)) {
                    String[] res = line.split("\t", 2);
                    if (res.length == 2) {
                        stmt.bindString(1, res[0].trim());
                        stmt.bindLong(2, Long.parseLong(res[1].trim()));
                        stmt.execute();
                    }

                    counter++;
                }

                stmt.close();
                db.setTransactionSuccessful();
            }
            catch(NumberFormatException exc) {
                exc.printStackTrace();
                throw new InternalException("Number format error (" + exc.getMessage() + ")");
            }
            catch(IOException exc) {
                throw new InternalException("I/O error: " + exc.getMessage());
            }
            finally {
                db.endTransaction();
                try {
                    in.close();
                } catch (IOException exc) {
                    exc.printStackTrace();
                }
            }
            return counter;
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            try {
                recreateDatabase(db);
            }
            catch(InternalException exc) {
                // Rien
            }
        }

        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }

        public int getWordsMaxCumul(SQLiteDatabase db) throws InternalException {
            Cursor cursor = db.query(WordTableDesc.TABLE_NAME,
                    new String[] { "MAX(" + WordTableDesc.COLUMN_NAME_CUMUL + ")" },
                    null, null, null, null, null);

            if(cursor.moveToFirst()) {
                int maxCumul = cursor.getInt(0);
                cursor.close();
                return maxCumul;
            }
            else {
                cursor.close();
                throw new InternalException("Cannot find the max cumul value");
            }
        }

        // fonction utilitaire pour débugguer
        String getDbContentDesc(SQLiteDatabase db) throws InternalException {
            String res;
            res = "Max cumul=" + getWordsMaxCumul(db) +". Nb entries=" + getWordsCount(db) + ".";

            Cursor cursor = db.query(WordTableDesc.TABLE_NAME,
                    new String[]{WordTableDesc.COLUMN_NAME_WORD, WordTableDesc.COLUMN_NAME_CUMUL},
                    null, null, null, null, null, "3");

            boolean hasData = cursor.moveToFirst();
            boolean first = true;
            while(hasData) {
                if(first)
                    first = false;
                else
                    res += ", ";
                res += cursor.getString(0) + "/" + cursor.getInt(1);
                hasData = cursor.moveToNext();
            }

            cursor.close();

            return res;
        }
    }

    static int getWordsCount(SQLiteDatabase db) throws InternalException {
        String[] select = new String[] { "count(*)" };
        Cursor cursor = db.query(WordTableDesc.TABLE_NAME, select, null, null, null, null, null);

        int res = -1;
        if(cursor.moveToFirst())
            res = cursor.getInt(0);
        cursor.close();

        return res;
    }

    static String getWord(Context context) throws InternalException {
        DbHelper mDbHelper = DbHelper.getInstance(context);
        SQLiteDatabase db = mDbHelper.getDb();

        String[] columns = { WordTableDesc.COLUMN_NAME_WORD, WordTableDesc.COLUMN_NAME_CUMUL };

        int maxCumul = mDbHelper.getWordsMaxCumul(db);
        long seuil = (long)Math.floor(Math.random() * maxCumul);

        String where = WordTableDesc.COLUMN_NAME_CUMUL + " > " + seuil;

        Cursor cursor = db.query(WordTableDesc.TABLE_NAME,  // The table to query
                columns,                               // The columns to return
                where,                                // The columns for the WHERE clause
                null,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null,                                 // The sort order
                "1"           // limit
        );

        String res;
        if(cursor.moveToFirst()) {
            res = cursor.getString(cursor.getColumnIndexOrThrow(WordTableDesc.COLUMN_NAME_WORD));
            cursor.close();
        }
        else {
            cursor.close();
            throw new InternalException("No result for " + where + ". Contents of the database: "
                + mDbHelper.getDbContentDesc(db));
        }

/*
        // TODO : à enlever
        int nbWords = getWordsCount(db);
        res = "x" + nbWords;
*/

        return res;
    }
}
