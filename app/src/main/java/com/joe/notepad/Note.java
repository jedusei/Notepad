package com.joe.notepad;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Note {
    public long id = -1; // Used by NotesDb to identify the note in the database
    public String title;
    public String content;
    public Date dateModified;

    public Note() {
    }

    public Note(String title, String content) {
        this.title = title.trim();
        this.content = content.trim();
        this.dateModified = Calendar.getInstance().getTime();
    }

    public Note(Note other) {
        this.id = other.id;
        this.title = other.title;
        this.content = other.content;
        this.dateModified = other.dateModified;
    }

    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy");
        return String.format("%s\n\n%s\n(%s)", title, content, sdf.format(dateModified));
    }


}
