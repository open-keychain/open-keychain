package org.sufficientlysecure.keychain.ui.widget;

import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import org.ocpsoft.prettytime.PrettyTime;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;

public class CertListWidget extends ViewAnimator
        implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final int LOADER_ID_LINKED_CERTS = 38572;

    public static final String ARG_URI = "uri";
    public static final String ARG_IS_SECRET = "is_secret";


    // These are the rows that we will retrieve.
    static final String[] CERTS_PROJECTION = new String[]{
            KeychainContract.Certs._ID,
            KeychainContract.Certs.MASTER_KEY_ID,
            KeychainContract.Certs.VERIFIED,
            KeychainContract.Certs.TYPE,
            KeychainContract.Certs.RANK,
            KeychainContract.Certs.KEY_ID_CERTIFIER,
            KeychainContract.Certs.USER_ID,
            KeychainContract.Certs.SIGNER_UID,
            KeychainContract.Certs.CREATION
    };
    public static final int INDEX_MASTER_KEY_ID = 1;
    public static final int INDEX_VERIFIED = 2;
    public static final int INDEX_TYPE = 3;
    public static final int INDEX_RANK = 4;
    public static final int INDEX_KEY_ID_CERTIFIER = 5;
    public static final int INDEX_USER_ID = 6;
    public static final int INDEX_SIGNER_UID = 7;
    public static final int INDEX_CREATION = 8;

    private TextView vCollapsed;
    private ListView vExpanded;
    private View vExpandButton;
    private boolean mIsSecret;

    public CertListWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        View root = getRootView();
        vCollapsed = (TextView) root.findViewById(R.id.cert_collapsed_list);
        vExpanded = (ListView) root.findViewById(R.id.cert_expanded_list);
        vExpandButton = root.findViewById(R.id.cert_expand_button);

        // for now
        vExpandButton.setVisibility(View.GONE);
        vExpandButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleExpanded();
            }
        });

        // vExpanded.setAdapter(null);

    }

    void toggleExpanded() {
        setDisplayedChild(getDisplayedChild() == 1 ? 0 : 1);
    }

    void setExpanded(boolean expanded) {
        setDisplayedChild(expanded ? 1 : 0);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri = args.getParcelable(ARG_URI);
        mIsSecret = args.getBoolean(ARG_IS_SECRET, false);
        return new CursorLoader(getContext(), uri,
                CERTS_PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data == null || !data.moveToFirst()) {
            return;
        }

        // TODO support external certificates
        Date userCert = null;
        while (!data.isAfterLast()) {

            int verified = data.getInt(INDEX_VERIFIED);
            Date creation = new Date(data.getLong(INDEX_CREATION) * 1000);

            if (verified == Certs.VERIFIED_SECRET) {
                if (userCert == null || userCert.after(creation)) {
                    userCert = creation;
                }
            }

            data.moveToNext();
        }

        if (userCert != null) {
            PrettyTime format = new PrettyTime();
            if (mIsSecret) {
                vCollapsed.setText("You created this identity "
                        + format.format(userCert) + ".");
            } else {
                vCollapsed.setText("You verified and confirmed this identity "
                        + format.format(userCert) + ".");
            }
        } else {
            vCollapsed.setText("This identity is not yet verified or confirmed.");
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        setVisibility(View.GONE);
    }

}
