package com.joe.notepad;

import android.content.Context;
import android.content.res.Resources;
import com.google.android.material.snackbar.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

public class ReminderAdapter extends ArrayAdapter<Reminder> {

    private int color_black, color_gray;

    public ReminderAdapter(Context context, List<Reminder> reminders) {
        super(context, 0, reminders);
        Resources res = context.getResources();
        color_black = res.getColor(android.R.color.black);
        color_gray = res.getColor(androidx.appcompat.R.color.abc_secondary_text_material_light);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.item_reminder, parent, false);
        }

        final TextView label = convertView.findViewById(R.id.txt_label);
        TextView date = convertView.findViewById(R.id.txt_date);
        final Switch toggle = convertView.findViewById(R.id.toggle);

        final Reminder reminder = getItem(position);
        label.setText(reminder.label);
        date.setText(reminder.getTimeString());

        if (reminder.isPast()) { // Reminder date/time has passed, gray out this item and disable switch
            label.setTextColor(color_gray);
            toggle.setChecked(false);
            toggle.setEnabled(false);
        } else {
            toggle.setChecked(reminder.enabled);
            toggle.setEnabled(true);
            label.setTextColor(color_black);
        }

        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int i = RemindersDb.update(position, reminder.label, reminder.dueDate, toggle.isChecked());
                if (i == -1) {
                    toggle.toggle();
                    Snackbar.make(label, R.string.error, Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        return convertView;
    }

}
