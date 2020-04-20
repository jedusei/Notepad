package com.joe.notepad;

import android.app.AlarmManager;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public final class App extends Application {

    public static final String NOTIFICATION_CHANNEL_ID = "main";

    // Response code sent when the note is deleted
    public final static int RESULT_DELETED = -2;

    private static Context context;
    private static String name;
    private static String playStoreURL;
    private static DbHelper dbHelper;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize variables...
        context = this;
        name = getResources().getString(R.string.app_name);
        playStoreURL = "https://www.play.google.com/store/apps.details?id=" + getPackageName();

        // Initialize dbHelper and db classes...
        dbHelper = new DbHelper(this, NotesDb.getCreateTableSql(), RemindersDb.getCreateTableSql());
        NotesDb.init(dbHelper);
        RemindersDb.init(this, dbHelper);

        // Create Notification channel (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    getString(R.string.reminders),
                    NotificationManager.IMPORTANCE_HIGH);

            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        dbHelper.close();
    }

    public static String getName() {
        return name;
    }

    public static String getPlayStoreURL() {
        return playStoreURL;
    }

    public static String getNiceDateString(Date date) {
        Calendar calendar = Calendar.getInstance();
        int curYear = calendar.get(Calendar.YEAR);
        int curMonth = calendar.get(Calendar.MONTH);
        int curDay = calendar.get(Calendar.DAY_OF_MONTH);

        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        boolean sameDay = false, sameYear = false;
        if (year == curYear) {
            if (month == curMonth) {
                if (day == curDay) {
                    sameDay = true;
                }
            }
            sameYear = true;
        }

        String formatString;
        if (sameDay)
            formatString = "h:mm a";
        else if (sameYear)
            formatString = "MMM dd";
        else
            formatString = "dd/MM/yyyy";

        SimpleDateFormat sdf = new SimpleDateFormat(formatString);
        return sdf.format(date);
    }

    // Clear database data and repopulate tables with predefined items
    public static void usePresetDb() {
        try {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.execSQL("DELETE FROM Notes;");
            db.execSQL("DELETE FROM Reminders;");

            BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("NotepadDb.sql")));
            String line, sql = null;
            while ((line = reader.readLine()) != null) { // Execute statements line by line
                if (sql == null)
                    sql = line;
                else
                    sql += line;

                if (sql.endsWith(";")) {
                    db.execSQL(sql);
                    sql = null;
                } else
                    sql += "\n";
            }
            reader.close();

        } catch (IOException ex) {
        }

        // Register reminders
        RemindersDb.init(context, dbHelper);
        RemindersDb.registerReminders();

        // Restart application
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 500, pendingIntent);
        System.exit(0);
    }


}
