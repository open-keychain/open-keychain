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


import java.util.Date;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;

import org.sufficientlysecure.keychain.ui.keyview.loader.KeyserverStatusLoader;
import org.sufficientlysecure.keychain.ui.keyview.loader.KeyserverStatusLoader.KeyserverStatus;


public class KeyserverStatusPresenter implements LoaderCallbacks<KeyserverStatus> {
    private final Context context;
    private final KeyserverStatusMvpView view;
    private final int loaderId;

    private final long masterKeyId;
    private final boolean isSecret;


    public KeyserverStatusPresenter(Context context, KeyserverStatusMvpView view, int loaderId, long masterKeyId,
            boolean isSecret) {
        this.context = context;
        this.view = view;
        this.loaderId = loaderId;

        this.masterKeyId = masterKeyId;
        this.isSecret = isSecret;
    }

    public void startLoader(LoaderManager loaderManager) {
        loaderManager.restartLoader(loaderId, null, this);
    }

    @Override
    public Loader<KeyserverStatus> onCreateLoader(int id, Bundle args) {
        return new KeyserverStatusLoader(context, context.getContentResolver(), masterKeyId);
    }

    @Override
    public void onLoadFinished(Loader<KeyserverStatus> loader, KeyserverStatus keyserverStatus) {
        if (keyserverStatus.hasBeenUpdated()) {
            if (keyserverStatus.isPublished()) {
                view.setDisplayStatusPublished();
            } else {
                view.setDisplayStatusNotPublished();
            }
            view.setLastUpdated(keyserverStatus.getLastUpdated());
        } else {
            view.setDisplayStatusUnknown();
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {

    }

    public interface KeyserverStatusMvpView {
        void setDisplayStatusPublished();
        void setDisplayStatusNotPublished();
        void setLastUpdated(Date lastUpdated);
        void setDisplayStatusUnknown();
    }
}
