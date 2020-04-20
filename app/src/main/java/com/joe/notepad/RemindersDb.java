package com.joe.notepad;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class RemindersDb {

    private static final String
            REMINDERS_TABLE = "Reminders",
            COLUMN_ID = "_id",
            COLUMN_ENABLED = "Enabled",
            COLUMN_LABEL = "Label",
            COLUMN_DATE = "DueDate";

    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static Context context;
    private static AlarmManager alarmManager;
    private static DbHelper helper;
    private static ArrayList<Reminder> reminders;


    public static String getCreateTableSql() {
        String sql = "CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s BOOL, %s TEXT, %s DATETIME)";
        sql = String.format(sql, REMINDERS_TABLE, COLUMN_ID, COLUMN_ENABLED, COLUMN_LABEL, COLUMN_DATE);
        return sql;
    }

    public static void init(Context context, DbHelper helper) {
        RemindersDb.context = context;
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        RemindersDb.helper = helper;
        reminders = null;
        getAll();
    }

    ///
    /// C-R-U-D Methods
    ///

    // Attempts to add a reminder to the database.
    // Returns the index of added reminder if the operation succeeded, and null otherwise.
    public static int add(String label, Date dueDate) {
        Reminder reminder = new Reminder(label, dueDate);

        ContentValues cv = new ContentValues();
        cv.put(COLUMN_ENABLED, true);
        cv.put(COLUMN_LABEL, label);
        cv.put(COLUMN_DATE, dateFormatter.format(dueDate));

        SQLiteDatabase db = helper.getWritableDatabase();
        reminder.id = db.insert(REMINDERS_TABLE, null, cv);
        if (reminder.id == -1) // Failed
            return -1;
        else {
            // Search the list from top to bottom,
            // and insert the reminder at the index of the first
            // reminder that is due earlier
            int i = 0;
            for (; i < reminders.size(); i++) {
                if (reminder.dueDate.after(reminders.get(i).dueDate))
                    break;
            }

            reminders.add(i, reminder);
            registerReminder(reminder);
            return i;
        }
    }

    // Gets a reminder by its index in the list.
    // (NOTE: index != id)
    public static Reminder get(int index) {
        return new Reminder(reminders.get(index));
    }

    // Gets a reminder by its id in the database
    public static Reminder get(long id) {
        Reminder reminder = null;
        for (int i = 0; i < reminders.size(); i++) {
            Reminder r = reminders.get(i);
            if (r.id == id) {
                reminder = r;
                break;
            }
        }
        return reminder;
    }

    // Returns an array of all the reminders in the database
    public static List<Reminder> getAll() {
        if (reminders == null) { // First time...load reminders from database
            SQLiteDatabase db = helper.getReadableDatabase();
            Cursor cursor = db.query(REMINDERS_TABLE, new String[]{"*"},
                    null, null, null, null,
                    COLUMN_DATE + " DESC");

            Reminder[] results = getResults(cursor);
            reminders = new ArrayList<>(Arrays.asList(results));
        }
        return reminders;
    }

    // Gets an array of reminders from a cursor.
    private static Reminder[] getResults(Cursor cursor) {
        int count = cursor.getCount();
        Reminder[] results = new Reminder[count];
        if (count > 0) {
            int index_id = cursor.getColumnIndex(COLUMN_ID);
            int index_enabled = cursor.getColumnIndex(COLUMN_ENABLED);
            int index_label = cursor.getColumnIndex(COLUMN_LABEL);
            int index_date = cursor.getColumnIndex(COLUMN_DATE);

            for (int i = 0; i < count; i++) {
                cursor.moveToNext();

                Reminder reminder = new Reminder();
                reminder.id = cursor.getLong(index_id);
                reminder.enabled = (cursor.getInt(index_enabled) != 0); // 0 = FALSE, any other value = TRUE
                reminder.label = cursor.getString(index_label);
                try {
                    reminder.dueDate = dateFormatter.parse(cursor.getString(index_date));
                } catch (ParseException ex) {
                }

                results[i] = reminder;
            }
        }
        cursor.close();
        return results;
    }

    // Searches the database for any reminders whose labels contain a given string.
    public static Reminder[] query(String s) {
        s = "%" + s + "%";
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(REMINDERS_TABLE, new String[]{"*"},
                COLUMN_LABEL + " LIKE ?", new String[]{s},
                null, null, COLUMN_DATE + " DESC");

        return getResults(cursor);
    }

    // Update the reminder at index with new properties,
    // and return the new index of the reminder.
    public static int update(int index, String newLabel, Date newDueDate, boolean enabled) {
        Reminder reminder = reminders.get(index);

        ContentValues cv = new ContentValues();
        cv.put(COLUMN_ENABLED, enabled);
        cv.put(COLUMN_LABEL, newLabel);
        cv.put(COLUMN_DATE, dateFormatter.format(newDueDate));

        SQLiteDatabase db = helper.getWritableDatabase();
        int i = db.update(REMINDERS_TABLE, cv, COLUMN_ID + " = ?", new String[]{String.valueOf(reminder.id)});
        if (i == -1) // Fail
            return -1;
        else {
            reminder.enabled = enabled;
            reminder.label = newLabel;
            if (newDueDate.compareTo(reminder.dueDate) != 0) {
                // Date has been changed...shift reminder to its proper position
                reminders.remove(index);
                for (i = 0; i < reminders.size(); i++) {
                    if (newDueDate.after(reminders.get(i).dueDate))
                        break;
                }
                reminder.dueDate = newDueDate;
                reminders.add(i, reminder);
                index = i;
            }
        }
        if (reminder.enabled)
            registerReminder(reminder);
        else
            unregisterReminder(reminder);

        return index;
    }

    // Delete the reminder stored at index.
    public static boolean delete(int index) {
        Reminder reminder = reminders.get(index);
        SQLiteDatabase db = helper.getWritableDatabase();
        int i = db.delete(REMINDERS_TABLE, COLUMN_ID + " = ?", new String[]{String.valueOf(reminder.id)});
        if (i == -1) { // Fail
            return false;
        } else {
            reminders.remove(index);
            unregisterReminder(reminder);
            return true;
        }
    }

    // Delete a reminder by its table id
    public static boolean delete(long id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int i = db.delete(REMINDERS_TABLE, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        if (i == -1) { // Fail
            return false;
        } else {
            // Deleted from database, now remove from list
            for (i = 0; i < reminders.size(); i++) {
                Reminder r = reminders.get(i);
                if (r.id == id) {
                    reminders.remove(i);
                    unregisterReminder(r);
                    break;
                }
            }
            return true;
        }
    }


    private static PendingIntent getIntentFor(Reminder reminder) {
        Intent intent = new Intent(context, ReminderActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.putExtra("id", reminder.id);
        intent.putExtra("showActivity", false);
        return PendingIntent.getActivity(context, (int) reminder.id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    // Registers the reminder with the Android system
    private static void registerReminder(Reminder reminder) {
        alarmManager.set(AlarmManager.RTC_WAKEUP, reminder.dueDate.getTime(), getIntentFor(reminder));
    }

    // Unregisters the reminder from the Android system
    private static void unregisterReminder(Reminder reminder) {
        alarmManager.cancel(getIntentFor(reminder));
    }

    // Register all active reminders
    public static void registerReminders() {
        for (int i = 0; i < reminders.size(); i++) {
            Reminder r = reminders.get(i);
            if (r.enabled && !r.isPast())
                registerReminder(r);
        }
    }

}
