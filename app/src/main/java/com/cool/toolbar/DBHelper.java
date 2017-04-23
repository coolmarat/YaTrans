package com.cool.toolbar;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by arov on 18.04.2017.
 */

public class DBHelper extends SQLiteOpenHelper {

    public static final String WORD_TABLE = "srcword";
    public static final String TRANSLATION_TABLE = "translation";
    public static final String SYNONIM_TABLE = "synonim";
    public static final String MEAN_TABLE = "mean";
    private Context context;


    public DBHelper(Context ctx){
        super(ctx, "myDB", null, 1);
        context = ctx;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table srcword ("
                 + "id integer primary key autoincrement, "
                 + "srctext text, "
                 + "srclng text, "
                 + "destlng text, "
                 + "maintranslate text, "
                 + "main_partofspeech text, "
                 + "isfavorite integer default 0" + ");");

        db.execSQL("create table translation ("
                 + "id integer primary key autoincrement, "
                 + "id_word integer, "
                 + "partofspeech text, "
                 + "desttext text" + ");");

        db.execSQL("create table synonim ("
                 + "id integer primary key autoincrement, "
                 + "id_translation integer, "
                 + "syntext text" + ");");

        db.execSQL("create table mean ("
                + "id integer primary key autoincrement, "
                + "id_translation integer, "
                + "meantext text" + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }


}
