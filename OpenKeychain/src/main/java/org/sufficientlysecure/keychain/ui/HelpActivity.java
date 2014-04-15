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
import org.sufficientlysecure.keychain.ui.adapter.TabsAdapter;

public class HelpActivity extends ActionBarActivity {
    public static final String EXTRA_SELECTED_TAB = "selected_tab";

    ViewPager mViewPager;
    TabsAdapter mTabsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.help_activity);

        mViewPager = (ViewPager) findViewById(R.id.pager);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setHomeButtonEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        mTabsAdapter = new TabsAdapter(this, mViewPager);

        int selectedTab = 0;
        Intent intent = getIntent();
        if (intent.getExtras() != null && intent.getExtras().containsKey(EXTRA_SELECTED_TAB)) {
            selectedTab = intent.getExtras().getInt(EXTRA_SELECTED_TAB);
        }

        Bundle startBundle = new Bundle();
        startBundle.putInt(HelpHtmlFragment.ARG_HTML_FILE, R.raw.help_start);
        mTabsAdapter.addTab(actionBar.newTab().setText(getString(R.string.help_tab_start)),
                HelpHtmlFragment.class, startBundle, (selectedTab == 0));

        Bundle faqBundle = new Bundle();
        faqBundle.putInt(HelpHtmlFragment.ARG_HTML_FILE, R.raw.help_faq);
        mTabsAdapter.addTab(actionBar.newTab().setText(getString(R.string.help_tab_faq)),
                HelpHtmlFragment.class, faqBundle, (selectedTab == 1));

        Bundle wotBundle = new Bundle();
        wotBundle.putInt(HelpHtmlFragment.ARG_HTML_FILE, R.raw.help_wot);
        mTabsAdapter.addTab(actionBar.newTab().setText(getString(R.string.help_tab_wot)),
                HelpHtmlFragment.class, wotBundle, (selectedTab == 2));

        Bundle nfcBundle = new Bundle();
        nfcBundle.putInt(HelpHtmlFragment.ARG_HTML_FILE, R.raw.help_nfc_beam);
        mTabsAdapter.addTab(actionBar.newTab().setText(getString(R.string.help_tab_nfc_beam)),
                HelpHtmlFragment.class, nfcBundle, (selectedTab == 3));

        Bundle changelogBundle = new Bundle();
        changelogBundle.putInt(HelpHtmlFragment.ARG_HTML_FILE, R.raw.help_changelog);
        mTabsAdapter.addTab(actionBar.newTab().setText(getString(R.string.help_tab_changelog)),
                HelpHtmlFragment.class, changelogBundle, (selectedTab == 4));

        mTabsAdapter.addTab(actionBar.newTab().setText(getString(R.string.help_tab_about)),
                HelpAboutFragment.class, null, (selectedTab == 5));
    }
}
