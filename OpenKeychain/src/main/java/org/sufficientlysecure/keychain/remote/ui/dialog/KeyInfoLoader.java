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

package org.sufficientlysecure.keychain.remote.ui.dialog;


import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import org.sufficientlysecure.keychain.livedata.KeyInfoInteractor;
import org.sufficientlysecure.keychain.livedata.KeyInfoInteractor.KeyInfo;
import org.sufficientlysecure.keychain.livedata.KeyInfoInteractor.KeySelector;


public class KeyInfoLoader extends AsyncTaskLoader<List<KeyInfo>> {
    private final KeySelector keySelector;

    private List<KeyInfo> cachedResult;
    private KeyInfoInteractor keyInfoInteractor;

    KeyInfoLoader(Context context, ContentResolver contentResolver, KeySelector keySelector) {
        super(context);

        this.keySelector = keySelector;
        this.keyInfoInteractor = new KeyInfoInteractor(contentResolver);
    }

    @Override
    public List<KeyInfo> loadInBackground() {
        return keyInfoInteractor.loadKeyInfo(keySelector);
    }

    @Override
    public void deliverResult(List<KeyInfo> keySubkeyStatus) {
        cachedResult = keySubkeyStatus;

        if (isStarted()) {
            super.deliverResult(keySubkeyStatus);
        }
    }

    @Override
    protected void onStartLoading() {
        if (cachedResult != null) {
            deliverResult(cachedResult);
        }

        if (takeContentChanged() || cachedResult == null) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        super.onStopLoading();

        cachedResult = null;
    }
}
