package com.joe.notepad;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class MainPagerAdapter extends FragmentPagerAdapter {

    private String[] titles;
    private Fragment[] fragments;

    public MainPagerAdapter(FragmentManager fm, Fragment[] fragments, String... titles) {
        super(fm);
        this.fragments = fragments;
        this.titles = titles;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        if (position < titles.length)
            return titles[position];
        else
            return null;
    }

    @Override
    public Fragment getItem(int i) {
        if (i < fragments.length)
            return fragments[i];
        else
            return null;
    }

    @Override
    public int getCount() {
        return fragments.length;
    }
}
