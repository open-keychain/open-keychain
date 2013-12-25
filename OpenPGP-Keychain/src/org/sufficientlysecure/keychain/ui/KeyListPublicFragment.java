/*
 * Copyright (C) 2012-2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserIds;
import org.sufficientlysecure.keychain.ui.adapter.KeyListAdapter;
import org.sufficientlysecure.keychain.R;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.app.LoaderManager;
import android.view.ContextMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

public class KeyListPublicFragment extends KeyListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private KeyListPublicActivity mKeyListPublicActivity;

    private KeyListAdapter mAdapter;

    /**
     * Define Adapter and Loader on create of Activity
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mKeyListPublicActivity = (KeyListPublicActivity) getActivity();

        mAdapter = new KeyListAdapter(mKeyListPublicActivity, null, Id.type.public_key);
        setListAdapter(mAdapter);

        // Start out with a progress indicator.
        setListShown(false);

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        // id is -1 as the child cursors are numbered 0,...,n
        getLoaderManager().initLoader(-1, null, this);
    }

    /**
     * Context Menu on Long Click
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, 23, 1, R.string.title_key_details); // :TODO: Fix magic number
        menu.add(0, Id.menu.update, 2, R.string.menu_update_key);
        menu.add(0, Id.menu.signKey, 3, R.string.menu_sign_key);
        menu.add(0, Id.menu.exportToServer, 4, R.string.menu_export_key_to_server);
        menu.add(0, Id.menu.share, 6, R.string.menu_share);
        menu.add(0, Id.menu.share_qr_code, 7, R.string.menu_share_qr_code);
        menu.add(0, Id.menu.share_nfc, 8, R.string.menu_share_nfc);

    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        ExpandableListContextMenuInfo expInfo = (ExpandableListContextMenuInfo) item.getMenuInfo();

        // expInfo.id would also return row id of childs, but we always want to get the row id of
        // the group item, thus we are using the following way
        int groupPosition = ExpandableListView.getPackedPositionGroup(expInfo.packedPosition);
        long keyRingRowId = getExpandableListAdapter().getGroupId(groupPosition);

        switch (item.getItemId()) {
        case Id.menu.update:
            long updateKeyId = 0;
            PGPPublicKeyRing updateKeyRing = ProviderHelper.getPGPPublicKeyRingByRowId(
                    mKeyListActivity, keyRingRowId);
            if (updateKeyRing != null) {
                updateKeyId = PgpKeyHelper.getMasterKey(updateKeyRing).getKeyID();
            }
            if (updateKeyId == 0) {
                // this shouldn't happen
                return true;
            }

            Intent queryIntent = new Intent(mKeyListActivity, KeyServerQueryActivity.class);
            queryIntent.setAction(KeyServerQueryActivity.ACTION_LOOK_UP_KEY_ID_AND_RETURN);
            queryIntent.putExtra(KeyServerQueryActivity.EXTRA_KEY_ID, updateKeyId);

            // TODO: lookup??
            startActivityForResult(queryIntent, Id.request.look_up_key_id);

            return true;
        case 23:
            
        	Intent detailsIntent = new Intent(mKeyListActivity, KeyDetailsActivity.class);
        	detailsIntent.putExtra("key", ProviderHelper.getPublicMasterKeyId(mKeyListActivity, keyRingRowId));
        	startActivity(detailsIntent);
            return true;
            
        case Id.menu.exportToServer:
            Intent uploadIntent = new Intent(mKeyListActivity, KeyServerUploadActivity.class);
            uploadIntent.setAction(KeyServerUploadActivity.ACTION_EXPORT_KEY_TO_SERVER);
            uploadIntent.putExtra(KeyServerUploadActivity.EXTRA_KEYRING_ROW_ID, (int)keyRingRowId);
            startActivityForResult(uploadIntent, Id.request.export_to_server);

            return true;

        case Id.menu.signKey:
            long keyId = 0;
            PGPPublicKeyRing signKeyRing = ProviderHelper.getPGPPublicKeyRingByRowId(
                    mKeyListActivity, keyRingRowId);
            if (signKeyRing != null) {
                keyId = PgpKeyHelper.getMasterKey(signKeyRing).getKeyID();
            }
            if (keyId == 0) {
                // this shouldn't happen
                return true;
            }

            Intent signIntent = new Intent(mKeyListActivity, SignKeyActivity.class);
            signIntent.putExtra(SignKeyActivity.EXTRA_KEY_ID, keyId);
            startActivity(signIntent);

            return true;

        case Id.menu.share_qr_code:
            // get master key id using row id
            long masterKeyId = ProviderHelper.getPublicMasterKeyId(mKeyListActivity, keyRingRowId);

            Intent qrCodeIntent = new Intent(mKeyListActivity, ShareActivity.class);
            qrCodeIntent.setAction(ShareActivity.ACTION_SHARE_KEYRING_WITH_QR_CODE);
            qrCodeIntent.putExtra(ShareActivity.EXTRA_MASTER_KEY_ID, masterKeyId);
            startActivityForResult(qrCodeIntent, 0);

            return true;

        case Id.menu.share_nfc:
            // get master key id using row id
            long masterKeyId2 = ProviderHelper.getPublicMasterKeyId(mKeyListActivity, keyRingRowId);

            Intent nfcIntent = new Intent(mKeyListActivity, ShareNfcBeamActivity.class);
            nfcIntent.setAction(ShareNfcBeamActivity.ACTION_SHARE_KEYRING_WITH_NFC);
            nfcIntent.putExtra(ShareNfcBeamActivity.EXTRA_MASTER_KEY_ID, masterKeyId2);
            startActivityForResult(nfcIntent, 0);

            return true;

        case Id.menu.share:
            // get master key id using row id
            long masterKeyId3 = ProviderHelper.getPublicMasterKeyId(mKeyListActivity, keyRingRowId);

            Intent shareIntent = new Intent(mKeyListActivity, ShareActivity.class);
            shareIntent.setAction(ShareActivity.ACTION_SHARE_KEYRING);
            shareIntent.putExtra(ShareActivity.EXTRA_MASTER_KEY_ID, masterKeyId3);
            startActivityForResult(shareIntent, 0);

            return true;

        default:
            return super.onContextItemSelected(item);

        }
    }

    // These are the rows that we will retrieve.
    static final String[] PROJECTION = new String[] { KeyRings._ID, KeyRings.MASTER_KEY_ID,
            UserIds.USER_ID };

    static final String SORT_ORDER = UserIds.USER_ID + " ASC";

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created. This
        // sample only has one Loader, so we don't care about the ID.
        Uri baseUri = KeyRings.buildPublicKeyRingsUri();

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(getActivity(), baseUri, PROJECTION, null, null, SORT_ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.setGroupCursor(data);

        // The list should now be shown.
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed. We need to make sure we are no
        // longer using it.
        mAdapter.setGroupCursor(null);
    }

}
