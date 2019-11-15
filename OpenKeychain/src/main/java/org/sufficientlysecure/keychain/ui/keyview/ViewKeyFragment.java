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

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.DialogFragmentWorkaround;
import org.sufficientlysecure.keychain.daos.AutocryptPeerDao;
import org.sufficientlysecure.keychain.model.KeyMetadata;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.ui.adapter.IdentityAdapter;
import org.sufficientlysecure.keychain.ui.adapter.IdentityAdapter.IdentityClickListener;
import org.sufficientlysecure.keychain.ui.dialog.UserIdInfoDialogFragment;
import org.sufficientlysecure.keychain.ui.keyview.loader.IdentityDao.AutocryptPeerInfo;
import org.sufficientlysecure.keychain.ui.keyview.loader.IdentityDao.IdentityInfo;
import org.sufficientlysecure.keychain.ui.keyview.loader.IdentityDao.UserIdInfo;
import org.sufficientlysecure.keychain.ui.keyview.loader.SubkeyStatusDao.KeyHealthStatus;
import org.sufficientlysecure.keychain.ui.keyview.loader.SubkeyStatusDao.KeySubkeyStatus;
import org.sufficientlysecure.keychain.ui.keyview.loader.SubkeyStatusDao.SubKeyItem;
import org.sufficientlysecure.keychain.ui.keyview.loader.SystemContactDao.SystemContactInfo;
import org.sufficientlysecure.keychain.ui.keyview.view.IdentitiesCardView;
import org.sufficientlysecure.keychain.ui.keyview.view.KeyHealthView;
import org.sufficientlysecure.keychain.ui.keyview.view.KeyStatusList.KeyDisplayStatus;
import org.sufficientlysecure.keychain.ui.keyview.view.KeyserverStatusView;
import org.sufficientlysecure.keychain.ui.keyview.view.SystemContactCardView;
import timber.log.Timber;


public class ViewKeyFragment extends Fragment implements OnMenuItemClickListener {
    private IdentitiesCardView identitiesCardView;
    private SystemContactCardView systemContactCard;
    private KeyHealthView keyStatusHealth;
    private KeyserverStatusView keyserverStatusView;
    private View keyStatusCardView;

    IdentityAdapter identitiesAdapter;

    private Integer displayedContextMenuPosition;
    private UnifiedKeyInfo unifiedKeyInfo;
    private KeySubkeyStatus subkeyStatus;
    private boolean showingExpandedInfo;

    public static ViewKeyFragment newInstance() {
        return new ViewKeyFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.view_key_fragment, viewGroup, false);

        identitiesCardView = view.findViewById(R.id.card_identities);
        systemContactCard = view.findViewById(R.id.linked_system_contact_card);
        keyStatusCardView = view.findViewById(R.id.subkey_status_card);
        keyStatusHealth = view.findViewById(R.id.key_status_health);
        keyserverStatusView = view.findViewById(R.id.key_status_keyserver);

        identitiesAdapter = new IdentityAdapter(requireContext(), new IdentityClickListener() {
            @Override
            public void onClickIdentity(int position) {
                showIdentityInfo(position);
            }

            @Override
            public void onClickIdentityMore(int position, View anchor) {
                showIdentityContextMenu(position, anchor);
            }
        });
        identitiesCardView.setIdentitiesAdapter(identitiesAdapter);

        keyStatusCardView.setVisibility(View.GONE);

        keyStatusHealth.setOnHealthClickListener((v) -> onKeyHealthClick());

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Context context = requireContext();

        UnifiedKeyInfoViewModel viewKeyViewModel = ViewModelProviders.of(requireActivity()).get(UnifiedKeyInfoViewModel.class);
        LiveData<UnifiedKeyInfo> unifiedKeyInfoLiveData = viewKeyViewModel.getUnifiedKeyInfoLiveData(requireContext());

        unifiedKeyInfoLiveData.observe(this, this::onLoadUnifiedKeyInfo);

        KeyFragmentViewModel model = ViewModelProviders.of(this).get(KeyFragmentViewModel.class);

