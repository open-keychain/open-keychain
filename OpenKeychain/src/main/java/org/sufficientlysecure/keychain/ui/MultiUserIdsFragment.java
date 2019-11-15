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


import java.util.ArrayList;
import java.util.List;

import androidx.lifecycle.LiveData;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.livedata.GenericLiveData;
import org.sufficientlysecure.keychain.model.UserPacket.UserId;
import org.sufficientlysecure.keychain.daos.KeyRepository;
import org.sufficientlysecure.keychain.service.CertifyActionsParcel;
import org.sufficientlysecure.keychain.ui.adapter.MultiUserIdsAdapter;
import timber.log.Timber;


public class MultiUserIdsFragment extends Fragment {
    public static final String ARG_CHECK_STATES = "check_states";
    public static final String EXTRA_KEY_IDS = "extra_key_ids";
    private boolean checkboxVisibility = true;

    ListView userIds;
    private MultiUserIdsAdapter userIdsAdapter;

    private long[] pubMasterKeyIds;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.multi_user_ids_fragment, container, false);
        userIds = view.findViewById(R.id.view_key_user_ids);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FragmentActivity activity = requireActivity();
        pubMasterKeyIds = activity.getIntent().getLongArrayExtra(EXTRA_KEY_IDS);
        if (pubMasterKeyIds == null) {
            Timber.e("List of key ids to certify missing!");
            activity.finish();
            return;
        }

        ArrayList<Boolean> checkedStates = null;
        if (savedInstanceState != null) {
            checkedStates = (ArrayList<Boolean>) savedInstanceState.getSerializable(ARG_CHECK_STATES);
        }

        userIdsAdapter = new MultiUserIdsAdapter(activity, null, 0, checkedStates, checkboxVisibility);
        userIds.setAdapter(userIdsAdapter);
        userIds.setDividerHeight(0);

        KeyRepository keyRepository = KeyRepository.create(activity);
        LiveData<List<UserId>> userIdLiveData =
                new GenericLiveData<>(getContext(), () -> keyRepository.getUserIds(pubMasterKeyIds));
        userIdLiveData.observe(this, this::onUserIdsLoaded);

    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        ArrayList<Boolean> states = userIdsAdapter.getCheckStates();
        // no proper parceling method available :(
        outState.putSerializable(ARG_CHECK_STATES, states);
    }

    public ArrayList<CertifyActionsParcel.CertifyAction> getSelectedCertifyActions() {
        if (!checkboxVisibility) {
            throw new AssertionError("Item selection not allowed");
        }

        return userIdsAdapter.getSelectedCertifyActions();
    }

    private void onUserIdsLoaded(List<UserId> userIds) {
        MatrixCursor matrix = new MatrixCursor(new String[]{
                "_id", "user_data", "grouped"
        }) {
            @Override
            public byte[] getBlob(int column) {
                return super.getBlob(column);
            }
        };

        long lastMasterKeyId = 0;
        String lastName = "";
        ArrayList<String> uids = new ArrayList<>();

        boolean header = true;
        boolean isFirst = true;

        // Iterate over all rows
        for (UserId userId : userIds) {
            // Two cases:

            boolean grouped = userId.master_key_id() == lastMasterKeyId;
            boolean subGrouped = isFirst || grouped && lastName != null && lastName.equals(userId.name());
            isFirst = false;
            // Remember for next loop
            lastName = userId.name();

            Timber.d(Long.toString(userId.master_key_id(), 16) + (grouped ? "grouped" : "not grouped"));

            if (!subGrouped) {
                // 1. This name should NOT be grouped with the previous, so we flush the buffer

                Parcel p = Parcel.obtain();
                p.writeStringList(uids);
                byte[] d = p.marshall();
                p.recycle();

                matrix.addRow(new Object[]{
                        lastMasterKeyId, d, header ? 1 : 0
                });
                // indicate that we have a header for this masterKeyId
                header = false;

                // Now clear the buffer, and add the new user id, for the next round
                uids.clear();

            }

            // 2. This name should be grouped with the previous, just add to buffer
            uids.add(userId.user_id());
            lastMasterKeyId = userId.master_key_id();

            // If this one wasn't grouped, the next one's gotta be a header
            if (!grouped) {
                header = true;
            }
        }

        // If there is anything left in the buffer, flush it one last time
        if (!uids.isEmpty()) {

            Parcel p = Parcel.obtain();
            p.writeStringList(uids);
            byte[] d = p.marshall();
            p.recycle();

            matrix.addRow(new Object[]{
                    lastMasterKeyId, d, header ? 1 : 0
            });

        }

        userIdsAdapter.swapCursor(matrix);
    }

    public void setCheckboxVisibility(boolean checkboxVisibility) {
        this.checkboxVisibility = checkboxVisibility;
    }
}
