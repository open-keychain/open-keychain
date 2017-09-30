/*
 * Copyright (C) 2017 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

package org.sufficientlysecure.keychain.ui.keyview.presenter;


import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.View;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.provider.AutocryptPeerDataAccessObject;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserPackets;
import org.sufficientlysecure.keychain.ui.EditIdentitiesActivity;
import org.sufficientlysecure.keychain.ui.adapter.IdentityAdapter;
import org.sufficientlysecure.keychain.ui.adapter.IdentityAdapter.IdentityClickListener;
import org.sufficientlysecure.keychain.ui.dialog.UserIdInfoDialogFragment;
import org.sufficientlysecure.keychain.ui.keyview.LinkedIdViewFragment;
import org.sufficientlysecure.keychain.ui.keyview.loader.IdentityLoader;
import org.sufficientlysecure.keychain.ui.keyview.loader.IdentityLoader.IdentityInfo;
import org.sufficientlysecure.keychain.ui.keyview.loader.IdentityLoader.LinkedIdInfo;
import org.sufficientlysecure.keychain.ui.keyview.loader.IdentityLoader.TrustIdInfo;
import org.sufficientlysecure.keychain.ui.keyview.loader.IdentityLoader.UserIdInfo;
import org.sufficientlysecure.keychain.ui.linked.LinkedIdWizard;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Preferences;


public class IdentitiesPresenter implements LoaderCallbacks<List<IdentityInfo>> {
    private final Context context;
    private final IdentitiesMvpView view;
    private final ViewKeyMvpView viewKeyMvpView;
    private final int loaderId;

    private final IdentityAdapter identitiesAdapter;

    private final long masterKeyId;
    private final boolean isSecret;
    private final boolean showLinkedIds;

    public IdentitiesPresenter(Context context, IdentitiesMvpView view, ViewKeyMvpView viewKeyMvpView,
            int loaderId, long masterKeyId, boolean isSecret) {
        this.context = context;
        this.view = view;
        this.viewKeyMvpView = viewKeyMvpView;
        this.loaderId = loaderId;

        this.masterKeyId = masterKeyId;
        this.isSecret = isSecret;

        showLinkedIds = Preferences.getPreferences(context).getExperimentalEnableLinkedIdentities();

        identitiesAdapter = new IdentityAdapter(context, isSecret, new IdentityClickListener() {
            @Override
            public void onClickIdentity(int position) {
                showIdentityInfo(position);
            }

            @Override
            public void onClickIdentityMore(int position, View anchor) {
                showIdentityContextMenu(position, anchor);

            }
        });
        view.setIdentitiesAdapter(identitiesAdapter);

        view.setEditIdentitiesButtonVisible(isSecret);
        view.setAddLinkedIdButtonVisible(showLinkedIds);

        view.setIdentitiesCardListener(new IdentitiesCardListener() {
            @Override
            public void onClickEditIdentities() {
                editIdentities();
            }

            @Override
            public void onClickAddIdentity() {
                addLinkedIdentity();
            }
        });
    }

    public void startLoader(LoaderManager loaderManager) {
        loaderManager.restartLoader(loaderId, null, this);
    }

    @Override
    public Loader<List<IdentityInfo>> onCreateLoader(int id, Bundle args) {
        return new IdentityLoader(context, context.getContentResolver(), masterKeyId, showLinkedIds);
    }

    @Override
    public void onLoadFinished(Loader<List<IdentityInfo>> loader, List<IdentityInfo> data) {
        viewKeyMvpView.setContentShown(true, false);
        identitiesAdapter.setData(data);
    }

    @Override
    public void onLoaderReset(Loader loader) {
        identitiesAdapter.setData(null);
    }

    private void showIdentityInfo(final int position) {
        IdentityInfo info = identitiesAdapter.getInfo(position);
        if (info instanceof LinkedIdInfo) {
            showLinkedId((LinkedIdInfo) info);
        } else if (info instanceof UserIdInfo) {
            showUserIdInfo((UserIdInfo) info);
        } else if (info instanceof TrustIdInfo) {
            Intent autocryptPeerIntent = ((TrustIdInfo) info).getTrustIdIntent();
            if (autocryptPeerIntent != null) {
                viewKeyMvpView.startActivity(autocryptPeerIntent);
            }
        }
    }

    private void showIdentityContextMenu(int position, View anchor) {
        viewKeyMvpView.showContextMenu(position, anchor);
    }

    private void showLinkedId(final LinkedIdInfo info) {
        final LinkedIdViewFragment frag;
        try {
            Uri dataUri = UserPackets.buildLinkedIdsUri(KeyRings.buildGenericKeyRingUri(masterKeyId));
            frag = LinkedIdViewFragment.newInstance(dataUri, info.getRank(), isSecret, masterKeyId);
        } catch (IOException e) {
            Log.e(Constants.TAG, "IOException", e);
            return;
        }

        viewKeyMvpView.switchToFragment(frag, "linked_id");
    }

    private void showUserIdInfo(UserIdInfo info) {
        if (!isSecret) {
            final int isVerified = info.getVerified();

            UserIdInfoDialogFragment dialogFragment = UserIdInfoDialogFragment.newInstance(false, isVerified);
            viewKeyMvpView.showDialogFragment(dialogFragment, "userIdInfoDialog");
        }
    }

    private void editIdentities() {
        Intent editIntent = new Intent(context, EditIdentitiesActivity.class);
        editIntent.setData(KeychainContract.KeyRingData.buildSecretKeyRingUri(masterKeyId));
        viewKeyMvpView.startActivityAndShowResultSnackbar(editIntent);
    }

    private void addLinkedIdentity() {
        Intent intent = new Intent(context, LinkedIdWizard.class);
        intent.setData(KeyRings.buildUnifiedKeyRingUri(masterKeyId));
        context.startActivity(intent);
    }

    public void onClickForgetIdentity(int position) {
        TrustIdInfo info = (TrustIdInfo) identitiesAdapter.getInfo(position);
        if (info == null) {
            Log.e(Constants.TAG, "got a 'forget' click on a bad trust id");
            return;
        }

        AutocryptPeerDataAccessObject autocryptPeerDao =
                new AutocryptPeerDataAccessObject(context, info.getPackageName());
        autocryptPeerDao.delete(info.getTrustId());
    }

    public interface IdentitiesMvpView {
        void setIdentitiesAdapter(IdentityAdapter userIdsAdapter);
        void setIdentitiesCardListener(IdentitiesCardListener identitiesCardListener);
        void setEditIdentitiesButtonVisible(boolean show);
        void setAddLinkedIdButtonVisible(boolean showLinkedIds);
    }

    public interface IdentitiesCardListener {
        void onClickEditIdentities();
        void onClickAddIdentity();
    }
}