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


import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;

import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.ui.EditIdentitiesActivity;
import org.sufficientlysecure.keychain.ui.adapter.UserIdsAdapter;
import org.sufficientlysecure.keychain.ui.dialog.UserIdInfoDialogFragment;


public class IdentitiesPresenter implements LoaderCallbacks<Cursor> {
    private final Context context;
    private final IdentitiesMvpView view;
    private final ViewKeyMvpView viewKeyMvpView;
    private final int loaderId;

    private final UserIdsAdapter userIdsAdapter;

    private final long masterKeyId;
    private final boolean isSecret;

    public IdentitiesPresenter(Context context, IdentitiesMvpView view, ViewKeyMvpView viewKeyMvpView,
            int loaderId, long masterKeyId, boolean isSecret) {
        this.context = context;
        this.view = view;
        this.viewKeyMvpView = viewKeyMvpView;
        this.loaderId = loaderId;

        this.masterKeyId = masterKeyId;
        this.isSecret = isSecret;

        userIdsAdapter = new UserIdsAdapter(context, null, 0, !isSecret);
        view.setUserIdsAdapter(userIdsAdapter);

        view.setEditIdentitiesButtonVisible(isSecret);

        view.setIdentitiesCardListener(new IdentitiesCardListener() {
            @Override
            public void onIdentityItemClick(int position) {
                showUserIdInfo(position);
            }

            @Override
            public void onClickEditIdentities() {
                editIdentities();
            }
        });
    }

    public void startLoader(LoaderManager loaderManager) {
        loaderManager.restartLoader(loaderId, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return UserIdsAdapter.createLoader(context, KeyRings.buildUnifiedKeyRingUri(masterKeyId));
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        viewKeyMvpView.setContentShown(true, false);
        userIdsAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader loader) {
        userIdsAdapter.swapCursor(null);
    }

    private void showUserIdInfo(final int position) {
        if (!isSecret) {
            final boolean isRevoked = userIdsAdapter.getIsRevoked(position);
            final int isVerified = userIdsAdapter.getIsVerified(position);

            UserIdInfoDialogFragment dialogFragment = UserIdInfoDialogFragment.newInstance(isRevoked, isVerified);
            viewKeyMvpView.showDialogFragment(dialogFragment, "userIdInfoDialog");
        }
    }

    private void editIdentities() {
        Intent editIntent = new Intent(context, EditIdentitiesActivity.class);
        editIntent.setData(KeychainContract.KeyRingData.buildSecretKeyRingUri(masterKeyId));
        viewKeyMvpView.startActivityAndShowResultSnackbar(editIntent);
    }

    public interface IdentitiesMvpView {
        void setUserIdsAdapter(UserIdsAdapter userIdsAdapter);
        void setIdentitiesCardListener(IdentitiesCardListener identitiesCardListener);
        void setEditIdentitiesButtonVisible(boolean show);
    }

    public interface IdentitiesCardListener {
        void onIdentityItemClick(int position);
        void onClickEditIdentities();
    }
}