        model.getIdentityInfo(context, unifiedKeyInfoLiveData).observe(this, this::onLoadIdentityInfo);
        model.getKeyserverStatus(context, unifiedKeyInfoLiveData).observe(this, this::onLoadKeyMetadata);
        model.getSystemContactInfo(context, unifiedKeyInfoLiveData).observe(this, this::onLoadSystemContact);
        model.getSubkeyStatus(context, unifiedKeyInfoLiveData).observe(this, this::onLoadSubkeyStatus);
    }

    private void onLoadSubkeyStatus(KeySubkeyStatus subkeyStatus) {
        if (subkeyStatus == null) {
            return;
        }

        keyStatusCardView.setVisibility(View.VISIBLE);

        this.subkeyStatus = subkeyStatus;

        KeyHealthStatus keyHealthStatus = subkeyStatus.keyHealthStatus;

        boolean isInsecure = keyHealthStatus == KeyHealthStatus.INSECURE;
        boolean isExpired = keyHealthStatus == KeyHealthStatus.EXPIRED;
        if (isInsecure) {
            boolean primaryKeySecurityProblem = subkeyStatus.keyCertify.mSecurityProblem != null;
            if (primaryKeySecurityProblem) {
                keyStatusHealth.setKeyStatus(keyHealthStatus);
                keyStatusHealth.setPrimarySecurityProblem(subkeyStatus.keyCertify.mSecurityProblem);
                keyStatusHealth.setShowExpander(false);
            } else {
                keyStatusHealth.setKeyStatus(keyHealthStatus);
                keyStatusHealth.setShowExpander(false);
                displayExpandedInfo(false);
            }
        } else if (isExpired) {
            keyStatusHealth.setKeyStatus(keyHealthStatus);
            keyStatusHealth.setPrimaryExpiryDate(subkeyStatus.keyCertify.mExpiry);
            keyStatusHealth.setShowExpander(false);
            keyStatusHealth.hideExpandedInfo();
        } else {
            keyStatusHealth.setKeyStatus(keyHealthStatus);
            keyStatusHealth.setShowExpander(keyHealthStatus != KeyHealthStatus.REVOKED);
            keyStatusHealth.hideExpandedInfo();
        }
    }

    private void displayExpandedInfo(boolean displayAll) {
        SubKeyItem keyCertify = subkeyStatus.keyCertify;
        SubKeyItem keySign = subkeyStatus.keysSign.isEmpty() ? null : subkeyStatus.keysSign.get(0);
        SubKeyItem keyEncrypt = subkeyStatus.keysEncrypt.isEmpty() ? null : subkeyStatus.keysEncrypt.get(0);

        KeyDisplayStatus certDisplayStatus = getKeyDisplayStatus(keyCertify);
        KeyDisplayStatus signDisplayStatus = getKeyDisplayStatus(keySign);
        KeyDisplayStatus encryptDisplayStatus = getKeyDisplayStatus(keyEncrypt);

        if (!displayAll) {
            if (certDisplayStatus == KeyDisplayStatus.OK) {
                certDisplayStatus = null;
            }
            if (certDisplayStatus == KeyDisplayStatus.INSECURE) {
                signDisplayStatus = null;
                encryptDisplayStatus = null;
            }
            if (signDisplayStatus == KeyDisplayStatus.OK) {
                signDisplayStatus = null;
            }
            if (encryptDisplayStatus == KeyDisplayStatus.OK) {
                encryptDisplayStatus = null;
            }
        }

        keyStatusHealth.showExpandedState(certDisplayStatus, signDisplayStatus, encryptDisplayStatus);
    }

    private void onKeyHealthClick() {
        if (showingExpandedInfo) {
            showingExpandedInfo = false;
            keyStatusHealth.hideExpandedInfo();
        } else {
            showingExpandedInfo = true;
            displayExpandedInfo(true);
        }
    }

    private KeyDisplayStatus getKeyDisplayStatus(SubKeyItem subKeyItem) {
        if (subKeyItem == null) {
            return KeyDisplayStatus.UNAVAILABLE;
        }

        if (subKeyItem.mIsRevoked) {
            return KeyDisplayStatus.REVOKED;
        }
        if (subKeyItem.mIsExpired) {
            return KeyDisplayStatus.EXPIRED;
        }
        if (subKeyItem.mSecurityProblem != null) {
            return KeyDisplayStatus.INSECURE;
        }
        if (subKeyItem.mSecretKeyType == SecretKeyType.GNU_DUMMY) {
            return KeyDisplayStatus.STRIPPED;
        }
        if (subKeyItem.mSecretKeyType == SecretKeyType.DIVERT_TO_CARD) {
            return KeyDisplayStatus.DIVERT;
        }

        return KeyDisplayStatus.OK;
    }

    private void onLoadUnifiedKeyInfo(UnifiedKeyInfo unifiedKeyInfo) {
        if (unifiedKeyInfo == null) {
            return;
        }

        this.unifiedKeyInfo = unifiedKeyInfo;
    }

    private void showIdentityInfo(final int position) {
        IdentityInfo info = identitiesAdapter.getInfo(position);
        if (info instanceof UserIdInfo) {
            showUserIdInfo((UserIdInfo) info);
        } else if (info instanceof AutocryptPeerInfo) {
            Intent autocryptPeerIntent = ((AutocryptPeerInfo) info).getAutocryptPeerIntent();
            if (autocryptPeerIntent != null) {
                startActivity(autocryptPeerIntent);
            }
        }
    }

    private void showIdentityContextMenu(int position, View anchor) {
        showContextMenu(position, anchor);
    }

    private void showUserIdInfo(UserIdInfo info) {
        if (!unifiedKeyInfo.has_any_secret()) {
            UserIdInfoDialogFragment dialogFragment = UserIdInfoDialogFragment.newInstance(false, info.isVerified());
            showDialogFragment(dialogFragment, "userIdInfoDialog");
        }
    }

    public void onClickForgetIdentity(int position) {
        AutocryptPeerInfo info = (AutocryptPeerInfo) identitiesAdapter.getInfo(position);
        if (info == null) {
            Timber.e("got a 'forget' click on a bad trust id");
            return;
        }

        AutocryptPeerDao.getInstance(requireContext()).deleteByIdentifier(info.getPackageName(), info.getIdentity());
    }

    private void onLoadIdentityInfo(List<IdentityInfo> identityInfos) {
        identitiesAdapter.setData(identityInfos);
    }

    private void onLoadSystemContact(SystemContactInfo systemContactInfo) {
        if (systemContactInfo == null) {
            systemContactCard.hideLinkedSystemContact();
            return;
        }

        systemContactCard.showLinkedSystemContact(systemContactInfo.contactName, systemContactInfo.contactPicture);
        systemContactCard.setSystemContactClickListener((v) -> launchAndroidContactActivity(systemContactInfo.contactId));
    }

    private void onLoadKeyMetadata(KeyMetadata keyMetadata) {
        if (keyMetadata == null) {
            keyserverStatusView.setDisplayStatusUnknown();
        } else if (keyMetadata.hasBeenUpdated()) {
            if (keyMetadata.isPublished()) {
                keyserverStatusView.setDisplayStatusPublished();
            } else {
                keyserverStatusView.setDisplayStatusNotPublished();
            }
            keyserverStatusView.setLastUpdated(keyMetadata.last_updated());
        } else {
            keyserverStatusView.setDisplayStatusUnknown();
        }
    }

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

    public void showDialogFragment(final DialogFragment dialogFragment, final String tag) {
        DialogFragmentWorkaround.INTERFACE.runnableRunDelayed(
                () -> dialogFragment.show(requireFragmentManager(), tag));
    }

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

        if (item.getItemId() == R.id.autocrypt_forget) {
            int position = displayedContextMenuPosition;
            displayedContextMenuPosition = null;
            onClickForgetIdentity(position);
            return true;
        }

        return false;
    }

    private void launchAndroidContactActivity(long contactId) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, String.valueOf(contactId));
        intent.setData(uri);
        startActivity(intent);
    }
}
