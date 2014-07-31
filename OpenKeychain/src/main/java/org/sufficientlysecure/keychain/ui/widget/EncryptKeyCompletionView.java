package org.sufficientlysecure.keychain.ui.widget;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.tokenautocomplete.FilteredArrayAdapter;
import com.tokenautocomplete.TokenCompleteTextView;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.ContactHelper;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EncryptKeyCompletionView extends TokenCompleteTextView {
    public EncryptKeyCompletionView(Context context) {
        super(context);
        initView();
    }

    public EncryptKeyCompletionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public EncryptKeyCompletionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView();
    }

    private void initView() {
        swapCursor(null);
        setPrefix(getContext().getString(R.string.label_to) + ": ");
        allowDuplicates(false);
    }

    @Override
    protected View getViewForObject(Object object) {
        if (object instanceof EncryptionKey) {
            LayoutInflater l = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            View view = l.inflate(R.layout.recipient_box_entry, null);
            ((TextView) view.findViewById(android.R.id.text1)).setText(((EncryptionKey) object).getPrimary());
            setImageByKey((ImageView) view.findViewById(android.R.id.icon), (EncryptionKey) object);
            return view;
        }
        return null;
    }

    private void setImageByKey(ImageView view, EncryptionKey key) {
        Bitmap photo = ContactHelper.photoFromFingerprint(getContext().getContentResolver(), key.getFingerprint());

        if (photo != null) {
            view.setImageBitmap(photo);
        } else {
            view.setImageResource(R.drawable.ic_generic_man);
        }
    }

    @Override
    protected Object defaultObject(String completionText) {
        // TODO: We could try to automagically download the key if it's unknown but a key id
        /*if (completionText.startsWith("0x")) {

        }*/
        return null;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getContext() instanceof FragmentActivity) {
            ((FragmentActivity) getContext()).getSupportLoaderManager().initLoader(0, null, new LoaderManager.LoaderCallbacks<Cursor>() {
                @Override
                public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                    return new CursorLoader(getContext(), KeychainContract.KeyRings.buildUnifiedKeyRingsUri(),
                            new String[]{KeychainContract.KeyRings.HAS_ENCRYPT, KeychainContract.KeyRings.KEY_ID, KeychainContract.KeyRings.USER_ID, KeychainContract.KeyRings.FINGERPRINT},
                            null, null, null);
                }

                @Override
                public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                    swapCursor(data);
                }

                @Override
                public void onLoaderReset(Loader<Cursor> loader) {
                    swapCursor(null);
                }
            });
        }
    }

    public void swapCursor(Cursor cursor) {
        if (cursor == null) {
            setAdapter(new EncryptKeyAdapter(Collections.<EncryptionKey>emptyList()));
            return;
        }
        ArrayList<EncryptionKey> keys = new ArrayList<EncryptionKey>();
        cursor.moveToFirst();
        while (cursor.moveToNext()) {
            try {
                if (cursor.getInt(cursor.getColumnIndexOrThrow(KeychainContract.KeyRings.HAS_ENCRYPT)) != 0) {
                    EncryptionKey key = new EncryptionKey(cursor);
                    keys.add(key);
                }
            } catch (Exception e) {
                Log.w(Constants.TAG, e);
                return;
            }
        }
        setAdapter(new EncryptKeyAdapter(keys));
    }

    public class EncryptionKey {
        private String mUserId;
        private long mKeyId;
        private String mFingerprint;

        public EncryptionKey(String userId, long keyId, String fingerprint) {
            this.mUserId = userId;
            this.mKeyId = keyId;
            this.mFingerprint = fingerprint;
        }

        public EncryptionKey(Cursor cursor) {
            this(cursor.getString(cursor.getColumnIndexOrThrow(KeychainContract.KeyRings.USER_ID)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(KeychainContract.KeyRings.KEY_ID)),
                    PgpKeyHelper.convertFingerprintToHex(
                            cursor.getBlob(cursor.getColumnIndexOrThrow(KeychainContract.KeyRings.FINGERPRINT))));

        }

        public EncryptionKey(CachedPublicKeyRing ring) throws PgpGeneralException {
            this(ring.getPrimaryUserId(), ring.extractOrGetMasterKeyId(),
                    PgpKeyHelper.convertFingerprintToHex(ring.getFingerprint()));
        }

        public String getUserId() {
            return mUserId;
        }

        public String getFingerprint() {
            return mFingerprint;
        }

        public String getPrimary() {
            String[] userId = KeyRing.splitUserId(mUserId);
            if (userId[0] != null && userId[2] != null) {
                return userId[0] + " (" + userId[2] + ")";
            } else if (userId[0] != null) {
                return userId[0];
            } else {
                return userId[1];
            }
        }

        public String getSecondary() {
            String[] userId = KeyRing.splitUserId(mUserId);
            if (userId[0] != null) {
                return userId[1] + " (" + getKeyIdHexShort() + ")";
            } else {
                return getKeyIdHex();
            }
        }

        public long getKeyId() {
            return mKeyId;
        }

        public String getKeyIdHex() {
            return PgpKeyHelper.convertKeyIdToHex(mKeyId);
        }

        public String getKeyIdHexShort() {
            return PgpKeyHelper.convertKeyIdToHexShort(mKeyId);
        }

        @Override
        public String toString() {
            return Long.toString(mKeyId);
        }
    }

    private class EncryptKeyAdapter extends FilteredArrayAdapter<EncryptionKey> {

        public EncryptKeyAdapter(List<EncryptionKey> objs) {
            super(EncryptKeyCompletionView.this.getContext(), 0, 0, objs);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater l = (LayoutInflater) getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            View view;
            if (convertView != null) {
                view = convertView;
            } else {
                view = l.inflate(R.layout.recipient_selection_list_entry, null);
            }
            ((TextView) view.findViewById(android.R.id.title)).setText(getItem(position).getPrimary());
            ((TextView) view.findViewById(android.R.id.text1)).setText(getItem(position).getSecondary());
            setImageByKey((ImageView) view.findViewById(android.R.id.icon), getItem(position));
            return view;
        }

        @Override
        protected boolean keepObject(EncryptionKey obj, String mask) {
            String m = mask.toLowerCase();
            return obj.getUserId().toLowerCase().contains(m) ||
                    obj.getKeyIdHex().contains(m) ||
                    obj.getKeyIdHexShort().startsWith(m);
        }
    }
}
