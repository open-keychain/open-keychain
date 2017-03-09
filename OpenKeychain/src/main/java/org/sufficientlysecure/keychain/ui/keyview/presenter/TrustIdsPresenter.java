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
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.View;

import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.ui.adapter.TrustIdsAdapter;
import org.sufficientlysecure.keychain.ui.keyview.presenter.ViewKeyMvpView.OnBackStackPoppedListener;
import org.sufficientlysecure.keychain.ui.util.recyclerview.RecyclerItemClickListener.OnItemClickListener;


public class TrustIdsPresenter implements LoaderCallbacks<Cursor> {
    private final Context context;
    private final TrustIdsMvpView view;
    private final ViewKeyMvpView viewKeyMvpView;
    private final int loaderId;

    private final TrustIdsAdapter trustIdsAdapter;

    private final long masterKeyId;
    private final boolean isSecret;

    public TrustIdsPresenter(Context context, TrustIdsMvpView view, ViewKeyMvpView viewKeyMvpView, int loaderId,
            long masterKeyId, boolean isSecret) {
        this.context = context;
        this.view = view;
        this.viewKeyMvpView = viewKeyMvpView;
        this.loaderId = loaderId;

        this.masterKeyId = masterKeyId;
        this.isSecret = isSecret;

        trustIdsAdapter = new TrustIdsAdapter(context, null);
        view.setTrustIdAdapter(trustIdsAdapter);

        trustIdsAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                onClickTrustId(position);
            }
        });
    }

    public void startLoader(LoaderManager loaderManager) {
        loaderManager.restartLoader(loaderId, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return TrustIdsAdapter.createLoader(context, KeyRings.buildUnifiedKeyRingUri(masterKeyId));
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        trustIdsAdapter.swapCursor(data);
        view.showCard(data.getCount() > 0);
    }

    @Override
    public void onLoaderReset(Loader loader) {
        trustIdsAdapter.swapCursor(null);
    }

    private void onClickTrustId(int position) {
        trustIdsAdapter.setExpandedView(position);

        viewKeyMvpView.addFakeBackStackItem("expand_trust_id", new OnBackStackPoppedListener() {
            @Override
            public void onBackStackPopped() {
                trustIdsAdapter.setExpandedView(null);
            }
        });
    }

    public interface TrustIdsMvpView {
        void setTrustIdAdapter(TrustIdsAdapter trustIdsAdapter);
        void showCard(boolean show);
    }
}