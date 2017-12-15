/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.View;

import com.astuetz.PagerSlidingTabStrip;

import org.sufficientlysecure.donations.DonationsFragment;
import org.sufficientlysecure.keychain.BuildConfig;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.adapter.PagerTabStripAdapter;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;


public class HelpActivity extends BaseActivity {
    public static final String EXTRA_SELECTED_TAB = "selected_tab";

    public static final int TAB_START = 0;
    public static final int TAB_CONFIRM = 1;
    public static final int TAB_FAQ = 2;
    public static final int TAB_DONATE = 3;
    public static final int TAB_CHANGELOG = 4;
    public static final int TAB_LICENSES = 5;
    public static final int TAB_ABOUT = 6;

    // Google Play
    private static final String[] GOOGLE_PLAY_CATALOG = new String[]{"keychain.donation.1",
            "keychain.donation.2", "keychain.donation.3", "keychain.donation.5", "keychain.donation.10",
            "keychain.donation.50", "keychain.donation.100"};

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

        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        PagerSlidingTabStrip slidingTabLayout =
                (PagerSlidingTabStrip) findViewById(R.id.sliding_tab_layout);

        mTabsAdapter = new PagerTabStripAdapter(this);
        viewPager.setAdapter(mTabsAdapter);

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

        Bundle donationsBundle = new Bundle();
        donationsBundle.putBoolean(DonationsFragment.ARG_DEBUG, Constants.DEBUG);
        if (BuildConfig.DONATIONS_GOOGLE) {
            donationsBundle.putBoolean(DonationsFragment.ARG_GOOGLE_ENABLED, true);
            donationsBundle.putString(DonationsFragment.ARG_GOOGLE_PUBKEY, BuildConfig.GOOGLE_PLAY_PUBKEY);
            donationsBundle.putStringArray(DonationsFragment.ARG_GOOGLE_CATALOG, GOOGLE_PLAY_CATALOG);
            donationsBundle.putStringArray(DonationsFragment.ARG_GOOGLE_CATALOG_VALUES,
                    getResources().getStringArray(R.array.help_donation_google_catalog_values));
        } else {
            donationsBundle.putBoolean(DonationsFragment.ARG_PAYPAL_ENABLED, true);
            donationsBundle.putString(DonationsFragment.ARG_PAYPAL_CURRENCY_CODE, BuildConfig.PAYPAL_CURRENCY_CODE);
            donationsBundle.putString(DonationsFragment.ARG_PAYPAL_USER, BuildConfig.PAYPAL_USER);
            donationsBundle.putString(DonationsFragment.ARG_PAYPAL_ITEM_NAME,
                    getString(R.string.help_donation_paypal_item));
            donationsBundle.putBoolean(DonationsFragment.ARG_FLATTR_ENABLED, true);
            donationsBundle.putString(DonationsFragment.ARG_FLATTR_PROJECT_URL, BuildConfig.FLATTR_PROJECT_URL);
            donationsBundle.putString(DonationsFragment.ARG_FLATTR_URL, BuildConfig.FLATTR_URL);
            donationsBundle.putBoolean(DonationsFragment.ARG_BITCOIN_ENABLED, true);
            donationsBundle.putString(DonationsFragment.ARG_BITCOIN_ADDRESS, BuildConfig.BITCOIN_ADDRESS);
        }
        mTabsAdapter.addTab(DonationsFragment.class, donationsBundle,
                getString(R.string.help_tab_donations));

        Bundle changelogBundle = new Bundle();
        changelogBundle.putInt(HelpMarkdownFragment.ARG_MARKDOWN_RES, R.raw.help_changelog);
        mTabsAdapter.addTab(HelpMarkdownFragment.class, changelogBundle,
                getString(R.string.help_tab_changelog));

        Bundle licensesBundle = new Bundle();
        licensesBundle.putInt(HelpMarkdownFragment.ARG_MARKDOWN_RES, R.raw.help_licenses);
        mTabsAdapter.addTab(HelpMarkdownFragment.class, licensesBundle,
                getString(R.string.help_tab_license));

        mTabsAdapter.addTab(HelpAboutFragment.class, null,
                getString(R.string.help_tab_about));

        // NOTE: must be after adding the tabs!
        slidingTabLayout.setViewPager(viewPager);

        // switch to tab selected by extra
        viewPager.setCurrentItem(selectedTab);
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.help_activity);
    }

    public static void startHelpActivity(Context context, int code) {
        Intent intent = new Intent(context, HelpActivity.class);
        intent.putExtra(HelpActivity.EXTRA_SELECTED_TAB, code);
        context.startActivity(intent);
    }

    /**
     * Needed for Google Play In-app Billing. It uses startIntentSenderForResult(). The result is not propagated to
     * the Fragment like in startActivityForResult(). Thus we need to propagate manually to our Fragment.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Fragment fragment = mTabsAdapter.getRegisteredFragment(TAB_DONATE);
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

}
