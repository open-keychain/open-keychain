package org.sufficientlysecure.keychain.ui.keyunlock.base;


import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey;

/**
 * Base unlock fragment where the user will input the data.
 */
public class UnlockOptionFragment extends Fragment {
    private CanonicalizedSecretKey.SecretKeyType secretKeyType;

    public static UnlockOptionFragment newInstance() {
        UnlockOptionFragment fragment = new UnlockOptionFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    public UnlockOptionFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.unlock_fragment, container, false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public CanonicalizedSecretKey.SecretKeyType getSecretKeyType() {
        return secretKeyType;
    }

    public void setSecretKeyType(CanonicalizedSecretKey.SecretKeyType secretKeyType) {
        this.secretKeyType = secretKeyType;
    }
}
