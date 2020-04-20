package com.joe.notepad;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {

    private static final String NAME = "NotepadDb";
    private static final int VERSION = 1;
    private String[] createTableSqls;

    public DbHelper(Context context, String... createTableSqls) {
        super(context, NAME, null, VERSION);
        this.createTableSqls = createTableSqls;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        if (createTableSqls != null) {
            for (int i = 0; i < createTableSqls.length; i++) {
                db.execSQL(createTableSqls[i]);
            }
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        //TODO: Drop all tables
    }
}
