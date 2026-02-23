package com.example.mytranslator;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class WordDatabaseHelper extends SQLiteOpenHelper {
    private Context context;
    private static final String DB_NAME = "wordlist";
    private static final int DB_VERSION = 1;

    WordDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE WORDLIST (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "ENGLISH TEXT, "
                + "FRENCH TEXT, "
                + "FREQUENCY INTEGER);");

        try {
            InputStreamReader in = new InputStreamReader(context.getResources().getAssets().open("2.txt"));
            BufferedReader bufReader = new BufferedReader(in);
            String line;
            while((line = bufReader.readLine()) != null){
                String[] words = line.trim().split("\\s+", 2);
                String english = words[0];
                String french = words[1];
                insertWord(db, english, french, 0);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    private static void insertWord(SQLiteDatabase db, String english, String french, int frequency){
        ContentValues wordValues = new ContentValues();
        wordValues.put("ENGLISH", english);
        wordValues.put("FRENCH", french);
        wordValues.put("FREQUENCY", frequency);
        db.insert("WORDLIST", null, wordValues);
    }


}
