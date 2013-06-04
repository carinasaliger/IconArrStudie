package com.example.iconarrstudie;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.Serializable;

/**
 * Created by john-louis on 18.05.13.
 */
public class LauncherDBHandler extends SQLiteOpenHelper implements Serializable{
    //TAG fÃ¼r logs
    private static final String TAG = LauncherDBHandler.class.getSimpleName();
    //Allgemeine Angaben für die Datenbank
    private static final String DATABASE_NAME = "launcher.db";
    private static final int DATABASE_VERSION = 1;

    //SQL-Anweisungen für Erstellen der Tabelle
    private static final String CREATE_LAUNCHER_DB =
            "CREATE TABLE favorites ( _id INTEGER PRIMARY KEY, title TEXT, intent TEXT, container INTEGER, screen INTEGER, cellX INTEGER, cellY INTEGER, spanX INTEGER, spanY INTEGER, itemType INTEGER, appWidgetId INTEGER NOT NULL DEFAULT -1, isShortcut INTEGER, iconType INTEGER, iconPackage TEXT, iconResource TEXT, icon BLOB, uri TEXT, displayMode INTEGER)";
    private static final String DROP_TABLE =
            "DROP TABLE IF EXISTS favorites";

    public LauncherDBHandler(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_LAUNCHER_DB);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrade der Datenbank " + DATABASE_NAME + " von Version " + oldVersion + " auf Version" + newVersion + "; Alle Daten werden gelÃ¶scht!");
        sqLiteDatabase.execSQL(DROP_TABLE);
        onCreate(sqLiteDatabase);
    }
}