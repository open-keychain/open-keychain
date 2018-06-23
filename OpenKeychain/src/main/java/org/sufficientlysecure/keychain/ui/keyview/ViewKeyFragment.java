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

package org.sufficientlysecure.keychain.ui.keyview;


import java.util.List;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.PopupMenu.OnMenuItemClickListener;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.DialogFragmentWorkaround;
import org.sufficientlysecure.keychain.model.KeyMetadata;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.ui.keyview.ViewKeyActivity.ViewKeyViewModel;
import org.sufficientlysecure.keychain.ui.keyview.loader.IdentityDao.IdentityInfo;
import org.sufficientlysecure.keychain.ui.keyview.loader.SubkeyStatusDao.KeySubkeyStatus;
import org.sufficientlysecure.keychain.ui.keyview.loader.SystemContactDao.SystemContactInfo;
import org.sufficientlysecure.keychain.ui.keyview.presenter.IdentitiesPresenter;
import org.sufficientlysecure.keychain.ui.keyview.presenter.KeyHealthPresenter;
import org.sufficientlysecure.keychain.ui.keyview.presenter.KeyserverStatusPresenter;
import org.sufficientlysecure.keychain.ui.keyview.presenter.SystemContactPresenter;
import org.sufficientlysecure.keychain.ui.keyview.presenter.ViewKeyMvpView;
import org.sufficientlysecure.keychain.ui.keyview.view.IdentitiesCardView;
import org.sufficientlysecure.keychain.ui.keyview.view.KeyHealthView;
import org.sufficientlysecure.keychain.ui.keyview.view.KeyserverStatusView;
import org.sufficientlysecure.keychain.ui.keyview.view.SystemContactCardView;


public class ViewKeyFragment extends Fragment implements ViewKeyMvpView, OnMenuItemClickListener {
    private IdentitiesCardView identitiesCardView;
    private IdentitiesPresenter identitiesPresenter;

    SystemContactCardView systemContactCard;
    SystemContactPresenter systemContactPresenter;

    KeyHealthView keyStatusHealth;
    KeyserverStatusView keyStatusKeyserver;

    KeyHealthPresenter keyHealthPresenter;
    KeyserverStatusPresenter keyserverStatusPresenter;

    private Integer displayedContextMenuPosition;

    public static ViewKeyFragment newInstance() {
        return new ViewKeyFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.view_key_fragment, viewGroup, false);

        identitiesCardView = view.findViewById(R.id.card_identities);
        systemContactCard = view.findViewById(R.id.linked_system_contact_card);
        keyStatusHealth = view.findViewById(R.id.key_status_health);
        keyStatusKeyserver = view.findViewById(R.id.key_status_keyserver);

        return view;
    }

    public static class KeyFragmentViewModel extends ViewModel {
        private LiveData<List<IdentityInfo>> identityInfo;
        private LiveData<KeySubkeyStatus> subkeyStatus;
        private LiveData<SystemContactInfo> systemContactInfo;
        private LiveData<KeyMetadata> keyserverStatus;

        LiveData<List<IdentityInfo>> getIdentityInfo(IdentitiesPresenter identitiesPresenter) {
            if (identityInfo == null) {
                identityInfo = identitiesPresenter.getLiveDataInstance();
            }
            return identityInfo;
        }

        LiveData<KeySubkeyStatus> getSubkeyStatus(KeyHealthPresenter keyHealthPresenter) {
            if (subkeyStatus == null) {
                subkeyStatus = keyHealthPresenter.getLiveDataInstance();
            }
            return subkeyStatus;
        }

        LiveData<SystemContactInfo> getSystemContactInfo(SystemContactPresenter systemContactPresenter) {
            if (systemContactInfo == null) {
                systemContactInfo = systemContactPresenter.getLiveDataInstance();
            }
            return systemContactInfo;
        }

        LiveData<KeyMetadata> getKeyserverStatus(KeyserverStatusPresenter keyserverStatusPresenter) {
            if (keyserverStatus == null) {
                keyserverStatus = keyserverStatusPresenter.getLiveDataInstance();
            }
            return keyserverStatus;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ViewKeyViewModel viewKeyViewModel = ViewModelProviders.of(requireActivity()).get(ViewKeyViewModel.class);
        viewKeyViewModel.getUnifiedKeyInfoLiveData(requireContext()).observe(this, this::onLoadUnifiedKeyInfo);
    }

    private void onLoadUnifiedKeyInfo(UnifiedKeyInfo unifiedKeyInfo) {
        KeyFragmentViewModel model = ViewModelProviders.of(this).get(KeyFragmentViewModel.class);

        identitiesPresenter = new IdentitiesPresenter(
                getContext(), identitiesCardView, this, unifiedKeyInfo.master_key_id(), unifiedKeyInfo.has_any_secret());
        model.getIdentityInfo(identitiesPresenter).observe(this, identitiesPresenter);

        systemContactPresenter = new SystemContactPresenter(
                getContext(), systemContactCard, unifiedKeyInfo.master_key_id(), unifiedKeyInfo.has_any_secret());
        model.getSystemContactInfo(systemContactPresenter).observe(this, systemContactPresenter);

        keyHealthPresenter = new KeyHealthPresenter(getContext(), keyStatusHealth, unifiedKeyInfo.master_key_id());
        model.getSubkeyStatus(keyHealthPresenter).observe(this, keyHealthPresenter);

        keyserverStatusPresenter = new KeyserverStatusPresenter(
                getContext(), keyStatusKeyserver, unifiedKeyInfo.master_key_id(), unifiedKeyInfo.has_any_secret());
        model.getKeyserverStatus(keyserverStatusPresenter).observe(this, keyserverStatusPresenter);
    }

    @Override
    public void switchToFragment(final Fragment frag, final String backStackName) {
        new Handler().post(() -> requireFragmentManager().beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.view_key_fragment, frag)
                .addToBackStack(backStackName)
                .commit());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // if a result has been returned, display a notify
        if (data != null && data.hasExtra(OperationResult.EXTRA_RESULT)) {
            OperationResult result = data.getParcelableExtra(OperationResult.EXTRA_RESULT);
            result.createNotify(getActivity()).show();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void startActivityAndShowResultSnackbar(Intent intent) {
        startActivityForResult(intent, 0);
    }

    @Override
    public void showDialogFragment(final DialogFragment dialogFragment, final String tag) {
        DialogFragmentWorkaround.INTERFACE.runnableRunDelayed(
                () -> dialogFragment.show(requireFragmentManager(), tag));
    }

    @Override
    public void showContextMenu(int position, View anchor) {
        displayedContextMenuPosition = position;

        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.inflate(R.menu.identity_context_menu);
        menu.setOnMenuItemClickListener(this);
        menu.setOnDismissListener(popupMenu -> displayedContextMenuPosition = null);
        menu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (displayedContextMenuPosition == null) {
            return false;
        }

        switch (item.getItemId()) {
            case R.id.autocrypt_forget:
                int position = displayedContextMenuPosition;
                displayedContextMenuPosition = null;
                identitiesPresenter.onClickForgetIdentity(position);
                return true;
        }

        return false;
    }
}
