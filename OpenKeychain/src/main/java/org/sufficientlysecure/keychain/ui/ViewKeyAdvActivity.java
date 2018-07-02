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


import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Transformations;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.OvershootInterpolator;

import com.astuetz.PagerSlidingTabStrip;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.livedata.GenericLiveData;
import org.sufficientlysecure.keychain.model.SubKey;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.model.UserPacket.UserId;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.daos.KeyRepository;
import org.sufficientlysecure.keychain.ui.adapter.PagerTabStripAdapter;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.ui.keyview.ViewKeyActivity;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;


public class ViewKeyAdvActivity extends BaseActivity implements OnPageChangeListener {
    public static final String EXTRA_MASTER_KEY_ID = "master_key_id";
    public static final String EXTRA_SELECTED_TAB = "selected_tab";

    KeyRepository keyRepository;

    // view
    private ViewPager mViewPager;
    private PagerSlidingTabStrip mSlidingTabLayout;

    private ActionMode mActionMode;
    private boolean hasSecret;
    private boolean mActionIconShown;

    enum ViewKeyAdvTab {
        START(ViewKeyAdvStartFragment.class, R.string.key_view_tab_start, false),
        SHARE(ViewKeyAdvShareFragment.class, R.string.key_view_tab_share, false),
        IDENTITIES(ViewKeyAdvUserIdsFragment.class, R.string.section_user_ids, true),
        SUBKEYS(ViewKeyAdvSubkeysFragment.class, R.string.key_view_tab_keys, true);

        public final Class<? extends Fragment> fragmentClass;
        public final int titleRes;
        public final boolean hasActionMode;

        ViewKeyAdvTab(Class<? extends Fragment> fragmentClass, @StringRes int titleRes, boolean hasActionMode) {
            this.titleRes = titleRes;
            this.fragmentClass = fragmentClass;
            this.hasActionMode = hasActionMode;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setFullScreenDialogClose(v -> finish());

        keyRepository = KeyRepository.create(this);

        mViewPager = findViewById(R.id.pager);
        mSlidingTabLayout = findViewById(R.id.sliding_tab_layout);

        if (!getIntent().hasExtra(EXTRA_MASTER_KEY_ID)) {
            throw new IllegalArgumentException("Missing required extra master_key_id");
        }

        ViewKeyAdvViewModel viewModel = ViewModelProviders.of(this).get(ViewKeyAdvViewModel.class);
        viewModel.setMasterKeyId(getIntent().getLongExtra(EXTRA_MASTER_KEY_ID, 0L));
        viewModel.getUnifiedKeyInfoLiveData(getApplicationContext()).observe(this, this::onLoadUnifiedKeyInfo);

        initTabs();
    }

    public static class ViewKeyAdvViewModel extends ViewModel {
        private Long masterKeyId;
        private LiveData<UnifiedKeyInfo> unifiedKeyInfoLiveData;
        private LiveData<List<SubKey>> subKeyLiveData;
        private LiveData<List<UserId>> userIdsLiveData;

        void setMasterKeyId(long masterKeyId) {
            if (this.masterKeyId != null) {
                throw new IllegalStateException("cannot change masterKeyId once set!");
            }
            this.masterKeyId = masterKeyId;
        }

        LiveData<UnifiedKeyInfo> getUnifiedKeyInfoLiveData(Context context) {
            if (masterKeyId == null) {
                throw new IllegalStateException("masterKeyId must be set to retrieve this!");
            }
            if (unifiedKeyInfoLiveData == null) {
                KeyRepository keyRepository = KeyRepository.create(context);
                unifiedKeyInfoLiveData = new GenericLiveData<>(context, masterKeyId,
                        () -> keyRepository.getUnifiedKeyInfo(masterKeyId));
            }
            return unifiedKeyInfoLiveData;
        }

        LiveData<List<SubKey>> getSubkeyLiveData(Context context) {
            if (subKeyLiveData == null) {
                KeyRepository keyRepository = KeyRepository.create(context);
                subKeyLiveData = Transformations.switchMap(getUnifiedKeyInfoLiveData(context),
                        (unifiedKeyInfo) -> unifiedKeyInfo == null ? null : new GenericLiveData<>(context,
                                () -> keyRepository.getSubKeysByMasterKeyId(unifiedKeyInfo.master_key_id())));
            }
            return subKeyLiveData;
        }

        LiveData<List<UserId>> getUserIdLiveData(Context context) {
            if (userIdsLiveData == null) {
                KeyRepository keyRepository = KeyRepository.create(context);
                userIdsLiveData = Transformations.switchMap(getUnifiedKeyInfoLiveData(context),
                        (unifiedKeyInfo) -> unifiedKeyInfo == null ? null : new GenericLiveData<>(context,
                                () -> keyRepository.getUserIds(unifiedKeyInfo.master_key_id())));
            }
            return userIdsLiveData;
        }
    }

    public void onLoadUnifiedKeyInfo(UnifiedKeyInfo unifiedKeyInfo) {
        if (unifiedKeyInfo == null) {
            return;
        }

        if (unifiedKeyInfo.name() != null) {
            setTitle(unifiedKeyInfo.name());
        } else {
            setTitle(R.string.user_id_no_name);
        }

        String formattedKeyId = KeyFormattingUtils.beautifyKeyIdWithPrefix(unifiedKeyInfo.master_key_id());
        mToolbar.setSubtitle(formattedKeyId);

        hasSecret = unifiedKeyInfo.has_any_secret();

        // Note: order is important
        int color;
        if (unifiedKeyInfo.is_revoked() || unifiedKeyInfo.is_expired()) {
            color = getResources().getColor(R.color.key_flag_red);
        } else if (unifiedKeyInfo.has_any_secret()) {
            color = getResources().getColor(R.color.android_green_light);
        } else {
            if (unifiedKeyInfo.is_verified()) {
                color = getResources().getColor(R.color.android_green_light);
            } else {
                color = getResources().getColor(R.color.key_flag_orange);
            }
        }
        mToolbar.setBackgroundColor(color);
        mStatusBar.setBackgroundColor(ViewKeyActivity.getStatusBarBackgroundColor(color));
        mSlidingTabLayout.setBackgroundColor(color);

        invalidateOptionsMenu();
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.view_key_adv_activity);
    }

    private void initTabs() {
        PagerTabStripAdapter tabAdapter = new PagerTabStripAdapter(this);
        mViewPager.setAdapter(tabAdapter);

        for (ViewKeyAdvTab tab : ViewKeyAdvTab.values()) {
            tabAdapter.addTab(tab.fragmentClass, null, getString(tab.titleRes));
        }

        // update layout after operations
        mSlidingTabLayout.setViewPager(mViewPager);
        mSlidingTabLayout.setOnPageChangeListener(this);

        // switch to tab selected by extra
        Intent intent = getIntent();
        int switchToTab = intent.getIntExtra(EXTRA_SELECTED_TAB, 0);
        mViewPager.setCurrentItem(switchToTab);
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
        if (!hasSecret) {
            return false;
        }

        // always add the item, switch its visibility depending on fragment
        getMenuInflater().inflate(R.menu.action_mode_edit, menu);
        final MenuItem vActionModeItem = menu.findItem(R.id.menu_action_mode_edit);

        boolean isCurrentActionFragment = ViewKeyAdvTab.values()[mViewPager.getCurrentItem()].hasActionMode;

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
