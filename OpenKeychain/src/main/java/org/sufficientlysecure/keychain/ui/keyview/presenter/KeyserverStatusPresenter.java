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

package org.sufficientlysecure.keychain.ui.keyview.presenter;


import java.util.Date;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.support.annotation.Nullable;

import org.sufficientlysecure.keychain.model.KeyMetadata;
import org.sufficientlysecure.keychain.ui.keyview.loader.ViewKeyLiveData.KeyserverStatusLiveData;


public class KeyserverStatusPresenter implements Observer<KeyMetadata> {
    private final Context context;
    private final KeyserverStatusMvpView view;

    private final long masterKeyId;
    private final boolean isSecret;


    public KeyserverStatusPresenter(Context context, KeyserverStatusMvpView view, long masterKeyId,
            boolean isSecret) {
        this.context = context;
        this.view = view;

        this.masterKeyId = masterKeyId;
        this.isSecret = isSecret;
    }

    public LiveData<KeyMetadata> getLiveDataInstance() {
        return new KeyserverStatusLiveData(context, masterKeyId);
    }

    @Override
    public void onChanged(@Nullable KeyMetadata keyserverStatus) {
        if (keyserverStatus == null) {
            view.setDisplayStatusUnknown();
            return;
        }

        if (keyserverStatus.hasBeenUpdated()) {
            if (keyserverStatus.isPublished()) {
                view.setDisplayStatusPublished();
            } else {
                view.setDisplayStatusNotPublished();
            }
            view.setLastUpdated(keyserverStatus.last_updated());
        } else {
            view.setDisplayStatusUnknown();
        }
    }

    public interface KeyserverStatusMvpView {
        void setDisplayStatusPublished();
        void setDisplayStatusNotPublished();
        void setLastUpdated(Date lastUpdated);
        void setDisplayStatusUnknown();
    }
}
