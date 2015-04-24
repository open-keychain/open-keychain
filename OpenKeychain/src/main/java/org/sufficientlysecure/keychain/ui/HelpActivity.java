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
import android.view.View;

import com.astuetz.PagerSlidingTabStrip;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.adapter.PagerTabStripAdapter;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;


public class HelpActivity extends BaseActivity {
    public static final String EXTRA_SELECTED_TAB = "selected_tab";

    public static final int TAB_START = 0;
    public static final int TAB_FAQ = 1;
    public static final int TAB_TRUST = 2;
    public static final int TAB_CHANGELOG = 3;
    public static final int TAB_ABOUT = 4;

    ViewPager mViewPager;
    private PagerTabStripAdapter mTabsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mViewPager = (ViewPager) findViewById(R.id.pager);
        PagerSlidingTabStrip slidingTabLayout =
                (PagerSlidingTabStrip) findViewById(R.id.sliding_tab_layout);

        mTabsAdapter = new PagerTabStripAdapter(this);
        mViewPager.setAdapter(mTabsAdapter);

        int selectedTab = TAB_START;
        Intent intent = getIntent();
        if (intent.getExtras() != null && intent.getExtras().containsKey(EXTRA_SELECTED_TAB)) {
            selectedTab = intent.getExtras().getInt(EXTRA_SELECTED_TAB);
        }

        Bundle startBundle = new Bundle();
        startBundle.putInt(HelpMarkdownFragment.ARG_MARKDOWN_RES, R.raw.help_start);
        mTabsAdapter.addTab(HelpMarkdownFragment.class, startBundle,
                getString(R.string.help_tab_start));

        Bundle certificationBundle = new Bundle();
        certificationBundle.putInt(HelpMarkdownFragment.ARG_MARKDOWN_RES, R.raw.help_certification);
        mTabsAdapter.addTab(HelpMarkdownFragment.class, certificationBundle,
                getString(R.string.help_tab_wot));

        Bundle faqBundle = new Bundle();
        faqBundle.putInt(HelpMarkdownFragment.ARG_MARKDOWN_RES, R.raw.help_faq);
        mTabsAdapter.addTab(HelpMarkdownFragment.class, faqBundle,
                getString(R.string.help_tab_faq));

        Bundle changelogBundle = new Bundle();
        changelogBundle.putInt(HelpMarkdownFragment.ARG_MARKDOWN_RES, R.raw.help_changelog);
        mTabsAdapter.addTab(HelpMarkdownFragment.class, changelogBundle,
                getString(R.string.help_tab_changelog));

        mTabsAdapter.addTab(HelpAboutFragment.class, null,
                getString(R.string.help_tab_about));

        // NOTE: must be after adding the tabs!
        slidingTabLayout.setViewPager(mViewPager);

        // switch to tab selected by extra
        mViewPager.setCurrentItem(selectedTab);
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.help_activity);
    }
}
