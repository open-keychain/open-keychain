/*
 * Copyright (C) 2014-2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2016 Alex Fong Jie Wen <alexfongg@gmail.com>
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