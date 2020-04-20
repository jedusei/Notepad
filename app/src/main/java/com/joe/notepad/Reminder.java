package com.joe.notepad;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Reminder {
    public long id = -1; // Used by RemindersDb to identify the note in the database
    public String label;
    public Date dueDate;
    public boolean enabled = true;

    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("h:mm a");

    public Reminder() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 1); // Default value is next "o'clock"
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        dueDate = calendar.getTime();
    }

    public Reminder(String label, Date dueDate) {
        this.label = label;
        this.dueDate = dueDate;
    }

    public Reminder(Reminder other) {
        this.id = other.id;
        this.label = other.label;
        this.dueDate = other.dueDate;
        this.enabled = other.enabled;
    }

    // Returns true if due date is past
    public boolean isPast() {
        return dueDate.before(Calendar.getInstance().getTime());
    }

    // Returns a short string describing the due date
    public String getTimeString() {
        String timeString = App.getNiceDateString(dueDate);
        if (!timeString.endsWith("M"))  // It's not today
            timeString = dateFormatter.format(dueDate) + " on " + timeString;

        return timeString;
    }
}
