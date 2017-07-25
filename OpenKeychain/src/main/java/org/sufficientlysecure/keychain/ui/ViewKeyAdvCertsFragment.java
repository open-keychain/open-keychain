/*
 * Copyright (C) 2014-2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

import org.jetbrains.annotations.NotNull;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.ui.base.RecyclerFragment;
import org.sufficientlysecure.keychain.ui.util.recyclerview.cursor.CertCursor;
import org.sufficientlysecure.keychain.ui.util.recyclerview.item.BaseHeaderItem;
import org.sufficientlysecure.keychain.ui.util.recyclerview.item.CertHeaderItem;
import org.sufficientlysecure.keychain.ui.util.recyclerview.item.CertItem;
import org.sufficientlysecure.keychain.util.Log;

import java.util.ArrayList;
import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.common.SmoothScrollLinearLayoutManager;

public class ViewKeyAdvCertsFragment extends RecyclerFragment<FlexibleAdapter<CertItem>>
        implements LoaderManager.LoaderCallbacks<Cursor>, FlexibleAdapter.OnItemClickListener {

    public static final String ARG_DATA_URI = "data_uri";
    private Uri mDataUriCerts;
    private List<CertItem> mCertItemList = new ArrayList<>();

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
            Log.e(Constants.TAG, "Data missing. Should be Uri of key!");
            getActivity().finish();
            return;
        } else {
            mDataUriCerts = KeychainContract.Certs.buildCertsUri(dataUri);
        }

        CertFlexibleAdapter adapter = new CertFlexibleAdapter(mCertItemList);
        adapter.setDisplayHeadersAtStartUp(true)
                .setStickyHeaders(true, getHeaderContainerWithPadding(16, 16))
                .setAnimationOnScrolling(true);
        adapter.addListener(this);

        setAdapter(adapter);
        setLayoutManager(new SmoothScrollLinearLayoutManager(getActivity()));

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public boolean onItemClick(int position) {
        final CertItem certItem = mCertItemList.get(position);
        if(certItem.getMasterKeyId() != 0L) {
            Intent viewIntent = new Intent(getActivity(), ViewCertActivity.class);
            viewIntent.setData(KeychainContract.Certs.buildCertsSpecificUri(
                    certItem.getMasterKeyId(), certItem.getRank(), certItem.getSignerKeyId()));
            startActivity(viewIntent);
        }
        return true;
    }

    class CertFlexibleAdapter extends FlexibleAdapter<CertItem> {
        CertFlexibleAdapter(@NotNull List<CertItem> certItems) {
            super(certItems);
        }


    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(getActivity(), mDataUriCerts,
                CertCursor.CERTS_PROJECTION, null, null,
                CertCursor.CERTS_SORT_ORDER);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCertItemList.clear();

        // Avoid NullPointerExceptions, if we get an empty result set.
        if (data.getCount() == 0) {
            return;
        }

        if (data.moveToFirst()) {
            while (!data.isAfterLast()) {
                mCertItemList.add(new CertItem(null, CertCursor.wrap(data)));
                data.moveToNext();
            }
        }

        for (CertItem certItem : mCertItemList) {
            certItem.setHeader(BaseHeaderItem.getInstance(this, certItem.getSection(), CertHeaderItem.class));
        }

        List<CertItem> itemList = new ArrayList<>(mCertItemList);
        getAdapter().updateDataSet(itemList);

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

    }
}
