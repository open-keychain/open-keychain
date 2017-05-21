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

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.ui.adapter.LinkedIdsAdapter;
import org.sufficientlysecure.keychain.ui.keyview.LinkedIdViewFragment;
import org.sufficientlysecure.keychain.ui.linked.LinkedIdWizard;
import org.sufficientlysecure.keychain.util.Log;


public class LinkedIdentitiesPresenter implements LoaderCallbacks<Cursor> {
    private final Context context;
    private final LinkedIdsMvpView view;
    private final int loaderId;
    private final LinkedIdsFragMvpView fragView;

    private LinkedIdsAdapter linkedIdsAdapter;

    private final long masterKeyId;
    private final boolean isSecret;

    public LinkedIdentitiesPresenter(
            Context context, LinkedIdsMvpView view, LinkedIdsFragMvpView fragView, int loaderId, long masterKeyId, boolean isSecret) {
        this.context = context;
        this.view = view;
        this.fragView = fragView;
        this.loaderId = loaderId;

        this.masterKeyId = masterKeyId;
        this.isSecret = isSecret;

        linkedIdsAdapter = new LinkedIdsAdapter(context, null, 0, isSecret);
        view.setLinkedIdsAdapter(linkedIdsAdapter);

        view.setSystemContactClickListener(new LinkedIdsClickListener() {
            @Override
            public void onLinkedIdItemClick(int position) {
                showLinkedId(position);
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
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return LinkedIdsAdapter.createLoader(context, KeyRings.buildUnifiedKeyRingUri(masterKeyId));
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        linkedIdsAdapter.swapCursor(data);

        boolean hasLinkedIdentities = linkedIdsAdapter.getCount() > 0;
        if (isSecret) {
            view.showCard(true);
            view.showEmptyView(!hasLinkedIdentities);
        } else {
            view.showCard(hasLinkedIdentities);
            view.showEmptyView(false);
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {
        linkedIdsAdapter.swapCursor(null);
    }

    private void showLinkedId(final int position) {
        final LinkedIdViewFragment frag;
        try {
            frag = linkedIdsAdapter.getLinkedIdFragment(position, masterKeyId);
        } catch (IOException e) {
            Log.e(Constants.TAG, "IOException", e);
            return;
        }

        fragView.switchToFragment(frag, "linked_id");
    }

    public interface LinkedIdsMvpView {
        void setSystemContactClickListener(LinkedIdsClickListener linkedIdsClickListener);
        void setLinkedIdsAdapter(LinkedIdsAdapter linkedIdsAdapter);

        void showCard(boolean visible);
        void showEmptyView(boolean visible);
    }

    public interface LinkedIdsFragMvpView {
        void switchToFragment(Fragment frag, String backStackName);
    }

    public interface LinkedIdsClickListener {
        void onLinkedIdItemClick(int position);
        void onClickAddIdentity();
    }

    private void addLinkedIdentity() {
        Intent intent = new Intent(context, LinkedIdWizard.class);
        intent.setData(KeyRings.buildUnifiedKeyRingUri(masterKeyId));
        context.startActivity(intent);
    }
}