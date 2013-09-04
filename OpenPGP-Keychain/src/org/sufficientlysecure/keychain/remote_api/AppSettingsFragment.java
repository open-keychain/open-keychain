package org.sufficientlysecure.keychain.remote_api;

import org.sufficientlysecure.keychain.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class AppSettingsFragment extends Fragment {
    
    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.api_app_settings_fragment, container, false);
    }
}
