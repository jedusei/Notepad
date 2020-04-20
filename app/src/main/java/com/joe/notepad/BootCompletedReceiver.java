package com.joe.notepad;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

// All alarms are automatically cancelled by the Android system
// when the device shuts down, so all enabled reminders should
// be re-registered
public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals( Intent.ACTION_BOOT_COMPLETED)) {
            RemindersDb.registerReminders();
        }
    }
}
