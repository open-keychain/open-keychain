/*
 * Copyright (C) 2017 Vincent Breitmoser <look@my.amazin.horse>
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


import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;

import org.sufficientlysecure.keychain.ui.CreateSecurityTokenImportFragment.StatusLine;
import org.sufficientlysecure.keychain.ui.PublicKeyRetrievalLoader.ContentUriRetrievalLoader;
import org.sufficientlysecure.keychain.ui.PublicKeyRetrievalLoader.KeyRetrievalResult;
import org.sufficientlysecure.keychain.ui.PublicKeyRetrievalLoader.KeyserverRetrievalLoader;
import org.sufficientlysecure.keychain.ui.PublicKeyRetrievalLoader.LocalKeyLookupLoader;
import org.sufficientlysecure.keychain.ui.PublicKeyRetrievalLoader.UriKeyRetrievalLoader;


class CreateSecurityTokenImportPresenter {
    private static final int LOADER_LOCAL = 0;
    private static final int LOADER_URI = 1;
    private static final int LOADER_KEYSERVER = 2;
    private static final int LOADER_CONTENT_URI = 3;
    private static final String ARG_CONTENT_URI = "content_uri";

    private Context context;

    private byte[][] tokenFingerprints;
    private byte[] tokenAid;
    private double tokenVersion;
    private String tokenUserId;
    private final String tokenUrl;

    private LoaderManager loaderManager;
    private CreateSecurityTokenImportMvpView view;
    private boolean searchedLocally;
    private boolean searchedAtUri;
    private boolean searchedKeyservers;

    private byte[] importKeyData;
    private Long masterKeyId;


    CreateSecurityTokenImportPresenter(Context context, byte[] tokenFingerprints, byte[] tokenAid,
            String tokenUserId, String tokenUrl, LoaderManager loaderManager) {
        this.context = context.getApplicationContext();

        this.tokenAid = tokenAid;
        this.tokenUserId = tokenUserId;
        this.tokenUrl = tokenUrl;
        this.loaderManager = loaderManager;

        if (tokenFingerprints.length % 20 != 0) {
            throw new IllegalArgumentException("fingerprints must be multiple of 20 bytes!");
        }
        this.tokenFingerprints = new byte[tokenFingerprints.length / 20][];
        for (int i = 0; i < tokenFingerprints.length / 20; i++) {
            this.tokenFingerprints[i] = new byte[20];
            System.arraycopy(tokenFingerprints, i*20, this.tokenFingerprints[i], 0, 20);
        }
    }

    public void setView(CreateSecurityTokenImportMvpView view) {
        this.view = view;
    }

    public void onActivityCreated() {
        continueSearch();
    }

    private void continueSearchAfterError() {
        view.statusLineError();
        continueSearch();
    }

    private void continueSearch() {
        if (!searchedLocally) {
            view.statusLineAdd(StatusLine.SEARCH_LOCAL);
            loaderManager.restartLoader(LOADER_LOCAL, null, loaderCallbacks);
            return;
        }

        if (!searchedAtUri) {
            view.statusLineAdd(StatusLine.SEARCH_URI);
            loaderManager.restartLoader(LOADER_URI, null, loaderCallbacks);
            return;
        }

        if (!searchedKeyservers) {
            view.statusLineAdd(StatusLine.SEARCH_KEYSERVER);
            loaderManager.restartLoader(LOADER_KEYSERVER, null, loaderCallbacks);
            return;
        }

        view.showActionRetryOrFromFile();
    }

    private LoaderCallbacks<KeyRetrievalResult> loaderCallbacks = new LoaderCallbacks<KeyRetrievalResult>() {
        @Override
        public Loader<KeyRetrievalResult> onCreateLoader(int id, Bundle args) {
            switch (id) {
                case LOADER_LOCAL:
                    return new LocalKeyLookupLoader(context, tokenFingerprints);
                case LOADER_URI:
                    return new UriKeyRetrievalLoader(context, tokenUrl, tokenFingerprints);
                case LOADER_KEYSERVER:
                    return new KeyserverRetrievalLoader(context, tokenFingerprints[0]);
                case LOADER_CONTENT_URI:
                    return new ContentUriRetrievalLoader(context, tokenFingerprints[0],
                            args.<Uri>getParcelable(ARG_CONTENT_URI));
            }
            throw new IllegalArgumentException("called with unknown loader id!");
        }

        @Override
        public void onLoadFinished(Loader<KeyRetrievalResult> loader, KeyRetrievalResult data) {
            switch (loader.getId()) {
                case LOADER_LOCAL: {
                    searchedLocally = true;
                    break;
                }
                case LOADER_URI: {
                    searchedAtUri = true;
                    break;
                }
                case LOADER_KEYSERVER: {
                    searchedKeyservers = true;
                    break;
                }
                case LOADER_CONTENT_URI: {
                    // nothing to do here
                    break;
                }
                default: {
                    throw new IllegalArgumentException("called with unknown loader id!");
                }
            }

            if (data.isSuccess()) {
                processResult(data);
            } else {
                continueSearchAfterError();
            }
        }

        @Override
        public void onLoaderReset(Loader<KeyRetrievalResult> loader) {

        }
    };

    private void processResult(KeyRetrievalResult result) {
        view.statusLineOk();

        byte[] importKeyData = result.getKeyData();
        Long masterKeyId = result.getMasterKeyId();
        if (importKeyData != null && masterKeyId != null) {
            view.showActionImport();
            this.importKeyData = importKeyData;
            this.masterKeyId = masterKeyId;
            return;
        }

        if (masterKeyId != null) {
            this.masterKeyId = masterKeyId;
            view.statusLineAdd(StatusLine.TOKEN_CHECK);
            view.operationPromote(masterKeyId, tokenAid);
            return;
        }

        throw new IllegalArgumentException("Method can only be called with successful result!");
    }

    void onClickImport() {
        view.statusLineAdd(StatusLine.IMPORT);
        view.hideAction();
        view.operationImportKey(importKeyData);
    }

    void onImportSuccess() {
        view.statusLineOk();
        view.statusLineAdd(StatusLine.TOKEN_PROMOTE);
        view.operationPromote(masterKeyId, tokenAid);
    }

    void onImportError() {
        view.statusLineError();
    }

    void onPromoteSuccess() {
        view.statusLineOk();
        view.showActionViewKey();
    }

    void onPromoteError() {
        view.statusLineError();
    }

    void onClickRetry() {
        searchedLocally = false;
        searchedAtUri = false;
        searchedKeyservers = false;

        view.hideAction();
        view.resetStatusLines();
        continueSearch();
    }

    void onClickViewKey() {
        view.finishAndShowKey(masterKeyId);
    }

    void onClickResetToken() {
        view.showConfirmResetDialog();
    }

    void onClickConfirmReset() {
        view.operationResetSecurityToken();
    }

    void onSecurityTokenResetSuccess() {
        // TODO
    }

    void onClickLoadFile() {
        view.showFileSelectDialog();
    }

    void onFileSelected(Uri contentUri) {
        view.resetStatusLines();
        view.statusLineAdd(StatusLine.SEARCH_CONTENT_URI);

        Bundle args = new Bundle();
        args.putParcelable(ARG_CONTENT_URI, contentUri);
        loaderManager.restartLoader(LOADER_CONTENT_URI, args, loaderCallbacks);
    }

    interface CreateSecurityTokenImportMvpView {
        void statusLineAdd(StatusLine statusLine);
        void statusLineOk();
        void statusLineError();
        void resetStatusLines();

        void showActionImport();
        void showActionViewKey();
        void showActionRetryOrFromFile();
        void hideAction();

        void operationImportKey(byte[] importKeyData);
        void operationPromote(long masterKeyId, byte[] cardAid);
        void operationResetSecurityToken();

        void finishAndShowKey(long masterKeyId);

        void showFileSelectDialog();
        void showConfirmResetDialog();
    }
}
