package com.joe.notepad;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static com.joe.notepad.App.RESULT_DELETED;

public class ReminderEditActivity extends AppCompatActivity {

    private EditText editLabel;
    private TextView txtDate, txtTime;
    private DatePicker datePicker;
    private TimePicker timePicker;
    private AlertDialog dateDialog, timeDialog, deleteDialog, exitDialog;
    private Calendar calendar;
    private DateFormat dateFormatter = SimpleDateFormat.getDateInstance();
    private DateFormat timeFormatter = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT);
    private int index;
    private boolean unsavedChanges;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_edit);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState != null) {
            index = savedInstanceState.getInt(MainActivity.EXTRA_INDEX, -1); // Restore saved index (if any)
        }
        else {
            index = getIntent().getIntExtra(MainActivity.EXTRA_INDEX, -1);
        }

        Reminder reminder;
        if (index != -1)
            reminder = RemindersDb.get(index);
        else { // New Reminder
            setTitle(R.string.new_reminder);
            reminder = new Reminder();
        }

        calendar = Calendar.getInstance();
        calendar.setTime(reminder.dueDate);
        createPickers();
        createDialogs();

        editLabel = findViewById(R.id.edit_label);
        editLabel.setText(reminder.label);
        editLabel.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                unsavedChanges = true;
            }
        });

        txtDate = findViewById(R.id.txt_date);
        txtDate.setText(dateFormatter.format(calendar.getTime()));
        txtDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dateDialog.show();
            }
        });

        txtTime = findViewById(R.id.txt_time);
        txtTime.setText(timeFormatter.format(calendar.getTime()));
        txtTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                timeDialog.show();
            }
        });
    }

    private void createPickers() {
        datePicker = new DatePicker(this);
        datePicker.setMinDate(Calendar.getInstance().getTimeInMillis());
        datePicker.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        timePicker = new TimePicker(this);
        timePicker.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY));
        timePicker.setCurrentMinute(calendar.get(Calendar.MINUTE));
    }

    private void createDialogs() {
        dateDialog = new AlertDialog.Builder(this)
                .setView(datePicker)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        calendar.set(Calendar.YEAR, datePicker.getYear());
                        calendar.set(Calendar.MONTH, datePicker.getMonth());
                        calendar.set(Calendar.DAY_OF_MONTH, datePicker.getDayOfMonth());
                        txtDate.setText(dateFormatter.format(calendar.getTime()));
                        unsavedChanges = true;
                    }
                })
                .create();

        timeDialog = new AlertDialog.Builder(this)
                .setView(timePicker)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        calendar.set(Calendar.HOUR_OF_DAY, timePicker.getCurrentHour());
                        calendar.set(Calendar.MINUTE, timePicker.getCurrentMinute());
                        txtTime.setText(timeFormatter.format(calendar.getTime()));
                        unsavedChanges = true;
                    }
                })
                .create();

        deleteDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.delete_reminder)
                .setMessage(R.string.question_delete_reminder)
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (index != -1) {// Not a new reminder (i.e. reminder is already in database)
                            Intent intent = new Intent();
                            intent.putExtra(MainActivity.EXTRA_INDEX, index);
                            setResult(RESULT_DELETED, intent); // Tell RemindersFragment to remove the reminder from the list
                        }
                        finish();
                    }
                })
                .create();

        exitDialog = new AlertDialog.Builder(this)
                .setMessage(getString(R.string.question_discard_changes))
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })
                .create();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reminder, menu);
        if (index == -1)
            menu.findItem(R.id.action_delete).setVisible(false); // New reminders can't be deleted
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                confirmExit();
                break;

            case R.id.action_save:
                saveChanges();
                break;

            case R.id.action_delete:
                deleteDialog.show();
                break;

            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        confirmExit();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(MainActivity.EXTRA_INDEX, index); // Store current index so we
    }

    private void confirmExit() {
        if (unsavedChanges)
            exitDialog.show();
        else
            finish();
    }

    private void saveChanges() {
        String newLabel = editLabel.getText().toString().trim();
        if (newLabel.isEmpty()) {
            Snackbar.make(editLabel, R.string.error_empty_title, Snackbar.LENGTH_SHORT).show();
            return;
        }

        Date newDate = calendar.getTime();
        if (newDate.before(Calendar.getInstance().getTime())) { // If date is past already
            Snackbar.make(editLabel, R.string.error_past, Snackbar.LENGTH_SHORT).show();
            return;
        }

        int newIndex;
        if (index == -1)  // New Reminder
            newIndex = RemindersDb.add(newLabel, newDate);
        else
            newIndex = RemindersDb.update(index, newLabel, newDate, true);

        if (newIndex == -1) // Fail
            Snackbar.make(editLabel, R.string.error, Snackbar.LENGTH_SHORT).show();
        else {
            if (index == -1) // Change from 'New Reminder' to 'Reminder'
                setTitle(R.string.reminder);

            setResult(RESULT_OK);
            finish();
        }
    }
}
