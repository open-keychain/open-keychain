/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010 Thialfihar <thi@thialfihar.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.thialfihar.android.apg.ui;

import org.thialfihar.android.apg.Id;
import org.thialfihar.android.apg.R;
import org.thialfihar.android.apg.provider.ProviderHelper;
import org.thialfihar.android.apg.ui.widget.ExpandableListFragment;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;

public class KeyListFragment extends ExpandableListFragment {
    protected KeyListActivity mKeyListActivity;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mKeyListActivity = (KeyListActivity) getActivity();

        // register long press context menu
        registerForContextMenu(getListView());

        // Give some text to display if there is no data. In a real
        // application this would come from a resource.
        setEmptyText(getString(R.string.listEmpty));
    }

    /**
     * Context Menu on Long Click
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, Id.menu.export, 5, R.string.menu_exportKey);
        menu.add(0, Id.menu.delete, 111, R.string.menu_deleteKey);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        ExpandableListContextMenuInfo expInfo = (ExpandableListContextMenuInfo) item.getMenuInfo();

        // expInfo.id would also return row id of childs, but we always want to get the row id of
        // the group item, thus we are using the following way
        int groupPosition = ExpandableListView.getPackedPositionGroup(expInfo.packedPosition);
        long keyRingRowId = getExpandableListAdapter().getGroupId(groupPosition);

        switch (item.getItemId()) {
        case Id.menu.export:
            long masterKeyId = ProviderHelper.getPublicMasterKeyId(mKeyListActivity, keyRingRowId);
            if (masterKeyId == -1) {
                masterKeyId = ProviderHelper.getSecretMasterKeyId(mKeyListActivity, keyRingRowId);
            }

            mKeyListActivity.showExportKeysDialog(masterKeyId);
            return true;

        case Id.menu.delete:
            mKeyListActivity.showDeleteKeyDialog(keyRingRowId);
            return true;

        default:
            return super.onContextItemSelected(item);

        }
    }

}
