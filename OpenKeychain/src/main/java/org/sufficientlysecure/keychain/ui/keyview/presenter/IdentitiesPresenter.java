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

package org.sufficientlysecure.keychain.ui.keyview.presenter;


import java.io.IOException;
import java.util.List;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.view.View;

import org.sufficientlysecure.keychain.provider.AutocryptPeerDao;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserPackets;
import org.sufficientlysecure.keychain.ui.adapter.IdentityAdapter;
import org.sufficientlysecure.keychain.ui.adapter.IdentityAdapter.IdentityClickListener;
import org.sufficientlysecure.keychain.ui.dialog.UserIdInfoDialogFragment;
import org.sufficientlysecure.keychain.ui.keyview.LinkedIdViewFragment;
import org.sufficientlysecure.keychain.ui.keyview.loader.IdentityDao.AutocryptPeerInfo;
import org.sufficientlysecure.keychain.ui.keyview.loader.IdentityDao.IdentityInfo;
import org.sufficientlysecure.keychain.ui.keyview.loader.IdentityDao.LinkedIdInfo;
import org.sufficientlysecure.keychain.ui.keyview.loader.IdentityDao.UserIdInfo;
import org.sufficientlysecure.keychain.ui.keyview.loader.ViewKeyLiveData.IdentityLiveData;
import org.sufficientlysecure.keychain.ui.linked.LinkedIdWizard;
import org.sufficientlysecure.keychain.util.Preferences;
import timber.log.Timber;


public class IdentitiesPresenter implements Observer<List<IdentityInfo>> {
    private final Context context;
    private final IdentitiesMvpView view;
    private final ViewKeyMvpView viewKeyMvpView;

    private final IdentityAdapter identitiesAdapter;

    private final long masterKeyId;
    private final boolean isSecret;
    private final boolean showLinkedIds;
    private AutocryptPeerDao autocryptPeerDao;

    public IdentitiesPresenter(Context context, IdentitiesMvpView view, ViewKeyMvpView viewKeyMvpView,
            long masterKeyId, boolean isSecret) {
        this.context = context;
        this.view = view;
        this.viewKeyMvpView = viewKeyMvpView;
        this.autocryptPeerDao = AutocryptPeerDao.getInstance(context);

        this.masterKeyId = masterKeyId;
        this.isSecret = isSecret;

        showLinkedIds = Preferences.getPreferences(context).getExperimentalEnableLinkedIdentities();

        identitiesAdapter = new IdentityAdapter(context, isSecret, new IdentityClickListener() {
            @Override
            public void onClickIdentity(int position) {
                showIdentityInfo(position);
            }

            @Override
            public void onClickIdentityMore(int position, View anchor) {
                showIdentityContextMenu(position, anchor);

            }
        });
        view.setIdentitiesAdapter(identitiesAdapter);

        view.setAddLinkedIdButtonVisible(showLinkedIds && isSecret);

        view.setIdentitiesCardListener(() -> addLinkedIdentity());
    }

    @Override
    public void onChanged(@Nullable List<IdentityInfo> identityInfos) {
        viewKeyMvpView.setContentShown(true, false);
        identitiesAdapter.setData(identityInfos);
    }

    private void showIdentityInfo(final int position) {
        IdentityInfo info = identitiesAdapter.getInfo(position);
        if (info instanceof LinkedIdInfo) {
            showLinkedId((LinkedIdInfo) info);
        } else if (info instanceof UserIdInfo) {
            showUserIdInfo((UserIdInfo) info);
        } else if (info instanceof AutocryptPeerInfo) {
            Intent autocryptPeerIntent = ((AutocryptPeerInfo) info).getAutocryptPeerIntent();
            if (autocryptPeerIntent != null) {
                viewKeyMvpView.startActivity(autocryptPeerIntent);
            }
        }
    }

    private void showIdentityContextMenu(int position, View anchor) {
        viewKeyMvpView.showContextMenu(position, anchor);
    }

    private void showLinkedId(final LinkedIdInfo info) {
        final LinkedIdViewFragment frag;
        try {
            Uri dataUri = UserPackets.buildLinkedIdsUri(KeyRings.buildGenericKeyRingUri(masterKeyId));
            frag = LinkedIdViewFragment.newInstance(dataUri, info.getRank(), isSecret, masterKeyId);
        } catch (IOException e) {
            Timber.e(e, "IOException");
            return;
        }

        viewKeyMvpView.switchToFragment(frag, "linked_id");
    }

    private void showUserIdInfo(UserIdInfo info) {
        if (!isSecret) {
            final int isVerified = info.getVerified();

            UserIdInfoDialogFragment dialogFragment = UserIdInfoDialogFragment.newInstance(false, isVerified);
            viewKeyMvpView.showDialogFragment(dialogFragment, "userIdInfoDialog");
        }
    }

    private void addLinkedIdentity() {
        Intent intent = new Intent(context, LinkedIdWizard.class);
        intent.setData(KeyRings.buildUnifiedKeyRingUri(masterKeyId));
        context.startActivity(intent);
    }

    public void onClickForgetIdentity(int position) {
        AutocryptPeerInfo info = (AutocryptPeerInfo) identitiesAdapter.getInfo(position);
        if (info == null) {
            Timber.e("got a 'forget' click on a bad trust id");
            return;
        }

        autocryptPeerDao.deleteByIdentifier(info.getPackageName(), info.getIdentity());
    }

    public LiveData<List<IdentityInfo>> getLiveDataInstance() {
        return new IdentityLiveData(context, masterKeyId, showLinkedIds);
    }

    public interface IdentitiesMvpView {
        void setIdentitiesAdapter(IdentityAdapter userIdsAdapter);
        void setIdentitiesCardListener(IdentitiesCardListener identitiesCardListener);
        void setAddLinkedIdButtonVisible(boolean showLinkedIds);
    }

    public interface IdentitiesCardListener {
        void onClickAddIdentity();
    }
}