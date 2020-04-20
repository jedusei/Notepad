package com.joe.notepad;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;

import static android.app.Activity.RESULT_OK;
import static com.joe.notepad.App.RESULT_DELETED;
import static com.joe.notepad.MainActivity.DELETE_TIMEOUT;
import static com.joe.notepad.MainActivity.EXTRA_INDEX;
import static com.joe.notepad.MainActivity.KEY_CANCEL_DELETE;


public class RemindersFragment extends Fragment implements AbsListView.MultiChoiceModeListener {

    private ReminderAdapter adapter, searchAdapter;
    private ArrayList<Reminder> searchResults;
    private ListView listView;
    private View noRemindersView, noResultsView;

    private AlertDialog deleteDialog;
    private boolean cancelDelete;
    private String lastSearch;

    public RemindersFragment() {
        // Required empty public constructor
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        deleteDialog = ((MainActivity) getActivity()).deleteDialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_reminders, container, false);
        setupListView(view);
        return view;
    }

    private void setupListView(View root) {
        adapter = new ReminderAdapter(getContext(), RemindersDb.getAll());
        searchResults = new ArrayList<>(adapter.getCount());
        searchAdapter = new ReminderAdapter(getContext(), searchResults);

        listView = root.findViewById(R.id.listView);
        noRemindersView = root.findViewById(R.id.no_reminders_view);
        noResultsView = root.findViewById(R.id.no_results_view);

        listView.setEmptyView(noRemindersView);
        listView.setAdapter(adapter);
        listView.setMultiChoiceModeListener(this);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getContext(), ReminderEditActivity.class);
                intent.putExtra(EXTRA_INDEX, i);
                startActivityForResult(intent, 0);
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_CANCEL_DELETE, cancelDelete);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            cancelDelete = savedInstanceState.getBoolean(KEY_CANCEL_DELETE, false);
            if (listView.getCheckedItemCount() > 0) { // Restore selection
                int first = listView.getCheckedItemPositions().keyAt(0);
                listView.setItemChecked(first, true);
            }
        }
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode actionMode, int i, long l, boolean checked) {
        int count = listView.getCheckedItemCount();
        if (count > 0) {
            actionMode.setTitle(count + " selected");
            Menu menu = actionMode.getMenu();
            // Hide the 'Select all' item if everything is selected already
            menu.findItem(R.id.action_select_all).setVisible(count != listView.getCount());
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        getActivity().getMenuInflater().inflate(R.menu.select_reminder, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            // Select all reminders
            case R.id.action_select_all:
                for (int i = 0; i < listView.getCount(); i++) {
                    listView.setItemChecked(i, true);
                }
                return true; // Return immediately so that selection mode doesn't close

            // Delete selected reminders
            case R.id.action_delete:
                startDeleteReminders(actionMode);
                return true;

            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) { // Reminder edited or added...must refresh list
            adapter.notifyDataSetChanged();
            Snackbar.make(listView, R.string.saved, Snackbar.LENGTH_SHORT).show();
        } else if (resultCode == RESULT_DELETED) { // User wants reminder deleted
            int index = data.getIntExtra(MainActivity.EXTRA_INDEX, -1);
            if (RemindersDb.delete(index)) { // requestCode is actually the index of the reminder that was being viewed
                Snackbar.make(listView, R.string.deleted, Snackbar.LENGTH_SHORT).show();
                adapter.notifyDataSetChanged();
            } else {
                Snackbar.make(listView, R.string.error, Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    // Show a exitDialog to confirm deletion.
    // If confirmed, show a snackbar with an action
    // to allow the user to cancel the action.
    // When the snackbar disappears, the reminders are deleted.
    private void startDeleteReminders(final ActionMode actionMode) {
        // Get list of selected reminders (indices)
        SparseBooleanArray checkedItems = listView.getCheckedItemPositions();
        int count = listView.getAdapter().getCount();
        final ArrayList<Integer> selectedIndices = new ArrayList<>(checkedItems.size());
        for (int i = 0; i < count; i++) {
            if (checkedItems.get(i))
                selectedIndices.add(i);
        }

        if (selectedIndices.size() == 1) { // Change messages to singular/plural form where appropriate :P
            deleteDialog.setTitle(R.string.delete_reminder);
            deleteDialog.setMessage(getString(R.string.question_delete_reminder));
        } else {
            deleteDialog.setTitle(R.string.delete_reminders);
            deleteDialog.setMessage(getString(R.string.question_delete_reminders));
        }

        deleteDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int n) {
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Give the user a chance to cancel delete operation
                        // Actual delete operation is carried out after this snackbar disappears
                        Snackbar.make(listView, R.string.deleting, DELETE_TIMEOUT)
                                .setAction(R.string.cancel, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        cancelDelete = true;
                                        Snackbar.make(listView, R.string.cancelled, Snackbar.LENGTH_SHORT).show();
                                    }
                                }).show();

                        // Run after the snackbar has disappeared
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!cancelDelete) // User hasn't cancelled the operation...go ahead.
                                    deleteReminders(selectedIndices);
                                else
                                    cancelDelete = false; // Reset cancelDelete
                            }
                        }, DELETE_TIMEOUT);
                    }
                }, 1000);
            }
        });

        // Reset the selection when this exitDialog is dismissed
        deleteDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                actionMode.finish();
            }
        });

        // Finally, show the exitDialog to the user.
        deleteDialog.show();
    }

    private void deleteReminders(ArrayList<Integer> selectedItems) {
        boolean success = false; // true if at least one reminder was deleted successfully
        if (listView.getAdapter() != searchAdapter) {
            // Loop in reverse because we're removing items as we go along
            for (int i = selectedItems.size() - 1; i >= 0; i--) {
                int pos = selectedItems.get(i);
                if (RemindersDb.delete(pos))
                    success = true;
            }
            adapter.notifyDataSetChanged();

        } else { // We're in search mode, so delete by id instead
            // Loop in reverse because we're removing items as we go along
            for (int i = selectedItems.size() - 1; i >= 0; i--) {
                int pos =selectedItems.get(i);
                Reminder reminder = searchResults.get(pos);
                if (RemindersDb.delete(reminder.id)) {
                    searchResults.remove(pos);
                    success = true;
                }
            }
            search(lastSearch); // Refresh search results
        }
        if (success)
            Snackbar.make(listView, R.string.deleted, Snackbar.LENGTH_SHORT).show();
        else
            Snackbar.make(listView, R.string.error, Snackbar.LENGTH_SHORT).show();
    }

    public void search(String query) {
        searchResults.clear();
        if (query.isEmpty()) { // Reset listView (Show all reminders)
            noResultsView.setVisibility(View.GONE);
            listView.setAdapter(adapter);
            adapter.notifyDataSetInvalidated();
        } else {
            if (listView.getAdapter() != searchAdapter)
                listView.setAdapter(searchAdapter);

            Reminder[] results = RemindersDb.query(query);
            searchResults.addAll(Arrays.asList(results));
            searchAdapter.notifyDataSetChanged();
            noRemindersView.setVisibility(View.GONE);
            lastSearch = query;
            if (results.length != 0)
                noResultsView.setVisibility(View.GONE);
            else
                noResultsView.setVisibility(View.VISIBLE);
        }
    }

}
