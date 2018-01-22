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

package org.sufficientlysecure.keychain.ui;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;

import com.tonicartos.superslim.LayoutManager;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.ui.adapter.CertSectionedListAdapter;
import org.sufficientlysecure.keychain.ui.base.RecyclerFragment;
import timber.log.Timber;


public class ViewKeyAdvCertsFragment extends RecyclerFragment<CertSectionedListAdapter>
        implements LoaderManager.LoaderCallbacks<Cursor>, CertSectionedListAdapter.CertListListener {

    public static final String ARG_DATA_URI = "data_uri";
    private Uri mDataUriCerts;

    /**
     * Creates new instance of this fragment
     */
    public static ViewKeyAdvCertsFragment newInstance(Uri dataUri) {
        ViewKeyAdvCertsFragment frag = new ViewKeyAdvCertsFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARG_DATA_URI, dataUri);

        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.view_key_adv_certs_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        hideList(false);

        Uri dataUri = getArguments().getParcelable(ARG_DATA_URI);
        if (dataUri == null) {
            Timber.e("Data missing. Should be Uri of key!");
            getActivity().finish();
            return;
        } else {
            mDataUriCerts = KeychainContract.Certs.buildCertsUri(dataUri);
        }

        CertSectionedListAdapter adapter = new CertSectionedListAdapter(getActivity(), null);
        adapter.setCertListListener(this);

        setAdapter(adapter);
        setLayoutManager(new LayoutManager(getActivity()));

        getLoaderManager().initLoader(0, null, this);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(getActivity(), mDataUriCerts,
                CertSectionedListAdapter.CertCursor.CERTS_PROJECTION, null, null,
                CertSectionedListAdapter.CertCursor.CERTS_SORT_ORDER);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Avoid NullPointerExceptions, if we get an empty result set.
        if (data.getCount() == 0) {
            return;
        }

        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        getAdapter().swapCursor(CertSectionedListAdapter.CertCursor.wrap(data));

        if (isResumed()) {
            showList(true);
        } else {
            showList(false);
        }
    }

    /**
     * This is called when the last Cursor provided to onLoadFinished() above is about to be closed.
     * We need to make sure we are no longer using it.
     */
    public void onLoaderReset(Loader<Cursor> loader) {
        getAdapter().swapCursor(null);
    }

    @Override
    public void onClick(long masterKeyId, long signerKeyId, long rank) {
        if(masterKeyId != 0L) {
            Intent viewIntent = new Intent(getActivity(), ViewCertActivity.class);
            viewIntent.setData(KeychainContract.Certs.buildCertsSpecificUri(
                    masterKeyId, rank, signerKeyId));
            startActivity(viewIntent);
        }
    }
}
