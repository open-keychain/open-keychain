/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.adapter.PagerTabStripAdapter;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.ContactHelper;
import org.sufficientlysecure.keychain.util.ExportHelper;
import org.sufficientlysecure.keychain.util.Log;

public class ViewKeyAdvActivity extends BaseActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    ExportHelper mExportHelper;
    ProviderHelper mProviderHelper;

    protected Uri mDataUri;

    public static final String EXTRA_SELECTED_TAB = "selected_tab";
    public static final int TAB_SHARE = 0;
    public static final int TAB_IDENTITIES = 1;
    public static final int TAB_SUBKEYS = 2;
    public static final int TAB_CERTS = 3;
    public static final int TAB_KEYBASE = 4;

    // view
    private ViewPager mViewPager;
    private PagerSlidingTabStrip mSlidingTabLayout;

    private static final int LOADER_ID_UNIFIED = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setFullScreenDialogClose(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mExportHelper = new ExportHelper(this);
        mProviderHelper = new ProviderHelper(this);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mSlidingTabLayout = (PagerSlidingTabStrip) findViewById(R.id.sliding_tab_layout);

        Intent intent = getIntent();
        int switchToTab = intent.getIntExtra(EXTRA_SELECTED_TAB, TAB_SHARE);

        mDataUri = getIntent().getData();
        if (mDataUri == null) {
            Log.e(Constants.TAG, "Data missing. Should be uri of key!");
            finish();
            return;
        }
        if (mDataUri.getHost().equals(ContactsContract.AUTHORITY)) {
            mDataUri = ContactHelper.dataUriFromContactUri(this, mDataUri);
            if (mDataUri == null) {
                Log.e(Constants.TAG, "Contact Data missing. Should be uri of key!");
                Toast.makeText(this, R.string.error_contacts_key_id_missing, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }

        // Prepare the loaders. Either re-connect with an existing ones,
        // or start new ones.
        getSupportLoaderManager().initLoader(LOADER_ID_UNIFIED, null, this);

        initTabs(mDataUri);

        // switch to tab selected by extra
        mViewPager.setCurrentItem(switchToTab);
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.view_key_adv_activity);
    }

    private void initTabs(Uri dataUri) {
        PagerTabStripAdapter adapter = new PagerTabStripAdapter(this);
        mViewPager.setAdapter(adapter);

        Bundle shareBundle = new Bundle();
        shareBundle.putParcelable(ViewKeyAdvUserIdsFragment.ARG_DATA_URI, dataUri);
        adapter.addTab(ViewKeyAdvShareFragment.class,
                shareBundle, getString(R.string.key_view_tab_share));

        Bundle userIdsBundle = new Bundle();
        userIdsBundle.putParcelable(ViewKeyAdvUserIdsFragment.ARG_DATA_URI, dataUri);
        adapter.addTab(ViewKeyAdvUserIdsFragment.class,
                userIdsBundle, getString(R.string.section_user_ids));

        Bundle keysBundle = new Bundle();
        keysBundle.putParcelable(ViewKeyAdvSubkeysFragment.ARG_DATA_URI, dataUri);
        adapter.addTab(ViewKeyAdvSubkeysFragment.class,
                keysBundle, getString(R.string.key_view_tab_keys));

        Bundle certsBundle = new Bundle();
        certsBundle.putParcelable(ViewKeyAdvCertsFragment.ARG_DATA_URI, dataUri);
        adapter.addTab(ViewKeyAdvCertsFragment.class,
                certsBundle, getString(R.string.key_view_tab_certs));

        Bundle trustBundle = new Bundle();
        trustBundle.putParcelable(ViewKeyTrustFragment.ARG_DATA_URI, dataUri);
        adapter.addTab(ViewKeyTrustFragment.class,
                trustBundle, getString(R.string.key_view_tab_keybase));

        // update layout after operations
        mSlidingTabLayout.setViewPager(mViewPager);
    }

    // These are the rows that we will retrieve.
    static final String[] PROJECTION = new String[]{
            KeychainContract.KeyRings._ID,
            KeychainContract.KeyRings.MASTER_KEY_ID,
            KeychainContract.KeyRings.USER_ID,
            KeychainContract.KeyRings.IS_REVOKED,
            KeychainContract.KeyRings.IS_EXPIRED,
            KeychainContract.KeyRings.VERIFIED,
            KeychainContract.KeyRings.HAS_ANY_SECRET
    };

    static final int INDEX_MASTER_KEY_ID = 1;
    static final int INDEX_USER_ID = 2;
    static final int INDEX_IS_REVOKED = 3;
    static final int INDEX_IS_EXPIRED = 4;
    static final int INDEX_VERIFIED = 5;
    static final int INDEX_HAS_ANY_SECRET = 6;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_UNIFIED: {
                Uri baseUri = KeychainContract.KeyRings.buildUnifiedKeyRingUri(mDataUri);
                return new CursorLoader(this, baseUri, PROJECTION, null, null, null);
            }

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Avoid NullPointerExceptions...
        if (data == null || data.getCount() == 0) {
            return;
        }
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        switch (loader.getId()) {
            case LOADER_ID_UNIFIED: {
                if (data.moveToFirst()) {
                    // get name, email, and comment from USER_ID
                    KeyRing.UserId mainUserId = KeyRing.splitUserId(data.getString(INDEX_USER_ID));
                    if (mainUserId.name != null) {
                        setTitle(mainUserId.name);
                    } else {
                        setTitle(R.string.user_id_no_name);
                    }

                    // get key id from MASTER_KEY_ID
                    long masterKeyId = data.getLong(INDEX_MASTER_KEY_ID);
                    getSupportActionBar().setSubtitle(KeyFormattingUtils.beautifyKeyIdWithPrefix(this, masterKeyId));

                    boolean isSecret = data.getInt(INDEX_HAS_ANY_SECRET) != 0;
                    boolean isRevoked = data.getInt(INDEX_IS_REVOKED) > 0;
                    boolean isExpired = data.getInt(INDEX_IS_EXPIRED) != 0;
                    boolean isVerified = data.getInt(INDEX_VERIFIED) > 0;

                    // Note: order is important
                    int color;
                    if (isRevoked || isExpired) {
                        color = getResources().getColor(R.color.key_flag_red);
                    } else if (isSecret) {
                        color = getResources().getColor(R.color.android_green_light);
                    } else {
                        if (isVerified) {
                            color = getResources().getColor(R.color.android_green_light);
                        } else {
                            color = getResources().getColor(R.color.key_flag_orange);
                        }
                    }
                    mToolbar.setBackgroundColor(color);
                    mStatusBar.setBackgroundColor(ViewKeyActivity.getStatusBarBackgroundColor(color));
                    mSlidingTabLayout.setBackgroundColor(color);

                    break;
                }
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // if a result has been returned, display a notify
        if (data != null && data.hasExtra(OperationResult.EXTRA_RESULT)) {
            OperationResult result = data.getParcelableExtra(OperationResult.EXTRA_RESULT);
            result.createNotify(this).show();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
