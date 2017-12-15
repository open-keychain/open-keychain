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


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.OvershootInterpolator;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.provider.KeyRepository;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.ui.adapter.PagerTabStripAdapter;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.ui.keyview.ViewKeyActivity;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.ContactHelper;
import org.sufficientlysecure.keychain.util.Log;

public class ViewKeyAdvActivity extends BaseActivity implements
        LoaderCallbacks<Cursor>, OnPageChangeListener {

    KeyRepository mKeyRepository;

    protected Uri mDataUri;

    public static final String EXTRA_SELECTED_TAB = "selected_tab";
    public static final int TAB_START = 0;
    public static final int TAB_SHARE = 1;
    public static final int TAB_IDENTITIES = 2;
    public static final int TAB_SUBKEYS = 3;
    public static final int TAB_CERTS = 4;

    // view
    private ViewPager mViewPager;
    private PagerSlidingTabStrip mSlidingTabLayout;

    private static final int LOADER_ID_UNIFIED = 0;
    private ActionMode mActionMode;
    private boolean mHasSecret;
    private PagerTabStripAdapter mTabAdapter;
    private boolean mActionIconShown;
    private boolean[] mTabsWithActionMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setFullScreenDialogClose(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mKeyRepository = KeyRepository.create(this);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mSlidingTabLayout = (PagerSlidingTabStrip) findViewById(R.id.sliding_tab_layout);

        mDataUri = getIntent().getData();
        if (mDataUri == null) {
            Log.e(Constants.TAG, "Data missing. Should be uri of key!");
            finish();
            return;
        }
        if (mDataUri.getHost().equals(ContactsContract.AUTHORITY)) {
            mDataUri = new ContactHelper(this).dataUriFromContactUri(mDataUri);
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
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.view_key_adv_activity);
    }

    private void initTabs(Uri dataUri) {
        mTabAdapter = new PagerTabStripAdapter(this);
        mViewPager.setAdapter(mTabAdapter);

        // keep track which of these are action mode enabled!
        mTabsWithActionMode = new boolean[5];

        mTabAdapter.addTab(ViewKeyAdvStartFragment.class,
                null, getString(R.string.key_view_tab_start));
        mTabsWithActionMode[0] = false;

        Bundle shareBundle = new Bundle();
        shareBundle.putParcelable(ViewKeyAdvShareFragment.ARG_DATA_URI, dataUri);
        mTabAdapter.addTab(ViewKeyAdvShareFragment.class,
                shareBundle, getString(R.string.key_view_tab_share));
        mTabsWithActionMode[1] = false;

        Bundle userIdsBundle = new Bundle();
        userIdsBundle.putParcelable(ViewKeyAdvUserIdsFragment.ARG_DATA_URI, dataUri);
        mTabAdapter.addTab(ViewKeyAdvUserIdsFragment.class,
                userIdsBundle, getString(R.string.section_user_ids));
        mTabsWithActionMode[2] = true;

        Bundle keysBundle = new Bundle();
        keysBundle.putParcelable(ViewKeyAdvSubkeysFragment.ARG_DATA_URI, dataUri);
        mTabAdapter.addTab(ViewKeyAdvSubkeysFragment.class,
                keysBundle, getString(R.string.key_view_tab_keys));
        mTabsWithActionMode[3] = true;

        Bundle certsBundle = new Bundle();
        certsBundle.putParcelable(ViewKeyAdvCertsFragment.ARG_DATA_URI, dataUri);
        mTabAdapter.addTab(ViewKeyAdvCertsFragment.class,
                certsBundle, getString(R.string.key_view_tab_certs));
        mTabsWithActionMode[4] = false;

        // update layout after operations
        mSlidingTabLayout.setViewPager(mViewPager);
        mSlidingTabLayout.setOnPageChangeListener(this);

        // switch to tab selected by extra
        Intent intent = getIntent();
        int switchToTab = intent.getIntExtra(EXTRA_SELECTED_TAB, TAB_START);
        mViewPager.setCurrentItem(switchToTab);

    }

    // These are the rows that we will retrieve.
    static final String[] PROJECTION = new String[]{
            KeychainContract.KeyRings._ID,
            KeychainContract.KeyRings.MASTER_KEY_ID,
            KeychainContract.KeyRings.USER_ID,
            KeychainContract.KeyRings.IS_REVOKED,
            KeychainContract.KeyRings.IS_EXPIRED,
            KeychainContract.KeyRings.VERIFIED,
            KeychainContract.KeyRings.HAS_ANY_SECRET,
            KeychainContract.KeyRings.FINGERPRINT,
            KeychainContract.KeyRings.NAME,
            KeychainContract.KeyRings.EMAIL,
            KeychainContract.KeyRings.COMMENT,
    };

    static final int INDEX_MASTER_KEY_ID = 1;
    static final int INDEX_USER_ID = 2;
    static final int INDEX_IS_REVOKED = 3;
    static final int INDEX_IS_EXPIRED = 4;
    static final int INDEX_VERIFIED = 5;
    static final int INDEX_HAS_ANY_SECRET = 6;
    static final int INDEX_FINGERPRINT = 7;
    static final int INDEX_NAME = 8;
    static final int INDEX_EMAIL = 9;
    static final int INDEX_COMMENT = 10;

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
                    String name = data.getString(INDEX_NAME);

                    if (name != null) {
                        setTitle(name);
                    } else {
                        setTitle(R.string.user_id_no_name);
                    }

                    byte[] fingerprint = data.getBlob(INDEX_FINGERPRINT);

                    // get key id from MASTER_KEY_ID
                    long masterKeyId = data.getLong(INDEX_MASTER_KEY_ID);
                    String formattedKeyId = KeyFormattingUtils.beautifyKeyIdWithPrefix(masterKeyId);
                    getSupportActionBar().setSubtitle(formattedKeyId);

                    mHasSecret = data.getInt(INDEX_HAS_ANY_SECRET) != 0;
                    boolean isRevoked = data.getInt(INDEX_IS_REVOKED) > 0;
                    boolean isExpired = data.getInt(INDEX_IS_EXPIRED) != 0;
                    boolean isVerified = data.getInt(INDEX_VERIFIED) > 0;

                    // Note: order is important
                    int color;
                    if (isRevoked || isExpired) {
                        color = getResources().getColor(R.color.key_flag_red);
                    } else if (mHasSecret) {
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (!mHasSecret) {
            return false;
        }

        // always add the item, switch its visibility depending on fragment
        getMenuInflater().inflate(R.menu.action_mode_edit, menu);
        final MenuItem vActionModeItem = menu.findItem(R.id.menu_action_mode_edit);

        boolean isCurrentActionFragment = mTabsWithActionMode[mViewPager.getCurrentItem()];

        // if the state is as it should be, never mind
        if (isCurrentActionFragment == mActionIconShown) {
            return isCurrentActionFragment;
        }

        // show or hide accordingly
        mActionIconShown = isCurrentActionFragment;
        vActionModeItem.setEnabled(isCurrentActionFragment);
        animateMenuItem(vActionModeItem, isCurrentActionFragment);

        return true;
    }

    private void animateMenuItem(final MenuItem vEditSubkeys, final boolean animateShow) {

        View actionView = LayoutInflater.from(this).inflate(R.layout.edit_icon, null);
        vEditSubkeys.setActionView(actionView);
        actionView.setTranslationX(animateShow ? 150 : 0);

        ViewPropertyAnimator animator = actionView.animate();
        animator.translationX(animateShow ? 0 : 150);
        animator.setDuration(300);
        animator.setInterpolator(new OvershootInterpolator(1.5f));
        animator.setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!animateShow) {
                    vEditSubkeys.setVisible(false);
                }
                vEditSubkeys.setActionView(null);
            }
        });
        animator.start();

    }

    @Override
    public void onActionModeStarted(final ActionMode mode) {
        super.onActionModeStarted(mode);
        mActionMode = mode;
    }

    @Override
    public void onActionModeFinished(ActionMode mode) {
        super.onActionModeFinished(mode);
        mActionMode = null;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        if (mActionMode != null) {
            mActionMode.finish();
            mActionMode = null;
        }
        invalidateOptionsMenu();
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

}
