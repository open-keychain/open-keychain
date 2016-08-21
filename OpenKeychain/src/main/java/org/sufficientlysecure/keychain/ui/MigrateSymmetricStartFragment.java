package org.sufficientlysecure.keychain.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.sufficientlysecure.keychain.R;

public class MigrateSymmetricStartFragment extends Fragment {

    private MigrateSymmetricActivity mMigrateSymmetricActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.migrate_symmetric_start_fragment, container, false);
        View startMigration = view.findViewById(R.id.start_migration);

        startMigration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SetMasterPassphraseFragment frag = SetMasterPassphraseFragment.newInstance(false, null);
                mMigrateSymmetricActivity.loadFragment(frag, MigrateSymmetricActivity.FragAction.TO_RIGHT);
            }
        });

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mMigrateSymmetricActivity = (MigrateSymmetricActivity) getActivity();
    }
}