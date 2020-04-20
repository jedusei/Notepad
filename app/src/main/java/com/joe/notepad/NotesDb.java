package com.joe.notepad;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class NotesDb {

    private static final String
            NOTES_TABLE = "Notes",
            COLUMN_ID = "_id",
            COLUMN_TITLE = "Title",
            COLUMN_CONTENT = "Content",
            COLUMN_DATE = "DateModified";

    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static DbHelper helper;
    private static ArrayList<Note> notes;


    public static String getCreateTableSql() {
        String sql = "CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT, %s TEXT, %s DATETIME)";
        sql = String.format(sql, NOTES_TABLE, COLUMN_ID, COLUMN_TITLE, COLUMN_CONTENT, COLUMN_DATE);
        return sql;
    }

    public static void init(DbHelper helper) {
        NotesDb.helper = helper;
        getAll();
    }

    ///
    /// C-R-U-D Methods
    ///

    // Attempts to add a note to the database.
    // Returns a copy of the added note if the operation succeeded, and null otherwise.
    public static Note add(String title, String content) {
        Note note = new Note(title, content);
        note.dateModified = Calendar.getInstance().getTime();

        ContentValues cv = new ContentValues();
        cv.put(COLUMN_TITLE, note.title);
        cv.put(COLUMN_CONTENT, note.content);
        cv.put(COLUMN_DATE, dateFormatter.format(note.dateModified));

        SQLiteDatabase db = helper.getWritableDatabase();
        note.id = db.insert(NOTES_TABLE, null, cv);
        if (note.id == -1) // Failed
            return null;
        else {
            notes.add(0, note); // Place on top of list, since it's the latest.
            return new Note(note); // Create a copy of the note
        }
    }

    // Gets a note by its index in the list.
    // (NOTE: index != id)
    public static Note get(int index) {
        return new Note(notes.get(index));
    }

    // Returns an array of all the notes in the database.
    public static List<Note> getAll() {
        if (notes == null) { // First time...load notes from database
            SQLiteDatabase db = helper.getReadableDatabase();
            Cursor cursor = db.query(NOTES_TABLE, new String[]{"*"},
                    null, null, null, null,
                    COLUMN_DATE + " DESC");

            Note[] results = getResults(cursor);
            notes = new ArrayList<>(Arrays.asList(results));
        }
        return notes;
    }

    // Gets an array of notes from a cursor.
    private static Note[] getResults(Cursor cursor) {
        int count = cursor.getCount();
        Note[] results = new Note[count];
        if (count > 0) {
            int index_id = cursor.getColumnIndex(COLUMN_ID);
            int index_title = cursor.getColumnIndex(COLUMN_TITLE);
            int index_content = cursor.getColumnIndex(COLUMN_CONTENT);
            int index_date = cursor.getColumnIndex(COLUMN_DATE);

            for (int i = 0; i < count; i++) {
                cursor.moveToNext();

                Note note = new Note();
                note.id = cursor.getLong(index_id);
                note.title = cursor.getString(index_title);
                note.content = cursor.getString(index_content);
                try {
                    note.dateModified = dateFormatter.parse(cursor.getString(index_date));
                } catch (ParseException ex) {
                }

                results[i] = note;
            }
        }
        cursor.close();
        return results;
    }

    // Searches the database for any notes which contain a given string (whether in the title or content).
    public static Note[] query(String s) {
        s = "%" + s + "%";
        String sql = "SELECT * FROM %s WHERE (%s LIKE ?) OR (%s LIKE ?) ORDER BY %s DESC";
        sql = String.format(sql, NOTES_TABLE, COLUMN_TITLE, COLUMN_CONTENT, COLUMN_DATE);
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.rawQuery(sql, new String[]{s, s});

        return getResults(cursor);
    }

    // Update the note at index with a new title and content.
    public static boolean update(int index, String newTitle, String newContent) {
        Note note = new Note(newTitle, newContent);
        note.id = notes.get(index).id;

        ContentValues cv = new ContentValues();
        cv.put(COLUMN_TITLE, newTitle);
        cv.put(COLUMN_CONTENT, newContent);
        cv.put(COLUMN_DATE, dateFormatter.format(note.dateModified));

        SQLiteDatabase db = helper.getWritableDatabase();
        int i = db.update(NOTES_TABLE, cv, COLUMN_ID + " = ?", new String[]{String.valueOf(note.id)});
        if (i == -1) // Fail
            return false;
        else {
            // Move note to top
            notes.remove(index);
            notes.add(0, note);
            return true;
        }
    }

    // Delete the note stored at index.
    public static boolean delete(int index) {
        Note note = notes.get(index);
        SQLiteDatabase db = helper.getWritableDatabase();
        int i = db.delete(NOTES_TABLE, COLUMN_ID + " = ?", new String[]{String.valueOf(note.id)});
        if (i == -1) { // Fail
            return false;
        } else {
            notes.remove(index);
            return true;
        }
    }

    // Delete a note by its table id.
    public static boolean delete(long id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int i = db.delete(NOTES_TABLE, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        if (i == -1) { // Fail
            return false;
        } else {
            // Deleted from database, now remove from list
            for (i = 0; i < notes.size(); i++) {
                if (notes.get(i).id == id) {
                    notes.remove(i);
                    break;
                }
            }
            return true;
        }
    }

}
