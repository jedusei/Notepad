package com.joe.notepad;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ReminderActivity extends AppCompatActivity {

    public final String EXTRA_ID = "id";
    public final String EXTRA_SHOW_ACTIVITY = "showActivity";
    Reminder reminder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        long id = intent.getLongExtra(EXTRA_ID, -1);
        reminder = RemindersDb.get(id);

        if (!intent.getBooleanExtra(EXTRA_SHOW_ACTIVITY, true)) {
            // Don't show the activity, just display the notification
            showNotification();
            finish();
            return;
        }

        setContentView(R.layout.dialog_reminder);
        setFinishOnTouchOutside(false);

        TextView label = findViewById(R.id.txt_label);
        label.setText(reminder.label);
        TextView date = findViewById(R.id.txt_date);
        date.setText(reminder.getTimeString());

        Button btn = findViewById(R.id.btn_view);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // View all reminders
                Intent intent = new Intent(ReminderActivity.this, MainActivity.class);
                intent.putExtra(MainActivity.EXTRA_INDEX, 1);
                startActivity(intent);
                finish();
            }
        });

        btn = findViewById(R.id.btn_ok);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void showNotification() {
        Intent intent = getIntent();
        intent.putExtra(EXTRA_SHOW_ACTIVITY, true); // Redirect to this activity, but this time show it
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this, App.NOTIFICATION_CHANNEL_ID)
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setSmallIcon(R.drawable.ic_no_reminders)
                .setContentTitle(reminder.label)
                .setContentText(getString(R.string.reminder))
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify((int) reminder.id, notification);
    }
}
