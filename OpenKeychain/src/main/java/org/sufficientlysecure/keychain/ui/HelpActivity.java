/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.adapter.PagerTabStripAdapter;
import org.sufficientlysecure.keychain.ui.adapter.TabsAdapter;
import org.sufficientlysecure.keychain.util.SlidingTabLayout;

public class HelpActivity extends ActionBarActivity {
    public static final String EXTRA_SELECTED_TAB = "selected_tab";

    public static final int TAB_START = 0;
    public static final int TAB_FAQ = 1;
    public static final int TAB_WOT = 2;
    public static final int TAB_NFC = 3;
    public static final int TAB_CHANGELOG = 4;
    public static final int TAB_ABOUT = 5;

    ViewPager mViewPager;
    private PagerTabStripAdapter mTabsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setHomeButtonEnabled(false);

        setContentView(R.layout.help_activity);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        SlidingTabLayout slidingTabLayout =
                (SlidingTabLayout) findViewById(R.id.sliding_tab_layout);

        mTabsAdapter = new PagerTabStripAdapter(this);
        mViewPager.setAdapter(mTabsAdapter);

        int selectedTab = TAB_START;
        Intent intent = getIntent();
        if (intent.getExtras() != null && intent.getExtras().containsKey(EXTRA_SELECTED_TAB)) {
            selectedTab = intent.getExtras().getInt(EXTRA_SELECTED_TAB);
        }

        Bundle startBundle = new Bundle();
        startBundle.putInt(HelpHtmlFragment.ARG_HTML_FILE, R.raw.help_start);
        mTabsAdapter.addTab(HelpHtmlFragment.class, startBundle,
                getString(R.string.help_tab_start));

        Bundle faqBundle = new Bundle();
        faqBundle.putInt(HelpHtmlFragment.ARG_HTML_FILE, R.raw.help_faq);
        mTabsAdapter.addTab(HelpHtmlFragment.class, faqBundle,
                getString(R.string.help_tab_faq));

        Bundle wotBundle = new Bundle();
        wotBundle.putInt(HelpHtmlFragment.ARG_HTML_FILE, R.raw.help_wot);
        mTabsAdapter.addTab(HelpHtmlFragment.class, wotBundle,
                getString(R.string.help_tab_wot));

        Bundle nfcBundle = new Bundle();
        nfcBundle.putInt(HelpHtmlFragment.ARG_HTML_FILE, R.raw.help_nfc_beam);
        mTabsAdapter.addTab(HelpHtmlFragment.class, nfcBundle,
                getString(R.string.help_tab_nfc_beam));

        Bundle changelogBundle = new Bundle();
        changelogBundle.putInt(HelpHtmlFragment.ARG_HTML_FILE, R.raw.help_changelog);
        mTabsAdapter.addTab(HelpHtmlFragment.class, changelogBundle,
                getString(R.string.help_tab_changelog));

        mTabsAdapter.addTab(HelpAboutFragment.class, null,
                getString(R.string.help_tab_about));

        // NOTE: must be after adding the tabs!
        slidingTabLayout.setViewPager(mViewPager);

        // switch to tab selected by extra
        mViewPager.setCurrentItem(selectedTab);
    }
}
