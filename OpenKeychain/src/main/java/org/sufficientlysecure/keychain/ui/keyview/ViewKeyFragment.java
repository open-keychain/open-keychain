/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.DialogFragmentWorkaround;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.ui.base.LoaderFragment;
import org.sufficientlysecure.keychain.ui.keyview.presenter.IdentitiesPresenter;
import org.sufficientlysecure.keychain.ui.keyview.presenter.KeyHealthPresenter;
import org.sufficientlysecure.keychain.ui.keyview.presenter.SystemContactPresenter;
import org.sufficientlysecure.keychain.ui.keyview.presenter.TrustIdsPresenter;
import org.sufficientlysecure.keychain.ui.keyview.presenter.ViewKeyMvpView;
import org.sufficientlysecure.keychain.ui.keyview.view.IdentitiesCardView;
import org.sufficientlysecure.keychain.ui.keyview.view.KeyHealthView;
import org.sufficientlysecure.keychain.ui.keyview.view.SystemContactCardView;
import org.sufficientlysecure.keychain.ui.keyview.view.TrustIdsIdCardView;
import org.sufficientlysecure.keychain.util.Preferences;


public class ViewKeyFragment extends LoaderFragment implements ViewKeyMvpView {
    public static final String ARG_MASTER_KEY_ID = "master_key_id";
    public static final String ARG_IS_SECRET = "is_secret";

    boolean mIsSecret = false;

    private static final int LOADER_IDENTITIES = 1;
    private static final int LOADER_ID_LINKED_CONTACT = 2;
    private static final int LOADER_ID_SUBKEY_STATUS = 3;
    private static final int LOADER_ID_TRUST_IDS = 4;

    private IdentitiesCardView mIdentitiesCardView;
    private IdentitiesPresenter mIdentitiesPresenter;

    private TrustIdsIdCardView mTrustIdsCard;
    private TrustIdsPresenter mTrustIdsPresenter;

    SystemContactCardView mSystemContactCard;
    SystemContactPresenter mSystemContactPresenter;

    KeyHealthView mKeyStatusHealth;

    KeyHealthPresenter mKeyHealthPresenter;

    /**
     * Creates new instance of this fragment
     */
    public static ViewKeyFragment newInstance(long masterKeyId, boolean isSecret) {
        ViewKeyFragment frag = new ViewKeyFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_MASTER_KEY_ID, masterKeyId);
        args.putBoolean(ARG_IS_SECRET, isSecret);

        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup superContainer, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, superContainer, savedInstanceState);
        View view = inflater.inflate(R.layout.view_key_fragment, getContainer());

        mIdentitiesCardView = (IdentitiesCardView) view.findViewById(R.id.card_identities);

        mTrustIdsCard = (TrustIdsIdCardView) view.findViewById(R.id.view_key_card_trust_ids);

        mSystemContactCard = (SystemContactCardView) view.findViewById(R.id.linked_system_contact_card);
        mKeyStatusHealth = (KeyHealthView) view.findViewById(R.id.key_status_health);

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        long masterKeyId = getArguments().getLong(ARG_MASTER_KEY_ID);
        mIsSecret = getArguments().getBoolean(ARG_IS_SECRET);

        mIdentitiesPresenter = new IdentitiesPresenter(
                getContext(), mIdentitiesCardView, this, LOADER_IDENTITIES, masterKeyId, mIsSecret);
        mIdentitiesPresenter.startLoader(getLoaderManager());

        mSystemContactPresenter = new SystemContactPresenter(
                getContext(), mSystemContactCard, LOADER_ID_LINKED_CONTACT, masterKeyId, mIsSecret);
        mSystemContactPresenter.startLoader(getLoaderManager());

        mKeyHealthPresenter = new KeyHealthPresenter(
                getContext(), mKeyStatusHealth, LOADER_ID_SUBKEY_STATUS, masterKeyId, mIsSecret);
        mKeyHealthPresenter.startLoader(getLoaderManager());

        mTrustIdsPresenter = new TrustIdsPresenter(
                getContext(), mTrustIdsCard, this, LOADER_ID_TRUST_IDS, masterKeyId, false);
        mTrustIdsPresenter.startLoader(getLoaderManager());
    }

    @Override
    public void switchToFragment(final Fragment frag, final String backStackName) {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                getFragmentManager().beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .replace(R.id.view_key_fragment, frag)
                        .addToBackStack(backStackName)
                        .commit();
            }
        });
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

    public boolean isValidForData(boolean isSecret) {
        return isSecret == mIsSecret;
    }

    @Override
    public void startActivityAndShowResultSnackbar(Intent intent) {
        startActivityForResult(intent, 0);
    }

    @Override
    public void showDialogFragment(final DialogFragment dialogFragment, final String tag) {
        DialogFragmentWorkaround.INTERFACE.runnableRunDelayed(new Runnable() {
            public void run() {
                dialogFragment.show(getActivity().getSupportFragmentManager(), tag);
            }
        });
    }

    @Override
    public void addFakeBackStackItem(String tag, final OnBackStackPoppedListener listener) {
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            return;
        }

        fragmentManager.beginTransaction()
                .addToBackStack("expand_trust_id")
                .commitAllowingStateLoss();
        fragmentManager.executePendingTransactions();

        fragmentManager.addOnBackStackChangedListener(new OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                FragmentManager fragMan = getFragmentManager();
                fragMan.popBackStack("expand_trust_id", FragmentManager.POP_BACK_STACK_INCLUSIVE);
                fragMan.removeOnBackStackChangedListener(this);

                listener.onBackStackPopped();
            }
        });
    }
}
