package com.joe.notepad;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class NoteAdapter extends ArrayAdapter<Note> {

    public NoteAdapter(Context context, List<Note> notes) {
        super(context, 0, notes);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.item_note, parent, false);
        }

        TextView title = convertView.findViewById(R.id.txt_title);
        TextView content = convertView.findViewById(R.id.txt_content);
        TextView date = convertView.findViewById(R.id.txt_date);

        Note note = getItem(position);
        title.setText(note.title);
        date.setText(App.getNiceDateString(note.dateModified));

        if (note.content.isEmpty()) // Collapse TextView if content is empty
            content.setVisibility(View.GONE);
        else {
            content.setText(note.content);
            content.setVisibility(View.VISIBLE);
        }

        return convertView;
    }


}
