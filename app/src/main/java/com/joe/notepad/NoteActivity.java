package com.joe.notepad;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import static com.joe.notepad.App.RESULT_DELETED;

public class NoteActivity extends AppCompatActivity {

    LinearLayout grpView, grpEdit;
    TextView txtTitle, txtContent, txtDate;
    EditText editTitle, editContent;

    MenuItem editAction;
    Menu menu;

    AlertDialog deleteDialog, exitDialog;

    // Is the user currently in edit mode?
    boolean isEditing;

    // Array index of the note that is currently being viewed
    // -1 if this is a new note
    int index;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        createDialogs();

        grpView = findViewById(R.id.content_note_view);
        grpEdit = findViewById(R.id.content_note_edit);
        txtTitle = findViewById(R.id.txt_title);
        txtContent = findViewById(R.id.txt_content);
        txtDate = findViewById(R.id.txt_date);
        editTitle = findViewById(R.id.edit_title);
        editContent = findViewById(R.id.edit_content);

        if (savedInstanceState != null) {
            index = savedInstanceState.getInt(MainActivity.EXTRA_INDEX, -1); // Restore saved index (if any)
        }
        else {
            index = getIntent().getIntExtra(MainActivity.EXTRA_INDEX, -1);
        }

        if (index == -1) // New Note
            setTitle(R.string.new_note);
        else {
            Note note = NotesDb.get(index);
            txtTitle.setText(note.title);
            txtContent.setText(note.content);
            txtDate.setText(App.getNiceDateString(note.dateModified));
            grpView.setVisibility(View.VISIBLE);
        }
    }

    private void createDialogs() {
        deleteDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.delete_note)
                .setMessage(R.string.question_delete_note)
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (index != -1) {// Not a new note (i.e. note is already in database)
                            Intent intent = new Intent();
                            intent.putExtra(MainActivity.EXTRA_INDEX, index);
                            setResult(RESULT_DELETED, intent); // Tell NotesFragment to remove the note from the list
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
                        if (index == -1) // New note
                            finish();
                        else
                            exitEditMode();
                    }
                })
                .create();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.note, menu);
        editAction = menu.findItem(R.id.action_edit);
        this.menu = menu;
        if (index == -1) { // New note
            enterEditMode();
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (isEditing)
                    confirmExit();
                else
                    finish();
                break;
            case R.id.action_edit:
                enterEditMode();
                break;
            case R.id.action_copy:
                copyNote();
                break;
            case R.id.action_share:
                shareNote();
                break;
            case R.id.action_save:
                saveNote();
                break;
            case R.id.action_cancel:
                confirmExit();
                break;
            case R.id.action_delete:
                confirmDelete();
                break;

            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (isEditing)
            confirmExit();
        else
            super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(MainActivity.EXTRA_INDEX, index); // Store current index
    }

    private void enterEditMode() {
        isEditing = true;
        editAction.setVisible(false);
        menu.setGroupVisible(R.id.group_view_mode_only, false);
        menu.setGroupVisible(R.id.group_edit_mode_only, true);

        grpView.setVisibility(View.GONE);

        if (index != -1) {
            editTitle.setText(txtTitle.getText());
            editContent.setText(txtContent.getText());
        }

        grpEdit.setVisibility(View.VISIBLE);
        editTitle.requestFocus();
    }

    private void exitEditMode() {
        isEditing = false;
        menu.setGroupVisible(R.id.group_edit_mode_only, false);
        editAction.setVisible(true);
        menu.setGroupVisible(R.id.group_view_mode_only, true);
        grpEdit.setVisibility(View.GONE);
        grpView.setVisibility(View.VISIBLE);
    }

    private void copyNote() {
        Note note = NotesDb.get(index);
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboardManager.setPrimaryClip(ClipData.newPlainText("text", note.toString()));
        Snackbar.make(txtTitle, R.string.copied, Snackbar.LENGTH_SHORT).show();
    }

    private void shareNote() {
        Note note = NotesDb.get(index);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, note.toString());
        intent = Intent.createChooser(intent, getString(R.string.share_with));
        startActivity(intent);
    }

    private void saveNote() {
        boolean success, newNote = false;
        String newTitle = editTitle.getText().toString();
        String newContent = editContent.getText().toString();
        Note note = null;

        if (newTitle.trim().isEmpty()) { // Don't save note without title
            editTitle.requestFocus();
            Snackbar.make(txtTitle, R.string.error_empty_title, Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (index != -1) {
            success = NotesDb.update(index, newTitle, newContent);
        } else { // New note
            note = NotesDb.add(newTitle, newContent);
            newNote = true;
            success = (note != null);
        }

        if (!success) { // Fail
            Snackbar.make(txtTitle, R.string.error, Snackbar.LENGTH_SHORT).show();
        } else { // Save operation succeeded

            if (newNote) // Change title from 'New Note'
                setTitle(R.string.note);
            else
                note = NotesDb.get(0); // Get updated note

            index = 0; // Note is now at the top

            txtTitle.setText(note.title);
            txtContent.setText(note.content);
            txtDate.setText(App.getNiceDateString(note.dateModified));

            // Set this flag so that the list in MainActivity is refreshed when the user leaves this activity
            setResult(RESULT_OK);


            Snackbar.make(txtTitle, R.string.saved, Snackbar.LENGTH_SHORT).show();
            exitEditMode();
        }
    }

    private void confirmDelete() {
        deleteDialog.show();
    }

    // If this is a new note and the text fields aren't empty, show confirm dialog.
    // If this isn't a new note then compare the TextViews and EditTexts
    // and show confirm dialog if there's a difference.
    private void confirmExit() {
        String title1 = editTitle.getText().toString();
        String content1 = editContent.getText().toString();
        if (index == -1) { // New note
            if (title1.trim().isEmpty() && content1.trim().isEmpty())
                finish();
            else
                exitDialog.show();
        } else {
            String title2 = txtTitle.getText().toString();
            String content2 = txtContent.getText().toString();
            if (!title1.equals(title2) || !content1.equals(content2)) // If a change has been made
                exitDialog.show();
            else
                exitEditMode();
        }
    }

}
