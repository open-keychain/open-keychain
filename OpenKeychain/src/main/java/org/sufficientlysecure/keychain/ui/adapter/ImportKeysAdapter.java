/*
 * Copyright (C) 2013-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.ui.adapter;

import android.databinding.DataBindingUtil;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.databinding.ImportKeysListItemBinding;
import org.sufficientlysecure.keychain.keyimport.ImportKeysListEntry;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.keyimport.processing.BytesLoaderState;
import org.sufficientlysecure.keychain.keyimport.processing.CloudLoaderState;
import org.sufficientlysecure.keychain.keyimport.processing.ImportKeysListener;
import org.sufficientlysecure.keychain.keyimport.processing.ImportKeysOperationCallback;
import org.sufficientlysecure.keychain.keyimport.processing.ImportKeysResultListener;
import org.sufficientlysecure.keychain.keyimport.processing.LoaderState;
import org.sufficientlysecure.keychain.operations.ImportOperation;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing;
import org.sufficientlysecure.keychain.service.ImportKeyringParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ParcelableFileCache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImportKeysAdapter extends RecyclerView.Adapter<ImportKeysAdapter.ViewHolder> implements ImportKeysResultListener {

    private FragmentActivity mActivity;
    private ImportKeysResultListener mListener;
    private boolean mNonInteractive;

    private LoaderState mLoaderState;
    private List<ImportKeysListEntry> mData;

    private KeyState[] mKeyStates;
    private int mCurrent;

    public ImportKeysAdapter(FragmentActivity activity, ImportKeysListener listener, boolean mNonInteractive) {
        this.mActivity = activity;
        this.mListener = listener;
        this.mNonInteractive = mNonInteractive;
    }

    public void setLoaderState(LoaderState loaderState) {
        this.mLoaderState = loaderState;
    }

    public void setData(List<ImportKeysListEntry> data) {
        this.mData = data;
        this.mKeyStates = new KeyState[data.size()];
        for (int i = 0; i < mKeyStates.length; i++) {
            mKeyStates[i] = new KeyState();
        }
        notifyDataSetChanged();
    }

    public void clearData() {
        mData = null;
        mKeyStates = null;
        notifyDataSetChanged();
    }

    /**
     * This method returns a list of all selected entries, with public keys sorted
     * before secret keys, see ImportOperation for specifics.
     *
     * @see ImportOperation
     */
    public List<ImportKeysListEntry> getEntries() {
        ArrayList<ImportKeysListEntry> result = new ArrayList<>();
        ArrayList<ImportKeysListEntry> secrets = new ArrayList<>();
        if (mData == null) {
            return result;
        }
        for (ImportKeysListEntry entry : mData) {
            // add this entry to either the secret or the public list
            (entry.isSecretKey() ? secrets : result).add(entry);
        }
        // add secret keys at the end of the list
        result.addAll(secrets);
        return result;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ImportKeysListItemBinding b;

        public ViewHolder(View view) {
            super(view);
            b = DataBindingUtil.bind(view);
            b.setNonInteractive(mNonInteractive);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        return new ViewHolder(inflater.inflate(R.layout.import_keys_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final ImportKeysListItemBinding b = holder.b;
        final ImportKeysListEntry entry = mData.get(position);
        b.setEntry(entry);

        final KeyState keyState = mKeyStates[position];
        final boolean downloaded = keyState.mDownloaded;
        final boolean showed = keyState.mShowed;

        b.importKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoaderState instanceof BytesLoaderState) {
                    importKey(new ParcelableKeyRing(entry.getEncodedRing()));
                } else if (mLoaderState instanceof CloudLoaderState) {
                    importKey(new ParcelableKeyRing(entry.getFingerprintHex(), entry.getKeyIdHex(),
                            entry.getKeybaseName(), entry.getFbUsername()));
                }
            }
        });

        b.expand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrent = position;
                if (!showed && !downloaded) {
                    getKey(entry);
                } else {
                    changeState(position, !showed);
                }
            }
        });

        float rotation = showed ? 180 : 0;
        if (!keyState.mAnimated) {
            keyState.mAnimated = true;
            float oldRotation = !showed ? 180 : 0;
            b.expand.setRotation(oldRotation);
            b.expand.animate().rotation(rotation).start();
        } else {
            b.expand.setRotation(rotation);
        }

        b.extraContainer.setVisibility(showed ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return mData != null ? mData.size() : 0;
    }

    public void importKey(ParcelableKeyRing keyRing) {
        ImportKeyringParcel inputParcel = prepareKeyOperation(keyRing, false);
        ImportKeysOperationCallback cb = new ImportKeysOperationCallback(mListener, inputParcel);
        CryptoOperationHelper operationHelper = new CryptoOperationHelper(1, mActivity, cb, R.string.progress_importing);
        operationHelper.cryptoOperation();
    }

    public void getKey(ImportKeysListEntry entry) {
        ParcelableKeyRing keyRing = null;
        if (mLoaderState instanceof BytesLoaderState) {
            keyRing = new ParcelableKeyRing(entry.getEncodedRing());
        } else if (mLoaderState instanceof CloudLoaderState) {
            keyRing = new ParcelableKeyRing(entry.getFingerprintHex(), entry.getKeyIdHex(),
                    entry.getKeybaseName(), entry.getFbUsername());
        }

        ImportKeyringParcel inputParcel = prepareKeyOperation(keyRing, true);
        ImportKeysOperationCallback cb = new ImportKeysOperationCallback(this, inputParcel);
        CryptoOperationHelper operationHelper = new CryptoOperationHelper(1, mActivity, cb, R.string.progress_downloading);
        operationHelper.cryptoOperation();
    }

    private ImportKeyringParcel prepareKeyOperation(ParcelableKeyRing keyRing, boolean skipSave) {
        ArrayList<ParcelableKeyRing> keysList = null;
        String keyserver = null;

        if (mLoaderState instanceof BytesLoaderState) {
            // instead of giving the entries by Intent extra, cache them into a
            // file to prevent Java Binder problems on heavy imports
            // read FileImportCache for more info.
            try {
                // We parcel this iteratively into a file - anything we can
                // display here, we should be able to import.
                ParcelableFileCache<ParcelableKeyRing> cache =
                        new ParcelableFileCache<>(mActivity, ImportOperation.CACHE_FILE_NAME);
                cache.writeCache(keyRing);
            } catch (IOException e) {
                Log.e(Constants.TAG, "Problem writing cache file", e);
                Notify.create(mActivity, "Problem writing cache file!", Notify.Style.ERROR).show();
            }
        } else if (mLoaderState instanceof CloudLoaderState) {
            keysList = new ArrayList<>();
            keysList.add(keyRing);
            keyserver = ((CloudLoaderState) mLoaderState).mCloudPrefs.keyserver;
        }

        return new ImportKeyringParcel(keysList, keyserver, skipSave);
    }

    @Override
    public void handleResult(ImportKeyResult result) {
        boolean resultStatus = result.success();
        Log.e(Constants.TAG, "getKey result: " + resultStatus);
        if (resultStatus) {
            ArrayList<CanonicalizedKeyRing> canKeyRings = result.mCanonicalizedKeyRings;
            if (canKeyRings.size() == 1) {
                CanonicalizedKeyRing keyRing = canKeyRings.get(0);
                Log.e(Constants.TAG, "Key ID: " + keyRing.getMasterKeyId() +
                        "| isRev: " + keyRing.isRevoked() + "| isExp: " + keyRing.isExpired());

                ImportKeysListEntry entry = mData.get(mCurrent);

                ArrayList<String> realUserIdsPlusKeybase = keyRing.getUnorderedUserIds();
                realUserIdsPlusKeybase.addAll(entry.getKeybaseUserIds());
                entry.setUserIds(realUserIdsPlusKeybase);

                mKeyStates[mCurrent].mDownloaded = true;
                changeState(mCurrent, true);
            } else {
                throw new RuntimeException("getKey retrieved more than one key ("
                        + canKeyRings.size() + ")");
            }
        } else {
            result.createNotify(mActivity).show();
        }
    }

    private class KeyState {
        public boolean mDownloaded = false;
        public boolean mShowed = false;
        public boolean mAnimated = true;
    }

    private void changeState(int position, boolean showed) {
        KeyState keyState = mKeyStates[position];
        keyState.mShowed = showed;
        keyState.mAnimated = true;
        notifyItemChanged(position);
    }

}
