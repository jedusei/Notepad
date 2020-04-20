package com.joe.notepad;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final int DELETE_TIMEOUT = 2000;
    public static final String EXTRA_INDEX = "index",
            KEY_CANCEL_DELETE = "cancelDelete",
            CURRENT_PAGE = "currentPage";

    private AppBarLayout appBar;
    private Toolbar toolbar;
    private SearchView searchView;
    private ViewPager viewPager;
    public AlertDialog deleteDialog;

    private NotesFragment notesFragment;
    private RemindersFragment remindersFragment;

    private FloatingActionButton fab;
    private ActionMode actionMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appBar = findViewById(R.id.appBar);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        deleteDialog = new AlertDialog.Builder(this)
                .setNegativeButton(android.R.string.no, null)
                .create();

        String[] titles = {getString(R.string.notes), getString(R.string.reminders)};
        MainPagerAdapter pagerAdapter = new MainPagerAdapter(getSupportFragmentManager(), getFragments(), titles);
        viewPager = findViewById(R.id.pager);
        viewPager.setAdapter(pagerAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {
            }

            @Override
            public void onPageSelected(int i) {
                if (actionMode != null)
                    actionMode.finish();

                closeSearch();
                appBar.setExpanded(true, false);
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);
        int currentPage = getIntent().getIntExtra(EXTRA_INDEX, 0);
        if (savedInstanceState != null)
            currentPage = savedInstanceState.getInt(CURRENT_PAGE, currentPage);

        if (viewPager.getCurrentItem() != currentPage)
            viewPager.setCurrentItem(currentPage, false);

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Fragment fragment;
                Intent intent;
                if (viewPager.getCurrentItem() == 0) {
                    fragment = notesFragment;
                    intent = new Intent(MainActivity.this, NoteActivity.class);
                } else {
                    fragment = remindersFragment;
                    intent = new Intent(MainActivity.this, ReminderEditActivity.class);
                }
                fragment.startActivityForResult(intent, 0);
            }
        });
    }

    // Checks if the fragments exist already,
    // If not, create new ones.
    private Fragment[] getFragments() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments.size() != 0) { // Activity was restarted
            notesFragment = (NotesFragment) fragments.get(0);
            remindersFragment = (RemindersFragment) fragments.get(1);
            return fragments.toArray(new Fragment[0]);
        } else {
            notesFragment = new NotesFragment();
            remindersFragment = new RemindersFragment();
            return new Fragment[]{notesFragment, remindersFragment};
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CURRENT_PAGE, viewPager.getCurrentItem());
    }

    @Override
    protected void onStop() {
        super.onStop();
        closeSearch();
        if (actionMode != null)
            actionMode.finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        final MenuItem aboutItem = menu.findItem(R.id.action_about);
        final MenuItem searchIcon = menu.findItem(R.id.search_icon);

        // Setup search view
        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setQueryHint(getResources().getString(R.string.search) + "...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                s = s.trim();
                if (s.isEmpty())
                    searchIcon.setIcon(R.drawable.ic_search);
                else
                    searchIcon.setIcon(R.drawable.ic_clear);

                if (viewPager.getCurrentItem() == 0)
                    notesFragment.search(s);
                else
                    remindersFragment.search(s);

                return true;
            }
        });
        searchView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View view) { // Expanded
                fab.hide();
                aboutItem.setVisible(false);
                searchIcon.setVisible(true);
            }

            @Override
            public void onViewDetachedFromWindow(View view) { // Collapsed
                searchIcon.setVisible(false);
                aboutItem.setVisible(true);
                fab.show();
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                startActivity(new Intent(this, AboutActivity.class));
                break;
            case R.id.search_icon:
                if (searchView.getQuery().length() != 0)
                    searchView.setQuery("", true);
                break;

            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onSupportActionModeStarted(@NonNull ActionMode mode) {
        super.onSupportActionModeStarted(mode);
        fab.hide();
        actionMode = mode;
    }

    @Override
    public void onSupportActionModeFinished(@NonNull ActionMode mode) {
        super.onSupportActionModeFinished(mode);
        fab.show();
        actionMode = null;
    }

    private void closeSearch() {
        toolbar.collapseActionView();
    }

}
